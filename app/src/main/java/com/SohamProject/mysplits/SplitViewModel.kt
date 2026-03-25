package com.SohamProject.mysplits

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth


class SplitViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).splitDao()
    private val authReady = kotlinx.coroutines.CompletableDeferred<Unit>()
    val allGroups: StateFlow<List<Group>> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener {
                Log.d("SplitViewModel", "✅ Signed in anonymously")
                authReady.complete(Unit) // ✅ unlock Firebase calls
            }
            .addOnFailureListener {
                Log.e("SplitViewModel", "❌ Auth failed: ${it.message}")
                authReady.completeExceptionally(it) // ✅ fail gracefully
            }
    }

    // -------------------------------------------------------------------------
    // Generates a short 6-character human-readable join code e.g. "A3F9KX"
    // Called ONLY when creating a group for the first time
    // -------------------------------------------------------------------------
    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    // -------------------------------------------------------------------------
    // Local DB operations
    // -------------------------------------------------------------------------

    fun addGroup(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertGroup(Group(name = name, isOnline = false, remoteId = null, joinCode = null))
        }
    }

    fun getPayers(groupId: Int): Flow<List<ExpensePayer>> = dao.getPayersForGroupFlow(groupId)

    fun getGroupShares(groupId: Int): Flow<List<ExpenseShare>> = dao.getGroupSharesFlow(groupId)

    fun getMembers(groupId: Int): Flow<List<Member>> = dao.getMembersForGroup(groupId)

    fun getGroup(groupId: Int): Flow<Group?> = dao.getGroupById(groupId)

    fun addMember(groupId: Int, name: String, weight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMember(Member(groupId = groupId, name = name, defaultWeight = weight))
        }
    }

    fun getExpenses(groupId: Int): Flow<List<Expense>> = dao.getExpensesForGroup(groupId)

    fun getExpensesWithPayers(groupId: Int): Flow<List<ExpenseWithPayers>> =
        dao.getExpensesWithPayersForGroup(groupId)

    fun getGroupBalances(groupId: Int): Flow<Map<String, Double>> = flow {
        val shares = dao.getAllGroupShares(groupId)
        val membersList = dao.getMembersForGroup(groupId).first()
        val balances = mutableMapOf<String, Double>()
        membersList.forEach { balances[it.name] = 0.0 }
        shares.forEach { share ->
            val memberName = membersList.find { it.id == share.memberId }?.name
            if (memberName != null) {
                balances[memberName] = (balances[memberName] ?: 0.0) + share.amountOwed
            }
        }
        emit(balances)
    }

    fun addExpense(
        groupId: Int,
        description: String,
        totalAmount: Double,
        payerMap: Map<Int, Double>,
        involvedMap: Map<Int, Double>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseId = dao.insertExpense(
                Expense(groupId = groupId, description = description, totalAmount = totalAmount)
            ).toInt()

            dao.insertPayers(payerMap.map { (id, amount) ->
                ExpensePayer(expenseId = expenseId, memberId = id, amountPaid = amount)
            })

            if (involvedMap.isNotEmpty()) {
                val totalWeight = involvedMap.values.sum()
                if (totalWeight > 0) {
                    val costPerUnit = totalAmount / totalWeight
                    dao.insertShares(involvedMap.map { (memberId, weight) ->
                        ExpenseShare(
                            expenseId = expenseId,
                            memberId = memberId,
                            weight = weight,
                            amountOwed = costPerUnit * weight
                        )
                    })
                }
            }

            val group = dao.getGroupByIdSync(groupId)
            if (group?.isOnline == true) {
                uploadGroupToCloud(group)
            }
        }
    }

    fun getSettlementPlan(groupId: Int): Flow<List<Settlement>> = flow {
        val members = dao.getMembersForGroup(groupId).first()
        val allShares = dao.getAllGroupShares(groupId)
        val allPayers = dao.getAllGroupPayers(groupId)
        val balances = mutableMapOf<Int, Double>()
        members.forEach { balances[it.id] = 0.0 }
        allPayers.forEach { payer ->
            balances[payer.memberId] = (balances[payer.memberId] ?: 0.0) + payer.amountPaid
        }
        allShares.forEach { share ->
            balances[share.memberId] = (balances[share.memberId] ?: 0.0) - share.amountOwed
        }
        val debtors = mutableListOf<Pair<Int, Double>>()
        val creditors = mutableListOf<Pair<Int, Double>>()
        balances.forEach { (id, amount) ->
            val rounded = Math.round(amount * 100.0) / 100.0
            if (rounded < 0) debtors.add(id to rounded)
            if (rounded > 0) creditors.add(id to rounded)
        }
        val settlements = mutableListOf<Settlement>()
        debtors.sortBy { it.second }
        creditors.sortByDescending { it.second }
        var i = 0; var j = 0
        while (i < debtors.size && j < creditors.size) {
            val debtor = debtors[i]; val creditor = creditors[j]
            val settleAmount = minOf(-debtor.second, creditor.second)
            val fromName = members.find { it.id == debtor.first }?.name ?: "?"
            val toName = members.find { it.id == creditor.first }?.name ?: "?"
            if (settleAmount > 0.01) settlements.add(Settlement(fromName, toName, settleAmount))
            val remainingDebtor = debtor.second + settleAmount
            val remainingCreditor = creditor.second - settleAmount
            debtors[i] = debtor.first to remainingDebtor
            creditors[j] = creditor.first to remainingCreditor
            if (Math.abs(remainingDebtor) < 0.01) i++
            if (Math.abs(remainingCreditor) < 0.01) j++
        }
        emit(settlements)
    }

    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteExpense(expenseId) }
    }

    suspend fun getExpenseShares(expenseId: Int): List<ExpenseShare> =
        dao.getSharesForExpenseSync(expenseId)

    fun deleteGroup(group: Group) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteGroup(group) }
    }

    // -------------------------------------------------------------------------
    // Firebase
    // -------------------------------------------------------------------------

    private val firebaseDb =
        com.google.firebase.database.FirebaseDatabase
            .getInstance("https://mysplit-f9b25-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("groups")

    // ✅ FIX: toggleOnline now PRESERVES the existing remoteId and joinCode
    // so the group's join code never changes after first creation
    fun toggleOnline(group: Group) {
        Log.d("SplitViewModel", "toggleOnline called! isOnline=${group.isOnline}, name=${group.name}")
        viewModelScope.launch(Dispatchers.IO) {
            authReady.await()
            if (!group.isOnline) {
                // ✅ KEY FIX: Reuse existing remoteId if the group was online before.
                // Only generate a new remoteId the very first time (when remoteId is null).
                val remoteId = group.remoteId ?: firebaseDb.push().key

                if (remoteId == null) {
                    Log.e("SplitViewModel", "Could not generate Firebase key — are you online?")
                    return@launch
                }

                // ✅ KEY FIX: Reuse existing joinCode if available.
                // Only generate a new joinCode the very first time (when joinCode is null).
                val joinCode = group.joinCode ?: generateJoinCode()

                Log.d("SplitViewModel", "Using remoteId: $remoteId, joinCode: $joinCode")

                val onlineGroup = group.copy(
                    isOnline = true,
                    remoteId = remoteId,
                    joinCode = joinCode       // ← stored permanently in local DB
                )
                dao.updateGroup(onlineGroup)
                uploadGroupToCloud(onlineGroup)
            } else {
                // Going offline — keep remoteId and joinCode intact so re-joining still works
                dao.updateGroup(group.copy(isOnline = false))
            }
        }
    }

    private suspend fun uploadGroupToCloud(group: Group) {
        Log.d("SplitViewModel", "uploadGroupToCloud: ${group.name}, remoteId=${group.remoteId}, joinCode=${group.joinCode}")
        authReady.await()
        val members  = dao.getMembersForGroupSync(group.id)
        val expenses = dao.getExpensesForGroupSync(group.id)

        fun nameOf(memberId: Int) = members.find { it.id == memberId }?.name ?: ""

        val firebaseMembers = members.map { m ->
            FirebaseMember(
                id            = m.id,
                groupId       = m.groupId,
                name          = m.name,
                defaultWeight = m.defaultWeight
            )
        }

        val firebaseExpenses = expenses.map { expense ->
            val payers = dao.getPayersForExpenseSync(expense.id)
            val shares = dao.getSharesForExpenseSync(expense.id)

            FirebaseExpense(
                id          = expense.id,
                groupId     = expense.groupId,
                description = expense.description,
                totalAmount = expense.totalAmount,
                payers = payers.map { p ->
                    FirebaseExpensePayer(
                        expenseId  = p.expenseId,
                        memberName = nameOf(p.memberId),
                        amountPaid = p.amountPaid
                    )
                },
                shares = shares.map { s ->
                    FirebaseExpenseShare(
                        expenseId  = s.expenseId,
                        memberName = nameOf(s.memberId),
                        weight     = s.weight,
                        amountOwed = s.amountOwed
                    )
                }
            )
        }

        val data = mapOf(
            "name"     to group.name,
            "joinCode" to (group.joinCode ?: ""),   // ✅ stored in Firebase permanently
            "members"  to firebaseMembers,
            "expenses" to firebaseExpenses
        )

        group.remoteId?.let { remoteId ->
            firebaseDb.child(remoteId).setValue(data)
                .addOnSuccessListener {
                    Log.d("SplitViewModel", "✅ Firebase write SUCCESS for joinCode: ${group.joinCode}")
                }
                .addOnFailureListener { e ->
                    Log.e("SplitViewModel", "❌ Firebase write FAILED: ${e.message}")
                }
        }
    }

    // ✅ NEW: Join a group using the short human-readable join code (e.g. "A3F9KX")
    // This searches Firebase for a group whose joinCode field matches
    suspend fun joinGroupByCode(joinCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                authReady.await()

                // Search Firebase for a group with this joinCode
                val snapshot = firebaseDb
                    .orderByChild("joinCode")
                    .equalTo(joinCode.trim().uppercase())
                    .get()
                    .await()

                if (!snapshot.exists()) {
                    Log.e("SplitViewModel", "No group found with joinCode: $joinCode")
                    return@withContext false
                }

                // Get the first matching group (joinCodes should be unique)
                val groupSnapshot = snapshot.children.firstOrNull() ?: return@withContext false
                val remoteId = groupSnapshot.key ?: return@withContext false

                // ✅ Prevent joining the same group twice
                val alreadyJoined = dao.getAllGroupsSync().any { it.remoteId == remoteId }
                if (alreadyJoined) {
                    Log.w("SplitViewModel", "Already joined group with joinCode: $joinCode")
                    return@withContext false
                }

                val firebaseGroupData = groupSnapshot.getValue(FirebaseGroupData::class.java)
                    ?: run {
                        Log.e("SplitViewModel", "Could not parse FirebaseGroupData for joinCode: $joinCode")
                        return@withContext false
                    }

                // Insert the group locally, preserving both remoteId and joinCode
                val newGroupId = dao.insertGroup(
                    Group(
                        name     = firebaseGroupData.name ?: "Joined Group",
                        isOnline = true,
                        remoteId = remoteId,
                        joinCode = joinCode.trim().uppercase()   // ✅ saved locally too
                    )
                ).toInt()

                val memberNameToLocalId = mutableMapOf<String, Int>()

                firebaseGroupData.members?.forEach { fm ->
                    val safeName = fm.name ?: "Unknown"
                    val newLocalId = dao.insertMember(
                        Member(
                            id            = 0,
                            groupId       = newGroupId,
                            name          = safeName,
                            defaultWeight = fm.defaultWeight
                        )
                    ).toInt()
                    memberNameToLocalId[safeName] = newLocalId
                }

                firebaseGroupData.expenses?.forEach { fe ->
                    val newExpenseId = dao.insertExpense(
                        Expense(
                            groupId     = newGroupId,
                            description = fe.description ?: "Unnamed Expense",
                            totalAmount = fe.totalAmount
                        )
                    ).toInt()

                    fe.payers?.forEach { fp ->
                        val localMemberId = memberNameToLocalId[fp.memberName]
                        if (localMemberId != null) {
                            dao.insertPayers(listOf(
                                ExpensePayer(
                                    expenseId  = newExpenseId,
                                    memberId   = localMemberId,
                                    amountPaid = fp.amountPaid
                                )
                            ))
                        } else {
                            Log.w("SplitViewModel", "Payer name not found: ${fp.memberName}")
                        }
                    }

                    fe.shares?.forEach { fs ->
                        val localMemberId = memberNameToLocalId[fs.memberName]
                        if (localMemberId != null) {
                            dao.insertShares(listOf(
                                ExpenseShare(
                                    expenseId  = newExpenseId,
                                    memberId   = localMemberId,
                                    weight     = fs.weight,
                                    amountOwed = fs.amountOwed
                                )
                            ))
                        } else {
                            Log.w("SplitViewModel", "Share member name not found: ${fs.memberName}")
                        }
                    }
                }

                Log.d("SplitViewModel", "✅ Successfully joined group with joinCode: $joinCode")
                true

            } catch (e: Exception) {
                Log.e("SplitViewModel", "Error joining group: ${e.message}", e)
                false
            }
        }
    }

    // ✅ KEPT for backward compatibility if you were using remoteId directly before
    suspend fun joinGroup(remoteId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                authReady.await()
                val alreadyJoined = dao.getAllGroupsSync().any { it.remoteId == remoteId }
                if (alreadyJoined) {
                    Log.w("SplitViewModel", "Already joined group: $remoteId")
                    return@withContext false
                }

                val snapshot = firebaseDb.child(remoteId.trim()).get().await()

                if (!snapshot.exists()) {
                    Log.e("SplitViewModel", "No Firebase data found for remoteId: $remoteId")
                    return@withContext false
                }

                val firebaseGroupData = snapshot.getValue(FirebaseGroupData::class.java)
                    ?: run {
                        Log.e("SplitViewModel", "Could not parse FirebaseGroupData for: $remoteId")
                        return@withContext false
                    }

                val savedJoinCode = firebaseGroupData.joinCode  // ✅ read joinCode from Firebase

                val newGroupId = dao.insertGroup(
                    Group(
                        name     = firebaseGroupData.name ?: "Joined Group",
                        isOnline = true,
                        remoteId = remoteId,
                        joinCode = savedJoinCode                 // ✅ preserve it locally
                    )
                ).toInt()

                val memberNameToLocalId = mutableMapOf<String, Int>()

                firebaseGroupData.members?.forEach { fm ->
                    val safeName = fm.name ?: "Unknown"
                    val newLocalId = dao.insertMember(
                        Member(
                            id            = 0,
                            groupId       = newGroupId,
                            name          = safeName,
                            defaultWeight = fm.defaultWeight
                        )
                    ).toInt()
                    memberNameToLocalId[safeName] = newLocalId
                }

                firebaseGroupData.expenses?.forEach { fe ->
                    val newExpenseId = dao.insertExpense(
                        Expense(
                            groupId     = newGroupId,
                            description = fe.description ?: "Unnamed Expense",
                            totalAmount = fe.totalAmount
                        )
                    ).toInt()

                    fe.payers?.forEach { fp ->
                        val localMemberId = memberNameToLocalId[fp.memberName]
                        if (localMemberId != null) {
                            dao.insertPayers(listOf(
                                ExpensePayer(
                                    expenseId  = newExpenseId,
                                    memberId   = localMemberId,
                                    amountPaid = fp.amountPaid
                                )
                            ))
                        }
                    }

                    fe.shares?.forEach { fs ->
                        val localMemberId = memberNameToLocalId[fs.memberName]
                        if (localMemberId != null) {
                            dao.insertShares(listOf(
                                ExpenseShare(
                                    expenseId  = newExpenseId,
                                    memberId   = localMemberId,
                                    weight     = fs.weight,
                                    amountOwed = fs.amountOwed
                                )
                            ))
                        }
                    }
                }

                true

            } catch (e: Exception) {
                Log.e("SplitViewModel", "Error joining group: ${e.message}", e)
                false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Real-time listener — syncs changes from other devices automatically
    // -------------------------------------------------------------------------

    private val activeListeners = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()

    fun startListeningToGroup(group: Group) {
        val remoteId = group.remoteId ?: return
        if (activeListeners.containsKey(remoteId)) return

        Log.d("SplitViewModel", "👂 Starting listener for: ${group.name}")

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                Log.d("SplitViewModel", "🔄 Firebase change detected for: ${group.name}")
                val firebaseData = snapshot.getValue(FirebaseGroupData::class.java) ?: return

                viewModelScope.launch(Dispatchers.IO) {
                    // 1. Delete old local data for this group
                    dao.deleteMembersForGroup(group.id)
                    dao.deleteExpensesForGroup(group.id)

                    // 2. Re-insert members
                    val memberNameToLocalId = mutableMapOf<String, Int>()
                    firebaseData.members?.forEach { fm ->
                        val safeName = fm.name ?: "Unknown"
                        val newId = dao.insertMember(
                            Member(
                                id = 0,
                                groupId = group.id,
                                name = safeName,
                                defaultWeight = fm.defaultWeight
                            )
                        ).toInt()
                        memberNameToLocalId[safeName] = newId
                    }

                    // 3. Re-insert expenses with payers and shares
                    firebaseData.expenses?.forEach { fe ->
                        val newExpenseId = dao.insertExpense(
                            Expense(
                                groupId = group.id,
                                description = fe.description ?: "Unnamed",
                                totalAmount = fe.totalAmount
                            )
                        ).toInt()

                        fe.payers?.forEach { fp ->
                            val localId = memberNameToLocalId[fp.memberName] ?: return@forEach
                            dao.insertPayers(listOf(
                                ExpensePayer(
                                    expenseId  = newExpenseId,
                                    memberId   = localId,
                                    amountPaid = fp.amountPaid
                                )
                            ))
                        }

                        fe.shares?.forEach { fs ->
                            val localId = memberNameToLocalId[fs.memberName] ?: return@forEach
                            dao.insertShares(listOf(
                                ExpenseShare(
                                    expenseId  = newExpenseId,
                                    memberId   = localId,
                                    weight     = fs.weight,
                                    amountOwed = fs.amountOwed
                                )
                            ))
                        }
                    }
                    Log.d("SplitViewModel", "✅ Local DB synced from Firebase for: ${group.name}")
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("SplitViewModel", "❌ Listener cancelled: ${error.message}")
            }
        }

        firebaseDb.child(remoteId).addValueEventListener(listener)
        activeListeners[remoteId] = listener
    }

    fun stopListeningToGroup(remoteId: String) {
        activeListeners[remoteId]?.let {
            firebaseDb.child(remoteId).removeEventListener(it)
            activeListeners.remove(remoteId)
            Log.d("SplitViewModel", "🛑 Stopped listener for remoteId: $remoteId")
        }
    }
}
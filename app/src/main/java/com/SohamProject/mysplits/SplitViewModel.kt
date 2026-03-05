package com.SohamProject.mysplits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Connect to Database
    private val dao = AppDatabase.getDatabase(application).splitDao()

    // 2. Groups List (Observed by UI)
    val allGroups: StateFlow<List<Group>> = dao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. Add Group
    fun addGroup(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertGroup(Group(name = name))
        }
    }

    // Inside SplitViewModel class...
    fun getPayers(groupId: Int): Flow<List<ExpensePayer>> = dao.getPayersForGroupFlow(groupId)

// Inside SplitViewModel...

    fun getGroupShares(groupId: Int): Flow<List<ExpenseShare>> = dao.getGroupSharesFlow(groupId)


    // --- MEMBER LOGIC ---

    // Get members for a specific group
    fun getMembers(groupId: Int): Flow<List<Member>> = dao.getMembersForGroup(groupId)

    // Add Member with a Default Weight (Req #6: e.g., Dan has weight 2.5)
    fun addMember(groupId: Int, name: String, weight: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMember(Member(groupId = groupId, name = name, defaultWeight = weight))
        }
    }

    // --- EXPENSE LOGIC ---

    // Get expenses
    fun getExpenses(groupId: Int): Flow<List<Expense>> = dao.getExpensesForGroup(groupId)

    // Get expenses with payers
    fun getExpensesWithPayers(groupId: Int): Flow<List<ExpenseWithPayers>> = dao.getExpensesWithPayersForGroup(groupId)

    // Get formatted totals (Who owes what)
    // This runs a complex calculation to determine net balances
    fun getGroupBalances(groupId: Int): Flow<Map<String, Double>> = flow {
        // This is a simplified "Total Spent" view.
        // Real settlement logic (Req #9) is complex, but this tracks "Cost per person"
        val shares = dao.getAllGroupShares(groupId)
        val membersList = dao.getMembersForGroup(groupId).first() // Get current members once

        val balances = mutableMapOf<String, Double>()
        membersList.forEach { balances[it.name] = 0.0 }

        shares.forEach { share ->
            // Find member name
            val memberName = membersList.find { it.id == share.memberId }?.name
            if (memberName != null) {
                val current = balances[memberName] ?: 0.0
                balances[memberName] = current + share.amountOwed
            }
        }
        emit(balances)
    }

    // THE BIG MATH FUNCTION (Req #5, #6, #7)
    // Updated: Now accepts 'involvedMap' which contains { MemberID -> Weight used for THIS expense }
    fun addExpense(
        groupId: Int,
        description: String,
        totalAmount: Double,
        payerMap: Map<Int, Double>,
        involvedMap: Map<Int, Double> // Changed from List<Int> to Map<Int, Double>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert Expense
            val expenseId = dao.insertExpense(
                Expense(
                    groupId = groupId,
                    description = description,
                    totalAmount = totalAmount
                )
            ).toInt()

            // 2. Insert Payers
            val payers = payerMap.map { (id, amount) ->
                ExpensePayer(expenseId = expenseId, memberId = id, amountPaid = amount)
            }
            dao.insertPayers(payers)

            // 3. Insert Shares (Using the CUSTOM weights from the dialog)
            if (involvedMap.isNotEmpty()) {
                // Calculate total weight based on the user's input for THIS expense
                val totalWeight = involvedMap.values.sum()

                if (totalWeight > 0) {
                    val costPerUnit = totalAmount / totalWeight

                    val shares = involvedMap.map { (memberId, weight) ->
                        ExpenseShare(
                            expenseId = expenseId,
                            memberId = memberId,
                            weight = weight,
                            amountOwed = costPerUnit * weight
                        )
                    }
                    dao.insertShares(shares)
                }
            }
        }
    }



    // --- SETTLEMENT LOGIC (Requirement #9) ---

    fun getSettlementPlan(groupId: Int): Flow<List<Settlement>> = flow {
        val members = dao.getMembersForGroup(groupId).first()
        val allShares = dao.getAllGroupShares(groupId)
        val allPayers = dao.getAllGroupPayers(groupId) // <--- Get data from new table

        val balances = mutableMapOf<Int, Double>()
        members.forEach { balances[it.id] = 0.0 }

        // A. Add Credit for Paying (Use new table)
        allPayers.forEach { payer ->
            val current = balances[payer.memberId] ?: 0.0
            balances[payer.memberId] = current + payer.amountPaid
        }

        // B. Subtract Debit for Consuming (Same as before)
        allShares.forEach { share ->
            val current = balances[share.memberId] ?: 0.0
            balances[share.memberId] = current - share.amountOwed
        }

        // ... The rest of the Greedy Algorithm (debtors/creditors loop) is exactly the same as before ...
        
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
            val amountOwed = -debtor.second
            val amountReceivable = creditor.second
            val settleAmount = minOf(amountOwed, amountReceivable)
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

    // Inside SplitViewModel...

    // 1. DELETE
    fun deleteExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteExpense(expenseId)
        }
    }

    // 2. FETCH (For pre-filling the dialog)
    suspend fun getExpenseShares(expenseId: Int): List<ExpenseShare> {
        return dao.getSharesForExpenseSync(expenseId)
    }
    fun deleteGroup(group: Group) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteGroup(group)
        }
    }

}

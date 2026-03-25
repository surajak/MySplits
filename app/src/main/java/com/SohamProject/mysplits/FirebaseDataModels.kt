package com.SohamProject.mysplits

import kotlinx.coroutines.tasks.await // For .await() Firebase calls
import android.util.Log // For Log.e
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

 import com.SohamProject.mysplits.FirebaseGroupData
 import com.SohamProject.mysplits.FirebaseMember
 import com.SohamProject.mysplits.FirebaseExpense
 import com.SohamProject.mysplits.FirebaseExpensePayer
 import com.SohamProject.mysplits.FirebaseExpenseShare

 //Data class to represent the structure of a group in Firebase
 data class FirebaseGroupData(
     val name: String? = null,
     val joinCode: String? = null,   // ← ADD THIS
     val members: List<FirebaseMember>? = null,
     val expenses: List<FirebaseExpense>? = null
 )
// Simplified Member for Firebase (Firebase IDs are string, Room IDs are int)
data class FirebaseMember(
    val id: Int = 0, // This is the local Room ID, used for mapping. Firebase stores its own ID.
    val groupId: Int = 0, // Not strictly needed for Firebase, but good for local mapping
    val name: String? = null,
    val defaultWeight: Double = 1.0
)

// Simplified Expense for Firebase
data class FirebaseExpense(
    val id: Int = 0, // Local Room ID
    val groupId: Int = 0,
    val description: String? = null,
    val totalAmount: Double = 0.0,
    val payers: List<FirebaseExpensePayer>? = null,
    val shares: List<FirebaseExpenseShare>? = null
)

data class FirebaseExpensePayer(
    val expenseId: Int = 0, // Local Room ID
    val memberName: String? = null, // Local Room ID
    val amountPaid: Double = 0.0
)

data class FirebaseExpenseShare(
    val expenseId: Int = 0, // Local Room ID
    val memberName: String? = null, // Local Room ID
    val weight: Double = 0.0,
    val amountOwed: Double = 0.0
)

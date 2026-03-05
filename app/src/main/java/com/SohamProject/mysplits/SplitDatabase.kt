package com.SohamProject.mysplits

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. THE DAO (Database Access Object)
@Dao
interface SplitDao {
    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<Group>>

    @Insert
    suspend fun insertGroup(group: Group)

    // New: Observe all shares to calculate "Cost" live
    @Query("SELECT expense_shares.* FROM expense_shares INNER JOIN expenses ON expense_shares.expenseId = expenses.id WHERE expenses.groupId = :groupId")
    fun getGroupSharesFlow(groupId: Int): Flow<List<ExpenseShare>>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: Int): Flow<List<Member>>

    @Insert
    suspend fun insertMember(member: Member)

    @Transaction
    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    fun getExpensesWithPayersForGroup(groupId: Int): Flow<List<ExpenseWithPayers>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    fun getExpensesForGroup(groupId: Int): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Insert
    suspend fun insertShares(shares: List<ExpenseShare>)

    @Query("""
        SELECT expense_shares.* FROM expense_shares
        INNER JOIN expenses ON expense_shares.expenseId = expenses.id 
        WHERE expenses.groupId = :groupId
    """)
    suspend fun getAllGroupShares(groupId: Int): List<ExpenseShare>

    // --- NEW METHODS FOR MULTI-PAYER ---
    @Insert
    suspend fun insertPayers(payers: List<ExpensePayer>)

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpense(expenseId: Int): List<ExpensePayer>

    // Helper for Settlement: Get ALL payers for the whole group
    @Query("""
        SELECT expense_payers.* FROM expense_payers
        INNER JOIN expenses ON expense_payers.expenseId = expenses.id 
        WHERE expenses.groupId = :groupId
    """)
    suspend fun getAllGroupPayers(groupId: Int): List<ExpensePayer>


    @Query("SELECT * FROM expense_payers WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId)")
    fun getPayersForGroupFlow(groupId: Int): Flow<List<ExpensePayer>>

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: Int)

    // Keep this helper for pre-filling the dialog!
    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getSharesForExpenseSync(expenseId: Int): List<ExpenseShare>

    @Delete
    suspend fun deleteGroup(group: Group)


}

// 2. THE DATABASE SETUP
@Database(
    entities = [Group::class, Member::class, Expense::class, ExpenseShare::class, ExpensePayer::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun splitDao(): SplitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "split_it_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

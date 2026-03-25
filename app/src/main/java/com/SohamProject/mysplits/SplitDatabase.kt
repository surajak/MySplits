package com.SohamProject.mysplits

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
interface SplitDao {

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpenseSync(expenseId: Int): List<ExpensePayer>
 
    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroupsSync(): List<Group>
 
    @Query("SELECT * FROM `groups` WHERE id = :groupId LIMIT 1")
    suspend fun getGroupByIdSync(groupId: Int): Group?

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: Int)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesForGroup(groupId: Int)

    @Query("SELECT * FROM `groups`")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM members WHERE name = :memberName LIMIT 1")
    suspend fun getMemberByNameSync(memberName: String?): Member?

    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun getGroupById(groupId: Int): Flow<Group?>

    @Insert
    suspend fun insertGroup(group: Group): Long

    @Query("SELECT expense_shares.* FROM expense_shares INNER JOIN expenses ON expense_shares.expenseId = expenses.id WHERE expenses.groupId = :groupId")
    fun getGroupSharesFlow(groupId: Int): Flow<List<ExpenseShare>>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: Int): Flow<List<Member>>

    @Insert
    suspend fun insertMember(member: Member): Long

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

    @Insert
    suspend fun insertPayers(payers: List<ExpensePayer>)

    @Query("SELECT * FROM expense_payers WHERE expenseId = :expenseId")
    suspend fun getPayersForExpense(expenseId: Int): List<ExpensePayer>

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

    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getSharesForExpenseSync(expenseId: Int): List<ExpenseShare>

    @Query("SELECT * FROM members WHERE groupId = :groupId")
    suspend fun getMembersForGroupSync(groupId: Int): List<Member>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getExpensesForGroupSync(groupId: Int): List<Expense>

    @Delete
    suspend fun deleteGroup(group: Group)

    @Update
    suspend fun updateGroup(group: Group)
}

@Database(
    entities = [Group::class, Member::class, Expense::class, ExpensePayer::class, ExpenseShare::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun splitDao(): SplitDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `groups` ADD COLUMN `joinCode` TEXT DEFAULT NULL"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "split_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

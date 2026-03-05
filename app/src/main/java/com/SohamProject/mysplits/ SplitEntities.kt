package com.SohamProject.mysplits

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

// 1. THE GROUP
@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

// 2. THE MEMBER
@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val defaultWeight: Double = 1.0
)

// 3. THE EXPENSE (Updated)
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val description: String,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis()
)

// 4. THE PAYERS
@Entity(
    tableName = "expense_payers",
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpensePayer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val amountPaid: Double
)

// 5. THE SPLIT DETAILS
@Entity(
    tableName = "expense_shares",
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseShare(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val weight: Double,
    val amountOwed: Double
)

// POJO for UI and joined queries
data class ExpenseWithPayers(
    @Embedded val expense: Expense,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val payers: List<ExpensePayer>
)

data class Settlement(
    val fromName: String,
    val toName: String,
    val amount: Double
)
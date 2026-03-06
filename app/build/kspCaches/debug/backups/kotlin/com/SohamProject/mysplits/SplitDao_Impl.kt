package com.SohamProject.mysplits

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class SplitDao_Impl(
  __db: RoomDatabase,
) : SplitDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGroup: EntityInsertAdapter<Group>

  private val __insertAdapterOfMember: EntityInsertAdapter<Member>

  private val __insertAdapterOfExpense: EntityInsertAdapter<Expense>

  private val __insertAdapterOfExpenseShare: EntityInsertAdapter<ExpenseShare>

  private val __insertAdapterOfExpensePayer: EntityInsertAdapter<ExpensePayer>

  private val __deleteAdapterOfGroup: EntityDeleteOrUpdateAdapter<Group>
  init {
    this.__db = __db
    this.__insertAdapterOfGroup = object : EntityInsertAdapter<Group>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `groups` (`id`,`name`,`createdAt`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Group) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.createdAt)
      }
    }
    this.__insertAdapterOfMember = object : EntityInsertAdapter<Member>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `members` (`id`,`groupId`,`name`,`defaultWeight`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Member) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.groupId.toLong())
        statement.bindText(3, entity.name)
        statement.bindDouble(4, entity.defaultWeight)
      }
    }
    this.__insertAdapterOfExpense = object : EntityInsertAdapter<Expense>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `expenses` (`id`,`groupId`,`description`,`totalAmount`,`date`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Expense) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.groupId.toLong())
        statement.bindText(3, entity.description)
        statement.bindDouble(4, entity.totalAmount)
        statement.bindLong(5, entity.date)
      }
    }
    this.__insertAdapterOfExpenseShare = object : EntityInsertAdapter<ExpenseShare>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `expense_shares` (`id`,`expenseId`,`memberId`,`weight`,`amountOwed`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExpenseShare) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.expenseId.toLong())
        statement.bindLong(3, entity.memberId.toLong())
        statement.bindDouble(4, entity.weight)
        statement.bindDouble(5, entity.amountOwed)
      }
    }
    this.__insertAdapterOfExpensePayer = object : EntityInsertAdapter<ExpensePayer>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `expense_payers` (`id`,`expenseId`,`memberId`,`amountPaid`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExpensePayer) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.expenseId.toLong())
        statement.bindLong(3, entity.memberId.toLong())
        statement.bindDouble(4, entity.amountPaid)
      }
    }
    this.__deleteAdapterOfGroup = object : EntityDeleteOrUpdateAdapter<Group>() {
      protected override fun createQuery(): String = "DELETE FROM `groups` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Group) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertGroup(group: Group): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGroup.insert(_connection, group)
  }

  public override suspend fun insertMember(member: Member): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMember.insert(_connection, member)
  }

  public override suspend fun insertExpense(expense: Expense): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfExpense.insertAndReturnId(_connection, expense)
    _result
  }

  public override suspend fun insertShares(shares: List<ExpenseShare>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfExpenseShare.insert(_connection, shares)
  }

  public override suspend fun insertPayers(payers: List<ExpensePayer>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfExpensePayer.insert(_connection, payers)
  }

  public override suspend fun deleteGroup(group: Group): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfGroup.handle(_connection, group)
  }

  public override fun getAllGroups(): Flow<List<Group>> {
    val _sql: String = "SELECT * FROM groups"
    return createFlow(__db, false, arrayOf("groups")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<Group> = mutableListOf()
        while (_stmt.step()) {
          val _item: Group
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = Group(_tmpId,_tmpName,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getGroupSharesFlow(groupId: Int): Flow<List<ExpenseShare>> {
    val _sql: String = "SELECT expense_shares.* FROM expense_shares INNER JOIN expenses ON expense_shares.expenseId = expenses.id WHERE expenses.groupId = ?"
    return createFlow(__db, false, arrayOf("expense_shares", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _columnIndexOfAmountOwed: Int = getColumnIndexOrThrow(_stmt, "amountOwed")
        val _result: MutableList<ExpenseShare> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseShare
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpWeight: Double
          _tmpWeight = _stmt.getDouble(_columnIndexOfWeight)
          val _tmpAmountOwed: Double
          _tmpAmountOwed = _stmt.getDouble(_columnIndexOfAmountOwed)
          _item = ExpenseShare(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpWeight,_tmpAmountOwed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMembersForGroup(groupId: Int): Flow<List<Member>> {
    val _sql: String = "SELECT * FROM members WHERE groupId = ?"
    return createFlow(__db, false, arrayOf("members")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDefaultWeight: Int = getColumnIndexOrThrow(_stmt, "defaultWeight")
        val _result: MutableList<Member> = mutableListOf()
        while (_stmt.step()) {
          val _item: Member
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpGroupId: Int
          _tmpGroupId = _stmt.getLong(_columnIndexOfGroupId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDefaultWeight: Double
          _tmpDefaultWeight = _stmt.getDouble(_columnIndexOfDefaultWeight)
          _item = Member(_tmpId,_tmpGroupId,_tmpName,_tmpDefaultWeight)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getExpensesWithPayersForGroup(groupId: Int): Flow<List<ExpenseWithPayers>> {
    val _sql: String = "SELECT * FROM expenses WHERE groupId = ?"
    return createFlow(__db, true, arrayOf("expense_payers", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfTotalAmount: Int = getColumnIndexOrThrow(_stmt, "totalAmount")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _collectionPayers: LongSparseArray<MutableList<ExpensePayer>> = LongSparseArray<MutableList<ExpensePayer>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionPayers.containsKey(_tmpKey)) {
            _collectionPayers.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipexpensePayersAscomSohamProjectMysplitsExpensePayer(_connection, _collectionPayers)
        val _result: MutableList<ExpenseWithPayers> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseWithPayers
          val _tmpExpense: Expense
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpGroupId: Int
          _tmpGroupId = _stmt.getLong(_columnIndexOfGroupId).toInt()
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpTotalAmount: Double
          _tmpTotalAmount = _stmt.getDouble(_columnIndexOfTotalAmount)
          val _tmpDate: Long
          _tmpDate = _stmt.getLong(_columnIndexOfDate)
          _tmpExpense = Expense(_tmpId,_tmpGroupId,_tmpDescription,_tmpTotalAmount,_tmpDate)
          val _tmpPayersCollection: MutableList<ExpensePayer>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpPayersCollection = checkNotNull(_collectionPayers.get(_tmpKey_1))
          _item = ExpenseWithPayers(_tmpExpense,_tmpPayersCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getExpensesForGroup(groupId: Int): Flow<List<Expense>> {
    val _sql: String = "SELECT * FROM expenses WHERE groupId = ?"
    return createFlow(__db, false, arrayOf("expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfTotalAmount: Int = getColumnIndexOrThrow(_stmt, "totalAmount")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _result: MutableList<Expense> = mutableListOf()
        while (_stmt.step()) {
          val _item: Expense
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpGroupId: Int
          _tmpGroupId = _stmt.getLong(_columnIndexOfGroupId).toInt()
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpTotalAmount: Double
          _tmpTotalAmount = _stmt.getDouble(_columnIndexOfTotalAmount)
          val _tmpDate: Long
          _tmpDate = _stmt.getLong(_columnIndexOfDate)
          _item = Expense(_tmpId,_tmpGroupId,_tmpDescription,_tmpTotalAmount,_tmpDate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllGroupShares(groupId: Int): List<ExpenseShare> {
    val _sql: String = """
        |
        |        SELECT expense_shares.* FROM expense_shares
        |        INNER JOIN expenses ON expense_shares.expenseId = expenses.id 
        |        WHERE expenses.groupId = ?
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _columnIndexOfAmountOwed: Int = getColumnIndexOrThrow(_stmt, "amountOwed")
        val _result: MutableList<ExpenseShare> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseShare
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpWeight: Double
          _tmpWeight = _stmt.getDouble(_columnIndexOfWeight)
          val _tmpAmountOwed: Double
          _tmpAmountOwed = _stmt.getDouble(_columnIndexOfAmountOwed)
          _item = ExpenseShare(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpWeight,_tmpAmountOwed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPayersForExpense(expenseId: Int): List<ExpensePayer> {
    val _sql: String = "SELECT * FROM expense_payers WHERE expenseId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, expenseId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfAmountPaid: Int = getColumnIndexOrThrow(_stmt, "amountPaid")
        val _result: MutableList<ExpensePayer> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpensePayer
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpAmountPaid: Double
          _tmpAmountPaid = _stmt.getDouble(_columnIndexOfAmountPaid)
          _item = ExpensePayer(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpAmountPaid)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllGroupPayers(groupId: Int): List<ExpensePayer> {
    val _sql: String = """
        |
        |        SELECT expense_payers.* FROM expense_payers
        |        INNER JOIN expenses ON expense_payers.expenseId = expenses.id 
        |        WHERE expenses.groupId = ?
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfAmountPaid: Int = getColumnIndexOrThrow(_stmt, "amountPaid")
        val _result: MutableList<ExpensePayer> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpensePayer
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpAmountPaid: Double
          _tmpAmountPaid = _stmt.getDouble(_columnIndexOfAmountPaid)
          _item = ExpensePayer(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpAmountPaid)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPayersForGroupFlow(groupId: Int): Flow<List<ExpensePayer>> {
    val _sql: String = "SELECT * FROM expense_payers WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = ?)"
    return createFlow(__db, false, arrayOf("expense_payers", "expenses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, groupId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfAmountPaid: Int = getColumnIndexOrThrow(_stmt, "amountPaid")
        val _result: MutableList<ExpensePayer> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpensePayer
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpAmountPaid: Double
          _tmpAmountPaid = _stmt.getDouble(_columnIndexOfAmountPaid)
          _item = ExpensePayer(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpAmountPaid)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSharesForExpenseSync(expenseId: Int): List<ExpenseShare> {
    val _sql: String = "SELECT * FROM expense_shares WHERE expenseId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, expenseId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfExpenseId: Int = getColumnIndexOrThrow(_stmt, "expenseId")
        val _columnIndexOfMemberId: Int = getColumnIndexOrThrow(_stmt, "memberId")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _columnIndexOfAmountOwed: Int = getColumnIndexOrThrow(_stmt, "amountOwed")
        val _result: MutableList<ExpenseShare> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExpenseShare
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpWeight: Double
          _tmpWeight = _stmt.getDouble(_columnIndexOfWeight)
          val _tmpAmountOwed: Double
          _tmpAmountOwed = _stmt.getDouble(_columnIndexOfAmountOwed)
          _item = ExpenseShare(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpWeight,_tmpAmountOwed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteExpense(expenseId: Int) {
    val _sql: String = "DELETE FROM expenses WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, expenseId.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipexpensePayersAscomSohamProjectMysplitsExpensePayer(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<ExpensePayer>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipexpensePayersAscomSohamProjectMysplitsExpensePayer(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`expenseId`,`memberId`,`amountPaid` FROM `expense_payers` WHERE `expenseId` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "expenseId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfExpenseId: Int = 1
      val _columnIndexOfMemberId: Int = 2
      val _columnIndexOfAmountPaid: Int = 3
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<ExpensePayer>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: ExpensePayer
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpExpenseId: Int
          _tmpExpenseId = _stmt.getLong(_columnIndexOfExpenseId).toInt()
          val _tmpMemberId: Int
          _tmpMemberId = _stmt.getLong(_columnIndexOfMemberId).toInt()
          val _tmpAmountPaid: Double
          _tmpAmountPaid = _stmt.getDouble(_columnIndexOfAmountPaid)
          _item_1 = ExpensePayer(_tmpId,_tmpExpenseId,_tmpMemberId,_tmpAmountPaid)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}

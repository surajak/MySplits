package com.SohamProject.mysplits

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _splitDao: Lazy<SplitDao> = lazy {
    SplitDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "06da9edb4f9030a63b243eab08f479d7", "025ecc92aaecd3305175e04fe2f76eb3") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `members` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `name` TEXT NOT NULL, `defaultWeight` REAL NOT NULL, FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` INTEGER NOT NULL, `description` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `date` INTEGER NOT NULL, FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `expense_shares` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `expenseId` INTEGER NOT NULL, `memberId` INTEGER NOT NULL, `weight` REAL NOT NULL, `amountOwed` REAL NOT NULL, FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `expense_payers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `expenseId` INTEGER NOT NULL, `memberId` INTEGER NOT NULL, `amountPaid` REAL NOT NULL, FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '06da9edb4f9030a63b243eab08f479d7')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `groups`")
        connection.execSQL("DROP TABLE IF EXISTS `members`")
        connection.execSQL("DROP TABLE IF EXISTS `expenses`")
        connection.execSQL("DROP TABLE IF EXISTS `expense_shares`")
        connection.execSQL("DROP TABLE IF EXISTS `expense_payers`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsGroups: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGroups.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGroups.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGroups: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGroups: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGroups: TableInfo = TableInfo("groups", _columnsGroups, _foreignKeysGroups, _indicesGroups)
        val _existingGroups: TableInfo = read(connection, "groups")
        if (!_infoGroups.equals(_existingGroups)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |groups(com.SohamProject.mysplits.Group).
              | Expected:
              |""".trimMargin() + _infoGroups + """
              |
              | Found:
              |""".trimMargin() + _existingGroups)
        }
        val _columnsMembers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMembers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMembers.put("groupId", TableInfo.Column("groupId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMembers.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMembers.put("defaultWeight", TableInfo.Column("defaultWeight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMembers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMembers.add(TableInfo.ForeignKey("groups", "CASCADE", "NO ACTION", listOf("groupId"), listOf("id")))
        val _indicesMembers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMembers: TableInfo = TableInfo("members", _columnsMembers, _foreignKeysMembers, _indicesMembers)
        val _existingMembers: TableInfo = read(connection, "members")
        if (!_infoMembers.equals(_existingMembers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |members(com.SohamProject.mysplits.Member).
              | Expected:
              |""".trimMargin() + _infoMembers + """
              |
              | Found:
              |""".trimMargin() + _existingMembers)
        }
        val _columnsExpenses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExpenses.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("groupId", TableInfo.Column("groupId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("totalAmount", TableInfo.Column("totalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenses.put("date", TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExpenses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysExpenses.add(TableInfo.ForeignKey("groups", "CASCADE", "NO ACTION", listOf("groupId"), listOf("id")))
        val _indicesExpenses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoExpenses: TableInfo = TableInfo("expenses", _columnsExpenses, _foreignKeysExpenses, _indicesExpenses)
        val _existingExpenses: TableInfo = read(connection, "expenses")
        if (!_infoExpenses.equals(_existingExpenses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |expenses(com.SohamProject.mysplits.Expense).
              | Expected:
              |""".trimMargin() + _infoExpenses + """
              |
              | Found:
              |""".trimMargin() + _existingExpenses)
        }
        val _columnsExpenseShares: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExpenseShares.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenseShares.put("expenseId", TableInfo.Column("expenseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenseShares.put("memberId", TableInfo.Column("memberId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenseShares.put("weight", TableInfo.Column("weight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpenseShares.put("amountOwed", TableInfo.Column("amountOwed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExpenseShares: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysExpenseShares.add(TableInfo.ForeignKey("expenses", "CASCADE", "NO ACTION", listOf("expenseId"), listOf("id")))
        _foreignKeysExpenseShares.add(TableInfo.ForeignKey("members", "CASCADE", "NO ACTION", listOf("memberId"), listOf("id")))
        val _indicesExpenseShares: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoExpenseShares: TableInfo = TableInfo("expense_shares", _columnsExpenseShares, _foreignKeysExpenseShares, _indicesExpenseShares)
        val _existingExpenseShares: TableInfo = read(connection, "expense_shares")
        if (!_infoExpenseShares.equals(_existingExpenseShares)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |expense_shares(com.SohamProject.mysplits.ExpenseShare).
              | Expected:
              |""".trimMargin() + _infoExpenseShares + """
              |
              | Found:
              |""".trimMargin() + _existingExpenseShares)
        }
        val _columnsExpensePayers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExpensePayers.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpensePayers.put("expenseId", TableInfo.Column("expenseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpensePayers.put("memberId", TableInfo.Column("memberId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsExpensePayers.put("amountPaid", TableInfo.Column("amountPaid", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExpensePayers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysExpensePayers.add(TableInfo.ForeignKey("expenses", "CASCADE", "NO ACTION", listOf("expenseId"), listOf("id")))
        _foreignKeysExpensePayers.add(TableInfo.ForeignKey("members", "CASCADE", "NO ACTION", listOf("memberId"), listOf("id")))
        val _indicesExpensePayers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoExpensePayers: TableInfo = TableInfo("expense_payers", _columnsExpensePayers, _foreignKeysExpensePayers, _indicesExpensePayers)
        val _existingExpensePayers: TableInfo = read(connection, "expense_payers")
        if (!_infoExpensePayers.equals(_existingExpensePayers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |expense_payers(com.SohamProject.mysplits.ExpensePayer).
              | Expected:
              |""".trimMargin() + _infoExpensePayers + """
              |
              | Found:
              |""".trimMargin() + _existingExpensePayers)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "groups", "members", "expenses", "expense_shares", "expense_payers")
  }

  public override fun clearAllTables() {
    super.performClear(true, "groups", "members", "expenses", "expense_shares", "expense_payers")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(SplitDao::class, SplitDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun splitDao(): SplitDao = _splitDao.value
}

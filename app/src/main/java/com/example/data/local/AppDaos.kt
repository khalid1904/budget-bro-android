package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.id, t.type, t.amountPaise, t.categoryId, 
               c.name as categoryName, c.iconName as categoryIcon, c.colorHex as categoryColor,
               t.title, t.merchant, t.date, t.paymentMethodId,
               p.name as paymentMethodName, t.notes, t.recurringId
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN payment_methods p ON t.paymentMethodId = p.id
        ORDER BY t.date DESC
    """)
    fun getAllTransactionDetails(): Flow<List<TransactionDetail>>

    @Query("""
        SELECT t.id, t.type, t.amountPaise, t.categoryId, 
               c.name as categoryName, c.iconName as categoryIcon, c.colorHex as categoryColor,
               t.title, t.merchant, t.date, t.paymentMethodId,
               p.name as paymentMethodName, t.notes, t.recurringId
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN payment_methods p ON t.paymentMethodId = p.id
        WHERE t.date >= :startDate AND t.date <= :endDate
        ORDER BY t.date DESC
    """)
    fun getTransactionDetailsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionDetail>>

    @Query("""
        SELECT t.id, t.type, t.amountPaise, t.categoryId, 
               c.name as categoryName, c.iconName as categoryIcon, c.colorHex as categoryColor,
               t.title, t.merchant, t.date, t.paymentMethodId,
               p.name as paymentMethodName, t.notes, t.recurringId
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN payment_methods p ON t.paymentMethodId = p.id
        ORDER BY t.date DESC
        LIMIT :limit
    """)
    fun getRecentTransactionDetails(limit: Int): Flow<List<TransactionDetail>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsByCategory(categoryId: Long): Int

    @Query("SELECT * FROM transactions")
    suspend fun getAllRawTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByNameAndType(name: String, type: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    suspend fun getAllRawCategories(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId IS NULL LIMIT 1")
    fun getOverallBudget(month: Int, year: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId IS NULL LIMIT 1")
    suspend fun getOverallBudgetDirect(month: Int, year: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetForCategory(month: Int, year: Int, categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBudgets(budgets: List<BudgetEntity>)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("SELECT * FROM budgets")
    suspend fun getAllRawBudgets(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY nextExecutionDate ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(item: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurring(item: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(item: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRawRecurring(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecurring(items: List<RecurringTransactionEntity>)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM financial_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<FinancialGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: FinancialGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: FinancialGoalEntity)

    @Query("SELECT * FROM financial_goals")
    suspend fun getAllRawGoals(): List<FinancialGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(goals: List<FinancialGoalEntity>)

    @Query("DELETE FROM financial_goals")
    suspend fun clearAll()
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY id ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPaymentMethods(methods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(method: PaymentMethodEntity)

    @Delete
    suspend fun deletePaymentMethod(method: PaymentMethodEntity)

    @Query("SELECT * FROM payment_methods")
    suspend fun getAllRawPaymentMethods(): List<PaymentMethodEntity>

    @Query("DELETE FROM payment_methods")
    suspend fun clearAll()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings")
    suspend fun clearAll()
}

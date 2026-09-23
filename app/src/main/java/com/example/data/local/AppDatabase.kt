package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        PaymentMethodEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        FinancialGoalEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun goalDao(): GoalDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_bro.db"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val paymentMethodDao = database.paymentMethodDao()
            val userSettingsDao = database.userSettingsDao()

            // 1. Default Expense Categories
            val defaultExpenseCategories = listOf(
                CategoryEntity(name = "Food", iconName = "Restaurant", type = "EXPENSE", colorHex = "#FF7043", isDefault = true),
                CategoryEntity(name = "Groceries", iconName = "ShoppingCart", type = "EXPENSE", colorHex = "#66BB6A", isDefault = true),
                CategoryEntity(name = "Transport", iconName = "DirectionsCar", type = "EXPENSE", colorHex = "#42A5F5", isDefault = true),
                CategoryEntity(name = "Shopping", iconName = "Store", type = "EXPENSE", colorHex = "#AB47BC", isDefault = true),
                CategoryEntity(name = "Bills", iconName = "Receipt", type = "EXPENSE", colorHex = "#FFA726", isDefault = true),
                CategoryEntity(name = "Entertainment", iconName = "Movie", type = "EXPENSE", colorHex = "#EC407A", isDefault = true),
                CategoryEntity(name = "Health", iconName = "FitnessCenter", type = "EXPENSE", colorHex = "#26A69A", isDefault = true),
                CategoryEntity(name = "Education", iconName = "School", type = "EXPENSE", colorHex = "#5C6BC0", isDefault = true),
                CategoryEntity(name = "Travel", iconName = "Flight", type = "EXPENSE", colorHex = "#29B6F6", isDefault = true),
                CategoryEntity(name = "Personal Care", iconName = "Face", type = "EXPENSE", colorHex = "#8D6E63", isDefault = true),
                CategoryEntity(name = "EMI", iconName = "AccountBalance", type = "EXPENSE", colorHex = "#EF5350", isDefault = true),
                CategoryEntity(name = "Rent", iconName = "Home", type = "EXPENSE", colorHex = "#7E57C2", isDefault = true),
                CategoryEntity(name = "Insurance", iconName = "Shield", type = "EXPENSE", colorHex = "#26C6DA", isDefault = true),
                CategoryEntity(name = "Family", iconName = "People", type = "EXPENSE", colorHex = "#FFCA28", isDefault = true),
                CategoryEntity(name = "Subscriptions", iconName = "Subscriptions", type = "EXPENSE", colorHex = "#78909C", isDefault = true),
                CategoryEntity(name = "Other", iconName = "MoreHoriz", type = "EXPENSE", colorHex = "#BDBDBD", isDefault = true)
            )

            // 2. Default Income Categories
            val defaultIncomeCategories = listOf(
                CategoryEntity(name = "Salary", iconName = "Work", type = "INCOME", colorHex = "#43A047", isDefault = true),
                CategoryEntity(name = "Freelance", iconName = "LaptopMac", type = "INCOME", colorHex = "#00ACC1", isDefault = true),
                CategoryEntity(name = "Business", iconName = "BusinessCenter", type = "INCOME", colorHex = "#3949AB", isDefault = true),
                CategoryEntity(name = "Investment", iconName = "TrendingUp", type = "INCOME", colorHex = "#7CB342", isDefault = true),
                CategoryEntity(name = "Bonus", iconName = "AttachMoney", type = "INCOME", colorHex = "#FDD835", isDefault = true),
                CategoryEntity(name = "Gift", iconName = "Redeem", type = "INCOME", colorHex = "#E91E63", isDefault = true),
                CategoryEntity(name = "Other", iconName = "MoreHoriz", type = "INCOME", colorHex = "#9E9E9E", isDefault = true)
            )

            categoryDao.insertAllCategories(defaultExpenseCategories + defaultIncomeCategories)

            // 3. Default Payment Methods
            val defaultPaymentMethods = listOf(
                PaymentMethodEntity(name = "UPI", type = "UPI", isDefault = true),
                PaymentMethodEntity(name = "Cash", type = "CASH", isDefault = false),
                PaymentMethodEntity(name = "Credit Card", type = "CARD", isDefault = false),
                PaymentMethodEntity(name = "Debit Card", type = "CARD", isDefault = false),
                PaymentMethodEntity(name = "Bank Transfer", type = "BANK", isDefault = false),
                PaymentMethodEntity(name = "Other", type = "OTHER", isDefault = false)
            )
            paymentMethodDao.insertAllPaymentMethods(defaultPaymentMethods)

            // 4. Default User Settings
            val defaultSettings = UserSettingsEntity(
                id = 1,
                userName = "Khalid",
                currencySymbol = "₹",
                currencyCode = "INR",
                themeMode = "SYSTEM",
                defaultMonthlyBudgetPaise = 6000000L, // ₹60,000
                warningThreshold = 0.80f,
                budgetAlertsEnabled = true,
                recurringRemindersEnabled = true,
                dailyReminderEnabled = true,
                appLockType = "NONE",
                hasCompletedOnboarding = true
            )
            userSettingsDao.insertOrUpdate(defaultSettings)
        }
    }
}

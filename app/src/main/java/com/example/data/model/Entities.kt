package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val type: String, // "EXPENSE" or "INCOME"
    val colorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payment_methods",
    indices = [Index(value = ["name"], unique = true)]
)
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // CASH, UPI, CARD, BANK, OTHER
    val isDefault: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["categoryId"]),
        Index(value = ["paymentMethodId"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amountPaise: Long, // 100 paise = ₹1.00
    val categoryId: Long,
    val title: String,
    val merchant: String? = null,
    val date: Long, // Epoch millis
    val paymentMethodId: Long,
    val notes: String? = null,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["month", "year", "categoryId"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: Int, // 1 - 12
    val year: Int,
    val categoryId: Long? = null, // null means overall monthly budget
    val amountPaise: Long,
    val warningThreshold: Float = 0.80f, // 80%
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recurring_transactions"
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amountPaise: Long,
    val title: String,
    val categoryId: Long,
    val paymentMethodId: Long,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: Long,
    val endDate: Long? = null,
    val nextExecutionDate: Long,
    val enabled: Boolean = true
)

@Entity(
    tableName = "financial_goals"
)
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmountPaise: Long,
    val currentAmountPaise: Long = 0L,
    val targetDate: Long? = null,
    val monthlyContributionPaise: Long? = null,
    val iconName: String = "Flag",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_settings"
)
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Khalid",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val defaultMonthlyBudgetPaise: Long = 6000000L, // ₹60,000
    val warningThreshold: Float = 0.80f,
    val budgetAlertsEnabled: Boolean = true,
    val recurringRemindersEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val appLockType: String = "NONE", // NONE, PIN
    val appLockPin: String? = null,
    val hasCompletedOnboarding: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TransactionDetail(
    val id: Long,
    val type: String,
    val amountPaise: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val title: String,
    val merchant: String?,
    val date: Long,
    val paymentMethodId: Long,
    val paymentMethodName: String,
    val notes: String?,
    val recurringId: Long?
)

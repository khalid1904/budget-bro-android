package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class BudgetRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringDao()
    private val goalDao = database.goalDao()
    private val paymentMethodDao = database.paymentMethodDao()
    private val userSettingsDao = database.userSettingsDao()

    // Flows
    val allTransactions: Flow<List<TransactionDetail>> = transactionDao.getAllTransactionDetails()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val expenseCategories: Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType("EXPENSE")
    val incomeCategories: Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType("INCOME")
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getAllPaymentMethods()
    val allGoals: Flow<List<FinancialGoalEntity>> = goalDao.getAllGoals()
    val allRecurring: Flow<List<RecurringTransactionEntity>> = recurringDao.getAllRecurringTransactions()
    val userSettings: Flow<UserSettingsEntity?> = userSettingsDao.getUserSettingsFlow()

    fun getTransactionsForDateRange(startDate: Long, endDate: Long): Flow<List<TransactionDetail>> {
        return transactionDao.getTransactionDetailsByDateRange(startDate, endDate)
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionDetail>> {
        return transactionDao.getRecentTransactionDetails(limit)
    }

    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }

    fun getOverallBudget(month: Int, year: Int): Flow<BudgetEntity?> {
        return budgetDao.getOverallBudget(month, year)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionById(id)
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            val id = transactionDao.insertTransaction(transaction)

            // Check budget alert if this is an expense
            if (transaction.type == "EXPENSE") {
                checkAndTriggerBudgetAlert(transaction)
            }

            id
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
            if (transaction.type == "EXPENSE") {
                checkAndTriggerBudgetAlert(transaction)
            }
        }
    }

    suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
        }
    }

    suspend fun canDeleteCategory(categoryId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            transactionDao.countTransactionsByCategory(categoryId) == 0
        }
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return withContext(Dispatchers.IO) {
            categoryDao.insertCategory(category)
        }
    }

    suspend fun updateCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.updateCategory(category)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    }

    suspend fun setBudget(budget: BudgetEntity): Long {
        return withContext(Dispatchers.IO) {
            val id = budgetDao.insertOrUpdateBudget(budget)
            checkOverallMonthlyBudget(budget.month, budget.year)
            id
        }
    }

    suspend fun deleteBudget(id: Long) {
        withContext(Dispatchers.IO) {
            budgetDao.deleteBudgetById(id)
        }
    }

    suspend fun insertGoal(goal: FinancialGoalEntity): Long {
        return withContext(Dispatchers.IO) {
            goalDao.insertGoal(goal)
        }
    }

    suspend fun updateGoal(goal: FinancialGoalEntity) {
        withContext(Dispatchers.IO) {
            goalDao.updateGoal(goal)
        }
    }

    suspend fun deleteGoal(goal: FinancialGoalEntity) {
        withContext(Dispatchers.IO) {
            goalDao.deleteGoal(goal)
        }
    }

    suspend fun addMoneyToGoal(goalId: Long, addAmountPaise: Long) {
        withContext(Dispatchers.IO) {
            val goals = goalDao.getAllRawGoals()
            val target = goals.find { it.id == goalId }
            if (target != null) {
                val updated = target.copy(
                    currentAmountPaise = target.currentAmountPaise + addAmountPaise,
                    updatedAt = System.currentTimeMillis()
                )
                goalDao.updateGoal(updated)
            }
        }
    }

    suspend fun insertRecurring(item: RecurringTransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            recurringDao.insertRecurring(item)
        }
    }

    suspend fun updateRecurring(item: RecurringTransactionEntity) {
        withContext(Dispatchers.IO) {
            recurringDao.updateRecurring(item)
        }
    }

    suspend fun deleteRecurring(item: RecurringTransactionEntity) {
        withContext(Dispatchers.IO) {
            recurringDao.deleteRecurring(item)
        }
    }

    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long {
        return withContext(Dispatchers.IO) {
            paymentMethodDao.insertPaymentMethod(method)
        }
    }

    suspend fun updatePaymentMethod(method: PaymentMethodEntity) {
        withContext(Dispatchers.IO) {
            paymentMethodDao.updatePaymentMethod(method)
        }
    }

    suspend fun deletePaymentMethod(method: PaymentMethodEntity) {
        withContext(Dispatchers.IO) {
            paymentMethodDao.deletePaymentMethod(method)
        }
    }

    suspend fun updateUserSettings(settings: UserSettingsEntity) {
        withContext(Dispatchers.IO) {
            userSettingsDao.insertOrUpdate(settings)
        }
    }

    suspend fun getUserSettings(): UserSettingsEntity {
        return withContext(Dispatchers.IO) {
            userSettingsDao.getUserSettings() ?: UserSettingsEntity()
        }
    }

    suspend fun checkOverallMonthlyBudget(month: Int, year: Int) {
        try {
            val settings = userSettingsDao.getUserSettings() ?: UserSettingsEntity()
            if (!settings.budgetAlertsEnabled) return

            val (start, end) = DateUtils.getMonthStartAndEndTimestamps(month, year)
            val monthTransactions = transactionDao.getTransactionDetailsByDateRange(start, end).first()

            val totalExpensePaise = monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountPaise }
            val overallBudget = budgetDao.getOverallBudgetDirect(month, year)
            val monthlyBudgetPaise = overallBudget?.amountPaise ?: settings.defaultMonthlyBudgetPaise
            val threshold = overallBudget?.warningThreshold ?: settings.warningThreshold // default 0.80f (80%)

            if (monthlyBudgetPaise > 0) {
                val ratio = totalExpensePaise.toFloat() / monthlyBudgetPaise.toFloat()
                val monthTitle = DateUtils.getMonthYearTitle(month, year)
                val lastAlertState = NotificationHelper.getAlertState(context, month, year)

                if (totalExpensePaise > monthlyBudgetPaise) {
                    if (lastAlertState != NotificationHelper.ALERT_STATE_EXCEEDED_100) {
                        val pct = (ratio * 100).toInt()
                        NotificationHelper.showMonthlyBudgetAlert(
                            context = context,
                            spentPaise = totalExpensePaise,
                            budgetPaise = monthlyBudgetPaise,
                            currencySymbol = settings.currencySymbol,
                            monthName = monthTitle,
                            percentageUsed = pct
                        )
                        NotificationHelper.setAlertState(context, month, year, NotificationHelper.ALERT_STATE_EXCEEDED_100)
                    }
                } else if (ratio >= threshold) {
                    if (lastAlertState == NotificationHelper.ALERT_STATE_NONE) {
                        val pct = (ratio * 100).toInt()
                        NotificationHelper.showMonthlyBudgetAlert(
                            context = context,
                            spentPaise = totalExpensePaise,
                            budgetPaise = monthlyBudgetPaise,
                            currencySymbol = settings.currencySymbol,
                            monthName = monthTitle,
                            percentageUsed = pct
                        )
                        NotificationHelper.setAlertState(context, month, year, NotificationHelper.ALERT_STATE_WARNING_80)
                    }
                } else {
                    if (lastAlertState != NotificationHelper.ALERT_STATE_NONE) {
                        NotificationHelper.setAlertState(context, month, year, NotificationHelper.ALERT_STATE_NONE)
                    }
                }
            }
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    private suspend fun checkAndTriggerBudgetAlert(expense: TransactionEntity) {
        try {
            val settings = userSettingsDao.getUserSettings() ?: UserSettingsEntity()
            if (!settings.budgetAlertsEnabled) return

            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)

            // Check overall monthly budget (80% threshold or exceeded)
            checkOverallMonthlyBudget(month, year)

            // Category budget alert
            val (start, end) = DateUtils.getMonthStartAndEndTimestamps(month, year)
            val monthTransactions = transactionDao.getTransactionDetailsByDateRange(start, end).first()
            val categoryBudget = budgetDao.getBudgetForCategory(month, year, expense.categoryId)
            val category = categoryDao.getCategoryById(expense.categoryId)

            if (categoryBudget != null && category != null && categoryBudget.amountPaise > 0) {
                val catSpent = monthTransactions.filter { it.type == "EXPENSE" && it.categoryId == expense.categoryId }.sumOf { it.amountPaise }
                val ratio = catSpent.toFloat() / categoryBudget.amountPaise.toFloat()

                if (catSpent > categoryBudget.amountPaise) {
                    val exceededPaise = catSpent - categoryBudget.amountPaise
                    NotificationHelper.showBudgetAlert(
                        context,
                        "Budget Exceeded: ${category.name}",
                        "You've exceeded your ${category.name} budget by ${CurrencyFormatter.formatPaise(exceededPaise, settings.currencySymbol)}.",
                        (2000 + category.id).toInt()
                    )
                } else if (ratio >= categoryBudget.warningThreshold) {
                    val percent = (ratio * 100).toInt()
                    NotificationHelper.showBudgetAlert(
                        context,
                        "Budget Alert: ${category.name}",
                        "You've used $percent% of your ${category.name} budget.",
                        (2000 + category.id).toInt()
                    )
                }
            }
        } catch (e: Exception) {
            // Non-blocking for alert
        }
    }

    // Demo Data
    suspend fun loadDemoData(month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, year: Int = Calendar.getInstance().get(Calendar.YEAR)) {
        withContext(Dispatchers.IO) {
            val catList = categoryDao.getAllRawCategories()
            val pmList = paymentMethodDao.getAllRawPaymentMethods()

            val salaryCat = catList.find { it.name == "Salary" && it.type == "INCOME" }?.id ?: 1L
            val rentCat = catList.find { it.name == "Rent" && it.type == "EXPENSE" }?.id ?: 12L
            val groceriesCat = catList.find { it.name == "Groceries" && it.type == "EXPENSE" }?.id ?: 2L
            val foodCat = catList.find { it.name == "Food" && it.type == "EXPENSE" }?.id ?: 1L
            val transportCat = catList.find { it.name == "Transport" && it.type == "EXPENSE" }?.id ?: 3L
            val shoppingCat = catList.find { it.name == "Shopping" && it.type == "EXPENSE" }?.id ?: 4L
            val subscriptionsCat = catList.find { it.name == "Subscriptions" && it.type == "EXPENSE" }?.id ?: 15L
            val billsCat = catList.find { it.name == "Bills" && it.type == "EXPENSE" }?.id ?: 5L

            val upiId = pmList.find { it.name == "UPI" }?.id ?: 1L
            val bankId = pmList.find { it.name == "Bank Transfer" }?.id ?: 5L
            val cardId = pmList.find { it.name == "Credit Card" }?.id ?: 3L

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)

            val demoTransactions = mutableListOf<TransactionEntity>()

            // 1. Income: Salary ₹1,00,000 (10000000 paise) on 1st of month
            cal.set(Calendar.DAY_OF_MONTH, 1)
            demoTransactions.add(
                TransactionEntity(
                    type = "INCOME",
                    amountPaise = 10000000L,
                    categoryId = salaryCat,
                    title = "Monthly Salary",
                    merchant = "Acme Corp Tech",
                    date = cal.timeInMillis,
                    paymentMethodId = bankId,
                    notes = "Monthly payroll credit"
                )
            )

            // 2. Rent ₹20,000 (2000000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 2)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 2000000L,
                    categoryId = rentCat,
                    title = "Apartment Rent",
                    merchant = "Landlord",
                    date = cal.timeInMillis,
                    paymentMethodId = bankId,
                    notes = "September apartment rent"
                )
            )

            // 3. Groceries ₹8,500 (850000 paise) across multiple visits
            cal.set(Calendar.DAY_OF_MONTH, 4)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 450000L,
                    categoryId = groceriesCat,
                    title = "Supermarket Grocery",
                    merchant = "DMart",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId,
                    notes = "Monthly essentials and staples"
                )
            )
            cal.set(Calendar.DAY_OF_MONTH, 14)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 400000L,
                    categoryId = groceriesCat,
                    title = "Organic Veggies & Fruits",
                    merchant = "Blinkit",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId,
                    notes = "Fresh produce delivery"
                )
            )

            // 4. Food ₹5,200 (520000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 7)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 180000L,
                    categoryId = foodCat,
                    title = "Dinner with Friends",
                    merchant = "Barbeque Nation",
                    date = cal.timeInMillis,
                    paymentMethodId = cardId,
                    notes = "Weekend get-together"
                )
            )
            cal.set(Calendar.DAY_OF_MONTH, 12)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 140000L,
                    categoryId = foodCat,
                    title = "Lunch at Restaurant",
                    merchant = "Saravana Bhavan",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId,
                    notes = "Family lunch"
                )
            )
            cal.set(Calendar.DAY_OF_MONTH, 18)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 200000L,
                    categoryId = foodCat,
                    title = "Food Delivery",
                    merchant = "Swiggy",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId,
                    notes = "Evening meal"
                )
            )

            // 5. Transport ₹3,000 (300000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 9)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 150000L,
                    categoryId = transportCat,
                    title = "Metro Pass & Cabs",
                    merchant = "Uber",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId
                )
            )
            cal.set(Calendar.DAY_OF_MONTH, 16)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 150000L,
                    categoryId = transportCat,
                    title = "Fuel Petrol",
                    merchant = "Indian Oil",
                    date = cal.timeInMillis,
                    paymentMethodId = cardId
                )
            )

            // 6. Shopping ₹6,500 (650000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 10)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 650000L,
                    categoryId = shoppingCat,
                    title = "New Casual Shoes",
                    merchant = "Amazon",
                    date = cal.timeInMillis,
                    paymentMethodId = cardId,
                    notes = "Festival sale discount"
                )
            )

            // 7. Subscriptions ₹1,200 (120000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 5)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 120000L,
                    categoryId = subscriptionsCat,
                    title = "Streaming & Cloud",
                    merchant = "Netflix & Spotify",
                    date = cal.timeInMillis,
                    paymentMethodId = cardId
                )
            )

            // 8. Bills ₹4,500 (450000 paise)
            cal.set(Calendar.DAY_OF_MONTH, 6)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 300000L,
                    categoryId = billsCat,
                    title = "Electricity Bill",
                    merchant = "BESCOM",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId
                )
            )
            cal.set(Calendar.DAY_OF_MONTH, 8)
            demoTransactions.add(
                TransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 150000L,
                    categoryId = billsCat,
                    title = "Broadband Internet",
                    merchant = "Airtel Fiber",
                    date = cal.timeInMillis,
                    paymentMethodId = upiId
                )
            )

            transactionDao.insertAllTransactions(demoTransactions)

            // Seed Budgets for current month: Overall ₹60,000 + categories
            val demoBudgets = listOf(
                BudgetEntity(month = month, year = year, categoryId = null, amountPaise = 6000000L), // ₹60,000 overall
                BudgetEntity(month = month, year = year, categoryId = rentCat, amountPaise = 2000000L), // ₹20,000 rent
                BudgetEntity(month = month, year = year, categoryId = foodCat, amountPaise = 1000000L), // ₹10,000 food
                BudgetEntity(month = month, year = year, categoryId = groceriesCat, amountPaise = 800000L), // ₹8,000 groceries
                BudgetEntity(month = month, year = year, categoryId = transportCat, amountPaise = 500000L), // ₹5,000 transport
                BudgetEntity(month = month, year = year, categoryId = shoppingCat, amountPaise = 700000L), // ₹7,000 shopping
                BudgetEntity(month = month, year = year, categoryId = billsCat, amountPaise = 600000L) // ₹6,000 bills
            )
            budgetDao.insertAllBudgets(demoBudgets)

            // Seed Sample Goals
            val demoGoals = listOf(
                FinancialGoalEntity(
                    name = "Emergency Fund",
                    targetAmountPaise = 30000000L, // ₹3,00,000
                    currentAmountPaise = 15000000L, // ₹1,50,000 (50%)
                    targetDate = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000),
                    monthlyContributionPaise = 2500000L,
                    iconName = "Shield"
                ),
                FinancialGoalEntity(
                    name = "New Phone",
                    targetAmountPaise = 8000000L, // ₹80,000
                    currentAmountPaise = 4800000L, // ₹48,000 (60%)
                    targetDate = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000),
                    monthlyContributionPaise = 1600000L,
                    iconName = "PhoneIphone"
                ),
                FinancialGoalEntity(
                    name = "Vacation Trip",
                    targetAmountPaise = 5000000L, // ₹50,000
                    currentAmountPaise = 2000000L, // ₹20,000 (40%)
                    targetDate = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
                    monthlyContributionPaise = 1000000L,
                    iconName = "Flight"
                )
            )
            goalDao.insertAllGoals(demoGoals)

            // Seed Sample Recurring Transactions
            val demoRecurring = listOf(
                RecurringTransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 2000000L,
                    title = "Apartment Rent",
                    categoryId = rentCat,
                    paymentMethodId = bankId,
                    frequency = "MONTHLY",
                    startDate = System.currentTimeMillis(),
                    nextExecutionDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                    enabled = true
                ),
                RecurringTransactionEntity(
                    type = "EXPENSE",
                    amountPaise = 120000L,
                    title = "Netflix & Streaming",
                    categoryId = subscriptionsCat,
                    paymentMethodId = cardId,
                    frequency = "MONTHLY",
                    startDate = System.currentTimeMillis(),
                    nextExecutionDate = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000),
                    enabled = true
                ),
                RecurringTransactionEntity(
                    type = "INCOME",
                    amountPaise = 10000000L,
                    title = "Monthly Salary",
                    categoryId = salaryCat,
                    paymentMethodId = bankId,
                    frequency = "MONTHLY",
                    startDate = System.currentTimeMillis(),
                    nextExecutionDate = System.currentTimeMillis() + (28L * 24 * 60 * 60 * 1000),
                    enabled = true
                )
            )
            recurringDao.insertAllRecurring(demoRecurring)
        }
    }

    // JSON Export
    suspend fun exportDataToJson(): String {
        return withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("version", 1)
            root.put("appName", "Budget Bro")
            root.put("exportedAt", System.currentTimeMillis())

            val transactions = transactionDao.getAllRawTransactions()
            val txArray = JSONArray()
            for (t in transactions) {
                val o = JSONObject()
                o.put("id", t.id)
                o.put("type", t.type)
                o.put("amountPaise", t.amountPaise)
                o.put("categoryId", t.categoryId)
                o.put("title", t.title)
                o.put("merchant", t.merchant ?: "")
                o.put("date", t.date)
                o.put("paymentMethodId", t.paymentMethodId)
                o.put("notes", t.notes ?: "")
                o.put("recurringId", t.recurringId ?: 0L)
                txArray.put(o)
            }
            root.put("transactions", txArray)

            val budgets = budgetDao.getAllRawBudgets()
            val bArray = JSONArray()
            for (b in budgets) {
                val o = JSONObject()
                o.put("id", b.id)
                o.put("month", b.month)
                o.put("year", b.year)
                if (b.categoryId != null) o.put("categoryId", b.categoryId)
                o.put("amountPaise", b.amountPaise)
                o.put("warningThreshold", b.warningThreshold)
                bArray.put(o)
            }
            root.put("budgets", bArray)

            val goals = goalDao.getAllRawGoals()
            val gArray = JSONArray()
            for (g in goals) {
                val o = JSONObject()
                o.put("id", g.id)
                o.put("name", g.name)
                o.put("targetAmountPaise", g.targetAmountPaise)
                o.put("currentAmountPaise", g.currentAmountPaise)
                if (g.targetDate != null) o.put("targetDate", g.targetDate)
                o.put("iconName", g.iconName)
                gArray.put(o)
            }
            root.put("goals", gArray)

            val settings = userSettingsDao.getUserSettings()
            if (settings != null) {
                val sObj = JSONObject()
                sObj.put("userName", settings.userName)
                sObj.put("currencySymbol", settings.currencySymbol)
                sObj.put("currencyCode", settings.currencyCode)
                sObj.put("themeMode", settings.themeMode)
                sObj.put("defaultMonthlyBudgetPaise", settings.defaultMonthlyBudgetPaise)
                sObj.put("warningThreshold", settings.warningThreshold)
                root.put("settings", sObj)
            }

            root.toString(2)
        }
    }

    // JSON Import
    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonStr)
                if (!root.has("transactions")) return@withContext false

                val txArray = root.getJSONArray("transactions")
                val txList = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            id = o.optLong("id", 0L),
                            type = o.getString("type"),
                            amountPaise = o.getLong("amountPaise"),
                            categoryId = o.getLong("categoryId"),
                            title = o.getString("title"),
                            merchant = o.optString("merchant").takeIf { it.isNotEmpty() },
                            date = o.getLong("date"),
                            paymentMethodId = o.getLong("paymentMethodId"),
                            notes = o.optString("notes").takeIf { it.isNotEmpty() },
                            recurringId = if (o.has("recurringId") && o.getLong("recurringId") > 0) o.getLong("recurringId") else null
                        )
                    )
                }

                if (txList.isNotEmpty()) {
                    transactionDao.insertAllTransactions(txList)
                }

                if (root.has("budgets")) {
                    val bArray = root.getJSONArray("budgets")
                    val bList = mutableListOf<BudgetEntity>()
                    for (i in 0 until bArray.length()) {
                        val o = bArray.getJSONObject(i)
                        bList.add(
                            BudgetEntity(
                                id = o.optLong("id", 0L),
                                month = o.getInt("month"),
                                year = o.getInt("year"),
                                categoryId = if (o.has("categoryId")) o.getLong("categoryId") else null,
                                amountPaise = o.getLong("amountPaise"),
                                warningThreshold = o.optDouble("warningThreshold", 0.8).toFloat()
                            )
                        )
                    }
                    if (bList.isNotEmpty()) {
                        budgetDao.insertAllBudgets(bList)
                    }
                }

                if (root.has("goals")) {
                    val gArray = root.getJSONArray("goals")
                    val gList = mutableListOf<FinancialGoalEntity>()
                    for (i in 0 until gArray.length()) {
                        val o = gArray.getJSONObject(i)
                        gList.add(
                            FinancialGoalEntity(
                                id = o.optLong("id", 0L),
                                name = o.getString("name"),
                                targetAmountPaise = o.getLong("targetAmountPaise"),
                                currentAmountPaise = o.optLong("currentAmountPaise", 0L),
                                targetDate = if (o.has("targetDate")) o.getLong("targetDate") else null,
                                iconName = o.optString("iconName", "Flag")
                            )
                        )
                    }
                    if (gList.isNotEmpty()) {
                        goalDao.insertAllGoals(gList)
                    }
                }

                if (root.has("settings")) {
                    val sObj = root.getJSONObject("settings")
                    val cur = userSettingsDao.getUserSettings() ?: UserSettingsEntity()
                    val updated = cur.copy(
                        userName = sObj.optString("userName", cur.userName),
                        currencySymbol = sObj.optString("currencySymbol", cur.currencySymbol),
                        currencyCode = sObj.optString("currencyCode", cur.currencyCode),
                        themeMode = sObj.optString("themeMode", cur.themeMode),
                        defaultMonthlyBudgetPaise = sObj.optLong("defaultMonthlyBudgetPaise", cur.defaultMonthlyBudgetPaise)
                    )
                    userSettingsDao.insertOrUpdate(updated)
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            transactionDao.clearAll()
            budgetDao.clearAll()
            goalDao.clearAll()
            recurringDao.clearAll()
            // Keep default categories, payment methods and user settings
            val settings = userSettingsDao.getUserSettings() ?: UserSettingsEntity()
            userSettingsDao.insertOrUpdate(settings.copy(hasCompletedOnboarding = true))
        }
    }
}

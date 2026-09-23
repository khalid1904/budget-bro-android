package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.ui.components.CategorySpendItem
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.TrendBarData
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TransactionFilterType { ALL, EXPENSE, INCOME }
enum class TransactionSortOrder { NEWEST, OLDEST, HIGHEST, LOWEST }
enum class AnalyticsTimeRange { THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, THIS_YEAR }

data class TransactionFilterState(
    val query: String = "",
    val type: TransactionFilterType = TransactionFilterType.ALL,
    val categoryId: Long? = null,
    val paymentMethodId: Long? = null,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST
)

data class CategoryBudgetUiModel(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val budgetAmountPaise: Long,
    val spentAmountPaise: Long,
    val remainingAmountPaise: Long,
    val percentageUsed: Int,
    val warningThreshold: Float
)

data class MonthlyComparisonUiModel(
    val currentMonthExpensePaise: Long,
    val previousMonthExpensePaise: Long,
    val differencePaise: Long, // positive means spent more
    val percentageChange: Float,
    val currentMonthTitle: String,
    val previousMonthTitle: String
)

data class SmartInsight(
    val iconName: String,
    val message: String
)

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    // Current selected month & year (1-based month)
    private val currentCal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(currentCal.get(Calendar.MONTH) + 1)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentCal.get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()

    // Base flows from repository
    val userSettings: StateFlow<UserSettingsEntity> = repository.userSettings
        .map { it ?: UserSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettingsEntity())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.expenseCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = repository.incomeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentMethods: StateFlow<List<PaymentMethodEntity>> = repository.allPaymentMethods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionDetail>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<FinancialGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurring: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month navigation
    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun setSelectedMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    // Selected Month Transactions
    val currentMonthTransactions: StateFlow<List<TransactionDetail>> = combine(
        allTransactions,
        selectedMonth,
        selectedYear
    ) { transactions, month, year ->
        val (start, end) = DateUtils.getMonthStartAndEndTimestamps(month, year)
        transactions.filter { it.date in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Balance (Income - Expense of all time)
    val totalBalancePaise: StateFlow<Long> = allTransactions.map { list ->
        val totalIncome = list.filter { it.type == "INCOME" }.sumOf { it.amountPaise }
        val totalExpense = list.filter { it.type == "EXPENSE" }.sumOf { it.amountPaise }
        totalIncome - totalExpense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Current Month Income & Expense
    val monthIncomePaise: StateFlow<Long> = currentMonthTransactions.map { list ->
        list.filter { it.type == "INCOME" }.sumOf { it.amountPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthExpensePaise: StateFlow<Long> = currentMonthTransactions.map { list ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amountPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Current Month Budgets from DB
    val currentMonthBudgets: StateFlow<List<BudgetEntity>> = combine(
        selectedMonth,
        selectedYear
    ) { month, year ->
        Pair(month, year)
    }.flatMapLatest { (m, y) ->
        repository.getBudgetsForMonth(m, y)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Month Budget Amount (falls back to default in settings if not configured)
    val overallMonthBudgetPaise: StateFlow<Long> = combine(
        currentMonthBudgets,
        userSettings
    ) { budgets, settings ->
        val overall = budgets.find { it.categoryId == null }
        overall?.amountPaise ?: settings.defaultMonthlyBudgetPaise
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6000000L)

    // Overall Month Budget Warning Threshold (percentage 0.5f - 0.95f)
    val overallMonthBudgetThreshold: StateFlow<Float> = combine(
        currentMonthBudgets,
        userSettings
    ) { budgets, settings ->
        val overall = budgets.find { it.categoryId == null }
        overall?.warningThreshold ?: settings.warningThreshold
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.80f)

    // Category Budgets UI Models
    val categoryBudgetsUi: StateFlow<List<CategoryBudgetUiModel>> = combine(
        currentMonthBudgets,
        currentMonthTransactions,
        allCategories
    ) { budgets, transactions, categories ->
        val catBudgets = budgets.filter { it.categoryId != null }
        val expenses = transactions.filter { it.type == "EXPENSE" }

        catBudgets.mapNotNull { b ->
            val cat = categories.find { it.id == b.categoryId } ?: return@mapNotNull null
            val spent = expenses.filter { it.categoryId == b.categoryId }.sumOf { it.amountPaise }
            val remaining = b.amountPaise - spent
            val percentage = if (b.amountPaise > 0) ((spent.toFloat() / b.amountPaise.toFloat()) * 100).roundToInt() else 0
            CategoryBudgetUiModel(
                budgetId = b.id,
                categoryId = cat.id,
                categoryName = cat.name,
                categoryIcon = cat.iconName,
                categoryColor = cat.colorHex,
                budgetAmountPaise = b.amountPaise,
                spentAmountPaise = spent,
                remainingAmountPaise = remaining,
                percentageUsed = percentage,
                warningThreshold = b.warningThreshold
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Insights (Rule-based)
    val smartInsights: StateFlow<List<SmartInsight>> = combine(
        currentMonthTransactions,
        overallMonthBudgetPaise,
        userSettings,
        allTransactions
    ) { currentMonthTx, overallBudget, settings, allTx ->
        val month = _selectedMonth.value
        val year = _selectedYear.value
        val insights = mutableListOf<SmartInsight>()
        val expenses = currentMonthTx.filter { it.type == "EXPENSE" }
        val totalSpent = expenses.sumOf { it.amountPaise }

        // Remaining Budget Insight
        val remainingBudget = overallBudget - totalSpent
        if (remainingBudget >= 0) {
            insights.add(
                SmartInsight(
                    "CheckCircle",
                    "You have ${CurrencyFormatter.formatPaise(remainingBudget, settings.currencySymbol)} remaining for ${DateUtils.getMonthYearTitle(month, year)}."
                )
            )
        } else {
            insights.add(
                SmartInsight(
                    "Warning",
                    "You have exceeded your ${DateUtils.getMonthYearTitle(month, year)} budget by ${CurrencyFormatter.formatPaise(-remainingBudget, settings.currencySymbol)}."
                )
            )
        }

        // Daily average
        val daysElapsed = DateUtils.getDaysElapsedInMonth(month, year)
        val dailyAvgPaise = if (daysElapsed > 0) totalSpent / daysElapsed else 0L
        if (dailyAvgPaise > 0) {
            insights.add(
                SmartInsight(
                    "TrendingUp",
                    "Your average daily spending is ${CurrencyFormatter.formatPaise(dailyAvgPaise, settings.currencySymbol)}."
                )
            )
        }

        // Top Category share
        val catSpendMap = expenses.groupBy { it.categoryName }.mapValues { entry -> entry.value.sumOf { it.amountPaise } }
        val topEntry = catSpendMap.maxByOrNull { it.value }
        if (topEntry != null && totalSpent > 0) {
            val share = ((topEntry.value.toFloat() / totalSpent.toFloat()) * 100).roundToInt()
            insights.add(
                SmartInsight(
                    "PieChart",
                    "${topEntry.key} accounts for $share% of your monthly expenses."
                )
            )
        }

        // Month-over-Month comparison
        val prevMonth = if (month == 1) 12 else month - 1
        val prevYear = if (month == 1) year - 1 else year
        val (pStart, pEnd) = DateUtils.getMonthStartAndEndTimestamps(prevMonth, prevYear)
        val prevMonthExpenses = allTx.filter { it.date in pStart..pEnd && it.type == "EXPENSE" }.sumOf { it.amountPaise }
        if (prevMonthExpenses > 0 && totalSpent > 0) {
            val diff = totalSpent - prevMonthExpenses
            val diffPct = ((abs(diff).toFloat() / prevMonthExpenses.toFloat()) * 100).roundToInt()
            if (diff > 0) {
                insights.add(
                    SmartInsight(
                        "ArrowUpward",
                        "You spent $diffPct% more this month compared to last month."
                    )
                )
            } else if (diff < 0) {
                insights.add(
                    SmartInsight(
                        "ArrowDownward",
                        "Great job! You spent $diffPct% less this month compared to last month."
                    )
                )
            }
        }

        insights
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // TRANSACTIONS SCREEN STATE (Search, Filter, Sort, Grouping)
    // ----------------------------------------------------
    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState = _filterState.asStateFlow()

    val searchQuery: StateFlow<String> = _filterState.map { it.query }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val typeFilter: StateFlow<TransactionFilterType> = _filterState.map { it.type }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionFilterType.ALL)

    val selectedCategoryIdFilter: StateFlow<Long?> = _filterState.map { it.categoryId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPaymentMethodIdFilter: StateFlow<Long?> = _filterState.map { it.paymentMethodId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sortOrder: StateFlow<TransactionSortOrder> = _filterState.map { it.sortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionSortOrder.NEWEST)

    fun setSearchQuery(query: String) { _filterState.update { it.copy(query = query) } }
    fun setTypeFilter(type: TransactionFilterType) { _filterState.update { it.copy(type = type) } }
    fun setCategoryFilter(catId: Long?) { _filterState.update { it.copy(categoryId = catId) } }
    fun setPaymentMethodFilter(pmId: Long?) { _filterState.update { it.copy(paymentMethodId = pmId) } }
    fun setSortOrder(order: TransactionSortOrder) { _filterState.update { it.copy(sortOrder = order) } }

    // Filtered & Grouped Transactions
    val filteredTransactionsGrouped: StateFlow<Map<String, List<TransactionDetail>>> = combine(
        allTransactions,
        _filterState
    ) { list, filter ->
        var filtered = list

        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                (it.merchant?.lowercase()?.contains(q) == true) ||
                it.categoryName.lowercase().contains(q) ||
                (it.notes?.lowercase()?.contains(q) == true)
            }
        }

        if (filter.type != TransactionFilterType.ALL) {
            val tName = filter.type.name
            filtered = filtered.filter { it.type == tName }
        }

        if (filter.categoryId != null) {
            filtered = filtered.filter { it.categoryId == filter.categoryId }
        }

        if (filter.paymentMethodId != null) {
            filtered = filtered.filter { it.paymentMethodId == filter.paymentMethodId }
        }

        filtered = when (filter.sortOrder) {
            TransactionSortOrder.NEWEST -> filtered.sortedByDescending { it.date }
            TransactionSortOrder.OLDEST -> filtered.sortedBy { it.date }
            TransactionSortOrder.HIGHEST -> filtered.sortedByDescending { it.amountPaise }
            TransactionSortOrder.LOWEST -> filtered.sortedBy { it.amountPaise }
        }

        // Group by Date for UI display (e.g. "TODAY", "YESTERDAY", "18 SEPTEMBER 2026")
        filtered.groupBy { DateUtils.formatDateForHeader(it.date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ----------------------------------------------------
    // ANALYTICS SCREEN STATE
    // ----------------------------------------------------
    private val _analyticsRange = MutableStateFlow(AnalyticsTimeRange.THIS_MONTH)
    val analyticsRange = _analyticsRange.asStateFlow()

    private val _trendChartType = MutableStateFlow("EXPENSE") // EXPENSE, INCOME, NET
    val trendChartType = _trendChartType.asStateFlow()

    fun setAnalyticsRange(range: AnalyticsTimeRange) { _analyticsRange.value = range }
    fun setTrendChartType(type: String) { _trendChartType.value = type }

    // Transactions in Analytics Range
    val analyticsTransactions: StateFlow<List<TransactionDetail>> = combine(
        allTransactions,
        analyticsRange
    ) { list, range ->
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val startTime = when (range) {
            AnalyticsTimeRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            AnalyticsTimeRange.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            AnalyticsTimeRange.LAST_3_MONTHS -> {
                cal.add(Calendar.MONTH, -3)
                cal.timeInMillis
            }
            AnalyticsTimeRange.LAST_6_MONTHS -> {
                cal.add(Calendar.MONTH, -6)
                cal.timeInMillis
            }
            AnalyticsTimeRange.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
        }
        val endTime = when (range) {
            AnalyticsTimeRange.LAST_MONTH -> {
                val c = Calendar.getInstance()
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.add(Calendar.DAY_OF_YEAR, -1)
                c.timeInMillis
            }
            else -> now
        }
        list.filter { it.date in startTime..endTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expense Breakdown for Donut Chart
    val expenseBreakdown: StateFlow<Pair<Long, List<CategorySpendItem>>> = analyticsTransactions.map { txList ->
        val expenses = txList.filter { it.type == "EXPENSE" }
        val total = expenses.sumOf { it.amountPaise }
        if (total == 0L) return@map Pair(0L, emptyList())

        val grouped = expenses.groupBy { it.categoryName }
            .mapValues { entry ->
                val amount = entry.value.sumOf { it.amountPaise }
                val color = CategoryIconHelper.parseColor(entry.value.first().categoryColor)
                val pct = (amount.toFloat() / total.toFloat()) * 100f
                CategorySpendItem(
                    categoryName = entry.key,
                    color = color,
                    amountPaise = amount,
                    percentage = pct
                )
            }.values.sortedByDescending { it.amountPaise }

        Pair(total, grouped)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0L, emptyList()))

    // Top 5 Categories
    val topSpendingCategories: StateFlow<List<CategorySpendItem>> = expenseBreakdown.map { it.second.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Comparison (Current month vs previous month)
    val monthlyComparison: StateFlow<MonthlyComparisonUiModel> = combine(
        allTransactions,
        selectedMonth,
        selectedYear
    ) { txList, curMonth, curYear ->
        val (curStart, curEnd) = DateUtils.getMonthStartAndEndTimestamps(curMonth, curYear)
        val curExpense = txList.filter { it.date in curStart..curEnd && it.type == "EXPENSE" }.sumOf { it.amountPaise }

        val prevMonth = if (curMonth == 1) 12 else curMonth - 1
        val prevYear = if (curMonth == 1) curYear - 1 else curYear
        val (prevStart, prevEnd) = DateUtils.getMonthStartAndEndTimestamps(prevMonth, prevYear)
        val prevExpense = txList.filter { it.date in prevStart..prevEnd && it.type == "EXPENSE" }.sumOf { it.amountPaise }

        val diff = curExpense - prevExpense
        val pct = if (prevExpense > 0) ((diff.toFloat() / prevExpense.toFloat()) * 100) else 0f

        MonthlyComparisonUiModel(
            currentMonthExpensePaise = curExpense,
            previousMonthExpensePaise = prevExpense,
            differencePaise = diff,
            percentageChange = pct,
            currentMonthTitle = DateUtils.getMonthYearTitle(curMonth, curYear),
            previousMonthTitle = DateUtils.getMonthYearTitle(prevMonth, prevYear)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyComparisonUiModel(0L, 0L, 0L, 0f, "", "")
    )

    // Trend Bar Chart Data
    val spendingTrends: StateFlow<List<TrendBarData>> = allTransactions.map { txList ->
        // Generate last 6 months trend
        val cal = Calendar.getInstance()
        val result = mutableListOf<TrendBarData>()

        for (i in 5 downTo 0) {
            val c = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.MONTH, -i)
            }
            val m = c.get(Calendar.MONTH) + 1
            val y = c.get(Calendar.YEAR)
            val (st, en) = DateUtils.getMonthStartAndEndTimestamps(m, y)

            val mExpenses = txList.filter { it.date in st..en && it.type == "EXPENSE" }.sumOf { it.amountPaise }
            val mIncome = txList.filter { it.date in st..en && it.type == "INCOME" }.sumOf { it.amountPaise }

            val label = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(c.time)
            result.add(
                TrendBarData(
                    label = label,
                    expensePaise = mExpenses,
                    incomePaise = mIncome
                )
            )
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun addTransaction(
        type: String,
        amountPaise: Long,
        categoryId: Long,
        title: String,
        merchant: String?,
        date: Long,
        paymentMethodId: Long,
        notes: String?,
        isRecurring: Boolean = false,
        recurringFrequency: String = "MONTHLY"
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                type = type,
                amountPaise = amountPaise,
                categoryId = categoryId,
                title = title.ifBlank { "Transaction" },
                merchant = merchant?.takeIf { it.isNotBlank() },
                date = date,
                paymentMethodId = paymentMethodId,
                notes = notes?.takeIf { it.isNotBlank() }
            )
            val id = repository.insertTransaction(tx)

            if (isRecurring) {
                val rec = RecurringTransactionEntity(
                    type = type,
                    amountPaise = amountPaise,
                    title = title.ifBlank { "Recurring $type" },
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId,
                    frequency = recurringFrequency,
                    startDate = date,
                    nextExecutionDate = date + (30L * 24 * 60 * 60 * 1000)
                )
                repository.insertRecurring(rec)
            }
        }
    }

    fun updateTransaction(
        id: Long,
        type: String,
        amountPaise: Long,
        categoryId: Long,
        title: String,
        merchant: String?,
        date: Long,
        paymentMethodId: Long,
        notes: String?
    ) {
        viewModelScope.launch {
            val existing = repository.getTransactionById(id)
            if (existing != null) {
                val updated = existing.copy(
                    type = type,
                    amountPaise = amountPaise,
                    categoryId = categoryId,
                    title = title.ifBlank { "Transaction" },
                    merchant = merchant?.takeIf { it.isNotBlank() },
                    date = date,
                    paymentMethodId = paymentMethodId,
                    notes = notes?.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateTransaction(updated)
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun setMonthlyOverallBudget(month: Int, year: Int, amountPaise: Long, warningThreshold: Float? = null) {
        viewModelScope.launch {
            val threshold = warningThreshold ?: _userSettings.value.warningThreshold
            val budget = BudgetEntity(
                month = month,
                year = year,
                categoryId = null,
                amountPaise = amountPaise,
                warningThreshold = threshold
            )
            repository.setBudget(budget)
        }
    }

    fun setCategoryBudget(month: Int, year: Int, categoryId: Long, amountPaise: Long, warningThreshold: Float = 0.8f) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                month = month,
                year = year,
                categoryId = categoryId,
                amountPaise = amountPaise,
                warningThreshold = warningThreshold
            )
            repository.setBudget(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun addGoal(name: String, targetAmountPaise: Long, initialAmountPaise: Long, targetDate: Long?, iconName: String = "Flag") {
        viewModelScope.launch {
            val goal = FinancialGoalEntity(
                name = name,
                targetAmountPaise = targetAmountPaise,
                currentAmountPaise = initialAmountPaise,
                targetDate = targetDate,
                iconName = iconName
            )
            repository.insertGoal(goal)
        }
    }

    fun updateGoal(goal: FinancialGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: FinancialGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addMoneyToGoal(goalId: Long, addAmountPaise: Long) {
        viewModelScope.launch {
            repository.addMoneyToGoal(goalId, addAmountPaise)
        }
    }

    fun addRecurring(item: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.insertRecurring(item)
        }
    }

    fun updateRecurring(item: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurring(item)
        }
    }

    fun deleteRecurring(item: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(item)
        }
    }

    fun addCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val canDelete = repository.canDeleteCategory(category.id)
            if (canDelete) {
                repository.deleteCategory(category)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateUserSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateUserSettings(settings)
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadDemoData(_selectedMonth.value, _selectedYear.value)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    suspend fun exportJson(): String {
        return repository.exportDataToJson()
    }

    suspend fun importJson(jsonStr: String): Boolean {
        return repository.importDataFromJson(jsonStr)
    }

    // Google Drive Backup state & functions
    private val _isDriveBackingUp = MutableStateFlow(false)
    val isDriveBackingUp: StateFlow<Boolean> = _isDriveBackingUp.asStateFlow()

    private val _isDriveRestoring = MutableStateFlow(false)
    val isDriveRestoring: StateFlow<Boolean> = _isDriveRestoring.asStateFlow()

    private val _driveBackupMessage = MutableStateFlow<String?>(null)
    val driveBackupMessage: StateFlow<String?> = _driveBackupMessage.asStateFlow()

    fun clearDriveMessage() {
        _driveBackupMessage.value = null
    }

    fun backupToGoogleDrive(context: android.content.Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _isDriveBackingUp.value = true
            _driveBackupMessage.value = "Connecting to Google Drive..."
            try {
                val driveService = com.example.data.drive.GoogleDriveService(context)
                val token = driveService.getAccessToken(account)
                if (token == null) {
                    _driveBackupMessage.value = "Failed to obtain Google Drive authorization token"
                    _isDriveBackingUp.value = false
                    return@launch
                }

                _driveBackupMessage.value = "Uploading backup to Google Drive..."
                val jsonBackup = repository.exportDataToJson()
                val success = driveService.uploadBackup(token, jsonBackup)
                if (success) {
                    val prefs = com.example.data.drive.DrivePreferences(context)
                    prefs.userEmail = account.email
                    prefs.lastBackupTime = System.currentTimeMillis()
                    _driveBackupMessage.value = "Backup successfully uploaded to Google Drive!"
                } else {
                    _driveBackupMessage.value = "Failed to upload backup to Google Drive"
                }
            } catch (e: Exception) {
                _driveBackupMessage.value = "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isDriveBackingUp.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(context: android.content.Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch {
            _isDriveRestoring.value = true
            _driveBackupMessage.value = "Connecting to Google Drive..."
            try {
                val driveService = com.example.data.drive.GoogleDriveService(context)
                val token = driveService.getAccessToken(account)
                if (token == null) {
                    _driveBackupMessage.value = "Failed to obtain authorization token"
                    _isDriveRestoring.value = false
                    return@launch
                }

                _driveBackupMessage.value = "Fetching backup file..."
                val jsonString = driveService.downloadBackup(token)
                if (jsonString.isNullOrBlank()) {
                    _driveBackupMessage.value = "No existing Budget Bro backup found on Google Drive"
                } else {
                    val success = repository.importDataFromJson(jsonString)
                    if (success) {
                        _driveBackupMessage.value = "Data successfully restored from Google Drive!"
                    } else {
                        _driveBackupMessage.value = "Failed parsing backup data from Google Drive"
                    }
                }
            } catch (e: Exception) {
                _driveBackupMessage.value = "Restore failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isDriveRestoring.value = false
            }
        }
    }

    fun setPeriodicBackupEnabled(context: android.content.Context, enabled: Boolean, frequency: String = "DAILY") {
        val prefs = com.example.data.drive.DrivePreferences(context)
        prefs.isPeriodicBackupEnabled = enabled
        prefs.periodicFrequency = frequency
        if (enabled) {
            com.example.data.drive.DriveBackupWorker.schedulePeriodicBackup(context, frequency)
        } else {
            com.example.data.drive.DriveBackupWorker.cancelPeriodicBackup(context)
        }
    }

    fun triggerTestBudgetAlert(context: android.content.Context, customPercentage: Int? = null) {
        val pct = customPercentage ?: (_userSettings.value.warningThreshold * 100).toInt()
        val symbol = _userSettings.value.currencySymbol
        com.example.util.NotificationHelper.triggerTestBudgetAlert(
            context = context,
            customPercentage = pct,
            currencySymbol = symbol
        )
    }

    fun checkBudgetAlertsNow() {
        viewModelScope.launch {
            repository.checkOverallMonthlyBudget(_selectedMonth.value, _selectedYear.value)
        }
    }
}

class BudgetViewModelFactory(
    private val repository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

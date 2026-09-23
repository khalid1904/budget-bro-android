package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionDetail
import com.example.data.repository.BudgetRepository
import com.example.ui.dialogs.*
import com.example.ui.screens.*
import com.example.ui.theme.BudgetBroTheme
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.BudgetViewModelFactory
import com.example.ui.viewmodel.CategoryBudgetUiModel
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    HOME("Home", Icons.Rounded.Home, "tab_home"),
    TRANSACTIONS("Transactions", Icons.Rounded.ReceiptLong, "tab_transactions"),
    BUDGET("Budget", Icons.Rounded.PieChart, "tab_budget"),
    ANALYTICS("Analytics", Icons.Rounded.BarChart, "tab_analytics"),
    SETTINGS("Settings", Icons.Rounded.Settings, "tab_settings")
}

class MainActivity : ComponentActivity() {

    private val sharedReceiptUriState = mutableStateOf<Uri?>(null)
    private val targetTabState = mutableStateOf<ScreenTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = BudgetRepository(database, applicationContext)
        val viewModelFactory = BudgetViewModelFactory(repository)

        // Seed demo data on first launch if empty
        lifecycleScope.launch {
            val settings = repository.getUserSettings()
            if (!settings.hasCompletedOnboarding) {
                repository.loadDemoData()
                repository.updateUserSettings(settings.copy(hasCompletedOnboarding = true))
            }
        }

        setContent {
            BudgetBroApp(
                viewModelFactory = viewModelFactory,
                sharedReceiptUri = sharedReceiptUriState.value,
                onClearSharedReceiptUri = { sharedReceiptUriState.value = null },
                targetTab = targetTabState.value,
                onClearTargetTab = { targetTabState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getStringExtra("OPEN_SCREEN") == "BUDGET") {
            targetTabState.value = ScreenTab.BUDGET
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } ?: intent.clipData?.getItemAt(0)?.uri

            if (uri != null) {
                sharedReceiptUriState.value = uri
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetBroApp(
    viewModelFactory: BudgetViewModelFactory,
    sharedReceiptUri: Uri? = null,
    onClearSharedReceiptUri: () -> Unit = {},
    targetTab: ScreenTab? = null,
    onClearTargetTab: () -> Unit = {},
    viewModel: BudgetViewModel = viewModel(factory = viewModelFactory)
) {
    BudgetBroTheme {
        // Notification permission request for Android 13+
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied handled gracefully */ }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        var currentTab by remember { mutableStateOf(ScreenTab.HOME) }

        LaunchedEffect(targetTab) {
            if (targetTab != null) {
                currentTab = targetTab
                onClearTargetTab()
            }
        }
        var activeSubScreen by remember { mutableStateOf<String?>(null) } // "CATEGORIES", "GOALS", "RECURRING"

        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var activeReceiptScanUri by remember { mutableStateOf<Uri?>(null) }

        LaunchedEffect(sharedReceiptUri) {
            if (sharedReceiptUri != null) {
                activeReceiptScanUri = sharedReceiptUri
            }
        }

        val pickReceiptLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                activeReceiptScanUri = uri
            }
        }

        val categories by viewModel.allCategories.collectAsState()
        val expenseCategories by viewModel.expenseCategories.collectAsState()
        val paymentMethods by viewModel.allPaymentMethods.collectAsState()
        val userSettings by viewModel.userSettings.collectAsState()
        val overallBudget by viewModel.overallMonthBudgetPaise.collectAsState()
        val overallThreshold by viewModel.overallMonthBudgetThreshold.collectAsState()
        val selectedMonth by viewModel.selectedMonth.collectAsState()
        val selectedYear by viewModel.selectedYear.collectAsState()

        // Dialog states
        var showAddEditTxDialog by remember { mutableStateOf(false) }
        var addTxInitialType by remember { mutableStateOf("EXPENSE") }
        var editingTransaction by remember { mutableStateOf<TransactionDetail?>(null) }
        var viewingTransactionDetail by remember { mutableStateOf<TransactionDetail?>(null) }

        var showSetBudgetDialog by remember { mutableStateOf(false) }
        var isSettingOverallBudget by remember { mutableStateOf(true) }
        var editingCategoryBudget by remember { mutableStateOf<CategoryBudgetUiModel?>(null) }

        var showAddGoalDialog by remember { mutableStateOf(false) }
        var editingGoal by remember { mutableStateOf<FinancialGoalEntity?>(null) }

        var showAddRecurringDialog by remember { mutableStateOf(false) }
        var editingRecurring by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("budget_bro_scaffold"),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (activeSubScreen == null) {
                    NavigationBar(
                        tonalElevation = 4.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        ScreenTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // Show FAB on Home, Transactions, and Budget screens
                if (activeSubScreen == null && currentTab in listOf(ScreenTab.HOME, ScreenTab.TRANSACTIONS, ScreenTab.BUDGET)) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            addTxInitialType = "EXPENSE"
                            showAddEditTxDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_expense")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Expense",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeSubScreen) {
                    "CATEGORIES" -> {
                        CategoriesScreen(
                            viewModel = viewModel,
                            onBack = { activeSubScreen = null }
                        )
                    }
                    "GOALS" -> {
                        GoalsScreen(
                            viewModel = viewModel,
                            onAddGoal = {
                                editingGoal = null
                                showAddGoalDialog = true
                            },
                            onEditGoal = { goal ->
                                editingGoal = goal
                                showAddGoalDialog = true
                            }
                        )
                    }
                    "RECURRING" -> {
                        RecurringScreen(
                            viewModel = viewModel,
                            onAddRecurring = {
                                editingRecurring = null
                                showAddRecurringDialog = true
                            },
                            onEditRecurring = { item ->
                                editingRecurring = item
                                showAddRecurringDialog = true
                            }
                        )
                    }
                    else -> {
                        when (currentTab) {
                            ScreenTab.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToTransactions = { currentTab = ScreenTab.TRANSACTIONS },
                                    onNavigateToBudget = { currentTab = ScreenTab.BUDGET },
                                    onNavigateToGoals = { activeSubScreen = "GOALS" },
                                    onNavigateToRecurring = { activeSubScreen = "RECURRING" },
                                    onOpenAddExpense = {
                                        editingTransaction = null
                                        addTxInitialType = "EXPENSE"
                                        showAddEditTxDialog = true
                                    },
                                    onOpenAddIncome = {
                                        editingTransaction = null
                                        addTxInitialType = "INCOME"
                                        showAddEditTxDialog = true
                                    },
                                    onScanReceipt = {
                                        pickReceiptLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onSelectTransaction = { tx ->
                                        viewingTransactionDetail = tx
                                    }
                                )
                            }
                            ScreenTab.TRANSACTIONS -> {
                                TransactionsScreen(
                                    viewModel = viewModel,
                                    onSelectTransaction = { tx -> viewingTransactionDetail = tx },
                                    onAddTransaction = {
                                        editingTransaction = null
                                        addTxInitialType = "EXPENSE"
                                        showAddEditTxDialog = true
                                    },
                                    onScanReceipt = {
                                        pickReceiptLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                            }
                            ScreenTab.BUDGET -> {
                                BudgetScreen(
                                    viewModel = viewModel,
                                    onOpenEditOverallBudget = {
                                        isSettingOverallBudget = true
                                        editingCategoryBudget = null
                                        showSetBudgetDialog = true
                                    },
                                    onOpenAddCategoryBudget = {
                                        isSettingOverallBudget = false
                                        editingCategoryBudget = null
                                        showSetBudgetDialog = true
                                    },
                                    onOpenEditCategoryBudget = { catBudget ->
                                        isSettingOverallBudget = false
                                        editingCategoryBudget = catBudget
                                        showSetBudgetDialog = true
                                    }
                                )
                            }
                            ScreenTab.ANALYTICS -> {
                                AnalyticsScreen(viewModel = viewModel)
                            }
                            ScreenTab.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToCategories = { activeSubScreen = "CATEGORIES" },
                                    onNavigateToGoals = { activeSubScreen = "GOALS" },
                                    onNavigateToRecurring = { activeSubScreen = "RECURRING" }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit Transaction Dialog
        if (showAddEditTxDialog) {
            AddEditTransactionDialog(
                initialType = addTxInitialType,
                existingTransaction = editingTransaction,
                categories = categories,
                paymentMethods = paymentMethods,
                currencySymbol = userSettings.currencySymbol,
                onDismiss = {
                    showAddEditTxDialog = false
                    editingTransaction = null
                },
                onSave = { type, amountPaise, categoryId, title, merchant, date, pmId, notes, isRecurring, recurringFreq ->
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(
                            id = editingTransaction!!.id,
                            type = type,
                            amountPaise = amountPaise,
                            categoryId = categoryId,
                            title = title,
                            merchant = merchant,
                            date = date,
                            paymentMethodId = pmId,
                            notes = notes
                        )
                    } else {
                        viewModel.addTransaction(
                            type = type,
                            amountPaise = amountPaise,
                            categoryId = categoryId,
                            title = title,
                            merchant = merchant,
                            date = date,
                            paymentMethodId = pmId,
                            notes = notes,
                            isRecurring = isRecurring,
                            recurringFrequency = recurringFreq
                        )
                    }
                    showAddEditTxDialog = false
                    editingTransaction = null
                }
            )
        }

        // Viewing Transaction Detail Dialog
        viewingTransactionDetail?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                currencySymbol = userSettings.currencySymbol,
                onDismiss = { viewingTransactionDetail = null },
                onEdit = {
                    editingTransaction = tx
                    addTxInitialType = tx.type
                    viewingTransactionDetail = null
                    showAddEditTxDialog = true
                },
                onDelete = {
                    viewModel.deleteTransaction(tx.id)
                    viewingTransactionDetail = null
                }
            )
        }

        // Set Budget Dialog (Overall or Category)
        if (showSetBudgetDialog) {
            SetBudgetDialog(
                isOverall = isSettingOverallBudget,
                initialCategoryBudget = editingCategoryBudget,
                expenseCategories = expenseCategories,
                currencySymbol = userSettings.currencySymbol,
                currentOverallBudgetPaise = overallBudget,
                currentOverallThreshold = overallThreshold,
                onDismiss = {
                    showSetBudgetDialog = false
                    editingCategoryBudget = null
                },
                onSaveOverall = { amountPaise, warningThreshold ->
                    viewModel.setMonthlyOverallBudget(selectedMonth, selectedYear, amountPaise, warningThreshold)
                },
                onSaveCategory = { categoryId, amountPaise, warningThreshold ->
                    viewModel.setCategoryBudget(selectedMonth, selectedYear, categoryId, amountPaise, warningThreshold)
                },
                onDeleteCategoryBudget = { budgetId ->
                    viewModel.deleteBudget(budgetId)
                }
            )
        }

        // Add/Edit Goal Dialog
        if (showAddGoalDialog) {
            AddEditGoalDialog(
                existingGoal = editingGoal,
                currencySymbol = userSettings.currencySymbol,
                onDismiss = {
                    showAddGoalDialog = false
                    editingGoal = null
                },
                onSave = { name, targetPaise, currentPaise, targetDate ->
                    if (editingGoal != null) {
                        viewModel.updateGoal(
                            editingGoal!!.copy(
                                name = name,
                                targetAmountPaise = targetPaise,
                                currentAmountPaise = currentPaise,
                                targetDate = targetDate
                            )
                        )
                    } else {
                        viewModel.addGoal(name, targetPaise, currentPaise, targetDate)
                    }
                    showAddGoalDialog = false
                    editingGoal = null
                },
                onDelete = { goal ->
                    viewModel.deleteGoal(goal)
                }
            )
        }

        // Add/Edit Recurring Dialog
        if (showAddRecurringDialog) {
            AddEditRecurringDialog(
                existingRecurring = editingRecurring,
                categories = categories,
                currencySymbol = userSettings.currencySymbol,
                onDismiss = {
                    showAddRecurringDialog = false
                    editingRecurring = null
                },
                onSave = { type, amountPaise, title, categoryId, frequency ->
                    if (editingRecurring != null) {
                        viewModel.updateRecurring(
                            editingRecurring!!.copy(
                                type = type,
                                amountPaise = amountPaise,
                                title = title,
                                categoryId = categoryId,
                                frequency = frequency
                            )
                        )
                    } else {
                        val newRecurring = RecurringTransactionEntity(
                            type = type,
                            amountPaise = amountPaise,
                            title = title,
                            categoryId = categoryId,
                            frequency = frequency,
                            paymentMethodId = 1L,
                            startDate = System.currentTimeMillis(),
                            nextExecutionDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                        )
                        viewModel.addRecurring(newRecurring)
                    }
                    showAddRecurringDialog = false
                    editingRecurring = null
                },
                onDelete = { item ->
                    viewModel.deleteRecurring(item)
                }
            )
        }

        // Receipt Scan Dialog (via direct share from UPI apps or in-app picker)
        if (activeReceiptScanUri != null) {
            ReceiptScanDialog(
                imageUri = activeReceiptScanUri!!,
                categories = categories,
                paymentMethods = paymentMethods,
                currencySymbol = userSettings.currencySymbol,
                onDismiss = {
                    activeReceiptScanUri = null
                    onClearSharedReceiptUri()
                },
                onSaveExpense = { amountPaise, categoryId, title, merchant, date, paymentMethodId, notes ->
                    viewModel.addTransaction(
                        type = "EXPENSE",
                        amountPaise = amountPaise,
                        categoryId = categoryId,
                        title = title,
                        merchant = merchant,
                        date = date,
                        paymentMethodId = paymentMethodId,
                        notes = notes
                    )
                    activeReceiptScanUri = null
                    onClearSharedReceiptUri()
                    scope.launch {
                        snackbarHostState.showSnackbar("UPI Expense added from receipt! ✨")
                    }
                }
            )
        }
    }
}

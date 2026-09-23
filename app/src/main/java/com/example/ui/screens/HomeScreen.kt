package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionDetail
import com.example.ui.components.BalanceSummaryCard
import com.example.ui.components.MonthlySpendingCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.SmartInsight
import com.example.util.DateUtils

@Composable
fun HomeScreen(
    viewModel: BudgetViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onScanReceipt: () -> Unit,
    onSelectTransaction: (TransactionDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val totalBalance by viewModel.totalBalancePaise.collectAsState()
    val monthIncome by viewModel.monthIncomePaise.collectAsState()
    val monthExpense by viewModel.monthExpensePaise.collectAsState()
    val overallBudget by viewModel.overallMonthBudgetPaise.collectAsState()

    val monthTransactions by viewModel.currentMonthTransactions.collectAsState()
    val recentTransactions = remember(monthTransactions) { monthTransactions.take(8) }
    val smartInsights by viewModel.smartInsights.collectAsState()

    val greeting = remember { DateUtils.getGreeting() }
    val monthTitle = remember(selectedMonth, selectedYear) {
        DateUtils.getMonthYearTitle(selectedMonth, selectedYear)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with greeting and Month Navigation
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$greeting, ${userSettings.userName}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track your money effortlessly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Goal badge/button
                    FilledTonalIconButton(
                        onClick = onNavigateToGoals,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("home_goals_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Goals",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Switcher Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous Month"
                            )
                        }

                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("month_year_header_text")
                        )

                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Next Month"
                            )
                        }
                    }
                }
            }
        }

        // 2. Balance Summary Card
        item {
            BalanceSummaryCard(
                balancePaise = totalBalance,
                incomePaise = monthIncome,
                expensePaise = monthExpense,
                currencySymbol = userSettings.currencySymbol
            )
        }

        // 3. Monthly Spending Progress Card
        item {
            MonthlySpendingCard(
                monthTitle = monthTitle,
                spentPaise = monthExpense,
                budgetPaise = overallBudget,
                currencySymbol = userSettings.currencySymbol
            )
        }

        // 4. Quick Action Buttons
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // + Expense (Primary)
                    Button(
                        onClick = onOpenAddExpense,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("quick_action_add_expense"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // + Income
                    FilledTonalButton(
                        onClick = onOpenAddIncome,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(50.dp)
                            .testTag("quick_action_add_income"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF047857)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Recurring
                    FilledTonalIconButton(
                        onClick = onNavigateToRecurring,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("quick_action_recurring"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Autorenew,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Budget
                    FilledTonalIconButton(
                        onClick = onNavigateToBudget,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("quick_action_budget"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = "Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 4b. Scan UPI Receipt Quick Feature Card
        item {
            Card(
                onClick = onScanReceipt,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_scan_receipt_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Scan UPI Receipt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "AI / OCR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share from GPay, PhonePe, Paytm or upload screenshot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5. Smart Rule-Based Insights
        if (smartInsights.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Smart Insights",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        smartInsights.take(2).forEach { insight ->
                            InsightItemCard(insight)
                        }
                    }
                }
            }
        }

        // 6. Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (recentTransactions.isNotEmpty()) {
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("home_view_all_transactions")
                    ) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                EmptyTransactionsCard(onAddExpense = onOpenAddExpense)
            }
        } else {
            items(recentTransactions, key = { it.id }) { item ->
                TransactionRowItem(
                    item = item,
                    currencySymbol = userSettings.currencySymbol,
                    onClick = { onSelectTransaction(item) }
                )
            }
        }
    }
}

@Composable
private fun InsightItemCard(insight: SmartInsight) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (insight.iconName) {
                    "Warning" -> Icons.Rounded.Warning
                    "TrendingUp" -> Icons.AutoMirrored.Rounded.TrendingUp
                    "PieChart" -> Icons.Rounded.PieChart
                    "ArrowUpward" -> Icons.Rounded.ArrowUpward
                    "ArrowDownward" -> Icons.Rounded.ArrowDownward
                    else -> Icons.Rounded.CheckCircle
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyTransactionsCard(onAddExpense: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Start tracking your spending to understand where your money goes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddExpense,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_state_add_expense_btn")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Expense")
            }
        }
    }
}

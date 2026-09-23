package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.CategorySpendItem
import com.example.ui.components.DonutChart
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.AnalyticsTimeRange
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val analyticsRange by viewModel.analyticsRange.collectAsState()
    val trendChartType by viewModel.trendChartType.collectAsState()

    val (totalSpent, breakdownItems) = viewModel.expenseBreakdown.collectAsState().value
    val topCategories by viewModel.topSpendingCategories.collectAsState()
    val trendData by viewModel.spendingTrends.collectAsState()
    val comparison by viewModel.monthlyComparison.collectAsState()

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    // Key metrics calculations
    val daysElapsed = remember(selectedMonth, selectedYear) {
        DateUtils.getDaysElapsedInMonth(selectedMonth, selectedYear)
    }
    val totalDays = remember(selectedMonth, selectedYear) {
        DateUtils.getTotalDaysInMonth(selectedMonth, selectedYear)
    }
    val dailyAveragePaise = if (daysElapsed > 0) totalSpent / daysElapsed else 0L
    val estimatedMonthTotalPaise = dailyAveragePaise * totalDays

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Range Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsTimeRange.values().forEach { range ->
                    val label = when (range) {
                        AnalyticsTimeRange.THIS_MONTH -> "This Month"
                        AnalyticsTimeRange.LAST_MONTH -> "Last Month"
                        AnalyticsTimeRange.LAST_3_MONTHS -> "Last 3 Months"
                        AnalyticsTimeRange.LAST_6_MONTHS -> "Last 6 Months"
                        AnalyticsTimeRange.THIS_YEAR -> "This Year"
                    }
                    FilterChip(
                        selected = analyticsRange == range,
                        onClick = { viewModel.setAnalyticsRange(range) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Daily Average Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatPaise(dailyAveragePaise, userSettings.currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Based on $daysElapsed days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Estimated Month Total Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Estimated Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatPaise(estimatedMonthTotalPaise, userSettings.currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Projected for $totalDays days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Category Breakdown Card (Donut Chart)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Expense Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DonutChart(
                        items = breakdownItems,
                        totalAmountPaise = totalSpent,
                        currencySymbol = userSettings.currencySymbol
                    )
                }
            }
        }

        // Spending Trend Bar Chart Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "6-Month Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Toggle Buttons (Expense / Income)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = trendChartType == "EXPENSE",
                                onClick = { viewModel.setTrendChartType("EXPENSE") },
                                label = { Text("Expense") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = trendChartType == "INCOME",
                                onClick = { viewModel.setTrendChartType("INCOME") },
                                label = { Text("Income") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    SpendingTrendBarChart(
                        data = trendData,
                        currencySymbol = userSettings.currencySymbol,
                        showType = trendChartType
                    )
                }
            }
        }

        // Monthly Comparison Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Month-over-Month Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = comparison.currentMonthTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPaise(comparison.currentMonthExpensePaise, userSettings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = comparison.previousMonthTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPaise(comparison.previousMonthExpensePaise, userSettings.currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val diff = comparison.differencePaise
                    val isMore = diff > 0
                    val isLess = diff < 0
                    val changeColor = if (isMore) FinanceRed else if (isLess) FinanceGreen else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Difference",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isMore) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = changeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${CurrencyFormatter.formatPaise(abs(diff), userSettings.currencySymbol)} (${abs(comparison.percentageChange).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = changeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

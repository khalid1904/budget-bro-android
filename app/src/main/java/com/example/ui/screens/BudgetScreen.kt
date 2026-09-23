package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceOrange
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.BudgetViewModel
import com.example.ui.viewmodel.CategoryBudgetUiModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onOpenEditOverallBudget: () -> Unit,
    onOpenAddCategoryBudget: () -> Unit,
    onOpenEditCategoryBudget: (CategoryBudgetUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val monthExpense by viewModel.monthExpensePaise.collectAsState()
    val overallBudget by viewModel.overallMonthBudgetPaise.collectAsState()
    val categoryBudgets by viewModel.categoryBudgetsUi.collectAsState()

    val monthTitle = remember(selectedMonth, selectedYear) {
        DateUtils.getMonthYearTitle(selectedMonth, selectedYear)
    }

    val overallRemaining = overallBudget - monthExpense
    val overallRatio = if (overallBudget > 0) (monthExpense.toFloat() / overallBudget.toFloat()) else 0f
    val overallPercent = (overallRatio * 100).toInt()

    val overallColor = when {
        overallRatio > 1.0f -> FinanceRed
        overallRatio >= 0.8f -> FinanceOrange
        else -> FinanceGreen
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector
        item {
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = "Prev")
                    }
                    Text(
                        text = "$monthTitle Budget",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { viewModel.nextMonth() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "Next")
                    }
                }
            }
        }

        // Overall Monthly Budget Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_budget_card")
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
                        Column {
                            Text(
                                text = "Overall Monthly Budget",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatPaise(overallBudget, userSettings.currencySymbol),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onOpenEditOverallBudget,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("btn_edit_overall_budget")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Budget",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = overallRatio.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "overallBudgetProgress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(overallColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPaise(monthExpense, userSettings.currencySymbol),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = overallColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Used",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$overallPercent%",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = overallColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (overallRemaining >= 0) "Remaining" else "Overspent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPaise(kotlin.math.abs(overallRemaining), userSettings.currencySymbol),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (overallRemaining >= 0) FinanceGreen else FinanceRed
                            )
                        }
                    }
                }
            }
        }

        // Category Budgets Header with "+ Set Budget"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = onOpenAddCategoryBudget,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_add_category_budget")
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set Budget")
                }
            }
        }

        if (categoryBudgets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No category budgets set",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set category-level limits for Food, Groceries, Shopping, etc.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(categoryBudgets, key = { it.budgetId }) { item ->
                CategoryBudgetItemCard(
                    item = item,
                    currencySymbol = userSettings.currencySymbol,
                    onClick = { onOpenEditCategoryBudget(item) }
                )
            }
        }

        // Alert Thresholds Notice Card
        item {
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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Local Budget Alerts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Budget Bro notifies you at 80%, 100%, and when exceeded. Works 100% on device without internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetItemCard(
    item: CategoryBudgetUiModel,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val iconColor = CategoryIconHelper.parseColor(item.categoryColor)
    val ratio = if (item.budgetAmountPaise > 0) (item.spentAmountPaise.toFloat() / item.budgetAmountPaise.toFloat()) else 0f
    val progressColor = when {
        ratio > 1.0f -> FinanceRed
        ratio >= item.warningThreshold -> FinanceOrange
        else -> FinanceGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("category_budget_item_${item.categoryId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(item.categoryIcon),
                            contentDescription = item.categoryName,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Budget: ${CurrencyFormatter.formatPaise(item.budgetAmountPaise, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${item.percentageUsed}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = ratio.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "catBudgetProgress"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ${CurrencyFormatter.formatPaise(item.spentAmountPaise, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (item.remainingAmountPaise >= 0) {
                        "Remaining: ${CurrencyFormatter.formatPaise(item.remainingAmountPaise, currencySymbol)}"
                    } else {
                        "Over: ${CurrencyFormatter.formatPaise(-item.remainingAmountPaise, currencySymbol)}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (item.remainingAmountPaise < 0) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (item.remainingAmountPaise >= 0) FinanceGreen else FinanceRed
                )
            }
        }
    }
}

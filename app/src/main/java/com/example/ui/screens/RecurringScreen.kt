package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringTransactionEntity
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.CurrencyFormatter

@Composable
fun RecurringScreen(
    viewModel: BudgetViewModel,
    onAddRecurring: () -> Unit,
    onEditRecurring: (RecurringTransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val recurringList by viewModel.allRecurring.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recurring_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recurring Expenses",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Subscriptions, bills & recurring income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddRecurring,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_create_recurring")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        if (recurringList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No recurring transactions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track predictable monthly expenses like rent, Netflix, gym memberships, electricity, or salary.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddRecurring, shape = RoundedCornerShape(12.dp)) {
                            Text("Add Recurring Transaction")
                        }
                    }
                }
            }
        } else {
            items(recurringList, key = { it.id }) { item ->
                val category = categories.find { it.id == item.categoryId }
                val isExpense = item.type == "EXPENSE"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRecurring(item) }
                        .testTag("recurring_item_${item.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isExpense) FinanceRed.copy(alpha = 0.12f)
                                    else FinanceGreen.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isExpense) Icons.Rounded.Autorenew else Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = if (isExpense) FinanceRed else FinanceGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.frequency.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (category != null) {
                                    Text(
                                        text = " • ${category.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = (if (isExpense) "- " else "+ ") + CurrencyFormatter.formatPaise(item.amountPaise, userSettings.currencySymbol),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isExpense) MaterialTheme.colorScheme.onSurface else FinanceGreen
                            )
                            Switch(
                                checked = item.enabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.updateRecurring(item.copy(enabled = isEnabled))
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

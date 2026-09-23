package com.example.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.CategoryBudgetUiModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    isOverall: Boolean,
    initialCategoryBudget: CategoryBudgetUiModel? = null,
    expenseCategories: List<CategoryEntity>,
    currencySymbol: String = "₹",
    currentOverallBudgetPaise: Long = 6000000L,
    currentOverallThreshold: Float = 0.80f,
    onDismiss: () -> Unit,
    onSaveOverall: (amountPaise: Long, warningThreshold: Float) -> Unit,
    onSaveCategory: (categoryId: Long, amountPaise: Long, warningThreshold: Float) -> Unit,
    onDeleteCategoryBudget: ((budgetId: Long) -> Unit)? = null
) {
    var amountInput by remember {
        mutableStateOf(
            if (isOverall) {
                CurrencyFormatter.paiseToInputString(currentOverallBudgetPaise)
            } else if (initialCategoryBudget != null) {
                CurrencyFormatter.paiseToInputString(initialCategoryBudget.budgetAmountPaise)
            } else {
                ""
            }
        )
    }

    var selectedCategoryId by remember {
        mutableStateOf(
            initialCategoryBudget?.categoryId ?: expenseCategories.firstOrNull()?.id ?: 1L
        )
    }

    var warningThreshold by remember {
        mutableStateOf(
            if (isOverall) currentOverallThreshold
            else (initialCategoryBudget?.warningThreshold ?: 0.8f)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("set_budget_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverall) "Set Overall Budget" else (if (initialCategoryBudget != null) "Edit Category Budget" else "Set Category Budget"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If Category budget and creating new, pick category
                if (!isOverall && initialCategoryBudget == null) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expenseCategories.forEach { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            val iconColor = CategoryIconHelper.parseColor(cat.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else iconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (!isOverall && initialCategoryBudget != null) {
                    Text(
                        text = "Category: ${initialCategoryBudget.categoryName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Amount
                Text(
                    text = "Budget Limit Amount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amountInput = input
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    placeholder = { Text("e.g. 10000") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_amount")
                )

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alert Warning Threshold",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "${(warningThreshold * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Slider(
                    value = warningThreshold,
                    onValueChange = { warningThreshold = it },
                    valueRange = 0.5f..0.95f,
                    steps = 8,
                    modifier = Modifier.testTag("slider_budget_threshold")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.50f, 0.70f, 0.75f, 0.80f, 0.85f, 0.90f).forEach { preset ->
                        val isSelected = (warningThreshold * 100).toInt() == (preset * 100).toInt()
                        FilterChip(
                            selected = isSelected,
                            onClick = { warningThreshold = preset },
                            label = { Text("${(preset * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                val parsedPaise = CurrencyFormatter.parseToPaise(amountInput)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialCategoryBudget != null && onDeleteCategoryBudget != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteCategoryBudget(initialCategoryBudget.budgetId)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceRed)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            if (parsedPaise > 0) {
                                if (isOverall) {
                                    onSaveOverall(parsedPaise, warningThreshold)
                                } else {
                                    onSaveCategory(selectedCategoryId, parsedPaise, warningThreshold)
                                }
                                onDismiss()
                            }
                        },
                        enabled = parsedPaise > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_confirm_save_budget"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Budget", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

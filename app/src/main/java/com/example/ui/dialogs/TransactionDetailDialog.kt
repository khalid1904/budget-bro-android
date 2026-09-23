package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionDetail
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun TransactionDetailDialog(
    transaction: TransactionDetail,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isExpense = transaction.type == "EXPENSE"
    val iconColor = CategoryIconHelper.parseColor(transaction.categoryColor)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_detail_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Category Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(transaction.categoryIcon),
                        contentDescription = transaction.categoryName,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                Text(
                    text = (if (isExpense) "- " else "+ ") + CurrencyFormatter.formatPaise(transaction.amountPaise, currencySymbol),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isExpense) MaterialTheme.colorScheme.onSurface else FinanceGreen
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Title / Merchant
                Text(
                    text = transaction.merchant ?: transaction.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (transaction.merchant != null && transaction.title != transaction.merchant) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Rows
                DetailRow(label = "Category", value = transaction.categoryName)
                Spacer(modifier = Modifier.height(10.dp))
                DetailRow(label = "Payment Method", value = transaction.paymentMethodName.ifBlank { "Unspecified" })
                Spacer(modifier = Modifier.height(10.dp))
                DetailRow(label = "Date", value = DateUtils.formatFullDate(transaction.date))

                if (!transaction.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow(label = "Notes", value = transaction.notes)
                }

                if (transaction.recurringId != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow(label = "Type", value = "Recurring Transaction")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Edit & Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_delete_transaction"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceRed)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_edit_transaction"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = FinanceRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

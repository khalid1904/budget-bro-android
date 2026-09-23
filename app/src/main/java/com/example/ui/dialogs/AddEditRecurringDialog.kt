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
import com.example.data.model.RecurringTransactionEntity
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter

@Composable
fun AddEditRecurringDialog(
    existingRecurring: RecurringTransactionEntity? = null,
    categories: List<CategoryEntity>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amountPaise: Long,
        title: String,
        categoryId: Long,
        frequency: String
    ) -> Unit,
    onDelete: ((RecurringTransactionEntity) -> Unit)? = null
) {
    var type by remember { mutableStateOf(existingRecurring?.type ?: "EXPENSE") }
    var title by remember { mutableStateOf(existingRecurring?.title ?: "") }
    var amountInput by remember {
        mutableStateOf(if (existingRecurring != null) CurrencyFormatter.paiseToInputString(existingRecurring.amountPaise) else "")
    }
    var frequency by remember { mutableStateOf(existingRecurring?.frequency ?: "MONTHLY") }
    var selectedCategoryId by remember {
        mutableStateOf(existingRecurring?.categoryId ?: categories.firstOrNull { it.type == type }?.id ?: 1L)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_recurring_dialog"),
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
                        text = if (existingRecurring != null) "Edit Recurring" else "Add Recurring",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Netflix, Apartment Rent, Gym") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amountInput = input
                        }
                    },
                    label = { Text("Amount") },
                    leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 1500") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Frequency Selector
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MONTHLY", "WEEKLY", "YEARLY").forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.filter { it.type == type }.forEach { cat ->
                        FilterChip(
                            selected = cat.id == selectedCategoryId,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val parsedPaise = CurrencyFormatter.parseToPaise(amountInput)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (existingRecurring != null && onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(existingRecurring)
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
                            if (title.isNotBlank() && parsedPaise > 0) {
                                onSave(type, parsedPaise, title.trim(), selectedCategoryId, frequency)
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && parsedPaise > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

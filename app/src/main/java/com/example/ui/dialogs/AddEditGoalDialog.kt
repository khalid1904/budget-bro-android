package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
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
import com.example.data.model.FinancialGoalEntity
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter

@Composable
fun AddEditGoalDialog(
    existingGoal: FinancialGoalEntity? = null,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (name: String, targetPaise: Long, currentPaise: Long, targetDate: Long?) -> Unit,
    onDelete: ((FinancialGoalEntity) -> Unit)? = null
) {
    var name by remember { mutableStateOf(existingGoal?.name ?: "") }
    var targetInput by remember {
        mutableStateOf(if (existingGoal != null) CurrencyFormatter.paiseToInputString(existingGoal.targetAmountPaise) else "")
    }
    var currentInput by remember {
        mutableStateOf(if (existingGoal != null) CurrencyFormatter.paiseToInputString(existingGoal.currentAmountPaise) else "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_goal_dialog"),
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
                        text = if (existingGoal != null) "Edit Goal" else "New Financial Goal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Goal Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. Emergency Fund, New Laptop, Trip") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Amount
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            targetInput = input
                        }
                    },
                    label = { Text("Target Amount") },
                    leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 50000") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Saved Amount
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            currentInput = input
                        }
                    },
                    label = { Text("Already Saved Amount (Optional)") },
                    leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                val targetPaise = CurrencyFormatter.parseToPaise(targetInput)
                val currentPaise = CurrencyFormatter.parseToPaise(currentInput)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (existingGoal != null && onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(existingGoal)
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
                            if (name.isNotBlank() && targetPaise > 0) {
                                onSave(name.trim(), targetPaise, currentPaise, null)
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank() && targetPaise > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Goal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

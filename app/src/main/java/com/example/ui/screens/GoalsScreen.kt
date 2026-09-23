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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialGoalEntity
import com.example.ui.theme.FinanceGreen
import com.example.ui.viewmodel.BudgetViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun GoalsScreen(
    viewModel: BudgetViewModel,
    onAddGoal: () -> Unit,
    onEditGoal: (FinancialGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allGoals.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var goalToAddMoneyTo by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    var addAmountInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("goals_screen"),
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
                        text = "Financial Goals",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Save purposefully for your milestones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddGoal,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_create_goal")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal")
                }
            }
        }

        if (goals.isEmpty()) {
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
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No financial goals yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set a goal for an Emergency Fund, Vacation, Gadgets or Long-term savings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAddGoal, shape = RoundedCornerShape(12.dp)) {
                            Text("Create a Goal")
                        }
                    }
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalItemCard(
                    goal = goal,
                    currencySymbol = userSettings.currencySymbol,
                    onAddMoney = { goalToAddMoneyTo = goal },
                    onClick = { onEditGoal(goal) }
                )
            }
        }
    }

    // Add Money Dialog
    goalToAddMoneyTo?.let { goal ->
        AlertDialog(
            onDismissRequest = {
                goalToAddMoneyTo = null
                addAmountInput = ""
            },
            title = { Text("Add Money to ${goal.name}") },
            text = {
                Column {
                    Text(
                        text = "Enter amount to deposit towards your goal:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addAmountInput,
                        onValueChange = { input ->
                            if (input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                addAmountInput = input
                            }
                        },
                        leadingIcon = { Text(userSettings.currencySymbol, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("e.g. 5000") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val parsed = CurrencyFormatter.parseToPaise(addAmountInput)
                Button(
                    onClick = {
                        if (parsed > 0) {
                            viewModel.addMoneyToGoal(goal.id, parsed)
                            goalToAddMoneyTo = null
                            addAmountInput = ""
                        }
                    },
                    enabled = parsed > 0
                ) {
                    Text("Add Money")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        goalToAddMoneyTo = null
                        addAmountInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoalItemCard(
    goal: FinancialGoalEntity,
    currencySymbol: String,
    onAddMoney: () -> Unit,
    onClick: () -> Unit
) {
    val ratio = if (goal.targetAmountPaise > 0) {
        (goal.currentAmountPaise.toFloat() / goal.targetAmountPaise.toFloat())
    } else 0f
    val percent = (ratio * 100).toInt()
    val isCompleted = goal.currentAmountPaise >= goal.targetAmountPaise

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) FinanceGreen.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = if (isCompleted) FinanceGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (goal.targetDate != null) {
                            Text(
                                text = "Target: ${DateUtils.formatRelativeDate(goal.targetDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCompleted) FinanceGreen.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCompleted) FinanceGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = ratio.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "goalProgress"
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
                        .background(if (isCompleted) FinanceGreen else MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${CurrencyFormatter.formatPaise(goal.currentAmountPaise, currencySymbol)} saved",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of ${CurrencyFormatter.formatPaise(goal.targetAmountPaise, currencySymbol)} goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onAddMoney,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Money")
                }
            }
        }
    }
}

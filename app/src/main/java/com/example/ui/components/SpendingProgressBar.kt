package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceOrange
import com.example.ui.theme.FinanceRed
import com.example.util.CurrencyFormatter

@Composable
fun MonthlySpendingCard(
    monthTitle: String,
    spentPaise: Long,
    budgetPaise: Long,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    val ratio = if (budgetPaise > 0) (spentPaise.toFloat() / budgetPaise.toFloat()) else 0f
    val percent = (ratio * 100).toInt()

    val progressColor = when {
        ratio > 1.0f -> FinanceRed
        ratio >= 0.8f -> FinanceOrange
        else -> FinanceGreen
    }

    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "spendingProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "$monthTitle Spending",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(progressColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percent% used",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${CurrencyFormatter.formatPaise(spentPaise, currencySymbol)} / ${CurrencyFormatter.formatPaise(budgetPaise, currencySymbol)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val remainingPaise = budgetPaise - spentPaise
                if (remainingPaise >= 0) {
                    Text(
                        text = "${CurrencyFormatter.formatPaise(remainingPaise, currencySymbol)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "${CurrencyFormatter.formatPaise(-remainingPaise, currencySymbol)} over",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FinanceRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
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
                        .background(progressColor)
                )
            }
        }
    }
}

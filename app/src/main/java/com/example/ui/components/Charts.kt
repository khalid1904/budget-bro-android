package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyFormatter

data class CategorySpendItem(
    val categoryName: String,
    val color: Color,
    val amountPaise: Long,
    val percentage: Float
)

data class TrendBarData(
    val label: String,
    val expensePaise: Long,
    val incomePaise: Long
)

@Composable
fun DonutChart(
    items: List<CategorySpendItem>,
    totalAmountPaise: Long,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || totalAmountPaise <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded in this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "donutProgress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .testTag("donut_chart_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f

                for (item in items) {
                    val sweepAngle = (item.percentage / 100f) * 360f * animatedProgress
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 1.5f.coerceAtMost(sweepAngle), // tiny gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += (item.percentage / 100f) * 360f
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatPaise(totalAmountPaise, currencySymbol),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            items.take(6).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = CurrencyFormatter.formatPaise(item.amountPaise, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingTrendBarChart(
    data: List<TrendBarData>,
    currencySymbol: String = "₹",
    showType: String = "EXPENSE", // "EXPENSE", "INCOME", "NET"
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No trend data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = data.maxOfOrNull {
        when (showType) {
            "EXPENSE" -> it.expensePaise
            "INCOME" -> it.incomePaise
            else -> kotlin.math.max(it.expensePaise, it.incomePaise)
        }
    }?.coerceAtLeast(10000L) ?: 10000L

    val barColor = when (showType) {
        "EXPENSE" -> MaterialTheme.colorScheme.primary
        "INCOME" -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.secondary
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("trend_bar_chart")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 24.dp.toPx()
                val totalBars = data.size
                val spacing = (size.width - (totalBars * barWidth)) / (totalBars + 1)
                val chartHeight = size.height - 30.dp.toPx()

                // Subtle grid lines
                for (i in 1..3) {
                    val y = chartHeight * (i / 3f)
                    drawLine(
                        color = surfaceVariant.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                data.forEachIndexed { index, item ->
                    val value = when (showType) {
                        "EXPENSE" -> item.expensePaise
                        "INCOME" -> item.incomePaise
                        else -> item.incomePaise - item.expensePaise
                    }
                    val heightRatio = (value.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)
                    val barH = chartHeight * heightRatio

                    val x = spacing + index * (barWidth + spacing)
                    val y = chartHeight - barH

                    // Background track
                    drawRoundRect(
                        color = surfaceVariant.copy(alpha = 0.3f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )

                    // Active bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                }
            }
        }

        // X-Axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

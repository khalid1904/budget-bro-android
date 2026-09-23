package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "budget_bro_alerts"
    private const val CHANNEL_NAME = "Budget Bro Alerts"
    private const val CHANNEL_DESC = "Notifications for budget warnings and spending updates"

    const val NOTIFICATION_ID_MONTHLY_BUDGET = 1001
    const val NOTIFICATION_ID_TEST_BUDGET = 1002

    private const val PREFS_NAME = "budget_alert_tracker_prefs"
    private const val KEY_ALERT_PREFIX = "notified_state_"

    const val ALERT_STATE_NONE = "NONE"
    const val ALERT_STATE_WARNING_80 = "WARNING_80"
    const val ALERT_STATE_EXCEEDED_100 = "EXCEEDED_100"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getAlertState(context: Context, month: Int, year: Int): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$KEY_ALERT_PREFIX${year}_$month", ALERT_STATE_NONE) ?: ALERT_STATE_NONE
    }

    fun setAlertState(context: Context, month: Int, year: Int, state: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_ALERT_PREFIX${year}_$month", state).apply()
    }

    fun showMonthlyBudgetAlert(
        context: Context,
        spentPaise: Long,
        budgetPaise: Long,
        currencySymbol: String,
        monthName: String,
        percentageUsed: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val isExceeded = spentPaise > budgetPaise
        val spentFormatted = CurrencyFormatter.formatPaise(spentPaise, currencySymbol)
        val budgetFormatted = CurrencyFormatter.formatPaise(budgetPaise, currencySymbol)

        val title = if (isExceeded) {
            "🚨 Monthly Budget Exceeded ($percentageUsed%)"
        } else {
            "⚠️ Budget Alert: $percentageUsed% Limit Reached"
        }

        val message = if (isExceeded) {
            val overage = CurrencyFormatter.formatPaise(spentPaise - budgetPaise, currencySymbol)
            "You have exceeded your $monthName budget of $budgetFormatted by $overage! Total spent: $spentFormatted."
        } else {
            val remaining = CurrencyFormatter.formatPaise(budgetPaise - spentPaise, currencySymbol)
            "You've reached $percentageUsed% of your $monthName budget ($spentFormatted of $budgetFormatted). Only $remaining remaining."
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "BUDGET")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MONTHLY_BUDGET,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID_MONTHLY_BUDGET, builder.build())
        } catch (e: Exception) {
            // Gracefully handle if notifications disabled by system
        }
    }

    fun showBudgetAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = NOTIFICATION_ID_MONTHLY_BUDGET
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "BUDGET")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            // Gracefully handle if notifications disabled by system
        }
    }

    fun triggerTestBudgetAlert(
        context: Context,
        customPercentage: Int = 80,
        currencySymbol: String = "₹"
    ) {
        val budgetPaise = 5000000L // ₹50,000
        val spentPaise = ((budgetPaise * customPercentage) / 100)
        showMonthlyBudgetAlert(
            context = context,
            spentPaise = spentPaise,
            budgetPaise = budgetPaise,
            currencySymbol = currencySymbol,
            monthName = "This Month",
            percentageUsed = customPercentage
        )
    }
}

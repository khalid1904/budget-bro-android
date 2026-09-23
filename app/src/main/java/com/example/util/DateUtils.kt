package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun getMonthYearTitle(month: Int, year: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getMonthStartAndEndTimestamps(month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun formatRelativeDate(timestamp: Long): String {
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()

        val isSameYear = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
        val isToday = isSameYear && calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = isSameYear && calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            isSameYear -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatDateForHeader(timestamp: Long): String {
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()

        val isSameYear = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
        val isToday = isSameYear && calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = isSameYear && calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "TODAY"
            isYesterday -> "YESTERDAY"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(timestamp)).uppercase(Locale.getDefault())
        }
    }

    fun formatFullDate(timestamp: Long): String {
        return SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDaysElapsedInMonth(month: Int, year: Int): Int {
        val calNow = Calendar.getInstance()
        val currentMonth = calNow.get(Calendar.MONTH) + 1
        val currentYear = calNow.get(Calendar.YEAR)

        return if (year == currentYear && month == currentMonth) {
            calNow.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else if (year < currentYear || (year == currentYear && month < currentMonth)) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
            }
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
    }

    fun getTotalDaysInMonth(month: Int, year: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}

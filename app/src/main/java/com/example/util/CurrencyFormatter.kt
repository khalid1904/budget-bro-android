package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats paise (Long) into Indian Rupee format, e.g. 12000000 paise -> "₹1,20,000"
     * Handles positive, negative, zero.
     */
    fun formatPaise(
        paise: Long,
        symbol: String = "₹",
        includeDecimalsIfAny: Boolean = false
    ): String {
        val isNegative = paise < 0
        val positivePaise = abs(paise)
        val rupees = positivePaise / 100
        val remainderPaise = positivePaise % 100

        val formattedRupees = formatIndianNumber(rupees)
        val result = if (includeDecimalsIfAny && remainderPaise > 0) {
            val paiseString = remainderPaise.toString().padStart(2, '0')
            "$symbol$formattedRupees.$paiseString"
        } else {
            "$symbol$formattedRupees"
        }

        return if (isNegative) "-$result" else result
    }

    /**
     * Formats integer number into Indian comma separation format:
     * e.g. 100000 -> "1,00,000"
     * 10000000 -> "1,00,00,000"
     */
    fun formatIndianNumber(number: Long): String {
        val s = number.toString()
        if (s.length <= 3) return s

        val lastThree = s.substring(s.length - 3)
        val remaining = s.substring(0, s.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(",")
            }
        }
        val groupedRemaining = sb.reverse().toString()
        return "$groupedRemaining,$lastThree"
    }

    /**
     * Converts a string input (e.g. "450", "450.50", "1,200") to paise
     */
    fun parseToPaise(input: String): Long {
        val clean = input.replace(",", "").trim()
        if (clean.isEmpty()) return 0L
        return try {
            if (clean.contains(".")) {
                val parts = clean.split(".")
                val whole = parts[0].toLongOrNull() ?: 0L
                val dec = parts[1].padEnd(2, '0').take(2)
                val fractional = dec.toLongOrNull() ?: 0L
                (whole * 100) + fractional
            } else {
                (clean.toLongOrNull() ?: 0L) * 100
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Formats a raw paise to decimal string for text input editing (e.g. 45050 -> "450.50")
     */
    fun paiseToInputString(paise: Long): String {
        val rupees = paise / 100
        val rem = paise % 100
        return if (rem > 0) {
            "$rupees.${rem.toString().padStart(2, '0')}"
        } else {
            rupees.toString()
        }
    }
}

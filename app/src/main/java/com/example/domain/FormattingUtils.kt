package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormattingUtils {
  fun formatSmart(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    if (abs(value) < 1e-12) return "0"
    val symbols = DecimalFormatSymbols(Locale.US)
    val absVal = abs(value)
    val pattern =
      when {
        absVal >= 1_000_000_000.0 || absVal < 0.00001 -> return String.format(Locale.US, "%.4e", value)
        absVal >= 1000.0 -> "#,##0.##"
        absVal >= 1.0 -> "#,##0.####"
        else -> "0.######"
      }
    return DecimalFormat(pattern, symbols).format(value)
  }

  fun formatMoney(value: Double, currencyCode: String = ""): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val symbols = DecimalFormatSymbols(Locale.US)
    val absVal = abs(value)
    val pattern =
      when {
        absVal >= 1000.0 -> "#,##0.00"
        absVal >= 1.0 -> "#,##0.00"
        absVal >= 0.01 -> "0.0000"
        absVal > 0.0 -> "0.000000"
        else -> "0.00"
      }
    val formatted = DecimalFormat(pattern, symbols).format(value)
    return if (currencyCode.isNotBlank()) "$formatted $currencyCode" else formatted
  }

  fun formatRate(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val symbols = DecimalFormatSymbols(Locale.US)
    val absVal = abs(value)
    val pattern =
      when {
        absVal >= 100.0 -> "#,##0.00"
        absVal >= 1.0 -> "0.0000"
        absVal >= 0.001 -> "0.00000"
        else -> "0.0000000"
      }
    return DecimalFormat(pattern, symbols).format(value)
  }

  fun formatTimestamp(millis: Long, locale: Locale = Locale.getDefault()): String {
    if (millis <= 0L) return "—"
    val fmt = SimpleDateFormat("MMM d, HH:mm:ss", locale)
    return fmt.format(Date(millis))
  }
}

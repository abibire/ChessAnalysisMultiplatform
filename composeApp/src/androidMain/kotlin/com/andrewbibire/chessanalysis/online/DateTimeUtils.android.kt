package com.andrewbibire.chessanalysis.online

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun formatGameDate(timeMillis: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timeMillis))
}

actual fun getCurrentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

actual fun getCurrentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1

actual fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month - 1)
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

package com.andrewbibire.chessanalysis.online

import platform.Foundation.*

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatGameDate(timeMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timeMillis / 1000.0)
    val dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "MMM dd, HH:mm"
    return dateFormatter.stringFromDate(date)
}

actual fun getCurrentYear(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitYear, fromDate = NSDate())
    return components.year.toInt()
}

actual fun getCurrentMonth(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitMonth, fromDate = NSDate())
    return components.month.toInt()
}

actual fun getMonthName(month: Int): String {
    val dateFormatter = NSDateFormatter()
    val monthSymbols = dateFormatter.monthSymbols as List<*>
    return monthSymbols[month - 1] as String
}

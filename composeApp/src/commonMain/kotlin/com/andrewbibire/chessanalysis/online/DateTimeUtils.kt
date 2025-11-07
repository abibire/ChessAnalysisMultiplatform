package com.andrewbibire.chessanalysis.online

expect fun currentTimeMillis(): Long

expect fun formatGameDate(timeMillis: Long): String

expect fun getCurrentYear(): Int

expect fun getCurrentMonth(): Int

expect fun getMonthName(month: Int): String

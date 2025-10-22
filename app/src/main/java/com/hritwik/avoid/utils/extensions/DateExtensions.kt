package com.hritwik.avoid.utils.extensions

import java.text.SimpleDateFormat
import java.util.*




fun Date.toDisplayString(pattern: String = "MMM dd, yyyy"): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}


fun Date.isToday(): Boolean {
    val today = Calendar.getInstance()
    val dateCalendar = Calendar.getInstance().apply { time = this@isToday }

    return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
}


fun Date.isThisWeek(): Boolean {
    val today = Calendar.getInstance()
    val dateCalendar = Calendar.getInstance().apply { time = this@isThisWeek }

    return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
            today.get(Calendar.WEEK_OF_YEAR) == dateCalendar.get(Calendar.WEEK_OF_YEAR)
}
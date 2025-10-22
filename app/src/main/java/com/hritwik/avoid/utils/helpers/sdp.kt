package com.hritwik.avoid.utils.helpers

import kotlin.math.round

fun calculateRoundedValue(value: Int): Int {
    return round(value * 0.7).toInt()
}
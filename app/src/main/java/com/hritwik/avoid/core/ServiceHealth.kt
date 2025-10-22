package com.hritwik.avoid.core

interface ServiceHealth {
    val uptime: Long
    val activeTasks: Int
    val errorCount: Int
}


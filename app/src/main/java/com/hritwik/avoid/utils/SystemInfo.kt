package com.hritwik.avoid.utils

import android.system.Os
import android.system.OsConstants


object SystemInfo {
    
    val pageSize: Long by lazy {
        runCatching { Os.sysconf(OsConstants._SC_PAGESIZE) }
            .getOrDefault(4096L)
    }
}


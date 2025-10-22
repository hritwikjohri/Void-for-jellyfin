package com.hritwik.avoid.utils

import android.app.Application

object CrashReporter {
    fun init(app: Application) = Unit

    fun report(throwable: Throwable) = Unit
}

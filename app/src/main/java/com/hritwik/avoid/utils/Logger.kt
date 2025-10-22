package com.hritwik.avoid.utils

import android.util.Log
import com.hritwik.avoid.domain.error.AppError

object Logger {
    private const val TAG = "AvoidApp"

    fun logError(error: AppError, throwable: Throwable? = null) {
        Log.e(TAG, "[${'$'}{error::class.simpleName}] ${'$'}{error.message}", throwable)
    }
}

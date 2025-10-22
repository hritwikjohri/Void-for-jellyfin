package com.hritwik.avoid.presentation.ui.common

import android.content.Context
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.error.AppError

class ErrorMessageProvider(private val context: Context) {
    fun getMessage(error: AppError): String {
        return when (error) {
            is AppError.Network -> context.getString(R.string.error_network)
            is AppError.Auth -> context.getString(R.string.error_auth)
            is AppError.Validation -> context.getString(R.string.error_validation)
            is AppError.Unknown -> context.getString(R.string.error_unknown)
        }
    }
}

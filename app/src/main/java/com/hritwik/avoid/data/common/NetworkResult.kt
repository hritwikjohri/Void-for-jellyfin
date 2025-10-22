package com.hritwik.avoid.data.common

import com.hritwik.avoid.domain.error.AppError

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val error: AppError, val exception: Throwable? = null) : NetworkResult<T>() {
        val message: String get() = error.message
        constructor(message: String, exception: Throwable? = null) : this(AppError.Unknown(message), exception)
    }
    class Loading<T> : NetworkResult<T>()
}
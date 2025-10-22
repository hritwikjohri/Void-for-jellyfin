package com.hritwik.avoid.domain.usecase.common

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.utils.Logger
import com.hritwik.avoid.utils.CrashReporter
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    suspend operator fun invoke(parameters: P): NetworkResult<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters)
            }
        } catch (e: Exception) {
            val error = when (e) {
                is IOException -> AppError.Network(e.message ?: "Network error")
                is SecurityException -> AppError.Auth(e.message ?: "Authentication error")
                is IllegalArgumentException -> AppError.Validation(e.message ?: "Validation error")
                else -> AppError.Unknown(e.message ?: "Unknown error occurred")
            }
            Logger.logError(error, e)
            CrashReporter.report(e)
            NetworkResult.Error<R>(error, e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): NetworkResult<R>
}

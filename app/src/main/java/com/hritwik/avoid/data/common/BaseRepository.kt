package com.hritwik.avoid.data.common

import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.network.NetworkRetryThrottler
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.utils.constants.AppConstants
import com.hritwik.avoid.utils.CrashReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException


abstract class BaseRepository(
    private val priorityDispatcher: PriorityDispatcher,
    private val serverConnectionManager: ServerConnectionManager
) {

    
    protected suspend fun <T> safeApiCall(
        serverUrl: String? = null,
        apiCall: suspend () -> T
    ): NetworkResult<T> {
        return withContext(Dispatchers.IO) {
            var currentDelay = 1000L
            var attempt = 0

            repeat(AppConstants.NETWORK_MAX_RETRY_ATTEMPTS) {
                NetworkRetryThrottler.throttleIfNeeded()
                try {
                    val result = apiCall()
                    serverUrl?.let { url -> serverConnectionManager.markRequestSuccess(url) }
                    return@withContext NetworkResult.Success(result)
                } catch (throwable: Throwable) {
                    if (throwable is IOException) {
                        NetworkRetryThrottler.onFailure()
                    }
                    val shouldRetry = when (throwable) {
                        is IOException -> true
                        is HttpException -> throwable.code() !in 400..499
                        else -> false
                    }

                    if (shouldRetry && attempt < AppConstants.NETWORK_MAX_RETRY_ATTEMPTS - 1) {
                        delay(currentDelay)
                        currentDelay *= 2
                        attempt++
                    } else {
                        CrashReporter.report(throwable)
                        val (errorMessage, appError) = when (throwable) {
                            is IOException -> "Network error. Please check your connection." to AppError.Network(
                                "Network error. Please check your connection."
                            )
                            is HttpException -> {
                                val message = when (throwable.code()) {
                                    401 -> "Invalid credentials"
                                    403 -> "Access forbidden"
                                    404 -> "Server not found"
                                    in 500..599 -> "Server error"
                                    else -> "HTTP ${throwable.code()}: ${throwable.message()}"
                                }
                                val appErrorForCode = when (throwable.code()) {
                                    401, 403 -> AppError.Auth(message)
                                    404 -> AppError.Network(message)
                                    in 500..599 -> AppError.Unknown(message)
                                    else -> AppError.Unknown(message)
                                }
                                message to appErrorForCode
                            }
                            else -> {
                                val message = "Unknown error: ${throwable.message}"
                                message to AppError.Unknown(message)
                            }
                        }
                        val errorResult = NetworkResult.Error<T>(appError, throwable)
                        if (throwable is IOException) {
                            serverUrl?.let { url ->
                                serverConnectionManager.markRequestFailure(url, errorMessage)
                            }
                        }
                        return@withContext errorResult
                    }
                }
            }

            
            serverUrl?.let { url ->
                serverConnectionManager.markRequestFailure(url, "Max retry attempts exceeded")
            }
            NetworkResult.Error<T>(AppError.Unknown("Max retry attempts exceeded"))
        }
    }

    
    fun <T> enqueue(
        priority: PriorityDispatcher.Priority,
        serverUrl: String? = null,
        apiCall: suspend () -> T
    ): Deferred<NetworkResult<T>> {
        val deferred = CompletableDeferred<NetworkResult<T>>()
        priorityDispatcher.enqueue(priority) {
            try {
                val result = safeApiCall(serverUrl, apiCall)
                deferred.complete(result)
            } catch (e: Exception) {
                CrashReporter.report(e)
                val message = "Error executing API call: ${e.message}"
                deferred.complete(NetworkResult.Error<T>(AppError.Unknown(message), e))
            }
        }
        return deferred
    }
}
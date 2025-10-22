package com.hritwik.avoid.domain.error

sealed class AppError(open val message: String) {
    data class Network(override val message: String) : AppError(message)
    data class Auth(override val message: String) : AppError(message)
    data class Validation(override val message: String) : AppError(message)
    data class Unknown(override val message: String) : AppError(message)
}

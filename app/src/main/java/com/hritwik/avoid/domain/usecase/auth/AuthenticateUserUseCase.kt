package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.LoginCredentials
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class AuthenticateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<AuthenticateUserUseCase.Params, AuthSession>(Dispatchers.IO) {

    data class Params(
        val serverUrl: String,
        val credentials: LoginCredentials
    )

    override suspend fun execute(parameters: Params): NetworkResult<AuthSession> {
        val result = authRepository.authenticateUser(parameters.serverUrl, parameters.credentials)
        if (result is NetworkResult.Success) {
            authRepository.saveAuthSession(result.data)
        }
        return result
    }
}
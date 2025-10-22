package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectInitiateResponse
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class InitiateQuickConnectUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<InitiateQuickConnectUseCase.Params, QuickConnectInitiateResponse>(Dispatchers.IO) {

    data class Params(
        val serverUrl: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<QuickConnectInitiateResponse> {
        return authRepository.initiateQuickConnect(parameters.serverUrl)
    }
}

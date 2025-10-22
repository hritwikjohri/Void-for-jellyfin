package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class IsQuickConnectEnabledUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<IsQuickConnectEnabledUseCase.Params, Boolean>(Dispatchers.IO) {

    data class Params(
        val serverUrl: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<Boolean> {
        return authRepository.isQuickConnectEnabled(parameters.serverUrl)
    }
}
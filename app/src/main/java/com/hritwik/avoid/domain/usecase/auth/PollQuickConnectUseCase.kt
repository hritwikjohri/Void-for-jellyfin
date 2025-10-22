package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectResult
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class PollQuickConnectUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<PollQuickConnectUseCase.Params, QuickConnectResult>(Dispatchers.IO) {

    data class Params(
        val secret: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<QuickConnectResult> {
        return authRepository.pollQuickConnect(parameters.secret)
    }
}
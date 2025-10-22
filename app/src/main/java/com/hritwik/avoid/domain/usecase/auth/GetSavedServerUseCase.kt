package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCaseNoParams
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetSavedServerUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCaseNoParams<Server?>(Dispatchers.IO) {

    override suspend fun execute(): NetworkResult<Server?> {
        return authRepository.getSavedServerConfig()
    }
}
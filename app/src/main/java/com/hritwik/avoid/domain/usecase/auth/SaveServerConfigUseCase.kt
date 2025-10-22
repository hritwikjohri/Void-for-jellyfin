package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class SaveServerConfigUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<Server, Unit>(Dispatchers.IO) {

    override suspend fun execute(parameters: Server): NetworkResult<Unit> {
        return authRepository.saveServerConfig(parameters)
    }
}
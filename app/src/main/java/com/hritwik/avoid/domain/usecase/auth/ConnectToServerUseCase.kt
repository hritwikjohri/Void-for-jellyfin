package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class ConnectToServerUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<String, Server>(Dispatchers.IO) {

    override suspend fun execute(parameters: String): NetworkResult<Server> {
        return authRepository.connectToServer(parameters)
    }
}
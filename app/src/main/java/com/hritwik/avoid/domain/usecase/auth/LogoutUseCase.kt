package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCaseNoParams
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCaseNoParams<Unit>(Dispatchers.IO) {

    override suspend fun execute(): NetworkResult<Unit> {
        return authRepository.logout()
    }
}
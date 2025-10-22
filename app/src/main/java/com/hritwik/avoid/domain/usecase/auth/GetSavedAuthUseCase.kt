package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCaseNoParams
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetSavedAuthUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCaseNoParams<AuthSession?>(Dispatchers.IO) {

    override suspend fun execute(): NetworkResult<AuthSession?> {
        return authRepository.getSavedAuthSession()
    }
}
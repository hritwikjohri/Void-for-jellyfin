package com.hritwik.avoid.domain.usecase.auth

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : BaseUseCase<ChangePasswordUseCase.Params, Unit>(Dispatchers.IO) {

    data class Params(
        val current: String,
        val new: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<Unit> {
        return authRepository.updatePassword(parameters.current, parameters.new)
    }
}
package com.hritwik.avoid.domain.usecase.jellyseer

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCaseNoParams
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class LogoutFromJellyseerUseCase @Inject constructor(
    private val repository: JellyseerRepository,
) : BaseUseCaseNoParams<Unit>(Dispatchers.IO) {

    override suspend fun execute(): NetworkResult<Unit> {
        return repository.logout()
    }
}

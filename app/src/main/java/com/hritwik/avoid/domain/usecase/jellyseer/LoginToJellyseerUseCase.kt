package com.hritwik.avoid.domain.usecase.jellyseer

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerUser
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class LoginToJellyseerUseCase @Inject constructor(
    private val repository: JellyseerRepository,
) : BaseUseCase<LoginToJellyseerUseCase.Params, JellyseerUser>(Dispatchers.IO) {

    data class Params(
        val baseUrl: String,
        val username: String,
        val password: String,
    )

    override suspend fun execute(parameters: Params): NetworkResult<JellyseerUser> {
        return repository.loginWithJellyfin(
            baseUrl = parameters.baseUrl,
            username = parameters.username,
            password = parameters.password,
        )
    }
}

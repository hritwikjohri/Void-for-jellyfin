package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetUserLibrariesUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetUserLibrariesUseCase.Params, List<Library>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<Library>> {
        return libraryRepository.getUserLibraries(parameters.userId, parameters.accessToken)
    }
}
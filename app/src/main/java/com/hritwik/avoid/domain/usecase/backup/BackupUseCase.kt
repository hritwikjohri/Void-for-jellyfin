package com.hritwik.avoid.domain.usecase.backup

import com.hritwik.avoid.data.backup.BackupManager
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import javax.inject.Inject

class BackupUseCase @Inject constructor(
    private val backupManager: BackupManager
) : BaseUseCase<BackupUseCase.Params, Boolean>() {

    data class Params(val action: Action)

    enum class Action { BACKUP, RESTORE }

    override suspend fun execute(parameters: Params): NetworkResult<Boolean> {
        return when (parameters.action) {
            Action.BACKUP -> {
                runCatching { backupManager.createBackup() }.fold(
                    onSuccess = { NetworkResult.Success(true) },
                    onFailure = {
                        val message = it.message ?: "Backup failed"
                        NetworkResult.Error<Boolean>(AppError.Unknown(message), it)
                    }
                )
            }
            Action.RESTORE -> {
                val result = runCatching { backupManager.restoreLatestBackup() }
                if (result.getOrDefault(false)) {
                    NetworkResult.Success(true)
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Restore failed"
                    NetworkResult.Error<Boolean>(AppError.Unknown(message), result.exceptionOrNull())
                }
            }
        }
    }
}


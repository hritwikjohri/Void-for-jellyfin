package com.hritwik.avoid.presentation.viewmodel.media

import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.media.GetPersonDetailUseCase
import com.hritwik.avoid.domain.usecase.media.GetPersonAppearancesUseCase
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.presentation.ui.state.PersonDetailState
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val getPersonDetailUseCase: GetPersonDetailUseCase,
    private val getPersonAppearancesUseCase: GetPersonAppearancesUseCase,
    serverConnectionManager: ServerConnectionManager
) : BaseViewModel(serverConnectionManager) {

    private val _state = MutableStateFlow(PersonDetailState())
    val state: StateFlow<PersonDetailState> = _state.asStateFlow()

    fun loadPersonDetails(personId: String, userId: String, accessToken: String) {
        viewModelScope.launch {
            _state.value = PersonDetailState(isLoading = true)

            // Load person detail
            val detailResult = getPersonDetailUseCase(
                GetPersonDetailUseCase.Params(userId, personId, accessToken)
            )

            when (detailResult) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        personDetail = detailResult.data,
                        isLoading = false,
                        error = null
                    )

                    // Load appearances
                    loadAppearances(personId, userId, accessToken)
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = detailResult.error
                    )
                }

                else -> {}
            }
        }
    }

    private fun loadAppearances(personId: String, userId: String, accessToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAppearances = true)

            val appearancesResult = getPersonAppearancesUseCase(
                GetPersonAppearancesUseCase.Params(userId, personId, accessToken)
            )

            when (appearancesResult) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        appearances = appearancesResult.data,
                        isLoadingAppearances = false
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingAppearances = false
                    )
                }

                else -> {}
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

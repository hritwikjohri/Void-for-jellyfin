package com.hritwik.avoid.domain.provider

import com.hritwik.avoid.domain.model.auth.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionProvider @Inject constructor() {
    private val _authSession = MutableStateFlow<AuthSession?>(null)
    val authSession: StateFlow<AuthSession?> = _authSession.asStateFlow()

    fun updateSession(session: AuthSession?) {
        _authSession.value = session
    }
}
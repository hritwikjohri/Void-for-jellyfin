package com.hritwik.avoid.presentation.ui.state

public sealed class PasswordChangeState {
    public object Idle : PasswordChangeState()
    public object Loading : PasswordChangeState()
    public object Success : PasswordChangeState()
    public data class Error(public val message: String) : PasswordChangeState()
}

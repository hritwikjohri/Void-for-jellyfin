package com.hritwik.avoid.data.connection

import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod

sealed class ServerConnectionEvent {
    data class MethodSwitched(
        val method: ServerConnectionMethod,
        val message: String
    ) : ServerConnectionEvent()

    data class Offline(
        val message: String
    ) : ServerConnectionEvent()
}

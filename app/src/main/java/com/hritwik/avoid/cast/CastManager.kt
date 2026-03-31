package com.hritwik.avoid.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _castContext: CastContext? = null
    val castContext: CastContext?
        get() = _castContext

    private var _castPlayer: CastPlayer? = null
    val castPlayer: CastPlayer? get() = _castPlayer

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _isCasting.value = true
            _castDeviceName.value = session.castDevice?.friendlyName
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _isCasting.value = false
            _castDeviceName.value = null
        }

        override fun onSessionEnding(session: CastSession) {}

        override fun onSessionEnded(session: CastSession, error: Int) {
            _isCasting.value = false
            _castDeviceName.value = null
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {}

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _isCasting.value = true
            _castDeviceName.value = session.castDevice?.friendlyName
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _isCasting.value = false
            _castDeviceName.value = null
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    fun initialize() {
        try {
            _castContext = CastContext.getSharedInstance(context)
            _castPlayer = CastPlayer(_castContext!!, VoidMediaItemConverter())
            _castContext?.sessionManager?.addSessionManagerListener(
                sessionManagerListener,
                CastSession::class.java
            )
            // Check if there's already an active session
            val currentSession = _castContext?.sessionManager?.currentCastSession
            if (currentSession != null && currentSession.isConnected) {
                _isCasting.value = true
                _castDeviceName.value = currentSession.castDevice?.friendlyName
            }
        } catch (e: Exception) {
            // Cast not available on this device (e.g., no Google Play Services)
            _castContext = null
            _castPlayer = null
        }
    }

    fun setSessionAvailabilityListener(listener: SessionAvailabilityListener?) {
        _castPlayer?.setSessionAvailabilityListener(listener)
    }

    fun getSessionManager(): SessionManager? = _castContext?.sessionManager

    fun getCurrentSession(): CastSession? = _castContext?.sessionManager?.currentCastSession

    fun release() {
        _castContext?.sessionManager?.removeSessionManagerListener(
            sessionManagerListener,
            CastSession::class.java
        )
        _castPlayer?.setSessionAvailabilityListener(null)
        _castPlayer?.release()
        _castPlayer = null
    }
}

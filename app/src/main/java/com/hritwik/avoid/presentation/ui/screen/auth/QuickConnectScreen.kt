package com.hritwik.avoid.presentation.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun QuickConnectScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quickState = state.quickConnectState

    BackHandler {
        viewModel.resetQuickConnectState()
        onCancel()
    }

    LaunchedEffect(Unit) {
        viewModel.resetQuickConnectState()
        viewModel.initiateQuickConnect()
    }

    LaunchedEffect(quickState.code) {
        if (quickState.code != null && !quickState.isPolling && state.authSession == null) {
            viewModel.pollQuickConnect()
        }
    }

    LaunchedEffect(state.authSession) {
        if (state.authSession != null) {
            onSuccess()
        }
    }

    AnimatedAmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(calculateRoundedValue(16).sdp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                Text(
                    text = "Quick Connect",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                quickState.code?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (quickState.isPolling) {
                    CircularProgressIndicator()
                }

                quickState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

                OutlinedButton(
                    onClick = {
                        viewModel.resetQuickConnectState()
                        onCancel()
                    },
                    enabled = !quickState.isPolling
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
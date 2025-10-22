package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.presentation.ui.common.ErrorMessageProvider
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ErrorMessage(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val message = ErrorMessageProvider(context).getMessage(error)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(calculateRoundedValue(24).sdp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = stringResource(R.string.error),
                modifier = Modifier.size(calculateRoundedValue(48).sdp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

            Text(
                text = stringResource(R.string.something_went_wrong),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onRetry != null || onDismiss != null) {
                Spacer(modifier = Modifier.height(calculateRoundedValue(24).sdp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    if (onRetry != null) {
                        val retryFocusRequester = remember { FocusRequester() }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .focusRequester(retryFocusRequester)
                                .semantics { role = Role.Button }
                                .focusable()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(calculateRoundedValue(18).sdp)
                            )
                            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                            Text(stringResource(R.string.retry))
                        }
                    }

                    if (onDismiss != null) {
                        val dismissFocusRequester = remember { FocusRequester() }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .focusRequester(dismissFocusRequester)
                                .semantics { role = Role.Button }
                                .focusable()
                        ) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineErrorMessage(
    error: AppError,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val message = ErrorMessageProvider(context).getMessage(error)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            if (onDismiss != null) {
                val dismissFocusRequester = remember { FocusRequester() }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .focusRequester(dismissFocusRequester)
                        .semantics { role = Role.Button }
                        .focusable()
                ) {
                    Text(
                        text = stringResource(R.string.dismiss),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
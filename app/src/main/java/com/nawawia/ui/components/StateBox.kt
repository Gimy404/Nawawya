package com.nawawia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.core.error.AppError
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Universal state contract for screens and data-driven regions.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Empty(
        val title: String,
        val message: String? = null,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : UiState<Nothing>
    data class Error(
        val error: AppError,
        val onRetry: (() -> Unit)? = null
    ) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}

/**
 * Mandatory state wrapper making blank screens impossible to ship by accident.
 */
@Composable
fun <T> StateBox(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val colors = NawawiaTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.5.dp,
                        color = colors.brand
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جارٍ التحميل…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is UiState.Empty -> {
                val cardShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(cardShape)
                        .background(colors.surface, cardShape)
                        .border(2.dp, colors.outline, cardShape)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = colors.inkMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                    if (state.message != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (state.actionLabel != null && state.onAction != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ChunkyButton(
                            label = state.actionLabel,
                            onClick = state.onAction,
                            variant = ChunkyButtonVariant.Tonal,
                            size = ChunkyButtonSize.Medium
                        )
                    }
                }
            }

            is UiState.Error -> {
                val cardShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(cardShape)
                        .background(colors.surface, cardShape)
                        .border(2.dp, colors.wrongSoft, cardShape)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = colors.wrong,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "تنبيه",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error.userMessageAr(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        textAlign = TextAlign.Center
                    )
                    if (state.onRetry != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ChunkyButton(
                            label = "إعادة المحاولة",
                            onClick = state.onRetry,
                            variant = ChunkyButtonVariant.Primary,
                            size = ChunkyButtonSize.Medium
                        )
                    }
                }
            }

            is UiState.Content -> {
                content(state.data)
            }
        }
    }
}

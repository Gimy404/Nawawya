package com.nawawia.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nawawia.ui.components.ChunkyButton
import com.nawawia.ui.components.ChunkyButtonSize
import com.nawawia.ui.components.ChunkyButtonVariant
import com.nawawia.ui.theme.NawawiaTheme

/**
 * Full-screen blocking state shown when any of the 7 Content Integrity Gate checks fail (Section 4.4).
 * Never degrades to partial or generated content.
 */
@Composable
fun ContentUnavailableScreen(
    validationErrorReason: String,
    onDiagnosticsCopied: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = NawawiaTheme.colors
    val clipboard = LocalClipboardManager.current
    val cardShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp)
            .testTag("content_unavailable_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface, cardShape)
                .border(2.dp, colors.wrong, cardShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.GppBad,
                contentDescription = null,
                tint = colors.wrong,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "تعذّر تحميل المحتوى. لا يمكن تشغيل التطبيق بمحتوى غير متحقَّق منه.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("content_unavailable_message")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = validationErrorReason,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("content_unavailable_reason")
            )

            Spacer(modifier = Modifier.height(24.dp))

            ChunkyButton(
                label = "نسخ تفاصيل التشخيص",
                leadingIcon = Icons.Default.ContentCopy,
                onClick = {
                    clipboard.setText(AnnotatedString("Nawawia Integrity Error: $validationErrorReason"))
                    onDiagnosticsCopied()
                },
                variant = ChunkyButtonVariant.Primary,
                size = ChunkyButtonSize.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copy_diagnostics_button")
            )
        }
    }
}

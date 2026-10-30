package com.nawawia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nawawia.ui.theme.DarkNawawiaColors
import com.nawawia.ui.theme.EaseOut
import com.nawawia.ui.theme.NawawiaDurations
import com.nawawia.ui.theme.NawawiaTheme

enum class ChunkyButtonVariant {
    Primary,
    Gold,
    Danger,
    Tonal,
    Ghost
}

enum class ChunkyButtonSize(val minHeight: Dp, val lipDepth: Dp, val cornerRadius: Dp) {
    Medium(minHeight = 48.dp, lipDepth = 4.dp, cornerRadius = 16.dp),
    Large(minHeight = 56.dp, lipDepth = 6.dp, cornerRadius = 16.dp)
}

/**
 * Duolingo-grade tactile button with a solid 3D lip that depresses on touch.
 * On press, content translates downwards by lipDepth and the bottom lip collapses to 0dp
 * inside a constant outer frame so surrounding layout never shifts.
 */
@Composable
fun ChunkyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ChunkyButtonVariant = ChunkyButtonVariant.Primary,
    size: ChunkyButtonSize = ChunkyButtonSize.Large,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = NawawiaTheme.colors
    val reduceMotion = NawawiaTheme.isReduceMotion

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Determine color tokens per variant
    val (faceColor, lipColor, textColor) = when (variant) {
        ChunkyButtonVariant.Primary -> Triple(colors.brand, colors.brandPress, colors.onBrand)
        ChunkyButtonVariant.Gold -> Triple(colors.gold, colors.goldPress, colors.onGold)
        ChunkyButtonVariant.Danger -> Triple(colors.wrong, colors.wrongPress, colors.onWrong)
        ChunkyButtonVariant.Tonal -> Triple(colors.surfaceAlt, colors.outline, colors.ink)
        ChunkyButtonVariant.Ghost -> Triple(Color.Transparent, Color.Transparent, colors.ink)
    }

    val finalFaceColor = if (enabled) faceColor else colors.surfaceAlt
    val finalLipColor = if (enabled) lipColor else Color.Transparent
    val finalTextColor = if (enabled) textColor else colors.inkMuted
    val currentLipDepth = if (enabled && variant != ChunkyButtonVariant.Ghost) size.lipDepth else 0.dp

    // Pressed displacement animation (120ms EaseOut)
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(
            durationMillis = NawawiaDurations.ButtonPress,
            easing = EaseOut
        ),
        label = "chunky_press"
    )

    val shape = RoundedCornerShape(size.cornerRadius)
    val density = LocalDensity.current
    val lipDepthPx = with(density) { currentLipDepth.toPx() }
    val translationY = lipDepthPx * pressProgress

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = size.minHeight)
            .wrapContentHeight()
            .clip(shape)
            .background(finalLipColor, shape)
            .then(
                if (enabled && !loading) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier.semantics {
                        role = Role.Button
                        disabled()
                    }
                }
            )
    ) {
        // Face layer: sits on top of the lip base, translating down by lip depth on press
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = currentLipDepth * (1f - pressProgress))
                .graphicsLayer { this.translationY = translationY }
                .clip(shape)
                .background(finalFaceColor, shape)
                .then(
                    if (variant == ChunkyButtonVariant.Ghost && enabled) {
                        Modifier.border(2.dp, colors.outline, shape)
                    } else Modifier
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = finalTextColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = finalTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = finalTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews: Light, Dark, RTL, and Font Scale 2.0 (using synthetic fixture text)
// ---------------------------------------------------------------------------

@Preview(name = "1. ChunkyButton - Light", showBackground = true, backgroundColor = 0xFFFBF9F4)
@Composable
fun ChunkyButtonPreviewLight() {
    NawawiaTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            ChunkyButton(
                label = "«‹زر تجريبي — TEST FIXTURE›»",
                onClick = {}
            )
        }
    }
}

@Preview(name = "2. ChunkyButton - Dark", showBackground = true, backgroundColor = 0xFF0D1411)
@Composable
fun ChunkyButtonPreviewDark() {
    NawawiaTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            ChunkyButton(
                label = "«‹زر تجريبي ليلي — TEST FIXTURE›»",
                onClick = {}
            )
        }
    }
}

@Preview(name = "3. ChunkyButton - RTL", showBackground = true, backgroundColor = 0xFFFBF9F4)
@Composable
fun ChunkyButtonPreviewRTL() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NawawiaTheme(darkTheme = false) {
            Box(modifier = Modifier.padding(16.dp)) {
                ChunkyButton(
                    label = "«‹زر تجريبي يمين لليسار — TEST FIXTURE›»",
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "4. ChunkyButton - Font Scale 2.0", showBackground = true, backgroundColor = 0xFFFBF9F4)
@Composable
fun ChunkyButtonPreviewFontScale20() {
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = 2.0f
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        NawawiaTheme(darkTheme = false) {
            Box(modifier = Modifier.padding(16.dp)) {
                ChunkyButton(
                    label = "«‹زر خط مضاعف — TEST FIXTURE›»",
                    onClick = {}
                )
            }
        }
    }
}

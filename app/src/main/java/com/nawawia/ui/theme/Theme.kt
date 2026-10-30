package com.nawawia.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

object NawawiaTheme {
    val colors: NawawiaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNawawiaColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val customTypography: NawawiaCustomTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNawawiaCustomTypography.current

    val isReduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current
}

@Composable
fun NawawiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotionOverride: Boolean? = null,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkNawawiaColors else LightNawawiaColors
    val materialColorScheme = colors.toMaterialColorScheme()

    val context = LocalContext.current
    val systemReduceMotion = try {
        val durationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        durationScale == 0f
    } catch (_: Exception) {
        false
    }
    val effectiveReduceMotion = reduceMotionOverride ?: systemReduceMotion

    CompositionLocalProvider(
        LocalNawawiaColors provides colors,
        LocalNawawiaCustomTypography provides NawawiaCustomTypography(),
        LocalReduceMotion provides effectiveReduceMotion
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = NawawiaMaterialTypography,
            content = content
        )
    }
}

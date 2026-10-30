package com.nawawia.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Nawawia brand color tokens as specified in Section 5.2.
 * Dynamic Color (Material You) is strictly disabled to guarantee exact branded aesthetics across all devices.
 */
@Immutable
data class NawawiaColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val inkMuted: Color,
    val outline: Color,
    val brand: Color,
    val brandPress: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val gold: Color,
    val goldPress: Color,
    val onGold: Color,
    val wrong: Color,
    val wrongPress: Color,
    val wrongSoft: Color,
    val onWrong: Color,
    val heart: Color,
    val info: Color,
    val isDark: Boolean
)

val LightNawawiaColors = NawawiaColors(
    bg = Color(0xFFFBF9F4),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF3EFE6),
    ink = Color(0xFF14201B),
    inkMuted = Color(0xFF55655C),
    outline = Color(0xFFDED8C9),
    brand = Color(0xFF12805C),
    brandPress = Color(0xFF0A5C43),
    brandSoft = Color(0xFFD9F2E5),
    onBrand = Color(0xFFFFFFFF),
    gold = Color(0xFFF2B93B),
    goldPress = Color(0xFFC8911F),
    onGold = Color(0xFF1A1407),
    wrong = Color(0xFFC3261C),
    wrongPress = Color(0xFF951A12),
    wrongSoft = Color(0xFFFBE3E0),
    onWrong = Color(0xFFFFFFFF),
    heart = Color(0xFFE0453C),
    info = Color(0xFF0B6E7A),
    isDark = false
)

val DarkNawawiaColors = NawawiaColors(
    bg = Color(0xFF0D1411),
    surface = Color(0xFF151F1A),
    surfaceAlt = Color(0xFF1E2A24),
    ink = Color(0xFFECF3EE),
    inkMuted = Color(0xFF9CAFA5),
    outline = Color(0xFF2C3A33),
    brand = Color(0xFF3BD68E),
    brandPress = Color(0xFF2FB176),
    brandSoft = Color(0xFF10332A),
    onBrand = Color(0xFF06251A), // Role flip: dark ink on bright emerald
    gold = Color(0xFFF7C85A),
    goldPress = Color(0xFFC8911F),
    onGold = Color(0xFF221A06),
    wrong = Color(0xFFFF7066),
    wrongPress = Color(0xFFD64E45),
    wrongSoft = Color(0xFF351817),
    onWrong = Color(0xFF2A0C09), // Role flip: dark ink on bright wrong
    heart = Color(0xFFFF7066),
    info = Color(0xFF4FC3D1),
    isDark = true
)

val LocalNawawiaColors = staticCompositionLocalOf { LightNawawiaColors }

/**
 * Maps Nawawia tokens to Material 3 ColorScheme so M3 components inherit the brand identity.
 */
fun NawawiaColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brand,
            secondary = gold,
            onSecondary = onGold,
            background = bg,
            onBackground = ink,
            surface = surface,
            onSurface = ink,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = inkMuted,
            outline = outline,
            error = wrong,
            onError = onWrong,
            errorContainer = wrongSoft,
            onErrorContainer = wrong
        )
    } else {
        lightColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brandPress,
            secondary = gold,
            onSecondary = onGold,
            background = bg,
            onBackground = ink,
            surface = surface,
            onSurface = ink,
            surfaceVariant = surfaceAlt,
            onSurfaceVariant = inkMuted,
            outline = outline,
            error = wrong,
            onError = onWrong,
            errorContainer = wrongSoft,
            onErrorContainer = wrongPress
        )
    }
}

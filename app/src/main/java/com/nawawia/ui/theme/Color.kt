package com.nawawia.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nawawia.domain.model.UnitColorRole

/**
 * ألوان نواوية — نظام «المَشكاة».
 *
 * اللون هنا مأخوذ من **أصباغ تذهيب المخطوطات**، ولكل صبغة وظيفة دلالية واحدة
 * لا تتغيّر في التطبيق كلّه:
 *
 * | الصبغة | الرمز | الوظيفة |
 * |---|---|---|
 * | لازَوَرْد (Lapis lazuli) | `brand` | الهوية، والفعل الرئيس، والعقدة النشطة |
 * | ذَهَب | `gold` | الإتقان والمكافأة والإنجاز |
 * | زِنْجَفْر (Vermillion) | `wrong` + `rubric` | **حِبر فقط**: الترقيم والتخريج، والخطأ |
 * | زَنْجار (Verdigris) | `correct` | الصواب وحده — لا يحمل هوية |
 * | حِبر العَفْص | `ink` | كل النصوص. بنّي داكن لا أسود محايد |
 * | رَق / نِيل | `bg` | الورق نهارًا، وزرقة الليل ليلًا |
 *
 * الأخضر كان لون الهوية في النسخة السابقة — وهو لون كل تطبيق قرآن وحديث في
 * المتجر. نُقل إلى وظيفته الواحدة «إجابة صحيحة»، وصارت الهوية لازَوَرْد وذهبًا.
 *
 * `Dynamic Color` مُعطَّل عمدًا: الهوية ثابتة على كل جهاز.
 * وكل زوج أدناه مُختبَر في `ColorContrastTest` مقابل WCAG 2.1.
 */
@Immutable
data class NawawiaColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val inkMuted: Color,
    val outline: Color,
    // لازَوَرْد
    val brand: Color,
    val brandPress: Color,
    val brandSoft: Color,
    val onBrand: Color,
    // ذَهَب
    val gold: Color,
    val goldPress: Color,
    val goldSoft: Color,
    val onGold: Color,
    /** ذهب داكن يصلح للنصوص الصغيرة على الخلفية (≥ 4.5:1). */
    val goldInk: Color,
    // زِنْجَفْر — حِبر الترقيم، وهو نفسه لون الخطأ
    val wrong: Color,
    val wrongPress: Color,
    val wrongSoft: Color,
    val onWrong: Color,
    /** حِبر الترقيم في المخطوط: أرقام الأحاديث وسطر التخريج وعناوين الأبواب. */
    val rubric: Color,
    // زَنْجار — الصواب
    val correct: Color,
    val correctPress: Color,
    val correctSoft: Color,
    val onCorrect: Color,
    val heart: Color,
    val info: Color,
    /** ألوان الوحدات الست — كانت `UnitColorRole` مُعرَّفة ولا تُستعمل في الواجهة إطلاقًا. */
    val unitTints: List<Color>,
    /** ألوان شريط العُنوان: تبقى عميقة في الوضعين لأنها تحمل نصًّا أبيض. */
    val unitBands: List<Color>,
    val isDark: Boolean
) {
    fun tintFor(role: UnitColorRole): Color = unitTints[role.ordinal.coerceIn(unitTints.indices)]
    fun bandFor(role: UnitColorRole): Color = unitBands[role.ordinal.coerceIn(unitBands.indices)]
}

/** ألوان شريط العُنوان — واحدة في الوضعين، فالنصّ فوقها أبيض دائمًا. */
private val UnitBandColors = listOf(
    Color(0xFF1B3C86), // لازَوَرْد
    Color(0xFF7A5310), // ذَهَب
    Color(0xFF17603F), // زَنْجار
    Color(0xFF5E2560), // أرجوان
    Color(0xFF8A3C1C), // طين محروق
    Color(0xFF6B571B)  // رمل
)

val LightNawawiaColors = NawawiaColors(
    bg = Color(0xFFF3F1EA),          // رَق — بارد قليلًا، لا كريميّ دافئ
    surface = Color(0xFFFAF8F2),
    surfaceAlt = Color(0xFFEDE9DD),
    ink = Color(0xFF241E16),         // حِبر العَفْص — بنّي داكن
    inkMuted = Color(0xFF6B6054),
    outline = Color(0xFFD8D1C2),
    brand = Color(0xFF1B3C86),
    brandPress = Color(0xFF142C63),
    brandSoft = Color(0xFFE3E8F6),
    onBrand = Color(0xFFFFFFFF),
    gold = Color(0xFFD9A43C),
    goldPress = Color(0xFFA9761B),
    goldSoft = Color(0xFFF6EBD3),
    onGold = Color(0xFF2A1D05),
    goldInk = Color(0xFF8A5F12),
    wrong = Color(0xFFB33018),
    wrongPress = Color(0xFF8A2512),
    wrongSoft = Color(0xFFF7E2DD),
    onWrong = Color(0xFFFFFFFF),
    rubric = Color(0xFFB33018),
    correct = Color(0xFF1C6F4C),
    correctPress = Color(0xFF124A32),
    correctSoft = Color(0xFFDDEEE5),
    onCorrect = Color(0xFFFFFFFF),
    heart = Color(0xFFB33018),
    info = Color(0xFF0B6E7A),
    unitTints = listOf(
        Color(0xFF1B3C86), Color(0xFF8A5F12), Color(0xFF1C6F4C),
        Color(0xFF6B2A6B), Color(0xFF9A4420), Color(0xFF7A6420)
    ),
    unitBands = UnitBandColors,
    isDark = false
)

val DarkNawawiaColors = NawawiaColors(
    bg = Color(0xFF0D1328),          // نِيل — زرقة ليل، لا أسود محايد
    surface = Color(0xFF141B33),
    surfaceAlt = Color(0xFF1B2342),
    ink = Color(0xFFF0EBE0),
    inkMuted = Color(0xFF9AA3BF),
    outline = Color(0xFF2A3254),
    brand = Color(0xFF5B8AE6),
    brandPress = Color(0xFF3F6DC4),
    brandSoft = Color(0xFF1A2545),
    onBrand = Color(0xFF0A1024),     // قلب الدور: حِبر داكن على لازَوَرْد فاتح
    gold = Color(0xFFE3B45A),
    goldPress = Color(0xFFC8912A),
    goldSoft = Color(0xFF2A2415),
    onGold = Color(0xFF1A1405),
    goldInk = Color(0xFFE3B45A),
    wrong = Color(0xFFF2715A),
    wrongPress = Color(0xFFD1543E),
    wrongSoft = Color(0xFF351817),
    onWrong = Color(0xFF2A0C09),     // قلب الدور
    rubric = Color(0xFFF2715A),
    correct = Color(0xFF49C68C),
    correctPress = Color(0xFF2FA06C),
    correctSoft = Color(0xFF11301F),
    onCorrect = Color(0xFF06240F),   // قلب الدور
    heart = Color(0xFFF2715A),
    info = Color(0xFF4FC3D1),
    unitTints = listOf(
        Color(0xFF5B8AE6), Color(0xFFE3B45A), Color(0xFF49C68C),
        Color(0xFFC58BC5), Color(0xFFE39070), Color(0xFFD4BE6A)
    ),
    unitBands = UnitBandColors,
    isDark = true
)

val LocalNawawiaColors = staticCompositionLocalOf { LightNawawiaColors }

/** يربط رموز نواوية بـ Material 3 لترث مكوّناته الهوية نفسها. */
fun NawawiaColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = brand,
            secondary = gold,
            onSecondary = onGold,
            secondaryContainer = goldSoft,
            onSecondaryContainer = gold,
            tertiary = correct,
            onTertiary = onCorrect,
            tertiaryContainer = correctSoft,
            onTertiaryContainer = correct,
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
            secondaryContainer = goldSoft,
            onSecondaryContainer = goldInk,
            tertiary = correct,
            onTertiary = onCorrect,
            tertiaryContainer = correctSoft,
            onTertiaryContainer = correctPress,
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

package com.nawawia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Three bundled Google Fonts families in res/font/:
 * 1. Baloo Bhaijaan 2: Display & UI headings & button labels (chunky, rounded).
 * 2. Readex Pro: Body text, UI, labels, numbers.
 * 3. Noto Naskh Arabic: Matn, narrator, and takhrij lines only.
 */
val BalooBhaijaanFontFamily = FontFamily(
    Font(R.font.baloo_bhaijaan_2, FontWeight.Normal),
    Font(R.font.baloo_bhaijaan_2, FontWeight.Bold),
    Font(R.font.baloo_bhaijaan_2, FontWeight.ExtraBold)
)

val ReadexProFontFamily = FontFamily(
    Font(R.font.readex_pro, FontWeight.Normal),
    Font(R.font.readex_pro, FontWeight.Medium),
    Font(R.font.readex_pro, FontWeight.SemiBold),
    Font(R.font.readex_pro, FontWeight.Bold)
)

val NotoNaskhArabicFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic, FontWeight.Bold)
)

val NawawiaMaterialTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BalooBhaijaanFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = BalooBhaijaanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BalooBhaijaanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BalooBhaijaanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BalooBhaijaanFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

/**
 * Custom typography roles set apart for sacred text and citations.
 */
@Immutable
data class NawawiaCustomTypography(
    val matn: TextStyle = TextStyle(
        fontFamily = NotoNaskhArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 40.sp
    ),
    val matnLarge: TextStyle = TextStyle(
        fontFamily = NotoNaskhArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 48.sp
    ),
    val takhrij: TextStyle = TextStyle(
        fontFamily = ReadexProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
)

val LocalNawawiaCustomTypography = staticCompositionLocalOf { NawawiaCustomTypography() }

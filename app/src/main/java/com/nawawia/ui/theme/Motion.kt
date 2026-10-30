package com.nawawia.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.compositionLocalOf

/**
 * Exact motion tokens defined in Section 5.5.
 */
val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
val EaseSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val SpringPlay = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

object NawawiaDurations {
    const val ButtonPress = 120
    const val AnswerSelect = 160
    const val BannerIn = 200
    const val PathUnlock = 280
    const val BottomSheet = 280
    const val ScreenTransition = 220
    const val XpCounterTick = 600
    const val UnitCelebration = 700
}

/**
 * CompositionLocal indicating if reduced motion is requested by the system or settings.
 * When true, transform-based animations are replaced with instant or subtle crossfades.
 */
val LocalReduceMotion = compositionLocalOf { false }

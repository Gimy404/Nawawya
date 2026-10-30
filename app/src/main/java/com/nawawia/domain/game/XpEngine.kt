package com.nawawia.domain.game

import kotlin.math.min

object XpEngine {
    const val FIRST_TRY_XP = 10
    const val RETRY_XP = 2
    const val UNIT_COMPLETION_BONUS = 50
    const val DAILY_GOAL_BONUS = 20

    /**
     * Computes XP earned for a lesson or review session according to Section 7.2:
     * xp = 10 * correctFirstTry + 2 * correctAfterRetry + unitBonus(50) + dailyGoalBonus(20) + streakMultiplier
     * where streakMultiplier = min(streakDays / 7, 2) * 5
     */
    fun calculateLessonXp(
        correctFirstTry: Int,
        correctAfterRetry: Int,
        isUnitComplete: Boolean = false,
        isDailyGoalReached: Boolean = false,
        streakDays: Int = 0
    ): Int {
        val base = (FIRST_TRY_XP * correctFirstTry.coerceAtLeast(0)) +
            (RETRY_XP * correctAfterRetry.coerceAtLeast(0))
        val unitBonus = if (isUnitComplete) UNIT_COMPLETION_BONUS else 0
        val dailyBonus = if (isDailyGoalReached) DAILY_GOAL_BONUS else 0
        val streakMultiplier = min(streakDays.coerceAtLeast(0) / 7, 2) * 5
        return base + unitBonus + dailyBonus + streakMultiplier
    }
}

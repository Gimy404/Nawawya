package com.nawawia.domain.game

import com.nawawia.core.clock.AppClock
import com.nawawia.domain.model.StreakState
import kotlin.math.max
import kotlin.math.min

object StreakEngine {

    /**
     * Resolves today's epoch day strictly from AppClock without calling LocalDate.now().
     */
    fun currentEpochDay(clock: AppClock): Long {
        return clock.now().atZone(clock.zone()).toLocalDate().toEpochDay()
    }

    /**
     * Records a completed lesson on the current clock day, handling consecutive days,
     * freeze consumption on 2-day gaps, multi-day resets, and anti-cheat clock rollback protection.
     */
    fun recordActivity(current: StreakState, clock: AppClock): StreakState {
        val today = currentEpochDay(clock)
        val last = current.lastActiveEpochDay

        // First-ever lesson
        if (last == null) {
            return current.copy(
                streakDays = 1,
                lastActiveEpochDay = today,
                activeEpochDaysHistory = current.activeEpochDaysHistory + today
            )
        }

        return when {
            // Clock moved backwards (manual rollback, travel across date line, DST edge)
            today < last -> {
                current.copy(
                    streakDays = current.streakDays,
                    lastActiveEpochDay = max(last, today),
                    clockRollbackDiagnosticsCount = current.clockRollbackDiagnosticsCount + 1
                )
            }

            // Same calendar day in current zone
            today == last -> {
                current.copy(
                    activeEpochDaysHistory = current.activeEpochDaysHistory + today
                )
            }

            // Consecutive next day
            today == last + 1L -> {
                val nextStreak = current.streakDays + 1
                val earnedFreeze = if (nextStreak % 7 == 0) 1 else 0
                val nextFreezes = min(StreakState.MAX_FREEZES, current.freezesAvailable + earnedFreeze)
                current.copy(
                    streakDays = nextStreak,
                    lastActiveEpochDay = today,
                    freezesAvailable = nextFreezes,
                    activeEpochDaysHistory = current.activeEpochDaysHistory + today
                )
            }

            // Missed exactly 1 day (2-day gap) with an available freeze
            today == last + 2L && current.freezesAvailable > 0 -> {
                val nextStreak = current.streakDays + 1
                val remainingFreezes = current.freezesAvailable - 1
                val earnedFreeze = if (nextStreak % 7 == 0) 1 else 0
                val nextFreezes = min(StreakState.MAX_FREEZES, remainingFreezes + earnedFreeze)
                current.copy(
                    streakDays = nextStreak,
                    lastActiveEpochDay = today,
                    freezesAvailable = nextFreezes,
                    activeEpochDaysHistory = current.activeEpochDaysHistory + today
                )
            }

            // Missed 1+ days with no freeze or gap > 2 days -> reset streak to 1
            else -> {
                current.copy(
                    streakDays = 1,
                    lastActiveEpochDay = today,
                    activeEpochDaysHistory = current.activeEpochDaysHistory + today
                )
            }
        }
    }
}

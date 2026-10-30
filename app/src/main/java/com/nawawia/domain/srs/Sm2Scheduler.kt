package com.nawawia.domain.srs

import com.nawawia.domain.model.Sm2State
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Sm2Scheduler {

    /**
     * Maps lesson performance (retries & hearts exhausted) to an SM-2 grade in 0..5.
     * - All first-try correct -> 5
     * - One retry -> 4
     * - Two or more retries -> 3
     * - Hearts exhausted -> 2
     */
    fun gradeFromLesson(retriesCount: Int, heartsExhausted: Boolean): Int {
        if (heartsExhausted) return 2
        return when {
            retriesCount <= 0 -> 5
            retriesCount == 1 -> 4
            else -> 3
        }
    }

    /**
     * Pure SM-2 scheduling function.
     * Updates easeFactor (floor 1.3), intervalDays, repetitions, dueEpochDay, and derived masteryPct.
     */
    fun schedule(
        current: Sm2State,
        grade: Int,
        todayEpochDay: Long
    ): Sm2State {
        val clampedGrade = grade.coerceIn(0, 5)
        val qDiff = 5 - clampedGrade
        val efDelta = 0.1 - qDiff * (0.08 + qDiff * 0.02)
        val nextEaseFactor = max(Sm2State.MIN_EASE_FACTOR, current.easeFactor + efDelta)

        val (nextRepetitions, nextIntervalDays) = if (clampedGrade < 3) {
            // Lapse: reset repetitions to 0 and interval to 1 day
            0 to 1
        } else {
            val reps = current.repetitions + 1
            val interval = when (reps) {
                1 -> 1
                2 -> 6
                else -> (current.intervalDays * nextEaseFactor).roundToInt().coerceAtLeast(1)
            }
            reps to interval
        }

        val nextMasteryPct = computeMasteryPct(nextRepetitions, clampedGrade)

        return current.copy(
            easeFactor = nextEaseFactor,
            intervalDays = nextIntervalDays,
            repetitions = nextRepetitions,
            dueEpochDay = todayEpochDay + nextIntervalDays,
            masteryPct = nextMasteryPct,
            lastGrade = clampedGrade
        )
    }

    /**
     * Deterministic mastery percentage derived strictly from repetitions and last grade.
     */
    fun computeMasteryPct(repetitions: Int, lastGrade: Int): Int {
        if (repetitions <= 0 || lastGrade < 3) {
            return if (lastGrade >= 2) 15 else 0
        }
        val baseFromReps = when (repetitions) {
            1 -> 60
            2 -> 80
            else -> 95
        }
        val gradeBonus = when (lastGrade) {
            5 -> 5
            4 -> 0
            else -> -10
        }
        return (baseFromReps + gradeBonus).coerceIn(0, 100)
    }
}

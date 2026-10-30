package com.nawawia.domain.game

import com.nawawia.core.clock.AppClock
import com.nawawia.domain.model.HeartsState
import kotlin.math.min

object HeartsEngine {

    private const val REFILL_INTERVAL_MILLIS = HeartsState.REFILL_INTERVAL_MINUTES * 60_000L

    /**
     * Recomputes current hearts on read based on elapsed time since lastHeartSpentEpochMillis.
     * Advances the anchor timestamp proportionally or clears it if full.
     */
    fun recomputeHearts(stored: HeartsState, clock: AppClock): HeartsState {
        val nowMillis = clock.now().toEpochMilli()
        if (stored.currentHearts >= HeartsState.MAX_HEARTS) {
            return stored.copy(
                currentHearts = HeartsState.MAX_HEARTS,
                lastHeartSpentEpochMillis = null
            )
        }

        val anchor = stored.lastHeartSpentEpochMillis ?: return stored.copy(
            lastHeartSpentEpochMillis = nowMillis
        )

        val elapsedMillis = nowMillis - anchor
        if (elapsedMillis < REFILL_INTERVAL_MILLIS) {
            return stored
        }

        val gainedHearts = (elapsedMillis / REFILL_INTERVAL_MILLIS).toInt()
        val newHearts = min(HeartsState.MAX_HEARTS, stored.currentHearts + gainedHearts)

        return if (newHearts >= HeartsState.MAX_HEARTS) {
            HeartsState(
                currentHearts = HeartsState.MAX_HEARTS,
                lastHeartSpentEpochMillis = null
            )
        } else {
            val newAnchor = anchor + gainedHearts * REFILL_INTERVAL_MILLIS
            HeartsState(
                currentHearts = newHearts,
                lastHeartSpentEpochMillis = newAnchor
            )
        }
    }

    /**
     * Spends 1 heart on a wrong answer. Sets anchor if transitioning from full hearts.
     */
    fun spendHeart(stored: HeartsState, clock: AppClock): HeartsState {
        val current = recomputeHearts(stored, clock)
        if (current.currentHearts <= 0) return current
        val nextCount = current.currentHearts - 1
        val anchor = current.lastHeartSpentEpochMillis ?: clock.now().toEpochMilli()
        return HeartsState(
            currentHearts = nextCount,
            lastHeartSpentEpochMillis = anchor
        )
    }

    /**
     * Grants +1 heart (e.g., on completing a review set), capped at MAX_HEARTS (5).
     */
    fun grantReviewHeart(stored: HeartsState, clock: AppClock): HeartsState {
        val current = recomputeHearts(stored, clock)
        val nextCount = min(HeartsState.MAX_HEARTS, current.currentHearts + 1)
        return if (nextCount >= HeartsState.MAX_HEARTS) {
            HeartsState(
                currentHearts = HeartsState.MAX_HEARTS,
                lastHeartSpentEpochMillis = null
            )
        } else {
            current.copy(currentHearts = nextCount)
        }
    }

    /**
     * Returns remaining seconds until the next single heart refills, or 0 if full.
     */
    fun secondsUntilNextRefill(stored: HeartsState, clock: AppClock): Long {
        val current = recomputeHearts(stored, clock)
        if (current.currentHearts >= HeartsState.MAX_HEARTS) return 0L
        val anchor = current.lastHeartSpentEpochMillis ?: return 0L
        val elapsedMillis = (clock.now().toEpochMilli() - anchor).coerceAtLeast(0L)
        val remainingMillis = (REFILL_INTERVAL_MILLIS - (elapsedMillis % REFILL_INTERVAL_MILLIS))
        return (remainingMillis + 999L) / 1000L
    }
}

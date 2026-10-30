package com.nawawia.domain

import com.google.common.truth.Truth.assertThat
import com.nawawia.core.clock.FakeClock
import com.nawawia.domain.game.HeartsEngine
import com.nawawia.domain.game.StreakEngine
import com.nawawia.domain.game.XpEngine
import com.nawawia.domain.model.HeartsState
import com.nawawia.domain.model.Sm2State
import com.nawawia.domain.model.StreakState
import com.nawawia.domain.security.LinkGuard
import com.nawawia.domain.srs.Sm2Scheduler
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class GameSystemsAndStreakTest {

    // -----------------------------------------------------------------------
    // Section 7.3 — Full 14-Case Streak Test Matrix
    // -----------------------------------------------------------------------

    @Test
    fun streak_01_firstEverLesson_startsAtOne() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val state = StreakEngine.recordActivity(StreakState(), clock)
        assertThat(state.streakDays).isEqualTo(1)
        assertThat(state.lastActiveEpochDay).isNotNull()
    }

    @Test
    fun streak_02_sameDay_doesNotIncrement() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        clock.advanceMinutes(180)
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(1)
    }

    @Test
    fun streak_03_nextDay_incrementsStreakAndGrantsFreezeAtSeven() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        var state = StreakState()
        for (day in 1..7) {
            state = StreakEngine.recordActivity(state, clock)
            clock.advanceDays(1)
        }
        assertThat(state.streakDays).isEqualTo(7)
        assertThat(state.freezesAvailable).isEqualTo(1)
    }

    @Test
    fun streak_04_twoDayGapWithFreeze_consumesFreezeAndIncrements() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val initial = StreakState(
            streakDays = 5,
            lastActiveEpochDay = StreakEngine.currentEpochDay(clock),
            freezesAvailable = 1
        )
        clock.advanceDays(2) // Skip 1 full calendar day
        val next = StreakEngine.recordActivity(initial, clock)
        assertThat(next.streakDays).isEqualTo(6)
        assertThat(next.freezesAvailable).isEqualTo(0)
    }

    @Test
    fun streak_05_twoDayGapWithoutFreeze_resetsToOne() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val initial = StreakState(
            streakDays = 5,
            lastActiveEpochDay = StreakEngine.currentEpochDay(clock),
            freezesAvailable = 0
        )
        clock.advanceDays(2)
        val next = StreakEngine.recordActivity(initial, clock)
        assertThat(next.streakDays).isEqualTo(1)
    }

    @Test
    fun streak_06_fiveDayGap_resetsToOneEvenWithFreezes() {
        val clock = FakeClock(Instant.parse("2026-03-01T10:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val initial = StreakState(
            streakDays = 14,
            lastActiveEpochDay = StreakEngine.currentEpochDay(clock),
            freezesAvailable = 2
        )
        clock.advanceDays(5)
        val next = StreakEngine.recordActivity(initial, clock)
        assertThat(next.streakDays).isEqualTo(1)
    }

    @Test
    fun streak_07_dstForward_handlesSpringForwardCleanly() {
        val zone = ZoneId.of("America/New_York")
        val clock = FakeClock(Instant.parse("2026-03-07T15:00:00Z"), zone)
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        clock.setTime(Instant.parse("2026-03-08T15:00:00Z")) // DST spring forward day
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(2)
    }

    @Test
    fun streak_08_dstBackward_handlesFallBackCleanly() {
        val zone = ZoneId.of("America/New_York")
        val clock = FakeClock(Instant.parse("2026-10-31T15:00:00Z"), zone)
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        clock.setTime(Instant.parse("2026-11-01T15:00:00Z")) // DST fall back day
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(2)
    }

    @Test
    fun streak_09_travelUtcPlus3ToUtcMinus5_doesNotResetWhenLocalDayMovesBack() {
        // Active at 01:00 AM on March 10 in Riyadh (UTC+3) -> 2026-03-09T22:00:00Z
        val clock = FakeClock(Instant.parse("2026-03-09T22:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val s1 = StreakEngine.recordActivity(StreakState(streakDays = 4, lastActiveEpochDay = StreakEngine.currentEpochDay(clock) - 1), clock)
        assertThat(s1.streakDays).isEqualTo(5)

        // User flies to New York (UTC-5/UTC-4) 1 hour later -> local date is still March 9!
        clock.advanceMinutes(60)
        clock.setTimeZone(ZoneId.of("America/New_York"))
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(5)
        assertThat(s2.clockRollbackDiagnosticsCount).isEqualTo(1)
    }

    @Test
    fun streak_10_travelUtcMinus5ToUtcPlus3_advancesDayCleanly() {
        val clock = FakeClock(Instant.parse("2026-03-09T20:00:00Z"), ZoneId.of("America/New_York"))
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        // 6 hours later in Riyadh is March 10
        clock.advanceMinutes(360)
        clock.setTimeZone(ZoneId.of("Asia/Riyadh"))
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(2)
    }

    @Test
    fun streak_11_deviceClockSetBackOneDay_doesNotResetOrReward() {
        val clock = FakeClock(Instant.parse("2026-05-10T12:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val day10 = StreakEngine.currentEpochDay(clock)
        val initial = StreakState(streakDays = 9, lastActiveEpochDay = day10)

        // Manual clock rollback by 1 day
        clock.setTime(Instant.parse("2026-05-09T12:00:00Z"))
        val afterRollback = StreakEngine.recordActivity(initial, clock)
        assertThat(afterRollback.streakDays).isEqualTo(9)
        assertThat(afterRollback.lastActiveEpochDay).isEqualTo(day10)
        assertThat(afterRollback.clockRollbackDiagnosticsCount).isEqualTo(1)
    }

    @Test
    fun streak_12_deviceClockSetForwardOneYear_resetsStreakToOne() {
        val clock = FakeClock(Instant.parse("2026-05-10T12:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val s1 = StreakEngine.recordActivity(StreakState(streakDays = 10, lastActiveEpochDay = StreakEngine.currentEpochDay(clock) - 1), clock)
        clock.advanceDays(365)
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(1)
    }

    @Test
    fun streak_13_yearBoundary_dec31ToJan1IncrementsStreak() {
        val clock = FakeClock(Instant.parse("2026-12-31T12:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        clock.setTime(Instant.parse("2027-01-01T12:00:00Z"))
        val s2 = StreakEngine.recordActivity(s1, clock)
        assertThat(s2.streakDays).isEqualTo(2)
    }

    @Test
    fun streak_14_leapDay_feb28ToFeb29ToMar1IncrementsCleanly() {
        val clock = FakeClock(Instant.parse("2028-02-28T12:00:00Z"), ZoneId.of("Asia/Riyadh"))
        val s1 = StreakEngine.recordActivity(StreakState(), clock)
        clock.setTime(Instant.parse("2028-02-29T12:00:00Z"))
        val s2 = StreakEngine.recordActivity(s1, clock)
        clock.setTime(Instant.parse("2028-03-01T12:00:00Z"))
        val s3 = StreakEngine.recordActivity(s2, clock)
        assertThat(s3.streakDays).isEqualTo(3)
    }

    // -----------------------------------------------------------------------
    // Section 7.1 — Hearts Refill & 3-Day Process Kill
    // -----------------------------------------------------------------------

    @Test
    fun hearts_refillAfterThreeDayProcessKill_restoresFullFiveHearts() {
        val clock = FakeClock(Instant.parse("2026-06-01T10:00:00Z"))
        var hearts = HeartsState(currentHearts = 5)
        hearts = HeartsEngine.spendHeart(hearts, clock)
        hearts = HeartsEngine.spendHeart(hearts, clock)
        hearts = HeartsEngine.spendHeart(hearts, clock)
        assertThat(hearts.currentHearts).isEqualTo(2)

        // Advance 30 minutes -> +1 heart (3)
        clock.advanceMinutes(30)
        val after30Min = HeartsEngine.recomputeHearts(hearts, clock)
        assertThat(after30Min.currentHearts).isEqualTo(3)

        // Simulate 3-day app kill -> restores to 5 and clears anchor
        clock.advanceDays(3)
        val after3Days = HeartsEngine.recomputeHearts(after30Min, clock)
        assertThat(after3Days.currentHearts).isEqualTo(5)
        assertThat(after3Days.lastHeartSpentEpochMillis).isNull()
    }

    // -----------------------------------------------------------------------
    // Section 7.2 & 7.4 — XP & SM-2 Spaced Repetition
    // -----------------------------------------------------------------------

    @Test
    fun xpEngine_calculatesExactFormula() {
        val xp = XpEngine.calculateLessonXp(
            correctFirstTry = 5,
            correctAfterRetry = 1,
            isUnitComplete = true,
            isDailyGoalReached = true,
            streakDays = 14
        )
        // 5*10 + 1*2 + 50 + 20 + min(14/7, 2)*5 = 50 + 2 + 50 + 20 + 10 = 132
        assertThat(xp).isEqualTo(132)
    }

    @Test
    fun sm2Scheduler_handlesGradesZeroThroughFiveAndEaseFactorFloor() {
        var state = Sm2State(hadithId = "h1")
        // Grade 5 first review -> interval 1, reps 1
        state = Sm2Scheduler.schedule(state, grade = 5, todayEpochDay = 100L)
        assertThat(state.repetitions).isEqualTo(1)
        assertThat(state.intervalDays).isEqualTo(1)
        assertThat(state.dueEpochDay).isEqualTo(101L)

        // Grade 4 second review -> interval 6, reps 2
        state = Sm2Scheduler.schedule(state, grade = 4, todayEpochDay = 101L)
        assertThat(state.repetitions).isEqualTo(2)
        assertThat(state.intervalDays).isEqualTo(6)

        //Repeated low grades (0) must never drop easeFactor below 1.3 floor
        for (i in 1..20) {
            state = Sm2Scheduler.schedule(state, grade = 0, todayEpochDay = 110L + i)
        }
        assertThat(state.easeFactor).isEqualTo(Sm2State.MIN_EASE_FACTOR)
        assertThat(state.repetitions).isEqualTo(0)
        assertThat(state.intervalDays).isEqualTo(1)
    }

    // -----------------------------------------------------------------------
    // Section 6.12 — LinkGuard Allowlist Security
    // -----------------------------------------------------------------------

    @Test
    fun linkGuard_allowsScholarlyHostsAndBlocksMaliciousSuffixes() {
        assertThat(LinkGuard.isAllowed("https://dorar.net/hadith/sharh/1")).isTrue()
        assertThat(LinkGuard.isAllowed("https://www.dorar.net/hadith")).isTrue()
        assertThat(LinkGuard.isAllowed("https://islamqa.info/ar/answers/1")).isTrue()
        assertThat(LinkGuard.isAllowed("https://sunnah.com/nawawi40:1")).isTrue()
        assertThat(LinkGuard.isAllowed("https://shamela.ws/book/1")).isTrue()

        // Malicious or invalid URLs must be blocked
        assertThat(LinkGuard.isAllowed("https://dorar.net.evil.com/hadith")).isFalse()
        assertThat(LinkGuard.isAllowed("http://dorar.net/hadith")).isFalse() // HTTP blocked
        assertThat(LinkGuard.isAllowed("https://user:pass@dorar.net/hadith")).isFalse() // Userinfo blocked
        assertThat(LinkGuard.isAllowed("https://dorar.net:8443/hadith")).isFalse() // Non-standard port blocked
    }
}

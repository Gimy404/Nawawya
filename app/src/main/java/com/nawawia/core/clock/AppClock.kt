package com.nawawia.core.clock

import java.time.Instant
import java.time.ZoneId

/**
 * AppClock provides an abstraction over system time.
 * Streaks, hearts refill, and SRS intervals MUST read time through this interface.
 * System.currentTimeMillis() or Instant.now() outside of AppClock implementations are strictly forbidden.
 */
interface AppClock {
    fun now(): Instant
    fun zone(): ZoneId
}

/**
 * Production implementation using java.time with configurable ZoneId.
 */
class SystemAppClock(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : AppClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = zoneId
}

/**
 * Controllable clock for JVM unit tests and deterministic simulation.
 */
class FakeClock(
    var currentInstant: Instant = Instant.parse("2026-10-23T12:00:00Z"),
    var currentZone: ZoneId = ZoneId.of("Asia/Riyadh")
) : AppClock {
    override fun now(): Instant = currentInstant
    override fun zone(): ZoneId = currentZone

    fun advanceSeconds(seconds: Long) {
        currentInstant = currentInstant.plusSeconds(seconds)
    }

    fun advanceMinutes(minutes: Long) {
        currentInstant = currentInstant.plusSeconds(minutes * 60)
    }

    fun advanceDays(days: Long) {
        currentInstant = currentInstant.plusSeconds(days * 86400)
    }

    fun setTime(instant: Instant) {
        currentInstant = instant
    }

    fun setTimeZone(zone: ZoneId) {
        currentZone = zone
    }
}

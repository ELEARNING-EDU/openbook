package com.elearning.openbook.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReadingLogTest {

    private val zone = ZoneId.of("UTC")

    private fun millis(date: LocalDate, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun aSessionAcrossMidnightSplitsBetweenTwoDays() {
        val log = ReadingLog()
        val day1 = LocalDate.of(2026, 10, 25)
        val day2 = LocalDate.of(2026, 10, 26)

        log.addSecond(millis(day1, 23, 59), listening = false, zone = zone)
        log.addSecond(millis(day2, 0, 0), listening = true, zone = zone)

        assertEquals(1, log.totalSeconds(day1))
        assertEquals(1, log.totalSeconds(day2))
        assertEquals(1, log.days.getValue(day1).readingSeconds)
        assertEquals(1, log.days.getValue(day2).listeningSeconds)
    }

    @Test
    fun listeningCountsTowardsTheTotal() {
        val log = ReadingLog()
        val date = LocalDate.of(2026, 10, 26)
        log.addSecond(millis(date), listening = false, zone = zone)
        log.addSecond(millis(date), listening = true, zone = zone)

        assertEquals(2, log.totalSeconds(date))
        assertEquals(1, log.days.getValue(date).readingSeconds)
        assertEquals(1, log.days.getValue(date).listeningSeconds)
    }

    @Test
    fun streakCountsToYesterdayWhenTodayIsNotMet() {
        val log = ReadingLog()
        val today = LocalDate.of(2026, 10, 26)
        for (offset in 1L..3L) {
            log.days[today.minusDays(offset)] = ReadingDay(
                date = today.minusDays(offset),
                readingSeconds = 5 * 60,
            )
        }

        assertEquals(3, log.summary(today, goalMinutes = 5).streak)
        assertEquals(3, log.summary(today, goalMinutes = 5).readingDays)
    }

    @Test
    fun changingTheGoalRecomputesStreakAndDays() {
        val log = ReadingLog()
        val today = LocalDate.of(2026, 10, 26)
        for (offset in 0L..1L) {
            log.days[today.minusDays(offset)] = ReadingDay(
                date = today.minusDays(offset),
                readingSeconds = 6 * 60,
            )
        }

        assertEquals(2, log.summary(today, goalMinutes = 5).streak)
        assertEquals(0, log.summary(today, goalMinutes = 10).streak)
        assertEquals(0, log.summary(today, goalMinutes = 10).readingDays)
    }

    @Test
    fun recordStreakSurvivesAGapAndAShortDay() {
        val log = ReadingLog()
        val monday = LocalDate.of(2026, 10, 19)
        for (offset in 0L..2L) {
            log.days[monday.plusDays(offset)] = ReadingDay(
                monday.plusDays(offset),
                readingSeconds = 5 * 60,
            )
        }
        log.days[monday.plusDays(3)] = ReadingDay(monday.plusDays(3), readingSeconds = 60)
        for (offset in 4L..5L) {
            log.days[monday.plusDays(offset)] = ReadingDay(
                monday.plusDays(offset),
                readingSeconds = 5 * 60,
            )
        }

        assertEquals(3, log.summary(monday.plusDays(5), 5).recordStreak)
    }

    @Test
    fun summaryTotalsWeekMonthAndYear() {
        val log = ReadingLog()
        val today = LocalDate.of(2026, 10, 28)
        log.days[LocalDate.of(2026, 10, 27)] = ReadingDay(today.minusDays(1), readingSeconds = 120)
        log.days[LocalDate.of(2026, 10, 1)] = ReadingDay(today.withDayOfMonth(1), readingSeconds = 60)
        log.days[LocalDate.of(2026, 1, 5)] = ReadingDay(
            LocalDate.of(2026, 1, 5),
            readingSeconds = 30,
            longestSessionSeconds = 30,
        )

        val summary = log.summary(today, goalMinutes = 1)

        assertEquals(120, summary.weekSeconds)
        assertEquals(180, summary.monthSeconds)
        assertEquals(210, summary.yearSeconds)
        assertEquals(30, summary.longestSessionSeconds)
        assertEquals(120, summary.bestDaySeconds)
    }

    @Test
    fun minutesLeftRoundsUpFromRealSeconds() {
        val summary = ReadingSummary(
            todaySeconds = 4 * 60 + 1,
            goalMinutes = 5,
            weekSeconds = 0,
            monthSeconds = 0,
            yearSeconds = 0,
            streak = 0,
            recordStreak = 0,
            readingDays = 0,
            longestSessionSeconds = 0,
            bestDaySeconds = 0,
        )
        assertEquals(1, summary.minutesLeft)
        assertEquals(4, summary.todayMinutes)
        assertFalse(summary.achieved)
    }

    @Test
    fun encodeDecodeKeepsEveryField() {
        val log = ReadingLog()
        log.days[LocalDate.of(2026, 10, 26)] = ReadingDay(
            LocalDate.of(2026, 10, 26),
            readingSeconds = 61,
            listeningSeconds = 9,
            sessions = 2,
            longestSessionSeconds = 40,
        )
        val copy = ReadingLog.decode(log.encode())
        assertEquals(log.days, copy.days)
        assertEquals(70, copy.totalSeconds(LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun emptyWeekIsAllZero() {
        val today = LocalDate.of(2026, 10, 26)
        val week = ReadingLog().week(today)
        assertTrue(week.all { it.minutes == 0 })
        assertEquals(7, week.size)
    }

    @Test
    fun sessionsAreCountedOnTheDayTheyStart() {
        val log = ReadingLog()
        val date = LocalDate.of(2026, 10, 26)
        log.noteSession(date, seconds = 10, newSession = true)
        log.noteSession(date, seconds = 25, newSession = false)
        log.noteSession(date, seconds = 5, newSession = true)

        assertEquals(2, log.days.getValue(date).sessions)
        assertEquals(25, log.days.getValue(date).longestSessionSeconds)
    }
}

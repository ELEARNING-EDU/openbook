package com.example.epubreader.reader

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** One local day of reading: screen time and narrated time are counted apart. */
data class ReadingDay(
    val date: LocalDate,
    val readingSeconds: Int = 0,
    val listeningSeconds: Int = 0,
    val sessions: Int = 0,
    val longestSessionSeconds: Int = 0,
) {
    val totalSeconds: Int get() = readingSeconds + listeningSeconds
}

/** A week column: Monday..Sunday of the current week, in minutes. */
data class WeekPoint(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val minutes: Int,
    val isToday: Boolean,
)

data class ReadingSummary(
    val todaySeconds: Int,
    val goalMinutes: Int,
    val weekSeconds: Int,
    val monthSeconds: Int,
    val yearSeconds: Int,
    val streak: Int,
    val recordStreak: Int,
    val readingDays: Int,
    val longestSessionSeconds: Int,
    val bestDaySeconds: Int,
) {
    val goalSeconds: Int get() = goalMinutes * 60
    val achieved: Boolean get() = todaySeconds >= goalSeconds
    /** Whole minutes shown in the card; a reader is never told "0 phút" mid-sentence. */
    val todayMinutes: Int get() = todaySeconds / 60
    val minutesLeft: Int get() = ((goalSeconds - todaySeconds) + 59) / 60
}

/**
 * The reading-time ledger: one bucket per local day, filled a second at a time.
 *
 * Pure JVM so the overnight split, the streak rule and the week's shape are unit-tested
 * without a device. Persistence lives in [com.example.epubreader.library.ReadingLogStore].
 */
class ReadingLog(val days: MutableMap<LocalDate, ReadingDay> = LinkedHashMap()) {

    fun day(date: LocalDate): ReadingDay = days.getOrPut(date) { ReadingDay(date) }

    /** One second of reading or listening, at the local date that second fell on. */
    fun addSecond(nowMillis: Long, listening: Boolean, zone: ZoneId = ZoneId.systemDefault()) {
        val date = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val current = day(date)
        days[date] = if (listening) {
            current.copy(listeningSeconds = current.listeningSeconds + 1)
        } else {
            current.copy(readingSeconds = current.readingSeconds + 1)
        }
    }

    fun noteSession(date: LocalDate, seconds: Int, newSession: Boolean) {
        val current = day(date)
        days[date] = current.copy(
            sessions = current.sessions + if (newSession) 1 else 0,
            longestSessionSeconds = maxOf(current.longestSessionSeconds, seconds),
        )
    }

    fun totalSeconds(date: LocalDate): Int = days[date]?.totalSeconds ?: 0

    fun summary(today: LocalDate, goalMinutes: Int): ReadingSummary {
        val goalSeconds = goalMinutes * 60
        val weekStart = today.with(DayOfWeek.MONDAY)
        val monthStart = today.withDayOfMonth(1)
        val yearStart = today.withDayOfYear(1)
        var week = 0
        var month = 0
        var year = 0
        var longestSession = 0
        var bestDay = 0
        for ((date, entry) in days) {
            if (!date.isBefore(weekStart)) week += entry.totalSeconds
            if (!date.isBefore(monthStart)) month += entry.totalSeconds
            if (!date.isBefore(yearStart)) year += entry.totalSeconds
            longestSession = maxOf(longestSession, entry.longestSessionSeconds)
            bestDay = maxOf(bestDay, entry.totalSeconds)
        }
        return ReadingSummary(
            todaySeconds = totalSeconds(today),
            goalMinutes = goalMinutes,
            weekSeconds = week,
            monthSeconds = month,
            yearSeconds = year,
            streak = streak(today, goalSeconds),
            recordStreak = recordStreak(goalSeconds),
            readingDays = days.values.count { it.totalSeconds >= goalSeconds },
            longestSessionSeconds = longestSession,
            bestDaySeconds = bestDay,
        )
    }

    /**
     * Consecutive days at or over the goal. A day still in progress does not break the
     * chain: today counts only once it is met, and otherwise the run ends yesterday.
     */
    fun streak(today: LocalDate, goalSeconds: Int): Int {
        var cursor = if (totalSeconds(today) >= goalSeconds) today else today.minusDays(1)
        var run = 0
        while (totalSeconds(cursor) >= goalSeconds) {
            run++
            cursor = cursor.minusDays(1)
        }
        return run
    }

    fun recordStreak(goalSeconds: Int): Int {
        if (days.isEmpty()) return 0
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in days.keys.sorted()) {
            run = if (previous != null && previous.plusDays(1) == date &&
                totalSeconds(date) >= goalSeconds
            ) {
                run + 1
            } else if (totalSeconds(date) >= goalSeconds) 1 else 0
            if (totalSeconds(date) >= goalSeconds) best = maxOf(best, run)
            previous = date
        }
        return best
    }

    /** Monday..Sunday of the week that holds [today]; days after today read zero. */
    fun week(today: LocalDate): List<WeekPoint> {
        val monday = today.with(DayOfWeek.MONDAY)
        return (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            val minutes = if (date.isAfter(today)) 0 else totalSeconds(date) / 60
            WeekPoint(date, date.dayOfWeek, minutes, date == today)
        }
    }

    /** `date|reading|listening|sessions|longest`, one line per day, oldest first. */
    fun encode(): String = days.keys.sorted().joinToString("\n") { date ->
        val entry = days.getValue(date)
        "$date|${entry.readingSeconds}|${entry.listeningSeconds}|" +
            "${entry.sessions}|${entry.longestSessionSeconds}"
    }

    companion object {
        fun decode(raw: String?): ReadingLog {
            val log = ReadingLog()
            if (raw.isNullOrBlank()) return log
            for (line in raw.lineSequence()) {
                val parts = line.split('|')
                if (parts.size != 5) continue
                val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: continue
                val reading = parts[1].toIntOrNull() ?: continue
                val listening = parts[2].toIntOrNull() ?: continue
                val sessions = parts[3].toIntOrNull() ?: 0
                val longest = parts[4].toIntOrNull() ?: 0
                log.days[date] = ReadingDay(
                    date = date,
                    readingSeconds = reading.coerceAtLeast(0),
                    listeningSeconds = listening.coerceAtLeast(0),
                    sessions = sessions.coerceAtLeast(0),
                    longestSessionSeconds = longest.coerceAtLeast(0),
                )
            }
            return log
        }
    }
}

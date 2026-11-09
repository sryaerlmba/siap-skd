package com.belajar.cpns.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Streak {
    /** Hari berturut-turut dengan aktivitas, dihitung mundur dari hari ini (atau kemarin jika hari ini belum). */
    fun compute(timestamps: List<Long>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = timestamps.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
        if (days.isEmpty()) return 0
        var cursor = if (today in days) today else today.minusDays(1)
        var n = 0
        while (cursor in days) { n++; cursor = cursor.minusDays(1) }
        return n
    }

    fun doneToday(timestamps: List<Long>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Boolean =
        timestamps.any { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today }
}

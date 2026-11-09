package com.belajar.cpns.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 9)
    private fun at(d: LocalDate) = d.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `tiga hari berturut-turut termasuk hari ini`() {
        val ts = listOf(at(today), at(today.minusDays(1)), at(today.minusDays(2)), at(today.minusDays(5)))
        assertEquals(3, Streak.compute(ts, today, zone))
        assertTrue(Streak.doneToday(ts, today, zone))
    }

    @Test
    fun `hari ini belum, streak dihitung dari kemarin`() {
        val ts = listOf(at(today.minusDays(1)), at(today.minusDays(2)))
        assertEquals(2, Streak.compute(ts, today, zone))
        assertFalse(Streak.doneToday(ts, today, zone))
    }

    @Test
    fun `putus dua hari berarti nol`() {
        val ts = listOf(at(today.minusDays(2)), at(today.minusDays(3)))
        assertEquals(0, Streak.compute(ts, today, zone))
    }

    @Test
    fun `dua sesi di hari yang sama dihitung satu hari`() {
        val ts = listOf(at(today), at(today) + 3600_000, at(today.minusDays(1)))
        assertEquals(2, Streak.compute(ts, today, zone))
    }
}

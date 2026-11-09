package com.belajar.cpns.domain

import com.belajar.cpns.data.model.AnswerOption
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.PassingGrade
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    private val config = ScoringConfig(
        year = 2024,
        counts = mapOf(Subtest.TWK to 30, Subtest.TIU to 35, Subtest.TKP to 45),
        durationSec = 6000,
        durationSecNetra = 7800,
        categories = mapOf(
            ApplicantCategory.UMUM to PassingGrade("Umum", pgTWK = 65, pgTIU = 80, pgTKP = 166),
            ApplicantCategory.CUMLAUDE to PassingGrade("Cumlaude", pgCumulative = 311, pgTIU = 85),
            ApplicantCategory.DISABILITAS to PassingGrade("Disabilitas", pgCumulative = 286, pgTIU = 60)
        )
    )

    private fun mc(id: String, st: Subtest, keyIndex: Int = 0) = Question(
        id, st, "g", "t", "stem", listOf("A", "B", "C", "D", "E").mapIndexed { i, k ->
            AnswerOption(k, "opsi $k", if (i == keyIndex) 5 else 0, "")
        }, "", 2, ""
    )

    private fun tkp(id: String) = Question(
        id, Subtest.TKP, "g", "t", "stem",
        listOf("A" to 1, "B" to 2, "C" to 3, "D" to 4, "E" to 5).map { (k, s) -> AnswerOption(k, "opsi $k", s, "r") },
        "", 2, ""
    )

    private val twk = (1..30).map { mc("twk$it", Subtest.TWK) }
    private val tiu = (1..35).map { mc("tiu$it", Subtest.TIU) }
    private val tkp = (1..45).map { tkp("tkp$it") }
    private val all = twk + tiu + tkp

    @Test
    fun `benar 5 salah 0 untuk TWK dan TIU, TKP ikut skor opsi, kosong 0`() {
        val answers = mutableMapOf<String, String?>()
        twk.take(13).forEach { answers[it.id] = "A" }      // 13 benar = 65
        twk.drop(13).forEach { answers[it.id] = "B" }      // salah
        tiu.take(16).forEach { answers[it.id] = "A" }      // 16 benar = 80
        tkp.take(40).forEach { answers[it.id] = "D" }      // 40 x 4 = 160
        tkp.drop(40).take(3).forEach { answers[it.id] = "E" } // 3 x 5 = 15 -> 175
        // 2 TKP kosong

        val r = Scoring.score(all, answers, config, ApplicantCategory.UMUM)
        assertEquals(65, r.of(Subtest.TWK).score)
        assertEquals(80, r.of(Subtest.TIU).score)
        assertEquals(175, r.of(Subtest.TKP).score)
        assertEquals(320, r.total)
        assertEquals(43, r.of(Subtest.TKP).answered)
        assertTrue(r.passAll)
    }

    @Test
    fun `gugur jika satu subtes di bawah ambang walau total tinggi`() {
        val answers = mutableMapOf<String, String?>()
        twk.take(12).forEach { answers[it.id] = "A" }   // 60 < 65
        tiu.take(20).forEach { answers[it.id] = "A" }   // 100
        tkp.forEach { answers[it.id] = "E" }            // 225
        val r = Scoring.score(all, answers, config, ApplicantCategory.UMUM)
        assertEquals(385, r.total)
        assertFalse(r.of(Subtest.TWK).pass)
        assertTrue(r.of(Subtest.TIU).pass)
        assertTrue(r.of(Subtest.TKP).pass)
        assertFalse(r.passAll)
        assertEquals(-5, r.of(Subtest.TWK).margin)
    }

    @Test
    fun `kategori cumlaude memakai kumulatif 311 dan TIU minimal 85`() {
        val answers = mutableMapOf<String, String?>()
        twk.take(10).forEach { answers[it.id] = "A" }   // 50 (di bawah 65, tapi tidak dipakai)
        tiu.take(17).forEach { answers[it.id] = "A" }   // 85
        tkp.take(36).forEach { answers[it.id] = "E" }   // 180 -> total 315
        val r = Scoring.score(all, answers, config, ApplicantCategory.CUMLAUDE)
        assertEquals(315, r.total)
        assertTrue(r.passAll)

        // TIU 80 < 85 -> gugur meski total cukup
        tiu.take(17).forEach { answers[it.id] = "B" }
        tiu.take(16).forEach { answers[it.id] = "A" }
        tkp.forEach { answers[it.id] = "E" }
        val r2 = Scoring.score(all, answers, config, ApplicantCategory.CUMLAUDE)
        assertTrue(r2.total >= 311)
        assertFalse(r2.passAll)
    }

    @Test
    fun `nilai maksimal 150 175 225`() {
        assertEquals(150, config.maxScore(Subtest.TWK))
        assertEquals(175, config.maxScore(Subtest.TIU))
        assertEquals(225, config.maxScore(Subtest.TKP))
        assertEquals(110, config.totalQuestions)
    }

    @Test
    fun `tie breaker TKP lalu TIU lalu TWK`() {
        fun res(twk: Int, tiu: Int, tkp: Int): SkdResult {
            val a = mutableMapOf<String, String?>()
            this.twk.take(twk / 5).forEach { a[it.id] = "A" }
            this.tiu.take(tiu / 5).forEach { a[it.id] = "A" }
            this.tkp.take(tkp / 5).forEach { a[it.id] = "E" }
            return Scoring.score(all, a, config, ApplicantCategory.UMUM)
        }
        val x = res(100, 100, 150) // total 350
        val y = res(90, 100, 160)  // total 350, TKP lebih tinggi
        val sorted = listOf(x, y).sortedWith(Scoring.tieBreaker)
        assertEquals(y, sorted.first())
    }

    @Test
    fun `shortfall menghitung poin dan perkiraan soal`() {
        val s = SubtestScore(Subtest.TKP, 158, 225, 166, false, 45, 45)
        assertEquals(8 to 4, Scoring.shortfall(s))
        val t = SubtestScore(Subtest.TIU, 70, 175, 80, false, 35, 35)
        assertEquals(10 to 2, Scoring.shortfall(t))
        assertEquals(null, Scoring.shortfall(SubtestScore(Subtest.TWK, 90, 150, 65, true, 30, 30)))
    }
}

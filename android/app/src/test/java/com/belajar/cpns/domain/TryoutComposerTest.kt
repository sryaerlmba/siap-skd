package com.belajar.cpns.domain

import com.belajar.cpns.data.model.AnswerOption
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.PassingGrade
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TryoutComposerTest {

    private val config = ScoringConfig(
        2024, mapOf(Subtest.TWK to 30, Subtest.TIU to 35, Subtest.TKP to 45), 6000, 7800,
        mapOf(ApplicantCategory.UMUM to PassingGrade("Umum", 65, 80, 166))
    )

    private fun q(id: String, st: Subtest, tag: String) = Question(
        id, st, "g", tag, "stem", listOf(AnswerOption("A", "a", 5, "")), "", 2, ""
    )

    private val pool = buildList {
        repeat(60) { add(q("twk-a-$it", Subtest.TWK, "pancasila")) }
        repeat(60) { add(q("twk-b-$it", Subtest.TWK, "uud_1945")) }
        repeat(30) { add(q("twk-c-$it", Subtest.TWK, "ejaan")) }
        repeat(105) { add(q("tiu-v-$it", Subtest.TIU, "verbal")) }
        repeat(175) { add(q("tiu-n-$it", Subtest.TIU, "numerik")) }
        repeat(70) { add(q("tiu-f-$it", Subtest.TIU, "figural")) }
        repeat(450) { add(q("tkp-$it", Subtest.TKP, "tkp${it % 6}")) }
    }

    @Test
    fun `tryout berisi 30 35 45 dengan urutan TWK TIU TKP`() {
        val items = TryoutComposer.composeTryout(pool, emptySet(), config, Random(1))
        assertEquals(110, items.size)
        assertEquals(30, items.count { it.subtest == Subtest.TWK })
        assertEquals(35, items.count { it.subtest == Subtest.TIU })
        assertEquals(45, items.count { it.subtest == Subtest.TKP })
        assertEquals(Subtest.TWK, items.first().subtest)
        assertEquals(Subtest.TKP, items.last().subtest)
        assertEquals(110, items.map { it.id }.toSet().size)
    }

    @Test
    fun `komposisi tag proporsional dengan isi bank`() {
        val items = TryoutComposer.composeTryout(pool, emptySet(), config, Random(2))
        val twk = items.filter { it.subtest == Subtest.TWK }
        // 60/150 -> 12, 60/150 -> 12, 30/150 -> 6
        assertEquals(12, twk.count { it.tag == "pancasila" })
        assertEquals(12, twk.count { it.tag == "uud_1945" })
        assertEquals(6, twk.count { it.tag == "ejaan" })
    }

    @Test
    fun `soal yang sudah dilihat dihindari selama masih ada yang baru`() {
        val first = TryoutComposer.composeTryout(pool, emptySet(), config, Random(3))
        val seen = first.map { it.id }.toSet()
        val second = TryoutComposer.composeTryout(pool, seen, config, Random(4))
        assertTrue(second.none { it.id in seen })
    }

    @Test
    fun `kalau soal baru habis, soal lama dipakai lagi tanpa error`() {
        val small = pool.filter { it.subtest == Subtest.TWK }.take(20) +
            pool.filter { it.subtest == Subtest.TIU }.take(35) +
            pool.filter { it.subtest == Subtest.TKP }.take(45)
        val items = TryoutComposer.composeTryout(small, small.map { it.id }.toSet(), config, Random(5))
        assertEquals(20 + 35 + 45, items.size)
    }
}

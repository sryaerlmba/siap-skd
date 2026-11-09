package com.belajar.cpns.domain

import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import kotlin.random.Random

/**
 * Menyusun paket soal. Tryout: 30/35/45 dengan komposisi tag proporsional dan mengutamakan soal
 * yang belum pernah muncul. Urutan soal mengikuti CAT: TWK, lalu TIU, lalu TKP.
 */
object TryoutComposer {

    fun composeTryout(
        pool: List<Question>,
        seen: Set<String>,
        config: ScoringConfig,
        random: Random = Random.Default
    ): List<Question> = Subtest.entries.flatMap { st ->
        pick(pool.filter { it.subtest == st }, seen, config.counts.getValue(st), random)
    }

    /**
     * Memilih [count] soal dari [pool] dengan komposisi per tag sebanding dengan isi bank.
     * Soal yang belum pernah dilihat didahulukan; kalau habis, soal lama dipakai lagi.
     */
    fun pick(pool: List<Question>, seen: Set<String>, count: Int, random: Random = Random.Default): List<Question> {
        if (pool.isEmpty() || count <= 0) return emptyList()
        val byTag = pool.groupBy { it.tag }
        val total = pool.size
        // Kuota per tag (pembulatan ke bawah), sisanya dibagi ke tag dengan pecahan terbesar.
        val quota = byTag.mapValues { (_, qs) -> qs.size * count / total }.toMutableMap()
        var remaining = count - quota.values.sum()
        byTag.entries
            .sortedByDescending { (tag, qs) -> qs.size * count % total }
            .forEach { (tag, _) -> if (remaining > 0) { quota[tag] = quota.getValue(tag) + 1; remaining-- } }

        val chosen = mutableListOf<Question>()
        quota.forEach { (tag, n) ->
            val qs = byTag.getValue(tag)
            val fresh = qs.filter { it.id !in seen }.shuffled(random)
            val stale = qs.filter { it.id in seen }.shuffled(random)
            chosen += (fresh + stale).take(n)
        }
        // Kalau ada tag yang soalnya kurang dari kuota, tambal dari tag lain.
        if (chosen.size < count) {
            val used = chosen.map { it.id }.toSet()
            val rest = pool.filter { it.id !in used }
            chosen += (rest.filter { it.id !in seen }.shuffled(random) + rest.filter { it.id in seen }.shuffled(random))
                .take(count - chosen.size)
        }
        return chosen.shuffled(random)
    }

    /** Kuis harian: fokus ke tag lemah (70%) dicampur acak (30%). */
    fun composeDaily(
        pool: List<Question>,
        seen: Set<String>,
        weakTags: List<String>,
        counts: Map<Subtest, Int>,
        random: Random = Random.Default
    ): List<Question> = Subtest.entries.flatMap { st ->
        val n = counts[st] ?: 0
        val subPool = pool.filter { it.subtest == st }
        val weakPool = subPool.filter { it.tag in weakTags }
        val focus = (n * 7 + 9) / 10
        val fromWeak = pick(weakPool, seen, minOf(focus, weakPool.size), random)
        val rest = pick(subPool.filter { q -> fromWeak.none { it.id == q.id } }, seen, n - fromWeak.size, random)
        (fromWeak + rest).shuffled(random)
    }
}

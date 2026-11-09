package com.belajar.cpns.domain

import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest

data class SubtestScore(
    val subtest: Subtest,
    val score: Int,
    val max: Int,
    val passingGrade: Int?,
    val pass: Boolean,
    val answered: Int,
    val total: Int
) {
    /** Selisih terhadap ambang; negatif berarti kurang. Null jika kategori tidak memakai ambang per subtes. */
    val margin: Int? get() = passingGrade?.let { score - it }
}

data class SkdResult(
    val perSubtest: Map<Subtest, SubtestScore>,
    val total: Int,
    val passAll: Boolean,
    val category: ApplicantCategory
) {
    fun of(subtest: Subtest): SubtestScore = perSubtest.getValue(subtest)
}

/**
 * Meniru aturan penilaian CAT BKN (KepmenPANRB 321/2024):
 * TWK/TIU benar 5 salah 0, TKP tiap opsi 1-5 kosong 0, dan setiap subtes harus lolos ambangnya sendiri
 * untuk kategori umum; kategori cumlaude/disabilitas memakai ambang kumulatif + TIU minimal.
 */
object Scoring {

    fun score(
        questions: List<Question>,
        answers: Map<String, String?>,
        config: ScoringConfig,
        category: ApplicantCategory
    ): SkdResult {
        val pg = config.passingGrade(category)
        val bySubtest = questions.groupBy { it.subtest }
        val raw = Subtest.entries.associateWith { st ->
            val qs = bySubtest[st].orEmpty()
            val score = qs.sumOf { it.scoreFor(answers[it.id]) }
            val answered = qs.count { answers[it.id] != null }
            Triple(score, answered, qs.size)
        }
        val total = raw.values.sumOf { it.first }

        val perSubtest = Subtest.entries.associateWith { st ->
            val (score, answered, count) = raw.getValue(st)
            val threshold = when (st) {
                Subtest.TWK -> pg.pgTWK
                Subtest.TIU -> pg.pgTIU
                Subtest.TKP -> pg.pgTKP
            }
            SubtestScore(
                subtest = st,
                score = score,
                max = config.maxScore(st),
                passingGrade = threshold,
                pass = threshold?.let { score >= it } ?: true,
                answered = answered,
                total = count
            )
        }

        val passAll = if (pg.perSubtest) {
            perSubtest.values.all { it.pass }
        } else {
            total >= (pg.pgCumulative ?: 0) && perSubtest.getValue(Subtest.TIU).pass
        }
        return SkdResult(perSubtest, total, passAll, category)
    }

    /** Urutan untuk nilai sama (aturan CAT): TKP, lalu TIU, lalu TWK tertinggi. */
    val tieBreaker: Comparator<SkdResult> = compareByDescending<SkdResult> { it.total }
        .thenByDescending { it.of(Subtest.TKP).score }
        .thenByDescending { it.of(Subtest.TIU).score }
        .thenByDescending { it.of(Subtest.TWK).score }

    /** Berapa poin lagi yang dibutuhkan subtes ini, dan kira-kira berapa soal. */
    fun shortfall(s: SubtestScore): Pair<Int, Int>? {
        val m = s.margin ?: return null
        if (m >= 0) return null
        val need = -m
        val perQuestion = if (s.subtest == Subtest.TKP) 2 else 5 // TKP: naik dari ~3 ke 5 per soal
        return need to ((need + perQuestion - 1) / perQuestion)
    }
}

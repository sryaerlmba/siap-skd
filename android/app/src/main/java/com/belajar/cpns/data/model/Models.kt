package com.belajar.cpns.data.model

enum class Subtest(val label: String, val fullName: String) {
    TWK("TWK", "Tes Wawasan Kebangsaan"),
    TIU("TIU", "Tes Intelegensia Umum"),
    TKP("TKP", "Tes Karakteristik Pribadi");

    companion object {
        fun from(value: String): Subtest = entries.first { it.name == value }
    }
}

data class AnswerOption(
    val key: String,
    val text: String,
    val score: Int,
    val rationale: String
)

data class Question(
    val id: String,
    val subtest: Subtest,
    val group: String,
    val tag: String,
    val stem: String,
    val options: List<AnswerOption>,
    val explanation: String,
    val difficulty: Int,
    val source: String
) {
    /** Skor maksimal yang bisa didapat dari soal ini (5 untuk semua subtes). */
    val maxScore: Int get() = options.maxOf { it.score }

    fun scoreFor(key: String?): Int = options.firstOrNull { it.key == key }?.score ?: 0
}

/** Kategori pelamar menentukan aturan ambang batas yang dipakai. */
enum class ApplicantCategory { UMUM, CUMLAUDE, DISABILITAS }

/** Ambang batas untuk satu kategori. Kategori umum memakai ambang per subtes, kategori lain kumulatif + TIU. */
data class PassingGrade(
    val label: String,
    val pgTWK: Int? = null,
    val pgTIU: Int? = null,
    val pgTKP: Int? = null,
    val pgCumulative: Int? = null
) {
    val perSubtest: Boolean get() = pgTWK != null && pgTKP != null
}

data class ScoringConfig(
    val year: Int,
    val counts: Map<Subtest, Int>,
    val durationSec: Int,
    val durationSecNetra: Int,
    val categories: Map<ApplicantCategory, PassingGrade>
) {
    val totalQuestions: Int get() = counts.values.sum()
    fun maxScore(subtest: Subtest): Int = (counts[subtest] ?: 0) * 5
    fun passingGrade(category: ApplicantCategory): PassingGrade = categories.getValue(category)
}

enum class SessionType { TRYOUT, DAILY, PRACTICE }

object SessionStatus {
    const val RUNNING = "RUNNING"
    const val FINISHED = "FINISHED"
}

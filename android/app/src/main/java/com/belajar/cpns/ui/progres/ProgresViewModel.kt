package com.belajar.cpns.ui.progres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.SessionEntity
import com.belajar.cpns.data.db.SessionResultEntity
import com.belajar.cpns.data.db.TagStatEntity
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.PassingGrade
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.Streak
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.strength
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgresUiState(
    val tryouts: List<SessionResultEntity> = emptyList(),   // terbaru dulu
    val sessions: List<SessionEntity> = emptyList(),
    val tagStats: List<TagStatEntity> = emptyList(),
    val streak: Int = 0,
    val dailyCount: Int = 0,
    val category: ApplicantCategory = ApplicantCategory.UMUM,
    val examDate: Long = 0L
) {
    /** Perkiraan skor SKD dari rasio skor per subtes di semua latihan, bukan hanya tryout. */
    fun estimate(config: ScoringConfig): Int = Subtest.entries.sumOf { st ->
        val stats = tagStats.filter { it.subtest == st.name }
        val max = stats.sumOf { it.maxSum }
        if (max == 0) 0 else (stats.sumOf { it.scoreSum }.toDouble() / max * config.maxScore(st)).toInt()
    }
}

class ProgresViewModel(
    private val questions: QuestionRepository,
    sessions: SessionRepository,
    preferences: AppPreferences
) : ViewModel() {

    val config: ScoringConfig get() = questions.scoringConfig
    fun passingGrade(category: ApplicantCategory): PassingGrade = config.passingGrade(category)

    val ui: StateFlow<ProgresUiState> = combine(
        sessions.tryoutResults(),
        sessions.finishedSessions(),
        sessions.tagStats(),
        sessions.activityTimestamps(),
        combine(preferences.category, preferences.examDate) { c, d -> c to d }
    ) { tryouts, sess, stats, ts, (cat, date) ->
        ProgresUiState(
            tryouts = tryouts,
            sessions = sess,
            tagStats = stats.sortedBy { it.strength().ordinal * 10 + (it.scoreSum.toDouble() / it.maxSum.coerceAtLeast(1) * 9).toInt() },
            streak = Streak.compute(ts),
            dailyCount = sess.count { it.type == SessionType.DAILY.name },
            category = cat,
            examDate = date
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgresUiState())
}

fun TagStatEntity.isWeak(): Boolean = strength() == Strength.LEMAH

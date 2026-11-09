package com.belajar.cpns.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.SkdResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mode latihan dengan umpan balik langsung: dipakai kuis harian dan latihan per materi.
 * Setelah memilih, skor semua opsi tampil, lalu lanjut ke soal berikutnya.
 */
data class QuizUiState(
    val loading: Boolean = true,
    val sessionId: Long = 0L,
    val title: String = "",
    val type: SessionType = SessionType.PRACTICE,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: String? = null,
    val revealed: Boolean = false,
    val perQuestionSec: Int = 0,
    val secondsLeft: Int = 0,
    val scores: List<Int> = emptyList(),
    val finished: Boolean = false,
    val result: SkdResult? = null
) {
    val current: Question? get() = questions.getOrNull(index)
    val runningAverage: Double get() {
        val tkp = questions.take(scores.size).zip(scores).filter { it.first.subtest == Subtest.TKP }
        return if (tkp.isEmpty()) 0.0 else tkp.sumOf { it.second }.toDouble() / tkp.size
    }
    val correctCount: Int get() = questions.take(scores.size).zip(scores).count { it.first.subtest != Subtest.TKP && it.second == 5 }
}

class QuizViewModel(
    private val sessions: SessionRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val _ui = MutableStateFlow(QuizUiState(sessionId = sessionId))
    val ui: StateFlow<QuizUiState> = _ui.asStateFlow()
    private var enteredAt = System.currentTimeMillis()
    private var timer: Job? = null

    init {
        viewModelScope.launch {
            val session = sessions.session(sessionId) ?: return@launch
            val qs = sessions.sessionQuestions(session)
            val saved = sessions.answers(sessionId)
            val answeredCount = saved.count { it.selectedKey != null }
            val perQ = if (preferences.dailyTimer.value && session.durationLimitSec > 0) 60 else 0
            _ui.update {
                it.copy(
                    loading = false, title = session.title, type = SessionType.valueOf(session.type), questions = qs,
                    index = answeredCount.coerceAtMost(qs.lastIndex.coerceAtLeast(0)),
                    scores = saved.sortedBy { a -> a.position }.take(answeredCount).map { a -> a.score },
                    perQuestionSec = perQ, secondsLeft = perQ
                )
            }
            if (answeredCount >= qs.size && qs.isNotEmpty()) finish() else startTimer()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        enteredAt = System.currentTimeMillis()
        val perQ = _ui.value.perQuestionSec
        if (perQ <= 0) return
        timer = viewModelScope.launch {
            var left = perQ
            while (left > 0 && !_ui.value.revealed) {
                _ui.update { it.copy(secondsLeft = left) }
                delay(1000); left--
            }
            if (!_ui.value.revealed) select(null) // waktu habis: dihitung kosong
        }
    }

    fun select(key: String?) {
        val s = _ui.value
        if (s.revealed) return
        val q = s.current ?: return
        timer?.cancel()
        val spent = System.currentTimeMillis() - enteredAt
        val score = q.scoreFor(key)
        _ui.update { it.copy(selected = key, revealed = true, scores = it.scores + score) }
        viewModelScope.launch { sessions.saveAnswer(sessionId, q, key, false, spent, s.index) }
    }

    fun next() {
        val s = _ui.value
        if (!s.revealed) return
        if (s.index >= s.questions.lastIndex) { finish(); return }
        _ui.update { it.copy(index = it.index + 1, selected = null, revealed = false, secondsLeft = it.perQuestionSec) }
        startTimer()
    }

    private fun finish() {
        timer?.cancel()
        viewModelScope.launch {
            val r = sessions.finishSession(sessionId)
            _ui.update { it.copy(finished = true, result = r) }
        }
    }

    override fun onCleared() { timer?.cancel() }
}

package com.belajar.cpns.ui.tryout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.SessionStatus
import com.belajar.cpns.data.prefs.AppPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemState(val selected: String? = null, val flagged: Boolean = false, val timeSpentMs: Long = 0L)

data class TryoutUiState(
    val loading: Boolean = true,
    val sessionId: Long = 0L,
    val title: String = "",
    val questions: List<Question> = emptyList(),
    val items: List<ItemState> = emptyList(),
    val index: Int = 0,
    val remainingSec: Int = 0,
    val finished: Boolean = false,
    val showGrid: Boolean = false,
    val confirmFinish: Boolean = false
) {
    val current: Question? get() = questions.getOrNull(index)
    val answeredCount: Int get() = items.count { it.selected != null }
    val flaggedCount: Int get() = items.count { it.flagged }
    val emptyCount: Int get() = items.count { it.selected == null }
}

class TryoutViewModel(
    private val questionsRepo: QuestionRepository,
    private val sessions: SessionRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val _ui = MutableStateFlow(TryoutUiState(sessionId = sessionId))
    val ui: StateFlow<TryoutUiState> = _ui.asStateFlow()

    private var startedAt = 0L
    private var limitSec = 0
    private var enteredAt = System.currentTimeMillis()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val session = sessions.session(sessionId) ?: return@launch
            val qs = sessions.sessionQuestions(session)
            val saved = sessions.answers(sessionId).associateBy { it.questionId }
            startedAt = session.startedAt
            limitSec = session.durationLimitSec
            val items = qs.map { q -> saved[q.id]?.let { ItemState(it.selectedKey, it.flagged, it.timeSpentMs) } ?: ItemState() }
            val firstEmpty = items.indexOfFirst { it.selected == null }.coerceAtLeast(0)
            _ui.update {
                it.copy(
                    loading = false, title = session.title, questions = qs, items = items,
                    index = if (session.status == SessionStatus.FINISHED) 0 else firstEmpty,
                    remainingSec = remaining(), finished = session.status == SessionStatus.FINISHED
                )
            }
            enteredAt = System.currentTimeMillis()
            startTimer()
        }
    }

    private fun remaining(): Int = (limitSec - (System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val r = remaining()
                _ui.update { it.copy(remainingSec = r) }
                if (r <= 0) { finish(); break }
                delay(1000)
            }
        }
    }

    fun select(key: String) {
        val s = _ui.value
        val q = s.current ?: return
        val item = s.items[s.index].copy(selected = key)
        commit(s.index, q, item)
    }

    fun toggleFlag() {
        val s = _ui.value
        val q = s.current ?: return
        commit(s.index, q, s.items[s.index].copy(flagged = !s.items[s.index].flagged))
    }

    private fun commit(index: Int, q: Question, item: ItemState) {
        _ui.update { it.copy(items = it.items.toMutableList().also { l -> l[index] = item }) }
        viewModelScope.launch { sessions.saveAnswer(sessionId, q, item.selected, item.flagged, item.timeSpentMs, index) }
    }

    fun goTo(index: Int) {
        accumulateTime()
        _ui.update { it.copy(index = index.coerceIn(0, it.questions.lastIndex), showGrid = false) }
    }

    fun next() = goTo(_ui.value.index + 1)
    fun prev() = goTo(_ui.value.index - 1)
    fun showGrid(show: Boolean) { _ui.update { it.copy(showGrid = show) } }
    fun askFinish(show: Boolean) { _ui.update { it.copy(confirmFinish = show) } }

    /** Waktu yang dihabiskan di soal saat ini ditambahkan ke catatan soal itu. */
    private fun accumulateTime() {
        val s = _ui.value
        val q = s.current ?: return
        val now = System.currentTimeMillis()
        val spent = now - enteredAt
        enteredAt = now
        if (spent <= 0) return
        val item = s.items[s.index].copy(timeSpentMs = s.items[s.index].timeSpentMs + spent)
        commit(s.index, q, item)
    }

    fun finish() {
        if (_ui.value.finished) return
        timerJob?.cancel()
        accumulateTime()
        viewModelScope.launch {
            sessions.finishSession(sessionId)
            _ui.update { it.copy(finished = true, confirmFinish = false) }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
    }
}

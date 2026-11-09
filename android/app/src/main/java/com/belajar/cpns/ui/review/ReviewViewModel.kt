package com.belajar.cpns.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.AnswerEntity
import com.belajar.cpns.data.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val answers: Map<String, AnswerEntity> = emptyMap(),
    val index: Int = 0,
    val onlyWrong: Boolean = false
) {
    val visibleIndices: List<Int>
        get() = if (!onlyWrong) questions.indices.toList()
        else questions.indices.filter { i -> (answers[questions[i].id]?.score ?: 0) < questions[i].maxScore }
    val current: Question? get() = questions.getOrNull(index)
}

class ReviewViewModel(private val sessions: SessionRepository, savedStateHandle: SavedStateHandle) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val startIndex: Int = savedStateHandle["index"] ?: 0
    private val _ui = MutableStateFlow(ReviewUiState(index = startIndex))
    val ui: StateFlow<ReviewUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessions.session(sessionId) ?: return@launch
            val qs = sessions.sessionQuestions(session)
            val answers = sessions.answers(sessionId).associateBy { it.questionId }
            _ui.update { it.copy(loading = false, questions = qs, answers = answers, index = startIndex.coerceIn(0, qs.lastIndex.coerceAtLeast(0))) }
        }
    }

    fun goTo(index: Int) = _ui.update { it.copy(index = index.coerceIn(0, it.questions.lastIndex)) }

    fun next() {
        val s = _ui.value
        val list = s.visibleIndices
        val nextIdx = list.firstOrNull { it > s.index } ?: return
        goTo(nextIdx)
    }

    fun prev() {
        val s = _ui.value
        val list = s.visibleIndices
        val prevIdx = list.lastOrNull { it < s.index } ?: return
        goTo(prevIdx)
    }

    fun toggleOnlyWrong() = _ui.update { s ->
        val next = !s.onlyWrong
        val list = if (next) s.copy(onlyWrong = true).visibleIndices else s.questions.indices.toList()
        s.copy(onlyWrong = next, index = if (s.index in list) s.index else (list.firstOrNull() ?: 0))
    }
}

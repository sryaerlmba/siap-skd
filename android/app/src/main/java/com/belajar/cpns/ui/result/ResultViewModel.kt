package com.belajar.cpns.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.AnswerEntity
import com.belajar.cpns.data.db.SessionEntity
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.domain.Scoring
import com.belajar.cpns.domain.SkdResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TagLoss(val subtest: String, val tag: String, val lost: Int, val detail: String)

data class ResultUiState(
    val loading: Boolean = true,
    val session: SessionEntity? = null,
    val result: SkdResult? = null,
    val timeUsedSec: Int = 0,
    val answered: Int = 0,
    val total: Int = 0,
    val previousTotal: Int? = null,
    val worstTags: List<TagLoss> = emptyList(),
    val config: ScoringConfig? = null
)

class ResultViewModel(
    private val questions: QuestionRepository,
    private val sessions: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])
    private val _ui = MutableStateFlow(ResultUiState())
    val ui: StateFlow<ResultUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessions.session(sessionId) ?: return@launch
            val qs = sessions.sessionQuestions(session)
            val answers = sessions.answers(sessionId)
            val category = runCatching { ApplicantCategory.valueOf(session.category) }.getOrDefault(ApplicantCategory.UMUM)
            val result = Scoring.score(qs, answers.associate { it.questionId to it.selectedKey }, questions.scoringConfig, category)
            val stored = sessions.result(sessionId)
            _ui.value = ResultUiState(
                loading = false,
                session = session,
                result = result,
                timeUsedSec = stored?.timeUsedSec ?: 0,
                answered = answers.count { it.selectedKey != null },
                total = qs.size,
                previousTotal = null,
                worstTags = worstTags(qs, answers),
                config = questions.scoringConfig
            )
        }
    }

    /** Tag yang paling banyak kehilangan poin, untuk arahan belajar. */
    private fun worstTags(qs: List<Question>, answers: List<AnswerEntity>): List<TagLoss> {
        val byId = answers.associateBy { it.questionId }
        return qs.groupBy { it.subtest to it.tag }.map { (key, items) ->
            val lost = items.sumOf { it.maxScore - (byId[it.id]?.score ?: 0) }
            val detail = if (key.first.name == "TKP") {
                val avg = items.sumOf { byId[it.id]?.score ?: 0 }.toDouble() / items.size
                "rata-rata %.1f".format(avg).replace('.', ',')
            } else {
                "${items.count { (byId[it.id]?.score ?: 0) > 0 } } dari ${items.size} benar"
            }
            TagLoss(key.first.name, key.second, lost, detail)
        }.sortedByDescending { it.lost }.take(3)
    }
}

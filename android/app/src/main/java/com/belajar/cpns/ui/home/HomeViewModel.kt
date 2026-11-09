package com.belajar.cpns.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.MaterialPage
import com.belajar.cpns.data.MaterialRepository
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.SessionResultEntity
import com.belajar.cpns.data.db.TagStatEntity
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.Streak
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.TryoutComposer
import com.belajar.cpns.domain.strength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Recommendation(val page: MaterialPage, val stat: TagStatEntity, val strength: Strength)

data class HomeUiState(
    val ready: Boolean = false,
    val bankCount: Int = 0,
    val latest: SessionResultEntity? = null,
    val tryoutCount: Int = 0,
    val runningSessionId: Long? = null,
    val category: ApplicantCategory = ApplicantCategory.UMUM,
    val examDate: Long = 0L,
    val userName: String = "",
    val starting: Boolean = false,
    val streak: Int = 0,
    val dailyDoneToday: Boolean = false,
    val dueCount: Int = 0,
    val recommendations: List<Recommendation> = emptyList()
)

class HomeViewModel(
    private val questions: QuestionRepository,
    private val sessions: SessionRepository,
    private val preferences: AppPreferences,
    private val materials: MaterialRepository
) : ViewModel() {

    private val ready = MutableStateFlow(false)
    private val bankCount = MutableStateFlow(0)
    private val running = MutableStateFlow<Long?>(null)
    private val starting = MutableStateFlow(false)
    private val pages = MutableStateFlow<List<MaterialPage>>(emptyList())

    private val base = combine(ready, bankCount, running, starting) { r, c, run, s -> Base(r, c, run, s) }
    private data class Base(val ready: Boolean, val count: Int, val running: Long?, val starting: Boolean)
    private data class Prefs(val category: ApplicantCategory, val examDate: Long, val name: String)
    private val prefs = combine(preferences.category, preferences.examDate, preferences.userName) { c, d, n -> Prefs(c, d, n) }
    private data class Activity(val streak: Int, val doneToday: Boolean, val due: Int)
    private val activity = combine(sessions.activityTimestamps(), sessions.dueReviewCount()) { ts, due -> Activity(Streak.compute(ts), Streak.doneToday(ts), due) }
    private val recs = combine(sessions.tagStats(), pages) { stats, p -> recommend(stats, p) }

    val uiState: StateFlow<HomeUiState> = combine(
        base, sessions.latestTryout(), sessions.finishedCount(SessionType.TRYOUT), prefs, combine(activity, recs) { a, r -> a to r }
    ) { b, latest, count, pr, (act, rec) ->
        HomeUiState(
            ready = b.ready, bankCount = b.count, latest = latest, tryoutCount = count, runningSessionId = b.running,
            category = pr.category, examDate = pr.examDate, userName = pr.name, starting = b.starting,
            streak = act.streak, dailyDoneToday = act.doneToday, dueCount = act.due, recommendations = rec
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            questions.ensureImported()
            bankCount.value = questions.count()
            running.value = sessions.runningSession(SessionType.TRYOUT)?.id
            pages.value = materials.pages()
            ready.value = true
        }
    }

    val config get() = questions.scoringConfig

    fun refreshRunning() { viewModelScope.launch { running.value = sessions.runningSession(SessionType.TRYOUT)?.id } }

    /** Dua sub-materi paling lemah yang punya halaman materi. */
    private fun recommend(stats: List<TagStatEntity>, pages: List<MaterialPage>): List<Recommendation> =
        stats.filter { it.strength() == Strength.LEMAH || it.strength() == Strength.SEDANG }
            .sortedBy { it.scoreSum.toDouble() / it.maxSum.coerceAtLeast(1) }
            .mapNotNull { st -> pages.firstOrNull { st.tag in it.tags }?.let { Recommendation(it, st, st.strength()) } }
            .distinctBy { it.page.id }
            .take(2)

    fun startOrResumeTryout(onReady: (Long) -> Unit) {
        if (starting.value) return
        viewModelScope.launch {
            starting.value = true
            try {
                sessions.runningSession(SessionType.TRYOUT)?.let { onReady(it.id); return@launch }
                val items = TryoutComposer.composeTryout(questions.all(), sessions.seenIds(SessionType.TRYOUT), questions.scoringConfig)
                val number = (uiState.value.tryoutCount + 1).toString().padStart(2, '0')
                val duration = if (preferences.extendedTime.value) questions.scoringConfig.durationSecNetra else questions.scoringConfig.durationSec
                val id = sessions.startSession(SessionType.TRYOUT, "Tryout $number", items, duration, preferences.category.value)
                running.value = id
                onReady(id)
            } finally { starting.value = false }
        }
    }
}

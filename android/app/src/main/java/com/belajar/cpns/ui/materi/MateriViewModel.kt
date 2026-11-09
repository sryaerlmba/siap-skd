package com.belajar.cpns.ui.materi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.MaterialPage
import com.belajar.cpns.data.MaterialRepository
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.TagStatEntity
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.TryoutComposer
import com.belajar.cpns.domain.strength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MateriRow(val page: MaterialPage, val strength: Strength, val read: Boolean)

data class MateriUiState(
    val pages: List<MateriRow> = emptyList(),
    val subtest: Subtest = Subtest.TWK,
    val readCount: Int = 0
)

class MateriViewModel(
    private val materials: MaterialRepository,
    private val sessions: SessionRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val pages = MutableStateFlow<List<MaterialPage>>(emptyList())
    private val subtest = MutableStateFlow(Subtest.TWK)

    val ui: StateFlow<MateriUiState> = combine(pages, subtest, sessions.tagStats(), preferences.readPages) { p, st, stats, read ->
        val byTag = stats.associateBy { it.tag }
        MateriUiState(
            pages = p.filter { it.subtest == st }.map { page -> MateriRow(page, pageStrength(page, byTag), page.id in read) },
            subtest = st,
            readCount = p.count { it.id in read }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriUiState())

    val total: Int get() = pages.value.size

    init { viewModelScope.launch { pages.value = materials.pages() } }

    fun select(st: Subtest) { subtest.value = st }

    private fun pageStrength(page: MaterialPage, byTag: Map<String, TagStatEntity>): Strength {
        val stats = page.tags.mapNotNull { byTag[it] }
        if (stats.isEmpty()) return Strength.BELUM
        val score = stats.sumOf { it.scoreSum }; val max = stats.sumOf { it.maxSum }; val attempts = stats.sumOf { it.attempts }
        return TagStatEntity(page.id, page.subtest.name, attempts, score, max, 0, 0).strength()
    }
}

data class MateriDetailUiState(
    val page: MaterialPage? = null,
    val read: Boolean = false,
    val starting: Boolean = false,
    val fontScale: Float = 1f
)

class MateriDetailViewModel(
    private val materials: MaterialRepository,
    private val questions: QuestionRepository,
    private val sessions: SessionRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val pageId: String = checkNotNull(savedStateHandle["pageId"])
    private val page = MutableStateFlow<MaterialPage?>(null)
    private val starting = MutableStateFlow(false)

    val ui: StateFlow<MateriDetailUiState> = combine(page, preferences.readPages, starting, preferences.readFontScale) { p, read, s, scale ->
        MateriDetailUiState(p, pageId in read, s, scale)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MateriDetailUiState())

    init { viewModelScope.launch { page.value = materials.page(pageId) } }

    fun toggleRead() { preferences.toggleRead(pageId) }

    /** Cubit: kalikan skala saat ini. Tombol: geser 0,1. */
    fun zoomBy(factor: Float) = preferences.setReadFontScale(preferences.readFontScale.value * factor)
    fun stepFont(delta: Float) = preferences.setReadFontScale(preferences.readFontScale.value + delta)
    fun resetFont() = preferences.setReadFontScale(1f)

    fun startPractice(onReady: (Long) -> Unit) {
        val p = page.value ?: return
        if (p.tags.isEmpty() || starting.value) return
        viewModelScope.launch {
            starting.value = true
            try {
                val pool = questions.byTags(p.tags)
                val items = TryoutComposer.pick(pool, sessions.seenIds(SessionType.PRACTICE), 10.coerceAtMost(pool.size))
                if (items.isEmpty()) return@launch
                onReady(sessions.startSession(SessionType.PRACTICE, "Latihan: ${p.title}", items, if (preferences.dailyTimer.value) 1 else 0, preferences.category.value))
            } finally { starting.value = false }
        }
    }
}

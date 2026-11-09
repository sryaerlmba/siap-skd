package com.belajar.cpns.ui.latihan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.TagCount
import com.belajar.cpns.data.db.TagStatEntity
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.Streak
import com.belajar.cpns.domain.TryoutComposer
import com.belajar.cpns.domain.strength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TagRow(val subtest: Subtest, val group: String, val tag: String, val total: Int, val strength: Strength)

data class LatihanUiState(
    val tags: List<TagRow> = emptyList(),
    val selectedSubtest: Subtest = Subtest.TIU,
    val selected: Set<String> = emptySet(),
    val count: Int = 20,
    val dueCount: Int = 0,
    val streak: Int = 0,
    val dailyDoneToday: Boolean = false,
    val dailyRunningId: Long? = null,
    val starting: Boolean = false
) {
    val available: Int get() = tags.filter { it.tag in selected }.sumOf { it.total }
}

class LatihanViewModel(
    private val questions: QuestionRepository,
    private val sessions: SessionRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val tagCounts = MutableStateFlow<List<TagCount>>(emptyList())
    private val selectedSubtest = MutableStateFlow(Subtest.TIU)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val count = MutableStateFlow(20)
    private val starting = MutableStateFlow(false)
    private val dailyRunning = MutableStateFlow<Long?>(null)

    val ui: StateFlow<LatihanUiState> = combine(
        combine(tagCounts, sessions.tagStats()) { counts, stats -> rows(counts, stats) },
        combine(selectedSubtest, selected, count, starting) { st, sel, c, s -> Triple(st, sel, c) to s },
        sessions.dueReviewCount(),
        sessions.activityTimestamps(),
        dailyRunning
    ) { rows, (sel, starting), due, ts, running ->
        LatihanUiState(
            tags = rows, selectedSubtest = sel.first, selected = sel.second, count = sel.third,
            dueCount = due, streak = Streak.compute(ts), dailyDoneToday = Streak.doneTodayDaily(ts),
            dailyRunningId = running, starting = starting
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LatihanUiState())

    init {
        viewModelScope.launch {
            questions.ensureImported()
            tagCounts.value = questions.tagCounts()
            dailyRunning.value = sessions.runningSession(SessionType.DAILY)?.id
        }
    }

    private fun rows(counts: List<TagCount>, stats: List<TagStatEntity>): List<TagRow> {
        val byTag = stats.associateBy { it.tag }
        return counts.map { c ->
            TagRow(Subtest.from(c.subtest), c.group, c.tag, c.total, byTag[c.tag]?.strength() ?: Strength.BELUM)
        }.sortedWith(compareBy({ it.subtest.ordinal }, { it.group }, { it.tag }))
    }

    fun selectSubtest(st: Subtest) { selectedSubtest.value = st }
    fun toggle(tag: String) { selected.value = if (tag in selected.value) selected.value - tag else selected.value + tag }
    fun setCount(n: Int) { count.value = n }

    fun refresh() { viewModelScope.launch { dailyRunning.value = sessions.runningSession(SessionType.DAILY)?.id } }

    /** Latihan per tag: tanpa batas waktu total; timer per soal mengikuti pengaturan. */
    fun startPractice(onReady: (Long) -> Unit) = launchOnce {
        val pool = questions.byTags(selected.value.toList())
        if (pool.isEmpty()) return@launchOnce
        val seen = sessions.seenIds(SessionType.PRACTICE)
        val items = TryoutComposer.pick(pool, seen, count.value.coerceAtMost(pool.size))
        val title = "Latihan: " + selected.value.take(2).joinToString(", ") { it.replace('_', ' ') } + if (selected.value.size > 2) " +${selected.value.size - 2}" else ""
        onReady(sessions.startSession(SessionType.PRACTICE, title, items, if (preferences.dailyTimer.value) 1 else 0, preferences.category.value))
    }

    /** Ulangi soal yang jatuh tempo. */
    fun startReview(onReady: (Long) -> Unit) = launchOnce {
        val due = sessions.dueReview().map { it.questionId }
        val items = questions.byIds(due).shuffled().take(20)
        if (items.isEmpty()) return@launchOnce
        onReady(sessions.startSession(SessionType.PRACTICE, "Pengulangan", items, 0, preferences.category.value))
    }

    /** Kuis harian: 4 TWK, 5 TIU, 6 TKP; 70% dari tag lemah. Satu sesi per hari, bisa dilanjutkan. */
    fun startOrResumeDaily(onReady: (Long) -> Unit) = launchOnce {
        sessions.runningSession(SessionType.DAILY)?.let { onReady(it.id); return@launchOnce }
        val stats = sessions.tagStatsOnce()
        val weak = stats.filter { it.strength() == Strength.LEMAH || it.strength() == Strength.SEDANG }.sortedBy { it.scoreSum.toDouble() / it.maxSum.coerceAtLeast(1) }.map { it.tag }
        val pool = questions.all()
        val seen = sessions.seenIds(SessionType.DAILY) + sessions.seenIds(SessionType.TRYOUT)
        val items = TryoutComposer.composeDaily(pool, seen, weak, mapOf(Subtest.TWK to 4, Subtest.TIU to 5, Subtest.TKP to 6))
        val id = sessions.startSession(SessionType.DAILY, "Kuis ${LocalDate.now()}", items, if (preferences.dailyTimer.value) 1 else 0, preferences.category.value)
        dailyRunning.value = id
        onReady(id)
    }

    private fun launchOnce(block: suspend () -> Unit) {
        if (starting.value) return
        viewModelScope.launch {
            starting.value = true
            try { block() } finally { starting.value = false }
        }
    }
}

private fun Streak.doneTodayDaily(ts: List<Long>): Boolean = doneToday(ts)

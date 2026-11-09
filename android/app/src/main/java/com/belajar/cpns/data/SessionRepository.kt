package com.belajar.cpns.data

import com.belajar.cpns.data.db.AnswerEntity
import com.belajar.cpns.data.db.ReviewQueueEntity
import com.belajar.cpns.data.db.SessionEntity
import com.belajar.cpns.data.db.SessionResultEntity
import com.belajar.cpns.data.db.SkdDatabase
import com.belajar.cpns.data.db.TagStatEntity
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.SessionStatus
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.domain.Scoring
import com.belajar.cpns.domain.SkdResult
import kotlinx.coroutines.flow.Flow

/** Sesi pengerjaan (tryout, kuis harian, latihan), jawaban, hasil, dan statistik per tag. */
class SessionRepository(private val db: SkdDatabase, private val questions: QuestionRepository) {

    suspend fun startSession(
        type: SessionType,
        title: String,
        items: List<Question>,
        durationLimitSec: Int,
        category: ApplicantCategory,
        now: Long = System.currentTimeMillis()
    ): Long {
        val id = db.sessions().insert(
            SessionEntity(
                type = type.name,
                title = title,
                startedAt = now,
                durationLimitSec = durationLimitSec,
                status = SessionStatus.RUNNING,
                questionOrder = items.joinToString(",") { it.id },
                category = category.name
            )
        )
        db.answers().upsertAll(items.mapIndexed { i, q ->
            AnswerEntity(id, q.id, null, 0, 0L, false, i)
        })
        return id
    }

    suspend fun runningSession(type: SessionType): SessionEntity? = db.sessions().running(type.name)

    suspend fun session(id: Long): SessionEntity? = db.sessions().byId(id)

    suspend fun sessionQuestions(session: SessionEntity): List<Question> =
        questions.byIds(session.questionOrder.split(",").filter { it.isNotBlank() })

    suspend fun answers(sessionId: Long): List<AnswerEntity> = db.answers().bySession(sessionId)

    suspend fun saveAnswer(sessionId: Long, question: Question, key: String?, flagged: Boolean, timeSpentMs: Long, position: Int) {
        db.answers().upsert(
            AnswerEntity(sessionId, question.id, key, question.scoreFor(key), timeSpentMs, flagged, position)
        )
    }

    suspend fun seenIds(type: SessionType): Set<String> = db.sessions().seenQuestionIds(type.name).toSet()

    /** Menutup sesi: hitung skor, simpan hasil, perbarui statistik tag dan antrian pengulangan. */
    suspend fun finishSession(sessionId: Long, now: Long = System.currentTimeMillis()): SkdResult? {
        val session = db.sessions().byId(sessionId) ?: return null
        val items = sessionQuestions(session)
        val answers = db.answers().bySession(sessionId).associateBy { it.questionId }
        val category = runCatching { ApplicantCategory.valueOf(session.category) }.getOrDefault(ApplicantCategory.UMUM)
        val result = Scoring.score(items, answers.mapValues { it.value.selectedKey }, questions.scoringConfig, category)

        db.sessions().update(session.copy(status = SessionStatus.FINISHED, finishedAt = now))
        db.results().upsert(
            SessionResultEntity(
                sessionId = sessionId,
                scoreTWK = result.of(Subtest.TWK).score,
                scoreTIU = result.of(Subtest.TIU).score,
                scoreTKP = result.of(Subtest.TKP).score,
                total = result.total,
                passTWK = result.of(Subtest.TWK).pass,
                passTIU = result.of(Subtest.TIU).pass,
                passTKP = result.of(Subtest.TKP).pass,
                passAll = result.passAll,
                answered = answers.values.count { it.selectedKey != null },
                timeUsedSec = ((now - session.startedAt) / 1000).toInt().coerceAtMost(session.durationLimitSec)
            )
        )
        items.forEach { q ->
            val a = answers[q.id]
            db.tagStats().record(q.tag, q.subtest.name, a?.score ?: 0, q.maxScore, a?.timeSpentMs ?: 0L, now)
            // Soal yang salah (TWK/TIU) atau skor TKP di bawah 4 masuk antrian pengulangan: 1 hari pertama.
            val weak = (a?.score ?: 0) < if (q.subtest == Subtest.TKP) 4 else 5
            if (weak) {
                val cur = db.reviewQueue().byQuestion(q.id)
                if (cur == null) db.reviewQueue().upsert(ReviewQueueEntity(q.id, now + DAY_MS, 1))
            } else {
                // Sudah benar: majukan interval 1 -> 3 -> 7, lalu keluar dari antrian.
                val cur = db.reviewQueue().byQuestion(q.id)
                if (cur != null) {
                    val next = when (cur.intervalDays) { 1 -> 3; 3 -> 7; else -> 0 }
                    if (next == 0) db.reviewQueue().remove(q.id)
                    else db.reviewQueue().upsert(cur.copy(intervalDays = next, dueAt = now + next * DAY_MS))
                }
            }
        }
        return result
    }

    /** Streak: jumlah hari berturut-turut (sampai hari ini atau kemarin) dengan kuis harian atau tryout selesai. */
    fun activityTimestamps(): Flow<List<Long>> = db.sessions().activityTimestamps()

    suspend fun meta(key: String): String? = db.meta().get(key)
    suspend fun setMeta(key: String, value: String) = db.meta().set(com.belajar.cpns.data.db.MetaEntity(key, value))

    fun tryoutResults(): Flow<List<SessionResultEntity>> = db.results().tryoutResults()
    fun latestTryout(): Flow<SessionResultEntity?> = db.results().latestTryout()
    suspend fun result(sessionId: Long): SessionResultEntity? = db.results().bySession(sessionId)
    fun finishedSessions(): Flow<List<SessionEntity>> = db.sessions().finished()
    fun finishedCount(type: SessionType): Flow<Int> = db.sessions().finishedCount(type.name)
    fun tagStats(): Flow<List<TagStatEntity>> = db.tagStats().all()
    suspend fun tagStatsOnce(): List<TagStatEntity> = db.tagStats().allOnce()
    suspend fun dueReview(now: Long = System.currentTimeMillis()): List<ReviewQueueEntity> = db.reviewQueue().due(now)
    fun dueReviewCount(now: Long = System.currentTimeMillis()): Flow<Int> = db.reviewQueue().dueCount(now)

    suspend fun clearProgress() {
        db.answers().deleteAll()
        db.results().deleteAll()
        db.sessions().deleteAll()
        db.tagStats().deleteAll()
        db.reviewQueue().deleteAll()
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

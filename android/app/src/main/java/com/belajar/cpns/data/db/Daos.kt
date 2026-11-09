package com.belajar.cpns.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuestionEntity>)

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE subtest = :subtest")
    suspend fun bySubtest(subtest: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE tag IN (:tags)")
    suspend fun byTags(tags: List<String>): List<QuestionEntity>

    @Query("SELECT id FROM questions WHERE subtest = :subtest")
    suspend fun idsBySubtest(subtest: String): List<String>

    @Query("SELECT subtest, `group`, tag, COUNT(*) AS total FROM questions GROUP BY subtest, `group`, tag")
    suspend fun tagCounts(): List<TagCount>
}

data class TagCount(val subtest: String, val `group`: String, val tag: String, val total: Int)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'RUNNING' AND type = :type ORDER BY startedAt DESC LIMIT 1")
    suspend fun running(type: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'FINISHED' ORDER BY finishedAt DESC")
    fun finished(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 'FINISHED' AND type = :type ORDER BY finishedAt DESC")
    fun finishedOfType(type: String): Flow<List<SessionEntity>>

    @Query("SELECT finishedAt FROM sessions WHERE status = 'FINISHED' AND type IN ('DAILY','TRYOUT') AND finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun activityTimestamps(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'FINISHED' AND type = :type")
    fun finishedCount(type: String): Flow<Int>

    /** Id soal yang sudah pernah muncul di sesi tryout mana pun, untuk menghindari pengulangan. */
    @Query("SELECT DISTINCT questionId FROM answers INNER JOIN sessions ON sessions.id = answers.sessionId WHERE sessions.type = :type")
    suspend fun seenQuestionIds(type: String): List<String>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(answer: AnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(answers: List<AnswerEntity>)

    @Query("SELECT * FROM answers WHERE sessionId = :sessionId ORDER BY position")
    suspend fun bySession(sessionId: Long): List<AnswerEntity>

    @Query("DELETE FROM answers")
    suspend fun deleteAll()
}

@Dao
interface ResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: SessionResultEntity)

    @Query("SELECT * FROM session_results WHERE sessionId = :sessionId")
    suspend fun bySession(sessionId: Long): SessionResultEntity?

    @Query("SELECT r.* FROM session_results r INNER JOIN sessions s ON s.id = r.sessionId WHERE s.type = 'TRYOUT' ORDER BY s.finishedAt DESC")
    fun tryoutResults(): Flow<List<SessionResultEntity>>

    @Query("SELECT r.* FROM session_results r INNER JOIN sessions s ON s.id = r.sessionId WHERE s.type = 'TRYOUT' ORDER BY s.finishedAt DESC LIMIT 1")
    fun latestTryout(): Flow<SessionResultEntity?>

    @Query("DELETE FROM session_results")
    suspend fun deleteAll()
}

@Dao
interface TagStatDao {
    @Query("SELECT * FROM tag_stats")
    fun all(): Flow<List<TagStatEntity>>

    @Query("SELECT * FROM tag_stats")
    suspend fun allOnce(): List<TagStatEntity>

    @Query("SELECT * FROM tag_stats WHERE tag = :tag")
    suspend fun byTag(tag: String): TagStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: TagStatEntity)

    @Transaction
    suspend fun record(tag: String, subtest: String, score: Int, max: Int, timeMs: Long, now: Long) {
        val cur = byTag(tag)
        upsert(
            if (cur == null) TagStatEntity(tag, subtest, 1, score, max, timeMs, now)
            else cur.copy(
                attempts = cur.attempts + 1,
                scoreSum = cur.scoreSum + score,
                maxSum = cur.maxSum + max,
                timeSumMs = cur.timeSumMs + timeMs,
                lastSeenAt = now
            )
        )
    }

    @Query("DELETE FROM tag_stats")
    suspend fun deleteAll()
}

@Dao
interface ReviewQueueDao {
    @Query("SELECT * FROM review_queue WHERE dueAt <= :now ORDER BY dueAt")
    suspend fun due(now: Long): List<ReviewQueueEntity>

    @Query("SELECT COUNT(*) FROM review_queue WHERE dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM review_queue WHERE questionId = :questionId")
    suspend fun byQuestion(questionId: String): ReviewQueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReviewQueueEntity)

    @Query("DELETE FROM review_queue WHERE questionId = :questionId")
    suspend fun remove(questionId: String)

    @Query("DELETE FROM review_queue")
    suspend fun deleteAll()
}

@Dao
interface MetaDao {
    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: MetaEntity)
}

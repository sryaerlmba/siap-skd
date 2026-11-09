package com.belajar.cpns.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "questions", indices = [Index("subtest"), Index("tag")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val subtest: String,
    val `group`: String,
    val tag: String,
    val stem: String,
    /** Opsi disimpan sebagai JSON array (lihat BankParser.optionsToJson). */
    val optionsJson: String,
    val explanation: String,
    val difficulty: Int,
    val source: String
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationLimitSec: Int,
    val status: String,
    /** Urutan id soal dalam sesi, dipisah koma. Disimpan supaya sesi bisa dilanjutkan dengan urutan yang sama. */
    val questionOrder: String,
    val category: String
)

@Entity(
    tableName = "answers",
    primaryKeys = ["sessionId", "questionId"],
    indices = [Index("questionId")]
)
data class AnswerEntity(
    val sessionId: Long,
    val questionId: String,
    val selectedKey: String?,
    val score: Int,
    val timeSpentMs: Long,
    val flagged: Boolean,
    val position: Int
)

@Entity(tableName = "session_results")
data class SessionResultEntity(
    @PrimaryKey val sessionId: Long,
    val scoreTWK: Int,
    val scoreTIU: Int,
    val scoreTKP: Int,
    val total: Int,
    val passTWK: Boolean,
    val passTIU: Boolean,
    val passTKP: Boolean,
    val passAll: Boolean,
    val answered: Int,
    val timeUsedSec: Int
)

/** Statistik per tag, dasar pemilihan soal kuis harian dan peta kekuatan. */
@Entity(tableName = "tag_stats")
data class TagStatEntity(
    @PrimaryKey val tag: String,
    val subtest: String,
    val attempts: Int,
    val scoreSum: Int,
    val maxSum: Int,
    val timeSumMs: Long,
    val lastSeenAt: Long
)

@Entity(tableName = "review_queue")
data class ReviewQueueEntity(
    @PrimaryKey val questionId: String,
    val dueAt: Long,
    val intervalDays: Int
)

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String
)

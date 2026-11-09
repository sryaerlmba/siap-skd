package com.belajar.cpns.data

import android.content.res.AssetManager
import com.belajar.cpns.data.content.BankParser
import com.belajar.cpns.data.db.MetaEntity
import com.belajar.cpns.data.db.QuestionEntity
import com.belajar.cpns.data.db.SkdDatabase
import com.belajar.cpns.data.db.TagCount
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Bank soal: impor dari assets sekali per versi, lalu dibaca dari Room. */
class QuestionRepository(private val assets: AssetManager, private val db: SkdDatabase) {

    private val importLock = Mutex()

    val scoringConfig: ScoringConfig by lazy {
        BankParser.parseScoringConfig(assets.open("scoring-config.json").bufferedReader().readText())
    }

    /** Pastikan bank soal di database sesuai versi di assets. Aman dipanggil berkali-kali. */
    suspend fun ensureImported() = withContext(Dispatchers.IO) {
        importLock.withLock {
            val json = assets.open("bank-soal.json").bufferedReader().readText()
            val version = org.json.JSONObject(json).optString("version", "0")
            val installed = db.meta().get(META_BANK_VERSION)
            if (installed == version && db.questions().count() > 0) return@withLock
            val bank = BankParser.parseBank(json)
            db.questions().insertAll(bank.questions.map { it.toEntity() })
            db.meta().set(MetaEntity(META_BANK_VERSION, bank.version))
        }
    }

    suspend fun bankVersion(): String = db.meta().get(META_BANK_VERSION) ?: "-"

    suspend fun count(): Int = db.questions().count()

    suspend fun bySubtest(subtest: Subtest): List<Question> =
        db.questions().bySubtest(subtest.name).map { it.toModel() }

    suspend fun all(): List<Question> = Subtest.entries.flatMap { bySubtest(it) }

    suspend fun byIds(ids: List<String>): List<Question> {
        if (ids.isEmpty()) return emptyList()
        // Room membatasi parameter IN; pecah per 500.
        val map = ids.chunked(500).flatMap { db.questions().byIds(it) }.associateBy { it.id }
        return ids.mapNotNull { map[it]?.toModel() }
    }

    suspend fun byTags(tags: List<String>): List<Question> =
        if (tags.isEmpty()) emptyList() else db.questions().byTags(tags).map { it.toModel() }

    suspend fun tagCounts(): List<TagCount> = db.questions().tagCounts()

    private fun Question.toEntity() = QuestionEntity(
        id = id,
        subtest = subtest.name,
        group = group,
        tag = tag,
        stem = stem,
        optionsJson = BankParser.optionsToJson(options),
        explanation = explanation,
        difficulty = difficulty,
        source = source
    )

    private fun QuestionEntity.toModel() = Question(
        id = id,
        subtest = Subtest.from(subtest),
        group = group,
        tag = tag,
        stem = stem,
        options = BankParser.parseOptions(org.json.JSONArray(optionsJson)),
        explanation = explanation,
        difficulty = difficulty,
        source = source
    )

    companion object {
        private const val META_BANK_VERSION = "bank_version"
    }
}

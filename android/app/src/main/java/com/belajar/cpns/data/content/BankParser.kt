package com.belajar.cpns.data.content

import com.belajar.cpns.data.model.AnswerOption
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.PassingGrade
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import org.json.JSONArray
import org.json.JSONObject

/** Membaca bank-soal.json dan scoring-config.json dari assets. Formatnya mengikuti soal/SCHEMA.md. */
object BankParser {

    data class Bank(val version: String, val questions: List<Question>)

    fun parseBank(json: String): Bank {
        val root = JSONObject(json)
        val arr = root.getJSONArray("questions")
        return Bank(
            version = root.optString("version", "0"),
            questions = List(arr.length()) { parseQuestion(arr.getJSONObject(it)) }
        )
    }

    fun parseQuestion(o: JSONObject): Question = Question(
        id = o.getString("id"),
        subtest = Subtest.from(o.getString("subtest")),
        group = o.getString("group"),
        tag = o.getString("tag"),
        stem = o.getString("stem"),
        options = parseOptions(o.getJSONArray("options")),
        explanation = o.optString("explanation", ""),
        difficulty = o.optInt("difficulty", 2),
        source = o.optString("source", "")
    )

    fun parseOptions(arr: JSONArray): List<AnswerOption> = List(arr.length()) { i ->
        val op = arr.getJSONObject(i)
        AnswerOption(
            key = op.getString("key"),
            text = op.getString("text"),
            score = op.getInt("score"),
            rationale = op.optString("rationale", "")
        )
    }

    fun optionsToJson(options: List<AnswerOption>): String {
        val arr = JSONArray()
        options.forEach { op ->
            arr.put(
                JSONObject()
                    .put("key", op.key)
                    .put("text", op.text)
                    .put("score", op.score)
                    .put("rationale", op.rationale)
            )
        }
        return arr.toString()
    }

    fun parseScoringConfig(json: String): ScoringConfig {
        val root = JSONObject(json)
        val counts = root.getJSONObject("counts")
        val cats = root.getJSONObject("categories")
        return ScoringConfig(
            year = root.getInt("year"),
            counts = Subtest.entries.associateWith { counts.getInt(it.name) },
            durationSec = root.getInt("durationSec"),
            durationSecNetra = root.getInt("durationSecNetra"),
            categories = ApplicantCategory.entries.associateWith { cat ->
                val c = cats.getJSONObject(cat.name)
                PassingGrade(
                    label = c.getString("label"),
                    pgTWK = c.optIntOrNull("pgTWK"),
                    pgTIU = c.optIntOrNull("pgTIU"),
                    pgTKP = c.optIntOrNull("pgTKP"),
                    pgCumulative = c.optIntOrNull("pgCumulative")
                )
            }
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key)) getInt(key) else null
}

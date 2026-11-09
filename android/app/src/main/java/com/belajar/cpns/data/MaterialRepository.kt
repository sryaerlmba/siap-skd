package com.belajar.cpns.data

import android.content.res.AssetManager
import com.belajar.cpns.data.model.Subtest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class MaterialPage(
    val id: String,
    val subtest: Subtest,
    val group: String,
    val title: String,
    val tags: List<String>,
    val readMinutes: Int,
    val markdown: String
)

/** Halaman materi dari assets/materi.json (dibangun oleh tools/build_materi.py). */
class MaterialRepository(private val assets: AssetManager) {

    private var cache: List<MaterialPage>? = null

    suspend fun pages(): List<MaterialPage> = cache ?: withContext(Dispatchers.IO) {
        val root = JSONObject(assets.open("materi.json").bufferedReader().readText())
        val arr = root.getJSONArray("pages")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val tags = o.getJSONArray("tags")
            MaterialPage(
                id = o.getString("id"),
                subtest = Subtest.from(o.getString("subtest")),
                group = o.getString("group"),
                title = o.getString("title"),
                tags = List(tags.length()) { tags.getString(it) },
                readMinutes = o.getInt("readMinutes"),
                markdown = o.getString("markdown")
            )
        }.also { cache = it }
    }

    suspend fun page(id: String): MaterialPage? = pages().firstOrNull { it.id == id }

    suspend fun pageForTag(tag: String): MaterialPage? = pages().firstOrNull { tag in it.tags }
}

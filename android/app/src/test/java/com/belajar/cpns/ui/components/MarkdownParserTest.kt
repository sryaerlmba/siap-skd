package com.belajar.cpns.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `heading paragraf tabel dan daftar terpisah dengan benar`() {
        val md = """
            ### 1.1 Judul

            Paragraf pertama yang
            bersambung ke baris kedua.

            | Kolom A | Kolom B |
            |---|:-:|
            | satu | dua |
            | tiga | empat |

            - item satu
            - item dua
            1. nomor satu
            2. nomor dua
        """.trimIndent()
        val blocks = parseMarkdown(md)
        assertEquals(MdBlock.Heading(3, "1.1 Judul"), blocks[0])
        assertEquals(MdBlock.Paragraph("Paragraf pertama yang bersambung ke baris kedua."), blocks[1])
        val table = blocks[2] as MdBlock.Table
        assertEquals(listOf("Kolom A", "Kolom B"), table.header)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("tiga", "empat"), table.rows[1])
        val bullets = blocks[3] as MdBlock.Bullets
        assertEquals(2, bullets.items.size)
        assertTrue(!bullets.ordered)
        val ordered = blocks[4] as MdBlock.Bullets
        assertTrue(ordered.ordered)
        assertEquals("nomor dua", ordered.items[1].second)
    }

    @Test
    fun `kutipan dan garis`() {
        val blocks = parseMarkdown("> **Inti:** satu\n> dua\n\n---\n\nakhir")
        assertEquals(MdBlock.Quote("**Inti:** satu dua"), blocks[0])
        assertEquals(MdBlock.Rule, blocks[1])
        assertEquals(MdBlock.Paragraph("akhir"), blocks[2])
    }

    @Test
    fun `seluruh materi terparse tanpa blok kosong`() {
        val json = java.io.File("src/main/assets/materi.json").readText()
        val pages = org.json.JSONObject(json).getJSONArray("pages")
        var blocks = 0
        for (i in 0 until pages.length()) {
            val parsed = parseMarkdown(pages.getJSONObject(i).getString("markdown"))
            assertTrue(parsed.isNotEmpty())
            parsed.forEach { b -> if (b is MdBlock.Table) assertTrue(b.header.isNotEmpty()) }
            blocks += parsed.size
        }
        assertTrue(blocks > 200)
    }
}

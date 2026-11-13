package com.belajar.cpns.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FigureParserTest {

    @Test
    fun `grid dan bentuk dikenali, teks biasa tidak`() {
        assertEquals(Figure.Grid(listOf("#..", "#..", "##.")), Figure.parse("#..\n#..\n##."))
        val s = Figure.parse("bentuk=segitiga; isi=hitam; rotasi=90; titik=2; panah=kanan") as Figure.Shape
        assertEquals("segitiga", s.shape); assertEquals("hitam", s.fill); assertEquals(90, s.rotation); assertEquals(2, s.dots); assertEquals("kanan", s.arrow)
        assertNull(Figure.parse("Hasil dari 3/4 + 5/6 adalah ..."))
        assertNull(Figure.parse("ab.\n...\n..."))
    }

    @Test
    fun `stem analogi grid dipecah jadi tiga gambar dan pertanyaan`() {
        val stem = "[grid] Gambar 1 : Gambar 2 = Gambar 3 : ?\nGambar 1:\n#..\n#..\n##.\nGambar 2:\n###\n#..\n...\nGambar 3:\noo.\n..#\n..#\nGambar pengganti tanda ? adalah ..."
        val f = parseFiguralStem(stem)!!
        assertEquals(3, f.figures.size)
        assertEquals("Gambar 3", f.figures[2].first)
        assertEquals("Gambar 1 : Gambar 2 = Gambar 3 : ?", f.intro)
        assertEquals("Gambar pengganti tanda ? adalah ...", f.question)
    }

    @Test
    fun `stem bentuk satu baris per gambar`() {
        val stem = "[bentuk] Lanjutkan barisan gambar berikut.\nGambar 1: bentuk=lingkaran; isi=abu; panah=kiri\nGambar 2: bentuk=lingkaran; isi=abu; panah=atas\nGambar 5 adalah ..."
        val f = parseFiguralStem(stem)!!
        assertEquals(2, f.figures.size)
        assertTrue(f.figures.all { it.second is Figure.Shape })
        assertEquals("Gambar 5 adalah ...", f.question)
    }

    @Test
    fun `ketidaksamaan tanpa gambar di stem`() {
        val f = parseFiguralStem("[grid] Lima grid berisi kotak hitam. Gambar yang berbeda adalah ...")!!
        assertEquals(0, f.figures.size)
        assertNotNull(f.intro)
        assertNull(parseFiguralStem("Tentukan suku berikutnya.\n7, 13, 19"))
    }

    @Test
    fun `semua soal figural di bank bisa diparse`() {
        val json = java.io.File("src/main/assets/bank-soal.json").readText()
        val arr = org.json.JSONObject(json).getJSONArray("questions")
        var n = 0
        for (i in 0 until arr.length()) {
            val q = arr.getJSONObject(i)
            if (q.getString("group") != "figural") continue
            val stem = parseFiguralStem(q.getString("stem"))
            assertNotNull(q.getString("id"), stem)
            val opts = q.getJSONArray("options")
            for (j in 0 until opts.length()) assertNotNull(q.getString("id"), Figure.parse(opts.getJSONObject(j).getString("text")))
            n++
        }
        assertEquals(70, n)
    }
}

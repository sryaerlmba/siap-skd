package com.belajar.cpns.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MathFormatTest {
    @Test
    fun `pecahan memakai garis pecahan tipografis, desimal dan tanggal tidak tersentuh`() {
        assertEquals("Hasil dari 3⁄4 + 5⁄6 adalah", formatMath("Hasil dari 3/4 + 5/6 adalah").text)
        assertEquals("2 1⁄3 × 1 2⁄7", formatMath("2 1/3 × 1 2/7").text)
        assertEquals("0,6 + 20%", formatMath("0,6 + 20%").text)
        assertEquals("17/08/1945", formatMath("17/08/1945").text)
    }

    @Test
    fun `pangkat jadi superskrip tanpa tanda pangkat`() {
        val a = formatMath("27^(2/3) + 16^(3/4)")
        assertEquals("272⁄3 + 163⁄4", a.text)
        assertEquals(2, a.spanStyles.size)
        assertEquals("x2 + y3", formatMath("x^2 + y^3").text)
    }
}

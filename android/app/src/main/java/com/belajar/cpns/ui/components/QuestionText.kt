package com.belajar.cpns.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.ui.theme.Mono

/**
 * Teks soal dan opsi yang ramah dibaca:
 * - Pangkat `27^(2/3)` dan `x^2` jadi superskrip.
 * - Pecahan `3/4` dan campuran `2 1/3` memakai garis pecahan tipografis (3⁄4) agar tidak terbaca "3 per 4" sebagai tanggal.
 * - Baris yang isinya angka dan simbol (deret, P = ..., Q = ...) ditampilkan monospace lebih besar.
 * - Stem figural [grid]/[bentuk] digambar lewat FiguralStemView.
 */
private val powerRe = Regex("(\\d+|\\))\\^\\(([^)]+)\\)|(\\d+|[a-z]|\\))\\^(\\d+)")
// Pecahan sederhana a/b; tanggal 17/08/1945 (dua garis miring) dibiarkan.
private val fractionRe = Regex("(?<![\\d,./])(\\d+)/(\\d+)(?![\\d/])")
private val numericLineRe = Regex("^[\\s\\d.,:;=+−\\-×÷*/%√∛^()?…,\\sPQ]+$")

fun formatMath(text: String): AnnotatedString = buildAnnotatedString {
    var rest = text.replace(fractionRe) { m -> "${m.groupValues[1]}⁄${m.groupValues[2]}" }
    while (true) {
        val m = powerRe.find(rest) ?: break
        append(rest.substring(0, m.range.first))
        val base = m.groupValues[1].ifEmpty { m.groupValues[3] }
        val exp = m.groupValues[2].ifEmpty { m.groupValues[4] }
        append(base)
        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)) { append(exp) }
        rest = rest.substring(m.range.last + 1)
    }
    append(rest)
}

private fun isNumericLine(line: String): Boolean =
    line.length >= 3 && numericLineRe.matches(line) && line.any { it.isDigit() }

/** Teks stem untuk TWK/TIU/TKP. Figural digambar; baris numerik ditebalkan dan dibuat monospace. */
@Composable
fun StemView(question: Question, modifier: Modifier = Modifier) {
    val figural = remember(question.id) { if (question.group == "figural") parseFiguralStem(question.stem) else null }
    if (figural != null) {
        FiguralStemView(figural, modifier)
        return
    }
    val lines = remember(question.id) { question.stem.split("\n") }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            if (question.subtest == Subtest.TIU && isNumericLine(line.trim())) {
                Text(formatMath(line.trim()), fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
            } else {
                Text(if (question.subtest == Subtest.TIU) formatMath(line) else AnnotatedString(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Isi satu opsi: gambar untuk figural, teks dengan notasi rapi untuk yang lain. */
@Composable
fun OptionView(question: Question, text: String, bold: Boolean, modifier: Modifier = Modifier) {
    val figure = remember(question.id, text) { if (question.group == "figural") Figure.parse(text) else null }
    if (figure != null) {
        FigureView(figure, 84.dp, modifier)
    } else if (question.subtest == Subtest.TIU && isNumericLine(text.trim())) {
        Text(formatMath(text), fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
    } else {
        Text(
            if (question.subtest == Subtest.TIU) formatMath(text) else AnnotatedString(text),
            style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, modifier = modifier
        )
    }
}

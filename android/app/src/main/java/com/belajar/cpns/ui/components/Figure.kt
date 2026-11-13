package com.belajar.cpns.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Soal figural disimpan sebagai notasi teks (lihat soal/REVIEW.md). Dua notasi:
 * - [grid]: tiga baris tiga karakter. "." kosong, "#" kotak hitam, "o" lingkaran, "x" silang,
 *   "/" dan "\" diagonal, "^" segitiga tegak.
 * - [bentuk]: "bentuk=segitiga; isi=hitam; rotasi=90; titik=2; panah=kanan; ukuran=besar; garis=3".
 */
sealed class Figure {
    data class Grid(val rows: List<String>) : Figure()
    data class Shape(
        val shape: String,
        val fill: String = "putih",
        val rotation: Int = 0,
        val dots: Int = 0,
        val arrow: String? = null,
        val size: String = "sedang",
        val lines: Int = 0
    ) : Figure()

    companion object {
        private val gridRow = Regex("^[.#ox/\\\\^]{3}$")

        /** Mengenali satu gambar dari teks; null jika bukan notasi gambar. */
        fun parse(text: String): Figure? {
            val t = text.trim()
            val lines = t.lines().map { it.trim() }
            if (lines.size == 3 && lines.all { gridRow.matches(it) }) return Grid(lines)
            if (t.startsWith("bentuk=")) {
                val kv = t.split(";").mapNotNull { part ->
                    val p = part.trim().split("=", limit = 2)
                    if (p.size == 2) p[0].trim() to p[1].trim() else null
                }.toMap()
                return Shape(
                    shape = kv["bentuk"] ?: "persegi",
                    fill = kv["isi"] ?: "putih",
                    rotation = kv["rotasi"]?.toIntOrNull() ?: 0,
                    dots = kv["titik"]?.toIntOrNull() ?: 0,
                    arrow = kv["panah"],
                    size = kv["ukuran"] ?: "sedang",
                    lines = kv["garis"]?.toIntOrNull() ?: 0
                )
            }
            return null
        }
    }
}

/** Stem figural: notasi, gambar bernomor, dan teks pertanyaan. */
data class FiguralStem(val figures: List<Pair<String, Figure>>, val intro: String, val question: String)

private val figureHeader = Regex("^Gambar (\\d+):\\s*(.*)$")

/** Memecah stem "[grid] ..." atau "[bentuk] ..." menjadi gambar-gambar dan teks. Null jika bukan soal figural. */
fun parseFiguralStem(stem: String): FiguralStem? {
    val m = Regex("^\\[(grid|bentuk)]\\s*").find(stem) ?: return null
    val body = stem.substring(m.range.last + 1).lines()
    val figures = mutableListOf<Pair<String, Figure>>()
    val intro = StringBuilder()
    val tail = StringBuilder()
    var i = 0
    while (i < body.size) {
        val line = body[i].trim()
        val h = figureHeader.find(line)
        if (h != null) {
            val label = "Gambar ${h.groupValues[1]}"
            val inlineValue = h.groupValues[2].trim()
            if (inlineValue.isNotEmpty()) {
                Figure.parse(inlineValue)?.let { figures += label to it }
                i++
            } else {
                val rows = body.drop(i + 1).take(3).map { it.trim() }
                Figure.parse(rows.joinToString("\n"))?.let { figures += label to it }
                i += 4
            }
        } else {
            if (figures.isEmpty()) intro.append(line).append(' ') else tail.append(line).append(' ')
            i++
        }
    }
    return FiguralStem(figures, intro.toString().trim(), tail.toString().trim())
}

@Composable
fun FigureView(figure: Figure, size: Dp = 84.dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier.size(size).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(8.dp))
    ) {
        when (figure) {
            is Figure.Grid -> drawGrid(figure)
            is Figure.Shape -> drawShape(figure)
        }
    }
}

/** Baris gambar bernomor untuk stem: Gambar 1, Gambar 2, ... dan kotak "?" di akhir bila ada tanda tanya. */
@Composable
fun FiguralStemView(stem: FiguralStem, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (stem.intro.isNotBlank()) Text(stem.intro, style = MaterialTheme.typography.bodyLarge)
        if (stem.figures.isNotEmpty()) {
            val showQuestionBox = stem.intro.contains("?") || stem.question.contains("?") || stem.question.contains("Gambar 5")
            val count = stem.figures.size + if (showQuestionBox) 1 else 0
            val perRow = 4
            stem.figures.chunked(perRow).forEachIndexed { rowIndex, chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    chunk.forEach { (label, fig) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FigureView(fig, 76.dp)
                            Text(label.replace("Gambar ", ""), style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                    val isLastRow = rowIndex == ceil(stem.figures.size / perRow.toFloat()).toInt() - 1
                    if (showQuestionBox && isLastRow && count - rowIndex * perRow <= perRow) QuestionBox()
                }
            }
            if (showQuestionBox && stem.figures.size % perRow == 0) Row { QuestionBox() }
        }
        if (stem.question.isNotBlank()) Text(stem.question, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun QuestionBox() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.size(76.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp)),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) { Text("?", style = MaterialTheme.typography.headlineSmall, color = Muted) }
        Text("?", style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

private fun DrawScope.drawGrid(g: Figure.Grid) {
    val cell = size.width / 3f
    val stroke = cell * 0.09f
    // Garis bantu tipis
    for (k in 1..2) {
        drawLine(Line, Offset(cell * k, 0f), Offset(cell * k, size.height), 1f)
        drawLine(Line, Offset(0f, cell * k), Offset(size.width, cell * k), 1f)
    }
    g.rows.forEachIndexed { r, row ->
        row.forEachIndexed { c, ch ->
            val x = c * cell; val y = r * cell
            val cx = x + cell / 2; val cy = y + cell / 2
            val pad = cell * 0.16f
            when (ch) {
                '#' -> drawRect(Ink, Offset(x + pad, y + pad), Size(cell - 2 * pad, cell - 2 * pad))
                'o' -> drawCircle(Ink, cell / 2 - pad, Offset(cx, cy), style = Stroke(stroke))
                'x' -> {
                    drawLine(Ink, Offset(x + pad, y + pad), Offset(x + cell - pad, y + cell - pad), stroke, StrokeCap.Round)
                    drawLine(Ink, Offset(x + cell - pad, y + pad), Offset(x + pad, y + cell - pad), stroke, StrokeCap.Round)
                }
                '/' -> drawLine(Ink, Offset(x + pad, y + cell - pad), Offset(x + cell - pad, y + pad), stroke, StrokeCap.Round)
                '\\' -> drawLine(Ink, Offset(x + pad, y + pad), Offset(x + cell - pad, y + cell - pad), stroke, StrokeCap.Round)
                '^' -> {
                    val p = Path().apply {
                        moveTo(cx, y + pad); lineTo(x + cell - pad, y + cell - pad); lineTo(x + pad, y + cell - pad); close()
                    }
                    drawPath(p, Ink)
                }
            }
        }
    }
}

private fun DrawScope.drawShape(s: Figure.Shape) {
    val scale = when (s.size) { "kecil" -> 0.5f; "besar" -> 0.9f; else -> 0.7f }
    val r = size.minDimension / 2 * scale
    val c = Offset(size.width / 2, size.height / 2)
    val fill = when (s.fill) { "hitam" -> Ink; "abu" -> Color(0xFF9AA3B5); else -> Color.White }
    val onFill = if (s.fill == "hitam") Color.White else Ink
    val stroke = Stroke(width = size.minDimension * 0.035f)

    val path: Path = when (s.shape) {
        "lingkaran" -> Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
        "persegi" -> polygon(c, r, 4, startDeg = -45f)
        "segitiga" -> polygon(c, r, 3, startDeg = -90f)
        "segilima" -> polygon(c, r, 5, startDeg = -90f)
        "segienam" -> polygon(c, r, 6, startDeg = 0f)
        "segitujuh" -> polygon(c, r, 7, startDeg = -90f)
        "segidelapan" -> polygon(c, r, 8, startDeg = 22.5f)
        "belah ketupat" -> Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * 0.65f, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.65f, c.y); close() }
        "trapesium" -> Path().apply { moveTo(c.x - r * 0.55f, c.y - r * 0.6f); lineTo(c.x + r * 0.55f, c.y - r * 0.6f); lineTo(c.x + r, c.y + r * 0.6f); lineTo(c.x - r, c.y + r * 0.6f); close() }
        "layang-layang" -> Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * 0.7f, c.y - r * 0.2f); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.7f, c.y - r * 0.2f); close() }
        else -> polygon(c, r, 4, startDeg = -45f)
    }
    // Rotasi hanya untuk garis luar bentuk (searah jarum jam), sesuai aturan notasi.
    rotate(s.rotation.toFloat(), pivot = c) {
        drawPath(path, fill)
        drawPath(path, Ink, style = stroke)
    }
    // Garis-garis sejajar di dalam
    if (s.lines > 0) {
        val span = r * 1.1f
        val gap = span / (s.lines + 1)
        for (i in 1..s.lines) {
            val y = c.y - span / 2 + gap * i
            drawLine(onFill, Offset(c.x - r * 0.45f, y), Offset(c.x + r * 0.45f, y), stroke.width, StrokeCap.Round)
        }
    }
    // Titik-titik: susun dalam kisi persegi agar mudah dihitung
    if (s.dots > 0) {
        val cols = ceil(sqrt(s.dots.toFloat())).toInt()
        val rows = ceil(s.dots / cols.toFloat()).toInt()
        val area = r * 1.0f
        val step = if (cols > 1) area / (cols - 1) else 0f
        val stepY = if (rows > 1) area / (rows - 1) else 0f
        val dotR = (size.minDimension * 0.035f).coerceAtMost(step / 3.2f.coerceAtLeast(1f))
        var n = 0
        for (ri in 0 until rows) for (ci in 0 until cols) {
            if (n++ >= s.dots) break
            val x = if (cols > 1) c.x - area / 2 + step * ci else c.x
            val y = if (rows > 1) c.y - area / 2 + stepY * ri else c.y
            drawCircle(onFill, dotR, Offset(x, y))
        }
    }
    // Panah: arah absolut, tidak ikut rotasi
    s.arrow?.let { dir ->
        val deg = when (dir) {
            "kanan" -> 0f; "kanan-bawah" -> 45f; "bawah" -> 90f; "kiri-bawah" -> 135f
            "kiri" -> 180f; "kiri-atas" -> 225f; "atas" -> 270f; "kanan-atas" -> 315f; else -> 0f
        }
        val len = r * 0.7f
        rotate(deg, pivot = c) {
            drawLine(onFill, Offset(c.x - len / 2, c.y), Offset(c.x + len / 2, c.y), stroke.width * 1.3f, StrokeCap.Round)
            val head = len * 0.3f
            drawLine(onFill, Offset(c.x + len / 2, c.y), Offset(c.x + len / 2 - head, c.y - head * 0.7f), stroke.width * 1.3f, StrokeCap.Round)
            drawLine(onFill, Offset(c.x + len / 2, c.y), Offset(c.x + len / 2 - head, c.y + head * 0.7f), stroke.width * 1.3f, StrokeCap.Round)
        }
    }
}

private fun polygon(c: Offset, r: Float, n: Int, startDeg: Float): Path = Path().apply {
    for (i in 0 until n) {
        val a = (startDeg + 360f / n * i) * PI.toFloat() / 180f
        val p = Offset(c.x + r * cos(a), c.y + r * sin(a))
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

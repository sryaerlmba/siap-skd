package com.belajar.cpns.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Mono
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.PrimaryTint

/**
 * Renderer Markdown sederhana untuk halaman materi: heading ###/####, paragraf, daftar bullet dan bernomor
 * (satu tingkat indentasi), tabel pipa, kutipan, kode blok, garis. Inline: **tebal**, *miring*, `kode`.
 */
sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullets(val items: List<Pair<Int, String>>, val ordered: Boolean) : MdBlock()
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class Code(val text: String) : MdBlock()
    data object Rule : MdBlock()
}

fun parseMarkdown(md: String): List<MdBlock> {
    val lines = md.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    val para = StringBuilder()
    fun flushPara() {
        if (para.isNotBlank()) blocks += MdBlock.Paragraph(para.toString().trim())
        para.clear()
    }
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        when {
            t.isEmpty() -> { flushPara(); i++ }
            t.startsWith("```") -> {
                flushPara()
                val buf = mutableListOf<String>(); i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) { buf += lines[i]; i++ }
                i++
                blocks += MdBlock.Code(buf.joinToString("\n"))
            }
            t == "---" || t == "***" -> { flushPara(); blocks += MdBlock.Rule; i++ }
            t.startsWith("#") -> {
                flushPara()
                val level = t.takeWhile { it == '#' }.length
                blocks += MdBlock.Heading(level, t.drop(level).trim())
                i++
            }
            t.startsWith(">") -> {
                flushPara()
                val buf = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) { buf += lines[i].trim().removePrefix(">").trim(); i++ }
                blocks += MdBlock.Quote(buf.joinToString(" "))
            }
            t.startsWith("|") -> {
                flushPara()
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = lines[i].trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                    // Baris pemisah header: |---|:-:|---:| dengan satu atau lebih tanda minus.
                    if (!cells.all { it.matches(Regex("^:?-+:?$")) }) rows += cells
                    i++
                }
                if (rows.isNotEmpty()) blocks += MdBlock.Table(rows.first(), rows.drop(1))
            }
            Regex("^(\\s*)([-*]|\\d+[.)])\\s+").containsMatchIn(line) -> {
                flushPara()
                val items = mutableListOf<Pair<Int, String>>()
                val ordered = Regex("^\\s*\\d+[.)]\\s+").containsMatchIn(line)
                while (i < lines.size) {
                    val m = Regex("^(\\s*)([-*]|\\d+[.)])\\s+(.*)$").find(lines[i])
                    // Daftar bernomor yang langsung menyusul daftar bullet (atau sebaliknya) adalah blok baru.
                    if (m != null && m.groupValues[2].first().isDigit() != ordered) break
                    if (m != null) {
                        items += (m.groupValues[1].length / 2) to m.groupValues[3].trim()
                        i++
                    } else if (lines[i].startsWith("   ") && lines[i].isNotBlank() && items.isNotEmpty()) {
                        // Lanjutan baris item sebelumnya
                        val (ind, txt) = items.removeAt(items.lastIndex)
                        items += ind to (txt + " " + lines[i].trim())
                        i++
                    } else break
                }
                blocks += MdBlock.Bullets(items, ordered)
            }
            else -> { if (para.isNotEmpty()) para.append(' '); para.append(t); i++ }
        }
    }
    flushPara()
    return blocks
}

fun inline(text: String, monoBg: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    var bold = false
    var italic = false
    var code = false
    val buf = StringBuilder()
    fun flush() {
        if (buf.isEmpty()) return
        var style = SpanStyle()
        if (bold) style = style.merge(SpanStyle(fontWeight = FontWeight.Bold))
        if (italic) style = style.merge(SpanStyle(fontStyle = FontStyle.Italic))
        if (code) style = style.merge(SpanStyle(fontFamily = Mono, background = monoBg))
        withStyle(style) { append(buf.toString()) }
        buf.clear()
    }
    while (i < text.length) {
        when {
            text[i] == '`' -> { flush(); code = !code; i++ }
            !code && text.startsWith("**", i) -> { flush(); bold = !bold; i += 2 }
            !code && text[i] == '*' && (i + 1 < text.length && text[i + 1] != ' ') || (!code && text[i] == '*' && italic) -> { flush(); italic = !italic; i++ }
            else -> { buf.append(text[i]); i++ }
        }
    }
    flush()
}

@Composable
fun MarkdownView(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val monoBg = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { b ->
            when (b) {
                is MdBlock.Heading -> Text(
                    b.text.replace(Regex("^[A-E0-9]+(\\.[0-9]+)?\\.?\\s+"), ""),
                    style = when (b.level) { 1, 2 -> MaterialTheme.typography.titleLarge; 3 -> MaterialTheme.typography.titleMedium; else -> MaterialTheme.typography.titleSmall },
                    modifier = Modifier.padding(top = if (b.level <= 3) 10.dp else 4.dp)
                )
                is MdBlock.Paragraph -> Text(inline(b.text, monoBg), style = MaterialTheme.typography.bodyMedium)
                is MdBlock.Quote -> Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PrimaryTint).padding(12.dp)
                ) {
                    Spacer(Modifier.width(3.dp).height(IntrinsicSize.Min).background(Primary))
                    Text(inline(b.text, monoBg), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
                is MdBlock.Code -> Text(
                    b.text, fontFamily = Mono, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(monoBg).padding(10.dp).horizontalScroll(rememberScrollState())
                )
                is MdBlock.Rule -> HorizontalDivider(color = Line)
                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    var n = 0
                    b.items.forEach { (indent, text) ->
                        if (indent == 0) n++
                        Row(Modifier.padding(start = (indent * 16).dp)) {
                            Text(
                                if (b.ordered && indent == 0) "$n." else "•",
                                style = MaterialTheme.typography.bodyMedium, color = Muted,
                                modifier = Modifier.width(if (b.ordered) 24.dp else 16.dp)
                            )
                            Text(inline(text, monoBg), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
                is MdBlock.Table -> MdTable(b, monoBg)
            }
        }
    }
}

/**
 * Tabel ditampilkan menumpuk ke bawah, bukan digeser ke samping, supaya enak dibaca di layar HP.
 * 2 kolom: label kecil di atas, isi di bawahnya. 3 kolom atau lebih: satu kartu per baris,
 * kolom pertama jadi judul kartu dan kolom lain jadi pasangan "nama kolom: isi".
 */
@Composable
private fun MdTable(t: MdBlock.Table, monoBg: Color) {
    val cols = t.header.size
    val allShort = remember(t) { t.rows.all { r -> r.all { it.length <= 14 } } && cols <= 3 }
    if (allShort) { CompactTable(t, monoBg); return }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Line, RoundedCornerShape(10.dp))
    ) {
        t.rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = Line)
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (cols >= 3) {
                    Text(inline(row.getOrNull(0) ?: "", monoBg), style = MaterialTheme.typography.titleSmall)
                    (1 until cols).forEach { c ->
                        val value = row.getOrNull(c).orEmpty()
                        if (value.isBlank()) return@forEach
                        Text(t.header[c].uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.padding(top = 4.dp))
                        Text(inline(value, monoBg), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(inline(row.getOrNull(0) ?: "", monoBg), style = MaterialTheme.typography.labelMedium, color = Primary)
                    Text(inline(row.getOrNull(1) ?: "", monoBg), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Tabel kecil berisi angka atau kata pendek tetap digambar sebagai grid, muat tanpa geser. */
@Composable
private fun CompactTable(t: MdBlock.Table, monoBg: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, Line, RoundedCornerShape(10.dp))) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            t.header.forEach { h ->
                Text(h.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp))
            }
        }
        t.rows.forEach { row ->
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth()) {
                t.header.indices.forEach { c ->
                    Text(inline(row.getOrNull(c) ?: "", monoBg), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
        }
    }
}

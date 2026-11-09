package com.belajar.cpns.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.Pill
import com.belajar.cpns.ui.components.SubtestChip
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.FailTint
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.PassTint

@Composable
fun ReviewScreen(onBack: () -> Unit, viewModel: ReviewViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val q = s.current ?: return
    val answer = s.answers[q.id]
    val selected = answer?.selectedKey
    val myScore = answer?.score ?: 0
    val visible = s.visibleIndices
    val posInVisible = visible.indexOf(s.index)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pembahasan", style = MaterialTheme.typography.titleSmall)
                Text("Soal ${s.index + 1} dari ${s.questions.size} · ${q.subtest.label}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            IconButton(onClick = { viewModel.prev() }, enabled = posInVisible > 0) { Icon(Icons.Default.ChevronLeft, contentDescription = "Soal sebelumnya") }
            IconButton(onClick = { viewModel.next() }, enabled = posInVisible in 0 until visible.lastIndex) { Icon(Icons.Default.ChevronRight, contentDescription = "Soal berikutnya") }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = s.onlyWrong, onClick = { viewModel.toggleOnlyWrong() }, label = { Text("Hanya yang kehilangan poin") })
            Text("${visible.size} soal", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubtestChip(q.subtest)
                Text(q.tag.replace('_', ' '), style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.weight(1f))
                Text("Kamu: ", style = MaterialTheme.typography.bodySmall, color = Muted)
                MonoText("$myScore", size = 14, color = if (myScore == q.maxScore) Pass else Fail)
                Text(" dari ${q.maxScore}", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (answer != null && answer.timeSpentMs > 0) Text(" · ${answer.timeSpentMs / 1000} dtk", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(10.dp))
            Text(q.stem, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(14.dp))

            // TKP: urutkan dari skor 5 ke 1 agar tangga skornya terbaca; TWK/TIU: urutan asli.
            val ordered = if (q.subtest == Subtest.TKP) q.options.sortedByDescending { it.score } else q.options
            ordered.forEach { op ->
                val isKey = op.score == q.maxScore
                val isMine = op.key == selected
                val (bg, border) = when {
                    isKey -> PassTint to Pass
                    isMine -> FailTint to Fail
                    else -> MaterialTheme.colorScheme.surface to Line
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .border(if (isKey || isMine) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier.width(28.dp).height(28.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (isKey) Pass else if (isMine) Fail else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoText(if (q.subtest == Subtest.TKP) "${op.score}" else op.key, size = 14, color = if (isKey || isMine) Color.White else Ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (if (q.subtest == Subtest.TKP) "${op.key}. " else "") + op.text,
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                                fontWeight = if (isKey) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isMine) { Spacer(Modifier.width(6.dp)); Pill("Pilihanmu", Fail, Color.White) }
                        }
                        if (op.rationale.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(op.rationale, style = MaterialTheme.typography.bodySmall, color = if (isKey) Pass else if (isMine) Fail else Muted)
                        }
                    }
                }
            }
            if (selected == null) {
                Spacer(Modifier.height(6.dp))
                Text("Soal ini kosong, jadi dinilai 0.", style = MaterialTheme.typography.bodySmall, color = Fail)
            }
            Spacer(Modifier.height(14.dp))
            if (q.explanation.isNotBlank()) {
                Panel {
                    Text(if (q.subtest == Subtest.TKP) "POLA YANG DINILAI" else "PEMBAHASAN", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Spacer(Modifier.height(6.dp))
                    Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                    if (q.source.isNotBlank() && q.source != "ditulis sendiri") {
                        Spacer(Modifier.height(6.dp))
                        Text("Sumber: ${q.source}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp).navigationBarsPadding())
        }
    }
}

@Suppress("unused")
private fun Question.keyLetter(): String = options.first { it.score == maxScore }.key

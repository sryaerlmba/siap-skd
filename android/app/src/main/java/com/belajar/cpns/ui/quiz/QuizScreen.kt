package com.belajar.cpns.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.belajar.cpns.data.model.SessionType
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.OptionView
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.StemView
import com.belajar.cpns.ui.components.SubtestChip
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.FailTint
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.PassTint
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.Warn
import com.belajar.cpns.ui.theme.WarnTint
import com.belajar.cpns.ui.theme.color

@Composable
fun QuizScreen(onExit: () -> Unit, viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler { onExit() }
    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (s.finished) { QuizSummary(s, onExit); return }
    val q = s.current ?: return

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, contentDescription = "Keluar, lanjutkan nanti") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.title, style = MaterialTheme.typography.titleSmall)
                Text("Soal ${s.index + 1} dari ${s.questions.size}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (s.perQuestionSec > 0 && !s.revealed) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (s.secondsLeft <= 10) FailTint else WarnTint).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    MonoText("0:%02d".format(s.secondsLeft), size = 13, color = if (s.secondsLeft <= 10) Fail else Warn)
                }
            } else Spacer(Modifier.width(48.dp))
        }
        // Progres per soal, diwarnai subtes
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            s.questions.forEachIndexed { i, item ->
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= s.index) item.subtest.color() else Line))
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubtestChip(q.subtest)
                Text(q.tag.replace('_', ' '), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(10.dp))
            StemView(q)
            Spacer(Modifier.height(14.dp))
            q.options.forEach { op ->
                val isMine = op.key == s.selected
                val isKey = op.score == q.maxScore
                val (bg, border, letterBg) = when {
                    !s.revealed && isMine -> Triple(MaterialTheme.colorScheme.primaryContainer, Primary, Primary)
                    s.revealed && isKey -> Triple(PassTint, Pass, Pass)
                    s.revealed && isMine -> Triple(FailTint, Fail, Fail)
                    else -> Triple(MaterialTheme.colorScheme.surface, Line, MaterialTheme.colorScheme.surfaceVariant)
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .border(if (border == Line) 1.dp else 1.5.dp, border, RoundedCornerShape(12.dp))
                        .clickable(enabled = !s.revealed) { viewModel.select(op.key) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(Modifier.width(28.dp).height(28.dp).clip(RoundedCornerShape(8.dp)).background(letterBg), contentAlignment = Alignment.Center) {
                        MonoText(
                            if (s.revealed && q.subtest == Subtest.TKP) "${op.score}" else op.key, size = 14,
                            color = if (letterBg == MaterialTheme.colorScheme.surfaceVariant) Muted else Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        OptionView(q, op.text, bold = s.revealed && isKey)
                        if (s.revealed && op.rationale.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(op.rationale, style = MaterialTheme.typography.bodySmall, color = if (isKey) Pass else if (isMine) Fail else Muted)
                        }
                    }
                }
            }
            if (s.revealed) {
                Spacer(Modifier.height(10.dp))
                val got = q.scoreFor(s.selected)
                val headline = when {
                    s.selected == null -> "Waktu habis, dihitung 0."
                    q.subtest == Subtest.TKP -> "Kamu dapat $got dari 5." + if (s.runningAverage > 0) " Rata-rata TKP sesi ini %.1f.".format(s.runningAverage).replace('.', ',') else ""
                    got == 5 -> "Benar."
                    else -> "Belum tepat."
                }
                Panel {
                    Text(headline, style = MaterialTheme.typography.titleSmall, color = if (got == q.maxScore) Pass else if (got == 0) Fail else Ink)
                    if (q.explanation.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = { viewModel.next() }, enabled = s.revealed,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (s.index >= s.questions.lastIndex) "Lihat ringkasan" else if (s.revealed) "Soal berikutnya" else "Pilih jawaban dulu", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun QuizSummary(s: QuizUiState, onExit: () -> Unit) {
    val r = s.result
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(if (s.type == SessionType.DAILY) "Kuis hari ini selesai" else "Latihan selesai", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("${s.questions.size} soal · ${s.title}", style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(20.dp))
        if (r != null) {
            Panel {
                Subtest.entries.forEach { st ->
                    val sc = r.of(st)
                    if (sc.total == 0) return@forEach
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(st.label, style = MaterialTheme.typography.titleSmall, color = st.color())
                        Text(
                            if (st == Subtest.TKP) "rata-rata %.1f dari 5".format(sc.score.toDouble() / sc.total).replace('.', ',')
                            else "${sc.score / 5} dari ${sc.total} benar",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Soal yang kehilangan poin masuk antrian pengulangan (muncul lagi 1, 3, lalu 7 hari). Peta kekuatan di tab Progres sudah diperbarui.",
                style = MaterialTheme.typography.bodySmall, color = Muted
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Kembali") }
    }
}

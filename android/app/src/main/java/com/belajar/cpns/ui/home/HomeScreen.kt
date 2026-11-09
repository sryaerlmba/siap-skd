package com.belajar.cpns.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.db.SessionResultEntity
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.model.ScoringConfig
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.domain.avgScore
import com.belajar.cpns.domain.avgSeconds
import com.belajar.cpns.domain.ratio
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.ScoreBar
import com.belajar.cpns.ui.components.SectionLabel
import com.belajar.cpns.ui.latihan.StrengthPill
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.FailTint
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.PassTint
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.Warn
import com.belajar.cpns.ui.theme.color
import com.belajar.cpns.ui.theme.tint
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onStartTryout: (Long) -> Unit,
    onOpenResult: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgres: () -> Unit,
    onOpenLatihan: () -> Unit,
    onOpenMateri: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshRunning() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(state.userName), style = MaterialTheme.typography.bodySmall, color = Muted)
                Text(examCountdown(state.examDate), style = MaterialTheme.typography.headlineSmall)
            }
            Row(Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Warn, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                MonoText("${state.streak}", size = 14)
                Text(" hari", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = Muted) }
        }
        Spacer(Modifier.height(14.dp))

        if (!state.ready) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Menyiapkan bank soal...", color = Muted)
                }
            }
        } else {
            LatestTryoutPanel(state.latest, viewModel.config, state.category, onOpenResult)
        }
        Spacer(Modifier.height(12.dp))

        // Kuis harian
        val dailyDone = state.dailyDoneToday
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (dailyDone) PassTint else Primary)
                .clickable { onOpenLatihan() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (dailyDone) Pass else Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (dailyDone) "Kuis hari ini selesai" else "Kuis hari ini", style = MaterialTheme.typography.titleMedium, color = if (dailyDone) Pass else Color.White)
                Text(
                    if (dailyDone) (if (state.dueCount > 0) "${state.dueCount} soal menunggu diulang." else "Streak aman. Lanjut latihan per materi?")
                    else "15 soal · 10 menit · fokus materi lemahmu" + if (state.dueCount > 0) " · ${state.dueCount} soal untuk diulang" else "",
                    style = MaterialTheme.typography.bodySmall, color = if (dailyDone) Ink else Color.White.copy(alpha = 0.85f)
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (dailyDone) Pass else Color.White)
        }
        Spacer(Modifier.height(12.dp))

        // Tryout
        val resuming = state.runningSessionId != null
        Panel(Modifier.clickable(enabled = state.ready && !state.starting) { viewModel.startOrResumeTryout(onStartTryout) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (resuming) "Lanjutkan tryout" else "Tryout CAT", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (resuming) "Sesi masih berjalan, timer tetap jalan." else "${viewModel.config.totalQuestions} soal, ${viewModel.config.durationSec / 60} menit. Paket ${(state.tryoutCount + 1).toString().padStart(2, '0')} siap.",
                        style = MaterialTheme.typography.bodySmall, color = Muted
                    )
                }
                if (state.starting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Disarankan untukmu", style = MaterialTheme.typography.titleMedium)
            Text("Peta kekuatan", color = Primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { onOpenProgres() })
        }
        Spacer(Modifier.height(10.dp))
        if (state.recommendations.isEmpty()) {
            Panel {
                Text(
                    if (state.tryoutCount == 0) "Kerjakan kuis harian atau tryout pertama. Setelah itu, materi yang paling lemah muncul di sini."
                    else "Belum ada sub-materi lemah yang terdeteksi. Pertahankan.",
                    style = MaterialTheme.typography.bodySmall, color = Muted
                )
            }
        } else {
            state.recommendations.forEach { rec ->
                val st = rec.page.subtest
                Panel(Modifier.padding(bottom = 8.dp).clickable { onOpenMateri(rec.page.id) }, padding = 12) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(8.dp).height(40.dp).clip(RoundedCornerShape(4.dp)).background(st.color()))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${st.label} · ${rec.page.title}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                (if (st == Subtest.TKP) "Rata-rata skormu %.1f dari 5".format(rec.stat.avgScore()).replace('.', ',') else "Akurasi ${(rec.stat.ratio() * 100).toInt()}%") +
                                    " · ${rec.stat.avgSeconds()} detik per soal · ${rec.page.readMinutes} menit baca",
                                style = MaterialTheme.typography.bodySmall, color = Muted
                            )
                        }
                        StrengthPill(rec.strength)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LatestTryoutPanel(latest: SessionResultEntity?, config: ScoringConfig, category: ApplicantCategory, onOpen: (Long) -> Unit) {
    val pg = config.passingGrade(category)
    Panel(Modifier.clickable(enabled = latest != null) { latest?.let { onOpen(it.sessionId) } }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Tryout terakhir")
            if (latest != null) Text("Lihat detail", style = MaterialTheme.typography.bodySmall, color = Primary)
        }
        Spacer(Modifier.height(12.dp))
        if (latest == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = Line)
                Spacer(Modifier.width(10.dp))
                Text("Belum ada. Hasil tryout pertamamu tampil di sini, per subtes terhadap passing grade.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            return@Panel
        }
        val rows = listOf(Triple(Subtest.TWK, latest.scoreTWK, pg.pgTWK), Triple(Subtest.TIU, latest.scoreTIU, pg.pgTIU), Triple(Subtest.TKP, latest.scoreTKP, pg.pgTKP))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { (st, score, threshold) ->
                Column(Modifier.weight(1f)) {
                    Text(st.label, style = MaterialTheme.typography.labelMedium, color = st.color())
                    MonoText(score.toString(), size = 24)
                    ScoreBar(score, config.maxScore(st), threshold, st.color(), st.tint(), height = 6)
                    val pass = when (st) { Subtest.TWK -> latest.passTWK; Subtest.TIU -> latest.passTIU; Subtest.TKP -> latest.passTKP }
                    Text(
                        when { threshold == null -> "PG kumulatif"; pass -> "Lolos · PG $threshold"; else -> "Kurang ${threshold - score} · PG $threshold" },
                        style = MaterialTheme.typography.labelSmall, color = if (pass) Pass else Fail
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val (bg, msg) = if (latest.passAll) PassTint to "Lolos SKD dengan total ${latest.total}."
        else FailTint to "Belum lolos SKD. Total ${latest.total}. Ketuk untuk lihat subtes yang kurang."
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).padding(10.dp)) {
            Text(msg, style = MaterialTheme.typography.bodySmall, color = Ink, fontWeight = FontWeight.Medium)
        }
    }
}

private fun greeting(name: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val part = when (hour) { in 4..10 -> "pagi"; in 11..14 -> "siang"; in 15..17 -> "sore"; else -> "malam" }
    return if (name.isBlank()) "Selamat $part" else "Selamat $part, $name"
}

private fun examCountdown(examDate: Long): String {
    if (examDate <= 0L) return "Siap menghadapi SKD"
    val days = TimeUnit.MILLISECONDS.toDays(examDate - System.currentTimeMillis())
    return when { days < 0 -> "SKD sudah lewat"; days == 0L -> "SKD hari ini"; else -> "$days hari menuju SKD" }
}

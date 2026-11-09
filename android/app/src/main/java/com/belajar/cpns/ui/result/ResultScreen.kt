package com.belajar.cpns.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.domain.Scoring
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.ScoreBar
import com.belajar.cpns.ui.components.SectionLabel
import com.belajar.cpns.ui.components.formatClock
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.FailTint
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.PassTint
import com.belajar.cpns.ui.theme.color
import com.belajar.cpns.ui.theme.tint

@Composable
fun ResultScreen(
    onBackHome: () -> Unit,
    onReview: (Long, Int) -> Unit,
    viewModel: ResultViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler { onBackHome() }
    if (s.loading || s.result == null || s.config == null || s.session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val r = s.result!!
    val config = s.config!!

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackHome) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke beranda") }
            Text("Hasil ${s.session!!.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Verdict
            val (bg, fg, title, note) = if (r.passAll) {
                listOf(PassTint, Pass, "Lolos SKD", "Semua ambang terpenuhi dengan total ${r.total}.")
            } else {
                val failing = Subtest.entries.filter { !r.of(it).pass }
                val detail = failing.joinToString("; ") { st ->
                    val sf = Scoring.shortfall(r.of(st))
                    if (sf != null) "${st.label} kurang ${sf.first} poin, kira-kira ${sf.second} soal" else "${st.label} belum lolos"
                }
                val cumul = r.perSubtest.values.firstOrNull { it.passingGrade == null }
                listOf(FailTint, Fail, "Belum lolos SKD", if (cumul != null && failing.isEmpty()) "Total ${r.total} belum mencapai ambang kumulatif." else detail)
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg as Color).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(fg as Color), contentAlignment = Alignment.Center) {
                    Icon(if (r.passAll) Icons.Default.Check else Icons.Default.PriorityHigh, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title as String, style = MaterialTheme.typography.titleLarge, color = fg)
                    Text(note as String, style = MaterialTheme.typography.bodySmall, color = Ink)
                }
            }
            Spacer(Modifier.height(14.dp))

            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Skor per subtes")
                    Text("Garis hitam = passing grade", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Spacer(Modifier.height(12.dp))
                Subtest.entries.forEach { st ->
                    val sc = r.of(st)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(st.label, style = MaterialTheme.typography.titleSmall, color = st.color())
                        Row(verticalAlignment = Alignment.Bottom) {
                            MonoText(sc.score.toString(), size = 20)
                            Text(" / ${sc.max}", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                    ScoreBar(sc.score, sc.max, sc.passingGrade, st.color(), st.tint(), height = 12)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            when {
                                sc.passingGrade == null -> "Dinilai dari total"
                                sc.pass -> "Lolos, lebih ${sc.margin} poin"
                                else -> "Kurang ${-sc.margin!!} poin"
                            },
                            style = MaterialTheme.typography.labelSmall, color = if (sc.pass) Pass else Fail
                        )
                        Text(
                            if (st == Subtest.TKP) "${sc.answered} dari ${sc.total} dijawab" else "${sc.score / 5} benar dari ${sc.total}",
                            style = MaterialTheme.typography.labelSmall, color = Muted
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                HorizontalDivider(color = Line)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Total", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.Bottom) {
                        MonoText(r.total.toString(), size = 22)
                        Text(" / ${Subtest.entries.sumOf { config.maxScore(it) }}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Waktu dipakai", formatClock(s.timeUsedSec), Modifier.weight(1f))
                Stat("Rata-rata/soal", "${if (s.answered > 0) s.timeUsedSec / s.answered else 0} dtk", Modifier.weight(1f))
                Stat("Dijawab", "${s.answered}/${s.total}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            if (s.worstTags.isNotEmpty()) {
                Panel {
                    Text("Paling banyak kehilangan poin", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    s.worstTags.forEach { t ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${t.subtest} · ${t.tag.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
                            Text(t.detail, style = MaterialTheme.typography.bodySmall, color = Fail, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBackHome, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Beranda") }
            Button(onClick = { onReview(s.session!!.id, 0) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Lihat pembahasan") }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Panel(modifier, padding = 12) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        MonoText(value, size = 16)
    }
}

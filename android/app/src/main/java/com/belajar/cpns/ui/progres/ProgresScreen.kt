package com.belajar.cpns.ui.progres

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.avgScore
import com.belajar.cpns.domain.avgSeconds
import com.belajar.cpns.domain.ratio
import com.belajar.cpns.domain.strength
import com.belajar.cpns.domain.tagLabel
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.SectionLabel
import com.belajar.cpns.ui.latihan.StrengthPill
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.TkpColor
import com.belajar.cpns.ui.theme.Warn
import com.belajar.cpns.ui.theme.color

@Composable
fun ProgresScreen(viewModel: ProgresViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    val pg = viewModel.passingGrade(s.category)

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Progres", style = MaterialTheme.typography.headlineMedium)
                Text("${s.tryouts.size} tryout · ${s.dailyCount} kuis harian · streak ${s.streak} hari", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (s.tagStats.isNotEmpty()) {
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Perkiraan SKD: ", style = MaterialTheme.typography.bodySmall, color = Muted)
                    MonoText("${s.estimate(viewModel.config)}", size = 14)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Skor tryout")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Subtest.entries.forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(10.dp).height(3.dp).background(st.color()))
                            Spacer(Modifier.width(4.dp))
                            Text(st.label, style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (s.tryouts.size < 2) {
                Text(
                    if (s.tryouts.isEmpty()) "Grafik muncul setelah tryout pertama selesai." else "Satu tryout tercatat. Grafik tren muncul setelah tryout kedua.",
                    style = MaterialTheme.typography.bodySmall, color = Muted
                )
                if (s.tryouts.size == 1) {
                    val t = s.tryouts.first()
                    Spacer(Modifier.height(8.dp))
                    Text("TWK ${t.scoreTWK} · TIU ${t.scoreTIU} · TKP ${t.scoreTKP} · total ${t.total}", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                TryoutChart(s.tryouts.reversed(), pg.pgTKP, viewModel.config.maxScore(Subtest.TKP))
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Peta kekuatan", style = MaterialTheme.typography.titleMedium)
            Text("akurasi · detik per soal", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(10.dp))
        if (s.tagStats.isEmpty()) {
            Panel { Text("Belum ada data. Kerjakan kuis harian atau tryout, nanti sub-materi yang lemah muncul di sini.", style = MaterialTheme.typography.bodySmall, color = Muted) }
        } else {
            Panel(padding = 0) {
                s.tagStats.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = Line)
                    val st = Subtest.from(t.subtest)
                    val strength = t.strength()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(6.dp).height(28.dp).clip(RoundedCornerShape(3.dp)).background(st.color()))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${st.label} · ${tagLabel(t.tag)}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                (if (st == Subtest.TKP) "skor rata-rata %.1f".format(t.avgScore()).replace('.', ',') else "akurasi ${(t.ratio() * 100).toInt()}%") + " · ${t.avgSeconds()} dtk · ${t.attempts} soal",
                                style = MaterialTheme.typography.bodySmall, color = Muted
                            )
                        }
                        Box(Modifier.width(64.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(Modifier.fillMaxWidth(t.ratio().toFloat().coerceIn(0f, 1f)).height(8.dp).background(when (strength) { Strength.LEMAH -> Fail; Strength.SEDANG -> Warn; Strength.KUAT -> Pass; Strength.BELUM -> Line }))
                        }
                        Spacer(Modifier.width(8.dp))
                        StrengthPill(strength)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Garis skor per subtes untuk tiap tryout, dengan garis putus-putus di passing grade TKP. */
@Composable
private fun TryoutChart(results: List<com.belajar.cpns.data.db.SessionResultEntity>, pgTkp: Int?, maxTkp: Int) {
    val labelColor = Muted
    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val left = 36f; val right = size.width - 8f; val top = 8f; val bottom = size.height - 20f
            val maxY = maxTkp.toFloat()
            fun y(v: Int) = bottom - (v / maxY) * (bottom - top)
            fun x(i: Int) = if (results.size == 1) (left + right) / 2 else left + (right - left) * i / (results.size - 1)
            listOf(0, 75, 150, 225).forEach { v ->
                drawLine(Line, Offset(left, y(v)), Offset(right, y(v)), strokeWidth = 1f)
            }
            if (pgTkp != null) drawLine(TkpColor, Offset(left, y(pgTkp)), Offset(right, y(pgTkp)), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val series = listOf(
                Subtest.TWK to results.map { it.scoreTWK },
                Subtest.TIU to results.map { it.scoreTIU },
                Subtest.TKP to results.map { it.scoreTKP }
            )
            series.forEach { (st, values) ->
                values.forEachIndexed { i, v ->
                    if (i > 0) drawLine(st.color(), Offset(x(i - 1), y(values[i - 1])), Offset(x(i), y(v)), strokeWidth = 5f, cap = StrokeCap.Round)
                }
                drawCircle(st.color(), radius = 7f, center = Offset(x(values.lastIndex), y(values.last())))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            results.forEachIndexed { i, _ -> Text("T${(i + 1).toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = labelColor) }
        }
        Spacer(Modifier.height(6.dp))
        val last = results.last()
        Text("Terakhir: TWK ${last.scoreTWK} · TIU ${last.scoreTIU} · TKP ${last.scoreTKP} · total ${last.total}. Garis putus-putus = PG TKP.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Suppress("unused")
private val unusedColor: Color = Color.Transparent

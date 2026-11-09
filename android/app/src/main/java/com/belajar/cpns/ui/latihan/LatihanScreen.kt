package com.belajar.cpns.ui.latihan

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.tagLabel
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.Pill
import com.belajar.cpns.ui.components.SectionLabel
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.FailTint
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.PassTint
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.Warn
import com.belajar.cpns.ui.theme.WarnTint
import com.belajar.cpns.ui.theme.color
import com.belajar.cpns.ui.theme.tint

@Composable
fun StrengthPill(strength: Strength) {
    when (strength) {
        Strength.LEMAH -> Pill("Lemah", Fail, FailTint)
        Strength.SEDANG -> Pill("Sedang", Warn, WarnTint)
        Strength.KUAT -> Pill("Kuat", Pass, PassTint)
        Strength.BELUM -> Pill("Belum", Muted, MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
fun LatihanScreen(onStartQuiz: (Long) -> Unit, viewModel: LatihanViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Latihan", style = MaterialTheme.typography.headlineMedium)
                    Text("Pilih materi, lalu atur jumlah soal.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Row(Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Warn, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${s.streak} hari", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(14.dp))

            // Kuis harian
            val dailyBg = if (s.dailyDoneToday && s.dailyRunningId == null) PassTint else Primary
            val dailyFg = if (s.dailyDoneToday && s.dailyRunningId == null) Pass else Color.White
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(dailyBg)
                    .clickable(enabled = !s.starting && !(s.dailyDoneToday && s.dailyRunningId == null)) { viewModel.startOrResumeDaily(onStartQuiz) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            s.dailyRunningId != null -> "Lanjutkan kuis hari ini"
                            s.dailyDoneToday -> "Kuis hari ini sudah selesai"
                            else -> "Kuis hari ini"
                        }, style = MaterialTheme.typography.titleMedium, color = dailyFg
                    )
                    Text(
                        if (s.dailyDoneToday && s.dailyRunningId == null) "Sampai jumpa besok. Streak aman." else "15 soal · sekitar 10 menit · fokus materi lemahmu",
                        style = MaterialTheme.typography.bodySmall, color = dailyFg.copy(alpha = 0.85f)
                    )
                }
                if (!(s.dailyDoneToday && s.dailyRunningId == null)) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = dailyFg)
            }
            Spacer(Modifier.height(10.dp))

            // Pengulangan
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (s.dueCount > 0) FailTint else MaterialTheme.colorScheme.surface)
                    .clickable(enabled = s.dueCount > 0 && !s.starting) { viewModel.startReview(onStartQuiz) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (s.dueCount > 0) Fail else Line), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (s.dueCount > 0) "${s.dueCount} soal jatuh tempo hari ini" else "Tidak ada soal untuk diulang", style = MaterialTheme.typography.titleSmall)
                    Text("Soal yang kehilangan poin muncul lagi setelah 1, 3, dan 7 hari.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                if (s.dueCount > 0) Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                SectionLabel("Materi")
                Text("${s.selected.size} dipilih · ${s.available} soal tersedia", style = MaterialTheme.typography.bodySmall, color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Subtest.entries.forEach { st ->
                    FilterChip(
                        selected = s.selectedSubtest == st, onClick = { viewModel.selectSubtest(st) }, label = { Text(st.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = st.tint(), selectedLabelColor = st.color(), labelColor = st.color())
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Panel(padding = 0) {
                val rows = s.tags.filter { it.subtest == s.selectedSubtest }
                rows.groupBy { it.group }.forEach { (group, items) ->
                    Text(tagLabel(group).uppercase(), style = MaterialTheme.typography.labelSmall, color = s.selectedSubtest.color(), modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 2.dp))
                    items.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.toggle(row.tag) }.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = row.tag in s.selected, onCheckedChange = { viewModel.toggle(row.tag) }, colors = CheckboxDefaults.colors(checkedColor = s.selectedSubtest.color()))
                            Text(tagLabel(row.tag), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${row.total} soal", style = MaterialTheme.typography.bodySmall, color = Muted)
                            Spacer(Modifier.width(8.dp))
                            StrengthPill(row.strength)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    HorizontalDivider(color = Line)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("JUMLAH SOAL", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.weight(1f))
                listOf(10, 20, 30).forEach { n ->
                    FilterChip(selected = s.count == n, onClick = { viewModel.setCount(n) }, label = { Text("$n") }, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.startPractice(onStartQuiz) }, enabled = s.selected.isNotEmpty() && !s.starting,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (s.selected.isEmpty()) "Pilih minimal satu materi" else "Mulai ${minOf(s.count, s.available)} soal", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

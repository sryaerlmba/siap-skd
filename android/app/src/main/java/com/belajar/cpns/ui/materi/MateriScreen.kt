package com.belajar.cpns.ui.materi

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.domain.Strength
import com.belajar.cpns.domain.tagLabel
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MarkdownView
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.ScaledText
import com.belajar.cpns.ui.components.pinchToZoom
import com.belajar.cpns.ui.components.SubtestChip
import com.belajar.cpns.ui.latihan.StrengthPill
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Pass
import com.belajar.cpns.ui.theme.color
import com.belajar.cpns.ui.theme.tint

@Composable
fun MateriScreen(onOpen: (String) -> Unit, viewModel: MateriViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Materi", style = MaterialTheme.typography.headlineMedium)
        Text("Mengikuti kisi-kisi resmi SKD. ${s.readCount} dari ${viewModel.total} halaman sudah dibaca.", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Subtest.entries.forEach { st ->
                FilterChip(
                    selected = s.subtest == st, onClick = { viewModel.select(st) }, label = { Text(st.label) }, modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = st.tint(), selectedLabelColor = st.color(), labelColor = st.color())
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        s.pages.groupBy { it.page.group }.forEach { (group, rows) ->
            Text(tagLabel(group).uppercase(), style = MaterialTheme.typography.labelMedium, color = s.subtest.color(), modifier = Modifier.padding(bottom = 6.dp))
            Panel(padding = 0) {
                rows.forEachIndexed { i, row ->
                    if (i > 0) HorizontalDivider(color = Line)
                    Row(Modifier.fillMaxWidth().clickable { onOpen(row.page.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(row.page.title, style = MaterialTheme.typography.titleSmall)
                                if (row.strength != Strength.BELUM) StrengthPill(row.strength)
                            }
                            Text("${row.page.readMinutes} menit baca" + if (row.page.tags.isNotEmpty()) " · ada latihan" else "", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (row.read) Icons.Default.CheckCircle else Icons.Outlined.Circle, contentDescription = if (row.read) "Sudah dibaca" else "Belum dibaca",
                            tint = if (row.read) Pass else Line, modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun MateriDetailScreen(onBack: () -> Unit, onStartQuiz: (Long) -> Unit, viewModel: MateriDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    val page = s.page
    if (page == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            SubtestChip(page.subtest)
            Spacer(Modifier.width(8.dp))
            Text(tagLabel(page.group), style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
            // Ukuran huruf: A- / persen (ketuk untuk reset) / A+
            IconButton(onClick = { viewModel.stepFont(-0.1f) }, enabled = s.fontScale > AppPreferences.MIN_FONT_SCALE + 0.01f) {
                Icon(Icons.Default.TextDecrease, contentDescription = "Perkecil huruf")
            }
            Text(
                "${(s.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Muted,
                modifier = Modifier.clickable { viewModel.resetFont() }.padding(horizontal = 2.dp, vertical = 8.dp)
            )
            IconButton(onClick = { viewModel.stepFont(0.1f) }, enabled = s.fontScale < AppPreferences.MAX_FONT_SCALE - 0.01f) {
                Icon(Icons.Default.TextIncrease, contentDescription = "Perbesar huruf")
            }
        }
        Column(
            Modifier.weight(1f).pinchToZoom { viewModel.zoomBy(it) }.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            ScaledText(s.fontScale) {
                Column {
                    Text(page.title, style = MaterialTheme.typography.headlineSmall)
                    Text("${page.readMinutes} menit baca · cubit layar untuk zoom", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Spacer(Modifier.height(12.dp))
                    MarkdownView(page.markdown)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { viewModel.toggleRead() }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                if (s.read) { Icon(Icons.Default.Check, contentDescription = null, tint = Pass); Spacer(Modifier.width(6.dp)) }
                Text(if (s.read) "Sudah dibaca" else "Tandai dibaca")
            }
            if (page.tags.isNotEmpty()) {
                Button(onClick = { viewModel.startPractice(onStartQuiz) }, enabled = !s.starting, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Latihan 10 soal")
                }
            }
        }
    }
}

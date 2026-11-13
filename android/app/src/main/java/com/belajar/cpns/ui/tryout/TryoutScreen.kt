package com.belajar.cpns.ui.tryout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.belajar.cpns.data.model.Question
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.MonoText
import com.belajar.cpns.ui.components.OptionLetter
import com.belajar.cpns.ui.components.OptionView
import com.belajar.cpns.ui.components.StemView
import com.belajar.cpns.ui.components.SubtestChip
import com.belajar.cpns.ui.components.formatClock
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.PrimaryTint
import com.belajar.cpns.ui.theme.Warn
import com.belajar.cpns.ui.theme.WarnTint
import com.belajar.cpns.ui.theme.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryoutScreen(
    onFinished: (Long) -> Unit,
    onExit: () -> Unit,
    viewModel: TryoutViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val s by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(s.finished) { if (s.finished) onFinished(s.sessionId) }
    // Tombol kembali tidak menutup sesi; timer tetap berjalan, sesi bisa dilanjutkan dari beranda.
    BackHandler { onExit() }

    if (s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val q = s.current ?: return
    val item = s.items[s.index]

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Header: paket, subtes, nomor, sisa waktu
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${s.title} · Paket CAT", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtestChip(q.subtest)
                    Text("Soal ${s.index + 1} / ${s.questions.size}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("SISA WAKTU", style = MaterialTheme.typography.labelSmall, color = Muted)
                MonoText(formatClock(s.remainingSec), size = 24, color = if (s.remainingSec < 300) Fail else Ink)
            }
        }
        LinearProgressIndicator(
            progress = { (s.index + 1).toFloat() / s.questions.size },
            modifier = Modifier.fillMaxWidth().height(4.dp), color = Primary, trackColor = Line
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            StemView(q)
            Spacer(Modifier.height(16.dp))
            q.options.forEach { op ->
                val selected = item.selected == op.key
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) PrimaryTint else MaterialTheme.colorScheme.surface)
                        .border(if (selected) 2.dp else 1.dp, if (selected) Primary else Line, RoundedCornerShape(12.dp))
                        .clickable { viewModel.select(op.key) }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OptionLetter(op.key, selected, Primary)
                    Spacer(Modifier.width(12.dp))
                    OptionView(q, op.text, bold = selected, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.toggleFlag() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Warn, containerColor = if (item.flagged) WarnTint else Color.Transparent),
                    border = BorderStroke(1.dp, if (item.flagged) Warn else Line)
                ) {
                    Icon(Icons.Outlined.Flag, contentDescription = null); Spacer(Modifier.width(6.dp))
                    Text(if (item.flagged) "Ragu-ragu" else "Tandai ragu-ragu")
                }
                Text("Jawaban bisa diubah sampai waktu habis.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }

        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { viewModel.prev() }, enabled = s.index > 0, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.ChevronLeft, null); Text("Sebelumnya")
            }
            OutlinedButton(onClick = { viewModel.showGrid(true) }, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.GridView, contentDescription = "Daftar nomor soal")
            }
            if (s.index < s.questions.lastIndex) {
                Button(onClick = { viewModel.next() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Berikutnya"); Icon(Icons.Default.ChevronRight, null)
                }
            } else {
                Button(onClick = { viewModel.askFinish(true) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Fail)) {
                    Text("Selesai")
                }
            }
        }
    }

    if (s.showGrid) {
        ModalBottomSheet(onDismissRequest = { viewModel.showGrid(false) }, containerColor = MaterialTheme.colorScheme.surface) {
            GridSheet(s, onPick = { viewModel.goTo(it) }, onFinish = { viewModel.showGrid(false); viewModel.askFinish(true) })
        }
    }

    if (s.confirmFinish) {
        AlertDialog(
            onDismissRequest = { viewModel.askFinish(false) },
            title = { Text("Selesaikan tryout?") },
            text = {
                Text(
                    if (s.emptyCount > 0) "${s.emptyCount} soal masih kosong dan akan dinilai 0. Untuk TKP, soal kosong juga 0 padahal semua opsi bernilai. Yakin selesai?"
                    else "Semua soal sudah dijawab. Setelah selesai, jawaban tidak bisa diubah."
                )
            },
            confirmButton = { Button(onClick = { viewModel.finish() }, colors = ButtonDefaults.buttonColors(containerColor = Fail)) { Text("Selesai sekarang") } },
            dismissButton = { TextButton(onClick = { viewModel.askFinish(false) }) { Text("Kembali ke soal") } }
        )
    }
}

@Composable
private fun GridSheet(s: TryoutUiState, onPick: (Int) -> Unit, onFinish: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nomor soal", style = MaterialTheme.typography.titleMedium)
                Text("Ketuk nomor untuk lompat", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            MonoText(formatClock(s.remainingSec), size = 20)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Primary, Color.Transparent, "Dijawab ${s.answeredCount}")
            Legend(WarnTint, Warn, "Ragu ${s.flaggedCount}")
            Legend(Color.White, Line, "Kosong ${s.emptyCount}")
        }
        Spacer(Modifier.height(12.dp))
        Subtest.entries.forEach { st ->
            val indices = s.questions.indices.filter { s.questions[it].subtest == st }
            if (indices.isEmpty()) return@forEach
            Text("${st.label} · ${indices.first() + 1} sampai ${indices.last() + 1}", style = MaterialTheme.typography.labelMedium, color = st.color())
            Spacer(Modifier.height(6.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().height(((indices.size + 9) / 10 * 38).dp),
                userScrollEnabled = false
            ) {
                items(indices) { i -> NumberCell(i, s, onPick) }
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onFinish, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Fail), colors = ButtonDefaults.outlinedButtonColors(contentColor = Fail)
        ) {
            Text(if (s.emptyCount > 0) "Selesaikan tryout (${s.emptyCount} soal masih kosong)" else "Selesaikan tryout")
        }
    }
}

@Composable
private fun NumberCell(i: Int, s: TryoutUiState, onPick: (Int) -> Unit) {
    val item = s.items[i]
    val isCurrent = i == s.index
    val (bg, fg, border) = when {
        item.flagged -> Triple(WarnTint, Warn, Warn)
        item.selected != null -> Triple(Primary, Color.White, Primary)
        else -> Triple(Color.White, Muted, Line)
    }
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(bg)
            .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) Ink else border, RoundedCornerShape(8.dp))
            .clickable { onPick(i) },
        contentAlignment = Alignment.Center
    ) {
        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (isCurrent && item.selected == null) Ink else fg)
    }
}

@Composable
private fun Legend(fill: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(12.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(fill).border(1.5.dp, border, RoundedCornerShape(4.dp)))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Suppress("unused")
private fun Question.firstLine(): String = stem.lineSequence().first()

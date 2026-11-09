package com.belajar.cpns.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.ui.AppViewModelProvider
import com.belajar.cpns.ui.components.Panel
import com.belajar.cpns.ui.components.Pill
import com.belajar.cpns.ui.components.SectionLabel
import com.belajar.cpns.ui.theme.Fail
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.PrimaryTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val category by viewModel.preferences.category.collectAsStateWithLifecycle()
    val examDate by viewModel.preferences.examDate.collectAsStateWithLifecycle()
    val extended by viewModel.preferences.extendedTime.collectAsStateWithLifecycle()
    val dailyTimer by viewModel.preferences.dailyTimer.collectAsStateWithLifecycle()
    val userName by viewModel.preferences.userName.collectAsStateWithLifecycle()
    val bankInfo by viewModel.bankInfo.collectAsStateWithLifecycle()

    var showCategory by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showName by remember { mutableStateOf(false) }

    val pg = viewModel.config.passingGrade(category)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            SectionLabel("Ujian", Modifier.padding(start = 4.dp, bottom = 8.dp))
            Panel(padding = 0) {
                SettingRow("Jalur pelamar", "Menentukan passing grade yang dipakai", pg.label) { showCategory = true }
                HorizontalDivider(color = Line)
                SettingRow("Tanggal ujian", "Hitung mundur di beranda", if (examDate > 0) fmtDate(examDate) else "Belum diisi") { showDate = true }
                HorizontalDivider(color = Line)
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Passing grade", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (pg.perSubtest) "TWK ${pg.pgTWK} · TIU ${pg.pgTIU} · TKP ${pg.pgTKP}" else "Total ${pg.pgCumulative} · TIU min ${pg.pgTIU}",
                            style = MaterialTheme.typography.bodySmall, color = Muted
                        )
                    }
                    Pill("Acuan ${viewModel.config.year}", Primary, PrimaryTint)
                }
            }
            Text("Angka diperbarui saat KepmenPANRB 2026 terbit.", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(16.dp))

            SectionLabel("Belajar", Modifier.padding(start = 4.dp, bottom = 8.dp))
            Panel(padding = 0) {
                SettingRow("Nama panggilan", "Dipakai di sapaan beranda", userName.ifBlank { "Belum diisi" }) { showName = true }
                HorizontalDivider(color = Line)
                ToggleRow("Timer 60 detik di kuis harian", "Latihan tempo; tryout tetap 100 menit", dailyTimer) { viewModel.setDailyTimer(it) }
                HorizontalDivider(color = Line)
                ToggleRow("Durasi tryout 130 menit", "Untuk pelamar disabilitas sensorik netra", extended) { viewModel.setExtendedTime(it) }
            }
            Spacer(Modifier.height(16.dp))

            SectionLabel("Data", Modifier.padding(start = 4.dp, bottom = 8.dp))
            Panel(padding = 0) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Paket soal", style = MaterialTheme.typography.titleSmall)
                        Text(bankInfo, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                HorizontalDivider(color = Line)
                Row(Modifier.fillMaxWidth().clickable { showClear = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hapus semua progres", style = MaterialTheme.typography.titleSmall, color = Fail)
                        Text("Riwayat tryout, kuis, dan statistik. Bank soal tetap ada.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Siap SKD 0.1.0 · Tidak terafiliasi dengan BKN atau KemenPANRB", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }

    if (showCategory) {
        AlertDialog(
            onDismissRequest = { showCategory = false },
            title = { Text("Jalur pelamar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ApplicantCategory.entries.forEach { c ->
                        val label = viewModel.config.passingGrade(c).label
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setCategory(c); showCategory = false }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(selected = c == category, onClick = { viewModel.setCategory(c); showCategory = false })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCategory = false }) { Text("Tutup") } }
        )
    }
    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (examDate > 0) examDate else null)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { viewModel.setExamDate(it) }; showDate = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { viewModel.setExamDate(0L); showDate = false }) { Text("Hapus") } }
        ) { DatePicker(state = state) }
    }
    if (showName) {
        var draft by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showName = false },
            title = { Text("Nama panggilan") },
            text = { OutlinedTextField(value = draft, onValueChange = { draft = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewModel.setUserName(draft.trim()); showName = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { showName = false }) { Text("Batal") } }
        )
    }
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Hapus semua progres?") },
            text = { Text("Semua riwayat tryout, jawaban, dan statistik akan dihapus permanen. Bank soal dan pengaturan tetap ada.") },
            confirmButton = { Button(onClick = { viewModel.clearProgress { showClear = false } }, colors = ButtonDefaults.buttonColors(containerColor = Fail)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SettingRow(title: String, note: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(note, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
    }
}

@Composable
private fun ToggleRow(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(note, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun fmtDate(millis: Long): String = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(millis))

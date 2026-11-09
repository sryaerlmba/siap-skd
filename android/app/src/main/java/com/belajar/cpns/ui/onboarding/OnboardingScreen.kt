package com.belajar.cpns.ui.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.ui.components.Pill
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.Primary
import com.belajar.cpns.ui.theme.TiuColor
import com.belajar.cpns.ui.theme.TiuTint
import com.belajar.cpns.ui.theme.TkpColor
import com.belajar.cpns.ui.theme.TkpTint
import com.belajar.cpns.ui.theme.TwkColor
import com.belajar.cpns.ui.theme.TwkTint

private data class CategoryOption(
    val category: ApplicantCategory,
    val title: String,
    val note: String,
    val pills: List<Triple<String, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>>
)

private val options = listOf(
    CategoryOption(
        ApplicantCategory.UMUM, "Umum", "Termasuk putra/putri Kalimantan. Tiap subtes harus lolos sendiri.",
        listOf(Triple("TWK 65", TwkColor, TwkTint), Triple("TIU 80", TiuColor, TiuTint), Triple("TKP 166", TkpColor, TkpTint))
    ),
    CategoryOption(
        ApplicantCategory.CUMLAUDE, "Lulusan cumlaude / diaspora", "Dinilai dari total, bukan per subtes.",
        listOf(Triple("Total 311", Ink, Line), Triple("TIU min 85", TiuColor, TiuTint))
    ),
    CategoryOption(
        ApplicantCategory.DISABILITAS, "Penyandang disabilitas / putra-putri Papua", "Opsi waktu 130 menit tersedia untuk disabilitas sensorik netra.",
        listOf(Triple("Total 286", Ink, Line), Triple("TIU min 60", TiuColor, TiuTint))
    )
)

@Composable
fun OnboardingScreen(preferences: AppPreferences, onDone: () -> Unit) {
    val selected by preferences.category.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Kamu mendaftar lewat jalur apa?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Passing grade tiap jalur berbeda. Pilihan ini menentukan garis batas lolos di semua tryout dan latihan. Bisa diubah kapan saja di pengaturan.",
            style = MaterialTheme.typography.bodyMedium, color = Muted
        )
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                val isSelected = opt.category == selected
                Card(
                    onClick = { preferences.setCategory(opt.category) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Primary else Line),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(selected = isSelected, onClick = { preferences.setCategory(opt.category) })
                        Column(Modifier.weight(1f).padding(top = 10.dp)) {
                            Text(opt.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(opt.note, style = MaterialTheme.typography.bodySmall, color = Muted)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                opt.pills.forEach { (t, c, bg) -> Pill(t, c, bg) }
                            }
                        }
                    }
                }
            }
            Text(
                "Angka mengikuti KepmenPANRB 321/2024. Begitu aturan 2026 terbit, konfigurasi diperbarui.",
                style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Lanjut", style = MaterialTheme.typography.titleMedium)
        }
    }
}

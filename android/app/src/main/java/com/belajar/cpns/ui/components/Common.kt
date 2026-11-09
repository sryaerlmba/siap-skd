package com.belajar.cpns.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.belajar.cpns.data.model.Subtest
import com.belajar.cpns.ui.theme.Ink
import com.belajar.cpns.ui.theme.Line
import com.belajar.cpns.ui.theme.Mono
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.theme.color
import com.belajar.cpns.ui.theme.tint

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Line),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}

@Composable
fun SubtestChip(subtest: Subtest, modifier: Modifier = Modifier) {
    Text(
        subtest.label,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(subtest.tint())
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = subtest.color(),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun Pill(text: String, color: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = Muted
    )
}

@Composable
fun MonoText(text: String, modifier: Modifier = Modifier, size: Int = 16, color: Color = Ink, weight: FontWeight = FontWeight.SemiBold) {
    Text(
        text,
        modifier = modifier,
        fontFamily = Mono,
        fontSize = androidx.compose.ui.unit.TextUnit(size.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = weight,
        color = color
    )
}

/** Bar skor dengan garis hitam di posisi passing grade. */
@Composable
fun ScoreBar(score: Int, max: Int, passingGrade: Int?, color: Color, track: Color, modifier: Modifier = Modifier, height: Int = 10) {
    val fraction = (score.toFloat() / max).coerceIn(0f, 1f)
    val pgFraction = passingGrade?.let { (it.toFloat() / max).coerceIn(0f, 1f) }
    Box(modifier.fillMaxWidth().height((height + 8).dp), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(height.dp / 2)).background(track)
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
        }
        if (pgFraction != null) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.fillMaxWidth(pgFraction))
                Box(Modifier.width(2.dp).height((height + 8).dp).background(Ink))
            }
        }
    }
}

@Composable
fun OptionLetter(letter: String, selected: Boolean, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(28.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Muted,
            textAlign = TextAlign.Center
        )
    }
}

fun formatClock(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

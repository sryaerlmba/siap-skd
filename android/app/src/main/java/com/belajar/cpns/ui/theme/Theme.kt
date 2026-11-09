package com.belajar.cpns.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.belajar.cpns.data.model.Subtest

// Palet mengikuti desain: latar terang, biru utama, warna subtes tetap di semua layar.
val Ground = Color(0xFFF3F5F8)
val Surface = Color(0xFFFFFFFF)
val Ink = Color(0xFF151B2B)
val Soft = Color(0xFF33364A)
val Muted = Color(0xFF5B6678)
val Line = Color(0xFFE1E6EE)
val Primary = Color(0xFF2F5BD9)
val PrimaryDark = Color(0xFF234AB8)
val PrimaryTint = Color(0xFFE8EEFC)
val Pass = Color(0xFF1E8A5A)
val PassTint = Color(0xFFE4F4EC)
val Fail = Color(0xFFC8403F)
val FailTint = Color(0xFFFBE9E8)
val Warn = Color(0xFFB87309)
val WarnTint = Color(0xFFFBF1DC)
val TwkColor = Color(0xFF2F5BD9)
val TwkTint = Color(0xFFE8EEFC)
val TiuColor = Color(0xFF7A4DD1)
val TiuTint = Color(0xFFEFE9FA)
val TkpColor = Color(0xFFB87309)
val TkpTint = Color(0xFFFBF1DC)
val TermBg = Color(0xFF1B1D2C)

fun Subtest.color(): Color = when (this) {
    Subtest.TWK -> TwkColor
    Subtest.TIU -> TiuColor
    Subtest.TKP -> TkpColor
}

fun Subtest.tint(): Color = when (this) {
    Subtest.TWK -> TwkTint
    Subtest.TIU -> TiuTint
    Subtest.TKP -> TkpTint
}

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryTint,
    onPrimaryContainer = PrimaryDark,
    secondary = TiuColor,
    onSecondary = Color.White,
    secondaryContainer = TiuTint,
    onSecondaryContainer = TiuColor,
    tertiary = TkpColor,
    onTertiary = Color.White,
    tertiaryContainer = TkpTint,
    onTertiaryContainer = TkpColor,
    background = Ground,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Muted,
    surfaceContainer = Surface,
    surfaceContainerLow = Surface,
    surfaceContainerHigh = Color(0xFFEEF1F6),
    outline = Line,
    outlineVariant = Line,
    error = Fail,
    onError = Color.White,
    errorContainer = FailTint,
    onErrorContainer = Fail
)

private val Base = Typography()
val Mono = FontFamily.Monospace

val AppTypography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 25.sp),
    bodyMedium = Base.bodyMedium.copy(lineHeight = 21.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
)

@Composable
fun SiapSkdTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)
}

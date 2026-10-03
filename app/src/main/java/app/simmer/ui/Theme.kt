package app.simmer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF1E211D)

/** A theme is a primary/secondary pair plus a ground; the rest is derived. */
data class Palette(
    val key: String,
    val primary: Color,
    val primaryDark: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val lightBg: Color,
    val darkBg: Color,
    val alwaysDark: Boolean = false,
)

val Palettes = listOf(
    Palette("herb", Color(0xFF1F6B47), Color(0xFF6FCB9B), Color(0xFFD2622E), Color(0xFFF0925F), Color(0xFFF8F6F1), Color(0xFF131512)),
    Palette("paprika", Color(0xFFB7431F), Color(0xFFF08A63), Color(0xFF2E6B4F), Color(0xFF7CCFA3), Color(0xFFFAF4EF), Color(0xFF191311)),
    Palette("midnight", Color(0xFF7FA7FF), Color(0xFF7FA7FF), Color(0xFFFFB86B), Color(0xFFFFB86B), Color(0xFF0E1320), Color(0xFF0E1320), alwaysDark = true),
    Palette("honey", Color(0xFF9A6B10), Color(0xFFE8B74A), Color(0xFF3E6E8E), Color(0xFF8BC0DE), Color(0xFFFCF7EA), Color(0xFF17140D)),
    Palette("ocean", Color(0xFF0F6E7A), Color(0xFF66C8D2), Color(0xFFD97A4A), Color(0xFFF2A277), Color(0xFFF1F7F7), Color(0xFF0F1A1C)),
    Palette("plum", Color(0xFF6A2E6B), Color(0xFFCB8FD0), Color(0xFFB08A3E), Color(0xFFE2BF78), Color(0xFFF8F3F8), Color(0xFF171017)),
    Palette("gold", Color(0xFFC9A227), Color(0xFFE6C65A), Color(0xFFE8E2D2), Color(0xFFE8E2D2), Color(0xFF111111), Color(0xFF111111), alwaysDark = true),
)

fun paletteFor(key: String) = Palettes.firstOrNull { it.key == key } ?: Palettes.first()

private fun Color.mix(other: Color, t: Float) = Color(
    red + (other.red - red) * t, green + (other.green - green) * t, blue + (other.blue - blue) * t, 1f,
)

private fun lightScheme(p: Palette): ColorScheme = lightColorScheme(
    primary = p.primary, onPrimary = Color.White,
    primaryContainer = p.primary.mix(Color.White, 0.84f), onPrimaryContainer = p.primary.mix(Color.Black, 0.5f),
    secondary = p.secondary, onSecondary = Color.White,
    secondaryContainer = p.secondary.mix(Color.White, 0.82f), onSecondaryContainer = p.secondary.mix(Color.Black, 0.55f),
    background = p.lightBg, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = p.lightBg.mix(Color(0xFFBDBDB5), 0.35f), onSurfaceVariant = Color(0xFF615E57),
    outline = Color(0xFFCFCBC2), outlineVariant = Color(0xFFE4E1DA),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
)

private fun darkScheme(p: Palette): ColorScheme = darkColorScheme(
    primary = p.primaryDark, onPrimary = p.primaryDark.mix(Color.Black, 0.75f),
    primaryContainer = p.primaryDark.mix(p.darkBg, 0.75f), onPrimaryContainer = p.primaryDark.mix(Color.White, 0.6f),
    secondary = p.secondaryDark, onSecondary = p.secondaryDark.mix(Color.Black, 0.75f),
    secondaryContainer = p.secondaryDark.mix(p.darkBg, 0.72f), onSecondaryContainer = p.secondaryDark.mix(Color.White, 0.6f),
    background = p.darkBg, onBackground = Color(0xFFEDEBE6),
    surface = p.darkBg.mix(Color.White, 0.06f), onSurface = Color(0xFFEDEBE6),
    surfaceVariant = p.darkBg.mix(Color.White, 0.12f), onSurfaceVariant = Color(0xFFA7A49C),
    outline = Color(0xFF454841), outlineVariant = Color(0xFF30332E),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF5C1A17), onErrorContainer = Color(0xFFF9DEDC),
)

private fun typography(serifHeadings: Boolean): Typography {
    val display = if (serifHeadings) FontFamily.Serif else FontFamily.SansSerif
    val w = if (serifHeadings) FontWeight.Bold else FontWeight.ExtraBold
    return Typography(
        displaySmall = TextStyle(fontFamily = display, fontWeight = w, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = w, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = w, fontSize = 22.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = display, fontWeight = w, fontSize = 20.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = display, fontWeight = w, fontSize = 17.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
        labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    )
}

@Composable
fun SimmerTheme(themeKey: String = "herb", sansFont: Boolean = false, content: @Composable () -> Unit) {
    val p = paletteFor(themeKey)
    val dark = p.alwaysDark || isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkScheme(p) else lightScheme(p),
        typography = typography(serifHeadings = !sansFont),
        content = content,
    )
}

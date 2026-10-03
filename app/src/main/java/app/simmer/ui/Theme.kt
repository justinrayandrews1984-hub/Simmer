package app.simmer.ui

import androidx.compose.foundation.isSystemInDarkTheme
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

// Palette: deep herb green, warm paprika accent, and a soft linen ground.
val Herb = Color(0xFF1F6B47)
val HerbSoft = Color(0xFFDDEFE3)
val Paprika = Color(0xFFD2622E)
val PaprikaSoft = Color(0xFFFBE4D6)
val Linen = Color(0xFFF8F6F1)
val Ink = Color(0xFF1E211D)

private val Light = lightColorScheme(
    primary = Herb,
    onPrimary = Color.White,
    primaryContainer = HerbSoft,
    onPrimaryContainer = Color(0xFF0E3A25),
    secondary = Paprika,
    onSecondary = Color.White,
    secondaryContainer = PaprikaSoft,
    onSecondaryContainer = Color(0xFF5A2410),
    tertiary = Color(0xFF8A6D2E),
    background = Linen,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDEAE3),
    onSurfaceVariant = Color(0xFF615E57),
    outline = Color(0xFFCFCBC2),
    outlineVariant = Color(0xFFE4E1DA),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF6FCB9B),
    onPrimary = Color(0xFF0B2A1A),
    primaryContainer = Color(0xFF1C3C2B),
    onPrimaryContainer = Color(0xFFCDEBD9),
    secondary = Color(0xFFF0925F),
    onSecondary = Color(0xFF3A1708),
    secondaryContainer = Color(0xFF4A2616),
    onSecondaryContainer = Color(0xFFFFDCCB),
    tertiary = Color(0xFFD9BB76),
    background = Color(0xFF131512),
    onBackground = Color(0xFFEDEBE6),
    surface = Color(0xFF1C1F1B),
    onSurface = Color(0xFFEDEBE6),
    surfaceVariant = Color(0xFF2A2D28),
    onSurfaceVariant = Color(0xFFA7A49C),
    outline = Color(0xFF454841),
    outlineVariant = Color(0xFF30332E),
    error = Color(0xFFF2B8B5),
)

private val Display = FontFamily.Serif

val SimmerTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun SimmerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = SimmerTypography,
        content = content,
    )
}

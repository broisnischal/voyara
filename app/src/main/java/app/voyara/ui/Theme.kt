package app.voyara.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.voyara.R

// Monochrome: neutrals carry the whole UI; the one hue, red, only ever means "live mock".
// primary = the strongest neutral (ink), tertiary/error = the live red.
private val Light = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E4E4),
    onPrimaryContainer = Color(0xFF0A0A0A),
    secondary = Color(0xFF3D3D3D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEDEDE),
    onSecondaryContainer = Color(0xFF0A0A0A),
    tertiary = Color(0xFFD7261E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDEDED),
    onTertiaryContainer = Color(0xFF0A0A0A),
    error = Color(0xFFD7261E),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF555555),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    outline = Color(0xFF8C8C8C),
    outlineVariant = Color(0xFFD4D4D4),
    inverseSurface = Color(0xFF1C1C1C),
    inverseOnSurface = Color(0xFFF2F2F2),
    inversePrimary = Color(0xFFFFFFFF),
)

// True black for the OLED panel; elevation reads as lighter greys.
private val Dark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2B2B2B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFC9C9C9),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF4136),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF1F1F1F),
    onTertiaryContainer = Color(0xFFF2F2F2),
    error = Color(0xFFFF4136),
    onError = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFA6A6A6),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF212121),
    surfaceContainerHighest = Color(0xFF2B2B2B),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF2C2C2C),
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF0A0A0A),
)

// The real-location dot. The one hue besides the live red: the two must read apart at a glance.
val YouLight = Color(0xFF1A73E8)
val YouDark = Color(0xFF5B9BFF)

private fun inter(weight: Int, opticalSize: Float) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opticalSize)),
)

// One family, two optical sizes: text cuts for UI sizes, display cuts for large titles.
private val Inter = FontFamily(inter(400, 14f), inter(500, 14f), inter(600, 14f), inter(700, 14f))
private val InterDisplay = FontFamily(inter(400, 32f), inter(500, 32f), inter(600, 32f), inter(700, 32f))

private val Type = Typography().let { t ->
    fun TextStyle.text(tracking: Float = 0f) = copy(fontFamily = Inter, letterSpacing = tracking.sp)
    fun TextStyle.display(tracking: Float) = copy(fontFamily = InterDisplay, letterSpacing = tracking.sp)
    Typography(
        displayLarge = t.displayLarge.display(-1f),
        displayMedium = t.displayMedium.display(-0.8f),
        displaySmall = t.displaySmall.display(-0.6f),
        headlineLarge = t.headlineLarge.display(-0.6f),
        headlineMedium = t.headlineMedium.display(-0.4f),
        headlineSmall = t.headlineSmall.display(-0.3f),
        titleLarge = t.titleLarge.display(-0.2f).copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.text(-0.1f).copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.text().copy(fontWeight = FontWeight.SemiBold),
        // Body copy at reading sizes wants no extra tracking (M3's defaults are tuned for Roboto).
        bodyLarge = t.bodyLarge.text(),
        bodyMedium = t.bodyMedium.text(),
        bodySmall = t.bodySmall.text(0.1f),
        labelLarge = t.labelLarge.text(0.1f).copy(fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.text(0.2f),
        labelSmall = t.labelSmall.text(0.3f),
    )
}

/** Digits of equal width, for values that change (coordinates, distances, speeds). */
fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

@Composable
fun VoyaraTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Type, content = content)
}

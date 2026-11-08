package io.lemoncore.opendatadex.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.ThemeMode

/** The app's colour tokens: a true-black OLED dark theme and a warm off-white light theme. Red is only for actions, caught and progress. */
@Immutable
data class DexColors(
    val isDark: Boolean,
    val bg: Color,
    val bgRaised: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val lineStrong: Color,
    val divider: Color,
    val track: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val textFaint: Color,
    val accent: Color,
    val accentText: Color,
    val accentTint: Color,
    val gold: Color,
    val goldText: Color,
    /** Number pill background when not caught. */
    val pillOff: Color,
    /** Swipe background when the swipe would unmark a caught Pokémon. */
    val swipeUndo: Color,
    /** Selected segment of a segmented control, and its label. */
    val segmentOn: Color,
    val segmentOnText: Color,
    /** Text and icons on an [accent] fill (the Search button). */
    val onAccent: Color = Color.White,
) {
    // Older names still used by screens that only take colours from the theme.
    val chip get() = surface2
    val chipText get() = textSecondary
    val sectionCard get() = surface
    val subtleText get() = textSubtle
}

val DarkDex = DexColors(
    isDark = true, bg = Color(0xFF050506), bgRaised = Color(0xFF0B0B0D), surface = Color(0xFF0C0C0F), surface2 = Color(0xFF0F1013),
    surface3 = Color(0xFF16171B), line = Color(0xFF1D1E23), lineStrong = Color(0xFF26272D), divider = Color(0xFF17181C),
    track = Color(0xFF17181C), text = Color(0xFFEEEEF0), textSecondary = Color(0xFFC9CAD0), textMuted = Color(0xFF9A9BA3),
    textSubtle = Color(0xFF8D8E96), textFaint = Color(0xFF6B6C74), accent = Color(0xFFEF4B43), accentText = Color(0xFFFF8A80),
    accentTint = Color(0xFFEF4B43).copy(alpha = 0.14f), gold = Color(0xFFF2C14E), goldText = Color(0xFFF2C14E),
    pillOff = Color(0xFF16171B), swipeUndo = Color(0xFF2A2B31), segmentOn = Color(0xFFEEEEF0), segmentOnText = Color(0xFF050506),
)

val LightDex = DexColors(
    isDark = false, bg = Color(0xFFF5F5F3), bgRaised = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFFFFFFF),
    surface3 = Color(0xFFF0F0EE), line = Color(0xFFE4E4E7), lineStrong = Color(0xFFE4E4E7), divider = Color(0xFFE4E4E7),
    track = Color(0xFFECECEF), text = Color(0xFF16171B), textSecondary = Color(0xFF3A3B42), textMuted = Color(0xFF5B5C64),
    textSubtle = Color(0xFF6B6C74), textFaint = Color(0xFF8D8E96), accent = Color(0xFFE5372F), accentText = Color(0xFFC62A22),
    accentTint = Color(0xFFE5372F).copy(alpha = 0.12f), gold = Color(0xFFD9A21B), goldText = Color(0xFFA87A0C),
    pillOff = Color(0xFF14141E).copy(alpha = 0.06f), swipeUndo = Color(0xFFC9CAD0), segmentOn = Color(0xFF16171B), segmentOnText = Color.White,
)

val LocalDexColors = staticCompositionLocalOf { DarkDex }

/**
 * These colours with the accent, background, chips and lines taken from a game's [hue] (Tint from game). Types, washes,
 * gold and the Poké Ball keep their own colours.
 */
fun DexColors.tinted(hue: Float): DexColors {
    val on = if (isDark) Color(0xFF050506) else Color.White
    // The accent darkens until its label passes 4.5:1 (light yellows and greens need it).
    var l = if (isDark) 0.70f else 0.58f
    var accent = oklch(l, 0.16f, hue)
    while (contrast(on, accent) < 4.5f && l > 0.3f) {
        l -= 0.03f
        accent = oklch(l, 0.16f, hue)
    }
    return if (isDark) copy(
        accent = accent, accentTint = accent.copy(alpha = 0.16f), accentText = oklch(0.85f, 0.11f, hue), onAccent = on,
        bg = oklch(0.115f, 0.012f, hue), surface2 = oklch(0.16f, 0.016f, hue), line = oklch(0.27f, 0.03f, hue), lineStrong = oklch(0.27f, 0.03f, hue),
    ) else copy(
        accent = accent, accentTint = accent.copy(alpha = 0.12f), accentText = oklch(0.42f, 0.13f, hue), onAccent = on,
        bg = oklch(0.97f, 0.008f, hue), line = oklch(0.88f, 0.03f, hue), lineStrong = oklch(0.88f, 0.03f, hue),
    )
}

/** WCAG contrast ratio between two opaque colours (1 to 21). */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** The tinted colours ease into a newly picked game rather than snapping. */
@Composable
private fun DexColors.animated(): DexColors {
    @Composable fun a(c: Color) = animateColorAsState(c, tween(400), label = "tint").value
    return copy(
        accent = a(accent), accentTint = a(accentTint), accentText = a(accentText), onAccent = a(onAccent),
        bg = a(bg), surface2 = a(surface2), line = a(line), lineStrong = a(lineStrong),
    )
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) = Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** UI typeface: every word in the app. */
val Manrope = FontFamily(
    variable(R.font.manrope, FontWeight.Normal), variable(R.font.manrope, FontWeight.Medium), variable(R.font.manrope, FontWeight.SemiBold),
    variable(R.font.manrope, FontWeight.Bold), variable(R.font.manrope, FontWeight.ExtraBold),
)

/** Number typeface: dex numbers, stats, counts and overlines. */
val JetBrainsMono = FontFamily(
    variable(R.font.jetbrains_mono, FontWeight.Normal), variable(R.font.jetbrains_mono, FontWeight.Medium),
    variable(R.font.jetbrains_mono, FontWeight.SemiBold), variable(R.font.jetbrains_mono, FontWeight.Bold),
)

/** Named text styles from the design. Colours are set where they are used. */
object DexType {
    private fun m(size: Int, weight: FontWeight, spacing: Double = 0.0) =
        TextStyle(fontFamily = Manrope, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.em)
    private fun mono(size: Int, weight: FontWeight, spacing: Double = 0.0) =
        TextStyle(fontFamily = JetBrainsMono, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.em)

    val wordmark = m(22, FontWeight.ExtraBold, -0.03)
    val wordmarkLarge = m(30, FontWeight.ExtraBold, -0.04)
    val screenTitle = m(22, FontWeight.ExtraBold, -0.02)
    val sheetTitle = m(20, FontWeight.ExtraBold, -0.02)
    val detailName = m(30, FontWeight.ExtraBold, -0.03).copy(lineHeight = 31.5.sp)
    val cardName = m(18, FontWeight.ExtraBold, -0.01).copy(lineHeight = 22.sp)
    val sectionTitle = m(15, FontWeight.ExtraBold)
    val itemTitle = m(14, FontWeight.ExtraBold)
    val fab = m(14, FontWeight.ExtraBold)
    val tab = m(13, FontWeight.Bold)
    val body = m(13, FontWeight.SemiBold)
    val chip = m(12, FontWeight.Bold).copy(lineHeight = 16.sp)
    val typeTag = m(11, FontWeight.Bold).copy(lineHeight = 14.sp)
    val drawerItem = m(14, FontWeight.Bold)
    val caption = m(12, FontWeight.SemiBold)
    val monoNumber = mono(12, FontWeight.SemiBold).copy(lineHeight = 16.sp)
    val monoValue = mono(13, FontWeight.Medium)
    val monoMeta = mono(11, FontWeight.SemiBold)
    val monoCount = mono(10, FontWeight.Medium)
    val overline = mono(10, FontWeight.SemiBold, 0.08)
}

private fun appTypography(): Typography {
    val base = Typography()
    fun TextStyle.m(w: FontWeight, size: Int? = null, spacing: Double = 0.0) =
        copy(fontFamily = Manrope, fontWeight = w, fontSize = size?.sp ?: fontSize, letterSpacing = spacing.em)
    return Typography(
        displayLarge = base.displayLarge.m(FontWeight.ExtraBold, spacing = -0.03), displayMedium = base.displayMedium.m(FontWeight.ExtraBold, spacing = -0.03),
        displaySmall = base.displaySmall.m(FontWeight.ExtraBold, spacing = -0.02),
        headlineLarge = base.headlineLarge.m(FontWeight.ExtraBold, spacing = -0.02), headlineMedium = base.headlineMedium.m(FontWeight.ExtraBold, spacing = -0.02),
        headlineSmall = base.headlineSmall.m(FontWeight.ExtraBold, spacing = -0.02),
        titleLarge = base.titleLarge.m(FontWeight.ExtraBold, 22, -0.02), titleMedium = base.titleMedium.m(FontWeight.ExtraBold, 16),
        titleSmall = base.titleSmall.m(FontWeight.Bold, 14),
        bodyLarge = base.bodyLarge.m(FontWeight.Medium, 15), bodyMedium = base.bodyMedium.m(FontWeight.Medium, 14),
        bodySmall = base.bodySmall.m(FontWeight.Medium, 12),
        labelLarge = base.labelLarge.m(FontWeight.Bold, 14), labelMedium = base.labelMedium.m(FontWeight.Bold, 12),
        labelSmall = base.labelSmall.m(FontWeight.Bold, 11),
    )
}

private val Light = lightColorScheme(
    primary = Color(0xFFE5372F), onPrimary = Color.White, secondary = Color(0xFFE5372F), onSecondary = Color.White,
    background = Color(0xFFF5F5F3), onBackground = Color(0xFF16171B), surface = Color(0xFFF5F5F3), onSurface = Color(0xFF16171B),
    surfaceVariant = Color(0xFFF0F0EE), onSurfaceVariant = Color(0xFF5B5C64), outline = Color(0xFFE4E4E7), outlineVariant = Color(0xFFE4E4E7),
    error = Color(0xFFC62A22), surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFAF9),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF), surfaceContainerHighest = Color(0xFFF0F0EE),
    secondaryContainer = Color(0xFFE5372F).copy(alpha = 0.12f), onSecondaryContainer = Color(0xFFC62A22),
    primaryContainer = Color(0xFFE5372F).copy(alpha = 0.12f), onPrimaryContainer = Color(0xFFC62A22),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFEF4B43), onPrimary = Color.White, secondary = Color(0xFFEF4B43), onSecondary = Color.White,
    background = Color(0xFF050506), onBackground = Color(0xFFEEEEF0), surface = Color(0xFF050506), onSurface = Color(0xFFEEEEF0),
    surfaceVariant = Color(0xFF0F1013), onSurfaceVariant = Color(0xFF9A9BA3), outline = Color(0xFF26272D), outlineVariant = Color(0xFF1D1E23),
    error = Color(0xFFFF7E76), surfaceContainerLowest = Color(0xFF050506), surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF0C0C0F), surfaceContainerHigh = Color(0xFF0F1013), surfaceContainerHighest = Color(0xFF16171B),
    secondaryContainer = Color(0xFFEF4B43).copy(alpha = 0.14f), onSecondaryContainer = Color(0xFFFF8A80),
    primaryContainer = Color(0xFFEF4B43).copy(alpha = 0.14f), onPrimaryContainer = Color(0xFFFF8A80),
)

private val Typography = appTypography()

@Composable
fun OpenDataDexTheme(mode: ThemeMode, tintHue: Float? = null, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // The app's own theme setting can differ from the phone's, so the status and navigation bar icons follow it.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    val base = if (dark) DarkDex else LightDex
    val colors = (if (tintHue != null) base.tinted(tintHue) else base).animated()
    // Material parts (switches, radio buttons, buttons, the text cursor) follow the same accent and background.
    val scheme = (if (dark) Dark else Light).copy(
        primary = colors.accent, onPrimary = colors.onAccent, secondary = colors.accent, onSecondary = colors.onAccent,
        primaryContainer = colors.accentTint, onPrimaryContainer = colors.accentText,
        secondaryContainer = colors.accentTint, onSecondaryContainer = colors.accentText,
        background = colors.bg, surface = colors.bg,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalDexColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}

/** Move category colours: Physical and Special tinted in dark mode, all grey in light mode. */
object CategoryColors {
    fun of(category: Int, dark: Boolean): Color = if (!dark) Color(0xFF6B6C74) else when (category) {
        2 -> Color(0xFFFBA587); 3 -> Color(0xFFA1BDF9); else -> Color(0xFF8D8E96)
    }
    @androidx.annotation.StringRes fun label(category: Int) = when (category) {
        2 -> R.string.category_physical; 3 -> R.string.category_special
        else -> R.string.category_status
    }
}

/** Damage multiplier label colours (4×, 2×, ½×, ¼×, 0×), in percent. */
fun multiplierColor(percent: Int, dark: Boolean): Color = when {
    percent >= 400 -> if (dark) Color(0xFFFF7E76) else Color(0xFFC62A22)
    percent > 100 -> if (dark) Color(0xFFFCA676) else Color(0xFFC62A22)
    percent == 0 -> if (dark) Color(0xFF9A9BA3) else Color(0xFF6B6C74)
    percent <= 25 -> if (dark) Color(0xFF67D2CC) else Color(0xFF00774B)
    percent < 100 -> if (dark) Color(0xFF82D2A8) else Color(0xFF00774B)
    else -> if (dark) Color(0xFFEEEEF0) else Color(0xFF16171B)
}

/** Fallback colours from PokéAPI's species colour, used until the artwork's palette has been read. */
fun speciesColor(colorId: Int): Color = when (colorId) {
    1 -> Color(0xFF8E9AA6); 2 -> Color(0xFF7EB6D9); 3 -> Color(0xFFC9A27E); 4 -> Color(0xFFA7B0B8); 5 -> Color(0xFF8ACBB0)
    6 -> Color(0xFFF2A9C4); 7 -> Color(0xFFB39DDB); 8 -> Color(0xFFEE837A); 9 -> Color(0xFFD5DBE0); 10 -> Color(0xFFF2D27A)
    else -> Color(0xFFB0BEC5)
}

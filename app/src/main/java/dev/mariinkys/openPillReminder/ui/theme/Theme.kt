package dev.mariinkys.openPillReminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.mariinkys.openPillReminder.model.SettingsState
import dev.mariinkys.openPillReminder.model.ThemeMode
import kotlin.math.abs

@Composable
fun OpenPillReminderTheme(
    settings: SettingsState,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        settings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> generateColorSchemeFromSeed(settings.seedColor, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Converts a Color to HSL floats: [hue 0–360, saturation 0–1, lightness 0–1]
 */
private fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val l = (max + min) / 2f

    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))

    val h = when {
        delta == 0f -> 0f
        max == r -> (((g - b) / delta) % 6f + 6f) % 6f * 60f
        max == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }

    return floatArrayOf(h, s, l)
}

/**
 * Builds a Color from HSL. Hue is automatically wrapped to [0, 360).
 */
private fun colorFromHsl(
    hue: Float,
    saturation: Float,
    lightness: Float,
    alpha: Float = 1f
): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)

    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m, alpha)
}

private fun generateColorSchemeFromSeed(seedColor: Int, isDark: Boolean): ColorScheme {
    val base = Color(seedColor)
    val (hue, sat, _) = base.toHsl()

    val s = sat.coerceIn(0.35f, 0.75f)

    val secondaryHue = hue + 30f
    val tertiaryHue = hue + 150f

    return if (isDark) {
        darkColorScheme(
            primary = colorFromHsl(hue, s, 0.78f),
            onPrimary = colorFromHsl(hue, s, 0.15f),
            primaryContainer = colorFromHsl(hue, s, 0.25f),
            onPrimaryContainer = colorFromHsl(hue, s, 0.90f),

            secondary = colorFromHsl(secondaryHue, s, 0.75f),
            onSecondary = colorFromHsl(secondaryHue, s, 0.15f),
            secondaryContainer = colorFromHsl(secondaryHue, s, 0.22f),
            onSecondaryContainer = colorFromHsl(secondaryHue, s, 0.90f),

            tertiary = colorFromHsl(tertiaryHue, s, 0.80f),
            onTertiary = colorFromHsl(tertiaryHue, s, 0.15f),
            tertiaryContainer = colorFromHsl(tertiaryHue, s, 0.25f),
            onTertiaryContainer = colorFromHsl(tertiaryHue, s, 0.92f),

            surface = Color(0xFF121212),
            onSurface = Color(0xFFE6E1E5),
            background = Color(0xFF121212),
            surfaceVariant = Color(0xFF1E1E1E),
        )
    } else {
        lightColorScheme(
            primary = colorFromHsl(hue, s, 0.38f),
            onPrimary = Color.White,
            primaryContainer = colorFromHsl(hue, s, 0.92f),
            onPrimaryContainer = colorFromHsl(hue, s, 0.15f),

            secondary = colorFromHsl(secondaryHue, s, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = colorFromHsl(secondaryHue, s, 0.90f),
            onSecondaryContainer = colorFromHsl(secondaryHue, s, 0.15f),

            tertiary = colorFromHsl(tertiaryHue, s, 0.38f),
            onTertiary = Color.White,
            tertiaryContainer = colorFromHsl(tertiaryHue, s, 0.91f),
            onTertiaryContainer = colorFromHsl(tertiaryHue, s, 0.13f),

            surface = Color.White,
            onSurface = Color(0xFF1C1B1F),
            background = Color.White,
            surfaceVariant = Color(0xFFF5F0FF),
        )
    }
}
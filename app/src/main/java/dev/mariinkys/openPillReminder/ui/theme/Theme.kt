package dev.mariinkys.openPillReminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.mariinkys.openPillReminder.model.SettingsState
import dev.mariinkys.openPillReminder.model.ThemeMode

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

private fun generateColorSchemeFromSeed(seedColor: Int, isDark: Boolean): ColorScheme {
    // Derive tonal variations by blending with black/white
    fun tone(color: Int, factor: Float): Color {
        val blend = if (factor > 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val amount = kotlin.math.abs(factor)
        val r = ((color shr 16 and 0xFF) * (1 - amount) + (blend shr 16 and 0xFF) * amount).toInt()
        val g = ((color shr 8 and 0xFF) * (1 - amount) + (blend shr 8 and 0xFF) * amount).toInt()
        val b = ((color and 0xFF) * (1 - amount) + (blend and 0xFF) * amount).toInt()
        return Color(r, g, b)
    }

    fun luminance(color: Int): Float {
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    val onSeedColor = if (luminance(seedColor) > 0.5f) Color.Black else Color.White

    return if (isDark) {
        darkColorScheme(
            primary = tone(seedColor, 0.6f),
            onPrimary = Color.Black,
            primaryContainer = tone(seedColor, -0.2f),
            onPrimaryContainer = tone(seedColor, 0.8f),
            secondary = tone(seedColor, 0.4f),
            onSecondary = Color.Black,
            secondaryContainer = tone(seedColor, -0.3f),
            onSecondaryContainer = tone(seedColor, 0.7f),
            tertiary = tone(seedColor, 0.5f),
            onTertiary = Color.Black,
            tertiaryContainer = tone(seedColor, -0.25f),
            onTertiaryContainer = tone(seedColor, 0.75f),
        )
    } else {
        lightColorScheme(
            primary = Color(seedColor),
            onPrimary = onSeedColor,
            primaryContainer = tone(seedColor, 0.8f),
            onPrimaryContainer = tone(seedColor, -0.6f),
            secondary = tone(seedColor, -0.2f),
            onSecondary = onSeedColor,
            secondaryContainer = tone(seedColor, 0.7f),
            onSecondaryContainer = tone(seedColor, -0.5f),
            tertiary = tone(seedColor, -0.1f),
            onTertiary = onSeedColor,
            tertiaryContainer = tone(seedColor, 0.75f),
            onTertiaryContainer = tone(seedColor, -0.55f),
        )
    }
}
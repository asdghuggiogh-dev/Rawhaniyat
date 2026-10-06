package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val titleArabic: String, val icon: String) {
    SYSTEM("تلقائي حسب إعدادات الهاتف", "📱"),
    LIGHT("الوضع الفاتح", "☀️"),
    DARK("الوضع الداكن", "🌙")
}

private val DarkColorScheme = darkColorScheme(
    primary = SoftEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftEmeraldContainerDark,
    onPrimaryContainer = SoftEmeraldLight,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E2405),
    onSecondaryContainer = GoldSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF404040)
)

private val LightColorScheme = lightColorScheme(
    primary = SoftEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftEmeraldContainer,
    onPrimaryContainer = SoftEmeraldDark,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    background = LightBackground, // أبيض نقي
    onBackground = LightTextPrimary,
    surface = LightSurface, // أبيض نقي
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant, // رمادي خفيف جداً
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun RowhaniyatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveOnSecondary,
    tertiary = ImmersiveGold,
    background = ImmersiveDarkBg,
    surface = ImmersiveDarkCard,
    onBackground = ImmersiveTextMain,
    onSurface = ImmersiveTextMain,
    surfaceVariant = ImmersiveDarkCard,
    onSurfaceVariant = ImmersiveTextMuted
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ImmersiveOnPrimary,
    onPrimary = Color.White,
    secondary = ImmersivePrimary,
    onSecondary = ImmersiveOnPrimary,
    tertiary = ImmersiveGold,
    background = LightBg,
    surface = LightCard,
    onBackground = DarkGrayText,
    onSurface = DarkGrayText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightGrayText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default dynamicColor to false to maintain our hand-crafted, beautiful design consistently
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

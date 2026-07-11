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
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = KajuGold,
    secondary = KajuTan,
    tertiary = PistachioGreen,
    background = KajuCream,
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = ChocolateBrown,
    onBackground = ChocolateBrown,
    onSurface = ChocolateBrown
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Enforce clean, elegant light-theme aesthetic as requested
  dynamicColor: Boolean = false, // Maintain branded colors
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

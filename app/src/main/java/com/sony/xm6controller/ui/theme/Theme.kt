package com.sony.xm6controller.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sony.xm6controller.domain.ThemeSkin

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimary,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SonySurfaceVariant,
    secondary = SonyGold
)

@Composable
fun XM6ControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeSkin: ThemeSkin = ThemeSkin.BLACK_COPPER,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> darkColorScheme(
            primary = themeSkin.primaryGold,
            onPrimary = Color.Black,
            secondary = themeSkin.accentCopper,
            onSecondary = Color.Black,
            background = themeSkin.backgroundObsidian,
            onBackground = Color(0xFFF1F5F9),
            surface = themeSkin.surfaceDark,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = themeSkin.surfaceDark,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = themeSkin.cardBorder,
            outlineVariant = themeSkin.cardBorder.copy(alpha = 0.5f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}



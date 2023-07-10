package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = AdditionalColors.PrimaryDarkerBlue,
    secondary = AdditionalColors.PrimaryBlue,
    tertiary = Pink40,
//    background = Color.Red,
//    surface = Color.Red,
//    onPrimary = Color.Red,
//    onSecondary = Color.Red,
//    onTertiary = Color.Red,
//    onBackground = Color.Red,
//    onSurface = Color.Red,

//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFC2DDF0),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
)


object AdditionalColors {
    var Enabled_Green = Color(0xff28B740)
    var Disabled_Red = Color(0xffB23131)
    var OrangePastel = Color(0xfff0d5c2)
    var CardSurface = Color(0xFFC2DDF0) //0xFFC2DDF0
    var CardSurfaceNoMappings = Color(0xFFC7D1D8)

    var PrimaryDarkerBlue = Color(0xFF11366B)
    var PrimaryBlue = Color(0xFF3076CC)
}


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // TODO remove
    val colorScheme = LightColorScheme
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    val test = "HELLO"
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
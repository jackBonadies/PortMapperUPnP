package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Blue40 = Color(0xff014C69)
val Blue80 = Color(0xff93C3F4)

private val DarkColorScheme = darkColorScheme(
    primary = Blue40,
    secondary = Blue40,
    tertiary = Pink80,

    onSurface = AdditionalColors.AdditionalColorsDark.TextColor
)

private val LightColorScheme = lightColorScheme(
    primary =  Blue80, //AAdditionalColors.PrimaryDarkerBlue,
    secondary = Blue80, //AdditionalColors.PrimaryBlue,
    tertiary = Pink40,
    // used in places where the background color isnt explicitly changed
    onSurface = AdditionalColors.AdditionalColorsLight.TextColor
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


    var CardContainerColor = AdditionalColorsLight.CardContainerColor
    var Background = AdditionalColorsLight.Background
    var TextColorStrong = AdditionalColorsLight.TextColorStrong
    var TextColor = AdditionalColorsLight.TextColor
    var TextColorWeak = AdditionalColorsLight.TextColorWeak
    var SubtleBorder = AdditionalColorsLight.SubtleBorder


    interface IAdditionalColors {
        val Background: Color
        val SubtleBorder: Color
        val CardContainerColor: Color
        val TextColorStrong: Color
        val TextColor: Color
        val TextColorWeak: Color
    }

    object AdditionalColorsLight : IAdditionalColors
    {
        override var Background = Color(0xffF0F0F2)
        override var SubtleBorder = Color(0xffE2E2E4)
        override var CardContainerColor = Color(0xffFCFCFE)


        override var TextColorStrong = Color(0xff1B1B1F)
        override var TextColor = Color(0xff303037)
        override var TextColorWeak = Color(0xff45464F)

    }

    object AdditionalColorsDark : IAdditionalColors
    {
        override var Background = Color(0xff181C1F)
        override var SubtleBorder = Color(0xff3A3E41)
        override var CardContainerColor = Color(0xff2D3134)

        override var TextColorStrong = Color(0xffffffff)
        override var TextColor = Color(0xffd8dad9)
        override var TextColorWeak = Color(0xffa3a8a6)

    }

}


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // TODO remove
    val useDark = true

    var darkLightAdditionalColors : AdditionalColors.IAdditionalColors = if (useDark) AdditionalColors.AdditionalColorsDark else AdditionalColors.AdditionalColorsLight
    AdditionalColors.Background = darkLightAdditionalColors.Background
    AdditionalColors.SubtleBorder = darkLightAdditionalColors.SubtleBorder
    AdditionalColors.CardContainerColor = darkLightAdditionalColors.CardContainerColor
    AdditionalColors.TextColorWeak = darkLightAdditionalColors.TextColorWeak
    AdditionalColors.TextColor = darkLightAdditionalColors.TextColor
    AdditionalColors.TextColorStrong = darkLightAdditionalColors.TextColorStrong

    val colorScheme = DarkColorScheme
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
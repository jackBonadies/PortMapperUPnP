package com.shinjiindustrial.portmapper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.ThemeUiState

fun getBlend(first: Color, second: Color, ratio: Double): Color {
    val newRed = first.red.toDouble() * (1f - ratio) + second.red.toDouble() * (ratio)
    val newGreen = first.green.toDouble() * (1f - ratio) + second.green.toDouble() * (ratio)
    val newBlue = first.blue.toDouble() * (1f - ratio) + second.blue.toDouble() * (ratio)
    return Color(newRed.toFloat(), newGreen.toFloat(), newBlue.toFloat(), 1f)
}


val Blue40 = Color(0xff014C69)
val Blue40_Lighter = Color(0xff0A7B9C)
val Blue80 = Color(0xff93C3F4)
val Blue80_Darker = Color(0xff509BEC)

private val DarkColorScheme = darkColorScheme(
    primary = Blue40_Lighter,
    secondary = Blue40,
    tertiary = Pink80,

    onSurface = AdditionalColors.AdditionalColorsDark.TextColor,
    onPrimary = AdditionalColors.AdditionalColorsDark.TextColorStrong,
    secondaryContainer = Blue40,
    background = Color(0xff181C1F),

    )

private val LightColorScheme = lightColorScheme(
    primary = Blue80_Darker, //AAdditionalColors.PrimaryDarkerBlue,
    secondary = Blue80, //AdditionalColors.PrimaryBlue,
    tertiary = Pink40,
    // used in places where the background color isnt explicitly changed
    onSurface = AdditionalColors.AdditionalColorsLight.TextColor,
    onPrimary = AdditionalColors.AdditionalColorsLight.TextColorStrong,
    secondaryContainer = Blue80,
    background = Color(0xffF0F0F2),
)


object AdditionalColors {
    var Enabled_Green = Color(0xff28B740)
    var Disabled_Red = Color(0xffB23131)
    var OrangePastel = Color(0xfff0d5c2)
    var CardSurface = Color(0xFFC2DDF0) //0xFFC2DDF0


    var CardSurfaceNoMappings = Color(0xFFC7D1D8)


    var PrimaryDarkerBlue = Color(0xFF11366B)
    var PrimaryBlue = Color(0xFF3076CC)

    //TODO: clean up, make part of constructor or such
    var CardContainerColor = AdditionalColorsLight.CardContainerColor
    var Background = AdditionalColorsLight.Background
    var TextColorStrong = AdditionalColorsLight.TextColorStrong
    var TextColor = AdditionalColorsLight.TextColor
    var TextColorWeak = AdditionalColorsLight.TextColorWeak
    var TopAppBarColor = AdditionalColorsLight.TextColorWeak
    var SubtleBorder = AdditionalColorsLight.SubtleBorder

    var LogErrorText = AdditionalColorsLight.LogErrorText
    var LogWarningText = AdditionalColorsLight.LogWarningText


    interface IAdditionalColors {
        val Background: Color
        val SubtleBorder: Color
        val CardContainerColor: Color
        val TextColorStrong: Color
        val TextColor: Color
        val TextColorWeak: Color
        val LogErrorText: Color
        val LogWarningText: Color
    }

    object AdditionalColorsLight : IAdditionalColors {
        override var Background = Color(0xffF0F0F2)
        override var SubtleBorder = Color(0xffE2E2E4)
        override var CardContainerColor = Color(0xffFCFCFE)


        override var TextColorStrong = Color(0xff1B1B1F)
        override var TextColor = Color(0xff303037)
        override var TextColorWeak = Color(0xff45464F)

        override var LogErrorText = Color(0xffCD0000)
        override var LogWarningText = Color(0xFFAD9727)

    }

    object AdditionalColorsDark : IAdditionalColors {
        override var Background = Color(0xff181C1F)
        override var SubtleBorder = Color(0xff3A3E41)
        override var CardContainerColor = Color(0xff2D3134)

        override var TextColorStrong = Color(0xffffffff)
        override var TextColor = Color(0xffd8dad9)
        override var TextColorWeak = Color(0xffa3a8a6)

        override var LogErrorText = Color(0xffCF5B56)
        override var LogWarningText = Color(0xffBBB529)

    }
}


@Composable
fun MyApplicationTheme(
    themeState: ThemeUiState,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {

    var dayNightMode = themeState.dayNightMode

    var useDark = darkTheme
    useDark = dayNightMode != DayNightMode.FORCE_DAY

    var useMaterialYou = themeState.materialYou
    key(useMaterialYou, dayNightMode)
    {
        val colorSchemeToUse = when {
            useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            useDark -> DarkColorScheme
            else -> LightColorScheme
        }

        var darkLightAdditionalColors: AdditionalColors.IAdditionalColors =
            if (useDark) AdditionalColors.AdditionalColorsDark else AdditionalColors.AdditionalColorsLight
        AdditionalColors.Background = darkLightAdditionalColors.Background
        AdditionalColors.SubtleBorder = darkLightAdditionalColors.SubtleBorder
        AdditionalColors.CardContainerColor = darkLightAdditionalColors.CardContainerColor
        AdditionalColors.TextColorWeak = darkLightAdditionalColors.TextColorWeak
        AdditionalColors.TextColor = darkLightAdditionalColors.TextColor
        AdditionalColors.TextColorStrong = darkLightAdditionalColors.TextColorStrong
        AdditionalColors.LogErrorText = darkLightAdditionalColors.LogErrorText
        AdditionalColors.LogWarningText = darkLightAdditionalColors.LogWarningText
        AdditionalColors.TopAppBarColor = colorSchemeToUse.secondary


        if (useMaterialYou) {
            AdditionalColors.TextColorStrong = colorSchemeToUse.onSurface
            AdditionalColors.TextColor = colorSchemeToUse.onSurfaceVariant
            AdditionalColors.TextColorWeak = colorSchemeToUse.onSurfaceVariant
            //AdditionalColors.Background = colorSchemeToUse.background //TODO: just set background for non material
            AdditionalColors.CardContainerColor = colorSchemeToUse.surfaceColorAtElevation(
                5.dp
            )
            AdditionalColors.TopAppBarColor = colorSchemeToUse.surfaceColorAtElevation(
                4.dp
            )

            AdditionalColors.SubtleBorder =
                getBlend(colorSchemeToUse.surface, colorSchemeToUse.onSurface, 0.2)
        }

        //val colorScheme = if(colorSchemeToUse) DarkColorScheme else LightColorScheme
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = AdditionalColors.TopAppBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    darkTheme
            }
        }

        // both clauses are the same. this is a hack to get
        // theme to recompose.
        //if (AdditionalColors.ThemeSetting.value == 0) {
        MaterialTheme(
            colorScheme = colorSchemeToUse,
            typography = Typography,
            content = content
        )
//        } else {
//            MaterialTheme(
//                colorScheme = colorSchemeToUse,
//                typography = Typography,
//                content = content
//            )
//        }
    }
}
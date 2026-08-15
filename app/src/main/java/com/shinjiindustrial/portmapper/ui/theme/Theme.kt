package com.shinjiindustrial.portmapper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.ThemeUiState

/*
 * These colors are generated
 */
private val PortMapperLightColors = lightColorScheme(
    primary = Color(0xFF005EB3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001B3C),
    inversePrimary = Color(0xFFA7C8FF),
    secondary = Color(0xFF555F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF6E5676),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF8D8FE),
    onTertiaryContainer = Color(0xFF27132F),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF005EB3),
    inverseSurface = Color(0xFF2E3035),
    inverseOnSurface = Color(0xFFF0F0F7),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceDim = Color(0xFFD9DAE0),
    surfaceContainer = Color(0xFFEDEDF4),
    surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val PortMapperDarkColors = darkColorScheme(
    primary = Color(0xFFA7C8FF),
    onPrimary = Color(0xFF003061),
    primaryContainer = Color(0xFF004789),
    onPrimaryContainer = Color(0xFFD5E3FF),
    inversePrimary = Color(0xFF005EB3),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    tertiary = Color(0xFFDBBCE1),
    onTertiary = Color(0xFF3E2845),
    tertiaryContainer = Color(0xFF563E5D),
    onTertiaryContainer = Color(0xFFF8D8FE),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF43474E),
    // Deliberate override, not a generated value: the palette's neutralVariant80
    // (#C4C6CF) reads too grey for body text. Keep this if the scheme is regenerated.
    onSurfaceVariant = Color(0xFFD8DAD9),
    surfaceTint = Color(0xFFA7C8FF),
    inverseSurface = Color(0xFFE1E2E9),
    inverseOnSurface = Color(0xFF2E3035),
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF37393E),
    surfaceDim = Color(0xFF111318),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerLowest = Color(0xFF0C0E13),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Immutable
data class SemanticColors(
    val enabled: Color,
    val disabled: Color,
    val logError: Color,
    val logWarning: Color,
)

private val LightSemanticColors = SemanticColors(
    enabled = Color(0xFF1E9B34),
    disabled = Color(0xFFB3261E),
    logError = Color(0xFFC00000),
    logWarning = Color(0xFF6F5B00),
)

private val DarkSemanticColors = SemanticColors(
    enabled = Color(0xFF60DD7F),
    disabled = Color(0xFFF2B8B5),
    logError = Color(0xFFFF897D),
    logWarning = Color(0xFFE0C74A),
)

/**
 * Default theme which breaks from M3 in a few chosen areas
 */
@Immutable
data class ComponentColors(
    val appBarContainer: Color,
    val appBarScrolledContainer: Color,
    val fabContainer: Color,
    val fabContent: Color,
    val cardContainer: Color,
)

private val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
private val LocalComponentColors = staticCompositionLocalOf {
    ComponentColors(
        appBarContainer = Color(0xFFE7EEFF),
        appBarScrolledContainer = Color(0xFFDFE8FE),
        fabContainer = PortMapperLightColors.primary,
        fabContent = PortMapperLightColors.onPrimary,
        cardContainer = PortMapperLightColors.surfaceContainerLow,
    )
}

object PortMapperTheme {
    val semanticColors: SemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current

    val componentColors: ComponentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalComponentColors.current
}

@Composable
fun MyApplicationTheme(
    themeState: ThemeUiState,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dayNightMode = themeState.dayNightMode
    val useDark =
        dayNightMode == DayNightMode.FORCE_NIGHT || (dayNightMode == DayNightMode.FOLLOW_SYSTEM && darkTheme)
    val useMaterialYou = themeState.materialYou

    val usingDynamicColor =
        useMaterialYou && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        usingDynamicColor -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDark -> PortMapperDarkColors
        else -> PortMapperLightColors
    }

    val cardContainer =
        if (useDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainerLow

    val componentColors = when {
        usingDynamicColor -> ComponentColors(
            appBarContainer = colorScheme.surfaceContainer,
            appBarScrolledContainer = colorScheme.surfaceContainerHigh,
            fabContainer = colorScheme.primaryContainer,
            fabContent = colorScheme.onPrimaryContainer,
            cardContainer = cardContainer,
        )

        useDark -> ComponentColors(
            appBarContainer = Color(0xFF004159),
            appBarScrolledContainer = Color(0xFF144C64),
            fabContainer = colorScheme.primary,
            fabContent = colorScheme.onPrimary,
            cardContainer = cardContainer,
        )

        else -> ComponentColors(
            appBarContainer = Color(0xFFE7EEFF),
            appBarScrolledContainer = Color(0xFFDFE8FE),
            fabContainer = colorScheme.primary,
            fabContent = colorScheme.onPrimary,
            cardContainer = cardContainer,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val statusBarColor = componentColors.appBarContainer.toArgb()
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor
            // controls the icon colors the system draws (i.e. time, wifi, battery)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    CompositionLocalProvider(
        LocalSemanticColors provides if (useDark) DarkSemanticColors else LightSemanticColors,
        LocalComponentColors provides componentColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

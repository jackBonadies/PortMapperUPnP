package com.shinjiindustrial.portmapper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.SharedPrefValues


@Composable
@Preview
fun SetupPreview() {
    SharedPrefValues.DayNightPref = DayNightMode.FORCE_NIGHT
    SharedPrefValues.MaterialYouTheme = false
}
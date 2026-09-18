package com.shinjiindustrial.portmapper

import androidx.annotation.StringRes
import com.example.myapplication.R

object SharedPrefKeys

object SharedPrefValues {
    var DayNightPref: DayNightMode = DayNightMode.FOLLOW_SYSTEM
    var MaterialYouTheme: Boolean = false
}

enum class DayNightMode(val intVal: Int, @StringRes val labelRes: Int) {
    FOLLOW_SYSTEM(0, R.string.theme_follow_system),
    FORCE_DAY(1, R.string.theme_light),
    FORCE_NIGHT(2, R.string.theme_dark);

    companion object {
        fun from(findValue: Int): DayNightMode =
            DayNightMode.values().first { it.intVal == findValue }
    }
}

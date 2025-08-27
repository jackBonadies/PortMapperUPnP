package com.shinjiindustrial.portmapper

import com.shinjiindustrial.portmapper.common.SortBy

object SharedPrefKeys
{
    val dayNightPref = "dayNightPref"
    val materialYouPref = "materialYouPref"
}

object SharedPrefValues
{
    var DayNightPref : DayNightMode = DayNightMode.FOLLOW_SYSTEM
    var MaterialYouTheme : Boolean = false
}

enum class DayNightMode(val intVal : Int) {
    FOLLOW_SYSTEM(0),
    FORCE_DAY(1),
    FORCE_NIGHT(2);

    companion object {
        fun from(findValue: Int): DayNightMode = DayNightMode.values().first { it.intVal == findValue }
    }
}

package com.shinjiindustrial.portmapper

object SharedPrefKeys
{
    val dayNightPref = "dayNightPref"
    val materialYouPref = "materialYouPref"
    val sortOrderPref = "sortOrderPref"
    val descAscPref = "descAscPref"
}

object SharedPrefValues
{
    var DayNightPref : DayNightMode = DayNightMode.FOLLOW_SYSTEM
    var MaterialYouTheme : Boolean = false
    var SortByPortMapping : SortBy = SortBy.Slot
    var Ascending : Boolean = true
}

enum class DayNightMode(val intVal : Int) {
    FOLLOW_SYSTEM(0),
    FORCE_DAY(1),
    FORCE_NIGHT(2);

    companion object {
        fun from(findValue: Int): DayNightMode = DayNightMode.values().first { it.intVal == findValue }
    }
}

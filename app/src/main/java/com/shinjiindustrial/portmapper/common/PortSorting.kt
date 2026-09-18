package com.shinjiindustrial.portmapper.common

import androidx.annotation.StringRes
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.ComparerWrapper
import com.shinjiindustrial.portmapper.PortMapperComparatorDescription
import com.shinjiindustrial.portmapper.PortMapperComparatorDevice
import com.shinjiindustrial.portmapper.PortMapperComparatorExpiration
import com.shinjiindustrial.portmapper.PortMapperComparatorExternalPort
import com.shinjiindustrial.portmapper.PortMapperComparatorInternalPort
import com.shinjiindustrial.portmapper.PortMapperComparerSlot
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref

// TODO organize re other portmappingsort file

data class SortInfo(val sortBy: SortBy, val sortDesc: Boolean)

// shortNameRes is the sort panel button label
enum class SortBy(val sortByValue: Int, @StringRes val shortNameRes: Int) {
    Slot(0, R.string.sort_slot),
    Description(1, R.string.description),
    InternalPort(2, R.string.sort_internal_port_short),
    ExternalPort(3, R.string.sort_external_port_short),
    Device(4, R.string.device),
    Expiration(5, R.string.sort_expiration);

    companion object {
        fun from(findValue: Int): SortBy = SortBy.values().first { it.sortByValue == findValue }
    }

    fun getName(): String {
        return if (this == InternalPort) {
            "Internal Port"
        } else if (this == ExternalPort) {
            "External Port"
        } else {
            name
        }
    }

    fun getComparer(ascending: Boolean): Comparator<PortMappingWithPref> {
        return ComparerWrapper(
            when (this) {
                Slot -> PortMapperComparerSlot(ascending)
                Description -> PortMapperComparatorDescription(ascending)
                InternalPort -> PortMapperComparatorInternalPort(ascending)
                ExternalPort -> PortMapperComparatorExternalPort(ascending)
                Device -> PortMapperComparatorDevice(ascending)
                Expiration -> PortMapperComparatorExpiration(ascending)
            }
        )
    }
}

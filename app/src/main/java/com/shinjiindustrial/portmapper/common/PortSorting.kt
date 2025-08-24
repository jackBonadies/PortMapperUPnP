package com.shinjiindustrial.portmapper.common

import com.shinjiindustrial.portmapper.ComparerWrapper
import com.shinjiindustrial.portmapper.PortMapperComparatorBase
import com.shinjiindustrial.portmapper.PortMapperComparatorDescription
import com.shinjiindustrial.portmapper.PortMapperComparatorDevice
import com.shinjiindustrial.portmapper.PortMapperComparatorExpiration
import com.shinjiindustrial.portmapper.PortMapperComparatorExternalPort
import com.shinjiindustrial.portmapper.PortMapperComparatorInternalPort
import com.shinjiindustrial.portmapper.PortMapperComparerSlot
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref

// TODO organize re other portmappingsort file

enum class SortBy(val sortByValue : Int) {
    Slot(0),
    Description(1),
    InternalPort(2),
    ExternalPort(3),
    Device(4),
    Expiration(5);

    companion object {
        fun from(findValue: Int): SortBy = SortBy.values().first { it.sortByValue == findValue }
    }

    fun getName() : String
    {
        return if(this == InternalPort) {
            "Internal Port"
        } else if(this == ExternalPort) {
            "External Port"
        } else {
            name
        }
    }

    fun getShortName() : String
    {
        return if(this == InternalPort) {
            "Int. Port"
        } else if(this == ExternalPort) {
            "Ext. Port"
        } else {
            name
        }
    }

    fun getComparer(ascending : Boolean):  Comparator<PortMappingWithPref> {
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

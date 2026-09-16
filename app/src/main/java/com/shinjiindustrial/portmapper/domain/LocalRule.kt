package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import kotlinx.parcelize.Parcelize

// same as port_mappings primary key.  the internal target is part of it so that two of our
//   rules for the same external port (i.e. minecraft on laptop A and on laptop B) are distinct
//   rows and distinct cards.
@Parcelize
data class LocalRuleKey(
    val udn: String,
    val externalPort: Int,
    val protocol: String,
    val internalIp: String,
    val internalPort: Int,
) : Parcelable

// the router holds one rule per slot, so two selected local rules at the same slot cannot both
//   be activated (the second would just overwrite the first).  keys only, no lookup needed.
fun Collection<LocalRuleKey>.hasSlotConflict(): Boolean {
    return groupingBy { Triple(it.udn, it.protocol, it.externalPort) }
        .eachCount()
        .any { it.value > 1 }
}

// what the router has at a local rule's external port / protocol.  a property of the slot, not
//   the row: the router holds one rule per slot, we may hold several.
enum class LocalRuleStatus {
    // nothing.  expired, router rebooted, or deleted out of band
    Missing,

    // a rule that matches none of ours (isRuleOurs false for every row at the slot) i.e. it was
    //   changed out of band, or something unrelated took the port.  shows under ON ROUTER as
    //   unmanaged.  the only status that gets a badge.
    Drifted,

    // another of our rules for the same port, which shows under ON ROUTER as ours.  this one is
    //   simply the inactive alternative; activating it replaces the sibling.
    SiblingActive,
}

// a rule we created on a router that the router no longer reports as ours
data class LocalRule(
    val entity: PortMappingEntity,
    val device: IIGDDevice,
    val status: LocalRuleStatus,
) {
    val key: LocalRuleKey
        get() = LocalRuleKey(
            entity.deviceSignature,
            entity.externalPort,
            entity.protocol,
            entity.internalIp,
            entity.internalPort
        )

    // lets the existing sort comparers and card layout run on a stored rule.  the lease and
    //   enabled fields are what we would ask for, not anything the router said.
    fun toPortMappingWithPref(): PortMappingWithPref {
        val portMapping = PortMapping(
            Description = entity.description,
            RemoteHost = "",
            InternalIP = entity.internalIp,
            ExternalPort = entity.externalPort,
            InternalPort = entity.internalPort,
            Protocol = entity.protocol,
            Enabled = entity.desiredEnabled,
            LeaseDuration = entity.desiredLeaseDuration,
            DeviceIP = device.getIpAddress(),
            TimeReadLeaseDurationMs = 0,
            Slot = 0
        )
        return PortMappingWithPref(portMapping, entity.getPrefs(0))
    }

    // remoteHost is "" to match PortMappingUserInput.toRequest, which is how it was first created
    fun toRequest(): PortMappingRequest {
        return PortMappingRequest(
            description = entity.description,
            internalIp = entity.internalIp,
            internalPort = entity.internalPort.toString(),
            externalIp = device.getIpAddress(),
            externalPort = entity.externalPort.toString(),
            protocol = entity.protocol,
            leaseDuration = entity.desiredLeaseDuration.toString(),
            enabled = entity.desiredEnabled,
            remoteHost = ""
        )
    }
}

// i.e. does it match what we wrote or did it change out of band
//   Enabled and LeaseDuration not compared
fun PortMappingEntity.matches(portMapping: PortMapping): Boolean {
    return this.description == portMapping.Description &&
            this.internalIp == portMapping.InternalIP &&
            this.internalPort == portMapping.InternalPort
}

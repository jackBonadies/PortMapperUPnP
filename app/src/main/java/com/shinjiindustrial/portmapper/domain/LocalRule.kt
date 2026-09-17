package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import kotlinx.parcelize.Parcelize

// the udn in this case is the target to activate under (and so what the rule is listed under in the UI)
//   NOT the udn that we originally created the rule with (important re "show local rules from all routers")
//   and so this key is still unique even in the "show local rules from all routers" case
@Parcelize
data class LocalRuleKey(
    val udn: String,
    val externalPort: Int,
    val protocol: String,
    val internalIp: String,
    val internalPort: Int,
) : Parcelable

// if 2 rules have the same (target device, protocol, external port) they cant both be activated
fun Collection<LocalRuleKey>.hasSlotConflict(): Boolean {
    return groupingBy { Triple(it.udn, it.protocol, it.externalPort) }
        .eachCount()
        .any { it.value > 1 }
}

// what the router has at a local rule's external port / protocol
enum class LocalRuleStatus {
    // there is no rule active on our external port, the normal case
    Missing,

    // a rule that doesn't belong to us is at the same external port
    Drifted,

    // one of our rules at the same external port is active
    SiblingActive,
}

// a rule we created on a router that the router no longer reports as ours.  device is the
//   router it is listed under / targets, NOT what it was originally created on
//   (i.e. "show local rules from all routers")
//   sourceDeviceName is the original device we created the rule on
data class LocalRule(
    val entity: PortMappingEntity,
    val device: IIGDDevice,
    val status: LocalRuleStatus,
    val sourceDeviceName: String? = null,
) {
    val key: LocalRuleKey
        get() = LocalRuleKey(
            device.udn,
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

package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import kotlinx.parcelize.Parcelize

// same as port_mappings primary key
@Parcelize
data class LocalRuleKey(val udn: String, val externalPort: Int, val protocol: String) : Parcelable

// a rule we created on a router that the router no longer reports as ours
data class LocalRule(
    val entity: PortMappingEntity,
    val device: IIGDDevice,
    // the router has a rule at this external port / protocol but it no longer matches what we
    //   stored (isRuleOurs false) i.e. it was changed out of band
    //   shows under ON ROUTER
    val drifted: Boolean,
) {
    val key: LocalRuleKey
        get() = LocalRuleKey(entity.deviceSignature, entity.externalPort, entity.protocol)

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

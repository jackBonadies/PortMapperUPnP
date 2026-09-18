package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ViewKey : Parcelable {
    // TODO include slot? what does it mean for 2 ports to be the same since we can edit them?
    @Parcelize
    data class PortViewKey(val externalPort: Int, val protocol: String, val deviceIp: String) :
        ViewKey()

    @Parcelize
    data class DeviceHeaderKey(val deviceIp: String) : ViewKey()

    @Parcelize
    data class DeviceEmptyKey(val deviceIp: String) : ViewKey()

    @Parcelize
    data class SectionHeaderKey(val udn: String, val section: String) : ViewKey()

    @Parcelize
    data class LocalRuleViewKey(
        val udn: String,
        val externalPort: Int,
        val protocol: String,
        val internalIp: String,
        val internalPort: Int,
    ) : ViewKey()
}

enum class RuleSection(val label: String) {
    OnRouter("ON ROUTER"),
    Local("LOCAL")
}

sealed class UpnpViewRow {
    abstract val key: ViewKey

    data class PortViewRow(val portMapping: PortMappingWithPref) : UpnpViewRow() {
        override val key = ViewKey.PortViewKey(
            portMapping.portMapping.ExternalPort,
            portMapping.portMapping.Protocol,
            portMapping.portMapping.DeviceIP
        )
    }

    data class DeviceHeaderViewRow(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceHeaderKey(device.getKey())
    }

    data class DeviceEmptyViewRow(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceEmptyKey(device.getKey())
    }

    data class SectionHeaderViewRow(val device: IIGDDevice, val section: RuleSection) :
        UpnpViewRow() {
        override val key = ViewKey.SectionHeaderKey(device.udn, section.name)
    }

    // from the rule's key, not the entity: the entity's UDN can be another router's (see
    //   LocalRuleKey) and the same row listed under two routers must be two LazyColumn keys
    data class LocalRuleViewRow(val localRule: LocalRule) : UpnpViewRow() {
        override val key = localRule.key.let {
            ViewKey.LocalRuleViewKey(
                it.udn,
                it.externalPort,
                it.protocol,
                it.internalIp,
                it.internalPort
            )
        }
    }
}

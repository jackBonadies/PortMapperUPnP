package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ViewKey : Parcelable {
    // TODO include slot? what does it mean for 2 ports to be the same since we can edit them?
    @Parcelize
    data class PortViewKey(val externalPort: Int, val protocol: String, val deviceIp : String) : ViewKey()
    @Parcelize
    data class DeviceHeaderKey(val deviceIp : String) : ViewKey()
    @Parcelize
    data class DeviceEmptyKey(val deviceIp : String) : ViewKey()
}

sealed class UpnpViewRow {
    abstract val key : ViewKey
    data class PortViewRow constructor(val portMapping: PortMappingWithPref) : UpnpViewRow() {
        override val key = ViewKey.PortViewKey(portMapping.portMapping.ExternalPort, portMapping.portMapping.Protocol, portMapping.portMapping.DeviceIP)
    }
    data class DeviceHeaderViewRow constructor(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceHeaderKey(device.getIpAddress())
    }
    data class DeviceEmptyViewRow constructor(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceEmptyKey(device.getIpAddress())
    }
}

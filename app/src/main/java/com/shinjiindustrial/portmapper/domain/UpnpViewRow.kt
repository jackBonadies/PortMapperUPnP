package com.shinjiindustrial.portmapper.domain

sealed class ViewKey {
    // TODO include slot? what does it mean for 2 ports to be the same since we can edit them?
    data class PortViewKey(val externalPort: Int, val protocol: String, val deviceIp : String) : ViewKey()
    data class DeviceHeaderKey(val deviceIp : String) : ViewKey()
    data class DeviceEmptyKey(val deviceIp : String) : ViewKey()
}

sealed class UpnpViewRow {
    abstract val key : ViewKey
    data class PortViewRow constructor(val portMapping: PortMapping) : UpnpViewRow() {
        override val key = ViewKey.PortViewKey(portMapping.ExternalPort, portMapping.Protocol, portMapping.ActualExternalIP)
    }
    data class DeviceHeaderViewRow constructor(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceHeaderKey(device.getIpAddress())
    }
    data class DeviceEmptyViewRow constructor(val device: IIGDDevice) : UpnpViewRow() {
        override val key = ViewKey.DeviceEmptyKey(device.getIpAddress())
    }
}

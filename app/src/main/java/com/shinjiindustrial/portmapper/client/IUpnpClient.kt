package com.shinjiindustrial.portmapper.client

import com.shinjiindustrial.portmapper.Event
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.UPnPCreateMappingResult
import com.shinjiindustrial.portmapper.UPnPCreateMappingResult2
import com.shinjiindustrial.portmapper.UPnPGetSpecificMappingResult
import com.shinjiindustrial.portmapper.UPnPResult
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import java.util.concurrent.Future

// Wrapper around cling provided upnp service
// allows us to better mock interactions
// so we need an interface for this
// this is our proxy for ALL cling network upnp calls
interface IUpnpClient {
    fun createPortMappingRule(
        device: IGDDevice,
        portMappingRequest: PortMappingRequest,
        callback: (UPnPCreateMappingResult2) -> Unit
    ): Future<Any>

    fun deletePortMapping(
        device: IGDDevice,
        portMapping: PortMapping,
        callback: (UPnPResult) -> Unit
    ): Future<Any>

    fun getSpecificPortMappingRule(
        device: IGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
        callback: (UPnPCreateMappingResult) -> Unit
    ) : Future<Any>

    fun getGenericPortMappingRule(device : IGDDevice,
        slotIndex : Int,
        callback: (UPnPGetSpecificMappingResult) -> Unit
    ) : Future<Any>

    fun search(maxSeconds: Int)

    fun clearOldDevices()

    fun isInitialized(): Boolean

    fun getInterfacesUsedInSearch(): MutableList<NetworkInterfaceInfo>

    val deviceFoundEvent: Event<IGDDevice>
}
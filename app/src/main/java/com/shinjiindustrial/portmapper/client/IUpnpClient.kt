package com.shinjiindustrial.portmapper.client

import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import org.fourthline.cling.model.message.UpnpResponse
import java.util.concurrent.Future

// Wrapper around cling provided upnp service
// allows us to better mock interactions
// so we need an interface for this
// this is our proxy for ALL cling network upnp calls
interface IUpnpClient {

    suspend fun createPortMappingRule(
        device: IGDDevice,
        portMappingRequest: PortMappingRequest,
    ) : UPnPCreateMappingResult

    suspend fun deletePortMapping(
        device: IGDDevice,
        portMapping: PortMapping,
    ): UPnPResult

    suspend fun getSpecificPortMappingRule(
        device: IGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
    ) : UPnPCreateMappingWrapperResult

    suspend fun getGenericPortMappingRule(device : IGDDevice,
        slotIndex : Int,
    ) : UPnPGetSpecificMappingResult

    fun search(maxSeconds: Int)

    fun clearOldDevices()

    fun isInitialized(): Boolean

    fun getInterfacesUsedInSearch(): MutableList<NetworkInterfaceInfo>

    val deviceFoundEvent: Event<IGDDevice>
}

sealed class UPnPCreateMappingWrapperResult
{
    data class Success(val requestInfo: PortMapping, val resultingMapping: PortMapping, val wasReadBack: Boolean) : UPnPCreateMappingWrapperResult()
    data class Failure(val reason: String, val response: UpnpResponse) : UPnPCreateMappingWrapperResult()
}

sealed class UPnPCreateMappingResult
{
    data class Success(val requestInfo: PortMapping) : UPnPCreateMappingResult()
    data class Failure(val reason: String, val response: UpnpResponse) : UPnPCreateMappingResult()
}

sealed class UPnPGetSpecificMappingResult
{
    data class Success(val requestInfo: PortMapping, val resultingMapping: PortMapping) : UPnPGetSpecificMappingResult()
    data class Failure(val reason: String, val response: UpnpResponse) : UPnPGetSpecificMappingResult()
}

sealed class UPnPResult
{
    data class Success(val requestInfo: PortMapping) : UPnPResult()
    data class Failure(val reason: String, val response: UpnpResponse) : UPnPResult()
}
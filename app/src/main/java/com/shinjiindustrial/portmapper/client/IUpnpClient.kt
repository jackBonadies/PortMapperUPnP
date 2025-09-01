package com.shinjiindustrial.portmapper.client

import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.ClingIGDDevice
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import org.fourthline.cling.model.message.UpnpResponse

// Wrapper around cling provided upnp service
// allows us to better mock interactions
// so we need an interface for this
// this is our proxy for ALL cling network upnp calls
interface IUpnpClient {

    suspend fun createPortMappingRule(
        device: IIGDDevice,
        portMappingRequest: PortMappingRequest,
    ): UPnPCreateMappingResult

    suspend fun deletePortMapping(
        device: IIGDDevice,
        portMapping: PortMapping,
    ): UPnPResult

    suspend fun getSpecificPortMappingRule(
        device: IIGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
    ): UPnPGetSpecificMappingResult

    suspend fun getGenericPortMappingRule(
        device: IIGDDevice,
        slotIndex: Int,
    ): UPnPGetGenericMappingResult

    fun search(maxSeconds: Int)

    fun clearOldDevices()

    fun isInitialized(): Boolean

    fun getInterfacesUsedInSearch(): MutableList<NetworkInterfaceInfo>

    fun instantiateAndBindUpnpService()

    val deviceFoundEvent: Event<ClingIGDDevice>
}

sealed class UPnPCreateMappingWrapperResult {
    data class Success(
        val requestInfo: PortMapping,
        val resultingMapping: PortMapping,
        val wasReadBack: Boolean
    ) : UPnPCreateMappingWrapperResult()

    data class Failure(val details: FailureDetails) :
        UPnPCreateMappingWrapperResult()
}

sealed class UPnPCreateMappingResult {
    data class Success(val requestInfo: PortMapping) : UPnPCreateMappingResult()
    data class Failure(val details: FailureDetails) : UPnPCreateMappingResult()
}

sealed class UPnPGetGenericMappingResult {
    data class Success(val requestInfo: PortMapping, val resultingMapping: PortMapping) :
        UPnPGetGenericMappingResult()

    data class Failure(val details: FailureDetails) :
        UPnPGetGenericMappingResult()
}

sealed class UPnPGetSpecificMappingResult {
    data class Success(val requestInfo: PortMapping, val resultingMapping: PortMapping) :
        UPnPGetSpecificMappingResult()

    data class Failure(val details: FailureDetails) :
        UPnPGetSpecificMappingResult()

    companion object
}

sealed class UPnPResult {
    data class Success(val requestInfo: PortMapping) : UPnPResult()
    data class Failure(val details: FailureDetails) : UPnPResult()
}

class FailureDetails(val reason: String, val response: UpnpResponse?)
{
    override fun toString() : String{
        if (response == null)
        {
            return "\t${reason}. No response from router."
        }
        return "\t${reason}\t${response}"
    }
}
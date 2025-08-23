package com.shinjiindustrial.portmapper.client

import android.content.Context
import com.shinjiindustrial.portmapper.GetPsuedoSlot
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.ACTION_NAMES
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.formatShortName
import com.shinjiindustrial.portmapper.domain.getIGDDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

class UpnpClient @Inject constructor(@ApplicationContext private val context: Context) : IUpnpClient {

    sealed class ClingExecutionResult {
        data class Success(val invocation: ActionInvocation<*>) : ClingExecutionResult()
        data class Failure(val invocation: ActionInvocation<*>, val operation : UpnpResponse, val defaultMsg : String) :
            ClingExecutionResult()
    }

    private lateinit var upnpService : UpnpService

    override fun instantiateAndBindUpnpService()
    {
        upnpService = UpnpServiceImpl(AndroidUpnpServiceConfigurationImpl(context))
        upnpService.registry?.addListener(object : RegistryListener {
            // ssdp datagrams have been alive and processed
            // services are unhydrated, service descriptors not yet retrieved
            override fun remoteDeviceDiscoveryStarted(
                registry: Registry,
                device: RemoteDevice
            ) {
                println("Discovery started: " + device.displayString)
            }

            override fun remoteDeviceDiscoveryFailed(
                registry: Registry,
                device: RemoteDevice,
                ex: Exception
            ) {
                println("Discovery failed: " + device.displayString + " => " + ex)
            }

            // complete metadata
            override fun remoteDeviceAdded(registry: Registry, rootDevice: RemoteDevice) {
                val igdDevice = rootDevice.getIGDDevice()
                if (igdDevice != null) {
                    deviceFoundEvent.invoke(igdDevice)
                }
            }


            // expiration timestamp updated
            override fun remoteDeviceUpdated(registry: Registry, device: RemoteDevice) {

                println("Device updated: " + device.displayString)
            }

            override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
                println("Device removed: " + device.displayString)
            }

            override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
                println("Added local device: " + device.displayString)
            }

            override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
                println("Removed local device: " + device.displayString)
            }

            override fun beforeShutdown(registry: Registry) {}

            override fun afterShutdown() {}
        })
    }

    init {
    }

    private suspend fun executeAction(actionInvocation: ActionInvocation<*>): ClingExecutionResult =
        suspendCancellableCoroutine { cont ->
            // TODO test throwing exception behavior
            val future = upnpService.controlPoint.execute(object :
                ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    val result = ClingExecutionResult.Success(invocation!!)
                    cont.resume(result)
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    val result = ClingExecutionResult.Failure(invocation!!, operation, defaultMsg)
                    cont.resume(result)
                }
            })
            cont.invokeOnCancellation { future.cancel(true) }
        }

    override suspend fun createPortMappingRule(
        device: IIGDDevice,
        portMappingRequest: PortMappingRequest,
    ) : UPnPCreateMappingResult {
                val externalIp = portMappingRequest.externalIp
                val actionInvocation = device.getActionInvocation(ACTION_NAMES.AddPortMapping)
                actionInvocation.setInput("NewRemoteHost", portMappingRequest.remoteHost)
                // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
                actionInvocation.setInput("NewExternalPort", portMappingRequest.externalPort)
                actionInvocation.setInput("NewProtocol", portMappingRequest.protocol)
                actionInvocation.setInput("NewInternalPort", portMappingRequest.internalPort)
                actionInvocation.setInput("NewInternalClient", portMappingRequest.internalIp)
                actionInvocation.setInput("NewEnabled", if (portMappingRequest.enabled) "1" else "0")
                actionInvocation.setInput("NewPortMappingDescription", portMappingRequest.description)
                actionInvocation.setInput("NewLeaseDuration", portMappingRequest.leaseDuration)
            val result = this.executeAction(actionInvocation)
            when(result) {
                is ClingExecutionResult.Success -> {
                    val result = UPnPCreateMappingResult.Success(portMappingRequest.realize())
                    return result
        }
        is ClingExecutionResult.Failure -> {
            // Handle failure
            println(result.defaultMsg)

            println(result.operation.statusMessage)
            println(result.operation.responseDetails)
            println(result.operation.statusCode)
            println(result.operation.isFailed)

            PortForwardApplication.Companion.OurLogger.log(
                Level.SEVERE,
                "Failed to create rule (${portMappingRequest.realize().shortName()})."
            )
            PortForwardApplication.Companion.OurLogger.log(Level.SEVERE, "\t$result.defaultMsg")

            return UPnPCreateMappingResult.Failure(result.defaultMsg, result.operation)
        }
            }
}

    override suspend fun deletePortMapping(
        device: IIGDDevice,
        portMapping: PortMapping,
    ): UPnPResult {
        val actionInvocation = device.getActionInvocation(ACTION_NAMES.DeletePortMapping)
        actionInvocation.setInput("NewRemoteHost", portMapping.getRemoteHostNormalizedForDelete())
        // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
        actionInvocation.setInput("NewExternalPort", "${portMapping.ExternalPort}")
        actionInvocation.setInput("NewProtocol", portMapping.Protocol)

        val result = this.executeAction(actionInvocation)
        when (result) {
            is ClingExecutionResult.Success -> {
                println("Successfully deleted")

                PortForwardApplication.Companion.OurLogger.log(
                    Level.INFO,
                    "Successfully deleted rule (${portMapping.shortName()})."
                )

                return UPnPResult.Success(portMapping)
            }
            is ClingExecutionResult.Failure -> {
                // Handle failure
                println(result.defaultMsg)

                println(result.operation.statusMessage)
                println(result.operation.responseDetails)
                println(result.operation.statusCode)
                println(result.operation.isFailed)

                PortForwardApplication.Companion.OurLogger.log(
                    Level.SEVERE,
                    "Failed to delete rule (${portMapping.shortName()})."
                )
                PortForwardApplication.Companion.OurLogger.log(Level.SEVERE, "\t$result.defaultMsg")

                return UPnPResult.Failure(result.defaultMsg, result.operation)
            }
        }
    }

    override suspend fun getSpecificPortMappingRule(
        device: IIGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
    ): UPnPCreateMappingWrapperResult {

        val remoteIp = device.getIpAddress()
        val actionInvocation = device.getActionInvocation(ACTION_NAMES.GetSpecificPortMappingEntry)
        // this is actually remote host
        actionInvocation.setInput("NewRemoteHost", remoteHost)
        actionInvocation.setInput("NewExternalPort", remotePort)
        actionInvocation.setInput("NewProtocol", protocol)

        val result = this.executeAction(actionInvocation)
        when (result) {
            is ClingExecutionResult.Success -> {
                val internalPort = actionInvocation.getOutput("NewInternalPort")
                val internalClient = actionInvocation.getOutput("NewInternalClient")
                val enabled = actionInvocation.getOutput("NewEnabled")
                val description = actionInvocation.getOutput("NewPortMappingDescription")
                val leaseDuration = actionInvocation.getOutput("NewLeaseDuration")

                val pm = PortMapping(
                    description.toString(),
                    remoteHost,
                    internalClient.toString(),
                    remotePort.toInt(),
                    internalPort.toString().toInt(),
                    protocol,
                    enabled.toString().toInt() == 1,
                    leaseDuration.toString().toInt(),
                    remoteIp,
                    System.currentTimeMillis(),
                    GetPsuedoSlot()
                ) // best bet for DateTime.UtcNow

                PortForwardApplication.Companion.OurLogger.log(
                    Level.INFO,
                    "Successfully read back our new rule (${pm.shortName()})"
                )

                val result = UPnPCreateMappingWrapperResult.Success(pm, pm, true)
                return result
            }

            is ClingExecutionResult.Failure -> {
                println(result.defaultMsg)

                println(result.operation.statusMessage)
                println(result.operation.responseDetails)
                println(result.operation.statusCode)
                println(result.operation.isFailed)

                val rule = formatShortName(protocol, remoteIp, remotePort)
                PortForwardApplication.Companion.OurLogger.log(
                    Level.SEVERE,
                    "Failed to read back our new rule ($rule)."
                )
                PortForwardApplication.Companion.OurLogger.log(Level.SEVERE, "\t$result.defaultMsg")

                val result = UPnPCreateMappingWrapperResult.Failure(result.defaultMsg, result.operation)
                return result
            }
        }
    }

    override suspend fun getGenericPortMappingRule(device : IIGDDevice,
                                                   slotIndex : Int) : UPnPGetSpecificMappingResult
    {
        val ipAddress = device.getIpAddress()
        val actionInvocation = device.getActionInvocation(ACTION_NAMES.GetGenericPortMappingEntry)
        println("requesting slot $slotIndex")
        actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
        val result = this.executeAction(actionInvocation)
        when (result) {
            is ClingExecutionResult.Success -> {
                val invocation = result.invocation
                PortForwardApplication.Companion.OurLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")

                val remoteHost = invocation.getOutput("NewRemoteHost") //string datatype // the .value is null (also empty if GetListOfPortMappings is used)
                val externalPort = invocation.getOutput("NewExternalPort") //unsigned 2 byte int
                val internalClient = invocation.getOutput("NewInternalClient") //string datatype
                val internalPort = invocation.getOutput("NewInternalPort")
                val protocol = invocation.getOutput("NewProtocol")
                val description = invocation.getOutput("NewPortMappingDescription")
                val enabled = invocation.getOutput("NewEnabled")
                val leaseDuration = invocation.getOutput("NewLeaseDuration")
                val portMapping = PortMapping(
                    description.toString(),
                    remoteHost.toString(),
                    internalClient.toString(),
                    externalPort.toString().toInt(),
                    internalPort.toString().toInt(),
                    protocol.toString(),
                    enabled.toString().toInt() == 1,
                    leaseDuration.toString().toInt(),
                    ipAddress,
                    System.currentTimeMillis(),
                    slotIndex
                )
                val result = UPnPGetSpecificMappingResult.Success(portMapping, portMapping)
                return result
            }
            is ClingExecutionResult.Failure -> {
                val result = UPnPGetSpecificMappingResult.Failure(result.defaultMsg, result.operation)
                return result
            }
        }
    }

    override fun search(maxSeconds : Int)
    {
        return upnpService.controlPoint.search(maxSeconds)
    }

    override fun clearOldDevices()
    {
        upnpService.registry?.removeAllLocalDevices()
        upnpService.registry?.removeAllRemoteDevices() // otherwise Add wont be called again (just Update)
    }

    override fun isInitialized() : Boolean
    {
        return upnpService.router.isEnabled
    }

    override fun getInterfacesUsedInSearch() : MutableList<NetworkInterfaceInfo>
    {
        return (upnpService.configuration as AndroidUpnpServiceConfigurationImpl).NetworkInterfacesUsedInfos
    }

    override val deviceFoundEvent = Event<IIGDDevice>()
//    private val _deviceFoundEvent = MutableSharedFlow<DeviceFoundEvent>(
//        replay = 0, extraBufferCapacity = 1
//    )
//    val deviceFoundEvent: SharedFlow<DeviceFoundEvent> = _deviceFoundEvent

//data class DeviceFoundEvent(val remoteDevice: RemoteDevice)
}
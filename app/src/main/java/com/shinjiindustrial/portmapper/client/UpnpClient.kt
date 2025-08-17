package com.shinjiindustrial.portmapper.client

import com.shinjiindustrial.portmapper.GetPsuedoSlot
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.ACTION_NAMES
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.formatShortName
import com.shinjiindustrial.portmapper.domain.getIGDDevice
import org.fourthline.cling.UpnpService
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import java.util.concurrent.Future
import java.util.logging.Level

class UpnpClient(private val upnpService: UpnpService) : IUpnpClient {
    init {
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

    private fun executeAction(callback: ActionCallback): Future<Any> {
        return upnpService.controlPoint.execute(callback)
    }

    override fun createPortMappingRule(
        device: IGDDevice,
        portMappingRequest: PortMappingRequest,
        callback: (UPnPCreateMappingResult) -> Unit
    ): Future<Any> {
        val externalIp = portMappingRequest.externalIp
        val action = device.actionsMap[ACTION_NAMES.AddPortMapping]
        val actionInvocation = ActionInvocation(action)
        actionInvocation.setInput("NewRemoteHost", portMappingRequest.remoteHost)
        // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
        actionInvocation.setInput("NewExternalPort", portMappingRequest.externalPort)
        actionInvocation.setInput("NewProtocol", portMappingRequest.protocol)
        actionInvocation.setInput("NewInternalPort", portMappingRequest.internalPort)
        actionInvocation.setInput("NewInternalClient", portMappingRequest.internalIp)
        actionInvocation.setInput("NewEnabled", if (portMappingRequest.enabled) "1" else "0")
        actionInvocation.setInput("NewPortMappingDescription", portMappingRequest.description)
        actionInvocation.setInput("NewLeaseDuration", portMappingRequest.leaseDuration)

        val future = this.executeAction(object :
            ActionCallback(actionInvocation) {
            override fun success(invocation: ActionInvocation<*>?) {
                invocation!!
                val result = UPnPCreateMappingResult.Success(portMappingRequest.realize())
                callback(result)
            }

            override fun failure(
                invocation: ActionInvocation<*>?,
                operation: UpnpResponse,
                defaultMsg: String
            ) {
                // Handle failure
                println(defaultMsg)

                println(operation.statusMessage)
                println(operation.responseDetails)
                println(operation.statusCode)
                println(operation.isFailed)

                OurLogger.log(
                    Level.SEVERE,
                    "Failed to create rule (${portMappingRequest.realize().shortName()})."
                )
                OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                val result = UPnPCreateMappingResult.Failure(defaultMsg, operation)
                callback(result)
            }
        })

        return future
    }

    override fun deletePortMapping(
        device: IGDDevice,
        portMapping: PortMapping,
        callback: (UPnPResult) -> Unit
    ): Future<Any> {
        val action = device.actionsMap[ACTION_NAMES.DeletePortMapping]
        val actionInvocation = ActionInvocation(action)
        actionInvocation.setInput("NewRemoteHost", portMapping.getRemoteHostNormalizedForDelete())
        // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
        actionInvocation.setInput("NewExternalPort", "${portMapping.ExternalPort}")
        actionInvocation.setInput("NewProtocol", portMapping.Protocol)

        val future = this.executeAction(object :
            ActionCallback(actionInvocation) {
            override fun success(invocation: ActionInvocation<*>?) {
                invocation!!
                println("Successfully deleted")

                OurLogger.log(
                    Level.INFO,
                    "Successfully deleted rule (${portMapping.shortName()})."
                )

                val result = UPnPResult.Success(portMapping)
                callback(result)

            }

            override fun failure(
                invocation: ActionInvocation<*>?,
                operation: UpnpResponse,
                defaultMsg: String
            ) {
                // Handle failure
                println(defaultMsg)

                println(operation.statusMessage)
                println(operation.responseDetails)
                println(operation.statusCode)
                println(operation.isFailed)

                OurLogger.log(
                    Level.SEVERE,
                    "Failed to delete rule (${portMapping.shortName()})."
                )
                OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                val result = UPnPResult.Failure(defaultMsg, operation)
                callback(result)
            }
        })

        return future

    }

    override fun getSpecificPortMappingRule(
        device: IGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
        callback: (UPnPCreateMappingWrapperResult) -> Unit
    ): Future<Any> {

        val remoteIp = device.ipAddress
        val action = device.actionsMap[ACTION_NAMES.GetSpecificPortMappingEntry]
        val actionInvocation = ActionInvocation(action)
        // this is actually remote host
        actionInvocation.setInput("NewRemoteHost", remoteHost)
        actionInvocation.setInput("NewExternalPort", remotePort)
        actionInvocation.setInput("NewProtocol", protocol)

        val future = this.executeAction(object :
            ActionCallback(actionInvocation) {
            override fun success(invocation: ActionInvocation<*>?) {
                invocation!!

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

                OurLogger.log(
                    Level.INFO,
                    "Successfully read back our new rule (${pm.shortName()})"
                )

                val result = UPnPCreateMappingWrapperResult.Success(pm, pm, true)
                callback(result)
            }

            override fun failure(
                invocation: ActionInvocation<*>?,
                operation: UpnpResponse,
                defaultMsg: String
            ) {
                println(defaultMsg)

                println(operation.statusMessage)
                println(operation.responseDetails)
                println(operation.statusCode)
                println(operation.isFailed)

                val rule = formatShortName(protocol, remoteIp, remotePort)
                OurLogger.log(
                    Level.SEVERE,
                    "Failed to read back our new rule ($rule)."
                )
                OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                val result = UPnPCreateMappingWrapperResult.Failure(defaultMsg, operation)
                callback(result)
            }
        })


        return future
    }


    override fun getGenericPortMappingRule(device : IGDDevice,
                                           slotIndex : Int,
                                           callback: (UPnPGetSpecificMappingResult) -> Unit) : Future<Any>
    {
        val ipAddress = device.ipAddress
        val action = device.actionsMap[ACTION_NAMES.GetGenericPortMappingEntry]
        println("requesting slot $slotIndex")
        val actionInvocation = ActionInvocation(action)
        actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
        val future = this.executeAction(object : ActionCallback(actionInvocation) {
            override fun success(invocation: ActionInvocation<*>?) {
                invocation!!
                OurLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")

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
                    slotIndex)
                val result = UPnPGetSpecificMappingResult.Success(portMapping, portMapping)
                callback(result)
            }

            override fun failure(
                invocation: ActionInvocation<*>?,
                operation: UpnpResponse,
                defaultMsg: String
            ) {
                val result = UPnPGetSpecificMappingResult.Failure(defaultMsg, operation)
                callback(result)
            }
        })
        return future
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

    override val deviceFoundEvent = Event<IGDDevice>()
//    private val _deviceFoundEvent = MutableSharedFlow<DeviceFoundEvent>(
//        replay = 0, extraBufferCapacity = 1
//    )
//    val deviceFoundEvent: SharedFlow<DeviceFoundEvent> = _deviceFoundEvent

//data class DeviceFoundEvent(val remoteDevice: RemoteDevice)
}
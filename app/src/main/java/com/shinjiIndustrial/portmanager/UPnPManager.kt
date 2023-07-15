package com.shinjiIndustrial.portmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.shinjiIndustrial.portmanager.PortForwardApplication.Companion.OurLogger
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.android.AndroidNetworkAddressFactory
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import org.fourthline.cling.transport.spi.NetworkAddressFactory
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.logging.Level
import kotlin.system.measureTimeMillis


class UpnpManager {


    companion object { //singleton
        private var _upnpService : UpnpService? = null
        private var _initialized : Boolean = false
        var HasSearched : Boolean = false
        var AnyIgdDevices : MutableState<Boolean>? = null

        object UPnPNames {
            val InternetGatewayDevice = "InternetGatewayDevice"
            val WANDevice = "WANDevice"
            val WANConnectionDevice = "WANConnectionDevice"
            val WANIPConnection = "WANIPConnection"
//            val IGD_DEVICE_TYPE: DeviceType = UDADeviceType(InternetGatewayDevice, 1)
//            val IGD_DEVICE_TYPE_2: DeviceType = UDADeviceType(InternetGatewayDevice, 2)
//
//            val CONNECTION_DEVICE_TYPE: DeviceType = UDADeviceType(WANConnectionDevice, 1)
//            val CONNECTION_DEVICE_TYPE_2: DeviceType = UDADeviceType(WANConnectionDevice, 2)
        }

        object ACTION_NAMES
        {
            // these actions are all Required in v1, v2
            val AddPortMapping : String = "AddPortMapping"
            val GetExternalIPAddress : String = "GetExternalIPAddress"
            val DeletePortMapping : String = "DeletePortMapping"
            val GetStatusInfo : String = "GetStatusInfo"
            val GetGenericPortMappingEntry : String = "GetGenericPortMappingEntry"
            val GetSpecificPortMappingEntry : String = "GetSpecificPortMappingEntry"

            // this action is required in v2, not present in v1
            var AddAnyPortMapping : String = "AddAnyPortMapping"

            // this action is required for device, optional for control point in v2, not present in v1
            var GetListOfPortMappings : String = "GetListOfPortMappings"
        }

        val ActionNames : List<String> = listOf(
            ACTION_NAMES.AddPortMapping,
            ACTION_NAMES.GetExternalIPAddress,
            ACTION_NAMES.DeletePortMapping,
            ACTION_NAMES.GetStatusInfo,
            ACTION_NAMES.GetGenericPortMappingEntry,
            ACTION_NAMES.GetSpecificPortMappingEntry,

            ACTION_NAMES.AddAnyPortMapping,
            ACTION_NAMES.GetListOfPortMappings,
        )

        var DeviceFoundEvent = Event<IGDDevice>()
        var PortFoundEvent = Event<PortMapping>()
        // used if we finish and there are no ports to show the "no devices" card
        var FinishedListingPortsEvent = Event<IGDDevice>()
        var UpdateUIFromData = Event<Any?>()
        var SearchStarted = Event<Any?>()
        //var NetworkInfoAtTimeOfSearch : OurNetworkInfoBundle? = null

        fun Search(onlyIfNotYetSearched : Boolean) : Boolean
        {
            if(onlyIfNotYetSearched && HasSearched)
            {
                return false
            }
            SearchStarted?.invoke(null)
            ClearOldData()
            //NetworkInfoAtTimeOfSearch = OurNetworkInfo.GetNetworkInfo(PortForwardApplication.appContext, true)
            HasSearched = true
            // can do urn:schemas-upnp-org:device:{deviceType}:{ver}
            // 0-1 second response time intentional delay from devices
            UpnpManager.GetUPnPService()?.controlPoint?.search(1)
            //launchMockUPnPSearch(this, upnpElementsViewModel)
            return true
        }

        fun ClearOldData(){
            UpnpManager.GetUPnPService()?.registry?.removeAllLocalDevices()
            UpnpManager.GetUPnPService()?.registry?.removeAllRemoteDevices() // otherwise Add wont be called again (just Update)
            AnyIgdDevices?.value = false
            synchronized(lockIgdDevices)
            {
                IGDDevices.clear()
            }
            UpdateUIFromData.invoke(null)
        }

        fun GetUPnPService() : UpnpService {
            return _upnpService!!
        }

        fun splitUserInputIntoRules(portMappingUserInput : PortMappingUserInput) : MutableList<PortMappingRequest>
        {
            var portMappingRequests : MutableList<PortMappingRequest> = mutableListOf()
            val (inStart, inEnd) = getRange(portMappingUserInput.internalPort)
            val (outStart, outEnd) = getRange(portMappingUserInput.internalPort)
            val protocols = getProtocols(portMappingUserInput.protocol)

            // many 1 to 1 makes sense
            // many external to 1 internal?? this makes sense but goes against retrieving ports with upnp
            //   in the sense that you are supposed to be able to retrieve 1 single port with 1
            //   (external ip, external port, protocol).
            // not sure about others...
            val inSize = inEnd - inStart + 1 // inclusive
            val outSize = outEnd - outStart + 1
            var xToOne = (inSize == 1 || outSize == 1) //many to one or one to one
            if (inSize != outSize && !xToOne)
            {
                throw java.lang.Exception("Internal and External Ranges do not match")
            }

            var sizeOfRange = maxOf(inSize, outSize) - 1 // if just 1 element then size is 0
            for (i in 0..sizeOfRange)
            {
                var inPort = if(inSize == 1) inStart else inStart + i
                var outPort = if(inSize == 1) outStart else outStart + i
                for (protocol in protocols)
                {
                    portMappingRequests.add(portMappingUserInput.with(inPort.toString(), outPort.toString(), protocol.toString()))
                }
            }
            return portMappingRequests
        }

        fun getRange(portRange : String) : Pair<Int, Int>
        {
            if(portRange.contains('-'))
            {
                var inRange = portRange.split('-')
                return Pair(inRange[0].toInt(), inRange[1].toInt())
            }
            else
            {
                return Pair(portRange.toInt(), portRange.toInt())
            }
        }

        fun getProtocols(protocol : String) : List<String>
        {
            return when (protocol) {
                Protocol.BOTH.str() -> listOf("TCP", "UDP")
                else -> listOf(protocol)
            }
        }

        data class PortMappingFullResult(val description : String, val internalIp : String, val internalPort : String)

        fun CreatePortMappingRules(
            portMappingUserInput: PortMappingUserInput,
            onCompleteBatchCallback: (MutableList<UPnPCreateMappingResult?>) -> Unit
        ) : FutureTask<MutableList<PortMappingRequest>>
        {
            val callable = Callable {

                var portMappingRequestRules = splitUserInputIntoRules(portMappingUserInput)
                var listOfResults: MutableList<UPnPCreateMappingResult?> =
                    MutableList(portMappingRequestRules.size) { null }

                for (i in 0 until portMappingRequestRules.size) {

                    fun callback(result: UPnPCreateMappingResult) {

                        defaultRuleAddedCallback(result)
                        listOfResults[i] = result
                    }

                    var future = CreatePortMappingRule(portMappingRequestRules[i], true, ::callback)
                    future.get()
                }

                onCompleteBatchCallback(listOfResults)

                portMappingRequestRules
            }

            val task = FutureTask(callable)
            Thread(task).start()
            return task // a FutureTask is a Future
        }

        fun DisableEnablePortMappingEntries(
            portMappings: List<PortMapping>,
            enable : Boolean,
            onCompleteBatchCallback: (MutableList<UPnPCreateMappingResult?>) -> Unit
        ) : FutureTask<List<PortMapping>>
        {
            val callable = Callable {

                var listOfResults: MutableList<UPnPCreateMappingResult?> =
                    MutableList(portMappings.size) { null }

                for (i in 0 until portMappings.size) {

                    fun callback(result: UPnPCreateMappingResult) {

                        enableDisableDefaultCallback(result)
                        listOfResults[i] = result
                    }

                    var future = DisableEnablePortMappingEntry(portMappings[i], enable, ::callback)
                    future.get()
                }

                onCompleteBatchCallback(listOfResults)
                portMappings
            }

            val task = FutureTask(callable)
            Thread(task).start()
            return task // a FutureTask is a Future
        }

        var FailedToInitialize : Boolean = false

        fun Initialize(context : Context, force : Boolean) : Boolean
        {
            if(_initialized && !force)
            {
                return true
            }

//            class AndroidNetworkFactory2(streamListenPort : Int) : AndroidNetworkAddressFactory(streamListenPort)
//            {
//                @Throws(InitializationException::class)
//                override fun discoverNetworkInterfaces() {
//                    try {
//
////                        val prop: LinkProperties = manager.getLinkProperties(network)
////                        val iface = NetworkInterface.getByName(prop.interfaceName)
//
//                        super.discoverNetworkInterfaces()
//                    } catch (ex: java.lang.Exception) {
//                        // TODO: ICS bug on some models with network interface disappearing while enumerated
//                        // http://code.google.com/p/android/issues/detail?id=33661
//                        //log.warning("Exception while enumerating network interfaces, trying once more: $ex")
//                        super.discoverNetworkInterfaces()
//                    }
//                }
//            }


            _upnpService = UpnpServiceImpl(AndroidConfig(context))
            // initialization failed. no point in trying as even if we later get service, we
            //   do not re-intialize automatically
            FailedToInitialize = !(_upnpService!!.router.isEnabled)
            if(FailedToInitialize)
            {
                return false
            }

            // Add a listener for device registration events
            _upnpService!!.registry?.addListener(object : RegistryListener {
                // ssdp datagrams have been alive and processed
                // services are unhydrated, service descriptors not yet retrieved
                override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
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

                    //println("Device added: ${rootDevice.displayString}.  Fully Initialized? {device.isFullyHydrated()}")

                    if (rootDevice.type.type.equals(UpnpManager.Companion.UPnPNames.InternetGatewayDevice)) // version agnostic
                    {
                        OurLogger.log(Level.INFO, "Device ${rootDevice.displayString} is of interest, type is ${rootDevice.type}")

                        // http://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v1-Service.pdf
                        // Device Tree: InternetGatewayDevice > WANDevice > WANConnectionDevice
                        // Service is WANIPConnection
                        var wanDevice = rootDevice.embeddedDevices.firstOrNull { it.type.type == UpnpManager.Companion.UPnPNames.WANDevice}
                        if (wanDevice != null)
                        {
                            var wanConnectionDevice = wanDevice.embeddedDevices.firstOrNull {it.type.type == UpnpManager.Companion.UPnPNames.WANConnectionDevice }
                            if (wanConnectionDevice != null)
                            {
                                var wanIPService = wanConnectionDevice.services.firstOrNull { it.serviceType.type == UpnpManager.Companion.UPnPNames.WANIPConnection }
                                if (wanIPService != null)
                                {
                                    //get relevant actions here...
                                    //TODO add relevant service (and cause event)
                                    var igdDevice = IGDDevice(rootDevice, wanIPService)
                                    UpnpManager.AddDevice(igdDevice)
                                    //TODO get port mappings from this relevant service
                                    igdDevice.EnumeratePortMappings()

                                }
                                else
                                {
                                    OurLogger.log(Level.SEVERE, "WanConnectionDevice does not have WanIPConnection service")
                                }
                            }
                            else
                            {
                                OurLogger.log(Level.SEVERE, "WanConnectionDevice not found under WanDevice")
                            }
                        }
                        else
                        {
                            OurLogger.log(Level.SEVERE, "WanDevice not found under InternetGatewayDevice")
                        }
                    }
                    else
                    {
                        OurLogger.log(Level.INFO, "Device ${rootDevice.displayString} is NOT of interest, type is ${rootDevice.type}")
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
            });

            _initialized = true
            return true
        }

        fun AddDevice(igdDevice: IGDDevice) {
            AnyIgdDevices?.value = true //Reading a state that was created after the snapshot was taken or in a snapshot that has not yet been applied
            synchronized(lockIgdDevices)
            {
                IGDDevices.add(igdDevice)
            }
            UpnpManager.DeviceFoundEvent.invoke(igdDevice)
        }

        var lockIgdDevices = Any()

        fun AnyExistingRules() : Boolean
        {
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices)
                {
                    if (device.portMappings.isNotEmpty()) {
                        return true
                    }
                }
                return false
            }
        }

        // returns anyEnabled, anyDisabled
        fun GetExistingRuleInfos() : Pair<Boolean, Boolean>
        {
            var anyEnabled : Boolean = false;
            var anyDisabled : Boolean = false;
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices)
                {
                    for (portMapping in device.portMappings)
                    {
                        if(portMapping.Enabled)
                        {
                            anyEnabled = true
                        }
                        else
                        {
                            anyDisabled = true
                        }

                        if(anyEnabled && anyDisabled)
                        {
                            //exit early
                            return Pair(anyEnabled, anyDisabled)
                        }
                    }
                }
            }
            return Pair(anyEnabled, anyDisabled)
        }

        // returns anyEnabled, anyDisabled
        fun GetEnabledDisabledRules(enabledRules : Boolean) : MutableList<PortMapping>
        {
            var enableDisabledPortMappings : MutableList<PortMapping> = mutableListOf();
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices)
                {
                    for (portMapping in device.portMappings)
                    {
                        if(portMapping.Enabled == enabledRules)
                        {
                            enableDisabledPortMappings.add(portMapping)
                        }
                    }
                }
            }
            return enableDisabledPortMappings
        }

        fun GetAllRules() : MutableList<PortMapping>
        {
            var allRules : MutableList<PortMapping> = mutableListOf();
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices)
                {
                    allRules.addAll(device.portMappings)
                }
            }
            return allRules
        }

        var IGDDevices : MutableList<IGDDevice> = mutableListOf()


        fun GetSpecificPortMappingRule(
            remoteIp : String,
            remotePort : String,
            protocol : String,
            callback: (UPnPCreateMappingResult) -> Unit)
             : Future<Any> {



            var device : IGDDevice = getIGDDevice(remoteIp)
            var action = device.actionsMap[ACTION_NAMES.GetSpecificPortMappingEntry]
            var actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost",remoteIp)
            actionInvocation.setInput("NewExternalPort",remotePort)
            actionInvocation.setInput("NewProtocol",protocol)

            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!

                    println("Successfully readback our new rule")

                    var internalPort = actionInvocation.getOutput("NewInternalPort")
                    var internalClient = actionInvocation.getOutput("NewInternalClient")
                    var enabled = actionInvocation.getOutput("NewEnabled")
                    var description = actionInvocation.getOutput("NewPortMappingDescription")
                    var leaseDuration = actionInvocation.getOutput("NewLeaseDuration")

                    var pm = PortMapping(
                        description.toString(),
                        remoteIp.toString(),
                        internalClient.toString(),
                        remotePort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt(),
                        remoteIp)

                    var result = UPnPCreateMappingResult(true)
                    result.ResultingMapping = pm
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

                    var result = UPnPCreateMappingResult(false)
                    result.UPnPFailureResponse = operation
                    result.FailureReason = defaultMsg
                    callback(result)
                }
            })


            return future
        }

        fun DisableEnablePortMappingEntry(
            portMapping: PortMapping,
            enable : Boolean,
            onDisableEnableCompleteCallback : (UPnPCreateMappingResult) -> Unit) : Future<Any>
        {
            // AddPortMapping
            //  This action creates a new port mapping or overwrites an existing mapping with the same internal client. If
            //  the ExternalPort and PortMappingProtocol pair is already mapped to another internal client, an error is
            //  returned.  (On my Nokia, I can modify other devices rules no problem).
            // However, deleting a mapping it is only a recommendation that they be the same..
            //  so Edit = Delete and Add is more powerful?


            var portMappingRequest = PortMappingRequest(
                portMapping.Description,
                portMapping.LocalIP,
                portMapping.InternalPort.toString(),
                portMapping.ActualExternalIP,
                portMapping.ExternalPort.toString(),
                portMapping.Protocol,
                portMapping.LeaseDuration.toString(),
                enable)
            return CreatePortMappingRule(portMappingRequest, false, onDisableEnableCompleteCallback)
        }

        fun enableDisableDefaultCallback(result : UPnPCreateMappingResult)
        {
            println("adding rule callback")
            if (result.Success!!)
            {
                result.ResultingMapping!!
                var device =
                    UpnpManager.getIGDDevice(result.ResultingMapping!!.ActualExternalIP)
                var oldMappingIndex = device.getMappingIndex(result.ResultingMapping!!.ExternalPort, result.ResultingMapping!!.Protocol) //.portMappings.indexOf(portMapping)
                device.portMappings[oldMappingIndex] = result.ResultingMapping!!
                UpnpManager.UpdateUIFromData.invoke(null)
            }
        }

        // DeletePortMappingRange is only available in v2.0 (also it can only delete
        //   a contiguous range)
        fun DeletePortMappingEntry(portMapping : PortMapping) : Future<Any>
        {

            fun callback(result : UPnPResult) {
                defaultRuleDeletedCallback(result)
                RunUIThread {
                    println("delete callback")
                    if (result.Success!!) {
                        Toast.makeText(
                            PortForwardApplication.appContext,
                            "Success",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            PortForwardApplication.appContext,
                            "Failure - ${result.FailureReason!!}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }


            var future = DeletePortMapping(portMapping, ::callback)
            return future
        }

        fun DeletePortMappingsEntry(portMappings : MutableList<PortMapping>,
                                    onCompleteBatchCallback: (MutableList<UPnPResult?>) -> Unit) : Future<MutableList<PortMapping>>
        {
            val callable = Callable {

                var listOfResults: MutableList<UPnPResult?> =
                    MutableList(portMappings.size) { null }

                for (i in 0 until portMappings.size) {

                    fun callback(result: UPnPResult) {

                        defaultRuleDeletedCallback(result)
                        listOfResults[i] = result
                    }

                    var future = DeletePortMapping(portMappings[i], ::callback)
                    future.get()
                }

                onCompleteBatchCallback(listOfResults)

                portMappings
            }

            val task = FutureTask(callable)
            Thread(task).start()
            return task // a FutureTask is a Future
        }

        //TODO: if List all port mappings exists, that should be used instead of getgeneric.

        fun DeletePortMapping(portMapping : PortMapping, callback : (UPnPResult) -> Unit) : Future<Any>
        {
            var device : IGDDevice = getIGDDevice(portMapping.ExternalIP)
            var action = device.actionsMap[ACTION_NAMES.DeletePortMapping]
            var actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost", "${portMapping.ActualExternalIP}");
            // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
            actionInvocation.setInput("NewExternalPort", "${portMapping.ExternalPort}");
            actionInvocation.setInput("NewProtocol", "${portMapping.Protocol}");

            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    println("Successfully deleted")

                    var result = UPnPResult(true)
                    result.RequestInfo = portMapping
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

                    var result = UPnPResult(false)
                    result.FailureReason = defaultMsg
                    result.UPnPFailureResponse = operation
                    callback(result)
                }
            })

            return future

        }

        // this method creates a rule, then grabs it again to verify it.
        fun CreatePortMappingRule(
            portMappingRequest : PortMappingRequest,
            skipReadingBack : Boolean,
            callback: (UPnPCreateMappingResult) -> Unit) : Future<Any>
        {
            //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()

            var externalIp = portMappingRequest.externalIp

            var device : IGDDevice = getIGDDevice(externalIp)
            var action = device.actionsMap[ACTION_NAMES.AddPortMapping]
            var actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost", externalIp);
            // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
            actionInvocation.setInput("NewExternalPort", portMappingRequest.externalPort);
            actionInvocation.setInput("NewProtocol", portMappingRequest.protocol);
            actionInvocation.setInput("NewInternalPort", portMappingRequest.internalPort);
            actionInvocation.setInput("NewInternalClient", portMappingRequest.internalIp);
            actionInvocation.setInput("NewEnabled", if (portMappingRequest.enabled) "1" else "0");
            actionInvocation.setInput("NewPortMappingDescription", portMappingRequest.description);
            actionInvocation.setInput("NewLeaseDuration", portMappingRequest.leaseDuration);

            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    println("Successfully added, now reading back")
                    if(skipReadingBack)
                    {
                        var result = UPnPCreateMappingResult(true) //TODO : parameter on whether we read back + parameter on the original requested (in case its different)
                        result.ResultingMapping = portMappingRequest.realize()
                        callback(result)
                    }
                    else {
                        var specificFuture = GetSpecificPortMappingRule(
                            externalIp,
                            portMappingRequest.externalPort,
                            portMappingRequest.protocol,
                            callback
                        )
                        specificFuture.get()
                    }

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

                    var result = UPnPCreateMappingResult(false)
                    result.FailureReason = defaultMsg
                    result.UPnPFailureResponse = operation
                    callback(result)
                }
            })

            return future
        }

        public fun getIGDDevice(ipAddr : String): IGDDevice {
            return IGDDevices.first {it.ipAddress == ipAddr };
        }


        //fun List<IGDDevice>
    }
}

fun defaultRuleAddedCallback(result : UPnPCreateMappingResult) {
    println("default adding rule callback")
    if (result.Success!!)
    {
        result.ResultingMapping!!
        var device =
            UpnpManager.getIGDDevice(result.ResultingMapping!!.ActualExternalIP)
        device.portMappings.add(result.ResultingMapping!!)
        UpnpManager.PortFoundEvent.invoke(result.ResultingMapping!!)

    }
    else
    {

    }
}

fun defaultRuleDeletedCallback(result : UPnPResult) {
    println("default adding rule callback")
    if (result.Success!!)
    {
        result.RequestInfo!!
        var device =
            UpnpManager.getIGDDevice(result.RequestInfo!!.ActualExternalIP)
        device.portMappings.remove(result.RequestInfo)
        UpnpManager.UpdateUIFromData.invoke(null)
    }
    else
    {

    }
}


data class PortMappingUserInput(val description : String, val internalIp : String, val internalPort : String, val externalIp : String, val externalPort : String, val protocol : String, val leaseDuration : String, val enabled : Boolean)
{
    fun with(internalPortSpecified : String, externalPortSpecified : String, portocolSpecified : String) : PortMappingRequest
    {
        return PortMappingRequest(description, internalIp, internalPortSpecified, externalIp, externalPortSpecified, portocolSpecified, leaseDuration, enabled)
    }
}
data class PortMappingRequest(val description : String, val internalIp : String, val internalPort : String, val externalIp : String, val externalPort : String, val protocol : String, val leaseDuration : String, val enabled : Boolean)
{
    fun realize() : PortMapping
    {
        return PortMapping(description, externalIp, internalIp, externalPort.toInt(), internalPort.toInt(), protocol, enabled, leaseDuration.toInt(), externalIp)
    }
}


//CompletableFuture is api >=24
//basic futures do not implement continuewith...

//class UPnPCreateMappingResult : UPnPResult
//{
//    constructor() : super()
//
//    @Volatile
//    var ResultingMapping : PortMapping? = null
//}
//
//class UPnPGetSpecificMappingResult : UPnPResult
//{
//    constructor() : super()
//
//    @Volatile
//    var ResultingMapping : PortMapping? = null
//}
//
//open class UPnPResult constructor()
//{
//    var Future : Future<Any>? = null
//
//    @Volatile
//    var Success : Boolean? = null
//    @Volatile
//    var FailureReason : String? = null
//
////    init
////    {
////        Success = success
////        Future = future
////    }
//}



class UPnPCreateMappingResult : UPnPResult
{
    constructor(success: Boolean) : super(success)
    var ResultingMapping : PortMapping? = null
}

class UPnPGetSpecificMappingResult : UPnPResult
{
    constructor(success : Boolean) : super(success)
    var ResultingMapping : PortMapping? = null
}

open class UPnPResult constructor(success : Boolean)
{
    var Success : Boolean
    var FailureReason : String? = null
    var UPnPFailureResponse : UpnpResponse? = null
    var RequestInfo : PortMapping? = null

    init
    {
        Success = success
    }
}

data class OurNetworkInfoBundle(val networkType: NetworkType, val ourIp: String?, val gatewayIp : String?)

class OurNetworkInfo {
    companion object { //singleton

        var retrieved: Boolean = false
        var ourIp: String? = null
        var gatewayIp: String? = null
        var networkType: NetworkType? = null

        fun GetNetworkInfo(context: Context, forceRefresh: Boolean): OurNetworkInfoBundle {
            GetConnectionType(context, forceRefresh)
            if (networkType == NetworkType.WIFI) {
                GetLocalAndGatewayIpAddrWifi(context, forceRefresh)
            } else {
                ourIp = null
                gatewayIp = null
            }

            return OurNetworkInfoBundle(networkType!!, ourIp, gatewayIp)
        }

        fun GetLocalAndGatewayIpAddrWifi(
            context: Context,
            forceRefresh: Boolean
        ): Pair<String?, String?> {
            if (!forceRefresh && retrieved) {
                return Pair(ourIp, gatewayIp)
            }

            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ipAddress = wifiManager.connectionInfo.ipAddress

            //var macAddress = wifiManager.connectionInfo.macAddress
            val gatewayIpAddress = wifiManager.dhcpInfo.gateway

            ourIp = formatIpv4(ipAddress)
            gatewayIp = formatIpv4(gatewayIpAddress)
            retrieved = true
            return Pair(ourIp, gatewayIp)

//        val byteBuffer = ByteBuffer.allocate(4)
//        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
//        byteBuffer.putInt(ipAddress)
//        val inetAddress = InetAddress.getByAddress(null, byteBuffer.array())
//        var hostAddress = inetAddress.hostAddress
//        val macParts: List<String> = macAddress.split(":")
//        val macBytes = ByteArray(macParts.size)
//        for (i in macParts.indices) {
//            macBytes[i] = macParts[i].toInt(16).toByte()
//        }
//
//        val en = NetworkInterface.getNetworkInterfaces()
//        while (en.hasMoreElements()) {
//            val intf = en.nextElement()
//            if (intf.hardwareAddress == macBytes) {
//                val enumIpAddr = intf.inetAddresses
//                while (enumIpAddr.hasMoreElements()) {
//                    val inetAddress = enumIpAddr.nextElement()
//                    if (!inetAddress.isLoopbackAddress) {
//                        return inetAddress.hostAddress
//                    }
//                }
//            }
//        }
//    } catch (ex: java.lang.Exception) {
//        Log.e("IP Address", ex.toString())
//    }
//    return null
        }

        fun GetNameTypeMappings(context: Context) : MutableMap<String, NetworkType> {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var mappings : MutableMap<String, NetworkType> = mutableMapOf<String, NetworkType>()
            for (net1 in cm.getAllNetworks()) {
                var netInfo = cm.getNetworkInfo(net1)
                var name = cm.getLinkProperties(net1)?.interfaceName
                if(name == null)
                {
                    continue
                }
                var type = getNetworkType(cm, net1, netInfo)
                mappings[name] = type
            }
            return mappings
        }

        fun GetTypeFromInterfaceName(_mappings : MutableMap<String, NetworkType>?, interfaceName : String) : NetworkType
        {
            var mappings = _mappings
            if (mappings == null)
            {
                mappings = GetNameTypeMappings(PortForwardApplication.appContext) //TODO: dont call this expensive call everytime
            }
            mappings
            if (mappings.containsKey(interfaceName))
            {
                return mappings[interfaceName] ?: NetworkType.NONE
            }
            else {
                if (interfaceName.contains("wlan"))
                {
                    return NetworkType.WIFI
                }
                else if(interfaceName.contains("rmnet") || interfaceName.contains("data"))
                {
                    return NetworkType.DATA
                }
            }
            return NetworkType.NONE
        }

        fun GetConnectionType(context: Context, forceRefresh: Boolean): NetworkType {

            if (!forceRefresh && networkType != null) {
                return networkType!!
            }


            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


            //get type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                networkType = getNetworkType(cm, cm.activeNetwork, cm.activeNetworkInfo)
            }
            else
            {
                networkType = getNetworkType(cm, null, cm.activeNetworkInfo)
            }
            return networkType as NetworkType
        }

        fun getNetworkType(cm: ConnectivityManager, network : Network?, networkInfo : NetworkInfo?) : NetworkType {
            var result = NetworkType.NONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.getNetworkCapabilities(network)?.run {
                    when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            result = NetworkType.WIFI
                        }

                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            result = NetworkType.DATA
                        }

                        else -> {
                            result = NetworkType.NONE
                        }
                    }
                }
            } else {
                networkInfo?.run {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            result = NetworkType.WIFI
                        }

                        ConnectivityManager.TYPE_MOBILE -> {
                            result = NetworkType.DATA
                        }

                        else -> {
                            result = NetworkType.NONE
                        }
                    }
                }
            }
            return result
        }

    }

    // java.net.networkinterface does not have .type etc.
//    fun test1(): String? {
//        try {
//            for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
//                for (inetAddress in Collections.list(networkInterface.inetAddresses)) {
//                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
//                        return inetAddress.hostAddress
//                    }
//                }
//            }
//        } catch (ex: SocketException) {
//            Log.e("IP Address", "Failed getting IP address", ex)
//        }
//        return null
//    }
}


class ConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {

        p0!!
        val cm = p0.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected) {
            println("info: " + cm.activeNetworkInfo!!.detailedState.toString())

            //TODO: multiple transports
            var isWifi = cm.activeNetworkInfo!!.type == ConnectivityManager.TYPE_WIFI
            if (isWifi) {
                PortForwardApplication.ShowToast("Is Connected Wifi", Toast.LENGTH_LONG)
            }

            var isData = cm.activeNetworkInfo!!.type == ConnectivityManager.TYPE_MOBILE
            if (isData) {
                PortForwardApplication.ShowToast("Is Connected Data", Toast.LENGTH_LONG)
            }

            //networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            //if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) return networkInfo;

//            if (info.IsConnected) {
//                PortForwardApplication.ShowToast("Is Connected", Toast.LENGTH_LONG)
//                SeekerApplication.ShowToast("Is Connected Wifi", ToastLength.Long)
//            }
//            info = cm.GetNetworkInfo(ConnectivityType.Mobile)
//            if (info.IsConnected) {
//                SeekerApplication.ShowToast("Is Connected Mobile", ToastLength.Long)
//            }
//        } else {
//            if (cm.activeNetworkInfo != null) {
//                MainActivity.LogDebug("info: " + cm.ActiveNetworkInfo.GetDetailedState().ToString())
//                SeekerApplication.ShowToast("Is Disconnected", ToastLength.Long)
//            } else {
//                MainActivity.LogDebug("info: Is Disconnected(null)")
//                SeekerApplication.ShowToast("Is Disconnected (null)", ToastLength.Long)
//            }
//        }
        }
    }
}

enum class NetworkType(val networkTypeString: String) {
    NONE("None"),
    WIFI("Wifi"),
    DATA("Data");

    override fun toString(): String {
        return networkTypeString
    }
}


class IGDDevice constructor(_rootDevice : RemoteDevice, _wanIPService : RemoteService)
{

    var rootDevice : RemoteDevice
    var wanIPService : RemoteService
    var displayName : String //i.e. Nokia IGD Version 2.00
    var friendlyDetailsName : String //i.e. Internet Home Gateway Device
    var manufacturer : String //i.e. Nokia
    var ipAddress : String //i.e. 192.168.18.1
    var upnpType : String //i.e. InternetGatewayDevice
    var upnpTypeVersion : Int //i.e. 2
    var actionsMap : MutableMap<String, Action<RemoteService>>
    var portMappings : MutableList<PortMapping> = mutableListOf()
//    var hasAddPortMappingAction : Boolean
//    var hasDeletePortMappingAction : Boolean

    fun getMappingIndex(externalPort : Int, protocol : String) : Int
    {
        return portMappings.indexOfFirst { it.ExternalPort == externalPort && it.Protocol == protocol }
    }

    init {
        this.rootDevice = _rootDevice
        this.wanIPService = _wanIPService
        this.displayName = this.rootDevice.displayString
        this.friendlyDetailsName = this.rootDevice.details.friendlyName
        this.ipAddress = this.rootDevice.details.presentationURI.host //TODO if null maybe another way to find out
        this.manufacturer = this.rootDevice.details.manufacturerDetails.manufacturer
        this.upnpType = this.rootDevice.type.type
        this.upnpTypeVersion = this.rootDevice.type.version
        this.actionsMap = mutableMapOf()
        for (action in _wanIPService.actions)
        {
            if(UpnpManager.ActionNames.contains(action.name))
            {
                // TODO: convert "actions" into strongly typed functions so they
                // are easy to call and parse output
                actionsMap[action.name] = action
            }
        }
    }



    fun EnumeratePortMappings()
    {
        // we enumerate port mappings later
        portMappings = mutableListOf()
        var finishedEnumeratingPortMappings = false

        val timeTakenMillis = measureTimeMillis {

//        if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings))
//        {
//            OurLogger.log(Level.INFO, "Enumerating Port Listings using GetListOfPortMappings")
//            var getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings]!!
//            getAllPortMappingsUsingListPortMappings(getPortMapping)
//        }
        if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry))
        {
            OurLogger.log(Level.INFO, "Enumerating Port Listings using GetGenericPortMappingEntry")
            var getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry]!!
            getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping)
        }
        else{
            OurLogger.log(Level.SEVERE, "device does not have GetGenericPortMappingEntry")
        }

        }
        OurLogger.log(Level.INFO, "Time to enumerate ports: $timeTakenMillis ms")


        finishedEnumeratingPortMappings = true
        UpnpManager.FinishedListingPortsEvent.invoke(this)
    }

//    private fun getAllPortMappingsUsingListPortMappings(getPortMapping : Action<RemoteService>) {
//
//        // this method doesnt work.
//        // if I specify a small range it returns
//        // but anything more than 100 entries it gives 500
//
//
//        var slotIndex : Int = 0;
//        var retryCount : Int = 0
//        while(true)
//        {
//            var shouldRetry : Boolean = false
//            var success : Boolean = false;
//            var actionInvocation = ActionInvocation(getPortMapping)
//            println("requesting slot $slotIndex")
//
//            actionInvocation.setInput("NewStartPort", "$MIN_PORT");
//            actionInvocation.setInput("NewEndPort", "$MAX_PORT");
//            actionInvocation.setInput("NewProtocol", "TCP");
//            actionInvocation.setInput("NewManage", "1");
//            actionInvocation.setInput("NewNumberOfPorts", "0");
//            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
//                override fun success(invocation: ActionInvocation<*>?) {
//                    invocation!!
//
//                    var dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
//                    val dBuilder = dbFactory.newDocumentBuilder()
//                    val inputSource = InputSource(StringReader(actionInvocation.getOutput("A_ARG_TYPE_PortListing").toString()))
//                    val doc = dBuilder.parse(inputSource)
//                    doc.documentElement.normalize()
//
//                    retryCount = 0
//                    OurLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")
//
//                    var remoteHost = invocation.getOutput("NewRemoteHost") //string datatype // the .value is null (also empty if GetListOfPortMappings is used)
//                    var externalPort = invocation.getOutput("NewExternalPort") //unsigned 2 byte int
//                    var internalClient = invocation.getOutput("NewInternalClient") //string datatype
//                    var internalPort = invocation.getOutput("NewInternalPort")
//                    var protocol = invocation.getOutput("NewProtocol")
//                    var description = invocation.getOutput("NewPortMappingDescription")
//                    var enabled = invocation.getOutput("NewEnabled")
//                    var leaseDuration = invocation.getOutput("NewLeaseDuration")
//                    var portMapping = PortMapping(
//                        description.toString(),
//                        remoteHost.toString(),
//                        internalClient.toString(),
//                        externalPort.toString().toInt(),
//                        internalPort.toString().toInt(),
//                        protocol.toString(),
//                        enabled.toString().toInt() == 1,
//                        leaseDuration.toString().toInt(),
//                        ipAddress)
//                    portMappings.add(portMapping)
//                    // TODO: new port mapping added event
//                    UpnpManager.PortFoundEvent.invoke(portMapping)
//                    success = true
//                }
//
//                override fun failure(
//                    invocation: ActionInvocation<*>?,
//                    operation: UpnpResponse,
//                    defaultMsg: String
//                ) {
//                    retryCount = 0
//                    OurLogger.log(Level.INFO, "GetGenericPortMapping failed for entry $slotIndex: $defaultMsg")
//                    // Handle failure
//                }
//            })
//
//            try {
//                future.get() // SYNCHRONOUS (note this can, and does, throw)
//            }
//            catch(e : Exception)
//            {
//                if(retryCount == 0) // if two exceptions in a row then stop.
//                {
//                    shouldRetry = true // network issue.. retrying does work.
//                    retryCount++
//                }
//                OurLogger.log(Level.SEVERE, "GetGenericPortMapping threw ${e.message}")
//            }
//
//            if(shouldRetry)
//            {
//                OurLogger.log(Level.INFO, "Retrying for entry $slotIndex")
//                continue
//            }
//
//
//            if (!success)
//            {
//                break
//            }
//
//            slotIndex++
//
//            if (slotIndex > MAX_PORT)
//            {
//                OurLogger.log(Level.SEVERE, "CRITICAL ERROR ENUMERATING PORTS, made it past 65535")
//            }
//        }
//    }

    val MIN_PORT = 1 // 0 is invalid port. wildcard port i.e. let the system choose.
    val MAX_PORT = 65535

    private fun getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping : Action<RemoteService>) {
        var slotIndex : Int = 0;
        var retryCount : Int = 0
        while(true)
        {
            var shouldRetry : Boolean = false
            var success : Boolean = false;
            var actionInvocation = ActionInvocation(getPortMapping)
            println("requesting slot $slotIndex")
            actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    retryCount = 0
                    OurLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")

                    var remoteHost = invocation.getOutput("NewRemoteHost") //string datatype // the .value is null (also empty if GetListOfPortMappings is used)
                    var externalPort = invocation.getOutput("NewExternalPort") //unsigned 2 byte int
                    var internalClient = invocation.getOutput("NewInternalClient") //string datatype
                    var internalPort = invocation.getOutput("NewInternalPort")
                    var protocol = invocation.getOutput("NewProtocol")
                    var description = invocation.getOutput("NewPortMappingDescription")
                    var enabled = invocation.getOutput("NewEnabled")
                    var leaseDuration = invocation.getOutput("NewLeaseDuration")
                    var portMapping = PortMapping(
                        description.toString(),
                        remoteHost.toString(),
                        internalClient.toString(),
                        externalPort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt(),
                        ipAddress)
                    portMappings.add(portMapping)
                    // TODO: new port mapping added event
                    UpnpManager.PortFoundEvent.invoke(portMapping)
                    success = true
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    retryCount = 0
                    OurLogger.log(Level.INFO, "GetGenericPortMapping failed for entry $slotIndex: $defaultMsg")
                    // Handle failure
                }
            })

            try {
                future.get() // SYNCHRONOUS (note this can, and does, throw)
            }
            catch(e : Exception)
            {
                if(retryCount == 0) // if two exceptions in a row then stop.
                {
                    shouldRetry = true // network issue.. retrying does work.
                    retryCount++
                }
                OurLogger.log(Level.SEVERE, "GetGenericPortMapping threw ${e.message}")
            }

            if(shouldRetry)
            {
                OurLogger.log(Level.INFO, "Retrying for entry $slotIndex")
                continue
            }


            if (!success)
            {
                break
            }

            slotIndex++

            if (slotIndex > 65535)
            {
                OurLogger.log(Level.SEVERE, "CRITICAL ERROR ENUMERATING PORTS, made it past 65535")
            }
        }
    }
}

class Event<T> {
    private val observers = mutableSetOf<(T) -> Unit>()

    operator fun plusAssign(observer: (T) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value: T) {
        for (observer in observers)
            observer(value)
    }
}

class UPnPViewElement constructor(
    underlyingElement : kotlin.Any,
    isSpecialEmpty : Boolean = false)
{
    var UnderlyingElement : kotlin.Any
    var IsSpecialEmpty : kotlin.Boolean

    init
    {
        IsSpecialEmpty = isSpecialEmpty
        UnderlyingElement = underlyingElement
    }

    fun IsIGDDevice() : Boolean
    {
        return UnderlyingElement is IGDDevice
    }

    fun GetUnderlyingIGDDevice() : IGDDevice
    {
        return UnderlyingElement as IGDDevice
    }

    fun IsPortMapping() : Boolean
    {
        return UnderlyingElement is PortMapping
    }

    fun GetUnderlyingPortMapping() : PortMapping
    {
        return UnderlyingElement as PortMapping
    }
}

class PortMapping constructor(
    val _Description: String,
    val _ExternalIP: String,
    val _LocalIP: String,
    val _ExternalPort: Int,
    val _InternalPort: Int,
    val _Protocol: String,
    val _Enabled: Boolean,
    val _LeaseDuration: Int,
    val _ActionExternalIP : String)
{
    var ExternalIP : String // the returned ip from get port mapping
    var LocalIP : String
    var ExternalPort : Int
    var InternalPort : Int
    var Protocol : String
    var Enabled : Boolean
    var LeaseDuration : Int
    var Description : String
    var ActualExternalIP : String // the actual ip of the IGD device

    init {
        this.ExternalIP = _ExternalIP
        this.LocalIP = _LocalIP
        this.ExternalPort = _ExternalPort
        this.InternalPort = _InternalPort
        this.Protocol = _Protocol
        this.Enabled = _Enabled
        this.LeaseDuration = _LeaseDuration
        this.Description = _Description
        this.ActualExternalIP = _ActionExternalIP
    }
}

class AndroidConfig(context : Context) : AndroidUpnpServiceConfiguration() {

    var Context : Context
    init {
        Context = context
    }

    override fun getServiceDescriptorBinderUDA10(): ServiceDescriptorBinder {
        return UDA10ServiceDescriptorBinderImpl()
    }

    // in case you want to get additional info from initialization
    // we can then maybe check if they support multicast (i.e. data typically does not)
    // and throw away.  also we can use connectionmanager to enumerate interfaces
    // by default this returns new AndroidNetworkAddressFactory
    // AndroidNetworkAddressFactory has a method discoverNetworkInterfaces() which
    //   calls the java NetworkInterface.getNetworkInterfaces();

    override fun createNetworkAddressFactory(streamListenPort: Int): NetworkAddressFactory? {
        //return NetworkAddressFactoryImpl(streamListenPort)
        var addressFactory = AndroidNetworkAddressFactory(streamListenPort);

        // the final set of usable interfaces and bind addresses
        var iterator = addressFactory.networkInterfaces
        val networkInterfaces: MutableList<java.net.NetworkInterface> = ArrayList()
        while (iterator.hasNext()) {
            networkInterfaces.add(iterator.next())
        }
        NetworkInterfacesUsed = networkInterfaces
        NetworkMappings = OurNetworkInfo.GetNameTypeMappings(Context)

        NetworkInterfacesUsedInfos = mutableListOf()
        for (netInterface in networkInterfaces)
        {
            NetworkInterfacesUsedInfos?.add(Pair<java.net.NetworkInterface, NetworkType>(netInterface, OurNetworkInfo.GetTypeFromInterfaceName(NetworkMappings, netInterface.name)))
        }

        return addressFactory
    }

    var NetworkInterfacesUsed : MutableList<java.net.NetworkInterface>? = null
    var NetworkMappings : MutableMap<String, NetworkType>? = null
    var NetworkInterfacesUsedInfos : MutableList<Pair<java.net.NetworkInterface, NetworkType>>? = null
}
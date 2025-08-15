package com.shinjiindustrial.portmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.UpnpManager.Companion.invokeUpdateUIFromData
import com.shinjiindustrial.portmapper.UpnpManager.Companion.lockIgdDevices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.logging.Level
import kotlin.system.measureTimeMillis


class UpnpManager {


    companion object { //singleton
        private var _upnpService: UpnpService? = null
        private var _initialized: Boolean = false
        var HasSearched: Boolean = false
        var AnyIgdDevices: MutableState<Boolean>? = null

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

        object ACTION_NAMES {
            // these actions are all Required in v1, v2
            val AddPortMapping: String = "AddPortMapping"
            val GetExternalIPAddress: String = "GetExternalIPAddress"
            val DeletePortMapping: String = "DeletePortMapping"
            val GetStatusInfo: String = "GetStatusInfo"
            val GetGenericPortMappingEntry: String = "GetGenericPortMappingEntry"
            val GetSpecificPortMappingEntry: String = "GetSpecificPortMappingEntry"

            // this action is required in v2, not present in v1
            var AddAnyPortMapping: String = "AddAnyPortMapping"

            // this action is required for device, optional for control point in v2, not present in v1
            var GetListOfPortMappings: String = "GetListOfPortMappings"
        }

        val ActionNames: List<String> = listOf(
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
        var PortAddedEvent = Event<PortMapping>()
        var PortInitialFoundEvent = Event<PortMapping>()

        // used if we finish and there are no ports to show the "no devices" card
        var FinishedListingPortsEvent = Event<IGDDevice>()
        var UpdateUIFromData = Event<Any?>()
        var SearchStarted = Event<Any?>()

        var UpdateUIFromDataCollating: MutableSharedFlow<Any?> = MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        fun SubscibeToUpdateData(coroutineScope: CoroutineScope, eventHandler: () -> Unit) {
            UpnpManager.UpdateUIFromDataCollating.conflate().onEach {
                eventHandler()
            }.launchIn(coroutineScope)
        }
        //var NetworkInfoAtTimeOfSearch : OurNetworkInfoBundle? = null

        fun Search(onlyIfNotYetSearched: Boolean): Boolean {
            if (onlyIfNotYetSearched && HasSearched) {
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

        fun ClearOldData() {
            UpnpManager.GetUPnPService()?.registry?.removeAllLocalDevices()
            UpnpManager.GetUPnPService()?.registry?.removeAllRemoteDevices() // otherwise Add wont be called again (just Update)
            AnyIgdDevices?.value = false
            synchronized(lockIgdDevices)
            {
                IGDDevices.clear()
            }
            invokeUpdateUIFromData()
        }

        fun GetDeviceByExternalIp(gatewayIp: String): IGDDevice? {
            synchronized(UpnpManager.lockIgdDevices)
            {
                for (device in UpnpManager.IGDDevices) {
                    if (gatewayIp == device.ipAddress) {
                        return device
                    }
                }
            }
            return null
        }

        fun GetGatewayIpsWithDefault(deviceGateway: String): Pair<MutableList<String>, String> {
            val gatewayIps: MutableList<String> = mutableListOf()
            var defaultGatewayIp = ""
            synchronized(UpnpManager.lockIgdDevices)
            {
                for (device in UpnpManager.IGDDevices) {
                    gatewayIps.add(device.ipAddress)
                    if (device.ipAddress == deviceGateway) {
                        defaultGatewayIp = device.ipAddress
                    }
                }
            }

            if (defaultGatewayIp == "" && !gatewayIps.isEmpty()) {
                defaultGatewayIp = gatewayIps[0]
            }
            return Pair<MutableList<String>, String>(gatewayIps, defaultGatewayIp)
        }

        fun GetUPnPService(): UpnpService {
            return _upnpService!!
        }

        fun splitUserInputIntoRules(portMappingUserInput: PortMappingUserInput): MutableList<PortMappingRequest> {
            val portMappingRequests: MutableList<PortMappingRequest> = mutableListOf()
            val (inStart, inEnd) = portMappingUserInput.getRange(true)
            val (outStart, outEnd) = portMappingUserInput.getRange(false)
            val protocols = portMappingUserInput.getProtocols()

            val errorString = portMappingUserInput.validateRange()
            if (errorString.isNotEmpty()) {
                throw java.lang.Exception(errorString)
            }



            val inSize = inEnd - inStart + 1 // inclusive
            val outSize = outEnd - outStart + 1
            val sizeOfRange = maxOf(inSize, outSize) - 1 // if just 1 element then size is 0

            for (i in 0..sizeOfRange) {
                val inPort = if (inSize == 1) inStart else inStart + i
                val outPort = if (outSize == 1) outStart else outStart + i
                for (protocol in protocols) {
                    portMappingRequests.add(
                        portMappingUserInput.with(
                            inPort.toString(),
                            outPort.toString(),
                            protocol.toString()
                        )
                    )
                }
            }
            return portMappingRequests
        }


        data class PortMappingFullResult(
            val description: String,
            val internalIp: String,
            val internalPort: String
        )

        fun CreatePortMappingRulesEntry(
            portMappingUserInput: PortMappingUserInput,
            onCompleteBatchCallback: (MutableList<UPnPCreateMappingResult?>) -> Unit
        ): FutureTask<MutableList<PortMappingRequest>> {

                val callable = Callable {

                    try {


                    val portMappingRequestRules = splitUserInputIntoRules(portMappingUserInput)
                    val listOfResults: MutableList<UPnPCreateMappingResult?> =
                        MutableList(portMappingRequestRules.size) { null }

                    for (i in 0 until portMappingRequestRules.size) {

                        fun callback(result: UPnPCreateMappingResult) {

                            defaultRuleAddedCallback(result)
                            listOfResults[i] = result
                        }

                        val future = CreatePortMappingRule(
                            portMappingRequestRules[i],
                            false,
                            "created",
                            ::callback
                        )
                        future.get()
                    }

                    onCompleteBatchCallback(listOfResults)

                    portMappingRequestRules

                    }
                    catch(exception : Exception)
                    {
                        PortForwardApplication.OurLogger.log(
                            Level.SEVERE,
                            "Create Rule Failed: " + exception.message + exception.stackTraceToString()
                        )
                        MainActivity.showSnackBarViewLog("Create Rule Failed")
                        throw exception
                    }
                }

                val task = FutureTask(callable)
                Thread(task).start()
                return task // a FutureTask is a Future

        }

        fun DisableEnablePortMappingEntries(
            portMappings: List<PortMapping>,
            enable: Boolean,
            onCompleteBatchCallback: (MutableList<UPnPCreateMappingResult?>) -> Unit
        ): FutureTask<List<PortMapping>> {

                val callable = Callable {

                    try {

                    val listOfResults: MutableList<UPnPCreateMappingResult?> =
                        MutableList(portMappings.size) { null }

                    for (i in 0 until portMappings.size) {

                        fun callback(result: UPnPCreateMappingResult) {

                            enableDisableDefaultCallback(result)
                            listOfResults[i] = result
                        }

                        val future = DisableEnablePortMapping(portMappings[i], enable, ::callback)
                        future.get()
                    }

                    onCompleteBatchCallback(listOfResults)
                    portMappings

                    } catch (exception: Exception) {

                        val enableDisableString = if(enable) "Enable" else "Disable"
                        PortForwardApplication.OurLogger.log(
                            Level.SEVERE,
                            "$enableDisableString Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                        )
                        MainActivity.showSnackBarViewLog("$enableDisableString Port Mappings Failed")
                        throw exception
                    }
                }

                val task = FutureTask(callable)
                Thread(task).start()
                return task // a FutureTask is a Future


        }

        var FailedToInitialize: Boolean = false

        fun FullRefresh() {
            Initialize(PortForwardApplication.appContext, true)
            Search(false)
        }

        fun Initialize(context: Context, force: Boolean): Boolean {
            if (_initialized && !force) {
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
            if (FailedToInitialize) {
                return false
            }

            // Add a listener for device registration events
            _upnpService!!.registry?.addListener(object : RegistryListener {
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

                    //println("Device added: ${rootDevice.displayString}.  Fully Initialized? {device.isFullyHydrated()}")

                    if (rootDevice.type.type.equals(UpnpManager.Companion.UPnPNames.InternetGatewayDevice)) // version agnostic
                    {
                        OurLogger.log(
                            Level.INFO,
                            "Device ${rootDevice.displayString} is of interest, type is ${rootDevice.type}"
                        )

                        // http://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v1-Service.pdf
                        // Device Tree: InternetGatewayDevice > WANDevice > WANConnectionDevice
                        // Service is WANIPConnection
                        val wanDevice =
                            rootDevice.embeddedDevices.firstOrNull { it.type.type == UpnpManager.Companion.UPnPNames.WANDevice }
                        if (wanDevice != null) {
                            val wanConnectionDevice =
                                wanDevice.embeddedDevices.firstOrNull { it.type.type == UpnpManager.Companion.UPnPNames.WANConnectionDevice }
                            if (wanConnectionDevice != null) {
                                val wanIPService =
                                    wanConnectionDevice.services.firstOrNull { it.serviceType.type == UpnpManager.Companion.UPnPNames.WANIPConnection }
                                if (wanIPService != null) {
                                    //get relevant actions here...
                                    //TODO add relevant service (and cause event)
                                    val igdDevice = IGDDevice(rootDevice, wanIPService)
                                    UpnpManager.AddDevice(igdDevice)
                                    //TODO get port mappings from this relevant service
                                    igdDevice.EnumeratePortMappings()

                                } else {
                                    OurLogger.log(
                                        Level.SEVERE,
                                        "WanConnectionDevice does not have WanIPConnection service"
                                    )
                                }
                            } else {
                                OurLogger.log(
                                    Level.SEVERE,
                                    "WanConnectionDevice not found under WanDevice"
                                )
                            }
                        } else {
                            OurLogger.log(
                                Level.SEVERE,
                                "WanDevice not found under InternetGatewayDevice"
                            )
                        }
                    } else {
                        OurLogger.log(
                            Level.INFO,
                            "Device ${rootDevice.displayString} is NOT of interest, type is ${rootDevice.type}"
                        )
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
            // if not UI thread: Reading a state that was created after the snapshot was taken or in a snapshot that has not yet been applied
            GlobalScope.launch(Dispatchers.Main)
            {
                AnyIgdDevices?.value = true
            }
            Log.i("portmapperUI", "IGD device added")
            synchronized(lockIgdDevices)
            {
                val alreadyAdded = IGDDevices.any {it -> it.ipAddress == igdDevice.ipAddress && it.displayName == igdDevice.displayName }
                if(alreadyAdded)
                {
                    OurLogger.log(
                        Level.INFO,
                        "Device ${igdDevice.displayName} at ${igdDevice.ipAddress} has already been added. Ignoring."
                    )
                    // we can also choose to replace the old with the new... not sure, since this seems to be fired rarely but randomly..
                    // should test if having the old in this case causes any network issues.
                    return
                }
                IGDDevices.add(igdDevice)
            }
            OurLogger.log(
                Level.INFO,
                "Added Device ${igdDevice.displayName} at ${igdDevice.ipAddress}."
            )
            UpnpManager.DeviceFoundEvent.invoke(igdDevice)
        }

        var lockIgdDevices = Any()

        fun AnyExistingRules(): Boolean {
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    if (device.portMappings.isNotEmpty()) {
                        return true
                    }
                }
                return false
            }
        }

        // returns anyEnabled, anyDisabled
        fun GetExistingRuleInfos(): Pair<Boolean, Boolean> {
            var anyEnabled: Boolean = false;
            var anyDisabled: Boolean = false;
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    for (portMapping in device.portMappings) {
                        if (portMapping.Enabled) {
                            anyEnabled = true
                        } else {
                            anyDisabled = true
                        }

                        if (anyEnabled && anyDisabled) {
                            //exit early
                            return Pair(anyEnabled, anyDisabled)
                        }
                    }
                }
            }
            return Pair(anyEnabled, anyDisabled)
        }

        // returns anyEnabled, anyDisabled
        fun GetEnabledDisabledRules(enabledRules: Boolean): MutableList<PortMapping> {
            val enableDisabledPortMappings: MutableList<PortMapping> = mutableListOf();
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    for (portMapping in device.portMappings) {
                        if (portMapping.Enabled == enabledRules) {
                            enableDisabledPortMappings.add(portMapping)
                        }
                    }
                }
            }
            return enableDisabledPortMappings
        }

        fun UpdateSorting() {
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    val newMappings = TreeSet<PortMapping>(
                        SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending)
                    )
                    newMappings.addAll(device.portMappings)
                    for (pm in newMappings) {
                        device.lookUpExisting[pm.getKey()] = pm
                    }
                    device.portMappings = newMappings
                }
            }
        }

        fun GetAllRules(): MutableList<PortMapping> {
            val allRules: MutableList<PortMapping> = mutableListOf();
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    allRules.addAll(device.portMappings)
                }
            }
            return allRules
        }

        var IGDDevices: MutableList<IGDDevice> = mutableListOf()


        fun GetSpecificPortMappingRule(
            remoteIp: String,
            remoteHost: String,
            remotePort: String,
            protocol: String,
            callback: (UPnPCreateMappingResult) -> Unit
        )
                : Future<Any> {


            val device: IGDDevice = getIGDDevice(remoteIp)
            val action = device.actionsMap[ACTION_NAMES.GetSpecificPortMappingEntry]
            val actionInvocation = ActionInvocation(action)
            // this is actually remote host
            actionInvocation.setInput("NewRemoteHost", remoteHost)
            actionInvocation.setInput("NewExternalPort", remotePort)
            actionInvocation.setInput("NewProtocol", protocol)

            val future = UpnpManager.GetUPnPService().controlPoint.execute(object :
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
                        remotePort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt(),
                        remoteIp,
                        System.currentTimeMillis(),
                        GetPsuedoSlot()
                    ) // best bet for DateTime.UtcNow

                    PortForwardApplication.OurLogger.log(
                        Level.INFO,
                        "Successfully read back our new rule (${pm.shortName()})"
                    )

                    val result = UPnPCreateMappingResult(true, true)
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

                    val rule = formatShortName(protocol, remoteIp, remotePort)
                    PortForwardApplication.OurLogger.log(
                        Level.SEVERE,
                        "Failed to read back our new rule ($rule)."
                    )
                    PortForwardApplication.OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                    val result = UPnPCreateMappingResult(false, true)
                    result.UPnPFailureResponse = operation
                    result.FailureReason = defaultMsg
                    callback(result)
                }
            })


            return future
        }

        fun DisableEnablePortMappingEntry(
            portMapping: PortMapping,
            enable: Boolean,
            onDisableEnableCompleteCallback: (UPnPCreateMappingResult) -> Unit
        ): Future<Any>? {
            try
            {
                return DisableEnablePortMapping(portMapping, enable, onDisableEnableCompleteCallback)
            }
            catch (exception: Exception) {
                val enableDisableString = if(enable) "Enable" else "Disable"
                PortForwardApplication.OurLogger.log(
                    Level.SEVERE,
                    "$enableDisableString Port Mapping Failed: " + exception.message + exception.stackTraceToString()
                )
                MainActivity.showSnackBarViewLog("$enableDisableString Port Mapping Failed")
                //throw exception // this will crash the app
                return null
            }
        }

        fun DisableEnablePortMapping(
            portMapping: PortMapping,
            enable: Boolean,
            onDisableEnableCompleteCallback: (UPnPCreateMappingResult) -> Unit
        ): Future<Any> {
            // AddPortMapping
            //  This action creates a new port mapping or overwrites an existing mapping with the same internal client. If
            //  the ExternalPort and PortMappingProtocol pair is already mapped to another internal client, an error is
            //  returned.  (On my Nokia, I can modify other devices rules no problem).
            // However, deleting a mapping it is only a recommendation that they be the same..
            //  so Edit = Delete and Add is more powerful?


            val portMappingRequest = PortMappingRequest(
                portMapping.Description,
                portMapping.InternalIP,
                portMapping.InternalPort.toString(),
                portMapping.ActualExternalIP,
                portMapping.ExternalPort.toString(),
                portMapping.Protocol,
                portMapping.LeaseDuration.toString(),
                enable,
                portMapping.RemoteHost
            )
            return CreatePortMappingRule(
                portMappingRequest,
                true,
                getEnabledDisabledString(enable).lowercase(),
                onDisableEnableCompleteCallback
            )
        }

        fun getEnabledDisabledString(enabled: Boolean): String {
            return if (enabled) "enabled" else "disabled"
        }

        fun enableDisableDefaultCallback(result: UPnPCreateMappingResult) {
            println("adding rule callback")
            if (result.Success!!) {
                result.ResultingMapping!!
                val device =
                    UpnpManager.getIGDDevice(result.ResultingMapping!!.ActualExternalIP)
                device.addOrUpdate(result.ResultingMapping!!)
                invokeUpdateUIFromData()
            }
        }

        // DeletePortMappingRange is only available in v2.0 (also it can only delete
        //   a contiguous range)
        fun DeletePortMappingEntry(portMapping: PortMapping): Future<Any> {

                fun callback(result: UPnPResult) {

                    try {
                        defaultRuleDeletedCallback(result)
                        RunUIThread {
                            println("delete callback")
                            if (result.Success) {

                                MainActivity.showSnackBarShortNoAction("Success!")

                            } else {

                                MainActivity.showSnackBarViewLog("Failed to delete entry.")

                            }
                        }
                    } catch (exception: Exception) {
                        PortForwardApplication.OurLogger.log(
                            Level.SEVERE,
                            "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                        )
                        MainActivity.showSnackBarViewLog("Delete Port Mappings Failed")
                        throw exception
                    }
                }

                val future = DeletePortMapping(portMapping, ::callback)
                return future


        }

        fun DeletePortMappingsEntry(
            portMappings: List<PortMapping>,
            onCompleteBatchCallback: (MutableList<UPnPResult?>) -> Unit
        ): Future<List<PortMapping>> {
            val callable = Callable {

                try {
                    val listOfResults: MutableList<UPnPResult?> =
                        MutableList(portMappings.size) { null }

                    for (i in 0 until portMappings.size) {

                        fun callback(result: UPnPResult) {

                            defaultRuleDeletedCallback(result)
                            listOfResults[i] = result
                        }

                        println("Requesting Delete: $i")

                        val future = DeletePortMapping(portMappings[i], ::callback)
                        future.get()
                    }

                    onCompleteBatchCallback(listOfResults)

                    portMappings
                } catch (exception: Exception) {
                    PortForwardApplication.OurLogger.log(
                        Level.SEVERE,
                        "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                    )
                    MainActivity.showSnackBarViewLog("Delete Port Mappings Failed")
                    throw exception
                }
            }

            val task = FutureTask(callable)
            Thread(task).start()
            return task // a FutureTask is a Future
        }

        //TODO: if List all port mappings exists, that should be used instead of getgeneric.

        fun DeletePortMapping(
            portMapping: PortMapping,
            callback: (UPnPResult) -> Unit
        ): Future<Any> {
            val device: IGDDevice = getIGDDevice(portMapping.ActualExternalIP)
            val action = device.actionsMap[ACTION_NAMES.DeletePortMapping]
            val actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost", "${portMapping.RemoteHost}"); //!!
            // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
            actionInvocation.setInput("NewExternalPort", "${portMapping.ExternalPort}");
            actionInvocation.setInput("NewProtocol", "${portMapping.Protocol}");

            val future = UpnpManager.GetUPnPService().controlPoint.execute(object :
                ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    println("Successfully deleted")

                    PortForwardApplication.OurLogger.log(
                        Level.INFO,
                        "Successfully deleted rule (${portMapping.shortName()})."
                    )

                    val result = UPnPResult(true)
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

                    PortForwardApplication.OurLogger.log(
                        Level.SEVERE,
                        "Failed to delete rule (${portMapping.shortName()})."
                    )
                    PortForwardApplication.OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                    val result = UPnPResult(false)
                    result.FailureReason = defaultMsg
                    result.UPnPFailureResponse = operation
                    callback(result)
                }
            })

            return future

        }

        // this method creates a rule, then grabs it again to verify it.
        fun CreatePortMappingRule(
            portMappingRequest: PortMappingRequest,
            skipReadingBack: Boolean,
            createContext: String,
            callback: (UPnPCreateMappingResult) -> Unit
        ): Future<Any> {
            //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()

            val externalIp = portMappingRequest.externalIp

            val device: IGDDevice = getIGDDevice(externalIp)
            val action = device.actionsMap[ACTION_NAMES.AddPortMapping]
            val actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost", portMappingRequest.remoteHost);
            // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
            actionInvocation.setInput("NewExternalPort", portMappingRequest.externalPort);
            actionInvocation.setInput("NewProtocol", portMappingRequest.protocol);
            actionInvocation.setInput("NewInternalPort", portMappingRequest.internalPort);
            actionInvocation.setInput("NewInternalClient", portMappingRequest.internalIp);
            actionInvocation.setInput("NewEnabled", if (portMappingRequest.enabled) "1" else "0");
            actionInvocation.setInput("NewPortMappingDescription", portMappingRequest.description);
            actionInvocation.setInput("NewLeaseDuration", portMappingRequest.leaseDuration);

            val future = UpnpManager.GetUPnPService().controlPoint.execute(object :
                ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!

                    PortForwardApplication.OurLogger.log(
                        Level.INFO,
                        "Successfully $createContext rule (${
                            portMappingRequest.realize().shortName()
                        })."
                    )
                    println("Successfully added, now reading back")
                    if (skipReadingBack) {
                        val result = UPnPCreateMappingResult(true, false)
                        result.ResultingMapping = portMappingRequest.realize()
                        callback(result)
                    } else {
                        val specificFuture = GetSpecificPortMappingRule(
                            portMappingRequest.externalIp,
                            portMappingRequest.remoteHost,
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

                    PortForwardApplication.OurLogger.log(
                        Level.SEVERE,
                        "Failed to create rule (${portMappingRequest.realize().shortName()})."
                    )
                    PortForwardApplication.OurLogger.log(Level.SEVERE, "\t$defaultMsg")

                    val result = UPnPCreateMappingResult(false, false)
                    result.FailureReason = defaultMsg
                    result.UPnPFailureResponse = operation
                    callback(result)
                }
            })

            return future
        }

        public fun getIGDDevice(ipAddr: String): IGDDevice {
            return IGDDevices.first { it.ipAddress == ipAddr };
        }


        //fun List<IGDDevice>

        fun invokeUpdateUIFromData() {
            //UpnpManager.UpdateUIFromData.invoke(null)
            GlobalScope.launch {
                Log.i("", "tell ui to update collating")
                UpdateUIFromDataCollating.emit(null)
            }
        }

    }
}


fun defaultRuleAddedCallback(result : UPnPCreateMappingResult) {
    println("default adding rule callback")
    if (result.Success!!)
    {
        result.ResultingMapping!!
        val device =
            UpnpManager.getIGDDevice(result.ResultingMapping!!.ActualExternalIP)
        val firstRule = device.portMappings.isEmpty()
        device.addOrUpdate(result.ResultingMapping!!)
        if(firstRule)
        {
            // full refresh since we have to remove the old "no port mappings"
            UpnpManager.invokeUpdateUIFromData()
        }
        else
        {
            UpnpManager.PortAddedEvent.invoke(result.ResultingMapping!!)
        }
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
        val device =
            UpnpManager.getIGDDevice(result.RequestInfo!!.ActualExternalIP)
        device.removeMapping(result.RequestInfo!!)
        invokeUpdateUIFromData()
    }
}


data class PortMappingUserInput(val description : String, val internalIp : String, val internalRange : String, val externalIp : String, val externalRange : String, val protocol : String, val leaseDuration : String, val enabled : Boolean)
{
    fun with(internalPortSpecified : String, externalPortSpecified : String, portocolSpecified : String) : PortMappingRequest
    {
        return PortMappingRequest(description, internalIp, internalPortSpecified, externalIp, externalPortSpecified, portocolSpecified, leaseDuration, enabled, "")
    }

    fun validateRange() : String
    {
        // many 1 to 1 makes sense
        // many different external to 1 internal
        // 1 external to many internal //this isnt a thing and it doesnt make sense with upnp port retrieval

        val (inStart, inEnd) = getRange(true)
        val (outStart, outEnd) = getRange(false)

        val inSize = inEnd - inStart + 1 // inclusive
        val outSize = outEnd - outStart + 1
        val xToOne = (inSize == 1) // many out to 1 in
        if (inSize != outSize && !xToOne)
        {
            return "Internal and External Ranges do not match up."
        }
        else
        {
            return ""
        }
    }

    fun getRange(internal : Boolean) : Pair<Int, Int>
    {
        val rangeInQuestion = if(internal) internalRange else externalRange
        if(rangeInQuestion.contains('-'))
        {
            val inRange = rangeInQuestion.split('-')
            return Pair(inRange[0].toInt(), inRange[1].toInt())
        }
        else
        {
            return Pair(rangeInQuestion.toInt(), rangeInQuestion.toInt())
        }
    }

    fun getProtocols() : List<String>
    {
        return when (protocol) {
            Protocol.BOTH.str() -> listOf("TCP", "UDP")
            else -> listOf(protocol)
        }
    }
}
data class PortMappingRequest(val description : String, val internalIp : String, val internalPort : String, val externalIp : String, val externalPort : String, val protocol : String, val leaseDuration : String, val enabled : Boolean, val remoteHost : String)
{
    fun realize() : PortMapping
    {
        return PortMapping(description, remoteHost, internalIp, externalPort.toInt(), internalPort.toInt(), protocol, enabled, leaseDuration.toInt(), externalIp, System.currentTimeMillis(), GetPsuedoSlot())
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

class UPnPCreateMappingResult(success: Boolean, wasReadBack : Boolean) : UPnPResult(success)
{
    var ResultingMapping : PortMapping? = null
    var WasReadBack = false
    init{
        WasReadBack = wasReadBack
    }
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
            val ipAddress = wifiManager.connectionInfo.ipAddress

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
            val mappings : MutableMap<String, NetworkType> = mutableMapOf<String, NetworkType>()
            for (net1 in cm.getAllNetworks()) {
                val netInfo = cm.getNetworkInfo(net1)
                val name = cm.getLinkProperties(net1)?.interfaceName
                if(name == null)
                {
                    continue
                }
                val type = getNetworkType(cm, net1, netInfo)
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
            val isWifi = cm.activeNetworkInfo!!.type == ConnectivityManager.TYPE_WIFI
            if (isWifi) {
                PortForwardApplication.ShowToast("Is Connected Wifi", Toast.LENGTH_LONG)
            }

            val isData = cm.activeNetworkInfo!!.type == ConnectivityManager.TYPE_MOBILE
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


class IGDDevice constructor(_rootDevice : RemoteDevice?, _wanIPService : RemoteService?)
{

    var rootDevice : RemoteDevice?
    var wanIPService : RemoteService?
    var displayName : String //i.e. Nokia IGD Version 2.00
    var friendlyDetailsName : String //i.e. Internet Home Gateway Device
    var manufacturer : String //i.e. Nokia
    var ipAddress : String //i.e. 192.168.18.1
    var upnpType : String //i.e. InternetGatewayDevice
    var upnpTypeVersion : Int //i.e. 2
    var actionsMap : MutableMap<String, Action<RemoteService>>
    var portMappings : TreeSet<PortMapping> = TreeSet<PortMapping>(SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending))
    var lookUpExisting : MutableMap<Pair<Int, String>,PortMapping> = mutableMapOf()
//    var hasAddPortMappingAction : Boolean
//    var hasDeletePortMappingAction : Boolean

//    fun getMappingIndex(externalPort : Int, protocol : String) : Int
//    {
//        return portMappings.indexOfFirst { it.ExternalPort == externalPort && it.Protocol == protocol }
//    }

    init {
        this.rootDevice = _rootDevice
        this.wanIPService = _wanIPService

        if(_wanIPService != null && _rootDevice != null) // these are only nullable for unit test purposes
        {
            // nullrefs warning: this is SSDP response, very possible for some fields to be null, DeviceDetails constructor allows it.
            // the best bet on ip is (rootDevice!!.identity.descriptorURL.host) imo.  since that is what we use in RetreiveRemoteDescriptors class
            // which then calls remoteDeviceAdded (us). presentationURI can be and is null sometimes.

            this.displayName = this.rootDevice!!.displayString
            this.friendlyDetailsName = this.rootDevice!!.details.friendlyName
            this.ipAddress = rootDevice!!.identity.descriptorURL.host //this.rootDevice!!.details.presentationURI.host
            this.manufacturer = this.rootDevice!!.details.manufacturerDetails?.manufacturer ?: ""
            this.upnpType = this.rootDevice!!.type.type
            this.upnpTypeVersion = this.rootDevice!!.type.version
            this.actionsMap = mutableMapOf()
            for (action in _wanIPService.actions)
            {
                if(UpnpManager.ActionNames.contains(action.name))
                {
                    // are easy to call and parse output
                    actionsMap[action.name] = action
                }
            }
        }
        else
        {
            this.displayName = ""
            this.friendlyDetailsName = ""
            this.ipAddress = ""
            this.manufacturer = ""
            this.upnpType = ""
            this.upnpTypeVersion = 1
            this.actionsMap = mutableMapOf()
        }
    }



    fun EnumeratePortMappings()
    {
        // we enumerate port mappings later
        portMappings = TreeSet<PortMapping>(SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending))
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
            val getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry]!!
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



    private fun getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping : Action<RemoteService>) {
        var slotIndex : Int = 0;
        var retryCount : Int = 0
        while(true)
        {
            var shouldRetry : Boolean = false
            var success : Boolean = false;
            val actionInvocation = ActionInvocation(getPortMapping)
            println("requesting slot $slotIndex")
            actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
            val future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    retryCount = 0
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
                        remoteHost.toString(), //TODO standardize remote host so null == string.empty
                        internalClient.toString(),
                        externalPort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt(),
                        ipAddress,
                        System.currentTimeMillis(),
                        slotIndex)
                    addOrUpdate(portMapping)
                    UpnpManager.PortInitialFoundEvent.invoke(portMapping)
                    success = true
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    retryCount = 0
                    OurLogger.log(Level.INFO, "GetGenericPortMapping failed for entry $slotIndex: $defaultMsg.  NOTE: This is normal.")
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

    fun addOrUpdate(mapping : PortMapping)
    {
        synchronized(lockIgdDevices)
        {
            val key = mapping.getKey()
            if (this.lookUpExisting.containsKey(key)) {
                val existing = this.lookUpExisting[key]
                this.portMappings.remove(existing) //TODO

                if (existing != null && MainActivity.MultiSelectItems?.remove(existing) ?: false) {
                    MainActivity.MultiSelectItems!!.add(mapping)
                }
            }
            this.lookUpExisting[key] = mapping
            this.portMappings.add(mapping)
        }

    }

    fun removeMapping(mapping : PortMapping)
    {
        synchronized(lockIgdDevices)
        {
            this.lookUpExisting.remove(mapping.getKey())
            this.portMappings.remove(mapping)
            MainActivity.MultiSelectItems?.remove(mapping)
        }
    }
}

val MIN_PORT = 1 // 0 is invalid port. wildcard port i.e. let the system choose.
val MAX_PORT = 65535

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
    val _RemoteHost: String,
    val _LocalIP: String,
    val _ExternalPort: Int,
    val _InternalPort: Int,
    val _Protocol: String,
    val _Enabled: Boolean,
    val _LeaseDuration: Int,
    val _ActionExternalIP : String,
    val _timeReadLeaseDurationMs : Long,
    val _pseudoSlot : Int)
{
    // the returned ip from get port mapping
    // https://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v2-Service.pdf page 17
    // (this is an optional filter for another device on the network that is trying to reach you.)
    // "The NAT Traversal or port mapping functionality allows creation of mappings for both TCP and UDP
    //protocols between an external IGD port (called ExternalPort) and an internal client address associated with
    //one of its ports (respectively called InternalClient and InternalPort). It is also possible to narrow the
    //mapping by limiting the mapping to a specific remote host1"
    // (this means that the external ip is a given, it is the igd device)
    // "2.3.17 RemoteHost
    //This variable represents the source of inbound IP packets. This variable can contain a host name or a
    //standard IPv4 address representation. This state variable MUST be formatted as:
    //• a domain name of a network host like it is defined in [RFC 1035],
    //• or as a set of four decimal digit groups separated by "." as defined in [RFC 3986],
    //• or an empty string.
    //This will be a wildcard in most cases (an empty string). In version 2.0, NAT vendors are REQUIRED to
    //support non-wildcarded IP addresses in addition to wildcards. A non-wildcard value will allow for “narrow”
    //port mappings, which MAY be desirable in some usage scenarios. When RemoteHost is a wildcard, all
    //traffic sent to the ExternalPort on the WAN interface of the gateway is forwarded to the InternalClient on
    //the InternalPort (this corresponds to the endpoint independent filtering behaviour defined in the [RFC
    //4787]). When RemoteHost is specified as a specific external IP address as opposed to a wildcard, the NAT
    //will only forward inbound packets from this RemoteHost to the InternalClient. All other packets will
    //dropped (this corresponds to the address dependent filtering behaviour defined in [RFC 4787])."
    var RemoteHost : String = _RemoteHost
    // the actual ip of the IGD device
    var ActualExternalIP : String = _ActionExternalIP
    var InternalIP : String = _LocalIP
    var ExternalPort : Int = _ExternalPort
    var InternalPort : Int= _InternalPort
    var Protocol : String = _Protocol
    var Enabled : Boolean = _Enabled
    var LeaseDuration : Int  = _LeaseDuration
    var Description : String = _Description

    var TimeReadLeaseDurationMs : Long = _timeReadLeaseDurationMs
    var Slot : Int = _pseudoSlot

    fun getKey() : Pair<Int, String>
    {
        return Pair<Int, String>(this.ExternalPort, this.Protocol)
    }

    fun shortName() : String
    {
        return formatShortName(Protocol,ActualExternalIP,ExternalPort.toString())
    }

    fun getRemainingLeaseTime() : Int
    {
        val secondsPassed = (System.currentTimeMillis() - TimeReadLeaseDurationMs)/1000L
        val timeToExpiration = (LeaseDuration.toLong() - secondsPassed)
        return timeToExpiration.toInt()
    }



    fun getRemainingLeaseTimeString() : String
    {
        // show only 2 units (i.e. days and hours. or hours and minutes. or minutes and seconds. or just seconds)
        val totalSecs = getRemainingLeaseTime()

        val dhms = getDHMS(totalSecs)

        val hasDays = dhms.days >= 1
        val hasHours = dhms.hours >= 1
        val hasMinutes = dhms.mins >= 1
        val hasSeconds = dhms.seconds >= 1

        if (hasDays)
        {
            return "${dhms.days} day${_plural(dhms.days)}, ${dhms.hours} hour${_plural(dhms.hours)}"
        }
        else if(hasHours)
        {
            return "${dhms.hours} hour${_plural(dhms.hours)}, ${dhms.mins} minute${_plural(dhms.mins)}"
        }
        else if(hasMinutes)
        {
            return "${dhms.mins} minute${_plural(dhms.mins)}, ${dhms.seconds} second${_plural(dhms.seconds)}"
        }
        else if(hasSeconds)
        {
            return "${dhms.seconds} second${_plural(dhms.seconds)}"
        }
        else
        {
            return "Expired"
        }
    }

    fun _plural(value : Int) : String
    {
        return if (value > 1) "s" else ""
    }
}

data class DayHourMinSec(val days : Int, val hours : Int, val mins : Int, val seconds : Int)
{
    fun totalSeconds() : Int
    {
        return days * 3600 * 24 + hours * 3600 + mins * 60 + seconds
    }
}

fun getDHMS(totalSeconds : Int) : DayHourMinSec
{
    val days = totalSeconds / (24*3600)
    val hours = (totalSeconds % (24*3600)) / 3600
    val mins =  (totalSeconds % (3600)) / 60
    val secs = (totalSeconds % (60))
    return DayHourMinSec(days, hours, mins, secs)
}

fun formatShortName(protocol: String, externalIp: String, externalPort: String) : String
{
    return "$protocol rule at $externalIp:$externalPort"
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
        val addressFactory = AndroidNetworkAddressFactory(streamListenPort);

        // the final set of usable interfaces and bind addresses
        val iterator = addressFactory.networkInterfaces
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
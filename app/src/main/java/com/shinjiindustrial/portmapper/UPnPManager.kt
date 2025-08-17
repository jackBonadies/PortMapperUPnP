package com.shinjiindustrial.portmapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.UpnpManager.Companion.ACTION_NAMES
import com.shinjiindustrial.portmapper.UpnpManager.Companion.AddDevice
import com.shinjiindustrial.portmapper.UpnpManager.Companion.GetUPnPClient
import com.shinjiindustrial.portmapper.UpnpManager.Companion.UPnPNames
import com.shinjiindustrial.portmapper.UpnpManager.Companion._initialized
import com.shinjiindustrial.portmapper.UpnpManager.Companion._upnpService
import com.shinjiindustrial.portmapper.UpnpManager.Companion.invokeUpdateUIFromData
import com.shinjiindustrial.portmapper.client.IUpnpClient
import com.shinjiindustrial.portmapper.client.UpnpClient
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.formatShortName
import com.shinjiindustrial.portmapper.domain.getIGDDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.logging.Level
import kotlin.math.max


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
            UpdateUIFromDataCollating.conflate().onEach {
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
            GetUPnPClient().search(1)
            //launchMockUPnPSearch(this, upnpElementsViewModel)
            return true
        }

        fun ClearOldData() {
            GetUPnPClient().clearOldDevices()
            AnyIgdDevices?.value = false
            synchronized(lockIgdDevices)
            {
                IGDDevices.clear()
            }
            invokeUpdateUIFromData()
        }

        fun GetDeviceByExternalIp(gatewayIp: String): IGDDevice? {
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
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
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
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

        fun GetUPnPClient(): IUpnpClient {
            return upnpClient!!
        }

        var upnpClient : IUpnpClient? = null

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


                    val portMappingRequestRules = portMappingUserInput.splitIntoRules()
                    val listOfResults: MutableList<UPnPCreateMappingResult?> =
                        MutableList(portMappingRequestRules.size) { null }

                    for (i in 0 until portMappingRequestRules.size) {

                        fun callback(result: UPnPCreateMappingResult) {

                            defaultRuleAddedCallback(result)
                            listOfResults[i] = result
                        }

                        val future = CreatePortMappingRuleWrapper(
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
                        OurLogger.log(
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
                        OurLogger.log(
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


            // TODO
            _upnpService = UpnpServiceImpl(AndroidUpnpServiceConfigurationImpl(context))
            upnpClient = UpnpClient(_upnpService!!)
            GetUPnPClient().deviceFoundEvent += { device ->
                AddDevice(device)
                device.EnumeratePortMappings()
            }
            // initialization failed. no point in trying as even if we later get service, we
            //   do not re-intialize automatically
            FailedToInitialize = !GetUPnPClient().isInitialized()
            if (FailedToInitialize) {
                return false
            }

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
            DeviceFoundEvent.invoke(igdDevice)
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
            var anyEnabled: Boolean = false
            var anyDisabled: Boolean = false
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
            val enableDisabledPortMappings: MutableList<PortMapping> = mutableListOf()
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

        // TODO: we need a flow for just the IGD , port mappings that are available
        // we need a flow for the sorting method
        // autorenew can subscribe to the 1st
        // UI can subscribe to both

        fun GetAllRules(): MutableList<PortMapping> {
            val allRules: MutableList<PortMapping> = mutableListOf()
            synchronized(lockIgdDevices)
            {
                for (device in IGDDevices) {
                    allRules.addAll(device.portMappings)
                }
            }
            return allRules
        }

        var IGDDevices: MutableList<IGDDevice> = mutableListOf()


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
                OurLogger.log(
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
            return CreatePortMappingRuleWrapper(
                portMappingRequest,
                false,
                getEnabledDisabledString(enable).lowercase(),
                onDisableEnableCompleteCallback
            )
        }

        fun getEnabledDisabledString(enabled: Boolean): String {
            return if (enabled) "enabled" else "disabled"
        }

        // !!
        fun enableDisableDefaultCallback(result: UPnPCreateMappingResult) {
            println("adding rule callback")
            if (result.Success!!) {
                result.ResultingMapping!!
                val device =
                    getIGDDevice(result.ResultingMapping!!.ActualExternalIP)
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
                        OurLogger.log(
                            Level.SEVERE,
                            "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                        )
                        MainActivity.showSnackBarViewLog("Delete Port Mappings Failed")
                        throw exception
                    }
                }

            val device: IGDDevice = getIGDDevice(portMapping.ActualExternalIP)
            val future =  GetUPnPClient().deletePortMapping(device, portMapping, ::callback)
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

                        val device: IGDDevice = getIGDDevice(portMappings[i].ActualExternalIP)
                        val future = GetUPnPClient().deletePortMapping(device,portMappings[i], ::callback)
                        future.get()
                    }

                    onCompleteBatchCallback(listOfResults)

                    portMappings
                } catch (exception: Exception) {
                    OurLogger.log(
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



        // this method creates a rule, then grabs it again to verify it.
        fun CreatePortMappingRuleWrapper(
            portMappingRequest: PortMappingRequest,
            skipReadingBack: Boolean,
            createContext: String,
            callback: (UPnPCreateMappingResult) -> Unit
        ): Future<Any> {
            //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()

            val externalIp = portMappingRequest.externalIp
            val device: IGDDevice = getIGDDevice(externalIp)
            fun createCallback(createMappingResult: UPnPCreateMappingResult2) {

                if (createMappingResult.Success)
                {
                    OurLogger.log(
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
                        val specificFuture = GetUPnPClient().getSpecificPortMappingRule(
                            device,
                            portMappingRequest.remoteHost,
                            portMappingRequest.externalPort,
                            portMappingRequest.protocol,
                            callback
                        )
                        specificFuture.get()
                    }
                }
                else
                {
                    // TODO: 1 should derive from 2
                    val result = UPnPCreateMappingResult(false, false)
                    result.FailureReason = createMappingResult.FailureReason
                    result.RequestInfo = createMappingResult.RequestInfo
                    result.ResultingMapping = createMappingResult.RequestedMapping
                    result.UPnPFailureResponse = createMappingResult.UPnPFailureResponse
                    callback(result)
                }
            }
            var future = GetUPnPClient().createPortMappingRule(device, portMappingRequest, ::createCallback)
            return future
        }

        fun getIGDDevice(ipAddr: String): IGDDevice {
            return IGDDevices.first { it.ipAddress == ipAddr }
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
            invokeUpdateUIFromData()
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


data class PortMappingRequest(val description : String, val internalIp : String, val internalPort : String, val externalIp : String, val externalPort : String, val protocol : String, val leaseDuration : String, val enabled : Boolean, val remoteHost : String)
{
    fun realize() : PortMapping
    {
        return PortMapping(description, remoteHost, internalIp, externalPort.toInt(), internalPort.toInt(), protocol, enabled, leaseDuration.toInt(), externalIp, System.currentTimeMillis(), GetPsuedoSlot())
    }
}


//CompletableFuture is api >=24
//basic futures do not implement continuewith...

class UPnPCreateMappingResult2(success: Boolean, wasReadBack : Boolean) : UPnPResult(success)
{
    var RequestedMapping : PortMapping? = null
}

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
    constructor(portMapping : PortMapping, success: Boolean) : this(success) {
        ResultingMapping = portMapping
    }
    constructor(success : Boolean) : super(success)
    var ResultingMapping : PortMapping? = null
}

open class UPnPResult(success : Boolean)
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
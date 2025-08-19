package com.shinjiindustrial.portmapper

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.UpnpManager.Companion.invokeUpdateUIFromData
import com.shinjiindustrial.portmapper.client.IUpnpClient
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingResult
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPGetSpecificMappingResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.client.UpnpClient
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.model.message.UpnpResponse
import java.util.TreeSet
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.logging.Level


class UpnpManager {


    companion object { //singleton
        private var _upnpService: UpnpService? = null
        private var _initialized: Boolean = false
        var HasSearched: Boolean = false
        var AnyIgdDevices: MutableState<Boolean>? = null

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

        suspend fun CreatePortMappingRulesEntry(
            portMappingUserInput: PortMappingUserInput,
        ): List<UPnPCreateMappingWrapperResult> {

            try {
                val portMappingRequestRules = portMappingUserInput.splitIntoRules()

                return portMappingRequestRules.map { portMappingRequestRule ->

                    val result = CreatePortMappingRuleWrapper(
                        portMappingRequestRule,
                        false,
                        "created")
                    defaultRuleAddedCallback(result)
                    result
                }
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

        fun DisableEnablePortMappingEntries(
            portMappings: List<PortMapping>,
            enable: Boolean,
            onCompleteBatchCallback: (MutableList<UPnPCreateMappingWrapperResult?>) -> Unit
        ): FutureTask<List<PortMapping>> {

                val callable = Callable {

                    try {

                    val listOfResults: MutableList<UPnPCreateMappingWrapperResult?> =
                        MutableList(portMappings.size) { null }

                    for (i in 0 until portMappings.size) {

                        fun callback(result: UPnPCreateMappingWrapperResult) {

                            enableDisableDefaultCallback(result)
                            listOfResults[i] = result
                        }

                        // TODO
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

            // TODO
            _upnpService = UpnpServiceImpl(AndroidUpnpServiceConfigurationImpl(context))
            upnpClient = UpnpClient(_upnpService!!)
            GetUPnPClient().deviceFoundEvent += { device ->
                // this is on the cling thread
                AddDevice(device)
                runBlocking {
                    device.EnumeratePortMappings()
                }
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
            onDisableEnableCompleteCallback: (UPnPCreateMappingWrapperResult) -> Unit
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
            onDisableEnableCompleteCallback: (UPnPCreateMappingWrapperResult) -> Unit
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
//            Future.CompletedFuture(null)
//            return CreatePortMappingRuleWrapper(
//                portMappingRequest,
//                false,
//                getEnabledDisabledString(enable).lowercase(),
//                onDisableEnableCompleteCallback
//            )
//            val completedFuture = CompletableFuture.completedFuture("");
//            return completedFuture
            return DisableEnablePortMapping(portMapping, enable,onDisableEnableCompleteCallback)//TODO revert
        }

        fun getEnabledDisabledString(enabled: Boolean): String {
            return if (enabled) "enabled" else "disabled"
        }

        // !!
        fun enableDisableDefaultCallback(result: UPnPCreateMappingWrapperResult) {
            println("adding rule callback")
            if (result is UPnPCreateMappingWrapperResult.Success) {
                val device =
                    getIGDDevice(result.resultingMapping.ActualExternalIP)
                device.addOrUpdate(result.resultingMapping)
                invokeUpdateUIFromData()
            }
        }

        // DeletePortMappingRange is only available in v2.0 (also it can only delete
        //   a contiguous range)
        suspend fun DeletePortMappingEntry(portMapping: PortMapping) {

            val device: IGDDevice = getIGDDevice(portMapping.ActualExternalIP)
            val result = GetUPnPClient().deletePortMapping(device, portMapping)
            try {
                defaultRuleDeletedCallback(result)
                RunUIThread {
                    println("delete callback")
                    when (result) {
                        is UPnPResult.Success -> {
                            MainActivity.showSnackBarShortNoAction("Success!")
                        }
                        is UPnPResult.Failure -> {
                            MainActivity.showSnackBarViewLog("Failed to delete entry.")
                        }
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

        suspend fun DeletePortMappingsEntry(
            portMappings: List<PortMapping>,
        ): List<UPnPResult> {

            return portMappings.map { portMapping ->

                try {
                    println("Requesting Delete: ${portMapping.shortName()}")

                    val device: IGDDevice = getIGDDevice(portMapping.ActualExternalIP)
                    val result = GetUPnPClient().deletePortMapping(device,portMapping)
                    defaultRuleDeletedCallback(result)
                    result

                } catch (exception: Exception) {
                    OurLogger.log(
                        Level.SEVERE,
                        "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                    )
                    MainActivity.showSnackBarViewLog("Delete Port Mappings Failed")
                    throw exception
                }
            }
        }

        //TODO: if List all port mappings exists, that should be used instead of getgeneric.



        // this method creates a rule, then grabs it again to verify it.
        suspend fun CreatePortMappingRuleWrapper(
            portMappingRequest: PortMappingRequest,
            skipReadingBack: Boolean,
            createContext: String,
        ) : UPnPCreateMappingWrapperResult {
            //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()

            val externalIp = portMappingRequest.externalIp
            val device: IGDDevice = getIGDDevice(externalIp)
            val createMappingResult = GetUPnPClient().createPortMappingRule(device, portMappingRequest)
            when (createMappingResult)
            {
                is UPnPCreateMappingResult.Success ->
                {
                    OurLogger.log(
                        Level.INFO,
                        "Successfully $createContext rule (${
                            portMappingRequest.realize().shortName()
                        })."
                    )
                    println("Successfully added, now reading back")
                    if (skipReadingBack) {
                        val result = UPnPCreateMappingWrapperResult.Success(portMappingRequest.realize(), portMappingRequest.realize(), false)
                        return result
                    } else {
                        val result = GetUPnPClient().getSpecificPortMappingRule(
                            device,
                            portMappingRequest.remoteHost,
                            portMappingRequest.externalPort,
                            portMappingRequest.protocol,
                        )
                        return result
                    }
                }
                is UPnPCreateMappingResult.Failure ->
                {
                    val result = UPnPCreateMappingWrapperResult.Failure(createMappingResult.reason, createMappingResult.response)
                    return result
                }
            }
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


fun defaultRuleAddedCallback(result : UPnPCreateMappingWrapperResult) {
    println("default adding rule callback")
    if (result is UPnPCreateMappingWrapperResult.Success)
    {
        val device =
            UpnpManager.getIGDDevice(result.resultingMapping.ActualExternalIP)
        val firstRule = device.portMappings.isEmpty()
        device.addOrUpdate(result.resultingMapping)
        if(firstRule)
        {
            // full refresh since we have to remove the old "no port mappings"
            invokeUpdateUIFromData()
        }
        else
        {
            UpnpManager.PortAddedEvent.invoke(result.resultingMapping)
        }
    }
}

fun defaultRuleDeletedCallback(result : UPnPResult) {
    println("default adding rule callback")
    if (result is UPnPResult.Success)
    {
        val device =
            UpnpManager.getIGDDevice(result.requestInfo.ActualExternalIP)
        device.removeMapping(result.requestInfo)
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



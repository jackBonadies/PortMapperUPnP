package com.shinjiindustrial.portmapper

import android.content.Context
import android.util.Log
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.client.IUpnpClient
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingResult
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPGetSpecificMappingResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.ACTION_NAMES
import com.shinjiindustrial.portmapper.domain.AndroidUpnpServiceConfigurationImpl
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.UpnpServiceImpl
import java.util.TreeSet
import java.util.logging.Level
import javax.inject.Inject
import kotlin.collections.map
import kotlin.collections.remove
import kotlin.collections.set
import kotlin.system.measureTimeMillis


class UpnpManager {

    companion object { //singleton

        lateinit var upnpClient : IUpnpClient

        // region data

        private var _initialized: Boolean = false
        var HasSearched: Boolean = false
        var FailedToInitialize: Boolean = false

        private val _devices = MutableStateFlow(listOf<IGDDevice>())  // TreeSet<PortMapping>
        val devices : StateFlow<List<IGDDevice>> =
            _devices//.map { it..sortedBy { d -> d.name } }
                //.stateIn(MainScope(), SharingStarted.Eagerly, emptyList())
        val anyDevices : Flow<Boolean> = _devices.map { !it.isEmpty() }

//        private val cmp = compareBy<PortMapping>({ it.ActualExternalIP }, { it.InternalPort })
        private var portMappingLookup : MutableMap<Pair<Int, String>,PortMapping> = mutableMapOf()

        // I think this should be a local var inside mutable state flow
        private var portMappingsSortedSource : TreeSet<PortMapping> = TreeSet<PortMapping>(SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending))
        private val _portMappings = MutableStateFlow(portMappingsSortedSource)  // TreeSet<PortMapping>
        val portMappings : StateFlow<Set<PortMapping>> = _portMappings
//
//        fun update(pm: PortMapping) = _setFlow.update { old ->
//            TreeSet(old).apply { add(pm) }
//        }
        fun add(device: IGDDevice)
        {
            // list is sorted
            _devices.update { curList ->
                buildList {
                    addAll(curList)
                    var index = 0
                    for (i in 0..size-1)
                    {
                        if (curList[i].ipAddress > device.ipAddress)
                        {
                            break
                        }
                        index++
                    }
                    add(index, device)
                }
            }
        }
        
        fun clearDevices()
        {
            _devices.update { listOf<IGDDevice>() }
        }

        fun addOrUpdateMapping(pm: PortMapping)
        {
            var existingRule : PortMapping? = null
            val key = pm.getKey()
            _portMappings.update { old ->
                if (portMappingLookup.containsKey(key)) {
                    existingRule = this.portMappingLookup[key]
                    old.remove(existingRule)
                }
                portMappingLookup[pm.getKey()] = pm
                TreeSet(old).apply { add(pm) }
            }

            if (existingRule != null && MainActivity.MultiSelectItems?.remove(existingRule) ?: false) {
                MainActivity.MultiSelectItems!!.add(pm)
            }
        }

        fun removeMapping(pm: PortMapping)
        {
            _portMappings.update { old ->
                portMappingLookup.remove(pm.getKey())
                TreeSet(old).apply { remove(pm) }
            }
            MainActivity.MultiSelectItems?.remove(pm)
        }

        //
        fun UpdateSorting() {
            _portMappings.update { old ->
                val newMappings = TreeSet<PortMapping>(
                    SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending)
                )
                newMappings.addAll(old)
                newMappings
            }
        }

        // endregion

        // region datagetters

        fun getIGDDevice(ipAddr: String): IGDDevice {
            return devices.value.first { it.ipAddress == ipAddr }
        }

        // returns anyEnabled, anyDisabled
        fun GetEnabledDisabledRules(enabledRules: Boolean): MutableList<PortMapping> {
            return portMappings.value.filter { it.Enabled == enabledRules }.toMutableList()
        }

        // TODO: we need a flow for just the IGD , port mappings that are available
        // we need a flow for the sorting method
        // autorenew can subscribe to the 1st
        // UI can subscribe to both

        fun GetAllRules(): MutableList<PortMapping> {
            return portMappings.value.toMutableList()
        }

        fun GetGatewayIpsWithDefault(deviceGateway: String): Pair<MutableList<String>, String> {
            val gatewayIps: MutableList<String> = mutableListOf()
            var defaultGatewayIp = ""
            synchronized(lockIgdDevices)
            {
                for (device in devices.value) {
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

        var lockIgdDevices = Any()

        // returns anyEnabled, anyDisabled
        fun GetExistingRuleInfos(): Pair<Boolean, Boolean> {
            var anyEnabled: Boolean = portMappings.value.any { pm -> pm.Enabled }
            var anyDisabled: Boolean = portMappings.value.any { pm -> !pm.Enabled }
            return Pair(anyEnabled, anyDisabled)
        }

        // endregion


        // region uievents

        var DeviceFoundEvent = Event<IGDDevice>()
        var PortAddedEvent = Event<PortMapping>()
        var PortInitialFoundEvent = Event<PortMapping>()

        // used if we finish and there are no ports to show the "no devices" card
        var FinishedListingPortsEvent = Event<IGDDevice>()
        var UpdateUIFromData = Event<Any?>()
        var SearchStarted = Event<Any?>()

        // endregion

        // region search

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

        // endregion

        fun GetUPnPClient(): IUpnpClient {
            return upnpClient
        }


        fun FullRefresh() {
            Initialize(PortForwardApplication.appContext, true)
            Search(false)
        }

        fun Initialize(context: Context, force: Boolean): Boolean {
            if (_initialized && !force) {
                return true
            }

            GetUPnPClient().instantiateAndBindUpnpService()
            // TODO need to unsub?
            GetUPnPClient().deviceFoundEvent += { device ->
                // this is on the cling thread
                AddDevice(device)
                runBlocking {
                    EnumeratePortMappings(device.ipAddress)
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

        //region actions

        // create new rule with enabled set, and update data
        suspend fun DisableEnablePortMappingEntry(
            portMapping: PortMapping,
            enable: Boolean,
        ): UPnPCreateMappingWrapperResult {
            // AddPortMapping
            //  This action creates a new port mapping or overwrites an existing mapping with the same internal client. If
            //  the ExternalPort and PortMappingProtocol pair is already mapped to another internal client, an error is
            //  returned.  (On my Nokia, I can modify other devices rules no problem).
            // However, deleting a mapping it is only a recommendation that they be the same..
            //  so Edit = Delete and Add is more powerful?
            try
            {
                val portMappingRequest = PortMappingRequest.from(portMapping).copy(enabled = enable)
                val res = CreatePortMappingRuleWrapper(
                    portMappingRequest,
                    false,
                    getEnabledDisabledString(enable).lowercase(),
                )
                updateMappingIfSuccessful(res)
                return res
            }
            catch (exception: Exception) {
                val enableDisableString = if(enable) "Enable" else "Disable"
                OurLogger.log(
                    Level.SEVERE,
                    "$enableDisableString Port Mapping Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }

        suspend fun RenewRules(portMappings : List<PortMapping>)
                : List<UPnPCreateMappingWrapperResult>
        {
            return portMappings.map { portMapping ->

                try {
                    val portMappingRequest = PortMappingRequest.from(portMapping)
                    val res = CreatePortMappingRuleWrapper(
                        portMappingRequest,
                        false,
                        "renewed",
                    )
                    updateMappingIfSuccessful(res)
                    res
                } catch (exception: Exception) {
                    OurLogger.log(
                        Level.SEVERE,
                        "Renew Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                    )
                    throw exception
                }
            }
        }

        suspend fun RenewRule(
            portMapping: PortMapping,
        ): UPnPCreateMappingWrapperResult {
            try {
                val portMappingRequest = PortMappingRequest.from(portMapping)
                val res = CreatePortMappingRuleWrapper(
                    portMappingRequest,
                    false,
                    "renewed",
                )
                updateMappingIfSuccessful(res)
                return res
            } catch (exception: Exception) {
                OurLogger.log(
                    Level.SEVERE,
                    "Renew Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }


        // DeletePortMappingRange is only available in v2.0 (also it can only delete
        //   a contiguous range)
        suspend fun DeletePortMappingEntry(portMapping: PortMapping) : UPnPResult {

            try {
                val device: IGDDevice = getIGDDevice(portMapping.ActualExternalIP)
                val result = GetUPnPClient().deletePortMapping(device, portMapping)
                removeRuleIfSuccessful(result)
                return result
            } catch (exception: Exception) {
                OurLogger.log(
                    Level.SEVERE,
                    "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
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
                    removeRuleIfSuccessful(result)
                    result

                } catch (exception: Exception) {
                    OurLogger.log(
                        Level.SEVERE,
                        "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                    )
                    throw exception
                }
            }
        }


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
                    addRuleIfSuccessful(result)
                    result
                }
            }
            catch(exception : Exception)
            {
                OurLogger.log(
                    Level.SEVERE,
                    "Create Rule Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }

        suspend fun DisableEnablePortMappingEntries(
            portMappings: List<PortMapping>,
            enable: Boolean,
        ): List<UPnPCreateMappingWrapperResult> {

            return portMappings.map { portMapping ->

                try {
                    val portMappingRequest = PortMappingRequest.from(portMapping).copy(enabled = enable)
                    val res = CreatePortMappingRuleWrapper(
                        portMappingRequest,
                        false,
                        getEnabledDisabledString(enable).lowercase(),
                    )
                    updateMappingIfSuccessful(res)
                    res
                } catch (exception: Exception) {

                    val enableDisableString = if (enable) "Enable" else "Disable"
                    OurLogger.log(
                        Level.SEVERE,
                        "$enableDisableString Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                    )
                    throw exception
                }
            }
        }

        //TODO: if List all port mappings exists, that should be used instead of getgeneric.

        // this method creates a rule, then grabs it again to verify it.
        private suspend fun CreatePortMappingRuleWrapper(
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


        suspend fun EnumeratePortMappings(externalIp : String)
        {
            // we enumerate port mappings later
            val device: IGDDevice = getIGDDevice(externalIp)
            val timeTakenMillis = measureTimeMillis {

//        if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings))
//        {
//            OurLogger.log(Level.INFO, "Enumerating Port Listings using GetListOfPortMappings")
//            var getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings]!!
//            getAllPortMappingsUsingListPortMappings(getPortMapping)
//        }
                if(device.actionsMap.containsKey(ACTION_NAMES.GetGenericPortMappingEntry))
                {
                    OurLogger.log(Level.INFO, "Enumerating Port Listings using GetGenericPortMappingEntry")
                    getAllPortMappingsUsingGenericPortMappingEntry(device)
                }
                else{
                    //TODO firebase integration
                    OurLogger.log(Level.SEVERE, "device does not have GetGenericPortMappingEntry")
                }

            }
            OurLogger.log(Level.INFO, "Time to enumerate ports: $timeTakenMillis ms")
        }

        // Had previously tried GetListOfPortMappings but it would encounter error more than 100 ports
        // TODO I dont like that this is here but the other classes that interact with Client are in UPnp manager
        private suspend fun getAllPortMappingsUsingGenericPortMappingEntry(device : IGDDevice) {
            var slotIndex : Int = 0;
            var retryCount : Int = 0
            while(true)
            {
                var shouldRetry : Boolean = false
                var success : Boolean = false;
                try {
                    //future.get() // SYNCHRONOUS (note this can, and does, throw)
                    val result = GetUPnPClient().getGenericPortMappingRule(device, slotIndex)
                    when (result) {
                        is UPnPGetSpecificMappingResult.Success -> {
                            val portMapping = result.resultingMapping
                            addOrUpdateMapping(portMapping) //!!
                            success = true
                            OurLogger.log(Level.INFO, portMapping.toStringFull())
                            retryCount = 0

                        }
                        is UPnPGetSpecificMappingResult.Failure -> {
                            OurLogger.log(Level.INFO, "GetGenericPortMapping failed for entry $slotIndex: ${result.reason}.  NOTE: This is normal.")
                        }
                    }
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

        // endregion


        //fun List<IGDDevice>


        private fun getEnabledDisabledString(enabled: Boolean): String {
            return if (enabled) "enabled" else "disabled"
        }

        //region DataUpdate

        fun ClearOldData() {
            GetUPnPClient().clearOldDevices()
            clearDevices()
        }

        private fun updateMappingIfSuccessful(result: UPnPCreateMappingWrapperResult) {
            println("adding rule callback")
            if (result is UPnPCreateMappingWrapperResult.Success) {
                addOrUpdateMapping(result.resultingMapping)
            }
        }

        private fun addRuleIfSuccessful(result : UPnPCreateMappingWrapperResult) {
            println("default adding rule callback")
            if (result is UPnPCreateMappingWrapperResult.Success)
            {
                addOrUpdateMapping(result.resultingMapping)
            }
        }

        private fun removeRuleIfSuccessful(result : UPnPResult) {
            println("default adding rule callback")
            if (result is UPnPResult.Success)
            {
                removeMapping(result.requestInfo)
            }
        }

        fun AddDevice(igdDevice: IGDDevice) {
            add(igdDevice)
            Log.i("portmapperUI", "IGD device added")
            OurLogger.log(
                Level.INFO,
                "Added Device ${igdDevice.displayName} at ${igdDevice.ipAddress}."
            )
        }

        //endregion

    }
}

data class PortMappingRequest(val description : String, val internalIp : String, val internalPort : String, val externalIp : String, val externalPort : String, val protocol : String, val leaseDuration : String, val enabled : Boolean, val remoteHost : String)
{
    fun realize() : PortMapping
    {
        return PortMapping(description, remoteHost, internalIp, externalPort.toInt(), internalPort.toInt(), protocol, enabled, leaseDuration.toInt(), externalIp, System.currentTimeMillis(), GetPsuedoSlot())
    }

    companion object {
        fun from(portMapping: PortMapping) : PortMappingRequest
        {
            return PortMappingRequest(portMapping.Description, portMapping.InternalIP, portMapping.InternalPort.toString(), portMapping.ActualExternalIP, portMapping.ExternalPort.toString(), portMapping.Protocol, portMapping.LeaseDuration.toString(), portMapping.Enabled, portMapping.RemoteHost)
        }
    }
}

//CompletableFuture is api >=24
//basic futures do not implement continuewith...



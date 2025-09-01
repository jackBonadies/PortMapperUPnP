package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.IUpnpClient
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingResult
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPGetGenericMappingResult
import com.shinjiindustrial.portmapper.client.UPnPGetSpecificMappingResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.ACTION_NAMES
import com.shinjiindustrial.portmapper.domain.DevicePreferences
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingPref
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.domain.formatShortName
import com.shinjiindustrial.portmapper.domain.getPrefs
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.DevicesEntity
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class UpnpManager @Inject constructor(
    private val upnpClient: IUpnpClient,
    private val portMappingDao: PortMappingDao,
    private val devicesDao: DevicesDao,
    private val ourLogger: ILogger,
    @ApplicationScope private val applicationContext: CoroutineScope
) {
    private val renewMutex = Mutex()
    private var counter = 1

    private var _initialized: Boolean = false
    var hasSearched: Boolean = false
    var failedToInitialize: Boolean = false

    private val _devices = MutableStateFlow(listOf<IIGDDevice>())  // TreeSet<PortMapping>
    val devices: StateFlow<List<IIGDDevice>> =
        _devices//.map { it..sortedBy { d -> d.name } }

    val anyDevices: Flow<Boolean> = _devices.map { !it.isEmpty() }

    //        private val cmp = compareBy<PortMapping>({ it.ActualExternalIP }, { it.InternalPort })
    private var _portMappings: MutableStateFlow<Map<PortMappingKey, PortMappingWithPref>> =
        MutableStateFlow(mapOf())

    // I think this should be a local var inside mutable state flow
    val portMappings: StateFlow<Map<PortMappingKey, PortMappingWithPref>> = _portMappings

    init {
        upnpClient.deviceFoundEvent += { device ->
            // this is on the cling thread
            if (_devices.value.any { it.getIpAddress() == device.deviceDetails.ipAddress }) {
                ourLogger.log(
                    Level.WARNING,
                    "device ${device.deviceDetails.udn} came in again at ${device.deviceDetails.ipAddress}. ignoring."
                )
            } else {

                val devicePreferencesNullable = runBlocking {
                    devicesDao.getByPrimaryKey(device.deviceDetails.ipAddress, device.deviceDetails.udn)
                }
                if (devicePreferencesNullable == null) {
                    ourLogger.log(Level.INFO, "Preferences not found for device")
                } else {
                    ourLogger.log(Level.INFO, "Preferences found for device")
                }
                val igdDevice =
                    IGDDevice(device.deviceDetails,
                        devicePreferencesNullable?.getPrefs() ?: DevicePreferences(),
                        device.remoteService)
                addDevice(igdDevice)//TODO test with suspend
                runBlocking {
                    enumeratePortMappings(igdDevice.getIpAddress())
                }
            }
        }
        //subscribeForAutoRenew()
        //subscribeForAutoRenewCancelTimerNotWork()
        subscribeForAutoRenewCancelTimerNotWork()
    }

    fun subscribeForAutoRenewCancelTimerNotWork() {
        // too early for app scope
        applicationContext.launch {
            portMappings.mapNotNull { it ->
                it.filter { it.value.getAutoRenewOrDefault() }
                    .minByOrNull { it.value.portMapping.getExpiresTimeMillis() }
            }.onEach {
                // null check
                    it ->
                println("running with ${it.value.portMapping.shortName()}")
            }.collectLatest {
                delayUntilExpiryBuffer(
                    (it.value.portMapping.getExpiresTimeMillis()),
                    PortForwardApplication.RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING
                )
                println("delay is over launching")
                withContext(NonCancellable)
                {
                    performBatchRenew()
                }
            }
        }
    }

    private suspend fun performBatchRenew() = renewMutex.withLock {
        println("renewing all rules")
        val snapshot = portMappings.first()

        val now = SystemClock.elapsedRealtime()
        val expiring =
            snapshot.filter { it.value.portMapping.getExpiresTimeMillis() - now <= 1000 * (PortForwardApplication.RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING + PortForwardApplication.RENEW_BATCH_WITHIN_X_SECONDS) }
        if (expiring.isEmpty()) {
            println("batch is empty")
            return
        }
        expiring.forEach {
            println("our batch $counter: ${it.value.portMapping.shortName()}")
        }
        counter++

        expiring.forEach {
            println("renewed ${it.value.portMapping.shortName()}")
            try {
                renewRule(it.value)
            } catch (exception: Exception) {
                // we already logged just continue for now (TODO)
            }
            println("updated ${it.value.portMapping.shortName()}")
        }
        //delay(1000) // just to prove that we do not cancel
        println("end renewing all rules")
    }

    private suspend fun delayUntilExpiryBuffer(
        expirationTime: Long,
        renewWithinXSecondsOfExpiring: Long
    ) {
        val delayMilliseconds =
            (expirationTime - renewWithinXSecondsOfExpiring * 1000 - SystemClock.elapsedRealtime()).coerceAtLeast(
                0L
            )
        println("wait for $delayMilliseconds ms")
        delay(delayMilliseconds)
    }

//    private fun delayUntilExpiryBufferThenEmit(portMapping: PortMappingWithPref, expirationTime: Long, renewWithinXSecondsOfExpiring: Long = 45L): Flow<Unit> = flow {
//        val delaySeconds = (expirationTime - renewWithinXSecondsOfExpiring).coerceAtLeast(0L)
//        println("wait for $delaySeconds seconds")
//        delay(delaySeconds * 1000)
//        emit(portMapping)
//    }
//
//    @OptIn(ExperimentalCoroutinesApi::class)
//    private fun subscribeForAutoRenew()
//    {
//        val ruleClosestToExpirationFlow = portMappings.mapNotNull { items ->
//            items
//                .filter { it.getAutoRenewOrDefault() }
//                .minByOrNull { it.portMapping.getExpiresTimeMillis() }
//                ?.let { pm -> pm to pm.portMapping.getExpiresTimeMillis() }
//        }.onEach { it -> println("Rule Closest to Expiration is ${it.first.portMapping.shortName()}") }
//        val ruleClosestToExpirationDoNotEmitIfSameRuleSameTime = ruleClosestToExpirationFlow.distinctUntilChanged { oldUser, newUser ->
//            oldUser.first.portMapping === newUser.first.portMapping && oldUser.second == newUser.second
//        }.onEach { it -> println("UPDATE: Rule Closest to Expiration is ${it.first.portMapping.shortName()}") }
//        val emitOnRenewTime = ruleClosestToExpirationDoNotEmitIfSameRuleSameTime.flatMapLatest { it ->
//            delayUntilExpiryBufferThenEmit(it.first, it.first.portMapping.getExpiresTimeMillis())
//            // this is a problem if we are done but we edited or deleted the rule in the meantime. i.e. we will get a rule created that we did not want.
//            // get next time to renew
//        }
//
//        ruleClosestToExpirationDoNotEmitIfSameRuleSameTime.onEach
//                portMappings.map { it ->
//            val minPortMappingOrNull = it.minByOrNull { if(it.getAutoRenewOrDefault()) it.portMapping.getExpiresTimeMillis() else Long.MAX_VALUE }
//            if (minPortMappingOrNull == null) {
//                null
//            }else {
//                Pair<PortMappingWithPref, Long>(
//                    minPortMappingOrNull,
//                    minPortMappingOrNull.portMapping.getExpiresTimeMillis()
//                )
//            }
//        }.filter(it -> it != null)
//    }

    // region data


    //
//        fun update(pm: PortMapping) = _setFlow.update { old ->
//            TreeSet(old).apply { add(pm) }
//        }
    fun add(device: IIGDDevice) {
        // list is sorted
        if (_devices.value.any { it.getIpAddress() == device.getIpAddress() }) {
            return
        }
        _devices.update { curList ->
            buildList {
                addAll(curList)
                var index = 0
                for (i in 0..size - 1) {
                    if (curList[i].getIpAddress() > device.getIpAddress()) {
                        break
                    }
                    index++
                }
                add(index, device)
            }
        }
    }


    private fun addOrUpdateMapping(pm: PortMappingWithPref) {
        _portMappings.update { old ->
            old + (pm.getKey() to pm)
        }
    }

    fun removeMapping(pm: PortMappingWithPref) {
        _portMappings.update { old ->
            old - pm.getKey()
        }
    }

    // endregion

    // region datagetters

    fun getIGDDevice(ipAddr: String): IIGDDevice {
        return devices.value.first { it.getIpAddress() == ipAddr }
    }

    // returns anyEnabled, anyDisabled
    fun getEnabledDisabledRules(enabledRules: Boolean): MutableList<PortMappingWithPref> {
        return portMappings.value.filter { it.value.portMapping.Enabled == enabledRules }.values.toMutableList()
    }

    // TODO: we need a flow for just the IGD , port mappings that are available
    // we need a flow for the sorting method
    // autorenew can subscribe to the 1st
    // UI can subscribe to both

    fun getAllRules(): MutableList<PortMappingWithPref> {
        return portMappings.value.values.toMutableList()
    }

    fun getGatewayIpsWithDefault(deviceGateway: String): Pair<MutableList<String>, String> {
        val gatewayIps: MutableList<String> = mutableListOf()
        var defaultGatewayIp = ""
        synchronized(lockIgdDevices)
        {
            for (device in devices.value) {
                gatewayIps.add(device.getIpAddress())
                if (device.getIpAddress() == deviceGateway) {
                    defaultGatewayIp = device.getIpAddress()
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
        val anyEnabled: Boolean = portMappings.value.any { pm -> pm.value.portMapping.Enabled }
        val anyDisabled: Boolean = portMappings.value.any { pm -> !pm.value.portMapping.Enabled }
        return Pair(anyEnabled, anyDisabled)
    }

    // endregion


    // region uievents


    // used if we finish and there are no ports to show the "no devices" card
    var PortInitialFoundEvent = Event<PortMapping>()
    var FinishedListingPortsEvent = Event<IGDDevice>()
    var UpdateUIFromData = Event<Any?>()
    var SearchStarted = Event<Any?>()

    // endregion

    // region search

    fun search(onlyIfNotYetSearched: Boolean): Boolean {
        if (onlyIfNotYetSearched && hasSearched) {
            return false
        }
        SearchStarted.invoke(null)
        clearOldData()
        //NetworkInfoAtTimeOfSearch = OurNetworkInfo.GetNetworkInfo(PortForwardApplication.appContext, true)
        hasSearched = true
        // can do urn:schemas-upnp-org:device:{deviceType}:{ver}
        // 0-1 second response time intentional delay from devices
        upnpClient.search(1)
        //launchMockUPnPSearch(this, upnpElementsViewModel)
        return true
    }

    // endregion

    fun GetUPnPClient(): IUpnpClient {
        return upnpClient
    }


    fun fullRefresh() {
        initialize(true)
        search(false)
    }

    fun initialize(force: Boolean): Boolean {
        if (_initialized && !force) {
            return true
        }

        upnpClient.instantiateAndBindUpnpService()

        // initialization failed. no point in trying as even if we later get service, we
        //   do not re-intialize automatically
        failedToInitialize = !upnpClient.isInitialized()
        if (failedToInitialize) {
            return false
        }

        _initialized = true
        return true
    }

    //region actions

    // create new rule with enabled set, and update data
    suspend fun disableEnablePortMappingEntry(
        portMapping: PortMappingWithPref,
        enable: Boolean,
    ): UPnPCreateMappingWrapperResult {
        // AddPortMapping
        //  This action creates a new port mapping or overwrites an existing mapping with the same internal client. If
        //  the ExternalPort and PortMappingProtocol pair is already mapped to another internal client, an error is
        //  returned.  (On my Nokia, I can modify other devices rules no problem).
        // However, deleting a mapping it is only a recommendation that they be the same..
        //  so Edit = Delete and Add is more powerful?
        try {
            val portMappingRequest = PortMappingRequest.from(portMapping).copy(enabled = enable)
            val res = createPortMappingRuleWrapper(
                portMappingRequest,
                false,
                getEnabledDisabledString(enable).lowercase(),
            )
            addOrUpdateForEnabledDisabled(res, portMapping)
            // this is to cause an update
            return res
        } catch (exception: Exception) {
            val enableDisableString = if (enable) "Enable" else "Disable"
            ourLogger.log(
                Level.SEVERE,
                "$enableDisableString Port Mapping Failed: " + exception.message + exception.stackTraceToString()
            )
            throw exception
        }
    }

    suspend fun renewRules(portMappings: List<PortMappingWithPref>)
            : List<UPnPCreateMappingWrapperResult> {
        return portMappings.map { portMapping ->

            try {
                val portMappingRequest = PortMappingRequest.from(portMapping)
                val res = createPortMappingRuleWrapper(
                    portMappingRequest,
                    false,
                    "renewed",
                )
                addOrUpdateForRenew(res, portMapping)
                res
            } catch (exception: Exception) {
                ourLogger.log(
                    Level.SEVERE,
                    "Renew Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }
    }

    suspend fun renewRule(
        portMapping: PortMappingWithPref,
    ): UPnPCreateMappingWrapperResult {
        try {
            val portMappingRequest = PortMappingRequest.from(portMapping)
            val res = createPortMappingRuleWrapper(
                portMappingRequest,
                false,
                "renewed",
            )
            // this is so it "refreshes"
            addOrUpdateForRenew(res, portMapping)
            return res
        } catch (exception: Exception) {
            ourLogger.log(
                Level.SEVERE,
                "Renew Port Mappings Failed: " + exception.message + exception.stackTraceToString()
            )
            throw exception
        }
    }

    suspend fun deletePortMappingWithFallback(device: IIGDDevice, portMapping : PortMapping) : UPnPResult {
        // if set to something special then proceed as normal
        if(!device.devicePreferences.isBlankOrStar(portMapping.RemoteHost))
        {
            return upnpClient.deletePortMapping(device, portMapping)
        }

        val result = upnpClient.deletePortMapping(device,
            portMapping.copy(RemoteHost = device.devicePreferences.getDefaultWildcard()))
        if (result is UPnPResult.Failure)
        {
            if (result.details.response != null && result.details.response.statusCode == 500)
            {
                // add breadcrumb? no only on another failure
                val portMappingFallback = portMapping.copy(RemoteHost = device.devicePreferences.getBackupWildcard())
                val fallbackResult = upnpClient.deletePortMapping(device, portMappingFallback)
                //TODO when()
                when(fallbackResult) {
                    is UPnPResult.Success -> {
                        ourLogger.log(Level.INFO, "Fallback Delete Success")
                        // set new default to be the opposite of the old
                        device.devicePreferences = device.devicePreferences.copy(useWildcardForRemoteHostDelete = !device.devicePreferences.useWildcardForRemoteHostDelete)
                        devicesDao.upsert(
                            DevicesEntity(
                                device.getIpAddress(),
                                device.udn,
                                device.devicePreferences.useWildcardForRemoteHostDelete
                            )
                        )
                        return fallbackResult
                    }
                    is UPnPResult.Failure -> {
                        // this means we maybe should not have retried
                        // log both original and fallback errors to firebase
                        // log to firebase
                        ourLogger.log(Level.SEVERE, "Fallback Original and Delete Failed. Original: " +
                                result.details.toString() + "\n Fallback: " + fallbackResult.details.toString())
                        return fallbackResult
                    }
                }
            }
            else
            {
                // we failed with non retriable error
                return result
            }
        }
        return result
    }


    // DeletePortMappingRange is only available in v2.0 (also it can only delete
    //   a contiguous range)
    suspend fun deletePortMappingEntry(portMappingWithPref: PortMappingWithPref): UPnPResult {

        try {
            val pm = portMappingWithPref.portMapping
            println("Requesting Delete: ${pm.shortName()}")
            val device: IIGDDevice = getIGDDevice(pm.DeviceIP)
            portMappingDao.deleteByKey(pm.DeviceIP, device.udn, pm.Protocol, pm.ExternalPort)
            val result = deletePortMappingWithFallback(device, pm)
            if (result is UPnPResult.Success)
            {
                ourLogger.log(
                    Level.INFO,
                    "Successfully deleted rule (${pm.shortName()})."
                )
                removeMapping(portMappingWithPref)
            }
            else if (result is UPnPResult.Failure)
            {
               ourLogger.log(
                    Level.SEVERE,
                    "Failed to delete rule (${pm.shortName()})."
                )
               ourLogger.log(Level.SEVERE,
                    result.details.toString())
            }
            return result
        } catch (exception: Exception) {
            ourLogger.log(
                Level.SEVERE,
                "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
            )
            throw exception
        }
    }

    suspend fun deletePortMappingsEntry(
        portMappings: List<PortMappingWithPref>,
    ): List<UPnPResult> {
        return portMappings.map { portMappingWithPref ->

            try {
                val portMapping = portMappingWithPref.portMapping
                println("Requesting Delete: ${portMapping.shortName()}")
                val device = getIGDDevice(portMapping.DeviceIP)
                portMappingDao.deleteByKey(
                    portMapping.DeviceIP,
                    device.udn,
                    portMapping.Protocol,
                    portMapping.ExternalPort
                )
                val result = deletePortMappingWithFallback(device, portMapping)
                if (result is UPnPResult.Success)
                {
                   ourLogger.log(
                        Level.INFO,
                        "Successfully deleted rule (${portMapping.shortName()})."
                    )
                    removeMapping(portMappingWithPref)
                }
                else if (result is UPnPResult.Failure)
                {
                   ourLogger.log(
                        Level.SEVERE,
                        "Failed to delete rule (${portMapping.shortName()})."
                    )
                   ourLogger.log(Level.SEVERE,
                        result.details.toString())
                }
                result

            } catch (exception: Exception) {
                ourLogger.log(
                    Level.SEVERE,
                    "Delete Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }
    }

    private fun createPortMappingDaoEntity(
        portMapping: PortMapping,
        pref: PortMappingPref
    ): PortMappingEntity {
        val igdDevice = getIGDDevice(portMapping.DeviceIP)
        return PortMappingEntity(
            igdDevice.getIpAddress(),
            igdDevice.udn,
            portMapping.ExternalPort,
            portMapping.Protocol,
            portMapping.Description,
            portMapping.InternalIP,
            portMapping.InternalPort,
            pref.autoRenew,
            pref.desiredLeaseDuration,
            portMapping.Enabled
        ) // TODO enabled
    }

    suspend fun createPortMappingRulesEntry(
        portMappingUserInput: PortMappingUserInput,
    ): List<UPnPCreateMappingWrapperResult> {

        try {
            val portMappingRequestRules = portMappingUserInput.splitIntoRules()

            return portMappingRequestRules.map { portMappingRequestRule ->

                val result = createPortMappingRuleWrapper(
                    portMappingRequestRule,
                    false,
                    "created"
                )
                // this is what the user input
                println("default adding rule callback")
                if (result is UPnPCreateMappingWrapperResult.Success) {
                    val portMapping = result.resultingMapping
                    val pref = PortMappingPref(
                        portMappingUserInput.autoRenew,
                        portMappingUserInput.leaseDuration.toIntOrMaxValue()
                    )
                    val entity = createPortMappingDaoEntity(portMapping, pref)
                    portMappingDao.upsert(entity)
                    addOrUpdateMapping(PortMappingWithPref(portMapping, pref))
                }
                result
            }
        } catch (exception: Exception) {
            ourLogger.log(
                Level.SEVERE,
                "Create Rule Failed: " + exception.message + exception.stackTraceToString()
            )
            throw exception
        }
    }

    suspend fun disableEnablePortMappingEntries(
        portMappings: List<PortMappingWithPref>,
        enable: Boolean,
    ): List<UPnPCreateMappingWrapperResult> {

        return portMappings.map { portMapping ->

            try {
                val portMappingRequest = PortMappingRequest.from(portMapping).copy(enabled = enable)
                val res = createPortMappingRuleWrapper(
                    portMappingRequest,
                    false,
                    getEnabledDisabledString(enable).lowercase(),
                )
                addOrUpdateForEnabledDisabled(res, portMapping)
                res
            } catch (exception: Exception) {

                val enableDisableString = if (enable) "Enable" else "Disable"
                ourLogger.log(
                    Level.SEVERE,
                    "$enableDisableString Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                throw exception
            }
        }
    }

    private fun addOrUpdateForEnabledDisabled(
        res: UPnPCreateMappingWrapperResult,
        portMapping: PortMappingWithPref
    ) {
        // TODO we dont need local database for now
        //   also these may not be ours
        if (res is UPnPCreateMappingWrapperResult.Success) {
            addOrUpdateMapping(
                PortMappingWithPref(
                    res.resultingMapping,
                    portMapping.portMappingPref
                )
            )
        }
    }

    private fun addOrUpdateForRenew(
        res: UPnPCreateMappingWrapperResult,
        portMapping: PortMappingWithPref
    ) {
        // this is so it "refreshes"
        if (res is UPnPCreateMappingWrapperResult.Success) {
            addOrUpdateMapping(
                PortMappingWithPref(
                    res.resultingMapping,
                    portMapping.portMappingPref
                )
            )
        }
    }

    //TODO: if List all port mappings exists, that should be used instead of getgeneric.

    // this method creates a rule, then grabs it again to verify it.
    private suspend fun createPortMappingRuleWrapper(
        portMappingRequest: PortMappingRequest,
        skipReadingBack: Boolean,
        createContext: String,
    ): UPnPCreateMappingWrapperResult {
        //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()

        val externalIp = portMappingRequest.externalIp
        val device: IIGDDevice = getIGDDevice(externalIp)
        val createMappingResult = upnpClient.createPortMappingRule(device, portMappingRequest)
        when (createMappingResult) {
            is UPnPCreateMappingResult.Success -> {
                ourLogger.log(
                    Level.INFO,
                    "Successfully $createContext rule (${
                        portMappingRequest.realize().shortName()
                    })."
                )
                println("Successfully added, now reading back")
                if (skipReadingBack) {
                    val result = UPnPCreateMappingWrapperResult.Success(
                        portMappingRequest.realize(),
                        portMappingRequest.realize(),
                        false
                    )
                    return result
                } else {
                    val result = upnpClient.getSpecificPortMappingRule(
                        device,
                        portMappingRequest.remoteHost,
                        portMappingRequest.externalPort,
                        portMappingRequest.protocol,
                    )
                    if (result is UPnPGetSpecificMappingResult.Success) {
                       ourLogger.log(
                            Level.INFO,
                            "Successfully read back our new rule (${result.requestInfo.shortName()})"
                        )
                    }
                    else if (result is UPnPGetSpecificMappingResult.Failure) {
                        val rule = formatShortName(portMappingRequest.protocol,
                            portMappingRequest.externalIp,
                            portMappingRequest.externalPort)
                       ourLogger.log(
                            Level.SEVERE,
                            "Failed to read back our new rule ($rule). Remote Host: ${portMappingRequest.remoteHost}"
                        )
                       ourLogger.log(Level.SEVERE, result.details.toString())
                    }

                    return result.toCreatePortMappingResult()
                }
            }

            is UPnPCreateMappingResult.Failure -> {
               ourLogger.log(
                    Level.SEVERE,
                    "Failed to $createContext rule (${portMappingRequest.realize().shortName()})."
                )
               ourLogger.log(Level.SEVERE,
                    createMappingResult.details.toString())

                val result = UPnPCreateMappingWrapperResult.Failure(
                    createMappingResult.details)
                return result
            }
        }
    }


    suspend fun enumeratePortMappings(externalIp: String) {
        // we enumerate port mappings later
        val device: IIGDDevice = getIGDDevice(externalIp)
        val timeTakenMillis = measureTimeMillis {
            if (device.supportsAction(ACTION_NAMES.GetGenericPortMappingEntry)) {
                ourLogger.log(
                    Level.INFO,
                    "Enumerating Port Listings using GetGenericPortMappingEntry"
                )
                getAllPortMappingsUsingGenericPortMappingEntry(device)
            } else {
                //TODO firebase integration
                ourLogger.log(Level.SEVERE, "device does not have GetGenericPortMappingEntry")
            }
        }
        ourLogger.log(Level.INFO, "Time to enumerate ports: $timeTakenMillis ms")
    }

    private fun isRuleOurs(
        databaseEntity: PortMappingEntity?,
        device: IIGDDevice,
        portMapping: PortMapping
    ): Boolean {
        if (databaseEntity == null) {
            return false
        } else {
            return databaseEntity.description == portMapping.Description &&
                    databaseEntity.internalIp == portMapping.InternalIP &&
                    databaseEntity.internalPort == portMapping.InternalPort
            // rule has been created / changed on the router side and no longer matches
            //   what we had before.  it now belongs to the router, do not take control of it.
        }
    }

    // Had previously tried GetListOfPortMappings but it would encounter error more than 100 ports
    private suspend fun getAllPortMappingsUsingGenericPortMappingEntry(device: IIGDDevice) {
        var slotIndex: Int = 0
        var retryCount: Int = 0
        while (true) {
            var shouldRetry: Boolean = false
            var success: Boolean = false
            try {
                //future.get() // SYNCHRONOUS (note this can, and does, throw)
                val result = upnpClient.getGenericPortMappingRule(device, slotIndex)
                when (result) {
                    is UPnPGetGenericMappingResult.Success -> {
                       ourLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")
                        val portMapping = result.resultingMapping
                        val entity = portMappingDao.getByPrimaryKey(
                            device.getIpAddress(),
                            device.udn,
                            portMapping.Protocol,
                            portMapping.ExternalPort
                        )
                        var pref: PortMappingPref? = null
                        if (isRuleOurs(entity, device, portMapping)) {
                            ourLogger.log(
                                Level.INFO,
                                "The rule is ours: ${portMapping.shortName()}"
                            )
                            pref = entity!!.getPrefs()
                        } else {
                            ourLogger.log(
                                Level.INFO,
                                "The rule is not ours: ${portMapping.shortName()}"
                            )
                        }
                        addOrUpdateMapping(PortMappingWithPref(portMapping, pref)) //!!
                        success = true
                        ourLogger.log(Level.INFO, portMapping.toStringFull())
                        retryCount = 0

                    }

                    is UPnPGetGenericMappingResult.Failure -> {
                        ourLogger.log(
                            Level.INFO,
                            "GetGenericPortMapping failed for entry $slotIndex: ${result.details}.  NOTE: This is normal."
                        )
                    }
                }
            } catch (e: Exception) {
                if (retryCount == 0) // if two exceptions in a row then stop.
                {
                    shouldRetry = true // network issue.. retrying does work.
                    retryCount++
                }
                ourLogger.log(Level.SEVERE, "GetGenericPortMapping threw ${e.message}")
            }

            if (shouldRetry) {
                ourLogger.log(Level.INFO, "Retrying for entry $slotIndex")
                continue
            }


            if (!success) {
                break
            }

            slotIndex++

            if (slotIndex > 65535) {
                ourLogger.log(Level.SEVERE, "CRITICAL ERROR ENUMERATING PORTS, made it past 65535")
            }
        }
    }

    // endregion


    //fun List<IGDDevice>


    private fun getEnabledDisabledString(enabled: Boolean): String {
        return if (enabled) "enabled" else "disabled"
    }

    //region DataUpdate

    private fun clearOldData() {
        upnpClient.clearOldDevices()
        _devices.update { listOf<IGDDevice>() }
        //_portMappings.update {TreeSet<PortMappingWithPref>(SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending))}
        _portMappings.update { mapOf() }
    }

    private fun addDevice(igdDevice: IIGDDevice) {
        // get dao
        portMappingDao
        add(igdDevice)
        ourLogger.log(
            Level.INFO,
            "Added Device ${igdDevice.getDisplayName()} at ${igdDevice.getIpAddress()}."
        )
    }

    //endregion

    fun portMappingsFromIds(selectedIds: Set<PortMappingKey>): List<PortMappingWithPref> {
        val portMappingWithPrefList = mutableListOf<PortMappingWithPref>()
        for (id in selectedIds) {
            if (portMappings.value.containsKey(id)) {
                portMappingWithPrefList.add(portMappings.value[id]!!)
            } else {
                ourLogger.log(Level.SEVERE, "Cannot find port mapping with key $id")
            }
        }

        return portMappingWithPrefList
    }

}

fun UPnPGetSpecificMappingResult.toCreatePortMappingResult() : UPnPCreateMappingWrapperResult =
    when(this){
        is UPnPGetSpecificMappingResult.Failure -> {
            UPnPCreateMappingWrapperResult.Failure(this.details)
        }
        is UPnPGetSpecificMappingResult.Success -> {
            UPnPCreateMappingWrapperResult.Success(this.resultingMapping, this.resultingMapping, true)
        }
}

// this is the request we give the router.  basically a port mapping minus time read and psuedo slot
data class PortMappingRequest(
    val description: String,
    val internalIp: String,
    val internalPort: String,
    val externalIp: String,
    val externalPort: String,
    val protocol: String,
    val leaseDuration: String,
    val enabled: Boolean,
    val remoteHost: String
) {
    fun realize(): PortMapping {
        return PortMapping(
            description,
            remoteHost,
            internalIp,
            externalPort.toInt(),
            internalPort.toInt(),
            protocol,
            enabled,
            leaseDuration.toInt(),
            externalIp,
            SystemClock.elapsedRealtime(),
            GetPsuedoSlot()
        )
    }

    companion object {
        fun from(portMapping: PortMapping, desiredLeaseDuration: Int): PortMappingRequest {
            return PortMappingRequest(
                portMapping.Description,
                portMapping.InternalIP,
                portMapping.InternalPort.toString(),
                portMapping.DeviceIP,
                portMapping.ExternalPort.toString(),
                portMapping.Protocol,
                desiredLeaseDuration.toString(),
                portMapping.Enabled,
                portMapping.RemoteHost
            )
        }

        fun from(portMapping: PortMapping): PortMappingRequest {
            return PortMappingRequest(
                portMapping.Description,
                portMapping.InternalIP,
                portMapping.InternalPort.toString(),
                portMapping.DeviceIP,
                portMapping.ExternalPort.toString(),
                portMapping.Protocol,
                portMapping.LeaseDuration.toString(),
                portMapping.Enabled,
                portMapping.RemoteHost
            )
        }

        fun from(portMapping: PortMappingWithPref): PortMappingRequest {
            return from(portMapping.portMapping, portMapping.getDesiredLeaseDurationOrDefault())
        }
    }
}

//CompletableFuture is api >=24
//basic futures do not implement continuewith...



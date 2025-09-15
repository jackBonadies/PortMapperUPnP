package com.shinjiindustrial.portmapper.client

import android.os.SystemClock
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
import com.shinjiindustrial.portmapper.domain.IClingIGDDevice
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.DevicePreferences
import com.shinjiindustrial.portmapper.domain.DeviceStatus
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fourthline.cling.model.action.ActionException
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.RemoteService

data class MockIGDDevice(val deviceDetails : DeviceDetails,
                         override val status: DeviceStatus,
                         override var devicePreferences: DevicePreferences
) : IIGDDevice()
{
    private val displayName : String = deviceDetails.displayName
    override val udn : String = deviceDetails.udn
    private val ipAddress : String = deviceDetails.ipAddress
    private val upnpTypeVersion : Int = deviceDetails.upnpVersion

    override fun getDisplayName(): String {
        return displayName
    }

    override fun getIpAddress(): String {
        return ipAddress
    }

    override fun supportsAction(actionName: String): Boolean {
        return true
    }

    override fun getActionInvocation(actionName: String): ActionInvocation<*> {
        // not actually used
        return ActionInvocation<RemoteService>(ActionException(0, ""))
    }

    override fun withStatus(status: DeviceStatus): IIGDDevice {
        return this.copy(status = status)
    }

    override fun getUpnpVersion() : Int {
        return upnpTypeVersion
    }

}

data class MockClingIGDDevice(override val deviceDetails: DeviceDetails) : IClingIGDDevice()
{
    override fun createClingDevice(preferences : DevicePreferences) : IIGDDevice
    {
        return MockIGDDevice(deviceDetails, DeviceStatus.Discovered, preferences)
    }
}

enum class Speed(val latency : Long) {
    Fastest(1),
    Fast(15),
    Medium(30),
    Slow(100),
    Slowest(2000)
}

enum class RuleSet() {
    Demo,
    Full
}

data class MockUpnpClientConfig(val speed : Speed, val ruleSet : RuleSet)

class MockUpnpClient(val config : MockUpnpClientConfig) : IUpnpClient {

    private companion object {
        const val NoDelete = "NoDelete"
        const val NoDisableEnable = "NoDisableEnable"
        const val NoUpdate = "NoUpdate"
        const val NoCreate = "NoCreate"

        const val Exception = "Exp"
    }

    private fun getKey(portMapping : PortMapping) : Key
    {
        return Key(portMapping.RemoteHost, portMapping.ExternalPort.toString(), portMapping.Protocol)
    }

    // remoteHost is literally remoteHost (since that's what get specific key mapping uses)
    data class Key(val remoteHost: String, val remotePort: String, val protocol: String)

    private val store = mutableMapOf<String, LinkedHashMap<Key, PortMapping>>()
    private val deviceStore = mutableMapOf<String, MockIGDDevice>()
    private val interfacesUsed = false
    private var initialized = false

    private fun getDescription(index: Int): String {
        return when (index % 16) {
            in 0..11 -> "Rule $index"
            12 -> "Rule $NoCreate $index"
            13 -> "Rule $NoCreate $Exception $index"
            14 -> "Rule $NoDelete $index"
            15 -> "Rule $NoDelete $Exception $index"
            else -> "Rule $index"
        }
    }

    private suspend fun tick() = delay(config.speed.latency)
    private fun shouldThrow(description: String, bannedString: String) = description.contains(bannedString) && description.contains(Exception)
    private fun shouldFail(description: String, bannedString: String) = description.contains(bannedString)

    private fun makePortMapping(
        description: String,
        localIP: String,
        externalPort: Int,
        internalPort: Int,
        protocol: String,
        enabled: Boolean,
        leaseDuration: Int,
        actionExternalIP: String,
        pseudoSlot: Int
    ): PortMapping =
        PortMapping(
            Description = description,
            RemoteHost = "",
            InternalIP = localIP,
            ExternalPort = externalPort,
            InternalPort = internalPort,
            Protocol = protocol,
            Enabled = enabled,
            LeaseDuration = leaseDuration,
            DeviceIP = actionExternalIP,
            TimeReadLeaseDurationMs = SystemClock.elapsedRealtime(),
            Slot = pseudoSlot
        )

    init {

        when(config.ruleSet)
        {
            RuleSet.Demo -> {
                var details = DeviceDetails("Nokia IGD v2", "192.168.18.1", 2, "UUID-1")
                var igdDevice = MockIGDDevice(details, DeviceStatus.Discovered, DevicePreferences())

                var mappings: List<PortMapping> = listOf(
                    makePortMapping(
                        description = "Minecraft Server",
                        localIP = "192.168.1.13",
                        externalPort = 5011,
                        internalPort = 5011,
                        protocol = "TCP",
                        enabled = true,
                        leaseDuration = 24*3600,
                        actionExternalIP = igdDevice.getIpAddress(),
                        pseudoSlot = 1
                    ),
                    makePortMapping(
                        description = "Web Server 1",
                        localIP = "192.168.1.18",
                        externalPort = 8080,
                        internalPort = 8080,
                        protocol = "TCP",
                        enabled = true,
                        leaseDuration = 18*3600,
                        actionExternalIP = igdDevice.getIpAddress(),
                        pseudoSlot = 2
                    ),
                    makePortMapping(
                        description = "ShareDrive",
                        localIP = "192.168.1.13",
                        externalPort = 4044,
                        internalPort = 4044,
                        protocol = "TCP",
                        enabled = true,
                        leaseDuration = 2*3600,
                        actionExternalIP = igdDevice.getIpAddress(),
                        pseudoSlot = 3
                    ),
                    makePortMapping(
                        description = "Game Host 1",
                        localIP = "192.168.1.13",
                        externalPort = 50345,
                        internalPort = 4000,
                        protocol = "TCP",
                        enabled = true,
                        leaseDuration = 1800,
                        actionExternalIP = igdDevice.getIpAddress(),
                        pseudoSlot = 4
                    ),
                )

                store.put(igdDevice.getKey(), linkedMapOf(*mappings.map { getKey(it) to it }.toTypedArray()))
                deviceStore.put(igdDevice.getKey(), igdDevice)
            }
            RuleSet.Full -> {
                var details = DeviceDetails("IGD Other", "192.168.1.255", 2, "UUID-1")
                var igdDevice = MockIGDDevice(details, DeviceStatus.Discovered, DevicePreferences())
                store.put(igdDevice.getKey(), linkedMapOf())
                deviceStore.put(igdDevice.getKey(), igdDevice)

                details = DeviceDetails("IGD Main", "192.168.1.1", 2, "UUID-2")
                igdDevice = MockIGDDevice(details, DeviceStatus.Discovered, DevicePreferences())
                var mappings: List<PortMapping> =
                    (1..200).map { i ->
                        makePortMapping(
                            description = getDescription(i),
                            localIP = "192.168.1.${i%2}",
                            externalPort = if (i < 100)  { 5000 + i } else { 3000 + i },
                            internalPort = 6000 + i,
                            protocol = if (i % 2 == 0) "TCP" else "UDP",
                            enabled = true,
                            leaseDuration = 3600,
                            actionExternalIP = igdDevice.getIpAddress(),
                            pseudoSlot = i
                        )
                    }

                store.put(igdDevice.getKey(), linkedMapOf(*mappings.map { getKey(it) to it }.toTypedArray()))
                deviceStore.put(igdDevice.getKey(), igdDevice)

                details = DeviceDetails("IGD Other2", "192.168.1.244", 2, "UUID-3")
                igdDevice = MockIGDDevice(details, DeviceStatus.Discovered, DevicePreferences())
                mappings =
                    (1..2).map { i ->
                        makePortMapping(
                            description = "Rule $i",
                            localIP = "192.168.1.${i%2}",
                            externalPort = 5000 + i,
                            internalPort = 6000 + i,
                            protocol = if (i % 2 == 0) "TCP" else "UDP",
                            enabled = true,
                            leaseDuration = 3600,
                            actionExternalIP = igdDevice.getIpAddress(),
                            pseudoSlot = i
                        )
                    }
                store.put(igdDevice.getKey(), linkedMapOf(*mappings.map { getKey(it) to it }.toTypedArray()))
                deviceStore.put(igdDevice.getKey(), igdDevice)
            }
        }
    }

    override suspend fun createPortMappingRule(
        device: IIGDDevice,
        portMappingRequest: PortMappingRequest,
    ) : UPnPCreateMappingResult {
        tick()
        return when{
            shouldThrow(portMappingRequest.description, NoCreate) -> throw IllegalStateException("Action blocked by $NoDelete $Exception")
            shouldFail(portMappingRequest.description, NoCreate) -> UPnPCreateMappingResult.Failure(
                FailureDetails("test",
                UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR))
            )
            else -> {
                store[device.getKey()]!!.put(getKey(portMappingRequest.realize()), portMappingRequest.realize())
                UPnPCreateMappingResult.Success(portMappingRequest.realize())
            }
        }
    }

    override suspend fun deletePortMapping(
        device: IIGDDevice,
        portMapping: PortMapping,
    ): UPnPResult {
        tick()
        return when{
            shouldThrow(portMapping.Description, NoDelete) -> throw IllegalStateException("Action blocked by $NoDelete $Exception")
            shouldFail(portMapping.Description, NoDelete) -> UPnPResult.Failure(
                FailureDetails("test",
                UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR))
            )
            else -> {
                store[device.getKey()]!!.remove(getKey(portMapping))
                UPnPResult.Success(portMapping)
            }
        }
    }

    override suspend fun getSpecificPortMappingRule(
        device: IIGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
    ): UPnPGetSpecificMappingResult {
        tick()
        val pm = store[device.getKey()]!!.get(Key(remoteHost, remotePort, protocol))
        pm!!
        return UPnPGetSpecificMappingResult.Success(pm, pm)
    }

    override suspend fun getGenericPortMappingRule(device : IIGDDevice,
                                                   slotIndex : Int) : UPnPGetGenericMappingResult
    {
        tick()
        return UPnPGetGenericMappingResult.Success(store[device.getKey()]!!.values.elementAt(slotIndex),store[device.getKey()]!!.values.elementAt(slotIndex))
    }

    override fun search(maxSeconds : Int)
    {
        GlobalScope.launch {
            for (device in deviceStore.values)
            {
                tick()
                deviceFoundEvent.invoke(MockClingIGDDevice(device.deviceDetails))
            }
        }
    }

    override fun clearOldDevices()
    {
    }

    override fun isInitialized() : Boolean
    {
        return true
    }

    override fun getInterfacesUsedInSearch() : MutableList<NetworkInterfaceInfo>
    {
        return mutableListOf<NetworkInterfaceInfo>()
    }

    override fun instantiateAndBindUpnpService() {

    }

    override val deviceFoundEvent: Event<IClingIGDDevice> = Event<IClingIGDDevice>()
//    private val _deviceFoundEvent = MutableSharedFlow<DeviceFoundEvent>(
//        replay = 0, extraBufferCapacity = 1
//    )
//    val deviceFoundEvent: SharedFlow<DeviceFoundEvent> = _deviceFoundEvent

//data class DeviceFoundEvent(val remoteDevice: RemoteDevice)
}
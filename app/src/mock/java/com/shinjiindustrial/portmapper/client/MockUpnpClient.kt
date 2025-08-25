package com.shinjiindustrial.portmapper.client

import android.os.SystemClock
import com.shinjiindustrial.portmapper.PortMappingRequest
import com.shinjiindustrial.portmapper.common.Event
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

class MockIGDDevice(private val displayName : String, private val ipAddress : String, override val udn : String, private val upnpTypeVersion : Int = 2) : IIGDDevice
{
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

    override fun getDeviceSignature(): String {
        return udn
    }

    override fun getUpnpVersion() : Int {
        return upnpTypeVersion
    }

}

enum class Speed(val latency : Long) {
    Fastest(1),
    Fast(15),
    Medium(30),
    Slow(100),
    Slowest(2000)
}

data class MockUpnpClientConfig(val speed : Speed)

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
        return Key(portMapping.DeviceIP, portMapping.ExternalPort.toString(), portMapping.Protocol)
    }

    data class Key(val externalIp: String, val remotePort: String, val protocol: String)

    private val store = mutableMapOf<IIGDDevice, LinkedHashMap<Key, PortMapping>>()
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
            description = description,
            remoteHost = "",
            localIP = localIP,
            externalPort = externalPort,
            internalPort = internalPort,
            protocol = protocol,
            enabled = enabled,
            leaseDuration = leaseDuration,
            deviceIP = actionExternalIP,
            timeReadLeaseDurationMs = SystemClock.elapsedRealtime(),
            pseudoSlot = pseudoSlot
        )

    init {
        var igdDevice = MockIGDDevice("IGD Other", "192.168.1.255", "UUID-1")
        store.put(igdDevice, linkedMapOf())

        igdDevice = MockIGDDevice("IGD Main", "192.168.1.1", "UUID-2")
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

        store.put(igdDevice, linkedMapOf(*mappings.map { getKey(it) to it }.toTypedArray()))

        igdDevice = MockIGDDevice("IGD Other2", "192.168.1.244", "UUID-3")
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
        store.put(igdDevice, linkedMapOf(*mappings.map { getKey(it) to it }.toTypedArray()))
    }

    override suspend fun createPortMappingRule(
        device: IIGDDevice,
        portMappingRequest: PortMappingRequest,
    ) : UPnPCreateMappingResult {
        tick()
        return when{
            shouldThrow(portMappingRequest.description, NoCreate) -> throw IllegalStateException("Action blocked by $NoDelete $Exception")
            shouldFail(portMappingRequest.description, NoCreate) -> UPnPCreateMappingResult.Failure(
                "test",
                UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR)
            )
            else -> {
                store[device]!!.put(getKey(portMappingRequest.realize()), portMappingRequest.realize())
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
                "test",
                UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR)
            )
            else -> {
                store[device]!!.remove(getKey(portMapping))
                UPnPResult.Success(portMapping)
            }
        }
    }

    override suspend fun getSpecificPortMappingRule(
        device: IIGDDevice,
        remoteHost: String,
        remotePort: String,
        protocol: String,
    ): UPnPCreateMappingWrapperResult {
        tick()
        val pm = store[device]!!.get(Key(remoteHost, remotePort, protocol))
        pm!!
        return UPnPCreateMappingWrapperResult.Success(pm, pm, true)
    }

    override suspend fun getGenericPortMappingRule(device : IIGDDevice,
                                                   slotIndex : Int) : UPnPGetSpecificMappingResult
    {
        tick()
        return UPnPGetSpecificMappingResult.Success(store[device]!!.values.elementAt(slotIndex),store[device]!!.values.elementAt(slotIndex))
    }

    override fun search(maxSeconds : Int)
    {
        GlobalScope.launch {
            tick()
            deviceFoundEvent.invoke(store.keys.elementAt(0))
            tick()
            deviceFoundEvent.invoke(store.keys.elementAt(1))
            tick()
            deviceFoundEvent.invoke(store.keys.elementAt(2))
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

    override val deviceFoundEvent = Event<IIGDDevice>()
//    private val _deviceFoundEvent = MutableSharedFlow<DeviceFoundEvent>(
//        replay = 0, extraBufferCapacity = 1
//    )
//    val deviceFoundEvent: SharedFlow<DeviceFoundEvent> = _deviceFoundEvent

//data class DeviceFoundEvent(val remoteDevice: RemoteDevice)
}
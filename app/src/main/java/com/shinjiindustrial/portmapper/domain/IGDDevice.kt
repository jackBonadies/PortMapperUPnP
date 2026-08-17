package com.shinjiindustrial.portmapper.domain

import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService

data class DevicePreferences(val useWildcardForRemoteHostDelete: Boolean = false) {
    fun isBlankOrStar(remoteHost : String) : Boolean {
        return (remoteHost.isBlank() || remoteHost == "*")
    }

    fun getDefaultWildcard() : String
    {
        return if (useWildcardForRemoteHostDelete) "*" else ""
    }

    fun getBackupWildcard() : String
    {
        return if (useWildcardForRemoteHostDelete) "" else "*"
    }
}

enum class DeviceStatus {
    Discovered,
    FinishedEnumeratingMappings
}

abstract class IIGDDevice {
    abstract fun getDisplayName(): String
    abstract fun getIpAddress(): String
    abstract fun getUpnpVersion(): Int
    abstract fun supportsAction(actionName: String): Boolean
    abstract fun getActionInvocation(actionName: String): ActionInvocation<*>
    abstract fun withStatus(status: DeviceStatus): IIGDDevice
    abstract val deviceDetails: DeviceDetails
    abstract val udn: String
    abstract val status: DeviceStatus
    abstract var devicePreferences: DevicePreferences

    fun getKey() : String {
        return "${getIpAddress()}:${udn}"
    }
}

data class DeviceDetails(
    val displayName: String,
    val ipAddress: String,
    // the IGD device type version i.e. the 2 in InternetGatewayDevice:2
    val upnpVersion: Int,
    val udn: String,
    // everything below comes straight off the SSDP description and is routinely absent
    val friendlyName: String? = null,
    val manufacturer: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val upc: String? = null,
    val deviceType: String? = null, //i.e. InternetGatewayDevice
    val udaVersion: String? = null, //i.e. 1.0, the UPnP spec version, not the device type version
)
{
    companion object {
        fun fromRemoteDevice(remoteDevice: RemoteDevice): DeviceDetails {
            val details = remoteDevice.details
            return DeviceDetails(
                remoteDevice.displayString,
                remoteDevice.identity.descriptorURL.host,
                remoteDevice.type.version,
                remoteDevice.root.identity.udn.toString(),
                friendlyName = details?.friendlyName,
                manufacturer = details?.manufacturerDetails?.manufacturer,
                modelName = details?.modelDetails?.modelName,
                modelNumber = details?.modelDetails?.modelNumber,
                serialNumber = details?.serialNumber,
                upc = details?.upc,
                deviceType = remoteDevice.type?.type,
                udaVersion = remoteDevice.version?.let { "${it.major}.${it.minor}" },
            )
        }
    }
}

// has state including rules. add update those rules. and do a upnp action. and sort.
data class IGDDevice(override val deviceDetails: DeviceDetails,
                     override var devicePreferences : DevicePreferences,
                     private val wanIPService: RemoteService,
                     override val status: DeviceStatus = DeviceStatus.Discovered) :
    IIGDDevice() {
    // nullrefs warning: this is SSDP response, very possible for some fields to be null, DeviceDetails constructor allows it.
    // the best bet on ip is (rootDevice!!.identity.descriptorURL.host) imo.  since that is what we use in RetreiveRemoteDescriptors class
    // which then calls remoteDeviceAdded (us). presentationURI can be and is null sometimes.
    private val displayName: String = deviceDetails.displayName//i.e. Nokia IGD Version 2.00
    private val ipAddress: String = deviceDetails.ipAddress //i.e. 192.168.18.1

    //this.rootDevice!!.details.presentationURI.host
    private val upnpTypeVersion: Int = deviceDetails.upnpVersion//i.e. 2
    private val actionsMap: MutableMap<String, Action<RemoteService>> = mutableMapOf()
    override val udn = deviceDetails.udn

    override fun getUpnpVersion(): Int {
        return upnpTypeVersion
    }

    override fun getDisplayName(): String {
        return displayName
    }

    override fun getIpAddress(): String {
        return ipAddress
    }

    override fun supportsAction(actionName: String): Boolean {
        return this.actionsMap.containsKey(actionName)
    }

    override fun getActionInvocation(actionName: String): ActionInvocation<*> {
        val action = this.actionsMap[actionName]
        return ActionInvocation(action)
    }

    override fun withStatus(status: DeviceStatus): IIGDDevice {
        return this.copy(status = status)
    }

    init {
        for (action in wanIPService.actions) {
            if (ActionNames.contains(action.name)) {
                // are easy to call and parse output
                actionsMap[action.name] = action
            }
        }
    }
}
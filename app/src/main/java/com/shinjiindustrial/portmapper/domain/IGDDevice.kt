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
    abstract val udn: String
    abstract val status: DeviceStatus
    abstract var devicePreferences: DevicePreferences

    fun getKey() : String {
        return "${getIpAddress()}:${udn}"
    }
}

data class DeviceDetails(val displayName: String, val ipAddress: String, val upnpVersion: Int, val udn : String)
{
    companion object {
        fun fromRemoteDevice(remoteDevice: RemoteDevice): DeviceDetails {
            return DeviceDetails(
                remoteDevice.displayString,
                remoteDevice.identity.descriptorURL.host,
                remoteDevice.type.version,
                remoteDevice.root.identity.udn.toString()
            )
        }
    }
}

// has state including rules. add update those rules. and do a upnp action. and sort.
data class IGDDevice(val deviceDetails: DeviceDetails,
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

    //private val upnpType : String = this.rootDevice.type.type //i.e. InternetGatewayDevice
    // can cause crashes if friendlyName is null
    //private val friendlyDetailsName : String =
    //    this.rootDevice.details.friendlyName //i.e. Internet Home Gateway Device
//    private val manufacturer : String =
//        this.rootDevice.details.manufacturerDetails?.manufacturer ?: "" //i.e. Nokia

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
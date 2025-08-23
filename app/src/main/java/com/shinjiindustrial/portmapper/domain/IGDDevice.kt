package com.shinjiindustrial.portmapper.domain

import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService

interface IIGDDevice {
    fun getDisplayName() : String
    fun getIpAddress() : String
    fun getUpnpVersion() : Int
    fun supportsAction(actionName : String) : Boolean
    fun getActionInvocation(actionName : String) : ActionInvocation<*>
}

// has state including rules. add update those rules. and do a upnp action. and sort.
class IGDDevice constructor(private val rootDevice : RemoteDevice, private val wanIPService : RemoteService) : IIGDDevice
{

    // nullrefs warning: this is SSDP response, very possible for some fields to be null, DeviceDetails constructor allows it.
    // the best bet on ip is (rootDevice!!.identity.descriptorURL.host) imo.  since that is what we use in RetreiveRemoteDescriptors class
    // which then calls remoteDeviceAdded (us). presentationURI can be and is null sometimes.
    private val displayName : String = this.rootDevice.displayString //i.e. Nokia IGD Version 2.00
    private val friendlyDetailsName : String =
        this.rootDevice.details.friendlyName //i.e. Internet Home Gateway Device
    private val manufacturer : String =
        this.rootDevice.details.manufacturerDetails?.manufacturer ?: "" //i.e. Nokia
    private val ipAddress : String = rootDevice.identity.descriptorURL.host //i.e. 192.168.18.1
    //this.rootDevice!!.details.presentationURI.host
    private val upnpType : String = this.rootDevice.type.type //i.e. InternetGatewayDevice
    private val upnpTypeVersion : Int = this.rootDevice.type.version //i.e. 2
    private val actionsMap : MutableMap<String, Action<RemoteService>> = mutableMapOf()

    override fun getUpnpVersion() : Int {
        return upnpTypeVersion
    }

    override fun getDisplayName() : String {
        return displayName
    }

    override fun getIpAddress() : String {
        return ipAddress
    }

    override fun supportsAction(actionName : String) : Boolean {
        return this.actionsMap.containsKey(actionName)
    }

    override fun getActionInvocation(actionName: String): ActionInvocation<*> {
        val action = this.actionsMap[actionName]
        return ActionInvocation(action)
    }

    init {
        for (action in wanIPService.actions)
        {
            if(ActionNames.contains(action.name))
            {
                // are easy to call and parse output
                actionsMap[action.name] = action
            }
        }
    }
}
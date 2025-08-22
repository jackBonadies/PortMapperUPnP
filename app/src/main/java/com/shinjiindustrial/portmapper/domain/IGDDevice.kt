package com.shinjiindustrial.portmapper.domain

import com.shinjiindustrial.portmapper.MainActivity
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.SharedPrefValues
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.UpnpManager.Companion.GetUPnPClient
import com.shinjiindustrial.portmapper.UpnpManager.Companion.lockIgdDevices
import com.shinjiindustrial.portmapper.client.UPnPGetSpecificMappingResult
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import java.util.TreeSet
import java.util.logging.Level
import kotlin.system.measureTimeMillis

// has state including rules. add update those rules. and do a upnp action. and sort.
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
                if(ActionNames.contains(action.name))
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
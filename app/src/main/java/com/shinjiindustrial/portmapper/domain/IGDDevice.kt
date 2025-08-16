package com.shinjiindustrial.portmapper.domain

import com.shinjiindustrial.portmapper.MainActivity
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.SharedPrefValues
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.UpnpManager.Companion.lockIgdDevices
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import java.util.TreeSet
import java.util.logging.Level
import kotlin.system.measureTimeMillis

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
                if(UpnpManager.ActionNames.contains(action.name))
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

    fun EnumeratePortMappings()
    {
        // we enumerate port mappings later
        portMappings = TreeSet<PortMapping>(SharedPrefValues.SortByPortMapping.getComparer(SharedPrefValues.Ascending))
        var finishedEnumeratingPortMappings = false

        val timeTakenMillis = measureTimeMillis {

//        if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings))
//        {
//            OurLogger.log(Level.INFO, "Enumerating Port Listings using GetListOfPortMappings")
//            var getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetListOfPortMappings]!!
//            getAllPortMappingsUsingListPortMappings(getPortMapping)
//        }
            if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry))
            {
                OurLogger.log(Level.INFO, "Enumerating Port Listings using GetGenericPortMappingEntry")
                val getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry]!!
                getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping)
            }
            else{
                OurLogger.log(Level.SEVERE, "device does not have GetGenericPortMappingEntry")
            }

        }
        OurLogger.log(Level.INFO, "Time to enumerate ports: $timeTakenMillis ms")


        finishedEnumeratingPortMappings = true
        UpnpManager.FinishedListingPortsEvent.invoke(this)
    }

    // Had previously tried GetListOfPortMappings but it would encounter error more than 100 ports
    private fun getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping : Action<RemoteService>) {
        var slotIndex : Int = 0;
        var retryCount : Int = 0
        while(true)
        {
            var shouldRetry : Boolean = false
            var success : Boolean = false;
            val actionInvocation = ActionInvocation(getPortMapping)
            println("requesting slot $slotIndex")
            actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
            val future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    retryCount = 0
                    OurLogger.log(Level.INFO, "GetGenericPortMapping succeeded for entry $slotIndex")

                    val remoteHost = invocation.getOutput("NewRemoteHost") //string datatype // the .value is null (also empty if GetListOfPortMappings is used)
                    val externalPort = invocation.getOutput("NewExternalPort") //unsigned 2 byte int
                    val internalClient = invocation.getOutput("NewInternalClient") //string datatype
                    val internalPort = invocation.getOutput("NewInternalPort")
                    val protocol = invocation.getOutput("NewProtocol")
                    val description = invocation.getOutput("NewPortMappingDescription")
                    val enabled = invocation.getOutput("NewEnabled")
                    val leaseDuration = invocation.getOutput("NewLeaseDuration")
                    val portMapping = PortMapping(
                        description.toString(),
                        remoteHost.toString(), //TODO standardize remote host so null == string.empty
                        internalClient.toString(),
                        externalPort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt(),
                        ipAddress,
                        System.currentTimeMillis(),
                        slotIndex)
                    addOrUpdate(portMapping)
                    UpnpManager.PortInitialFoundEvent.invoke(portMapping)
                    success = true
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    retryCount = 0
                    OurLogger.log(Level.INFO, "GetGenericPortMapping failed for entry $slotIndex: $defaultMsg.  NOTE: This is normal.")
                    // Handle failure
                }
            })

            try {
                future.get() // SYNCHRONOUS (note this can, and does, throw)
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
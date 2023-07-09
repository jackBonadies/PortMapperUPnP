package com.example.myapplication

import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.fourthline.cling.controlpoint.ActionCallback
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Action
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class UpnpManager {


    companion object { //singleton
        private var _upnpService : UpnpService? = null

        object UPnPNames {
            val InternetGatewayDevice = "InternetGatewayDevice"
            val WANDevice = "WANDevice"
            val WANConnectionDevice = "WANConnectionDevice"
            val WANIPConnection = "WANIPConnection"
//            val IGD_DEVICE_TYPE: DeviceType = UDADeviceType(InternetGatewayDevice, 1)
//            val IGD_DEVICE_TYPE_2: DeviceType = UDADeviceType(InternetGatewayDevice, 2)
//
//            val CONNECTION_DEVICE_TYPE: DeviceType = UDADeviceType(WANConnectionDevice, 1)
//            val CONNECTION_DEVICE_TYPE_2: DeviceType = UDADeviceType(WANConnectionDevice, 2)
        }

        object ACTION_NAMES
        {
            // these actions are all Required in v1, v2
            val AddPortMapping : String = "AddPortMapping"
            val GetExternalIPAddress : String = "GetExternalIPAddress"
            val DeletePortMapping : String = "DeletePortMapping"
            val GetStatusInfo : String = "GetStatusInfo"
            val GetGenericPortMappingEntry : String = "GetGenericPortMappingEntry"
            val GetSpecificPortMappingEntry : String = "GetSpecificPortMappingEntry"

            // this action is required in v2, not present in v1
            var AddAnyPortMapping : String = "AddAnyPortMapping"

            // this action is required for device, optional for control point in v2, not present in v1
            var GetListOfPortMappings : String = "GetListOfPortMappings"
        }

        val ActionNames : List<String> = listOf(
            ACTION_NAMES.AddPortMapping,
            ACTION_NAMES.GetExternalIPAddress,
            ACTION_NAMES.DeletePortMapping,
            ACTION_NAMES.GetStatusInfo,
            ACTION_NAMES.GetGenericPortMappingEntry,
            ACTION_NAMES.GetSpecificPortMappingEntry,

            ACTION_NAMES.AddAnyPortMapping,
            ACTION_NAMES.GetListOfPortMappings,
        )

        var DeviceFoundEvent = Event<IGDDevice>()
        var PortFoundEvent = Event<PortMapping>()
        // used if we finish and there are no ports to show the "no devices" card
        var FinishedListingPortsEvent = Event<IGDDevice>()



        fun GetUPnPService() : UpnpService {
            return _upnpService!!
        }

        fun Initialize()
        {
            class AndroidConfig : AndroidUpnpServiceConfiguration() {
                override fun getServiceDescriptorBinderUDA10(): ServiceDescriptorBinder {
                    return UDA10ServiceDescriptorBinderImpl()
                }
            }
            _upnpService = UpnpServiceImpl(AndroidConfig())
        }

        fun AddDevice(igdDevice: IGDDevice) {
            IGDDevices.add(igdDevice)
            UpnpManager.DeviceFoundEvent.invoke(igdDevice)
        }

        var IGDDevices : MutableList<IGDDevice> = mutableListOf()


        fun GetSpecificPortMappingRule(
            remoteIp : String,
            remotePort : String,
            protocol : String,
            callback: (UPnPCreateMappingResult) -> Unit)
             : Future<Any> {



            var device : IGDDevice = getIGDDevice(remoteIp)
            var action = device.actionsMap[ACTION_NAMES.GetSpecificPortMappingEntry]
            var actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost",remoteIp)
            actionInvocation.setInput("NewExternalPort",remotePort)
            actionInvocation.setInput("NewProtocol",protocol)

            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!

                    println("Successfully readback our new rule")

                    var internalPort = actionInvocation.getOutput("NewInternalPort")
                    var internalClient = actionInvocation.getOutput("NewInternalClient")
                    var enabled = actionInvocation.getOutput("NewEnabled")
                    var description = actionInvocation.getOutput("NewPortMappingDescription")
                    var leaseDuration = actionInvocation.getOutput("NewLeaseDuration")

                    var pm = PortMapping(
                        description.toString(),
                        remoteIp.toString(),
                        internalClient.toString(),
                        remotePort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt())

                    var result = UPnPCreateMappingResult(true)
                    result.ResultingMapping = pm
                    callback(result)
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    println(defaultMsg)

                    println(operation.statusMessage)
                    println(operation.responseDetails)
                    println(operation.statusCode)
                    println(operation.isFailed)

                    var result = UPnPCreateMappingResult(false)
                    result.UPnPFailureResponse = operation
                    result.FailureReason = defaultMsg
                    callback(result)
                }
            })


            return future
        }

        // this method creates a rule, then grabs it again to verify it.
        fun CreatePortMappingRule(
            description : String,
            internalIp : String,
            internalPortText : String,
            externalIp : String,
            externalPortText : String,
            protocol : String,
            leaseDuration : String,
            callback: (UPnPCreateMappingResult) -> Unit) : Future<Any>
        {
            //var completeableFuture = CompletableFuture<UPnPCreateMappingResult>()



            var device : IGDDevice = getIGDDevice(externalIp)
            var action = device.actionsMap[ACTION_NAMES.AddPortMapping]
            var actionInvocation = ActionInvocation(action)
            actionInvocation.setInput("NewRemoteHost", "$externalIp");
            // it does validate the args (to at least be in range of 2 unsigned bytes i.e. 65535)
            actionInvocation.setInput("NewExternalPort", "$externalPortText");
            actionInvocation.setInput("NewProtocol", "$protocol");
            actionInvocation.setInput("NewInternalPort", "$internalPortText");
            actionInvocation.setInput("NewInternalClient", "$internalIp");
            actionInvocation.setInput("NewEnabled", "1");
            actionInvocation.setInput("NewPortMappingDescription", description);
            actionInvocation.setInput("NewLeaseDuration", "$leaseDuration");

            var future = UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!
                    println("Successfully added, now reading back")

                    var specificFuture = GetSpecificPortMappingRule(externalIp, externalPortText, protocol, callback)
                    specificFuture.get()

                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    // Handle failure
                    println(defaultMsg)

                    println(operation.statusMessage)
                    println(operation.responseDetails)
                    println(operation.statusCode)
                    println(operation.isFailed)

                    var result = UPnPCreateMappingResult(false)
                    result.FailureReason = defaultMsg
                    result.UPnPFailureResponse = operation
                    callback(result)
                }
            })

            return future
        }

        public fun getIGDDevice(ipAddr : String): IGDDevice {
            return IGDDevices.first {it.ipAddress == ipAddr };
        }


        //fun List<IGDDevice>
    }
}

//CompletableFuture is api >=24
//basic futures do not implement continuewith...

//class UPnPCreateMappingResult : UPnPResult
//{
//    constructor() : super()
//
//    @Volatile
//    var ResultingMapping : PortMapping? = null
//}
//
//class UPnPGetSpecificMappingResult : UPnPResult
//{
//    constructor() : super()
//
//    @Volatile
//    var ResultingMapping : PortMapping? = null
//}
//
//open class UPnPResult constructor()
//{
//    var Future : Future<Any>? = null
//
//    @Volatile
//    var Success : Boolean? = null
//    @Volatile
//    var FailureReason : String? = null
//
////    init
////    {
////        Success = success
////        Future = future
////    }
//}



class UPnPCreateMappingResult : UPnPResult
{
    constructor(success: Boolean) : super(success)
    var ResultingMapping : PortMapping? = null
}

class UPnPGetSpecificMappingResult : UPnPResult
{
    constructor(success : Boolean) : super(success)
    var ResultingMapping : PortMapping? = null
}

open class UPnPResult constructor(success : Boolean)
{
    var Success : Boolean
    var FailureReason : String? = null
    var UPnPFailureResponse : UpnpResponse? = null

    init
    {
        Success = success
    }
}

class OurNetworkInfo {
    companion object { //singleton

        var retrieved : Boolean = false
        var ourIp : String? = null
        var gatewayIp : String? = null

        fun GetLocalAndGatewayIpAddr(context: Context, forceRefresh : Boolean): Pair<String?,String?> {
            if(!forceRefresh && retrieved)
            {
                return Pair(ourIp, gatewayIp)
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var ipAddress = wifiManager.connectionInfo.ipAddress
            //var macAddress = wifiManager.connectionInfo.macAddress
            val gatewayIpAddress = wifiManager.dhcpInfo.gateway

            ourIp = formatIpv4(ipAddress)
            gatewayIp = formatIpv4(gatewayIpAddress)
            retrieved = true
            return Pair(ourIp, gatewayIp)

//        val byteBuffer = ByteBuffer.allocate(4)
//        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
//        byteBuffer.putInt(ipAddress)
//        val inetAddress = InetAddress.getByAddress(null, byteBuffer.array())
//        var hostAddress = inetAddress.hostAddress
//        val macParts: List<String> = macAddress.split(":")
//        val macBytes = ByteArray(macParts.size)
//        for (i in macParts.indices) {
//            macBytes[i] = macParts[i].toInt(16).toByte()
//        }
//
//        val en = NetworkInterface.getNetworkInterfaces()
//        while (en.hasMoreElements()) {
//            val intf = en.nextElement()
//            if (intf.hardwareAddress == macBytes) {
//                val enumIpAddr = intf.inetAddresses
//                while (enumIpAddr.hasMoreElements()) {
//                    val inetAddress = enumIpAddr.nextElement()
//                    if (!inetAddress.isLoopbackAddress) {
//                        return inetAddress.hostAddress
//                    }
//                }
//            }
//        }
//    } catch (ex: java.lang.Exception) {
//        Log.e("IP Address", ex.toString())
//    }
//    return null
        }
    }
}

class IGDDevice constructor(_rootDevice : RemoteDevice, _wanIPService : RemoteService)
{

    var rootDevice : RemoteDevice
    var wanIPService : RemoteService
    var displayName : String //i.e. Nokia IGD Version 2.00
    var friendlyDetailsName : String //i.e. Internet Home Gateway Device
    var manufacturer : String //i.e. Nokia
    var ipAddress : String //i.e. 192.168.18.1
    var upnpType : String //i.e. InternetGatewayDevice
    var upnpTypeVersion : Int //i.e. 2
    var actionsMap : MutableMap<String, Action<RemoteService>>
    var portMappings : MutableList<PortMapping> = mutableListOf()
//    var hasAddPortMappingAction : Boolean
//    var hasDeletePortMappingAction : Boolean

    init {
        this.rootDevice = _rootDevice
        this.wanIPService = _wanIPService
        this.displayName = this.rootDevice.displayString
        this.friendlyDetailsName = this.rootDevice.details.friendlyName
        this.ipAddress = this.rootDevice.details.presentationURI.host //TODO if null maybe another way to find out
        this.manufacturer = this.rootDevice.details.manufacturerDetails.manufacturer
        this.upnpType = this.rootDevice.type.type
        this.upnpTypeVersion = this.rootDevice.type.version
        this.actionsMap = mutableMapOf()
        for (action in _wanIPService.actions)
        {
            if(UpnpManager.ActionNames.contains(action.name))
            {
                // TODO: convert "actions" into strongly typed functions so they
                // are easy to call and parse output
                actionsMap[action.name] = action
            }
        }
    }



    fun EnumeratePortMappings()
    {
        // we enumerate port mappings later
        portMappings = mutableListOf()
        var finishedEnumeratingPortMappings = false
        if(actionsMap.containsKey(UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry))
        {
            var getPortMapping = actionsMap[UpnpManager.Companion.ACTION_NAMES.GetGenericPortMappingEntry]!!
            getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping)
        }
        finishedEnumeratingPortMappings = true
        UpnpManager.FinishedListingPortsEvent.invoke(this)
    }

    private fun getAllPortMappingsUsingGenericPortMappingEntry(getPortMapping : Action<RemoteService>) {
        var slotIndex : Int = 0;
        while(true)
        {
            var success : Boolean = false;
            var actionInvocation = ActionInvocation(getPortMapping)
            actionInvocation.setInput("NewPortMappingIndex", "$slotIndex");
            UpnpManager.GetUPnPService().controlPoint.execute(object : ActionCallback(actionInvocation) {
                override fun success(invocation: ActionInvocation<*>?) {
                    invocation!!

                    var remoteHost = invocation.getOutput("NewRemoteHost") //string datatype // the .value is null (also empty if GetListOfPortMappings is used)
                    var externalPort = invocation.getOutput("NewExternalPort") //unsigned 2 byte int
                    var internalClient = invocation.getOutput("NewInternalClient") //string datatype
                    var internalPort = invocation.getOutput("NewInternalPort")
                    var protocol = invocation.getOutput("NewProtocol")
                    var description = invocation.getOutput("NewPortMappingDescription")
                    var enabled = invocation.getOutput("NewEnabled")
                    var leaseDuration = invocation.getOutput("NewLeaseDuration")
                    var portMapping = PortMapping(
                        description.toString(),
                        remoteHost.toString(),
                        internalClient.toString(),
                        externalPort.toString().toInt(),
                        internalPort.toString().toInt(),
                        protocol.toString(),
                        enabled.toString().toInt() == 1,
                        leaseDuration.toString().toInt())
                    portMappings.add(portMapping)
                    // TODO: new port mapping added event
                    UpnpManager.PortFoundEvent.invoke(portMapping)
                    success = true
                }

                override fun failure(
                    invocation: ActionInvocation<*>?,
                    operation: UpnpResponse,
                    defaultMsg: String
                ) {
                    println("GetGenericPortMapping failed for entry $slotIndex: $defaultMsg")
                    // Handle failure
                }
            }).get() // SYNCHRONOUS

            if (!success)
            {
                break;
            }

            slotIndex++

            if (slotIndex > 65535)
            {
                println("CRITICAL ERROR ENUMERATING PORTS, made it past 65535")
            }
        }
    }

}

class Event<T> {
    private val observers = mutableSetOf<(T) -> Unit>()

    operator fun plusAssign(observer: (T) -> Unit) {
        observers.add(observer)
    }

    operator fun minusAssign(observer: (T) -> Unit) {
        observers.remove(observer)
    }

    operator fun invoke(value: T) {
        for (observer in observers)
            observer(value)
    }
}

class UPnPViewElement constructor(
    underlyingElement : kotlin.Any,
    isSpecialEmpty : Boolean = false)
{
    var UnderlyingElement : kotlin.Any
    var IsSpecialEmpty : kotlin.Boolean

    init
    {
        IsSpecialEmpty = isSpecialEmpty
        UnderlyingElement = underlyingElement
    }

    fun IsIGDDevice() : Boolean
    {
        return UnderlyingElement is IGDDevice
    }

    fun GetUnderlyingIGDDevice() : IGDDevice
    {
        return UnderlyingElement as IGDDevice
    }

    fun IsPortMapping() : Boolean
    {
        return UnderlyingElement is PortMapping
    }

    fun GetUnderlyingPortMapping() : PortMapping
    {
        return UnderlyingElement as PortMapping
    }
}

class PortMapping constructor(
    val _Description: String,
    val _ExternalIP: String,
    val _LocalIP: String,
    val _ExternalPort: Int,
    val _InternalPort: Int,
    val _Protocol: String,
    val _Enabled: Boolean,
    val _LeaseDuration: Int)
{
    var ExternalIP : String
    var LocalIP : String
    var ExternalPort : Int
    var InternalPort : Int
    var Protocol : String
    var Enabled : Boolean
    var LeaseDuration : Int
    var Description : String

    init {
        this.ExternalIP = _ExternalIP
        this.LocalIP = _LocalIP
        this.ExternalPort = _ExternalPort
        this.InternalPort = _InternalPort
        this.Protocol = _Protocol
        this.Enabled = _Enabled
        this.LeaseDuration = _LeaseDuration
        this.Description = _Description
    }
}
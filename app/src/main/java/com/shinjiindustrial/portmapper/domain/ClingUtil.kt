package com.shinjiindustrial.portmapper.domain

import com.shinjiindustrial.portmapper.ILogger
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import java.util.logging.Level

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

object ACTION_NAMES {
    // these actions are all Required in v1, v2
    val AddPortMapping: String = "AddPortMapping"
    val GetExternalIPAddress: String = "GetExternalIPAddress"
    val DeletePortMapping: String = "DeletePortMapping"
    val GetStatusInfo: String = "GetStatusInfo"
    val GetGenericPortMappingEntry: String = "GetGenericPortMappingEntry"
    val GetSpecificPortMappingEntry: String = "GetSpecificPortMappingEntry"

    // this action is required in v2, not present in v1
    var AddAnyPortMapping: String = "AddAnyPortMapping"

    // this action is required for device, optional for control point in v2, not present in v1
    var GetListOfPortMappings: String = "GetListOfPortMappings"
}

val ActionNames: List<String> = listOf(
    ACTION_NAMES.AddPortMapping,
    ACTION_NAMES.GetExternalIPAddress,
    ACTION_NAMES.DeletePortMapping,
    ACTION_NAMES.GetStatusInfo,
    ACTION_NAMES.GetGenericPortMappingEntry,
    ACTION_NAMES.GetSpecificPortMappingEntry,

    ACTION_NAMES.AddAnyPortMapping,
    ACTION_NAMES.GetListOfPortMappings,
)

abstract class IClingIGDDevice()
{
    abstract val deviceDetails : DeviceDetails
    abstract fun createClingDevice(preferences : DevicePreferences) : IIGDDevice
}

data class ClingIGDDevice(override val deviceDetails: DeviceDetails, val remoteService: RemoteService) : IClingIGDDevice()
{
    override fun createClingDevice(preferences : DevicePreferences) : IIGDDevice
    {
        return IGDDevice(deviceDetails, preferences, remoteService)
    }
}

fun RemoteDevice.getIGDDevice(ourLogger : ILogger): ClingIGDDevice? {

    if (this.type.type.equals(UPnPNames.InternetGatewayDevice)) // version agnostic
    {
        ourLogger.log(
            Level.INFO,
            "Device ${this.displayString} is of interest, type is ${this.type}"
        )

        // http://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v1-Service.pdf
        // Device Tree: InternetGatewayDevice > WANDevice > WANConnectionDevice
        // Service is WANIPConnection
        val wanDevice =
            this.embeddedDevices.firstOrNull { it.type.type == UPnPNames.WANDevice }
        if (wanDevice != null) {
            val wanConnectionDevice =
                wanDevice.embeddedDevices.firstOrNull { it.type.type == UPnPNames.WANConnectionDevice }
            if (wanConnectionDevice != null) {
                val wanIPService =
                    wanConnectionDevice.services.firstOrNull { it.serviceType.type == UPnPNames.WANIPConnection }
                if (wanIPService != null) {
                    //get relevant actions here...
                    //TODO add relevant service (and cause event)
                    val igdDevice = ClingIGDDevice(DeviceDetails.fromRemoteDevice(this), wanIPService)
                    return igdDevice

                } else {
                    ourLogger.log(
                        Level.SEVERE,
                        "WanConnectionDevice does not have WanIPConnection service"
                    )
                }
            } else {
                ourLogger.log(
                    Level.SEVERE,
                    "WanConnectionDevice not found under WanDevice"
                )
            }
        } else {
            ourLogger.log(
                Level.SEVERE,
                "WanDevice not found under InternetGatewayDevice"
            )
        }
    } else {
        ourLogger.log(
            Level.INFO,
            "Device ${this.displayString} is NOT of interest, type is ${this.type}"
        )
    }
    return null
}
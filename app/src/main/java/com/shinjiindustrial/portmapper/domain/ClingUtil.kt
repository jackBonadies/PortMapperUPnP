package com.shinjiindustrial.portmapper.domain

import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger
import com.shinjiindustrial.portmapper.UpnpManager.Companion.AddDevice
import com.shinjiindustrial.portmapper.UpnpManager.Companion.UPnPNames
import org.fourthline.cling.model.meta.RemoteDevice
import java.util.logging.Level

fun RemoteDevice.getIGDDevice(): IGDDevice? {

    if (this.type.type.equals(UPnPNames.InternetGatewayDevice)) // version agnostic
    {
        OurLogger.log(
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
                    val igdDevice = IGDDevice(this, wanIPService)
                    return igdDevice;

                } else {
                    OurLogger.log(
                        Level.SEVERE,
                        "WanConnectionDevice does not have WanIPConnection service"
                    )
                }
            } else {
                OurLogger.log(
                    Level.SEVERE,
                    "WanConnectionDevice not found under WanDevice"
                )
            }
        } else {
            OurLogger.log(
                Level.SEVERE,
                "WanDevice not found under InternetGatewayDevice"
            )
        }
    } else {
        OurLogger.log(
            Level.INFO,
            "Device ${this.displayString} is NOT of interest, type is ${this.type}"
        )
    }
    return null;
}
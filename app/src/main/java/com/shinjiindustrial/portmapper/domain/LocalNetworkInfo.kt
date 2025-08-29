package com.shinjiindustrial.portmapper.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.common.NetworkType
import com.shinjiindustrial.portmapper.formatIpv4

data class OurNetworkInfoBundle(
    val networkType: NetworkType,
    val ourIp: String?,
    val gatewayIp: String?
)

class OurNetworkInfo {
    companion object { //singleton

        var retrieved: Boolean = false
        var ourIp: String? = null
        var gatewayIp: String? = null
        var networkType: NetworkType? = null

        fun GetNetworkInfo(context: Context, forceRefresh: Boolean): OurNetworkInfoBundle {
            GetConnectionType(context, forceRefresh)
            if (networkType == NetworkType.WIFI) {
                GetLocalAndGatewayIpAddrWifi(context, forceRefresh)
            } else {
                ourIp = null
                gatewayIp = null
            }

            return OurNetworkInfoBundle(networkType!!, ourIp, gatewayIp)
        }

        fun GetLocalAndGatewayIpAddrWifi(
            context: Context,
            forceRefresh: Boolean
        ): Pair<String?, String?> {
            if (!forceRefresh && retrieved) {
                return Pair(ourIp, gatewayIp)
            }

            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress

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

        fun GetNameTypeMappings(context: Context): MutableMap<String, NetworkType> {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mappings: MutableMap<String, NetworkType> = mutableMapOf<String, NetworkType>()
            for (net1 in cm.allNetworks) {
                val netInfo = cm.getNetworkInfo(net1)
                val name = cm.getLinkProperties(net1)?.interfaceName
                if (name == null) {
                    continue
                }
                val type = getNetworkType(cm, net1, netInfo)
                mappings[name] = type
            }
            return mappings
        }

        fun GetTypeFromInterfaceName(
            _mappings: MutableMap<String, NetworkType>?,
            interfaceName: String
        ): NetworkType {
            var mappings = _mappings
            if (mappings == null) {
                mappings =
                    GetNameTypeMappings(PortForwardApplication.appContext) //TODO: dont call this expensive call everytime
            }
            mappings
            if (mappings.containsKey(interfaceName)) {
                return mappings[interfaceName] ?: NetworkType.NONE
            } else {
                if (interfaceName.contains("wlan")) {
                    return NetworkType.WIFI
                } else if (interfaceName.contains("rmnet") || interfaceName.contains("data")) {
                    return NetworkType.DATA
                }
            }
            return NetworkType.NONE
        }

        fun GetConnectionType(context: Context, forceRefresh: Boolean): NetworkType {

            if (!forceRefresh && networkType != null) {
                return networkType!!
            }


            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


            //get type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkType = getNetworkType(cm, cm.activeNetwork, cm.activeNetworkInfo)
            } else {
                networkType = getNetworkType(cm, null, cm.activeNetworkInfo)
            }
            return networkType as NetworkType
        }

        fun getNetworkType(
            cm: ConnectivityManager,
            network: Network?,
            networkInfo: NetworkInfo?
        ): NetworkType {
            var result = NetworkType.NONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.getNetworkCapabilities(network)?.run {
                    when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            result = NetworkType.WIFI
                        }

                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            result = NetworkType.DATA
                        }

                        else -> {
                            result = NetworkType.NONE
                        }
                    }
                }
            } else {
                networkInfo?.run {
                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> {
                            result = NetworkType.WIFI
                        }

                        ConnectivityManager.TYPE_MOBILE -> {
                            result = NetworkType.DATA
                        }

                        else -> {
                            result = NetworkType.NONE
                        }
                    }
                }
            }
            return result
        }

    }

    // java.net.networkinterface does not have .type etc.
//    fun test1(): String? {
//        try {
//            for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
//                for (inetAddress in Collections.list(networkInterface.inetAddresses)) {
//                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
//                        return inetAddress.hostAddress
//                    }
//                }
//            }
//        } catch (ex: SocketException) {
//            Log.e("IP Address", "Failed getting IP address", ex)
//        }
//        return null
//    }
}
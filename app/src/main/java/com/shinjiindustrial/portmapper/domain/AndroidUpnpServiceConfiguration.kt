package com.shinjiindustrial.portmapper.domain

import android.content.Context
import com.shinjiindustrial.portmapper.common.NetworkType
import org.fourthline.cling.android.AndroidNetworkAddressFactory
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.fourthline.cling.transport.spi.NetworkAddressFactory

class AndroidUpnpServiceConfigurationImpl(context : Context) : AndroidUpnpServiceConfiguration() {

    var Context : Context
    init {
        Context = context
    }

    override fun getServiceDescriptorBinderUDA10(): ServiceDescriptorBinder {
        return UDA10ServiceDescriptorBinderImpl()
    }

    // in case you want to get additional info from initialization
    // we can then maybe check if they support multicast (i.e. data typically does not)
    // and throw away.  also we can use connectionmanager to enumerate interfaces
    // by default this returns new AndroidNetworkAddressFactory
    // AndroidNetworkAddressFactory has a method discoverNetworkInterfaces() which
    //   calls the java NetworkInterface.getNetworkInterfaces();

    override fun createNetworkAddressFactory(streamListenPort: Int): NetworkAddressFactory? {
        //return NetworkAddressFactoryImpl(streamListenPort)
        val addressFactory = AndroidNetworkAddressFactory(streamListenPort)

        // the final set of usable interfaces and bind addresses
        val iterator = addressFactory.networkInterfaces
        val networkInterfaces: MutableList<java.net.NetworkInterface> = ArrayList()
        while (iterator.hasNext()) {
            networkInterfaces.add(iterator.next())
        }
        NetworkInterfacesUsed = networkInterfaces
        NetworkMappings = OurNetworkInfo.GetNameTypeMappings(Context)

        NetworkInterfacesUsedInfos = mutableListOf()
        for (netInterface in networkInterfaces)
        {
            NetworkInterfacesUsedInfos.add(NetworkInterfaceInfo(netInterface, OurNetworkInfo.GetTypeFromInterfaceName(NetworkMappings, netInterface.name)))
        }

        return addressFactory
    }

    private var NetworkInterfacesUsed : MutableList<java.net.NetworkInterface>? = null
    private var NetworkMappings : MutableMap<String, NetworkType>? = null
    var NetworkInterfacesUsedInfos : MutableList<NetworkInterfaceInfo> = mutableListOf()
}

data class NetworkInterfaceInfo(val networkInterface : java.net.NetworkInterface, val networkType : NetworkType)
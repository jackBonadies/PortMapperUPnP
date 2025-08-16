package com.shinjiindustrial.portmapper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fourthline.cling.support.igd.callback.GetExternalIP
import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import java.util.TreeSet
import java.util.UUID
import kotlin.system.measureTimeMillis


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UPnPManagerTests {

    @Test
    fun addingAndReplaceRules()
    {
        var igdDevice = IGDDevice(null, null)

        // if compare returns equals then they are considered the same and will be replaced in the TreeSet


        var portMappingsToAdd : MutableList<PortMapping> = mutableListOf()
        for (i in 0..65000) {
            portMappingsToAdd.add(generateRule())
        }


        val ss = TreeSet<PortMapping>(PortMapperComparatorExternalPort(true))
        val lookUpExisting : MutableMap<Pair<Int,String>, PortMapping> = mutableMapOf()
        for (pm in portMappingsToAdd)
        {
            val key = Pair<Int,String>(pm.ExternalPort, pm.Protocol)
            if(lookUpExisting.containsKey(key))
            {
                ss.remove(lookUpExisting[key])
            }
            lookUpExisting[key] = pm
            ss.add(pm)


            //val list = ss.toList()
            //print(list.count())
        }

        for (pm in ss)
        {
            print(pm.ExternalPort)
        }
    }

    @Test
    fun groupRules()
    {
        var igdDevice = IGDDevice(null, null)

        // if compare returns equals then they are considered the same and will be replaced in the TreeSet
        var groupByProtocol = false
        var groupByRange = true

        var portMappingsToAdd : MutableList<PortMapping> = mutableListOf()
        for (i in 0..65000) {
            portMappingsToAdd.add(generateRule())
        }
        val ss =
            TreeSet<PortMapping>(PortMapperComparatorExternalPort(true)) // ext port then protocol
        val timeInMillis = measureTimeMillis {

            val lookUpExisting: MutableMap<Pair<Int, String>, PortMapping> = mutableMapOf()
            for (pm in portMappingsToAdd) {
                val key = Pair<Int, String>(pm.ExternalPort, pm.Protocol)
                if (lookUpExisting.containsKey(key)) {
                    ss.remove(lookUpExisting[key])
                }
                lookUpExisting[key] = pm
                ss.add(pm)
            }

            println(ss.size)

            var curGroup = mutableListOf<PortMapping>()
            //tempPortMapping.remove
            for (pm in ss)
            {
                // start group
                if(curGroup.isEmpty())
                {
                    curGroup.add(pm)
                }
                else
                {
                    // add to group?
                    var addedToGroup = false
                    if(groupByRange)
                    {
                        curGroup.add(pm)
                        groupByRange = true
                    }

                    if(!addedToGroup)
                    {
                        //add previous group as UpnpView
                        //clear group
                        //add this mapping to group
                    }
                }

            }

            val ss2 =
                TreeSet<PortMapping>(PortMapperComparatorInternalPort(true)) // ext port then protocol
            ss2.addAll(ss)

            println(ss2.size)
        }

        println("TIME IN MILLISECONDS: $timeInMillis") //185

        for (pm in ss)
        {
            //println(pm.ExternalPort)
        }
    }



    fun generateRule(description : String? = null, externalIP : String? = null, externalPort : Int? = null, internalIP: String? = null, internalPort : Int? = null, leaseDuration : Int? = null) : PortMapping
    {    // Generate random values if necessary
        val _description = description ?: UUID.randomUUID().toString()
        val _externalIP = externalIP ?: "${Random().nextInt(256)}.${Random().nextInt(256)}.${Random().nextInt(256)}.${Random().nextInt(256)}"
        val _externalPort = externalPort ?: Random().nextInt(65535) // Port number is typically within 0-65535
        val _internalIP = internalIP ?: "${Random().nextInt(256)}.${Random().nextInt(256)}.${Random().nextInt(256)}.${Random().nextInt(256)}"
        val _internalPort = internalPort ?: Random().nextInt(65535) // Port number is typically within 0-65535
        val _leaseDuration = leaseDuration ?: Random().nextInt()

        val _protocol = "TCP"
        val _enabled = true

        return PortMapping(_description, "", _internalIP.toString(), _externalPort, _internalPort, _protocol, _enabled, _leaseDuration, _externalIP, 0, GetPsuedoSlot())
    }
}
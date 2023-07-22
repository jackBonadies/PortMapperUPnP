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


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UPnPManagerTests {
    @Test
    fun eventFlow() {

        var sharedVar = 0

        var msf : MutableSharedFlow<Any?> = MutableSharedFlow(extraBufferCapacity = 1,  onBufferOverflow = BufferOverflow.DROP_OLDEST)

//        GlobalScope.launch(Dispatchers.Default) {
//            msf.conflate().onEach {
//
//                println("performing the event for $sharedVar")
//                Thread.sleep(1000)
//            }.collect {}
//        }

        msf.conflate().onEach {

            println("performing the event for $sharedVar")
            Thread.sleep(1000)
        }.launchIn(CoroutineScope(Dispatchers.IO))//lifecycleScope) //shorthand for scope.launch { flow.collect() }

        GlobalScope.launch(Dispatchers.Default) {
            for(i in 0 until 2000)
            {
                java.lang.Thread.sleep(1)
                msf.emit(null)
                sharedVar += 1
            }

            java.lang.Thread.sleep(2000)
            sharedVar += 1
            msf.emit(null)

            println("done")
        }

        Thread.sleep(40000)



//        val eventFlow = MutableSharedFlow<Any>()
//        eventFlow.conflate().onEach{}
//
//        eventFlow.emit("Some event data")


//        val eventFlow = MutableSharedFlow<List<MyEvent>>() // Use your event type here
//
//// In your event handler...
//        eventFlow
//            .conflate() // Only process the most recent list of events
//            .onEach { eventList ->
//                // Handle the event here. This will only be called for the most recent list of events.
//            }
//            .launchIn(scope) // Launch this in a CoroutineScope. You might use the lifecycleScope in Android.
//
//// When you add an event to the list...
//        val newList = myList + newEvent // Add the new event to the list
//        eventFlow.emit(newList) // Emit the new list

    }
    @Test
    fun addingAndReplaceRules()
    {
        var igdDevice = IGDDevice(null, null)

        // if compare returns equals then they are considered the same and will be replaced in the TreeSet


        var portMappingsToAdd : MutableList<PortMapping> = mutableListOf()
        for (i in 0..65000) {
            portMappingsToAdd.add(generateRule())
        }


        val ss = TreeSet<PortMapping>(PortMapperComparatorExternalPort())
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

        return PortMapping(_description, _externalIP, _internalIP.toString(), _externalPort, _internalPort, _protocol, _enabled, _leaseDuration, _externalIP, 0)
    }
}
package com.shinjiindustrial.portmapper.common

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.domain.IGDDevice
import io.mockk.every
import io.mockk.mockkStatic
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ValidatorsTest {
    @Test
    fun `test description validation`() {
        assertFalse(validateDescription("Description is required").hasError)
        assertTrue(validateDescription("").hasError)
    }

    @Test
    fun `test start port validation`() {
        assertTrue(validateStartPort("").hasError)
        assertTrue(validateStartPort("70000").hasError)
        assertTrue(validateStartPort("-100").hasError)
        assertFalse(validateDescription("4000").hasError)
    }

    @Test
    fun `test end port validation`() {
        assertFalse(validateEndPort("", "").hasError)
        assertFalse(validateEndPort("1000", "").hasError)
        assertFalse(validateEndPort("1000", "").hasError)
        assertTrue(validateEndPort("1000", "70000").hasError)
        assertTrue(validateEndPort("1000", "-100").hasError)
        assertTrue(validateEndPort("1000", "100").hasError)
        assertFalse(validateEndPort("4000", "4040").hasError)
    }

    @Test
    fun `test validate internal ip`() {
        assertFalse(validateInternalIp("192.168.10.100").hasError)
        assertTrue(validateInternalIp("-192.168.10.100").hasError)
        assertTrue(validateInternalIp("-19216810100").hasError)
    }

    @Test
    fun `flow map and collect`() = runBlocking  {

        val _devices = MutableStateFlow(listOf<Int>())  // TreeSet<PortMapping>
        _devices//.map { it..sortedBy { d -> d.name } }
        _devices.update { it + 0 }
        val size_flow = _devices.map { it.size }.onEach { println("size is " + it) }
            //.also { println("size is " + it) }

        launch {
            size_flow.collect { it -> println(it) }
        }

        launch {

            while(true)
            {
                delay(100)
                _devices.update { it + 1 }
                _devices.update { it + 2 }
            }
        }

        delay(10000)

        val numbers = (1..5).asFlow()
        numbers.collect {
            println(it)
        }

        val doubled = numbers.map { it * 2 }.toList()
        delay(10000)

        assertEquals(listOf(2, 4, 6, 8, 10), doubled)
    }

    @Test
    fun `cold flow`() = runBlocking  {

        val flow = flow {
            while(true)
            {
                println("producing 1")
                emit(1)
                delay(1000)
                println("producing 2")
                emit(2)
                delay(1000)
                println("producing 3")
                emit(3)
                delay(1000)
            }

        }

        val intermediateFlow = flow//.map { it -> it * it }.onEach { println("intermediate " + it) }

        delay(5000)
        intermediateFlow.launchIn(this)
        val job = launch {
            intermediateFlow.collect() //{ it -> println("final " + it) }
        }
        launch {
            intermediateFlow.collect() //{ it -> println("final " + it) }
        }

        delay(5000)
        job.cancel()
        delay(15000)
    }

    @Test
    fun `client test`() = runBlocking  {
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Medium))

        launch {
            client.search(10)
            client.deviceFoundEvent += {
                println("device found " + it)
                GlobalScope.launch {
                    val index = 0
                    while(true)
                    {
                        try{
                            val result = client.getGenericPortMappingRule(it, index)
                            println("port found " + result.toString())
                        }
                        catch (e: Exception)
                        {
                            break
                        }
                    }
                }
            }
        }

        delay(50000)

    }

    @Test
    fun `repo test`() = runBlocking  {

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        //every { Log.w(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest))
        val repo = UpnpManager(client)
        GlobalScope.launch {
            repo.Search(false)
        }
        delay(60000)
        repo.GetAllRules()

    }

}
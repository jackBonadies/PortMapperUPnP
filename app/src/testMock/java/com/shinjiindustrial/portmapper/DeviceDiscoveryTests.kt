package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class DeviceDiscoveryTests {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
        unmockkStatic(SystemClock::class)
    }

    private fun createRepository(client: MockUpnpClient): UpnpRepository {
        val portMappingDao = mockk<PortMappingDao>(relaxed = true)
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any(), any(), any()) } returns null
        every { portMappingDao.observeAll() } returns MutableStateFlow(emptyList())
        val devicesDao = mockk<DevicesDao>(relaxed = true)
        coEvery { devicesDao.getByPrimaryKey(any()) } returns null
        every { devicesDao.observeAll() } returns MutableStateFlow(emptyList())
        val preferencesManager = mockk<PreferencesManager>()
        every { preferencesManager.showAllLocalRules } returns MutableStateFlow(false)
        return UpnpRepository(
            client, portMappingDao, devicesDao, mockk(relaxed = true), scope, preferencesManager
        )
    }

    // FRITZ!Box and others might have two IGD roots (IGD:1 and IGD:2, different UDNs) at one ip, and
    //   cling delivers each remoteDeviceAdded on its own thread leading to TOCTOU.
    @Test
    fun `two roots at one ip arriving concurrently add one device`() {
        repeat(20) {
            val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))
            val repository = createRepository(client)
            val roots = listOf(
                MockClingIGDDevice(DeviceDetails("FRITZ!Box IGD v1", "192.168.18.1", 1, "UUID-1")),
                MockClingIGDDevice(DeviceDetails("FRITZ!Box IGD v2", "192.168.18.1", 2, "UUID-2")),
            )
            val barrier = CyclicBarrier(roots.size)
            roots.map { root ->
                thread {
                    barrier.await()
                    client.deviceFoundEvent(root)
                }
            }.forEach { it.join() }

            assertEquals(1, repository.devices.value.size)
            assertEquals("192.168.18.1", repository.devices.value.single().getIpAddress())
        }
    }

    @Test
    fun `two routers at different ips are both added`() {
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))
        val repository = createRepository(client)

        client.deviceFoundEvent(MockClingIGDDevice(DeviceDetails("Nokia IGD v2", "192.168.18.1", 2, "UUID-1")))
        client.deviceFoundEvent(MockClingIGDDevice(DeviceDetails("Other IGD", "192.168.1.1", 1, "UUID-2")))

        assertEquals(listOf("192.168.1.1", "192.168.18.1"), repository.devices.value.map { it.getIpAddress() })
    }
}

package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPGetSpecificMappingResult
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class DisableEnableTests {

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

    private fun createRepository(): UpnpRepository {
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))
        val portMappingDao = mockk<PortMappingDao>()
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any(), any()) } returns null
        val devicesDao = mockk<DevicesDao>()
        coEvery { devicesDao.getByPrimaryKey(any(), any()) } returns null
        val repository =
            UpnpRepository(client, portMappingDao, devicesDao, mockk(relaxed = true), scope)
        client.deviceFoundEvent(
            MockClingIGDDevice(DeviceDetails("Nokia IGD v2", "192.168.18.1", 2, "UUID-1"))
        )
        return repository
    }

    private fun rule(description: String) = PortMappingWithPref(
        PortMapping(description, "", "192.168.1.13", 5015, 5015, "TCP", true, 3600, "192.168.18.1", 0, 1)
    )

    @Test
    fun `disable succeeds when router honors NewEnabled`() = runBlocking {
        val repository = createRepository()

        val res = repository.disableEnablePortMappingEntry(rule("Plain Rule"), false)

        assertTrue(res is UPnPCreateMappingWrapperResult.Success)
        res as UPnPCreateMappingWrapperResult.Success
        assertTrue(res.enabledMatchesRequest)
        assertFalse(res.resultingMapping.Enabled)
    }

    @Test
    fun `disable reports mismatch when router ignores NewEnabled`() = runBlocking {
        val repository = createRepository()

        // the NoDisableEnable marker makes the mock behave like miniupnpd: success but ignored
        val res = repository.disableEnablePortMappingEntry(rule("Rule NoDisableEnable"), false)

        assertTrue(res is UPnPCreateMappingWrapperResult.Success)
        res as UPnPCreateMappingWrapperResult.Success
        assertFalse(res.enabledMatchesRequest)
        assertTrue(res.resultingMapping.Enabled)
    }

    @Test
    fun `read back result carries enabled comparison`() {
        val readBackEnabled =
            PortMapping("r", "", "192.168.1.2", 80, 80, "TCP", true, 3600, "192.168.1.1", 0, 1)

        val mismatch = UPnPGetSpecificMappingResult.Success(readBackEnabled, readBackEnabled)
            .toCreatePortMappingResult(requestedEnabled = false)
        assertTrue(mismatch is UPnPCreateMappingWrapperResult.Success)
        assertFalse((mismatch as UPnPCreateMappingWrapperResult.Success).enabledMatchesRequest)

        val match = UPnPGetSpecificMappingResult.Success(readBackEnabled, readBackEnabled)
            .toCreatePortMappingResult(requestedEnabled = true)
        assertTrue((match as UPnPCreateMappingWrapperResult.Success).enabledMatchesRequest)
    }
}

package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class CreateRuleTests {

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
        val portMappingDao = mockk<PortMappingDao>(relaxed = true)
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any(), any(), any()) } returns null
        every { portMappingDao.observeAll() } returns MutableStateFlow(emptyList())
        val devicesDao = mockk<DevicesDao>(relaxed = true)
        coEvery { devicesDao.getByPrimaryKey(any()) } returns null
        every { devicesDao.observeAll() } returns MutableStateFlow(emptyList())
        val preferencesManager = mockk<PreferencesManager>()
        every { preferencesManager.showAllLocalRules } returns MutableStateFlow(false)
        val repository = UpnpRepository(
            client, portMappingDao, devicesDao, mockk(relaxed = true), scope, preferencesManager
        )
        client.deviceFoundEvent(
            MockClingIGDDevice(DeviceDetails("Nokia IGD v2", "192.168.18.1", 2, "UUID-1"))
        )
        return repository
    }

    private fun input(description: String, leaseDuration: String) = PortMappingUserInput(
        description = description,
        internalIp = "192.168.18.13",
        internalRange = "5015",
        externalIp = "192.168.18.1",
        externalRange = "5015",
        protocol = "TCP",
        leaseDuration = leaseDuration,
        enabled = true,
        autoRenew = false,
        autoRenewManualCadence = -1,
    )

    @Test
    fun `a rejected create with a blank lease is reported as Failure, not thrown`() = runBlocking {
        val repository = createRepository()

        val result = repository.createPortMappingRulesEntry(input("Rule NoCreate", ""))

        assertEquals(1, result.size)
        assertTrue(result[0] is UPnPCreateMappingWrapperResult.Failure)
    }

    @Test
    fun `a rejected create with a valid lease is reported as Failure`() = runBlocking {
        val repository = createRepository()

        val result = repository.createPortMappingRulesEntry(input("Rule NoCreate", "3600"))

        assertEquals(1, result.size)
        assertTrue(result[0] is UPnPCreateMappingWrapperResult.Failure)
    }
}

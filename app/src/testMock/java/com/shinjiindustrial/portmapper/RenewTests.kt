package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.PortMappingKey
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class RenewTests {

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

    @Test
    fun `renewing a selection only renews the selected rules`() = runBlocking {
        val repository = createRepository()
        val allRules = repository.getAllRules()
        assertTrue(allRules.size > 2)

        val selectedIds = allRules.take(2).map { it.getKey() }.toSet()
        val selectedRules = repository.portMappingsFromIds(selectedIds)
        assertEquals(selectedIds, selectedRules.map { it.getKey() }.toSet())

        // rules were read at time 0, renew them a minute later
        every { SystemClock.elapsedRealtime() } returns 60_000L
        val results = repository.renewRules(selectedRules)

        assertEquals(selectedIds.size, results.size)
        assertTrue(results.all { it is UPnPCreateMappingWrapperResult.Success })

        // the renewed rules restart their lease at the renew time, the rest are untouched
        val after = repository.portMappings.value
        assertEquals(allRules.size, after.size)
        for (rule in allRules) {
            val key = rule.getKey()
            val expectedReadTime = if (key in selectedIds) 60_000L else 0L
            assertEquals(expectedReadTime, after[key]!!.portMapping.TimeReadLeaseDurationMs)
            assertEquals(
                rule.portMapping.LeaseDuration,
                after[key]!!.portMapping.LeaseDuration
            )
        }
    }

    @Test
    fun `selection ids that no longer exist are skipped`() {
        val repository = createRepository()
        val existingId = repository.getAllRules().first().getKey()
        // ex. the rule was deleted between opening the overflow menu and tapping Renew
        val deletedId = PortMappingKey("192.168.18.1", 9999, "TCP")

        val rules = repository.portMappingsFromIds(setOf(existingId, deletedId))

        assertEquals(1, rules.size)
        assertEquals(existingId, rules.first().getKey())
    }

    @Test
    fun `renewing an empty selection does nothing`() = runBlocking {
        val repository = createRepository()

        val results = repository.renewRules(repository.portMappingsFromIds(emptySet()))

        assertTrue(results.isEmpty())
    }
}

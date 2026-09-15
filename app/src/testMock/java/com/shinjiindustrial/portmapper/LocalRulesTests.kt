package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.LocalRule
import com.shinjiindustrial.portmapper.domain.LocalRuleKey
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocalRulesTests {

    private companion object {
        const val UDN = "UUID-1"
        const val DEVICE_IP = "192.168.18.1"
    }

    private lateinit var scope: CoroutineScope
    private lateinit var entities: MutableStateFlow<List<PortMappingEntity>>

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

    private fun entity(
        description: String,
        externalPort: Int,
        protocol: String = "TCP",
        internalIp: String = "192.168.1.13",
        internalPort: Int = externalPort,
        createdAtUtcMs: Long? = 1_000L,
    ) = PortMappingEntity(
        deviceSignature = UDN,
        protocol = protocol,
        externalPort = externalPort,
        deviceIp = DEVICE_IP,
        description = description,
        internalIp = internalIp,
        internalPort = internalPort,
        autoRenew = true,
        desiredLeaseDuration = 3600,
        autoRenewManualCadence = -1,
        desiredEnabled = true,
        createdAtUtcMs = createdAtUtcMs,
        lastSeenAtUtcMs = createdAtUtcMs,
    )

    private fun PortMappingEntity.hasKey(udn: String, protocol: String, externalPort: Int) =
        this.deviceSignature == udn && this.protocol == protocol && this.externalPort == externalPort

    private fun createRepository(stored: List<PortMappingEntity>): UpnpRepository {
        entities = MutableStateFlow(stored)
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))
        val portMappingDao = mockk<PortMappingDao>(relaxed = true)
        every { portMappingDao.observeAll() } returns entities
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any()) } answers {
            entities.value.firstOrNull { it.hasKey(firstArg(), secondArg(), thirdArg()) }
        }
        coEvery { portMappingDao.upsert(any()) } answers {
            val upserted = firstArg<PortMappingEntity>()
            entities.update { list ->
                list.filterNot {
                    it.hasKey(upserted.deviceSignature, upserted.protocol, upserted.externalPort)
                } + upserted
            }
        }
        coEvery { portMappingDao.deleteByKey(any(), any(), any()) } answers {
            val before = entities.value.size
            entities.update { list ->
                list.filterNot { it.hasKey(firstArg(), secondArg(), thirdArg()) }
            }
            before - entities.value.size
        }
        val devicesDao = mockk<DevicesDao>(relaxed = true)
        coEvery { devicesDao.getByPrimaryKey(any()) } returns null
        val repository =
            UpnpRepository(client, portMappingDao, devicesDao, mockk(relaxed = true), scope)
        // enumerates inside runBlocking, so the device has FinishedEnumeratingMappings on return
        client.deviceFoundEvent(
            MockClingIGDDevice(DeviceDetails("Nokia IGD v2", DEVICE_IP, 2, UDN))
        )
        return repository
    }

    private fun UpnpRepository.awaitLocalRules(
        predicate: (Map<LocalRuleKey, LocalRule>) -> Boolean
    ): Map<LocalRuleKey, LocalRule> = runBlocking {
        withTimeout(2_000) { localRules.first(predicate) }
    }

    private fun key(externalPort: Int, protocol: String = "TCP") =
        LocalRuleKey(UDN, externalPort, protocol)

    @Test
    fun `rule the router no longer reports is local`() {
        val repository = createRepository(listOf(entity("Gone", 7777)))

        val local = repository.awaitLocalRules { it.containsKey(key(7777)) }

        val rule = local[key(7777)]!!
        assertFalse(rule.drifted)
        assertEquals("Gone", rule.entity.description)
        assertEquals(UDN, rule.device.udn)
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 7777, "TCP")])
    }

    @Test
    fun `rule migrated from v3 is ignored`() {
        val repository = createRepository(
            listOf(entity("Gone", 7777), entity("Pre v4", 8888, createdAtUtcMs = null))
        )

        val local = repository.awaitLocalRules { it.containsKey(key(7777)) }

        assertFalse(local.containsKey(key(8888)))
    }

    @Test
    fun `rule still on the router and unchanged is not local`() {
        // matches Demo store Minecraft Server row exactly
        val repository = createRepository(
            listOf(entity("Gone", 7777), entity("Minecraft Server", 5011))
        )

        val local = repository.awaitLocalRules { it.containsKey(key(7777)) }

        assertFalse(local.containsKey(key(5011)))
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]
        assertNotNull(onRouter)
        assertNotNull("rule should be ours", onRouter!!.portMappingPref)
    }

    @Test
    fun `rule changed out of band is local and drifted`() {
        // doesnt match Demo store Minecraft Server at TCP 5011
        val repository = createRepository(listOf(entity("Mine", 5011)))

        val local = repository.awaitLocalRules { it.containsKey(key(5011)) }

        assertTrue(local[key(5011)]!!.drifted)
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]
        assertNotNull(onRouter)
        assertEquals("Minecraft Server", onRouter!!.portMapping.Description)
        assertNull("router's version is not ours", onRouter.portMappingPref)
    }

    @Test
    fun `forget removes the local rule`() = runBlocking {
        val repository = createRepository(listOf(entity("Gone", 7777)))
        val rule = repository.awaitLocalRules { it.containsKey(key(7777)) }[key(7777)]!!

        repository.forgetLocalRule(rule)

        repository.awaitLocalRules { !it.containsKey(key(7777)) }
        assertTrue(entities.value.none { it.hasKey(UDN, "TCP", 7777) })
    }

    @Test
    fun `recreate puts the rule back on the router and keeps its creation time`() = runBlocking {
        val repository = createRepository(listOf(entity("Gone", 7777, createdAtUtcMs = 1_000L)))
        val rule = repository.awaitLocalRules { it.containsKey(key(7777)) }[key(7777)]!!

        val res = repository.recreateLocalRule(rule)

        assertTrue(res is UPnPCreateMappingWrapperResult.Success)
        repository.awaitLocalRules { !it.containsKey(key(7777)) }
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 7777, "TCP")]
        assertNotNull(onRouter)
        assertEquals("Gone", onRouter!!.portMapping.Description)
        assertNotNull("recreated rule should be ours", onRouter.portMappingPref)
        val stored = entities.value.first { it.hasKey(UDN, "TCP", 7777) }
        assertEquals(1_000L, stored.createdAtUtcMs)
        assertTrue(stored.lastSeenAtUtcMs!! > 1_000L)
    }
}

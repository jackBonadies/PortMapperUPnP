package com.shinjiindustrial.portmapper

import android.os.SystemClock
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.DeviceStatus
import com.shinjiindustrial.portmapper.domain.LocalRule
import com.shinjiindustrial.portmapper.domain.LocalRuleKey
import com.shinjiindustrial.portmapper.domain.LocalRuleStatus
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.hasSlotConflict
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
        const val LAPTOP_A = "192.168.1.13"
        const val LAPTOP_B = "192.168.1.14"
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
        internalIp: String = LAPTOP_A,
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

    private fun PortMappingEntity.hasKey(
        udn: String,
        protocol: String,
        externalPort: Int,
        internalIp: String,
        internalPort: Int
    ) = this.deviceSignature == udn && this.protocol == protocol &&
            this.externalPort == externalPort && this.internalIp == internalIp &&
            this.internalPort == internalPort

    private fun PortMappingEntity.hasKey(key: LocalRuleKey) =
        hasKey(key.udn, key.protocol, key.externalPort, key.internalIp, key.internalPort)

    private fun createRepository(stored: List<PortMappingEntity>): UpnpRepository {
        entities = MutableStateFlow(stored)
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, RuleSet.Demo))
        val portMappingDao = mockk<PortMappingDao>(relaxed = true)
        every { portMappingDao.observeAll() } returns entities
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any(), any(), any()) } answers {
            entities.value.firstOrNull {
                it.hasKey(arg(0), arg(1), arg(2), arg(3), arg(4))
            }
        }
        coEvery { portMappingDao.upsert(any()) } answers {
            val upserted = firstArg<PortMappingEntity>()
            entities.update { list ->
                list.filterNot {
                    it.hasKey(
                        upserted.deviceSignature,
                        upserted.protocol,
                        upserted.externalPort,
                        upserted.internalIp,
                        upserted.internalPort
                    )
                } + upserted
            }
        }
        coEvery { portMappingDao.deleteByKey(any(), any(), any(), any(), any()) } answers {
            val before = entities.value.size
            entities.update { list ->
                list.filterNot { it.hasKey(arg(0), arg(1), arg(2), arg(3), arg(4)) }
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

    private fun key(
        externalPort: Int,
        protocol: String = "TCP",
        internalIp: String = LAPTOP_A,
        internalPort: Int = externalPort,
    ) = LocalRuleKey(UDN, externalPort, protocol, internalIp, internalPort)

    private fun userInput(
        description: String,
        externalPort: Int,
        internalIp: String,
        internalPort: Int = externalPort,
    ) = PortMappingUserInput(
        description = description,
        internalIp = internalIp,
        internalRange = internalPort.toString(),
        externalIp = DEVICE_IP,
        externalRange = externalPort.toString(),
        protocol = "TCP",
        leaseDuration = "3600",
        enabled = true,
        autoRenew = true,
        autoRenewManualCadence = -1,
    )

    @Test
    fun `device is stamped with a wall clock refresh time once enumerated`() {
        val before = System.currentTimeMillis()
        val repository = createRepository(emptyList())

        val device = repository.devices.value.single()
        assertEquals(DeviceStatus.FinishedEnumeratingMappings, device.status)
        val enumeratedAt = device.enumeratedAtUtcMs
        assertNotNull(enumeratedAt)
        assertTrue(enumeratedAt!! >= before && enumeratedAt <= System.currentTimeMillis())
    }

    @Test
    fun `rule the router no longer reports is local`() {
        val repository = createRepository(listOf(entity("Gone", 7777)))

        val local = repository.awaitLocalRules { it.containsKey(key(7777)) }

        val rule = local[key(7777)]!!
        assertEquals(LocalRuleStatus.Missing, rule.status)
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
        // same target as Demo store Minecraft Server at TCP 5011, different description.  the
        //   row is found by key, so this is exactly the case matches() is left to catch.
        val repository = createRepository(listOf(entity("Mine", 5011)))

        val local = repository.awaitLocalRules { it.containsKey(key(5011)) }

        assertEquals(LocalRuleStatus.Drifted, local[key(5011)]!!.status)
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
        assertTrue(entities.value.none { it.hasKey(key(7777)) })
    }

    @Test
    fun `activate puts the rule back on the router and keeps its creation time`() = runBlocking {
        val repository = createRepository(listOf(entity("Gone", 7777, createdAtUtcMs = 1_000L)))
        val rule = repository.awaitLocalRules { it.containsKey(key(7777)) }[key(7777)]!!

        val res = repository.activateLocalRule(rule)

        assertTrue(res is UPnPCreateMappingWrapperResult.Success)
        repository.awaitLocalRules { !it.containsKey(key(7777)) }
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 7777, "TCP")]
        assertNotNull(onRouter)
        assertEquals("Gone", onRouter!!.portMapping.Description)
        assertNotNull("activated rule should be ours", onRouter.portMappingPref)
        val stored = entities.value.first { it.hasKey(key(7777)) }
        assertEquals(1_000L, stored.createdAtUtcMs)
        assertTrue(stored.lastSeenAtUtcMs!! > 1_000L)
    }

    @Test
    fun `deactivate takes our rule off the router and keeps its creation time`() = runBlocking {
        // matches Demo store Minecraft Server row exactly
        val repository = createRepository(listOf(entity("Minecraft Server", 5011, createdAtUtcMs = 1_000L)))
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!
        assertNotNull("precondition: rule is ours", onRouter.portMappingPref)

        val res = repository.deactivatePortMappingEntry(onRouter)

        assertTrue(res is UPnPResult.Success)
        val local = repository.awaitLocalRules { it.containsKey(key(5011)) }
        assertEquals(LocalRuleStatus.Missing, local[key(5011)]!!.status)
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")])
        val stored = entities.value.single { it.hasKey(key(5011)) }
        assertEquals(1_000L, stored.createdAtUtcMs)
        assertEquals("Minecraft Server", stored.description)
    }

    @Test
    fun `deactivate adopts a rule that is not ours`() = runBlocking {
        val before = System.currentTimeMillis()
        val repository = createRepository(emptyList())
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 8080, "TCP")]!!
        assertNull("precondition: rule is not ours", onRouter.portMappingPref)

        val res = repository.deactivatePortMappingEntry(onRouter)

        assertTrue(res is UPnPResult.Success)
        repository.awaitLocalRules { it.containsKey(key(8080, internalIp = "192.168.1.18")) }
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 8080, "TCP")])
        val stored = entities.value.single { it.hasKey(key(8080, internalIp = "192.168.1.18")) }
        assertEquals("Web Server 1", stored.description)
        assertEquals("192.168.1.18", stored.internalIp)
        assertEquals(18 * 3600, stored.desiredLeaseDuration)
        assertTrue(stored.createdAtUtcMs!! >= before)
    }

    // two of our rules for the same external port, different internal target

    @Test
    fun `sibling of a rule that is on the router is local and not drifted`() {
        // laptop A matches the Demo store Minecraft Server row; laptop B is the alternative
        val repository = createRepository(
            listOf(
                entity("Minecraft Server", 5011, internalIp = LAPTOP_A),
                entity("Minecraft Server", 5011, internalIp = LAPTOP_B),
            )
        )

        val local = repository.awaitLocalRules { it.containsKey(key(5011, internalIp = LAPTOP_B)) }

        assertEquals(LocalRuleStatus.SiblingActive, local[key(5011, internalIp = LAPTOP_B)]!!.status)
        assertFalse(local.containsKey(key(5011, internalIp = LAPTOP_A)))
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!
        assertEquals(LAPTOP_A, onRouter.portMapping.InternalIP)
        assertNotNull("router's version is ours", onRouter.portMappingPref)
    }

    @Test
    fun `activating a sibling swaps which rule is on the router`() = runBlocking {
        val repository = createRepository(
            listOf(
                entity("Minecraft Server", 5011, internalIp = LAPTOP_A, createdAtUtcMs = 1_000L),
                entity("Minecraft Server", 5011, internalIp = LAPTOP_B, createdAtUtcMs = 2_000L),
            )
        )
        val ruleB = repository.awaitLocalRules {
            it.containsKey(key(5011, internalIp = LAPTOP_B))
        }[key(5011, internalIp = LAPTOP_B)]!!

        val res = repository.activateLocalRule(ruleB)

        assertTrue(res is UPnPCreateMappingWrapperResult.Success)
        val local = repository.awaitLocalRules { it.containsKey(key(5011, internalIp = LAPTOP_A)) }
        assertEquals(LocalRuleStatus.SiblingActive, local[key(5011, internalIp = LAPTOP_A)]!!.status)
        assertFalse(local.containsKey(key(5011, internalIp = LAPTOP_B)))
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!
        assertEquals(LAPTOP_B, onRouter.portMapping.InternalIP)
        assertNotNull("activated rule should be ours", onRouter.portMappingPref)
        // both rows survive with their own creation times
        assertEquals(1_000L, entities.value.single { it.hasKey(key(5011, internalIp = LAPTOP_A)) }.createdAtUtcMs)
        assertEquals(2_000L, entities.value.single { it.hasKey(key(5011, internalIp = LAPTOP_B)) }.createdAtUtcMs)
    }

    @Test
    fun `creating a rule at a port we already have adds a sibling with its own creation time`() = runBlocking {
        val before = System.currentTimeMillis()
        val repository = createRepository(
            listOf(entity("Gone", 7777, internalIp = LAPTOP_A, createdAtUtcMs = 1_000L))
        )
        repository.awaitLocalRules { it.containsKey(key(7777, internalIp = LAPTOP_A)) }

        val results = repository.createPortMappingRulesEntry(userInput("Gone", 7777, LAPTOP_B))

        assertTrue(results.single() is UPnPCreateMappingWrapperResult.Success)
        val local = repository.awaitLocalRules {
            it[key(7777, internalIp = LAPTOP_A)]?.status == LocalRuleStatus.SiblingActive
        }
        assertFalse(local.containsKey(key(7777, internalIp = LAPTOP_B)))
        val storedA = entities.value.single { it.hasKey(key(7777, internalIp = LAPTOP_A)) }
        val storedB = entities.value.single { it.hasKey(key(7777, internalIp = LAPTOP_B)) }
        assertEquals(1_000L, storedA.createdAtUtcMs)
        assertTrue("new row must not inherit the sibling's creation time", storedB.createdAtUtcMs!! >= before)
    }

    @Test
    fun `deactivating the router's rule at a drifted port keeps our drifted rule`() = runBlocking {
        // ours points at laptop B; the router (Demo store) has Minecraft Server -> laptop A
        val repository = createRepository(
            listOf(entity("Minecraft Server", 5011, internalIp = LAPTOP_B, createdAtUtcMs = 1_000L))
        )
        val ours = key(5011, internalIp = LAPTOP_B)
        val adopted = key(5011, internalIp = LAPTOP_A)
        assertEquals(
            LocalRuleStatus.Drifted,
            repository.awaitLocalRules { it.containsKey(ours) }[ours]!!.status
        )
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!
        assertNull("precondition: router's rule is not ours", onRouter.portMappingPref)

        val res = repository.deactivatePortMappingEntry(onRouter)

        assertTrue(res is UPnPResult.Success)
        val local = repository.awaitLocalRules { it.containsKey(adopted) }
        assertEquals(LocalRuleStatus.Missing, local[adopted]!!.status)
        assertEquals(LocalRuleStatus.Missing, local[ours]!!.status)
        assertEquals(1_000L, entities.value.single { it.hasKey(ours) }.createdAtUtcMs)
        assertEquals(LAPTOP_A, entities.value.single { it.hasKey(adopted) }.internalIp)
    }

    @Test
    fun `deleting an unmanaged rule at a drifted port keeps our drifted rule`() = runBlocking {
        val repository = createRepository(
            listOf(entity("Minecraft Server", 5011, internalIp = LAPTOP_B))
        )
        val ours = key(5011, internalIp = LAPTOP_B)
        repository.awaitLocalRules { it[ours]?.status == LocalRuleStatus.Drifted }
        val onRouter = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!

        val res = repository.deletePortMappingEntry(onRouter)

        assertTrue(res is UPnPResult.Success)
        val local = repository.awaitLocalRules { it[ours]?.status == LocalRuleStatus.Missing }
        assertEquals(1, local.keys.count { it.externalPort == 5011 })
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")])
        assertTrue(entities.value.any { it.hasKey(ours) })
    }

    // multi select

    @Test
    fun `activate all puts every selected local rule on the router`() = runBlocking {
        val repository = createRepository(
            listOf(entity("Gone A", 7777, createdAtUtcMs = 1_000L), entity("Gone B", 7778, createdAtUtcMs = 2_000L))
        )
        val local = repository.awaitLocalRules {
            it.containsKey(key(7777)) && it.containsKey(key(7778))
        }
        val selected = repository.localRulesFromIds(setOf(key(7777), key(7778)))
        assertEquals(2, selected.size)

        val results = repository.activateLocalRules(selected)

        assertEquals(2, results.size)
        assertTrue(results.all { it is UPnPCreateMappingWrapperResult.Success })
        repository.awaitLocalRules { !it.containsKey(key(7777)) && !it.containsKey(key(7778)) }
        assertNotNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 7777, "TCP")]?.portMappingPref)
        assertNotNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 7778, "TCP")]?.portMappingPref)
        assertEquals(1_000L, entities.value.single { it.hasKey(key(7777)) }.createdAtUtcMs)
        assertEquals(2_000L, entities.value.single { it.hasKey(key(7778)) }.createdAtUtcMs)
    }

    @Test
    fun `deactivate all moves every selected router rule to local`() = runBlocking {
        val repository = createRepository(emptyList())
        val minecraft = repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")]!!
        val web = repository.portMappings.value[PortMappingKey(DEVICE_IP, 8080, "TCP")]!!

        val results = repository.deactivatePortMappingEntries(listOf(minecraft, web))

        assertEquals(2, results.size)
        assertTrue(results.all { it is UPnPResult.Success })
        val local = repository.awaitLocalRules {
            it.containsKey(key(5011)) && it.containsKey(key(8080, internalIp = "192.168.1.18"))
        }
        assertEquals(LocalRuleStatus.Missing, local[key(5011)]!!.status)
        assertEquals(LocalRuleStatus.Missing, local[key(8080, internalIp = "192.168.1.18")]!!.status)
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 5011, "TCP")])
        assertNull(repository.portMappings.value[PortMappingKey(DEVICE_IP, 8080, "TCP")])
    }

    @Test
    fun `forget all removes every selected local rule and nothing else`() = runBlocking {
        val repository = createRepository(
            listOf(entity("Gone A", 7777), entity("Gone B", 7778), entity("Kept", 7779))
        )
        repository.awaitLocalRules { it.size == 3 }

        repository.forgetLocalRules(repository.localRulesFromIds(setOf(key(7777), key(7778))))

        val local = repository.awaitLocalRules { it.size == 1 }
        assertTrue(local.containsKey(key(7779)))
        assertEquals(listOf("Kept"), entities.value.map { it.description })
    }

    @Test
    fun `local rules from ids skips a key that is no longer local`() {
        val repository = createRepository(listOf(entity("Gone", 7777)))
        repository.awaitLocalRules { it.containsKey(key(7777)) }

        val found = repository.localRulesFromIds(setOf(key(7777), key(9999)))

        assertEquals(listOf(key(7777)), found.map { it.key })
    }

    @Test
    fun `two selected local rules at the same slot conflict`() {
        // same ext port + protocol, different internal target: only one can be on the router
        assertTrue(listOf(key(5011, internalIp = LAPTOP_A), key(5011, internalIp = LAPTOP_B)).hasSlotConflict())
        // different protocol is a different slot
        assertFalse(listOf(key(5011, protocol = "TCP"), key(5011, protocol = "UDP")).hasSlotConflict())
        assertFalse(listOf(key(5011), key(5012)).hasSlotConflict())
        assertFalse(emptyList<LocalRuleKey>().hasSlotConflict())
    }
}

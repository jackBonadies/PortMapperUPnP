package com.shinjiindustrial.portmapper

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import com.shinjiindustrial.portmapper.background.BackgroundRenewer
import com.shinjiindustrial.portmapper.background.RenewScheduler
import com.shinjiindustrial.portmapper.background.backgroundRenewDeadline
import com.shinjiindustrial.portmapper.client.MockClingIGDDevice
import com.shinjiindustrial.portmapper.client.MockUpnpClient
import com.shinjiindustrial.portmapper.client.MockUpnpClientConfig
import com.shinjiindustrial.portmapper.client.RuleSet
import com.shinjiindustrial.portmapper.client.Speed
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.DeviceStatus
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.persistence.DevicesDao
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

// the alarm-driven renew path (background/). the timer loop in UpnpRepository is untouched by
//   these and has its own tests in RenewTests.
class BackgroundRenewTests {

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

    // region RenewScheduler

    private class SchedulerFixture(sdkInt: Int) {
        val alarmManager = mockk<AlarmManager>(relaxed = true)
        val operation = mockk<PendingIntent>()
        val scheduler = RenewScheduler(alarmManager, { operation }, sdkInt, mockk(relaxed = true))
    }

    @Test
    fun `margin is 5 percent of the lease with a 30 s floor`() {
        assertEquals(180_000L, RenewScheduler.marginMs(3600))
        assertEquals(4_320_000L, RenewScheduler.marginMs(24 * 3600))
        assertEquals(30_000L, RenewScheduler.marginMs(600))
        assertEquals(30_000L, RenewScheduler.marginMs(90))
    }

    @Test
    fun `the inexact window closes at the deadline`() {
        // one hour lease: renew by 57 min
        val untilDeadline = 3600_000L - RenewScheduler.marginMs(3600)
        val delay = RenewScheduler.triggerDelay(untilDeadline, 31)

        // AlarmManagerService.maxTriggerTime: window = 0.75 x futurity, delivered at window end
        val windowEnd = delay + (0.75 * delay).toLong()
        assertTrue(abs(untilDeadline - windowEnd) < 1_000)
        // the one hour cap does not bind at this length
        assertEquals(delay, RenewScheduler.triggerDelay(untilDeadline, 33))
    }

    @Test
    fun `long leases use the one hour window cap from T`() {
        val untilDeadline = 24 * 3600_000L - RenewScheduler.marginMs(24 * 3600)

        // before T: /1.75 only, conservative
        assertEquals((untilDeadline / 1.75).toLong(), RenewScheduler.triggerDelay(untilDeadline, 32))
        // from T the window is at most an hour, so trigger an hour before the deadline
        assertEquals(untilDeadline - AlarmManager.INTERVAL_HOUR, RenewScheduler.triggerDelay(untilDeadline, 33))
    }

    @Test
    fun `short and past deadlines are floored`() {
        assertEquals(RenewScheduler.MIN_DELAY_MS, RenewScheduler.triggerDelay(60_000L, 33))
        assertEquals(RenewScheduler.MIN_DELAY_MS, RenewScheduler.triggerDelay(-60_000L, 33))
    }

    @Test
    fun `scheduleBy arms an inexact allow-while-idle alarm`() {
        val f = SchedulerFixture(sdkInt = 34)
        val deadline = 3600_000L

        val triggerAt = f.scheduler.scheduleBy(deadline)

        assertEquals(RenewScheduler.triggerDelay(deadline, 34), triggerAt)
        verify(exactly = 1) {
            f.alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, f.operation)
        }
        verify(exactly = 0) { f.alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { f.alarmManager.setExact(any(), any(), any()) }
    }

    @Test
    fun `before M there is no allow-while-idle`() {
        val f = SchedulerFixture(sdkInt = 22)

        f.scheduler.scheduleBy(3600_000L)

        verify(exactly = 1) { f.alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, any(), f.operation) }
    }

    @Test
    fun `cancel drops the alarm`() {
        val f = SchedulerFixture(sdkInt = 34)

        f.scheduler.cancel()

        verify(exactly = 1) { f.alarmManager.cancel(f.operation) }
    }

    // endregion

    // region rows and repository

    private val udn = "UUID-1"
    private val deviceIp = "192.168.18.1"

    // rows of ours for the Demo rule set, auto-renew on
    private fun demoRow(
        description: String,
        externalPort: Int,
        internalPort: Int,
        lease: Int,
        autoRenew: Boolean = true,
        lastSeenAtUtcMs: Long? = 1L,
    ) = PortMappingEntity(
        deviceSignature = udn,
        protocol = "TCP",
        externalPort = externalPort,
        deviceIp = deviceIp,
        description = description,
        internalIp = "192.168.1.13",
        internalPort = internalPort,
        autoRenew = autoRenew,
        desiredLeaseDuration = lease,
        autoRenewManualCadence = -1,
        desiredEnabled = true,
        createdAtUtcMs = 1L,
        lastSeenAtUtcMs = lastSeenAtUtcMs,
    )

    private fun shareDriveRow(autoRenew: Boolean = true, lastSeenAtUtcMs: Long? = 1L) =
        demoRow("ShareDrive", 4044, 4044, 2 * 3600, autoRenew, lastSeenAtUtcMs)

    private fun gameHostRow() = demoRow("Game Host 1", 50345, 4000, 1800)

    private fun minecraftRow() = demoRow("Minecraft Server", 5011, 5011, 24 * 3600)

    private class RepositoryFixture(
        val repository: UpnpRepository,
        val client: MockUpnpClient,
        val portMappingDao: PortMappingDao,
    )

    private fun createRepository(
        rows: List<PortMappingEntity>,
        ruleSet: RuleSet = RuleSet.Demo,
    ): RepositoryFixture {
        val client = MockUpnpClient(MockUpnpClientConfig(Speed.Fastest, ruleSet))
        val portMappingDao = mockk<PortMappingDao>(relaxed = true)
        coEvery { portMappingDao.getByPrimaryKey(any(), any(), any(), any(), any()) } returns null
        for (row in rows) {
            coEvery {
                portMappingDao.getByPrimaryKey(
                    row.deviceSignature, row.protocol, row.externalPort, row.internalIp, row.internalPort
                )
            } returns row
        }
        every { portMappingDao.observeAll() } returns MutableStateFlow(rows)
        val devicesDao = mockk<DevicesDao>(relaxed = true)
        coEvery { devicesDao.getByPrimaryKey(any()) } returns null
        every { devicesDao.observeAll() } returns MutableStateFlow(emptyList())
        val preferencesManager = mockk<PreferencesManager>()
        every { preferencesManager.showAllLocalRules } returns MutableStateFlow(false)
        val repository = UpnpRepository(
            client, portMappingDao, devicesDao, mockk(relaxed = true), scope, preferencesManager
        )
        return RepositoryFixture(repository, client, portMappingDao)
    }

    private fun RepositoryFixture.discoverDemo() {
        client.deviceFoundEvent(MockClingIGDDevice(DeviceDetails("Nokia IGD v2", deviceIp, 2, udn)))
    }

    private fun RepositoryFixture.rule(externalPort: Int): PortMappingWithPref {
        return repository.portMappings.value.values.single { it.portMapping.ExternalPort == externalPort }
    }

    // endregion

    // region backgroundRenewDeadline

    @Test
    fun `deadline is the soonest live rule's expiry less its margin`() {
        val f = createRepository(listOf(shareDriveRow(), gameHostRow()))
        f.discoverDemo()

        val deadline = backgroundRenewDeadline(
            f.repository.devices.value, f.repository.portMappings.value, emptyList(), 0L, 0L
        )

        // read at 0: Game Host (30 min) beats ShareDrive (2 h). 5% of 30 min = 90 s
        assertEquals(1800_000L - 90_000L, deadline)
    }

    @Test
    fun `rules that are not ours, not auto-renew or disabled do not count`() = runBlocking {
        val f = createRepository(listOf(shareDriveRow(autoRenew = false), gameHostRow()))
        f.discoverDemo()
        f.repository.disableEnablePortMappingEntry(f.rule(50345), false)

        val deadline = backgroundRenewDeadline(
            f.repository.devices.value, f.repository.portMappings.value, emptyList(), 0L, 0L
        )

        // Minecraft and Web Server are not ours, ShareDrive has auto-renew off, Game Host is disabled
        assertNull(deadline)
    }

    @Test
    fun `an enumerated router's stored rows do not count, only what it reports`() {
        // ours with auto-renew and seen just now, but the router no longer has it
        val gone = demoRow("Gone", 7777, 7777, 3600, lastSeenAtUtcMs = 1_000_000L)
        val f = createRepository(listOf(gone))
        f.discoverDemo()

        val deadline = backgroundRenewDeadline(
            f.repository.devices.value, f.repository.portMappings.value, listOf(gone), 0L, 1_000_000L
        )

        assertNull(deadline)
    }

    @Test
    fun `a router not enumerated falls back to stored rows seen within one lease`() {
        val nowUtc = 10 * 3600_000L
        val nowElapsed = 5_000L
        val seenAnHourAgo = shareDriveRow(lastSeenAtUtcMs = nowUtc - 3600_000L)
        val seenThreeHoursAgo = gameHostRow().copy(desiredLeaseDuration = 2 * 3600, lastSeenAtUtcMs = nowUtc - 3 * 3600_000L)
        val neverSeen = minecraftRow().copy(lastSeenAtUtcMs = null)
        val autoRenewOff = shareDriveRow(autoRenew = false, lastSeenAtUtcMs = nowUtc).copy(externalPort = 4045)
        val noDevices = emptyList<IIGDDevice>()
        val noMappings = emptyMap<PortMappingKey, PortMappingWithPref>()

        val deadline = backgroundRenewDeadline(
            noDevices, noMappings, listOf(seenAnHourAgo, seenThreeHoursAgo, neverSeen, autoRenewOff), nowElapsed, nowUtc
        )

        // 2 h lease seen 1 h ago: at most 1 h left, less 5% of 2 h
        assertEquals(nowElapsed + 3600_000L - 360_000L, deadline)

        // a 2 h lease seen 3 h ago has certainly lapsed: nothing left to protect
        assertNull(
            backgroundRenewDeadline(noDevices, noMappings, listOf(seenThreeHoursAgo, neverSeen, autoRenewOff), nowElapsed, nowUtc)
        )
    }

    // endregion

    // region UpnpRepository.renewLiveAutoRenewRules

    @Test
    fun `renews only live rules and does not resurrect one that fell off`() = runBlocking {
        val f = createRepository(listOf(shareDriveRow(), gameHostRow(), minecraftRow()))
        f.discoverDemo()
        // Game Host disabled, Minecraft removed from the router out of band
        f.repository.disableEnablePortMappingEntry(f.rule(50345), false)
        f.client.deletePortMapping(f.repository.getIGDDevice(deviceIp), f.rule(5011).portMapping)

        every { SystemClock.elapsedRealtime() } returns 600_000L
        val renewed = f.repository.renewLiveAutoRenewRules()

        // ShareDrive only (Web Server is not ours)
        assertEquals(1, renewed)
        assertTrue(f.repository.portMappings.value.values.none { it.portMapping.ExternalPort == 5011 })
        assertEquals(false, f.rule(50345).portMapping.Enabled)
        // the renew stamps lastSeen, which the cold-start estimate relies on
        coVerify {
            f.portMappingDao.markSeen(udn, "TCP", 4044, "192.168.1.13", 4044, any(), deviceIp)
        }
    }

    @Test
    fun `discovers the router first when the process knows nothing`() = runBlocking {
        // cold process woken by the alarm: no search has run, the in-memory list is empty
        val f = createRepository(listOf(shareDriveRow()))
        assertTrue(f.repository.devices.value.isEmpty())

        val renewed = f.repository.renewLiveAutoRenewRules()

        assertEquals(1, renewed)
        assertEquals(1, f.repository.devices.value.size)
        assertTrue(f.repository.hasSearched)
        assertTrue(f.rule(4044).getAutoRenewOrDefault())
    }

    private val mainUdn = "uuid:2f402f80-da50-11e1-9b23-0017881c4e33"

    // RuleSet.Full's IGD Main: Rule i = (TCP if even else UDP) 5000+i -> 192.168.1.(i%2):6000+i
    private fun fullRow(i: Int, description: String) = shareDriveRow().copy(
        deviceSignature = mainUdn, deviceIp = "192.168.1.1", externalPort = 5000 + i,
        protocol = if (i % 2 == 0) "TCP" else "UDP",
        description = description, internalIp = "192.168.1.${i % 2}", internalPort = 6000 + i,
    )

    // one test for both because a RuleSet.Full enumeration costs ~7 s
    @Test
    fun `waits for a late second router, and a rule that throws does not stop the rest`() = runBlocking {
        // RuleSet.Full: an empty IGD answers first and finishes enumerating instantly, the one
        //   with the rules only shows up afterwards. "all devices finished" must not fire on the
        //   first alone.
        //   the mock throws on create for "NoCreate Exp"; Rule 13 is read before Rule 14
        val f = createRepository(
            listOf(fullRow(13, "Rule NoCreate Exp 13"), fullRow(14, "Rule NoDelete 14")),
            RuleSet.Full
        )

        val renewed = f.repository.renewLiveAutoRenewRules()

        assertEquals(1, renewed)
        // Rule 13 really was a candidate, so the 1 is Rule 14 after it threw
        assertTrue(f.rule(5013).getAutoRenewOrDefault())
        assertEquals(3, f.repository.devices.value.size)
        assertTrue(f.repository.devices.value.all { it.status == DeviceStatus.FinishedEnumeratingMappings })
    }

    // endregion

    // region BackgroundRenewer

    private class RenewerFixture(
        rows: List<PortMappingEntity>,
        scope: CoroutineScope,
        enabled: Boolean = true,
        onLan: Boolean = true,
        foreground: Boolean = false,
    ) {
        val repository = mockk<UpnpRepository>()
        val scheduler = mockk<RenewScheduler>(relaxed = true)
        val dao = mockk<PortMappingDao>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>()
        val enabledFlow = MutableStateFlow(enabled)
        val renewer: BackgroundRenewer

        init {
            every { repository.devices } returns MutableStateFlow(emptyList())
            every { repository.portMappings } returns MutableStateFlow(emptyMap())
            coEvery { repository.renewLiveAutoRenewRules() } returns 2
            every { dao.observeAll() } returns MutableStateFlow(rows)
            coEvery { dao.getAll() } returns rows
            every { preferencesManager.backgroundRenew } returns enabledFlow
            renewer = BackgroundRenewer(
                repository, dao, preferencesManager, scheduler, mockk(relaxed = true), scope,
                { onLan }, { foreground }, debounceMs = 0L
            )
        }
    }

    // a rule of ours seen a minute ago: might still be on the router
    private fun recentRow() = shareDriveRow(lastSeenAtUtcMs = System.currentTimeMillis() - 60_000L)

    @Test
    fun `arms at a cold start from the stored rows`() {
        val f = RenewerFixture(listOf(recentRow()), scope)

        verify(exactly = 1) { f.scheduler.scheduleBy(any()) }
        verify(exactly = 0) { f.scheduler.cancel() }
    }

    @Test
    fun `cancels when nothing might still be on the router`() {
        val lapsed = shareDriveRow(lastSeenAtUtcMs = System.currentTimeMillis() - 3 * 3600_000L)
        val f = RenewerFixture(listOf(lapsed), scope)

        verify(exactly = 0) { f.scheduler.scheduleBy(any()) }
        verify(exactly = 1) { f.scheduler.cancel() }
    }

    @Test
    fun `the setting arms and cancels`() {
        val f = RenewerFixture(listOf(recentRow()), scope, enabled = false)
        verify(exactly = 0) { f.scheduler.scheduleBy(any()) }
        verify(exactly = 1) { f.scheduler.cancel() }

        f.enabledFlow.value = true
        verify(exactly = 1) { f.scheduler.scheduleBy(any()) }

        f.enabledFlow.value = false
        verify(exactly = 2) { f.scheduler.cancel() }
    }

    @Test
    fun `an alarm renews and re-arms`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope)

        val renewed = f.renewer.renewAll()

        assertEquals(2, renewed)
        coVerify(exactly = 1) { f.repository.renewLiveAutoRenewRules() }
        // once on construction, once after the alarm
        verify(exactly = 2) { f.scheduler.scheduleBy(any()) }
    }

    @Test
    fun `in the foreground the router is not touched but the alarm is re-armed`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope, foreground = true)

        val renewed = f.renewer.renewAll()

        assertEquals(0, renewed)
        coVerify(exactly = 0) { f.repository.renewLiveAutoRenewRules() }
        verify(exactly = 2) { f.scheduler.scheduleBy(any()) }
    }

    @Test
    fun `off Wi-Fi the router is not touched but the alarm is re-armed`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope, onLan = false)

        val renewed = f.renewer.renewAll()

        assertEquals(0, renewed)
        coVerify(exactly = 0) { f.repository.renewLiveAutoRenewRules() }
        verify(exactly = 2) { f.scheduler.scheduleBy(any()) }
    }

    @Test
    fun `a failed renew still re-arms`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope)
        coEvery { f.repository.renewLiveAutoRenewRules() } throws RuntimeException("router went away")

        val renewed = f.renewer.renewAll()

        assertEquals(0, renewed)
        verify(exactly = 2) { f.scheduler.scheduleBy(any()) }
    }

    @Test
    fun `with the setting off an alarm does nothing and cancels`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope, enabled = false)

        f.renewer.renewAll()

        coVerify(exactly = 0) { f.repository.renewLiveAutoRenewRules() }
        verify(exactly = 0) { f.scheduler.scheduleBy(any()) }
        verify(exactly = 2) { f.scheduler.cancel() }
    }

    @Test
    fun `the receiver is released once the renew completes`() = runBlocking {
        val f = RenewerFixture(listOf(recentRow()), scope)
        var finished = false

        f.renewer.renewFromAlarm { finished = true }.join()

        assertTrue(finished)
    }

    // endregion
}

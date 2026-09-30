package com.shinjiindustrial.portmapper.background

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.shinjiindustrial.portmapper.ApplicationScope
import com.shinjiindustrial.portmapper.ILogger
import com.shinjiindustrial.portmapper.PreferencesManager
import com.shinjiindustrial.portmapper.UpnpRepository
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.persistence.PortMappingDao
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton

// "Best effort renewal in background": calculates renew by time and schedules alarm. determines
//   when to call UPnPManager renewAll.
@OptIn(FlowPreview::class)
@Singleton
class BackgroundRenewer internal constructor(
    private val upnpRepository: UpnpRepository,
    private val portMappingDao: PortMappingDao,
    private val preferencesManager: PreferencesManager,
    private val renewScheduler: RenewScheduler,
    private val ourLogger: ILogger,
    private val applicationScope: CoroutineScope,
    private val onLan: () -> Boolean,
    private val inForeground: () -> Boolean,
    debounceMs: Long = OBSERVER_DEBOUNCE_MS,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        upnpRepository: UpnpRepository,
        portMappingDao: PortMappingDao,
        preferencesManager: PreferencesManager,
        renewScheduler: RenewScheduler,
        ourLogger: ILogger,
        @ApplicationScope applicationScope: CoroutineScope,
    ) : this(
        upnpRepository, portMappingDao, preferencesManager, renewScheduler, ourLogger, applicationScope,
        { isOnLan(context) },
        { isInForeground() },
    )

    init {
        // recalculates alarm on new devices, rules, rule settings, background setting
        applicationScope.launch {
            combine(
                preferencesManager.backgroundRenew,
                upnpRepository.devices,
                upnpRepository.portMappings,
                portMappingDao.observeAll(),
            ) { enabled, devices, portMappings, rows ->
                Inputs(enabled, devices, portMappings, rows)
            }
                .debounce(debounceMs)
                .map { inputs ->
                    if (!inputs.enabled) {
                        null
                    } else {
                        ourLogger.log(Level.FINE, "recalculating renew deadline")
                        backgroundRenewDeadline(
                            inputs.devices,
                            inputs.portMappings,
                            inputs.rows,
                            SystemClock.elapsedRealtime(),
                            System.currentTimeMillis()
                        )
                    }
                }
                // stored row estimates has jitter (bc recomputed against clock each time),
                //   use second resolution
                .distinctUntilChanged { old, new -> old?.div(1000) == new?.div(1000) }
                .collect { apply(it) }
        }
    }

    // alarm entry point. best effort renewal (not cancelled when budget runs out)
    fun renewFromAlarm(onFinished: () -> Unit): Job = applicationScope.launch {
        val work = async { renewAll() }
        try {
            val renewed = withTimeoutOrNull(ALARM_BUDGET_MS) { work.await() }
            if (renewed == null) {
                ourLogger.log(
                    Level.WARNING,
                    "Background renew: over the ${ALARM_BUDGET_MS / 1000} s budget, continuing without the receiver"
                )
            }
        } finally {
            onFinished()
        }
    }

    // returns the number of rules renewed and re-arms if applicable
    suspend fun renewAll(): Int {
        var renewed = 0
        try {
            if (!preferencesManager.backgroundRenew.first()) {
                ourLogger.log(Level.INFO, "Background renew: setting is off")
            } else if (inForeground()) {
                // TODO: is that an issue when rescheduling?  since foreground renew might happen in
                //   say 30s, our next schedule might be ASAP when it doesnt need to be (or alternatively
                //   we might miss the next window if the next foreground renew happens in 5 min and
                //   they expire but out next schedule is in 10)
                // This clause might be rare enough to just remove.. Theres not too much harm in it.
                // We still don't want to renew if the FGS is active bc then its pointless
                ourLogger.log(Level.INFO, "Background renew: skipped, app is in the foreground")
            } else if (!onLan()) {
                ourLogger.log(Level.INFO, "Background renew: skipped, not on Wi-Fi / Ethernet")
            } else {
                ourLogger.log(Level.INFO, "Background renew: starting")
                renewed = upnpRepository.renewLiveAutoRenewRules()
                ourLogger.log(Level.INFO, "Background renew: renewed $renewed rule(s)")
            }
        } catch (e: Exception) {
            ourLogger.log(Level.WARNING, "Background renew failed: ${e.message}", e)
        } finally {
            reschedule()
        }
        return renewed
    }

    suspend fun reschedule() {
        val deadline = if (!preferencesManager.backgroundRenew.first()) {
            null
        } else {
            backgroundRenewDeadline(
                upnpRepository.devices.value,
                upnpRepository.portMappings.value,
                portMappingDao.getAll(),
                SystemClock.elapsedRealtime(),
                System.currentTimeMillis()
            )
        }
        apply(deadline)
    }

    private fun apply(deadline: Long?) {
        ourLogger.log(Level.FINE, "update alarm")
        if (deadline == null) {
            renewScheduler.cancel()
        } else {
            renewScheduler.scheduleBy(deadline)
        }
    }

    private data class Inputs(
        val enabled: Boolean,
        val devices: List<IIGDDevice>,
        val portMappings: Map<PortMappingKey, PortMappingWithPref>,
        val rows: List<PortMappingEntity>,
    )

    companion object {
        // under the ~10 s broadcast timeout goAsync() gets
        const val ALARM_BUDGET_MS = 9_000L

        const val OBSERVER_DEBOUNCE_MS = 1_000L

        fun isInForeground(): Boolean {
            return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }

        fun isOnLan(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
                return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return info.type == ConnectivityManager.TYPE_WIFI || info.type == ConnectivityManager.TYPE_ETHERNET
        }
    }
}

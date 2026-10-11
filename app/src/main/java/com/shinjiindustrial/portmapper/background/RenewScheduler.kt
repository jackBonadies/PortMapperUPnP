package com.shinjiindustrial.portmapper.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.shinjiindustrial.portmapper.ILogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

// Wrapper around AlarmManager, each alarm replaces the previous one, factors in inexact window
//
// Inexact only (setAndAllowWhileIdle) - allows for renewal 7/hr with minimum window 17.5 mins.
//   Buckets will restrict this (i.e. if not in Active or Working set it can only be scheduled
//   once an hour).  Still fires during Doze (idle). ~10s limit for background activities.
// Exact allows for 10 min windows + no bucket restrictions.  But runtime permission and scrutiny
//   (alarm and calendar apps)
@Singleton
class RenewScheduler internal constructor(
    private val alarmManager: AlarmManager,
    private val pendingIntent: () -> PendingIntent,
    private val sdkInt: Int,
    private val ourLogger: ILogger,
) {
    @Inject
    constructor(@ApplicationContext context: Context, ourLogger: ILogger) : this(
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager,
        {
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, RenewAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        },
        Build.VERSION.SDK_INT,
        ourLogger,
    )

    // sets alarm (accounts for Android inexact window time)
    fun scheduleBy(deadlineElapsedMs: Long): Long {
        val now = SystemClock.elapsedRealtime()
        val delay = triggerDelay(deadlineElapsedMs - now, sdkInt)
        val triggerAt = now + delay
        val operation = pendingIntent()
        if (sdkInt >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation)
        }
        ourLogger.log(
            Level.INFO,
            "Background renew: alarm in ${delay / 1000} s, deadline in ${(deadlineElapsedMs - now) / 1000} s"
        )
        return triggerAt
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
        ourLogger.log(Level.INFO, "Background renew: alarm cancelled")
    }

    companion object {
        const val MIN_MARGIN_MS = 30_000L
        const val MARGIN_FRACTION = 0.05

        // min delay hard cutoff (actual is closer to 10 minutes enforced by android)
        const val MIN_DELAY_MS = 5L * 60 * 1000

        private const val INEXACT_WINDOW_FACTOR = 1.75
        private const val MAX_INEXACT_WINDOW_MS = AlarmManager.INTERVAL_HOUR

        // how long before expiry a rule must be renewed by
        fun marginMs(leaseSeconds: Int): Long {
            return max(MIN_MARGIN_MS, (leaseSeconds * 1000L * MARGIN_FRACTION).toLong())
        }

        // delay taking Android inexact window into account
        fun triggerDelay(untilDeadlineMs: Long, sdkInt: Int): Long {
            var delay = (untilDeadlineMs / INEXACT_WINDOW_FACTOR).toLong()
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                delay = max(delay, untilDeadlineMs - MAX_INEXACT_WINDOW_MS)
            }
            return max(delay, MIN_DELAY_MS)
        }
    }
}

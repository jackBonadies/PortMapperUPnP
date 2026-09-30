package com.shinjiindustrial.portmapper.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// target of the PendingIntent RenewScheduler alarms, calls down to UPnPManager.renewAll
class RenewAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RenewAlarmEntryPoint {
        fun backgroundRenewer(): BackgroundRenewer
    }

    override fun onReceive(context: Context, intent: Intent) {
        val backgroundRenewer = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RenewAlarmEntryPoint::class.java
        ).backgroundRenewer()
        // keeps broadcast active even after returning from this function
        val pendingResult = goAsync()
        backgroundRenewer.renewFromAlarm { pendingResult.finish() }
    }
}

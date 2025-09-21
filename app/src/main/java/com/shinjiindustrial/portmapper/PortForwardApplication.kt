package com.shinjiindustrial.portmapper

import android.app.Application
import androidx.compose.ui.unit.dp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


//TODO clean up create (picker for duration, indication that 0 is max), port range, full screen (?)
//TODO group by algorithm. way to convert from grouped port mappings to individial
//TODO are "slots" ordering important?
//TODO swipe to refresh still broken...
@HiltAndroidApp
class PortForwardApplication : Application() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    companion object {

        //        lateinit var showPopup : MutableState<Boolean>
        val PaddingBetweenCreateNewRuleRows = 4.dp

        // can create a singleton logs repo

        const val ScrollToBottom = "ScrollToBottom"

        const val RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING = 20L
        const val RENEW_BATCH_WITHIN_X_SECONDS = 5L
    }


}
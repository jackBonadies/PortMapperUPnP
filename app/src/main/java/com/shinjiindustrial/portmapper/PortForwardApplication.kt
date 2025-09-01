package com.shinjiindustrial.portmapper

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject


//TODO clean up create (picker for duration, indication that 0 is max), port range, full screen (?)
//TODO group by algorithm. way to convert from grouped port mappings to individial
//TODO are "slots" ordering important?
//TODO swipe to refresh still broken...
@HiltAndroidApp
class PortForwardApplication : Application() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {

        super.onCreate()

        FirebaseConditional.Initialize(this)

        println("PortForwardApplication onCreate Finished")
    }


    companion object {

        //        lateinit var showPopup : MutableState<Boolean>
        var PaddingBetweenCreateNewRuleRows = 4.dp

        // can create a singleton logs repo

        val ScrollToBottom = "ScrollToBottom"
        var crashlyticsEnabled: Boolean = false

        val RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING = 30L
        val RENEW_BATCH_WITHIN_X_SECONDS = 5L
    }


}
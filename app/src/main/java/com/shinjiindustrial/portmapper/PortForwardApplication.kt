package com.shinjiindustrial.portmapper

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.shinjiindustrial.portmapper.common.SortBy
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        instance = this
        appContext = applicationContext

        val logger = Logger.getLogger("")
        logger.addHandler(StringBuilderHandler(Logs))

        println("PortForwardApplication onCreate Finished")
    }



    companion object {

        lateinit var appContext: Context
        lateinit var instance: PortForwardApplication
        var CurrentActivity: ComponentActivity? = null
        //        lateinit var showPopup : MutableState<Boolean>
        lateinit var currentSingleSelectedObject : MutableState<PortMappingWithPref?>
        var showContextMenu : MutableState<Boolean> = mutableStateOf(false)
        var PaddingBetweenCreateNewRuleRows = 4.dp
        var Logs : SnapshotStateList<String> = mutableStateListOf<String>()
        var OurLogger : Logger = Logger.getLogger("PortMapper")
        val ScrollToBottom = "ScrollToBottom"
        var crashlyticsEnabled: Boolean = false

        val RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING = 30L;
        val RENEW_BATCH_WITHIN_X_SECONDS = 5L;

        fun ShowToast( msg : String,  toastLength : Int)
        {
            GlobalScope.launch(Dispatchers.Main) {
                CurrentActivity?.runOnUiThread {
                    Toast.makeText(
                        appContext,
                        msg,
                        toastLength
                    ).show()
                }
            }
        }
    }


}
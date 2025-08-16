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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger


private val Context.dataStore by preferencesDataStore("preferences")

//TODO clean up create (picker for duration, indication that 0 is max), port range, full screen (?)
//TODO group by algorithm. way to convert from grouped port mappings to individial
//TODO are "slots" ordering important?
//TODO swipe to refresh still broken...
class PortForwardApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        FirebaseConditional.Initialize(this)

        RestoreSharedPrefs()

        instance = this
        appContext = applicationContext

        val logger = Logger.getLogger("")
        logger.addHandler(StringBuilderHandler(Logs))

        println("PortForwardApplication onCreate Finished")
    }

    // TODO move these
    fun RestoreSharedPrefs()
    {
        runBlocking {
            val preferences = dataStore.data.first()
            val nightModeKey = androidx.datastore.preferences.core.intPreferencesKey(SharedPrefKeys.dayNightPref)
            SharedPrefValues.DayNightPref = DayNightMode.from(preferences[nightModeKey] ?: 0)
            val materialYouKey = androidx.datastore.preferences.core.booleanPreferencesKey(SharedPrefKeys.materialYouPref)
            SharedPrefValues.MaterialYouTheme = preferences[materialYouKey] ?: false
            val sortOrderKey = androidx.datastore.preferences.core.intPreferencesKey(SharedPrefKeys.sortOrderPref)
            // much better default than slot. with slot updating a rule (i.e. enable or disable) sends it down to bottom
            SharedPrefValues.SortByPortMapping = SortBy.from(preferences[sortOrderKey] ?: SortBy.ExternalPort.sortByValue)
            val descAsc = androidx.datastore.preferences.core.booleanPreferencesKey(SharedPrefKeys.descAscPref)
            SharedPrefValues.Ascending = preferences[descAsc] ?: true
        }
    }

    fun SaveSharedPrefs() {
        GlobalScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                val nightModeKey = androidx.datastore.preferences.core.intPreferencesKey(SharedPrefKeys.dayNightPref)
                preferences[nightModeKey] = SharedPrefValues.DayNightPref.intVal
                val materialYouKey = androidx.datastore.preferences.core.booleanPreferencesKey(SharedPrefKeys.materialYouPref)
                preferences[materialYouKey] = SharedPrefValues.MaterialYouTheme
                val sortKey = androidx.datastore.preferences.core.intPreferencesKey(SharedPrefKeys.sortOrderPref)
                preferences[sortKey] = SharedPrefValues.SortByPortMapping.sortByValue
                val descAscKey = androidx.datastore.preferences.core.booleanPreferencesKey(SharedPrefKeys.descAscPref)
                preferences[descAscKey] = SharedPrefValues.Ascending
            }
        }
    }


    companion object {

        lateinit var appContext: Context
        lateinit var instance: PortForwardApplication
        var CurrentActivity: ComponentActivity? = null
        //        lateinit var showPopup : MutableState<Boolean>
        lateinit var currentSingleSelectedObject : MutableState<Any?>
        var showContextMenu : MutableState<Boolean> = mutableStateOf(false)
        var PaddingBetweenCreateNewRuleRows = 4.dp
        var Logs : SnapshotStateList<String> = mutableStateListOf<String>()
        var OurLogger : Logger = Logger.getLogger("PortMapper")
        val ScrollToBottom = "ScrollToBottom"
        var crashlyticsEnabled: Boolean = false

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
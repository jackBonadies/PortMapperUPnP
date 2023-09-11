@file:OptIn(ExperimentalMaterialApi::class)

package com.shinjiindustrial.portmapper


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.example.myapplication.R
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.shinjiindustrial.portmapper.PortForwardApplication.Companion.OurLogger

import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.model.meta.RemoteDevice
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.*
import kotlin.random.Random

var PseudoSlotCounter : Int = MAX_PORT // as starting slot
fun GetPsuedoSlot() : Int
{
    PseudoSlotCounter += 1
    return PseudoSlotCounter
}

private val Context.dataStore by preferencesDataStore("preferences")

//object UpnpManager {
//    va UpnpService? : UpnpService = null
//}

fun test1()
{
    // gets blocked for the duration of the call, until all the coroutines inside runBlocking { ... } complete their execution
    // launch is declared only in the coroutine scope
    runBlocking { // this: CoroutineScope this also causes it to wait for the other async coroutine to be done
        launch {
            extracted()
        }
        println("Hello") // main coroutine continues while a previous one is delayed
    }
}

suspend fun extracted() {
    delay(4000L) // non-blocking delay for 1 second (default time unit is ms)
    println("World!") // print after delay
}

object SharedPrefKeys
{
    val dayNightPref = "dayNightPref"
    val materialYouPref = "materialYouPref"
    val sortOrderPref = "sortOrderPref"
    val descAscPref = "descAscPref"
}

object SharedPrefValues
{
    var DayNightPref : DayNightMode = DayNightMode.FOLLOW_SYSTEM
    var MaterialYouTheme : Boolean = false
    var SortByPortMapping : SortBy = SortBy.Slot
    var Ascending : Boolean = true
}

enum class DayNightMode(val intVal : Int) {
    FOLLOW_SYSTEM(0),
    FORCE_DAY(1),
    FORCE_NIGHT(2);

    companion object {
        fun from(findValue: Int): DayNightMode = DayNightMode.values().first { it.intVal == findValue }
    }
}


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
//        this.registerReceiver(
//            ConnectionReceiver(),
//            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
//        )

        val logger = Logger.getLogger("")
        logger.addHandler(StringBuilderHandler(Logs))

        var test = NetworkType.DATA.toString()

        //test1()
        println("done")
    }

    fun RestoreSharedPrefs()
    {
        runBlocking {
            var preferences = dataStore.data.first()
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
                    ).show();
                }
            }
        }
    }


}

class StringBuilderHandler(private val stringBuilder: SnapshotStateList<String>) : java.util.logging.Handler() {

      override fun publish(record: LogRecord?) {
          record?.let {
              val prefix = when (it.level)
              {
                  Level.INFO -> "I: "
                  Level.WARNING -> "W: "
                  Level.SEVERE -> "E: "
                  else -> return // i.e. do not log
              }
              stringBuilder.add(prefix + it.message)
          }
      }
//
      override fun flush() {
          // Nothing to do here since StringBuilder doesn't need to be flushed
      }

      override fun close() {
          // Nothing to do here since StringBuilder doesn't need to be closed
      }
}

class FileHandlerExample {

    fun setupLogger() {
        val logger = Logger.getLogger("")
        val fh: FileHandler

        try {
            // This block configure the logger with handler and formatter
            fh = FileHandler("MyLogFile.log")
            logger.addHandler(fh)
            val formatter = SimpleFormatter()
            fh.formatter = formatter

            // the following statement is used to log any messages
            logger.info("My first log")

        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


enum class Protocol(val protocol: String) {
    TCP("TCP"),
    UDP("UDP"),
    BOTH("BOTH");
    fun str() : String
    {
        return protocol
    }
}


class MainActivity : ComponentActivity() {

    fun deviceFoundHandler(remoteDevice: IGDDevice) {
        runOnUiThread {
            mainSearchInProgressAndNothingFoundYet?.value = false
        }
        UpnpManager.invokeUpdateUIFromData()
        //updateUIFromData()
//        runOnUiThread {
//            mainSearchInProgressAndNothingFoundYet?.value = false
//            Log.i("portmapperUI", "deviceFoundHandler add device")
//            upnpElementsViewModel.addItem(UPnPViewElement(remoteDevice)) // calls LiveData.setValue i.e. must be done on UI thread
//        }
    }

    fun portMappingAddedHandler(portMapping: PortMapping) {
//        runOnUiThread {
//            upnpElementsViewModel.addItem(UPnPViewElement(portMapping)) // calls LiveData.setValue i.e. must be done on UI thread
//        }
        UpnpManager.invokeUpdateUIFromData()
        // we dont know if this rule already exists. and so we dont want to add it twice.
        //updateUIFromData(null)
    }

    fun portMappingFoundHandler(portMapping: PortMapping) {
        UpnpManager.invokeUpdateUIFromData()
        //updateUIFromData()
//        runOnUiThread {
//            upnpElementsViewModel.addItem(UPnPViewElement(portMapping)) // calls LiveData.setValue i.e. must be done on UI thread
//        }
    }

    fun deviceFinishedListingPortsHandler(remoteDevice: IGDDevice) {
        if (remoteDevice.portMappings.isEmpty()) {
            UpnpManager.invokeUpdateUIFromData()
        }
        //updateUIFromData()

    }

    fun searchStarted(o: Any?) {
        runOnUiThread {
            mainSearchInProgressAndNothingFoundYet!!.value = true // controls when loading bar is there
            searchInProgressJob?.cancel() // cancel old search timer
            if (mainSearchInProgressAndNothingFoundYet!!.value) {
                searchInProgressJob = GlobalScope.launch {
                    delay(6000)
                    mainSearchInProgressAndNothingFoundYet!!.value = false
                }
            }
        }
    }

    fun updateUIFromData(o: Any? = null) {
        Log.i("portmapperUI", "updateUIFromData non ui thread")
        runOnUiThread {

            Log.i("portmapperUI", "updateUIFromData")

            var data: MutableList<UPnPViewElement> = mutableListOf()
            synchronized(UpnpManager.lockIgdDevices)
            {
                for (device in UpnpManager.IGDDevices) {
                    data.add(UPnPViewElement(device))

                    if (device.portMappings.isEmpty()) {
                        data.add(
                            UPnPViewElement(
                                device,
                                true
                            )
                        ) // calls LiveData.setValue i.e. must be done on UI thread
                    } else {

                        for (mapping in device.portMappings)
                        {
                            data.add(UPnPViewElement(mapping))
                        }

                    }
                }
            }
            upnpElementsViewModel.setData(data) // calls LiveData.setValue i.e. must be done on UI thread
        }
    }

    companion object {

        fun viewLogCallback() {
            val intent =
                Intent(PortForwardApplication.CurrentActivity, LogViewActivity::class.java)
            intent.putExtra(PortForwardApplication.ScrollToBottom, true);
            PortForwardApplication.CurrentActivity?.startActivity(intent)
        }

        fun showSnackBarViewLog(message: String) {
            showSnackBar(message, "View Log", SnackbarDuration.Long, ::viewLogCallback)
        }

        fun showSnackBarShortNoAction(message: String) {
            showSnackBar(message, null, SnackbarDuration.Short)
        }

        fun showSnackBarLongNoAction(message: String) {
            showSnackBar(message, null, SnackbarDuration.Long)
        }

        fun showSnackBar(
            message: String,
            action: String?,
            duration: SnackbarDuration,
            onAction: () -> Unit = { }
        ) {
            if (OurSnackbarHostState == null) {
                PortForwardApplication.ShowToast(
                    message,
                    if ((duration == SnackbarDuration.Long || duration == SnackbarDuration.Indefinite)) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                )
            }
            GlobalScope.launch(Dispatchers.Main) {

                var snackbarResult = OurSnackbarHostState!!.showSnackbar(
                    message,
                    action,
                    (duration == SnackbarDuration.Indefinite),
                    duration
                )
                println("shown")
                when (snackbarResult) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> onAction()
                }
            }
        }

        var OurSnackbarHostState: SnackbarHostState? = null
        var MultiSelectItems : SnapshotStateList<PortMapping>? = null
    }

    lateinit var upnpElementsViewModel: UPnPElementViewModel

    //var upnpElementsViewModel: ViewModel by viewModels()
    //var upnpElementsViewModel = UPnPElementViewModel()
    var searchInProgressJob: Job? = null


    var mainSearchInProgressAndNothingFoundYet: MutableState<Boolean>? = null //TODO similar thing with pullrefresh...

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this
            
        onBackPressedDispatcher.addCallback(this)
        {
            println("My Back Pressed Callback")
            if(IsMultiSelectMode())
            {
                MainActivity.MultiSelectItems!!.clear()
            }
            else
            {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        
        //OurNetworkInfo.GetNameTypeMappings(this)
        //getConnectionType(this)
        //getLocalIpAddress()

        //android router set wifi enabled

//        val future = CompletableFuture<String>()
//        future.complete("Hello")
//        var result = future.get()

        //AndroidRouter().enableWiFi()

        // viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
        //     var upnpElementsViewModel = UPnPElementViewModel()

        var success = UpnpManager.Initialize(this, false)
        upnpElementsViewModel = ViewModelProvider(this).get(UPnPElementViewModel::class.java)
        //updateUIFromData() // no longer needed with ViewModelProvided ViewModel

        UpnpManager.DeviceFoundEvent += ::deviceFoundHandler
        UpnpManager.PortAddedEvent += ::portMappingAddedHandler
        UpnpManager.PortInitialFoundEvent += ::portMappingFoundHandler
        UpnpManager.FinishedListingPortsEvent += ::deviceFinishedListingPortsHandler

        UpnpManager.SubscibeToUpdateData(lifecycleScope) {
            Log.i("portmapper", "ui update data handler")
            updateUIFromData(null)
        }
//
//        UpnpManager.UpdateUIFromDataCollating.conflate().onEach {
//
//                Log.i("portmapper","ui update data handler")
//                updateUIFromData(null)
//
//            }.launchIn(lifecycleScope)

        UpnpManager.SearchStarted += ::searchStarted

        if (UpnpManager.FailedToInitialize) {
            mainSearchInProgressAndNothingFoundYet = mutableStateOf(false)
        } else {
            mainSearchInProgressAndNothingFoundYet = mutableStateOf(!UpnpManager.HasSearched)
            var searchStarted = UpnpManager.Search(true) // by default STAll
        }
        //var refreshState = mutableStateOf(false)


        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigator()
                }
            }
        }
    }


    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun AppNavigator() {
        val navController = rememberAnimatedNavController()

        AnimatedNavHost(navController = navController, startDestination = "main_screen") {
            composable("main_screen", exitTransition = {
//                slideOutHorizontally(
//                    targetOffsetX = { -300 }, animationSpec = tween(
//                        durationMillis = 300, easing = FastOutSlowInEasing
//                    )
//                )
                fadeOut(animationSpec = tween(300))
            },
                popEnterTransition = {
                    //fadeIn(animationSpec = tween(0))
                    //this is whats used when going back from "create rule"
                    slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(
                        durationMillis = 200, easing = FastOutSlowInEasing
                    )) + fadeIn(animationSpec = tween(400))
                    //fadeIn(animationSpec = tween(300))
//                    slideInHorizontally(
//                        initialOffsetX = { -300 }, animationSpec = tween(
//                            durationMillis = 300, easing = FastOutSlowInEasing
//                        )
//                    ) //+ fadeIn(animationSpec = tween(300))

                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300))
//                    slideInVertically(
//                        initialOffsetY = { -300 }, animationSpec = tween(
//                            durationMillis = 300, easing = FastOutSlowInEasing
//                        )
//                    ) //+ fadeIn(animationSpec = tween(300))

                },
            )
            {
                MainScreen(navController = navController)
            }
            composable("full_screen_dialog?description={description}&internalIp={internalIp}&internalRange={internalRange}&externalIp={externalIp}&externalRange={externalRange}&protocol={protocol}&leaseDuration={leaseDuration}&enabled={enabled}",
                arguments = listOf(
                    navArgument("description") { nullable = true; type = NavType.StringType }, // only if nullable (or default) are they optional
                    navArgument("internalIp") { nullable = true; type = NavType.StringType },
                    navArgument("internalRange") { nullable = true; type = NavType.StringType },
                    navArgument("externalIp") { nullable = true; type = NavType.StringType },
                    navArgument("externalRange") { nullable = true; type = NavType.StringType },
                    navArgument("protocol") { nullable = true; type = NavType.StringType },
                    navArgument("leaseDuration") { nullable = true; type = NavType.StringType },
                    navArgument("enabled") { type = NavType.BoolType; defaultValue = false },
                ),
                    popExitTransition = {

                                       slideOutVertically(
                            targetOffsetY = { it / 2 }, animationSpec = tween(
                                durationMillis = 200, easing = FastOutSlowInEasing
                            )) + fadeOut(animationSpec = tween(100))
//                slideOutVertically(
//                    targetOffsetY = { -300 }, animationSpec = tween(
//                        durationMillis = 300, easing = FastOutSlowInEasing
//                    )
                    //) //+
                    //fadeOut(animationSpec = tween(300))
                },
                exitTransition = {
               slideOutVertically(
                   targetOffsetY = { it }, animationSpec = tween(
                       durationMillis = 5000, easing = FastOutSlowInEasing
                   ))
                //) //+
            },
                enterTransition = {

                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(
                            durationMillis = 200, easing = FastOutSlowInEasing
                        )) + fadeIn(animationSpec = tween(400))
//                        initialOffsetY = { -300 }, animationSpec = tween(
//                            durationMillis = 300, easing = FastOutSlowInEasing
//                        )

                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(150))
                }
            )
            {
            backStackEntry ->
                val arguments = backStackEntry.arguments
                val desc = arguments?.getString("description")
                val internalIp = arguments?.getString("internalIp")
                val internalRange = arguments?.getString("internalRange")
                val externalIp = arguments?.getString("externalIp")
                val externalRange = arguments?.getString("externalRange")
                val protocol = arguments?.getString("protocol")
                val leaseDuration = arguments?.getString("leaseDuration")
                val enabled = arguments?.getBoolean("enabled")

                var portMappingUserInputToEdit : PortMappingUserInput? = null
                if(desc != null)
                {
                    portMappingUserInputToEdit = PortMappingUserInput(desc, internalIp!!, internalRange!!, externalIp!!, externalRange!!, protocol!!, leaseDuration!!, enabled!!)
                }

                RuleCreationDialog(navController = navController, portMappingUserInputToEdit)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy();
        UpnpManager.DeviceFoundEvent -= ::deviceFoundHandler
        UpnpManager.PortAddedEvent -= ::portMappingAddedHandler
        UpnpManager.FinishedListingPortsEvent -= ::deviceFinishedListingPortsHandler
        UpnpManager.UpdateUIFromData -= ::updateUIFromData
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun MainScreen(navController : NavHostController) {
        // A surface container using the 'background' color from the theme
        val scrollState = rememberScrollState()
        val showAddRuleDialogState = remember { mutableStateOf(false) }
        val showAboutDialogState = remember { mutableStateOf(false) }
        val showMoreInfoDialogState = remember { mutableStateOf(false) }
        var showAddRuleDialog by showAddRuleDialogState //mutable state binds to UI (in sense if value changes, redraw). remember says when redrawing dont discard us.
        val showAboutDialog by showAboutDialogState //mutable state binds to UI (in sense if value changes, redraw). remember says when redrawing dont discard us.

//                var showMainLoading = remember { mutableStateOf(searchStarted) }
//


        PortForwardApplication.currentSingleSelectedObject =
            remember { mutableStateOf(null) }

//                if(singleSelectionPopup)
//                {
//                    Popup(alignment = Alignment.Center) {
//                        DropdownMenu(
//                            expanded = true,
//                            onDismissRequest = {}
//                        ) {
//                            DropdownMenuItem(
//                                text = { Text("Load") },
//                                onClick = {  }
//                            )
//                            DropdownMenuItem(
//                                text = { Text("Save") },
//                                onClick = {  }
//                            )
//                        }
//                    }
        //}

        if (PortForwardApplication.showContextMenu.value && PortForwardApplication.currentSingleSelectedObject.value != null) {
            EnterContextMenu(
                PortForwardApplication.currentSingleSelectedObject,
                showMoreInfoDialogState,
                navController
            )
        }

        if (showMoreInfoDialogState.value) {
            MoreInfoDialog(
                portMapping = PortForwardApplication.currentSingleSelectedObject.value as PortMapping,
                showDialog = showMoreInfoDialogState
            )
        }


        if (showAddRuleDialog) {

            EnterPortDialog(showAddRuleDialogState)

//                    Dialog(onDismissRequest = {
//                        println("dismiss request")
//                        showDialog = false })
//                    {
//                            // Dialog content here
//                            Box(modifier = Modifier.background(Color.White)) {
//                            Text("Hello, Dialog!")
//                        }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialogState.value = false },
                title = { Text("About") },
                text = {

                    Column()
                    {
                        var aboutString: String =
                            PortForwardApplication.appContext.getString(R.string.about_body);
                        var packageInfo =
                            PortForwardApplication.appContext.packageManager.getPackageInfo(
                                PortForwardApplication.appContext.packageName,
                                0
                            )

                        aboutString = String.format(aboutString, packageInfo.versionName)
                        var spannedString: Spanned? = null
                        if (Build.VERSION.SDK_INT >= 24) {
                            spannedString =
                                Html.fromHtml(aboutString, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            spannedString = Html.fromHtml(aboutString)
                        }

                        // compose doesn't allow spannable nor link movement method
                        AndroidView(factory = { context ->
                            TextView(context).apply {
                                setTextColor(AdditionalColors.TextColor.toArgb());
                                setTextSize(16f);
                                text = spannedString;
                                movementMethod = LinkMovementMethod.getInstance()
                                // you can apply other TextView properties here
                            }
                        })

                        //Text(aboutString)
                    }


                },
                confirmButton = {
                    Button(onClick = { showAboutDialogState.value = false }) {
                        Text("OK")
                    }
                })
        }


        MainActivity.OurSnackbarHostState = remember { SnackbarHostState() }
        MainActivity.MultiSelectItems = remember { mutableStateListOf<PortMapping>() }
        val scope = rememberCoroutineScope()
        val coroutineScope: CoroutineScope = rememberCoroutineScope()
        val anyIgdDevices = remember { mutableStateOf(!UpnpManager.IGDDevices.isEmpty()) }
        UpnpManager.AnyIgdDevices = anyIgdDevices

        val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetElevation = 24.dp,
            sheetBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            sheetContent = {
                // This is what will be shown in the bottom sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    BottomSheetSortBy()
                }
            }
        ) {

            Scaffold(

                snackbarHost = {
                    SnackbarHost(MainActivity.OurSnackbarHostState!!) { data ->
                        // custom snackbar with the custom colors
                        Snackbar(
                            data,
                            actionColor = AdditionalColors.PrimaryDarkerBlue
                            // according to https://m2.material.io/design/color/dark-theme.html
                            // light snackbar in darkmode is good.
//                                    containerColor = AdditionalColors.CardContainerColor,
//                                    contentColor = AdditionalColors.TextColor
//                                    //contentColor = ...,
                        )
                    }
                },
                floatingActionButton = {

                    if (anyIgdDevices.value) {
                        FloatingActionButton(
                            // uses MaterialTheme.colorScheme.secondaryContainer
                            containerColor = MaterialTheme.colorScheme.secondaryContainer, //todo revert to secondar
                            onClick = {
                                //showAddRuleDialogState.value = true
                                navController.navigate("full_screen_dialog")

                                //this works

                            }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Localized description",
                                tint = AdditionalColors.TextColor,
                            )
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            if(IsMultiSelectMode())
                            {
                                IconButton(onClick = {
                                    MultiSelectItems!!.clear()
                                })
                                {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
//                                modifier = Modifier.height(40.dp),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = AdditionalColors.TopAppBarColor
                        ),
                        //colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                        title = {
                            val title = if(MultiSelectItems!!.isEmpty()) "PortMapper" else "${MultiSelectItems!!.count()} Selected"
                            Text(
                                text = title,
                                color = AdditionalColors.TextColorStrong,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        actions = {

                            if(MultiSelectItems!!.isEmpty())
                            {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        bottomSheetState.show()
                                    }
                                })
                                {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                            }
                            else
                            {
                                IconButton(onClick = {
                                    deleteAll(MultiSelectItems)
                                })
                                {
                                    Icon(Icons.Default.Delete, contentDescription = "Sort")
                                }
                            }


                            OverflowMenu(
                                showAddRuleDialogState,
                                showAboutDialogState
                            )
                        }
                    )
                },
                content = { it ->

                    val refreshScope = rememberCoroutineScope()

                    var refreshState = remember { mutableStateOf(false) }
                    var refreshing by refreshState

                    val andResult = remember { derivedStateOf { refreshing && mainSearchInProgressAndNothingFoundYet!!.value } }

                    val onlyShowMainProgressBar = true // looks slightly better in practice

                    fun refresh() = refreshScope.launch {
                        refreshing = !onlyShowMainProgressBar
                        UpnpManager.FullRefresh()

                        //TODO cancel if already done

                        delay(6000)
                        println("finish refreshing $refreshing")
                        refreshing = false
                    }

                    val state = rememberPullRefreshState(andResult.value, ::refresh)

                    BoxWithConstraints(
                        Modifier
                            .padding(it)
                            .pullRefresh(state)
                            .fillMaxHeight()
                            .fillMaxWidth()
                    )
                    {

                        val boxHeight =
                            with(LocalDensity.current) { constraints.maxHeight.toDp() }



                        Column(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                        ) {
//                                    for (i in 1..10)
//                                    {
//                                        Text("test")
//                                    }
                            if (mainSearchInProgressAndNothingFoundYet!!.value) {
                                val offset = boxHeight * 0.28f
                                LoadingIcon(
                                    "Searching for devices",
                                    Modifier.offset(y = offset)
                                )
                            } else if (!anyIgdDevices.value) {
                                val offset = boxHeight * 0.28f
                                Column(modifier = Modifier.offset(y = offset))
                                {
                                    Text(
                                        "No UPnP enabled internet gateway devices found",
                                        modifier = Modifier
                                            .padding(0.dp, 10.dp)
                                            .align(Alignment.CenterHorizontally),
                                        textAlign = TextAlign.Center
                                    )

                                    var interfacesUsedInSearch =
                                        (UpnpManager.GetUPnPService().configuration as AndroidConfig).NetworkInterfacesUsedInfos
                                    var anyInterfaces =
                                        UpnpManager.GetUPnPService().router.isEnabled
                                    if (!anyInterfaces) {
                                        Text(
                                            "No valid interfaces",
                                            modifier = Modifier
                                                .padding(0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                    } else {

                                        Text(
                                            "Interfaces Searched:",
                                            modifier = Modifier
                                                .padding(0.dp, 10.dp, 0.dp, 0.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )


                                        for (interfaceUsed in interfacesUsedInSearch!!) {
                                            Text(
                                                "${interfaceUsed.first.displayName} (${interfaceUsed.second.networkTypeString.lowercase()})",
                                                modifier = Modifier
                                                    .padding(0.dp, 0.dp)
                                                    .align(Alignment.CenterHorizontally),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // if no wifi
                                    if (interfacesUsedInSearch == null ||
                                        !interfacesUsedInSearch.any { it.second == NetworkType.WIFI }
                                    ) {
                                        Text(
                                            "Enable WiFi and retry",
                                            modifier = Modifier
                                                .padding(0.dp, 20.dp, 0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            "Ensure a valid UPnP enabled internet gateway device is on the network and retry",
                                            modifier = Modifier
                                                .padding(0.dp, 20.dp, 0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            UpnpManager.Initialize(
                                                PortForwardApplication.appContext,
                                                true
                                            )
                                            UpnpManager.FullRefresh()
                                        },
                                        modifier = Modifier
                                            .padding(0.dp, 10.dp)
                                            .align(Alignment.CenterHorizontally),
                                        shape = RoundedCornerShape(4),
                                    ) {
                                        Text(
                                            "Retry",
                                            color = AdditionalColors.TextColorStrong
                                        )
                                    }
                                }
                            } else {
                                ConversationEntryPoint(upnpElementsViewModel)
                            }


//                                MyScreen(viewModel)
//                                Greeting("Android")
//                                Text("hello")
//                                MessageCard("hello", "message content", true)
                        }

                        PullRefreshIndicator(
                            refreshing,
                            state,
                            Modifier.align(Alignment.TopCenter)
                        )
                    }

                }
            )
//                Surface(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .verticalScroll(scrollState),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    Column {
//
//                        MyScreen(viewModel)
//                        Greeting("Android")
//                        Text("hello")
//                        MessageCard("hello", "message content")
//
//                    }
//                }
//                        }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RuleCreationDialog(navController : NavHostController, ruleToEdit : PortMappingUserInput? = null)  {

        var isPreview = false;

        val hasSubmitted = remember { mutableStateOf(false) }
        val internalPortText = remember { mutableStateOf(ruleToEdit?.internalRange ?: "") }
        val internalPortTextEnd = remember { mutableStateOf("") }
        val externalPortText = remember { mutableStateOf(ruleToEdit?.externalRange ?: "") }
        val externalPortTextEnd = remember { mutableStateOf("") }
        val leaseDuration = remember { mutableStateOf(ruleToEdit?.leaseDuration ?: "0 (max)") }
        val description = remember { mutableStateOf(ruleToEdit?.description ?: "") }
        val descriptionHasError = remember { mutableStateOf(validateDescription(description.value).first) }
        var startInternalHasError = remember { mutableStateOf(validateStartPort(internalPortText.value).first) }
        var endInternalHasError = remember { mutableStateOf(validateEndPort(internalPortText.value,internalPortTextEnd.value).first) }
        var selectedProtocolMutable = remember { mutableStateOf(Protocol.TCP.str()) }
        var startExternalHasError = remember { mutableStateOf(validateStartPort(externalPortText.value).first) }
        var endExternalHasError = remember { mutableStateOf(validateEndPort(externalPortText.value,externalPortTextEnd.value).first) }
        var (ourIp, ourGatewayIp) = remember {
            if (isPreview) Pair<String, String>(
                "192.168.0.1",
                ""
            ) else OurNetworkInfo.GetLocalAndGatewayIpAddrWifi(
                PortForwardApplication.appContext,
                false
            )
        }
        val internalIp = remember { mutableStateOf(ruleToEdit?.internalIp ?: ourIp!!) }
        var internalIpHasError = remember { mutableStateOf(validateInternalIp(internalIp.value).first) }
        var (gatewayIps, defaultGatewayIp) = remember { UpnpManager.GetGatewayIpsWithDefault(ourGatewayIp!!) }
        var externalDeviceText = remember { mutableStateOf(defaultGatewayIp) }
        var expandedInternal = remember { mutableStateOf(false) }
        var expandedExternal = remember { mutableStateOf(false) }
        var wanIpVersionOfGatewayIsVersion1 = remember { derivedStateOf {
            var version = UpnpManager.GetDeviceByExternalIp(defaultGatewayIp)?.upnpTypeVersion ?: 2
            version == 1
            } }
        //END




        Scaffold(

            snackbarHost = {
                SnackbarHost(MainActivity.OurSnackbarHostState!!) { data ->
                    // custom snackbar with the custom colors
                    Snackbar(
                        data,
                        actionColor = AdditionalColors.PrimaryDarkerBlue
                        // according to https://m2.material.io/design/color/dark-theme.html
                        // light snackbar in darkmode is good.
//                                    containerColor = AdditionalColors.CardContainerColor,
//                                    contentColor = AdditionalColors.TextColor
//                                    //contentColor = ...,
                    )
                }
            },
            topBar = {
                TopAppBar(
//                                modifier = Modifier.height(40.dp),
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AdditionalColors.TopAppBarColor
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    //colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                    title = {
                        Text(
                            text = if(ruleToEdit == null) "New Rule" else "Edit Rule",
                            color = AdditionalColors.TextColorStrong,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = {

                                var actualEndInternalError = expandedInternal.value && endInternalHasError.value
                                var actualEndExternalError = expandedExternal.value && endExternalHasError.value

                                val usingInternalRange = expandedInternal.value && internalPortTextEnd.value.isNotEmpty()
                                val usingExternalRange = expandedExternal.value && externalPortTextEnd.value.isNotEmpty()

                                hasSubmitted.value = true
                                if (descriptionHasError.value ||
                                    startInternalHasError.value ||
                                    startExternalHasError.value ||
                                    actualEndInternalError ||
                                    actualEndExternalError ||
                                    internalIpHasError.value
                                ) {
                                    // show toast and return
                                    // Invalid Description, ExternalPort.
                                    var invalidFields = mutableListOf<String>()
                                    if(descriptionHasError.value)
                                    {
                                        invalidFields.add("Description")
                                    }
                                    if(startInternalHasError.value)
                                    {
                                        invalidFields.add(if(usingInternalRange) "Internal Port Start" else "Internal Port")
                                    }
                                    if(startExternalHasError.value)
                                    {
                                        invalidFields.add(if(usingExternalRange) "External Port Start" else "External Port")
                                    }
                                    if(actualEndInternalError)
                                    {
                                        invalidFields.add("Internal Port End")
                                    }
                                    if(actualEndExternalError)
                                    {
                                        invalidFields.add("External Port End")
                                    }
                                    if(internalIpHasError.value)
                                    {
                                        invalidFields.add("Internal IP")
                                    }

                                    var invalidFieldsStr = invalidFields.joinToString(", ")

                                    MainActivity.showSnackBarLongNoAction("Invalid Fields: $invalidFieldsStr")
                                    return@TextButton;
                                }

                                var internalRangeStr = if(usingInternalRange) internalPortText.value + "-" + internalPortTextEnd.value else internalPortText.value
                                var externalRangeStr = if(usingExternalRange) externalPortText.value + "-" + externalPortTextEnd.value else externalPortText.value

                                var portMappingRequestInput = PortMappingUserInput(
                                    description.value,
                                    internalIp.value,
                                    internalRangeStr,
                                    externalDeviceText.value,
                                    externalRangeStr,
                                    selectedProtocolMutable.value,
                                    leaseDuration.value.replace(" (max)",""),
                                    true
                                )

                                var errorString = portMappingRequestInput.validateRange()
                                if(errorString.isNotEmpty())
                                {
                                    MainActivity.showSnackBarLongNoAction(errorString)
                                    return@TextButton;
                                }

                                //Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()

                                val modifyCase = ruleToEdit != null

                                fun batchCallback(result: MutableList<UPnPCreateMappingResult?>) {

                                    RunUIThread {
                                        //debug
                                        for (res in result) {
                                            res!!
                                            print(res.Success)
                                            print(res.FailureReason)
                                            print(res.ResultingMapping?.Protocol)
                                        }

                                        var numFailed = result.count { !it?.Success!! }

                                        var anyFailed = numFailed > 0

                                        if (anyFailed) {

                                            val verbString = if(modifyCase) "modify" else "create"

                                            // all failed
                                            if (numFailed == result.size) {
                                                if (result.size == 1) {
                                                    MainActivity.showSnackBarViewLog("Failed to $verbString rule.")
                                                } else {
                                                    MainActivity.showSnackBarViewLog("Failed to create rules.")
                                                }
                                            } else {
                                                MainActivity.showSnackBarViewLog("Failed to create some rules.")
                                            }


//                                            var res = result[0]
//                                            res!!
//                                            // this will always be too long (text length) for a toast.
//                                            Toast.makeText(
//                                                PortForwardApplication.appContext,
//                                                "Failure - ${res.FailureReason!!}",
//                                                Toast.LENGTH_LONG
//                                            ).show()
                                        } else {
                                            MainActivity.showSnackBarShortNoAction("Success!")
                                        }
                                    }
                                }

                                if(ruleToEdit != null)
                                {
                                    if(ruleToEdit == portMappingRequestInput)
                                    {
                                        PortForwardApplication.OurLogger.log(
                                            Level.INFO,
                                            "Rule has not changed. Nothing to do."
                                        )
                                        navController.popBackStack()
                                    }
                                    else
                                    {
                                        // delete old rule and add new rules...

                                        fun onDeleteCallback(result: UPnPResult) {

                                            try {
                                                defaultRuleDeletedCallback(result)
                                                RunUIThread {
                                                    println("delete callback")
                                                    if (result.Success!!) {

                                                        // if successfully deleted original rule,
                                                        //   add new rule.
                                                        var future =
                                                            UpnpManager.CreatePortMappingRulesEntry(portMappingRequestInput, ::batchCallback)
                                                    }
                                                    else
                                                    {
                                                        // the old rule must still be remaining if it failed to delete
                                                        // so nothing has changed.
                                                        MainActivity.showSnackBarViewLog("Failed to modify entry.")

                                                    }
                                                }
                                            } catch (exception: Exception) {
                                                PortForwardApplication.OurLogger.log(
                                                    Level.SEVERE,
                                                    "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                                                )
                                                MainActivity.showSnackBarViewLog("Failed to modify entry.")
                                                throw exception
                                            }
                                        }

                                        var oldRulesToDelete = UpnpManager.splitUserInputIntoRules(ruleToEdit)

                                        UpnpManager.DeletePortMapping(oldRulesToDelete[0].realize(), ::onDeleteCallback) //TODO: handle delete multiple (when that becomes a thing)

                                        navController.popBackStack()
                                    }
                                }
                                else
                                {
                                    var future =
                                        UpnpManager.CreatePortMappingRulesEntry(portMappingRequestInput, ::batchCallback) //spilts into rules
                                    //showDialogMutable.value = false

                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text(
                                text = if(ruleToEdit == null) "CREATE" else "APPLY",
                                color = AdditionalColors.TextColorStrong,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
//                        Text("CREATE", modifier = Modifier.clickable {
//                            // navigate
//                        })
//                        IconButton(onClick = {
//
//                        })
//                        {
//                            Icon(Icons.Default.Sort, contentDescription = "Sort")
//                        }
                    }
                )
            },
            content = { it ->

                Column(modifier = Modifier
                    .padding(it)
                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally)
                {
                    CreateRuleContents(
                        hasSubmitted,
                        internalPortText,
                        internalPortTextEnd,
                        externalPortText,
                        externalPortTextEnd,
                        leaseDuration,
                        description,
                        descriptionHasError,
                        startInternalHasError,
                        endInternalHasError,
                        selectedProtocolMutable,
                        startExternalHasError,
                        endExternalHasError,
                        internalIp,
                        internalIpHasError,
                        gatewayIps,
                        externalDeviceText,
                        expandedInternal,
                        expandedExternal,
                        wanIpVersionOfGatewayIsVersion1
                    )
                }

            })
    }
}




//caused weird visual artifacts at runtime
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//@Preview
//fun Picker()
//{
//    SetupPreview()
//
//        Row()
//        {
//
//            for (i in 0..4) {
//                AndroidView(
//                    modifier = Modifier
//                        .weight(1.0f)
//                        .padding(10.dp),
////                    update = { view ->
////                        // Apply color and typography directly
////                        view.findViewById<TextView>(R.id.myTextView).apply {
////                            setTextColor(color.toArgb())
////                            setTextAppearance(textAppearance)
////                        }
////                    },
//                    factory = { context ->
//                        var np = NumberPicker(context)
//                        //np.textColor = MaterialTheme.colorScheme.primary
//                        np.setDisplayedValues(
//                            listOf(
//                                "1 hour",
//                                "2 hour",
//                                "3 hour"
//                            ).toTypedArray()
//                        )
//                        np.apply {
//                            setOnValueChangedListener { numberPicker, i, i2 -> }
//                            minValue = 0
//                            maxValue = 2
//
//                        }
//            }
//        }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun ourBar()
{
    var showDialogMutable = remember { mutableStateOf(false) }
    TopAppBar(
//                                modifier = Modifier.height(40.dp),
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null) // Your icon
                Spacer(modifier = Modifier.width(8.dp)) // Optional space between icon and title
                Text("1 Selected")
            }
        },
        actions = {
            IconButton(onClick = {  }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Selection")
            }

            OverflowMenu(showDialogMutable, showDialogMutable)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun BottomSheetSortBy() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "Sort By",
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = AdditionalColors.TextColorStrong,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        Column()
        {
            val numRow = 2
            val numCol = 3
            val curIndex = remember { mutableStateOf(SharedPrefValues.SortByPortMapping.sortByValue) }

            for (i in 0 until numRow) {
                Row(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    //FilterField.values().forEach {
                    for (j in 0 until numCol) {
                        val index = i * numCol + j
                        //todo if index is out of range of sortby.values then produce an empty
                        SortSelectButton(
                            modifier = Modifier.weight(1.0f),
                            text = SortBy.from(index).getShortName(),//FilterField.from(i).name,
                            isSelected = index == curIndex.value,
                            onClick = {

                                curIndex.value = index
                                SharedPrefValues.SortByPortMapping = SortBy.from(index)
                                UpnpManager.UpdateSorting()
                                UpnpManager.invokeUpdateUIFromData()
                                PortForwardApplication.instance.SaveSharedPrefs()

                            })
                    }
                    //}
                }
            }

            Divider(modifier = Modifier.fillMaxWidth())

            val asc = remember { mutableStateOf(SharedPrefValues.Ascending) }

            Row(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                //FilterField.values().forEach {
                for (j in 0 until 2) {
                    var ascendingButton = j == 0
                    SortSelectButton(
                        modifier = Modifier.weight(1.0f),
                        text = if(ascendingButton) "Ascending" else "Descending",//FilterField.from(i).name,
                        isSelected = ascendingButton == asc.value,
                        onClick = {

                            asc.value = ascendingButton
                            SharedPrefValues.Ascending = ascendingButton
                            UpnpManager.UpdateSorting()
                            UpnpManager.invokeUpdateUIFromData()
                            PortForwardApplication.instance.SaveSharedPrefs()

                        })
                }
            }
        }
    }
}

enum class SortBy(val sortByValue : Int) {
    Slot(0),
    Description(1),
    InternalPort(2),
    ExternalPort(3),
    Device(4),
    Expiration(5);

    companion object {
        fun from(findValue: Int): SortBy = SortBy.values().first { it.sortByValue == findValue }
    }

    fun getName() : String
    {
        return if(this == InternalPort) {
            "Internal Port"
        } else if(this == ExternalPort) {
            "External Port"
        } else {
            name
        }
    }

    fun getShortName() : String
    {
        return if(this == InternalPort) {
            "Int. Port"
        } else if(this == ExternalPort) {
            "Ext. Port"
        } else {
            name
        }
    }

    fun getComparer(ascending : Boolean): PortMapperComparatorBase {
        return when(this)
        {
            Slot -> PortMapperComparerSlot(ascending)
            Description -> PortMapperComparatorDescription(ascending)
            InternalPort -> PortMapperComparatorInternalPort(ascending)
            ExternalPort -> PortMapperComparatorExternalPort(ascending)
            Device -> PortMapperComparatorDevice(ascending)
            Expiration -> PortMapperComparatorExpiration(ascending)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun SortSelectButton(modifier : Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val buttonColor: Color
    val textColor: Color
    if (isSelected) {
        buttonColor = MaterialTheme.colorScheme.primary
        textColor = MaterialTheme.colorScheme.onPrimary
    } else {
        buttonColor = MaterialTheme.colorScheme.secondaryContainer
        textColor = MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier.then(
            Modifier
                .height(60.dp)
                .padding(6.dp)),
        colors = CardDefaults.cardColors(containerColor = buttonColor),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Row()
//            {
//                Icon(Icons.Filled.Ascending)
                Text(
                    modifier = Modifier.padding(2.dp),
                    text = text,
                    fontSize = 16.sp,
                    fontStyle = MaterialTheme.typography.headlineMedium.fontStyle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                )
            //}
        }
    }
}

fun launchMockUPnPSearch(activity : MainActivity, upnpElementsViewModel : UPnPElementViewModel)
{
    GlobalScope.launch {
        var iter = 0
        while(true)
        {
            iter++

            delay(1000L)
            activity.runOnUiThread {
                var index = Random.nextInt(0,upnpElementsViewModel.items.value!!.size+1)
                upnpElementsViewModel.insertItem(UPnPViewElement(PortMapping("Web Server $iter", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis(), GetPsuedoSlot())),index)
            }

        }

//            delay(10000L) // non-blocking delay for 5 seconds (5000 milliseconds)
//            println(remoteDeviceOfInterest?.displayString)
//            for (device in UpnpManager.GetUPnPService().registry.devices){
//                println(device.displayString)
//                println(device.type)
//            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ScaffoldDemo() {
    val materialBlue700= Color(0xFF1976D2)
    Scaffold(
        topBar = {



            TopAppBar(
                modifier = Modifier.height(36.dp),  // change the height here
                title = { Text(text = "hello world") },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Yellow))
                 },
        floatingActionButton = { FloatingActionButton(onClick = {}){
            Text("X")
        } },
        content = { it  ->
            Column(Modifier.padding(it)){
            Text("BodyContent")
            Text("BodyContent")
            Text("BodyContent")
            Text("BodyContent")
            Text("BodyContent")}
 },
    )
}


fun fallbackRecursiveSearch(rootDevice : RemoteDevice)
{
    // recursively look through devices
    var deviceList = mutableListOf<RemoteDevice>() //.toMutableList()
    deviceList.add(rootDevice)
    while(deviceList.isNotEmpty())
    {
        var deviceInQuestion = deviceList.removeAt(0)
        for (service in deviceInQuestion.services)
        {
            for (action in service.actions)
            {
                if (UpnpManager.ActionNames.contains(action.name))
                {
                    println("Service ${service.serviceType} contains relevant action ${action}")
                }
            }
        }
        deviceList.addAll(deviceInQuestion.embeddedDevices)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun EnterContextMenu()
{
    var showDialogMutable : MutableState<Any?> = remember { mutableStateOf(null) }
    var showDialog = remember { mutableStateOf(false) }
    //EnterContextMenu(showDialogMutable, showDialog, null)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterContextMenu(singleSelectedItem : MutableState<Any?>, showMoreInfoDialog : MutableState<Boolean>, navController : NavHostController)
{
    if(singleSelectedItem.value == null)
    {
        return;
    }

    var prop = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
        securePolicy = SecureFlagPolicy.SecureOff,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = true,
//            usePlatformDefaultWidth = false,
//            shape = RoundedCornerShape(16.dp),
//            backgroundColor = Color.LightGray // set your preferred color here
    )
    MyApplicationTheme {
        Dialog(
            onDismissRequest = { PortForwardApplication.showContextMenu.value = false },
            properties = prop,
        ) {
            Surface(
                shape = RoundedCornerShape(size = 6.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)

                ) {
                    // it redraws starting at this inner context...
                    if (singleSelectedItem.value != null) {


                        var menuItems: MutableList<Pair<String, () -> Unit>> = mutableListOf()
                        var portMapping = singleSelectedItem.value as PortMapping
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Edit"
                            ) {
                                //                val desc = arguments?.getString("description")
                                //                val internalIp = arguments?.getString("internalIp")
                                //                val internalRange = arguments?.getString("internalRange")
                                //                val externalIp = arguments?.getString("externalIp")
                                //                val externalRange = arguments?.getString("externalRange")
                                //                val protocol = arguments?.getString("protocol")
                                //                val leaseDuration = arguments?.getString("leaseDuration")
                                //                val enabled = arguments?.getBoolean("enabled")

                                ///{description}/{internalIp}/{internalRange}/{externalIp}/{externalRange}/{protocol}/{leaseDuration}/{enabled}
                                val uriBuilder = Uri.Builder()
                                    .path("full_screen_dialog")
                                    .appendQueryParameter("description", portMapping.Description)
                                    .appendQueryParameter("internalIp", portMapping.InternalIP)
                                    .appendQueryParameter("internalRange", portMapping.InternalPort.toString())
                                    .appendQueryParameter("externalIp", portMapping.ExternalIP)
                                    .appendQueryParameter("externalRange", portMapping.ExternalPort.toString())
                                    .appendQueryParameter("protocol", portMapping.Protocol)
                                    .appendQueryParameter("leaseDuration", portMapping.LeaseDuration.toString())
                                    .appendQueryParameter("enabled", portMapping.Enabled.toString())
                                val uri = uriBuilder.build()
                                navController.navigate(uri.toString())
                                //navController.navigate("full_screen_dialog/${portMapping.Description}/${portMapping.InternalIP}/${portMapping.InternalPort}/${portMapping.ActualExternalIP}/${portMapping.ExternalPort}/${portMapping.Protocol}/${portMapping.LeaseDuration}/${portMapping.Enabled}")
//                                Toast.makeText(
//                                    PortForwardApplication.appContext,
//                                    "Edit clicked",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                if (portMapping.Enabled) "Disable" else "Enable"
                            ) {

                                fun enableDisableCallback(result: UPnPCreateMappingResult) {
                                    //portMapping : PortMapping,
                                    UpnpManager.enableDisableDefaultCallback(result)
                                    RunUIThread {
                                        if (result.Success!!) {
                                            Toast.makeText(
                                                PortForwardApplication.appContext,
                                                "Success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                PortForwardApplication.appContext,
                                                "Failure - ${result.FailureReason!!}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }

                                var future = UpnpManager.DisableEnablePortMappingEntry(
                                    portMapping,
                                    !portMapping.Enabled,
                                    ::enableDisableCallback
                                )
//                                Toast.makeText(
//                                    PortForwardApplication.appContext,
//                                    "Disable clicked",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Delete"
                            ) {
                                var future = UpnpManager.DeletePortMappingEntry(portMapping)

//
//                                    Toast.makeText(
//                                        PortForwardApplication.appContext,
//                                        "Delete clicked",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>("More Info"
                            ) {
                                showMoreInfoDialog.value = true
                            }
                        )
                        var index = 0
                        var lastIndex = menuItems.size - 1
                        for (menuItem in menuItems) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        menuItem.second()
                                        PortForwardApplication.showContextMenu.value = false
                                    }
                                    .padding(vertical = 14.dp)
                            ) {
                                Text(
                                    text = menuItem.first,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 20.sp
                                )
                            }

                            if (index != lastIndex) {
                                Divider(color = Color.LightGray)
                            }
                            index++
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun EnterPortDialog()
{
    SetupPreview()
    var showDialogMutable = remember { mutableStateOf(false) }
    EnterPortDialog(showDialogMutable, true)
}

//In IGD release 1.0, a value of 0 was used to create a static port mapping. In version 2.0, it is no longer
//possible to create static port mappings via UPnP actions. Instead, an out-of-band mechanism is REQUIRED
//to do so (cf. WWW-administration, remote management or local management). In order to be backward
//compatible with legacy control points, the value of 0 MUST be interpreted as the maximum value (e.g.
//604800 seconds, which corresponds to one week).

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun EnterPortDialog(showDialogMutable : MutableState<Boolean>, isPreview : Boolean = false) {


    var prop = DialogProperties(
    dismissOnClickOutside = true,
    dismissOnBackPress = true,
    securePolicy = SecureFlagPolicy.SecureOff,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
    )
    MyApplicationTheme {
        Dialog(
            onDismissRequest = { showDialogMutable.value = false },
            properties = prop,
        ) {
            Surface(
                shape = RoundedCornerShape(size = 6.dp))
            {











                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
//                    .background(
//                        color = Color.DarkGray, shape = RoundedCornerShape(16.dp)
//                    )
                        .fillMaxWidth(1.0f)
                ) {
                    Text("Create New Rule", style = MaterialTheme.typography.headlineLarge, modifier = Modifier
                        .padding(6.dp, 6.dp)
                        .align(Alignment.Start))
                    Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp))




                }
            }
        }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.CreateRuleContents(hasSubmitted : MutableState<Boolean>,
                                   internalPortText: MutableState<String>,
                                   internalPortTextEnd: MutableState<String>,
                                   externalPortText: MutableState<String>,
                                   externalPortTextEnd: MutableState<String>,
                                   leaseDuration: MutableState<String>,
                                   description: MutableState<String>,
                                   descriptionHasError: MutableState<Boolean>,
                                   startInternalHasError: MutableState<Boolean>,
                                   endInternalHasError: MutableState<Boolean>,
                                   selectedProtocolMutable: MutableState<String>,
                                   startExternalHasError: MutableState<Boolean>,
                                   endExternalHasError: MutableState<Boolean>,
                                   internalIp: MutableState<String>,
                                   internalIpHasError: MutableState<Boolean>,
                                   gatewayIps : MutableList<String>,
                                   externalDeviceText: MutableState<String>,
                                   expandedInternal: MutableState<Boolean>,
                                   expandedExternal: MutableState<Boolean>,
                                   wanIpIsV1: State<Boolean>
)
{




    var showLeaseDialog = remember { mutableStateOf(false) }





    if(showLeaseDialog.value)
    {
        DurationPickerDialog(showLeaseDialog, leaseDuration, wanIpIsV1.value)
    }









    val descriptionErrorString = remember { mutableStateOf(validateDescription(description.value).second) }
    var interalIpErrorString = remember { mutableStateOf(validateInternalIp(internalIp.value).second) }




    Row(
        modifier = Modifier
            .fillMaxWidth(createNewRuleRowWidth)
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
    )
    {
        OutlinedTextField(
            value = description.value,
            onValueChange = {
                description.value = it;
                descriptionHasError.value = validateDescription(description.value).first
                descriptionErrorString.value = validateDescription(description.value).second
            },
            label = { Text("Description") },
            singleLine = true,
            modifier = Modifier
                .weight(0.4f, true)
            ,//.height(60.dp),
            isError = hasSubmitted.value && descriptionHasError.value,
            trailingIcon = {
                if (hasSubmitted.value && descriptionHasError.value) {
                    Icon(Icons.Filled.Error, descriptionErrorString.value, tint = MaterialTheme.colorScheme.error)
                }
            },
            supportingText = {
                if(hasSubmitted.value && descriptionHasError.value) {
                    Text(
                        descriptionErrorString.value,
                        color = MaterialTheme.colorScheme.error
                    )
                }
//                                if (triedToSubmit.value && descriptionError.value) {
//                                    Text("Description is empty", color = MaterialTheme.colorScheme.error)
//                                }
            },


            )
    }

    var textfieldSize by remember { mutableStateOf(Size.Zero) }

    var portStartSize = remember { mutableStateOf(IntSize.Zero) }

    DeviceRow()
    {
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = internalIp.value,
            onValueChange = {
                internalIp.value = it.filter { charIt -> charIt.isDigit() || charIt == '.' }
                internalIpHasError.value = validateInternalIp(internalIp.value).first
                interalIpErrorString.value = validateInternalIp(internalIp.value).second
            },
            isError = hasSubmitted.value && internalIpHasError.value,
            supportingText = {
                if(hasSubmitted.value && internalIpHasError.value) {
                    Text(
                        interalIpErrorString.value,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (hasSubmitted.value && internalIpHasError.value) {
                    Icon(Icons.Filled.Error, interalIpErrorString.value, tint = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true,
            label = { Text("Internal Device") },
            modifier = Modifier
                .weight(0.5f, true)
                //.height(60.dp)
                .onGloballyPositioned { coordinates ->
                    //This value is used to assign to the DropDown the same width
                    println("OurInternalDevice: " + coordinates.size.toSize())
                    // Get density.


                    fun pxToDp(context: Context, px: Float): Float {
                        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
                    }

//                                    val density = pxToDp(
//                                        PortForwardApplication.appContext,
//                                        (coordinates.size.height.toFloat())
//                                    )
                    textfieldSize = coordinates.size.toSize()
                }
        )
        Spacer(modifier = Modifier.width(8.dp))

        //pass in expanded, pass in startHasErrorString, pass in internalPortText, pass in StartExternalHasError
    // port start
        var startHasErrorString = remember { mutableStateOf(validateStartPort(internalPortText.value).second) }
        StartPortExpandable(internalPortText, startInternalHasError, startHasErrorString, hasSubmitted, expandedInternal, portStartSize)
    }

    AnimatedVisibility(
        visible = expandedInternal.value
    ) {
    //if(expandedInternal.value) {
        PortRangeRow(
            internalPortText,
            internalPortTextEnd,
            startInternalHasError,
            endInternalHasError,
            hasSubmitted,
            Modifier.weight(0.2f, true),
            portStartSize.value
        )
    }


//                    val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
//                    var expanded by remember { mutableStateOf(false) }
//                    var selectedOptionText by remember { mutableStateOf(options[0]) }


    var selectedText by externalDeviceText


    DeviceRow()
    {
//        var defaultModifier = Modifier
//            .weight(0.5f, true)
//            //.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        var defaultModifier = Modifier
            .weight(0.5f, true)
//            //.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        DropDownOutline(defaultModifier, externalDeviceText, gatewayIps, "External Device")
        Spacer(modifier = Modifier.width(8.dp))
        var startHasErrorString = remember { mutableStateOf(validateStartPort(externalPortText.value).second) }
        StartPortExpandable(externalPortText, startExternalHasError, startHasErrorString, hasSubmitted, expandedExternal, portStartSize)

    }

    AnimatedVisibility(
        visible = expandedExternal.value
    ) {
        PortRangeRow(
            externalPortText,
            externalPortTextEnd,
            startExternalHasError,
            endExternalHasError,
            hasSubmitted,
            Modifier.weight(0.2f, true),
            portStartSize.value
        )
    }

    var selectedPort by selectedProtocolMutable

    Row(
        modifier = Modifier
            .fillMaxWidth(createNewRuleRowWidth)
            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
    ) {
        var defaultModifier = Modifier
            .weight(0.5f, true)
        //.height(60.dp)
        //.height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        DropDownOutline(defaultModifier,//Size(500f, textfieldSize.height),
            selectedText = selectedProtocolMutable,
            suggestions = listOf(Protocol.TCP.str(), Protocol.UDP.str(), Protocol.BOTH.str()),
            "Protocol")

        Spacer(modifier = Modifier.width(8.dp))

        var isFocused = remember { mutableStateOf(false) }
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = leaseDuration.value,
            onValueChange = {

                var digitsOnly = it.filter { charIt -> charIt.isDigit() }
                if (digitsOnly.isBlank()) {
                    leaseDuration.value = digitsOnly

                } else{
                    leaseDuration.value = capLeaseDur(digitsOnly, wanIpIsV1.value)
                }

                            },
            label = { Text("Lease") },
            trailingIcon = {
                IconButton(onClick = { showLeaseDialog.value = true })
                {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = ""
                    ) //TODO set error on main theme if not already.
                }
            },
            modifier = Modifier
                .weight(0.5f, true)
                .onFocusChanged {

                    // one is allowed to temporarily leave blank.
                    // but on leaving it must be valid
                    if(!it.isFocused && leaseDuration.value.isBlank())
                    {
                        leaseDuration.value = "0"
                    }

                    if (it.isFocused) {
                        if (leaseDuration.value == "0 (max)") {
                            leaseDuration.value = "0"
                        }

                    } else {

                        if (leaseDuration.value == "0") {
                            leaseDuration.value = "0 (max)"
                        }
                    }
                },//isFocused.value = it.isFocused },
//            visualTransformation = if (leaseDuration.value == "0" && !isFocused.value)
//                PlaceholderTransformation("0 (max)")
//            else VisualTransformation.None,
            //.fillMaxWidth(.4f)
            //.height(60.dp)
        )

    }

//    var showDialogMutable = remember { mutableStateOf(true) }
//    var withButtons = false;
//    if(withButtons) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth(createNewRuleRowWidth)
//                .padding(
//                    top = PortForwardApplication.PaddingBetweenCreateNewRuleRows + 10.dp,
//                    bottom = 10.dp
//                ),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Button(
//                onClick = {
//                    //TODO
//                    showDialogMutable.value = false
//                },
//                shape = RoundedCornerShape(16),
//                modifier = Modifier
//                    .weight(.6f)
//                    .height(46.dp)
//            ) {
//                Text("Cancel")
//            }
//            Spacer(modifier = Modifier.padding(18.dp))
//            val interactionSource = remember { MutableInteractionSource() }
//
//            val text = buildAnnotatedString {
//                withStyle(
//                    style = SpanStyle(
//                        color = MaterialTheme.colorScheme.primary,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                ) {
//                    append("CREATE")
//                }
//            }
//
//            Button(
//                onClick = {
//
//                    hasSubmitted.value = true
//                    if (descriptionHasError.value ||
//                        startInternalHasError.value ||
//                        startExternalHasError.value ||
//                        endInternalHasError.value ||
//                        endExternalHasError.value ||
//                        internalIpHasError.value
//                    ) {
//                        // show toast and return
//                        return@Button;
//                    }
//
//                    //Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()
//
//                    fun batchCallback(result: MutableList<UPnPCreateMappingResult?>) {
//
//                        RunUIThread {
//
//
//                            //debug
//                            for (res in result) {
//                                res!!
//                                print(res.Success)
//                                print(res.FailureReason)
//                                print(res.ResultingMapping?.Protocol)
//                            }
//
//                            var numFailed = result.count { !it?.Success!! }
//
//                            var anyFailed = numFailed > 0
//
//                            if (anyFailed) {
//
//                                // all failed
//                                if (numFailed == result.size) {
//                                    if (result.size == 1) {
//                                        MainActivity.showSnackBarViewLog("Failed to create rule.")
//                                    } else {
//                                        MainActivity.showSnackBarViewLog("Failed to create rules.")
//                                    }
//                                } else {
//                                    MainActivity.showSnackBarViewLog("Failed to create some rules.")
//                                }
//
//
////                                            var res = result[0]
////                                            res!!
////                                            // this will always be too long (text length) for a toast.
////                                            Toast.makeText(
////                                                PortForwardApplication.appContext,
////                                                "Failure - ${res.FailureReason!!}",
////                                                Toast.LENGTH_LONG
////                                            ).show()
//                            } else {
//                                MainActivity.showSnackBarShortNoAction("Success!")
//                            }
//                        }
//                    }
//
//                    var portMappingRequestInput = PortMappingUserInput(
//                        description.value,
//                        internalIp.value,
//                        internalPortText.value,
//                        externalDeviceText.value,
//                        externalPortText.value,
//                        selectedProtocolMutable.value,
//                        leaseDuration.value,
//                        true
//                    )
//                    var future =
//                        UpnpManager.CreatePortMappingRules(portMappingRequestInput, ::batchCallback)
//                    showDialogMutable.value = false
//                },
//                shape = RoundedCornerShape(16),
//                modifier = Modifier
//                    .weight(1.0f)
//                    .height(46.dp),
//
//                )
//            {
//                Text("Create")
//            }
//        }
//    }
}

// v1 - max is ui4 maxvalue (which is 100+ years, so just cap at signed int4 max)
// v2 - max is 1 week (604800)
fun capLeaseDur(leaseDurString : String, v1: Boolean) : String
{
    return if(v1)
    {
        leaseDurString.toIntOrMaxValue().toString()
    }
    else
    {
        val leaseInt = leaseDurString.toIntOrMaxValue()
        minOf(leaseInt, 604800).toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPortExpandable(portText : MutableState<String>, hasError : MutableState<Boolean>, errorString : MutableState<String>, hasSubmitted: MutableState<Boolean>, expanded : MutableState<Boolean>, startPortSize : MutableState<IntSize>)
{
    OutlinedTextField(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        value = portText.value,
        modifier = Modifier
            .fillMaxWidth(.4f)
            .onSizeChanged { startPortSize.value = it },
        //.weight(0.4f, true),
        singleLine = true,
        onValueChange = {
            portText.value = it.filter { charIt -> charIt.isDigit() }
            hasError.value = validateStartPort(portText.value).first
            errorString.value = validateStartPort(portText.value).second
        },
        label = { if(expanded.value) Text("Port Start") else Text("Port") },
        //modifier = Modifier.then(modifier),
        //.height(60.dp),
        isError = hasSubmitted.value && hasError.value,
        supportingText = {
            if(hasSubmitted.value && hasError.value)
            {
                Text(errorString.value, color = MaterialTheme.colorScheme.error)
            }
        },
        trailingIcon = {
            if(hasSubmitted.value && hasError.value)
            {
                Icon(Icons.Filled.Error, contentDescription = errorString.value, tint = MaterialTheme.colorScheme.error) //TODO set error on main theme if not already.
            }
            else
            {
                IconButton(onClick = { expanded.value = !expanded.value })
                {
                    if (expanded.value) {
                        Icon(
                            Icons.Filled.UnfoldLess,
                            contentDescription = ""
                        ) //TODO set error on main theme if not already.
                    } else {
                        Icon(
                            Icons.Filled.UnfoldMore,
                            contentDescription = ""
                        ) //TODO set error on main theme if not already.
                    }
                }
            }
        }
    )
}

val createNewRuleRowWidth = 1.0f

fun validateDescription(description : String) : Pair<Boolean, String>
{
    if(description.isEmpty())
    {
        return Pair(true, "Cannot be empty")
    }
    return Pair(false, "")
}

fun validateStartPort(startPort : String) : Pair<Boolean, String>
{
    if(startPort.isEmpty())
    {
        return Pair(true, "Port cannot be empty")
    }
    val portInt = startPort.toIntOrMaxValue()
    if(portInt < MIN_PORT || portInt > MAX_PORT)
    {
        return Pair(true, "Port must be between $MIN_PORT and $MAX_PORT")
    }
    return Pair(false, "")
}

fun validateEndPort(startPort : String, endPort : String) : Pair<Boolean, String>
{
    if(startPort.isEmpty())
    {
        // let them deal with that error first
        return Pair(false, "")
    }
    if(endPort.isEmpty())
    {
        //valid
        return Pair(false, "")
    }

    if(endPort.isEmpty())
    {
        //valid
        return Pair(false, "")
    }

    val startPortInt = startPort.toIntOrMaxValue()
    val endPortInt = startPort.toIntOrMaxValue()

    if(endPortInt < MIN_PORT || endPortInt > MAX_PORT)
    {
        return Pair(true, "Port must be between $MIN_PORT and $MAX_PORT")
    }

    if(startPortInt > endPortInt)
    {
        return Pair(true, "End port must be after start")
    }


    return Pair(false, "")
}

fun String.toIntOrMaxValue(): Int {
    return try {
        this.toInt()
    }
    catch(e : Exception)
    {
        Int.MAX_VALUE
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun outlineTextWithPicker()
{
    Column()
    {
        var expanded = remember { mutableStateOf(false) };
        Row()
        {
            OutlinedTextField(
                "Device",
                modifier = Modifier.weight(.5f),
                onValueChange = {

                },
                trailingIcon = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField("Port",
                onValueChange = {

                },
                modifier = Modifier.weight(.5f),
                trailingIcon = {
                    IconButton(onClick = { expanded.value = !expanded.value })
                    {
                        if (expanded.value) {
                            Icon(
                                Icons.Filled.UnfoldLess,
                                contentDescription = ""
                            ) //TODO set error on main theme if not already.
                        } else {
                            Icon(
                                Icons.Filled.UnfoldMore,
                                contentDescription = ""
                            ) //TODO set error on main theme if not already.
                        }


                    }
                }
            )
        }

    val expandAnimation by animateDpAsState(
        targetValue = if (expanded.value) 60.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 2000, // Duration of the animation
            easing = LinearEasing // Animation easing
        )
    )
        

//        AnimatedVisibility(
//            visible = expanded.value,
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
    if (expanded.value) {
        Row(modifier = Modifier.height(expandAnimation))
        {
            Spacer(
                modifier = Modifier.weight(.5f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField("Port",
                onValueChange = {

                },
                modifier = Modifier.weight(.5f),
                trailingIcon = {
                    IconButton(onClick = { expanded.value = !expanded.value })
                    {
                        if (expanded.value) {
                            Icon(
                                Icons.Filled.UnfoldLess,
                                contentDescription = ""
                            ) //TODO set error on main theme if not already.
                        } else {
                            Icon(
                                Icons.Filled.UnfoldMore,
                                contentDescription = ""
                            ) //TODO set error on main theme if not already.
                        }


                    }
                }
            )
        }
    }
        //}

        Row()
        {
            OutlinedTextField(
                "External Device",
                modifier = Modifier.weight(.5f),
                onValueChange = {

                },
                trailingIcon = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField("Port",
                onValueChange = {

                },
                modifier = Modifier.weight(.5f),
                trailingIcon = {
                    IconButton(onClick = {  })
                    {

                        Icon(
                            Icons.Filled.UnfoldMore,
                            contentDescription = ""
                        ) //TODO set error on main theme if not already.



                    }
                }
            )
        }
    }


}

fun validateInternalIp(ip : String) : Pair<Boolean, String>
{
    val regexIPv4 = """^(25[0-5]|2[0-4]\d|[0-1]?\d?\d)(\.(25[0-5]|2[0-4]\d|[0-1]?\d?\d)){3}$""".toRegex()
    return if(regexIPv4.matches(ip)) Pair(false, "") else Pair(true, "Must be valid IP address")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortRangeRow(startPortText : MutableState<String>, endPortText : MutableState<String>, startHasError : MutableState<Boolean>, endHasError : MutableState<Boolean>, hasSubmitted : MutableState<Boolean>, modifier : Modifier, portSize : IntSize)
{
    DeviceRow()
    {

        var endHasErrorString = remember { mutableStateOf(validateEndPort(startPortText.value,endPortText.value).second) }

        Spacer(modifier = Modifier.weight(0.5f, true))

        Spacer(modifier = Modifier.width(8.dp))

        Text("to", modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.width(8.dp))

        

        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = endPortText.value,
            onValueChange = {
                endPortText.value = it.filter { charIt -> charIt.isDigit() }
                endHasError.value = validateEndPort(startPortText.value, endPortText.value).first
                endHasErrorString.value = validateEndPort(startPortText.value, endPortText.value).second
                            },
            label = { Text("Port End") },
            modifier = Modifier.width(with(LocalDensity.current) { portSize.width.toDp() }),
            isError = hasSubmitted.value && endHasError.value,
            supportingText = {
                if(hasSubmitted.value && endHasError.value)
                {
                    Text(endHasErrorString.value, color = MaterialTheme.colorScheme.error)
                }
            },


            trailingIcon =
                if (hasSubmitted.value && endHasError.value) {
                    @Composable {
                        if(hasSubmitted.value && endHasError.value)
                        {
                            Icon(Icons.Filled.Error, contentDescription = endHasErrorString.value, tint = MaterialTheme.colorScheme.error) //TODO set error on main theme if not already.
                        }
                    }
                } else {
                    null
                }

//
//            {
//                if(hasSubmitted.value && endHasError.value)
//                {
//                    Icon(Icons.Filled.Error, contentDescription = endHasErrorString.value, tint = MaterialTheme.colorScheme.error) //TODO set error on main theme if not already.
//                }
//            },
//            visualTransformation = if (endPortText.value.isEmpty())
//                PlaceholderTransformation("-")
//            else VisualTransformation.None,
        )
//        Surface(
//            shape = RoundedCornerShape(6.dp), // Adjust corner size to your preference
//            color = Color.DarkGray, // Adjust background color to your preference
//            modifier = Modifier.padding(8.dp) // Adjust padding to your preference
//        ) {

        //}
    }
}


fun RunUIThread(runnable: Runnable)
{
    val mainLooper = Looper.getMainLooper()

    // Create a Handler to run some code on the UI thread
    val handler = Handler(mainLooper)
    handler.post(runnable)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownOutline(defaultModifier : Modifier, selectedText : MutableState<String>, suggestions : List<String>, outlineLabel : String)
{
//    var defaultModifier = Modifier.then(Modifier)
//    if(textfieldSize != null)
//    {
//        // bind size
//        defaultModifier = Modifier
//            .width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
//    }
    var expanded by remember { mutableStateOf(false) }
    val icon = if (expanded)
        Icons.Filled.ArrowDropUp //it requires androidx.compose.material:material-icons-extended
    else
        Icons.Filled.ArrowDropDown

    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }


    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = defaultModifier
            .then(
                Modifier.clickable(interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        println("TextField clicked")
                        expanded = !expanded
                        focusRequester.requestFocus()
                    })
            )
            .onSizeChanged { boxSize = it }
    ) {

        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            readOnly = true,
            value = selectedText.value,
            onValueChange = { selectedText.value = it },
            interactionSource = interactionSource,
            modifier = defaultModifier
                .onGloballyPositioned { coordinates ->

                    println("OurExternalDevice: " + coordinates.size.toSize())
                    //This value is used to assign to the DropDown the same width
                    //textfieldSize = coordinates.size.toSize()
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        println("TextField clicked")
                        expanded = !expanded
                    }
                )
                .focusRequester(focusRequester),
            label = { Text(outlineLabel) },
            trailingIcon = {
                Icon(icon, "contentDescription",
                    Modifier.clickable { expanded = !expanded })
            }
        )
        //println(textfieldSize.toString())
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { boxSize.width.toDp() })
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        println("DropdownMenuItem clicked")
                        expanded = !expanded
                    }
                ),
        ) {
            suggestions.forEach { label ->
                DropdownMenuItem(
                    text = {
                        Text(text = label)
                    },
                    onClick = {
                        selectedText.value = label
                        expanded = false
                    })
            }
        }
        Box(
//                            onClick = {
//                                println("TextField clicked")
//                                expanded = !expanded
//                            },
            modifier = Modifier
                .width(with(LocalDensity.current) { boxSize.width.toDp() })
                .background(Color.Blue)
//                            .onGloballyPositioned { coordinates ->
//                                //This value is used to assign to the DropDown the same width
//                                //textfieldSize = coordinates.size.toSize()
//                            }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        println("TextField clicked")
                        expanded = !expanded
                        focusRequester.requestFocus()
                    }
                ),
        )
    }
}

@Composable
fun DeviceRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(createNewRuleRowWidth)
            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top // for the error text
    ) {
        content(this)
    }
}

@Composable
fun OverflowMenu(showDialog : MutableState<Boolean>, showAboutDialogState : MutableState<Boolean>) {
    var expanded by remember { mutableStateOf(false) }




    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "menu")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        // this gets called on expanded, so I dont think we need to monitor additional state.

        val items : MutableList<String> = mutableListOf()
        if(MainActivity.MultiSelectItems!!.isEmpty())
        {
            items.add("Refresh")
            if(UpnpManager.IGDDevices.isNotEmpty())
            {
                var (anyEnabled, anyDisabled) = UpnpManager.GetExistingRuleInfos()
                if(anyEnabled) // also get info i.e. any enabled, any disabled
                {
                    items.add("Disable All")
                }
                if (anyDisabled)
                {
                    items.add("Enable All")
                }
                if(anyDisabled || anyEnabled)
                {
                    items.add("Delete All")
                }
            }
            items.add("View Log")
            items.add("Settings")
            items.add("About")
        }
        else
        {
            var anyEnabled = MainActivity.MultiSelectItems!!.any() { it -> it.Enabled }
            var anyDisabled = MainActivity.MultiSelectItems!!.any() { it -> !it.Enabled }
            if(anyEnabled)
            {
                items.add("Disable")
            }
            if(anyDisabled)
            {
                items.add("Enable")
            }
        }

        var test : List<PortMapping>? = MainActivity.MultiSelectItems

        items.forEach { label ->
            DropdownMenuItem(text = { Text(label) }, onClick = {
                // handle item click
                expanded = false

                when (label) {
                    "Refresh" ->
                    {
                        UpnpManager.FullRefresh()
                    }
                    "Disable All" ->
                    {
                        enableDisableAll(false)
                    }
                    "Enable All" ->
                    {
                        enableDisableAll(true)
                    }
                    "Disable" ->
                    {
                        enableDisableAll(false, MainActivity.MultiSelectItems)
                    }
                    "Enable" ->
                    {
                        enableDisableAll(true, MainActivity.MultiSelectItems)
                    }
                    "Delete All" ->
                    {
                        deleteAll()
                    }
                    "View Log" ->
                    {
                        val intent =
                            Intent(PortForwardApplication.CurrentActivity, LogViewActivity::class.java)
                        PortForwardApplication.CurrentActivity?.startActivity(intent)
                    }
                    "Settings" ->
                    {
                        val intent =
                            Intent(PortForwardApplication.CurrentActivity, SettingsActivity::class.java)
                        PortForwardApplication.CurrentActivity?.startActivity(intent)
                    }
                    "About" ->
                    {
                        showAboutDialogState.value = true
                        println("Item 1 pressed")
                    }
                }
            })
        }
    }
}

fun deleteAll(chosenOnly : List<PortMapping>? = null)
{
    fun batchCallback(result : MutableList<UPnPResult?>) {

        RunUIThread {

            //debug
            for (res in result)
            {
                res!!
                print(res.Success)
                print(res.FailureReason)
                print(res.RequestInfo?.Description)
            }

            var anyFailed = result.any {!it?.Success!!}

            if(anyFailed) {
                var res = result.first {!it?.Success!!}
                res!!
                Toast.makeText(
                    PortForwardApplication.appContext,
                    "Failure - ${res.FailureReason!!}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else
            {
                Toast.makeText(
                    PortForwardApplication.appContext,
                    "Success",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // get all enabled. note: need to clone.
    var rules = chosenOnly?.toList() ?: UpnpManager.GetAllRules();
    var future = UpnpManager.DeletePortMappingsEntry(rules, ::batchCallback)
}

fun enableDisableAll(enable : Boolean, chosenRulesOnly : List<PortMapping>? = null)
{
    fun batchCallback(result : MutableList<UPnPCreateMappingResult?>) {

        RunUIThread {

            //debug
            for (res in result)
            {
                res!!
                print(res.Success)
                print(res.FailureReason)
                print(res.ResultingMapping?.Protocol)
            }

            var anyFailed = result.any {!it?.Success!!}

            if(anyFailed) {
                var res = result.first {!it?.Success!!}
                res!!
                Toast.makeText(
                    PortForwardApplication.appContext,
                    "Failure - ${res.FailureReason!!}",
                    Toast.LENGTH_LONG
                ).show()
            }
            else
            {
                Toast.makeText(
                    PortForwardApplication.appContext,
                    "Success",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if(chosenRulesOnly != null)
    {
        var rules = chosenRulesOnly.filter { it -> it.Enabled != enable };
        UpnpManager.DisableEnablePortMappingEntries(rules, enable, ::batchCallback)
    }
    else {
        var rules = UpnpManager.GetEnabledDisabledRules(!enable);
        UpnpManager.DisableEnablePortMappingEntries(rules, enable, ::batchCallback)
    }
}

fun formatIpv4(ipAddr : Int) : String
{
    val byteBuffer = ByteBuffer.allocate(4)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putInt(ipAddr)
    val inetAddress = InetAddress.getByAddress(null, byteBuffer.array())
    return inetAddress.hostAddress
}



//fun getLocalIpAddress(context: Context): InetAddress? {
//    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//    var ipAddress = wifiManager.connectionInfo.ipAddress
//    var macAddress = wifiManager.connectionInfo.macAddress
//
//    // Convert little-endian to big-endian if needed
////    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
////        ipAddress = Integer.reverseBytes(ipAddress)
////    }
//
//    val ipByteArray = ByteArray(4)
//    ipByteArray[0] = (ipAddress and 0xff).toByte()
//    ipByteArray[1] = (ipAddress shr 8 and 0xff).toByte()
//    ipByteArray[2] = (ipAddress shr 16 and 0xff).toByte()
//    ipByteArray[3] = (ipAddress shr 24 and 0xff).toByte()
//
//    return try {
//        InetAddress.getByAddress(ipByteArray)
//    } catch (e: UnknownHostException) {
//        null
//    }
//}

class UPnPElementViewModel: ViewModel() {
    private val _items = MutableLiveData<List<UPnPViewElement>>(emptyList())
    val items: LiveData<List<UPnPViewElement>> get() = _items

    fun setData(data : List<UPnPViewElement>)
    {
        _items.value = data
    }

    fun addItem(item: UPnPViewElement) {
        _items.value = _items.value!! + listOf(item)
    }

    fun insertItem(item: UPnPViewElement, index : Int) {
        _items.value = _items.value!!.subList(0,index) + listOf(item) + _items.value!!.subList(index, _items.value!!.size)
    }
}





fun main() {
    GlobalScope.launch {
        delay(5000L) // non-blocking delay for 5 seconds (5000 milliseconds)
        println("Hello from a separate thread after 5 seconds!")
    }

    // Keep the main thread alive, otherwise the program will exit before the separate thread gets a chance to print
    Thread.sleep(6000L)
}


data class Message(val name : String, val msg : String)

fun _getDefaultPortMapping() : PortMapping
{
    return PortMapping("Web Server", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis(), 0)
}

@Preview
@Composable
fun PreviewConversation() {
    MyApplicationTheme() {

        val msgs = mutableListOf<UPnPViewElement>()
        val pm = _getDefaultPortMapping()
        val upnpViewEl = UPnPViewElement(pm)
        for (i in 0..20)
        {
            msgs.add(upnpViewEl)
        }
        Conversation(msgs)
    }

    //}
}

@Composable
fun ConversationEntryPoint(modelView : UPnPElementViewModel)
{
    val items by modelView.items.observeAsState(emptyList())
    Conversation(items)
}

//lazy column IS recycler view basically. both recycle.
@OptIn(ExperimentalFoundationApi::class, ExperimentalUnitApi::class, ExperimentalMaterialApi::class)
@Composable
fun Conversation(messages: List<UPnPViewElement>) {


        LazyColumn(
            //modifier = Modifier.background(MaterialTheme.colorScheme.background),
            modifier = Modifier
                .background(AdditionalColors.Background)
                .fillMaxHeight()
                .fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),

            ) {
//        stickyHeader {
//            Column( modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxWidth())
//            {
//                Text("DEVICE IS THIS")
//                Text("DEVICE IS THIS")
//            }
//        }

            itemsIndexed(messages) { index, message -> //, key = {indexIt, keyIt -> keyIt.hashCode() }

                if(message.IsSpecialEmpty)
                {
                    NoMappingsCard(message.GetUnderlyingIGDDevice())
                }
                else if(message.IsIGDDevice())
                {
                    DeviceHeader(message.GetUnderlyingIGDDevice())
                }
                else
                {
                    PortMappingCard(message.GetUnderlyingPortMapping(), Modifier.animateItemPlacement())
                }


//            Box(modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer))
//            {
                //PortMapping("Web Server", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0))
                //}

//            if(index == 2)
//            {
//                Column( modifier = Modifier
//                    .background(
//                        MaterialTheme.colorScheme.tertiaryContainer,
//                        shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)
//                    )
//                    .fillMaxWidth()
//                    .padding(4.dp, 0.dp))
//                {
////                    Divider(color = Color.Gray, thickness = 1.dp)
//                    Spacer(modifier = Modifier.padding(4.dp))
//                }
//                Spacer(modifier = Modifier.padding(4.dp))
//            }


            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // so FAB doesnt get in way
            }


        }

}

@Composable
fun MyScreen(myViewModel: UPnPElementViewModel = UPnPElementViewModel()) {
    val items by myViewModel.items.observeAsState(emptyList())

    LazyColumn {
        items(items) { item ->
            Text(item.UnderlyingElement.toString())
        }
    }
}




@Composable
fun MessageCard(name: String, message : String, even : Boolean) {
    Row {
        Text("row I guess\r\ntest\r\ntest1", style = TextStyle(fontSize = 10.sp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = "Hello $name!", color = if (even) Color.Blue else Color.Red)
            Text(text = "$message!", color =  if (even) Color.Blue else Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageCard() {
    SetupPreview()
    MessageCard("Android", "I want to chat", true)
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PortMappingCard()
{
    SetupPreview()
        PortMappingCard(
            PortMapping(
                "Web Server",
                "192.168.18.1",
                "192.168.18.13",
                80,
                80,
                "UDP",
                true,
                0,
                "192.168.18.1",
                System.currentTimeMillis(),
                0
            )
        )
}

@Preview(showBackground = true)
@Composable
fun PortMappingCardAlt()
{
    SetupPreview()
    MyApplicationTheme() {

    PortMappingCardAlt(
        PortMapping(
            "Web Server",
            "192.168.18.1",
            "192.168.18.13",
            80,
            80,
            "UDP",
            true,
            0,
            "192.168.18.1",
            System.currentTimeMillis(),
            0
        )
    )

    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun PortMappingCardAlt(portMapping: PortMapping)
{
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp, 4.dp)
//            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { },
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors(
//            containerColor = Color(0xffc5dceb),
        ),

        ) {
        Row(
            modifier = Modifier
                .padding(15.dp, 6.dp),//.background(Color(0xffc5dceb)),
            //.background(MaterialTheme.colorScheme.secondaryContainer),
            verticalAlignment = Alignment.CenterVertically

        ) {
            Column(modifier = Modifier.weight(1f)){
                Text(portMapping.Description, fontSize = TextUnit(20f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold)
                Text("${portMapping.InternalIP}")
                Text("${portMapping.ExternalPort} ➝ ${portMapping.InternalPort} • ${portMapping.Protocol}")
            }


//                buildAnnotatedString {
//                    append("welcome to ")
//                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
//                    ) {
//                        append("Jetpack Compose Playground")
//                    }
//                }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "On",
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(0.dp)
                        .background(color = Color(0xFF8FCE91), shape = RoundedCornerShape(10.dp))
                        .padding(16.dp, 8.dp),
                )

            }
        }



//        Column(
//            modifier = Modifier.padding(15.dp)
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically){
//                Text(portMapping.Description, fontSize = TextUnit(20f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold)
//                Text(portMapping.Protocol)
//                Text("Enabled")
//                Text(
//                    text = "On",
//                    modifier = Modifier
//                        .padding(2.dp)
//                        .background(color = Color(0xff90ee90), shape = RoundedCornerShape(10.dp))
//                        .padding(8.dp),
//                    color = Color.Black
//                )
//            }
//
//
////                buildAnnotatedString {
////                    append("welcome to ")
////                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
////                    ) {
////                        append("Jetpack Compose Playground")
////                    }
////                }
//            Row() {
//                Text(portMapping.LocalIP)
//                Text(" • 80 → 80") // ➝ ➜
//            }
//        }
    }
}

// TODO: for mock purposes
class IGDDeviceHolder
{

    var displayName: String = "Nokia"
        get(){
            return ""
        }

}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun DeviceHeader(device : IGDDevice)
{
    Spacer(modifier = Modifier.padding(2.dp))
    Column(
        modifier = Modifier
//                    .background(
//                        MaterialTheme.colorScheme.secondaryContainer,
//                        shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)
//                    )
            .fillMaxWidth()
            .padding(6.dp, 4.dp)
    )
    {
//                    Divider(color = Color.Gray, thickness = 1.dp)
        Text(
            device.displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = TextUnit(24f, TextUnitType.Sp),
            color = AdditionalColors.TextColor

        )
        Text(device.ipAddress, color = AdditionalColors.TextColor)
    }
    Spacer(modifier = Modifier.padding(2.dp))
}

@Preview
@Composable
fun LoadingIcon()
{
    LoadingIcon("Searching for devices", Modifier)
}

@Composable
fun LoadingIcon(label : String, modifier : Modifier)
{
    Column(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(160.dp), strokeWidth = 6.dp, color = MaterialTheme.colorScheme.secondary)
        Text("Searching for devices", modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(0.dp, 30.dp, 0.dp, 0.dp))
    }

}

@Composable
fun NoMappingsCard(remoteDevice : IGDDevice) {
    NoMappingsCard()
}

@Preview
@Composable
fun NoMappingsCard()
{
    MyApplicationTheme() {
        Card(
//        onClick = {
//            if(PortForwardApplication.showPopup != null)
//            {
//                PortForwardApplication.showPopup.value = true
//            }
//                  },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp, 4.dp),
//            .background(MaterialTheme.colorScheme.secondaryContainer)
//                .clickable {
//
//                    showAddRuleDialogState.value =
//                    //PortForwardApplication.currentSingleSelectedObject.value = portMapping
//                },
            elevation = CardDefaults.cardElevation(),
            colors = CardDefaults.cardColors(
                containerColor = AdditionalColors.CardContainerColor,
            ),
            border = BorderStroke(1.dp, AdditionalColors.SubtleBorder),

            ) {

            Row(
                modifier = Modifier
                    .padding(15.dp, 36.dp),//.background(Color(0xffc5dceb)),
                //.background(MaterialTheme.colorScheme.secondaryContainer),
                verticalAlignment = Alignment.CenterVertically

            ) {
                Column(modifier = Modifier.weight(1f)) {


                    // this one is awkward if one intentionally removes all port mappings
                    //var deviceHasNoPortMappings = "No port mappings found \nfor this device"
                    var deviceHasNoPortMappings = "Device has no UPnP port mappings"

                    Text(
                        "No port mappings found \nfor this device",
                        fontSize = TextUnit(20f, TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(0.dp, 0.dp, 0.dp, 8.dp),
                        textAlign = TextAlign.Center,
                        color = AdditionalColors.TextColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap ",
                            color = AdditionalColors.TextColor
                            //color = MaterialTheme.colors.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add icon",
                            tint = AdditionalColors.TextColor
                            //tint = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = " to add new rules",
                            //style = MaterialTheme.typography.body1,
                            color = AdditionalColors.TextColor
                        )
                    }
//                    Text("${portMapping.LocalIP}")

//                    val text = buildAnnotatedString {
//                        withStyle(style = SpanStyle(color = AdditionalColors.Enabled_Green)) {
//                            append("⬤")
//                        }
//                        withStyle(style = SpanStyle()) {
//                            append(if (portMapping.Enabled) " Enabled" else " Disabled")
//                        }
//                    }
//
//
//                    Text(text)
                }
//
//
////                buildAnnotatedString {
////                    append("welcome to ")
////                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
////                    ) {
////                        append("Jetpack Compose Playground")
////                    }
////                }
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(
//                        "${portMapping.ExternalPort} ➝ ${portMapping.InternalPort}",
//                        fontSize = TextUnit(20f, TextUnitType.Sp),
//                        fontWeight = FontWeight.SemiBold
//                    )
//                    Text("${portMapping.Protocol}")
//
//                }
        }
        }
    }
}

@OptIn(ExperimentalUnitApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PortMappingCard(portMapping: PortMapping, additionalModifier : Modifier = Modifier)
{
    println("external ip test ${portMapping.ExternalIP}")

    MyApplicationTheme(){

        //var isRound by remember { mutableStateOf(true)}
        //val borderRadius by animateIntAsState(targetValue = if(isRound) 100 else 0, animationSpec = tween(durationMillis = 2000))

    Card(
//        onClick = {
//            if(PortForwardApplication.showPopup != null)
//            {
//                PortForwardApplication.showPopup.value = true
//            }
//                  },
        modifier = additionalModifier
            .fillMaxWidth()
            //.clip(RoundedCornerShape(borderRadius))
            .padding(4.dp, 4.dp)
//            .background(MaterialTheme.colorScheme.secondaryContainer)
            .combinedClickable(
                onClick = {

                    if (IsMultiSelectMode()) {
                        ToggleSelection(portMapping)
                    } else {
                        PortForwardApplication.showContextMenu.value = true
                        PortForwardApplication.currentSingleSelectedObject.value = portMapping
                    }

                },
                onLongClick = {
                    ToggleSelection(portMapping)
                }
            ),


//                Snackbar
//                    .make(parentlayout, "This is main activity", Snackbar.LENGTH_LONG)
//                    .setAction("CLOSE", object : OnClickListener() {
//                        fun onClick(view: View?) {}
//                    })
//                    .setActionTextColor(getResources().getColor(R.color.holo_red_light))
//                    .show()
                //isRound = !isRound


            elevation = CardDefaults.cardElevation(),
            border = BorderStroke(1.dp, AdditionalColors.SubtleBorder),
            colors = CardDefaults.cardColors(
                containerColor = AdditionalColors.CardContainerColor,
            ),
    ) {

        Row(
            modifier = Modifier
                .padding(2.dp, 6.dp, 15.dp, 6.dp),//.background(Color(0xffc5dceb)),
            //.background(MaterialTheme.colorScheme.secondaryContainer),
            verticalAlignment = Alignment.CenterVertically

        ) {

            var multiSelectMode = IsMultiSelectMode()
            //var padLeft = if(multiSelectMode) 5.dp else 13.dp

            //TODO need to do existing content slide to right and fade new element in

            val padLeft = 13.dp
            //val transition = updateTransition(multiSelectMode, label = "transition")

//            val offsetState = remember { MutableTransitionState(initialState = false) }
//            val visState = remember { MutableTransitionState(initialState = false) }
//
////            val padLeft by transition.animateDp(
////                transitionSpec = {
////                    if (false isTransitioningTo true) {
////                        spring(stiffness = Spring.StiffnessLow)
////                    } else {
////                        spring(stiffness = Spring.StiffnessLow)
////                    }
////                },
////                label = "offset transition",
////            ) { isVisible -> if (isVisible) 5.dp else 13.dp}
//
//
//            val padLeft by animateDpAsState(
//                if (offsetState.currentState) 30.dp else 5.dp,
//                finishedListener = {
//                   if (it == 30.dp) {
//                       offsetState.targetState = false
//                   }
//                }
//            )
//
//            offsetState.targetState = multiSelectMode

            AnimatedVisibility(
                visible = multiSelectMode,
            ) {


                CircleCheckbox(
                    MainActivity.MultiSelectItems!!.contains(portMapping),
                    true,
                    Modifier.padding(10.dp, 0.dp, 2.dp, 0.dp)
                ) {

                    ToggleSelection(portMapping)

                }
            }

            Column(modifier = Modifier.weight(1f).padding(padLeft,0.dp,0.dp,0.dp)) {
                Text(
                    portMapping.Description,
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,
                    color = AdditionalColors.TextColor
                )
                Text("${portMapping.InternalIP}", color = AdditionalColors.TextColor)

                val text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = if (portMapping.Enabled) AdditionalColors.Enabled_Green else AdditionalColors.Disabled_Red)) {
                        append("⬤")
                    }
                    withStyle(style = SpanStyle()) {
                        append(if (portMapping.Enabled) " Enabled" else " Disabled")
                    }
                }


                Text(text, color = AdditionalColors.TextColor)
            }


//                buildAnnotatedString {
//                    append("welcome to ")
//                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
//                    ) {
//                        append("Jetpack Compose Playground")
//                    }
//                }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${portMapping.ExternalPort} ➝ ${portMapping.InternalPort}",
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold,
                    color = AdditionalColors.TextColor
                )
                Text("${portMapping.Protocol}", color = AdditionalColors.TextColor)

            }
        }
    }



//        Column(
//            modifier = Modifier.padding(15.dp)
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically){
//                Text(portMapping.Description, fontSize = TextUnit(20f, TextUnitType.Sp), fontWeight = FontWeight.SemiBold)
//                Text(portMapping.Protocol)
//                Text("Enabled")
//                Text(
//                    text = "On",
//                    modifier = Modifier
//                        .padding(2.dp)
//                        .background(color = Color(0xff90ee90), shape = RoundedCornerShape(10.dp))
//                        .padding(8.dp),
//                    color = Color.Black
//                )
//            }
//
//
////                buildAnnotatedString {
////                    append("welcome to ")
////                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
////                    ) {
////                        append("Jetpack Compose Playground")
////                    }
////                }
//            Row() {
//                Text(portMapping.LocalIP)
//                Text(" • 80 → 80") // ➝ ➜
//            }
//        }
    }
}

fun ToggleSelection(portMapping : PortMapping)
{
    var has = MainActivity.MultiSelectItems!!.contains(portMapping)
    if(has)
    {
        MainActivity.MultiSelectItems!!.remove(portMapping)
    }
    else
    {
        MainActivity.MultiSelectItems!!.add(portMapping)
    }
}

fun IsMultiSelectMode() : Boolean
{
    return MainActivity.MultiSelectItems!!.isNotEmpty()
}

@Composable
fun CircleCheckbox(selected: Boolean, enabled: Boolean = true, modifier : Modifier = Modifier, onChecked: () -> Unit) {

    val color = MaterialTheme.colorScheme
    val imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle
    val tint = if (selected) color.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f) // colorScheme.primary.copy(alpha=.8f)
    val background = if (selected) Color.White else Color.Transparent

    IconButton(onClick = {  },
        enabled = enabled, modifier = modifier.then(Modifier.size(28.dp))) {

        Icon(imageVector = imageVector, tint = tint,
            modifier = Modifier.background(background, shape = CircleShape).size(28.dp),
            contentDescription = "checkbox")
    }
}

@Preview
@Composable
fun CircleCheckboxPreview()
{
    var selected = remember { mutableStateOf(false)}
    CircleCheckbox(selected.value, true) { selected.value = !selected.value }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("Click me") }
    Text(
        text = text,
        modifier = Modifier.clickable {
            if (text.equals("Click me"))
            {
                text = "testing clicked"
            }
            else
            {
                text = "Click me"
            }

            println("clicked...");
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}

@Preview
@Composable
fun MoreInfoDialog()
{
    SetupPreview()
    MoreInfoDialog(_getDefaultPortMapping(), remember { mutableStateOf(true) })
}

@Composable
fun MoreInfoDialog(portMapping : PortMapping,  showDialog : MutableState<Boolean>)
{
    if(showDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Info") },
            text = {
                var pairs = mutableListOf<Pair<String, String>>()
                pairs.add(Pair("Internal IP", portMapping.InternalIP))
                pairs.add(Pair("Internal Port", portMapping.InternalPort.toString()))
                pairs.add(Pair("External IP", portMapping.ExternalIP))
                pairs.add(Pair("External Port", portMapping.ExternalPort.toString()))
                pairs.add(Pair("Protocol", portMapping.Protocol))
                pairs.add(Pair("Enabled", if (portMapping.Enabled) "True" else "False"))
                pairs.add(Pair("Expires", portMapping.getRemainingLeaseTimeString()))
                Column() {
                    for (p in pairs) {
                        Row()
                        {
                            Text(
                                p.first,
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 10.dp, 4.dp)
                                    .width(100.dp),
                                textAlign = TextAlign.Right,
                                color = AdditionalColors.TextColorWeak
                            )
                            Text(p.second, color = AdditionalColors.TextColorStrong)
                        }
                    }
                }

            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { showDialog.value = false }) {
                    Text("OK")
                }
            })
    }
}

class PlaceholderTransformation(val placeholder: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return PlaceholderFilter(text, placeholder)
    }
}

fun PlaceholderFilter(text: AnnotatedString, placeholder: String): TransformedText {

    var out = placeholder

    val numberOffsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return 0
        }

        override fun transformedToOriginal(offset: Int): Int {
            return 0
        }
    }

    return TransformedText(AnnotatedString(placeholder), numberOffsetTranslator)
}
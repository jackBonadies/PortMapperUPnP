@file:OptIn(ExperimentalMaterialApi::class)

package com.shinjiindustrial.portmapper


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.example.myapplication.R
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.model.meta.RemoteDevice
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.*
import kotlin.random.Random


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
    lateinit var DayNightPref : DayNightMode
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


        RestoreSharedPrefs()

        instance = this
        appContext = applicationContext
//        this.registerReceiver(
//            ConnectionReceiver(),
//            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
//        )

        val logger = Logger.getLogger("")
        Logs = StringBuilder()
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
            SharedPrefValues.SortByPortMapping = SortBy.from(preferences[sortOrderKey] ?: 0)
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
        var Logs : StringBuilder = StringBuilder()
        var OurLogger : Logger = Logger.getLogger("PortMapper")
        val ScrollToBottom = "ScrollToBottom"

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

class StringBuilderHandler(private val stringBuilder: StringBuilder) : java.util.logging.Handler() {

//    override fun handleMessage(msg: Message) {
//        super.handleMessage(msg)
//    }
      override fun publish(record: LogRecord?) {
          record?.let {
              val prefix = when (it.level)
              {
                  Level.INFO -> "I: "
                  Level.WARNING -> "W: "
                  Level.SEVERE -> "E: "
                  else -> return // i.e. do not log
              }
              stringBuilder.append(prefix + it.message).append("\n")
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
            mainSearchInProgressAndNothingFoundYet!!.value = true
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
                    for (mapping in device.portMappings) {
                        data.add(UPnPViewElement(mapping))
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
    }

    lateinit var upnpElementsViewModel: UPnPElementViewModel

    //var upnpElementsViewModel: ViewModel by viewModels()
    //var upnpElementsViewModel = UPnPElementViewModel()
    var searchInProgressJob: Job? = null


    var mainSearchInProgressAndNothingFoundYet: MutableState<Boolean>? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this
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
                slideOutHorizontally(
                    targetOffsetX = { -300 }, animationSpec = tween(
                        durationMillis = 300, easing = FastOutSlowInEasing
                    )
                ) //+ fadeOut(animationSpec = tween(300))
            },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -300 }, animationSpec = tween(
                            durationMillis = 300, easing = FastOutSlowInEasing
                        )
                    ) //+ fadeIn(animationSpec = tween(300))

                },
            )
            {
                MainScreen(navController = navController)
            }
            composable("full_screen_dialog", exitTransition = {
//                slideOutVertically(
//                    targetOffsetY = { -300 }, animationSpec = tween(
//                        durationMillis = 300, easing = FastOutSlowInEasing
//                    )
                //) //+
                fadeOut(animationSpec = tween(300))
            },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300))
//                    slideInVertically(
//                        initialOffsetY = { -300 }, animationSpec = tween(
//                            durationMillis = 300, easing = FastOutSlowInEasing
//                        )
//                    ) //+ fadeIn(animationSpec = tween(300))

                },
            )
            {
                RuleCreationDialog()
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

    @OptIn(ExperimentalMaterial3Api::class)
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
                showMoreInfoDialogState
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
//                                modifier = Modifier.height(40.dp),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = AdditionalColors.TopAppBarColor
                        ),
                        //colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                        title = {
                            Text(
                                text = "PortMapper",
                                color = AdditionalColors.TextColorStrong,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        actions = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    bottomSheetState.show()
                                }
                            })
                            {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
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


//                            val refreshScope = rememberCoroutineScope()
//                            var refreshing by remember { refreshState }

                    fun refresh() = refreshScope.launch {
                        refreshing = true
                        //UpnpManager.Search(false)
                        delay(6000)
                        println("finish refreshing $refreshing")
                        refreshing = false
                    }


                    val state = rememberPullRefreshState(refreshing, ::refresh)

                    //

                    BoxWithConstraints(
                        Modifier
                            .pullRefresh(state)
                            .fillMaxHeight()
                            .fillMaxWidth()
                    )
                    {

                        val boxHeight =
                            with(LocalDensity.current) { constraints.maxHeight.toDp() }



                        Column(
                            Modifier
                                .padding(it)
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
                                            UpnpManager.Search(false)
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
    fun RuleCreationDialog() {
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
                    //colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                    title = {
                        Text(
                            text = "Create New Rule",
                            color = AdditionalColors.TextColorStrong,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    actions = {
                        IconButton(onClick = {

                        })
                        {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                    }
                )
            },
            content = { it ->
                Text("Hello World", modifier = Modifier.padding(it))
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
//                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
//                    FilterField.values().forEach {
//                        FilterButton(
//                            text = it.name,
//                            isSelected = it.name == "FOLLOW_SYSTEM",
//                            onClick = { })
//                    }
//                }
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

    fun getComparer(): PortMapperComparatorBase {
        return when(this)
        {
            Slot -> PortMapperComparatorExternalPort() //TODO
            Description -> PortMapperComparatorDescription()
            InternalPort -> PortMapperComparatorInternalPort()
            ExternalPort -> PortMapperComparatorExternalPort()
            Device -> PortMapperComparatorDevice()
            Expiration -> PortMapperComparatorExpiration()
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
        modifier = modifier.then(Modifier
            .height(60.dp)
            .padding(6.dp)),
        colors = CardDefaults.cardColors(containerColor = buttonColor),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                upnpElementsViewModel.insertItem(UPnPViewElement(PortMapping("Web Server $iter", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis())),index)
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
    EnterContextMenu(showDialogMutable, showDialog)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterContextMenu(singleSelectedItem : MutableState<Any?>, showMoreInfoDialog : MutableState<Boolean>)
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
                                Toast.makeText(
                                    PortForwardApplication.appContext,
                                    "Edit clicked",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                                Toast.makeText(
                                    PortForwardApplication.appContext,
                                    "Disable clicked",
                                    Toast.LENGTH_SHORT
                                ).show()
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
    val internalPortText = remember { mutableStateOf("") }
    val externalPortText = remember { mutableStateOf("") }
    val leaseDuration = remember { mutableStateOf("0") }
    val description = remember { mutableStateOf("") }

    var (ourIp, ourGatewayIp) = if (isPreview) Pair<String, String>("192.168.0.1","") else OurNetworkInfo.GetLocalAndGatewayIpAddrWifi(
        PortForwardApplication.appContext,
        false
    )

    val internalIp = remember { mutableStateOf(ourIp!!) }



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
            onDismissRequest = { showDialogMutable.value = false },
            properties = prop,
        ) {
            Surface(
                shape = RoundedCornerShape(size = 6.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
//                    .background(
//                        color = Color.DarkGray, shape = RoundedCornerShape(16.dp)
//                    )
                        .fillMaxWidth(0.9f)
                ) {
                    Text("Create New Rule", style = MaterialTheme.typography.headlineLarge, modifier = Modifier
                        .padding(6.dp, 6.dp)
                        .align(Alignment.Start))
                    Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(.9f)
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    )
                    {
                        OutlinedTextField(
                            value = description.value,
                            onValueChange = { description.value = it },
                            label = { Text("Description") },
                            modifier = Modifier
                                .weight(0.4f, true)
                                .height(60.dp)
                        )
                    }

                    var textfieldSize by remember { mutableStateOf(Size.Zero) }

                    DeviceRow()
                    {
                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = internalIp.value,
                            onValueChange = { internalIp.value = it },
                            label = { Text("Internal Device") },
                            modifier = Modifier
                                .weight(0.4f, true)
                                .height(60.dp)
                                .onGloballyPositioned { coordinates ->
                                    //This value is used to assign to the DropDown the same width
                                    println("OurInternalDevice: " + coordinates.size.toSize())
                                    // Get density.


                                    fun pxToDp(context: Context, px: Float): Float {
                                        return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
                                    }

                                    val density = pxToDp(
                                        PortForwardApplication.appContext,
                                        (coordinates.size.height.toFloat())
                                    )
                                    textfieldSize = coordinates.size.toSize()
                                }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = internalPortText.value,
                            onValueChange = { internalPortText.value = it },
                            label = { Text("Port") },
                            modifier = Modifier
                                .weight(0.2f, true)
                                .height(60.dp)
                        )

                    }
//                    val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
//                    var expanded by remember { mutableStateOf(false) }
//                    var selectedOptionText by remember { mutableStateOf(options[0]) }
                    var gatewayIps: MutableList<String> = mutableListOf()
                    var defaultGatewayIp = ""

                    if(isPreview) {


                    }
                    else
                    {

                        synchronized(UpnpManager.lockIgdDevices)
                        {
                            for (device in UpnpManager.IGDDevices) {
                                gatewayIps.add(device.ipAddress)
                                if (device.ipAddress == ourGatewayIp) {
                                    defaultGatewayIp = device.ipAddress
                                }
                            }
                        }

                        if (defaultGatewayIp == "" && !gatewayIps.isEmpty()) {
                            defaultGatewayIp = gatewayIps[0]
                        }
                    }

                    val suggestions = gatewayIps
                    var externalDeviceText = remember { mutableStateOf(defaultGatewayIp) }
                    var selectedText by externalDeviceText



                    DeviceRow()
                    {
                        var defaultModifier = Modifier
                            .width(with(LocalDensity.current) { textfieldSize.width.toDp() })
                            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
                        DropDownOutline(defaultModifier, externalDeviceText, suggestions, "External Device")

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = externalPortText.value,
                            onValueChange = { externalPortText.value = it },
                            label = { Text("Port") },
                            modifier = Modifier
                                .weight(0.2f, true)
                                .height(60.dp)
                        )


                    }


//                    ExposedDropdownMenuBox(
//                        expanded = expanded,
//                        onExpandedChange = {
//                            expanded = !expanded
//                        }
//                    ) {
//                        TextField(
//                            readOnly = true,
//                            value = selectedOptionText,
//                            onValueChange = { },
//                            label = { Text("Label") },
//                            trailingIcon = {
//                                ExposedDropdownMenuDefaults.TrailingIcon(
//                                    expanded = expanded
//                                )
//                            },
//                            colors = ExposedDropdownMenuDefaults.textFieldColors()
//                        )
//                        ExposedDropdownMenu(
//                            expanded = expanded,
//                            onDismissRequest = {
//                                expanded = false
//                            }
//                        ) {
//                            options.forEach { selectionOption ->
//                                DropdownMenuItem(
//                                    text = {
//                                        Text(selectionOption)
//                                    },
//                                    onClick = {
//                                        selectedOptionText = selectionOption
//                                        expanded = false
//                                    }
//                                )
//                            }
//                        }
//                    }

                    var selectedProtocolMutable = remember { mutableStateOf(Protocol.TCP.str()) }
                    var selectedPort by selectedProtocolMutable

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(.9f)
                            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
                    ) {
                        var defaultModifier = Modifier
                            .fillMaxWidth(.5f)
                            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
                        DropDownOutline(defaultModifier,//Size(500f, textfieldSize.height),
                            selectedText = selectedProtocolMutable,
                            suggestions = listOf(Protocol.TCP.str(), Protocol.UDP.str(), Protocol.BOTH.str()),
                            "Protocol")

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = leaseDuration.value,
                            onValueChange = { leaseDuration.value = it },
                            label = { Text("Lease") },
                            modifier = Modifier
                                .weight(0.4f, true)
                                //.fillMaxWidth(.4f)
                                .height(60.dp)
                        )

                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth(.9f)
                            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                showDialogMutable.value = false
                            },
                            shape = RoundedCornerShape(4),
                            modifier = Modifier.weight(.6f)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.padding(18.dp))
                        val interactionSource = remember { MutableInteractionSource() }

                        val text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                append("CREATE")
                            }
                        }

//                        Text(text = "Create",
//                            modifier = Modifier.weight(1.0f).clickable(interactionSource = interactionSource,
//                                indication = null,
//                                onClick = {
//                                    println("TextField clicked")
//                                })
//                        )

                        Button(
                            onClick = {

                                //Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()

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

                                        var numFailed = result.count {!it?.Success!!}

                                        var anyFailed = numFailed > 0

                                        if(anyFailed) {

                                            // all failed
                                            if(numFailed == result.size)
                                            {
                                                if(result.size == 1)
                                                {
                                                    MainActivity.showSnackBarViewLog("Failed to create rule.")
                                                }
                                                else
                                                {
                                                    MainActivity.showSnackBarViewLog("Failed to create rules.")
                                                }
                                            }
                                            else
                                            {
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
                                        }
                                        else
                                        {
                                            MainActivity.showSnackBarShortNoAction("Success!")
                                        }
                                    }
                                }

                                var portMappingRequestInput = PortMappingUserInput(
                                    description.value,
                                    internalIp.value,
                                    internalPortText.value,
                                    externalDeviceText.value,
                                    externalPortText.value,
                                    selectedProtocolMutable.value,
                                    leaseDuration.value,
                                    true
                                )
                                var future = UpnpManager.CreatePortMappingRules(portMappingRequestInput, ::batchCallback)
                                showDialogMutable.value = false
                            },
                            shape = RoundedCornerShape(4),
                            modifier = Modifier.weight(1.0f),

                            )
                        {
                            Text("Create")
                        }
                    }

                }
            }
        }
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
    Box(
        modifier = Modifier.clickable(interactionSource = interactionSource,
            indication = null,
            onClick = {
                println("TextField clicked")
                expanded = !expanded
                focusRequester.requestFocus()
            })
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
            modifier = defaultModifier
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
            modifier = defaultModifier
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
fun DeviceRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(.9f)
            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
        horizontalArrangement = Arrangement.End
    ) {
        content()
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
        items.forEach { label ->
            DropdownMenuItem(text = { Text(label) }, onClick = {
                // handle item click
                expanded = false

                when (label) {
                    "Refresh" ->
                    {
                        UpnpManager.Initialize(PortForwardApplication.appContext,true)
                        UpnpManager.Search(false)
                    }
                    "Disable All" ->
                    {
                        enableDisableAll(false)
                    }
                    "Enable All" ->
                    {
                        enableDisableAll(true)
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

fun deleteAll()
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
                var res = result[0]
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

    // get all enabled
    var rules = UpnpManager.GetAllRules();
    UpnpManager.DeletePortMappingsEntry(rules, ::batchCallback)
}

fun enableDisableAll(enable : Boolean)
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
                var res = result[0]
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

    var rules = UpnpManager.GetEnabledDisabledRules(!enable);
    UpnpManager.DisableEnablePortMappingEntries(rules, enable, ::batchCallback)
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
    return PortMapping("Web Server", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis())
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
    MessageCard("Android", "I want to chat", true)
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PortMappingCard()
{
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
                System.currentTimeMillis()
            )
        )
}

@Preview(showBackground = true)
@Composable
fun PortMappingCardAlt()
{
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
            System.currentTimeMillis()
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

@OptIn(ExperimentalUnitApi::class, ExperimentalMaterial3Api::class)
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
            .clickable {


//                Snackbar
//                    .make(parentlayout, "This is main activity", Snackbar.LENGTH_LONG)
//                    .setAction("CLOSE", object : OnClickListener() {
//                        fun onClick(view: View?) {}
//                    })
//                    .setActionTextColor(getResources().getColor(R.color.holo_red_light))
//                    .show()
                //isRound = !isRound
                PortForwardApplication.showContextMenu.value = true
                PortForwardApplication.currentSingleSelectedObject.value = portMapping
            },
            elevation = CardDefaults.cardElevation(),
            border = BorderStroke(1.dp, AdditionalColors.SubtleBorder),
            colors = CardDefaults.cardColors(
                containerColor = AdditionalColors.CardContainerColor,
            ),
    ) {

        Row(
            modifier = Modifier
                .padding(15.dp, 6.dp),//.background(Color(0xffc5dceb)),
            //.background(MaterialTheme.colorScheme.secondaryContainer),
            verticalAlignment = Alignment.CenterVertically

        ) {
            Column(modifier = Modifier.weight(1f)) {
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
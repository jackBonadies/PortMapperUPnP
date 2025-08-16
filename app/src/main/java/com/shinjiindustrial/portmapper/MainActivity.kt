@file:OptIn(ExperimentalMaterialApi::class)

package com.shinjiindustrial.portmapper


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.R
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.shinjiindustrial.portmapper.common.MAX_PORT
import com.shinjiindustrial.portmapper.ui.SetupPreview
import com.shinjiindustrial.portmapper.common.ValidationError
import com.shinjiindustrial.portmapper.common.capLeaseDur
import com.shinjiindustrial.portmapper.common.toMessage
import com.shinjiindustrial.portmapper.common.validateDescription
import com.shinjiindustrial.portmapper.common.validateEndPort
import com.shinjiindustrial.portmapper.common.validateInternalIp
import com.shinjiindustrial.portmapper.common.validateStartPort
import com.shinjiindustrial.portmapper.ui.BottomSheetSortBy
import com.shinjiindustrial.portmapper.ui.ConversationEntryPoint
import com.shinjiindustrial.portmapper.ui.DeviceHeader
import com.shinjiindustrial.portmapper.ui.DurationPickerDialog
import com.shinjiindustrial.portmapper.ui.LoadingIcon
import com.shinjiindustrial.portmapper.ui.MoreInfoDialog
import com.shinjiindustrial.portmapper.ui.NoMappingsCard
import com.shinjiindustrial.portmapper.ui.PortMappingCard
import com.shinjiindustrial.portmapper.ui.RuleCreationDialog
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fourthline.cling.model.meta.RemoteDevice
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.random.Random

var PseudoSlotCounter : Int = MAX_PORT // as starting slot
fun GetPsuedoSlot() : Int
{
    PseudoSlotCounter += 1
    return PseudoSlotCounter
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

            val data: MutableList<UPnPViewElement> = mutableListOf()
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
            intent.putExtra(PortForwardApplication.ScrollToBottom, true)
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
            if (OurSnackbarHostState == null)
            {
                PortForwardApplication.ShowToast(
                    message,
                    if ((duration == SnackbarDuration.Long || duration == SnackbarDuration.Indefinite)) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                )
            }
            else
            {
                GlobalScope.launch(Dispatchers.Main) {

                    val snackbarResult = OurSnackbarHostState!!.showSnackbar(
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
        super.onCreate(savedInstanceState)

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

        UpnpManager.Initialize(this, false)
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
            UpnpManager.Search(true) // by default STAll
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
        super.onDestroy()
        UpnpManager.DeviceFoundEvent -= ::deviceFoundHandler
        UpnpManager.PortAddedEvent -= ::portMappingAddedHandler
        UpnpManager.FinishedListingPortsEvent -= ::deviceFinishedListingPortsHandler
        UpnpManager.UpdateUIFromData -= ::updateUIFromData
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun MainScreen(navController : NavHostController) {
        // A surface container using the 'background' color from the theme
        rememberScrollState()
        val showAboutDialogState = remember { mutableStateOf(false) }
        val showMoreInfoDialogState = remember { mutableStateOf(false) }
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

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialogState.value = false },
                title = { Text("About") },
                text = {

                    Column()
                    {
                        var aboutString: String =
                            PortForwardApplication.appContext.getString(R.string.about_body)
                        val packageInfo =
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
                                setTextColor(AdditionalColors.TextColor.toArgb())
                                textSize = 16f
                                text = spannedString
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
        rememberCoroutineScope()
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
                                showAboutDialogState
                            )
                        }
                    )
                },
                content = { it ->

                    val refreshScope = rememberCoroutineScope()

                    val refreshState = remember { mutableStateOf(false) }
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

                                    val interfacesUsedInSearch =
                                        (UpnpManager.GetUPnPService().configuration as AndroidConfig).NetworkInterfacesUsedInfos
                                    val anyInterfaces =
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
                                // TODO: ??????
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

// TODO: expires in 5 minutes, autorenews in 5 minutes status symbols for each portmapping
// TODO: mock profile
fun launchMockUPnPSearch(activity : MainActivity, upnpElementsViewModel : UPnPElementViewModel)
{
    GlobalScope.launch {
        var iter = 0
        while(true)
        {
            iter++

            delay(1000L)
            activity.runOnUiThread {
                val index = Random.nextInt(0,upnpElementsViewModel.items.value!!.size+1)
                upnpElementsViewModel.insertItem(UPnPViewElement(PortMapping("Web Server $iter", "","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis(), GetPsuedoSlot())),index)
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

fun fallbackRecursiveSearch(rootDevice : RemoteDevice)
{
    // recursively look through devices
    val deviceList = mutableListOf<RemoteDevice>() //.toMutableList()
    deviceList.add(rootDevice)
    while(deviceList.isNotEmpty())
    {
        val deviceInQuestion = deviceList.removeAt(0)
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
fun EnterContextMenu(singleSelectedItem : MutableState<Any?>, showMoreInfoDialog : MutableState<Boolean>, navController : NavHostController)
{
    if(singleSelectedItem.value == null)
    {
        return
    }

    val prop = DialogProperties(
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


                        val menuItems: MutableList<Pair<String, () -> Unit>> = mutableListOf()
                        val portMapping = singleSelectedItem.value as PortMapping
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Edit"
                            ) {

                                ///{description}/{internalIp}/{internalRange}/{externalIp}/{externalRange}/{protocol}/{leaseDuration}/{enabled}
                                val uriBuilder = Uri.Builder()
                                    .path("full_screen_dialog")
                                    .appendQueryParameter("description", portMapping.Description)
                                    .appendQueryParameter("internalIp", portMapping.InternalIP)
                                    .appendQueryParameter("internalRange", portMapping.InternalPort.toString())
                                    .appendQueryParameter("externalIp", portMapping.ActualExternalIP) // this is actual external IP as we only use it to delete the old rule...
                                    .appendQueryParameter("externalRange", portMapping.ExternalPort.toString())
                                    .appendQueryParameter("protocol", portMapping.Protocol)
                                    .appendQueryParameter("leaseDuration", portMapping.LeaseDuration.toString())
                                    .appendQueryParameter("enabled", portMapping.Enabled.toString())
                                val uri = uriBuilder.build()
                                navController.navigate(uri.toString())
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

                                UpnpManager.DisableEnablePortMappingEntry(
                                    portMapping,
                                    !portMapping.Enabled,
                                    ::enableDisableCallback
                                )
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Delete"
                            ) {
                                UpnpManager.DeletePortMappingEntry(portMapping)
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>("More Info"
                            ) {
                                showMoreInfoDialog.value = true
                            }
                        )
                        var index = 0
                        val lastIndex = menuItems.size - 1
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

//In IGD release 1.0, a value of 0 was used to create a static port mapping. In version 2.0, it is no longer
//possible to create static port mappings via UPnP actions. Instead, an out-of-band mechanism is REQUIRED
//to do so (cf. WWW-administration, remote management or local management). In order to be backward
//compatible with legacy control points, the value of 0 MUST be interpreted as the maximum value (e.g.
//604800 seconds, which corresponds to one week).

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

    val showLeaseDialog = remember { mutableStateOf(false) }

    if(showLeaseDialog.value)
    {
        DurationPickerDialog(showLeaseDialog, leaseDuration, wanIpIsV1.value)
    }

    val descriptionErrorString = remember { mutableStateOf(validateDescription(description.value).validationError) }
    val interalIpErrorString = remember { mutableStateOf(validateInternalIp(internalIp.value).validationError) }

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
                description.value = it
                descriptionHasError.value = validateDescription(description.value).hasError
                descriptionErrorString.value = validateDescription(description.value).validationError
            },
            label = { Text("Description") },
            singleLine = true,
            modifier = Modifier
                .weight(0.4f, true)
            ,//.height(60.dp),
            isError = hasSubmitted.value && descriptionHasError.value,
            trailingIcon = {
                if (hasSubmitted.value && descriptionHasError.value) {
                    Icon(Icons.Filled.Error, descriptionErrorString.value.toMessage(), tint = MaterialTheme.colorScheme.error)
                }
            },
            supportingText = {
                if(hasSubmitted.value && descriptionHasError.value) {
                    Text(
                        descriptionErrorString.value.toMessage(),
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

    val portStartSize = remember { mutableStateOf(IntSize.Zero) }

    DeviceRow()
    {
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = internalIp.value,
            onValueChange = {
                internalIp.value = it.filter { charIt -> charIt.isDigit() || charIt == '.' }
                internalIpHasError.value = validateInternalIp(internalIp.value).hasError
                interalIpErrorString.value = validateInternalIp(internalIp.value).validationError
            },
            isError = hasSubmitted.value && internalIpHasError.value,
            supportingText = {
                if(hasSubmitted.value && internalIpHasError.value) {
                    Text(
                        interalIpErrorString.value.toMessage(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (hasSubmitted.value && internalIpHasError.value) {
                    Icon(Icons.Filled.Error, interalIpErrorString.value.toMessage(), tint = MaterialTheme.colorScheme.error)
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
        val startHasErrorString = remember { mutableStateOf(validateStartPort(internalPortText.value).validationError) }
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


    DeviceRow()
    {
//        var defaultModifier = Modifier
//            .weight(0.5f, true)
//            //.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        val defaultModifier = Modifier
            .weight(0.5f, true)
//            //.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
//            .height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        DropDownOutline(defaultModifier, externalDeviceText, gatewayIps, "External Device")
        Spacer(modifier = Modifier.width(8.dp))
        val startHasErrorString = remember { mutableStateOf(validateStartPort(externalPortText.value).validationError) }
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

    Row(
        modifier = Modifier
            .fillMaxWidth(createNewRuleRowWidth)
            .padding(top = PortForwardApplication.PaddingBetweenCreateNewRuleRows),
    ) {
        val defaultModifier = Modifier
            .weight(0.5f, true)
        //.height(60.dp)
        //.height(with(LocalDensity.current) { textfieldSize.height.toDp() })
        DropDownOutline(defaultModifier,//Size(500f, textfieldSize.height),
            selectedText = selectedProtocolMutable,
            suggestions = listOf(Protocol.TCP.str(), Protocol.UDP.str(), Protocol.BOTH.str()),
            "Protocol")

        Spacer(modifier = Modifier.width(8.dp))

        remember { mutableStateOf(false) }
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = leaseDuration.value,
            onValueChange = {

                val digitsOnly = it.filter { charIt -> charIt.isDigit() }
                if (digitsOnly.isBlank()) {
                    leaseDuration.value = digitsOnly

                } else{
                    leaseDuration.value = capLeaseDur(digitsOnly, wanIpIsV1.value)
                }

                            },
            label = { Text("Lease") },
            trailingIcon = {
                IconButton(onClick = { showLeaseDialog.value = true  })
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
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPortExpandable(portText : MutableState<String>, hasError : MutableState<Boolean>, errorString : MutableState<ValidationError>, hasSubmitted: MutableState<Boolean>, expanded : MutableState<Boolean>, startPortSize : MutableState<IntSize>)
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
            hasError.value = validateStartPort(portText.value).hasError
            errorString.value = validateStartPort(portText.value).validationError
        },
        label = { if(expanded.value) Text("Port Start") else Text("Port") },
        //modifier = Modifier.then(modifier),
        //.height(60.dp),
        isError = hasSubmitted.value && hasError.value,
        supportingText = {
            if(hasSubmitted.value && hasError.value)
            {
                Text(errorString.value.toMessage(), color = MaterialTheme.colorScheme.error)
            }
        },
        trailingIcon = {
            if(hasSubmitted.value && hasError.value)
            {
                Icon(Icons.Filled.Error, contentDescription = errorString.value.toMessage(), tint = MaterialTheme.colorScheme.error) //TODO set error on main theme if not already.
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
        val expanded = remember { mutableStateOf(false) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortRangeRow(startPortText : MutableState<String>, endPortText : MutableState<String>, startHasError : MutableState<Boolean>, endHasError : MutableState<Boolean>, hasSubmitted : MutableState<Boolean>, modifier : Modifier, portSize : IntSize)
{
    DeviceRow()
    {

        val endHasErrorString = remember { mutableStateOf(validateEndPort(startPortText.value, endPortText.value).validationError) }

        Spacer(modifier = Modifier.weight(0.5f, true))

        Spacer(modifier = Modifier.width(8.dp))

        Text("to", modifier = Modifier.align(Alignment.CenterVertically))

        Spacer(modifier = Modifier.width(8.dp))

        

        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = endPortText.value,
            onValueChange = {
                endPortText.value = it.filter { charIt -> charIt.isDigit() }
                endHasError.value = validateEndPort(startPortText.value, endPortText.value).hasError
                endHasErrorString.value = validateEndPort(startPortText.value, endPortText.value).validationError
                            },
            label = { Text("Port End") },
            modifier = Modifier.width(with(LocalDensity.current) { portSize.width.toDp() }),
            isError = hasSubmitted.value && endHasError.value,
            supportingText = {
                if(hasSubmitted.value && endHasError.value)
                {
                    Text(endHasErrorString.value.toMessage(), color = MaterialTheme.colorScheme.error)
                }
            },


            trailingIcon =
                if (hasSubmitted.value && endHasError.value) {
                    @Composable {
                        if(hasSubmitted.value && endHasError.value)
                        {
                            Icon(Icons.Filled.Error, contentDescription = endHasErrorString.value.toMessage(), tint = MaterialTheme.colorScheme.error) //TODO set error on main theme if not already.
                        }
                    }
                } else {
                    null
                }
        )

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
fun OverflowMenu(showAboutDialogState : MutableState<Boolean>) {
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
                val (anyEnabled, anyDisabled) = UpnpManager.GetExistingRuleInfos()
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
            val anyEnabled = MainActivity.MultiSelectItems!!.any { it -> it.Enabled }
            val anyDisabled = MainActivity.MultiSelectItems!!.any { it -> !it.Enabled }
            if(anyEnabled)
            {
                items.add("Disable")
            }
            if(anyDisabled)
            {
                items.add("Enable")
            }
        }

        MainActivity.MultiSelectItems

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

            val anyFailed = result.any {!it?.Success!!}

            if(anyFailed) {
                val res = result.first {!it?.Success!!}
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
    val rules = chosenOnly?.toList() ?: UpnpManager.GetAllRules()
    UpnpManager.DeletePortMappingsEntry(rules, ::batchCallback)
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

            val anyFailed = result.any {!it?.Success!!}

            if(anyFailed) {
                val res = result.first {!it?.Success!!}
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
        val rules = chosenRulesOnly.filter { it -> it.Enabled != enable }
        UpnpManager.DisableEnablePortMappingEntries(rules, enable, ::batchCallback)
    }
    else {
        val rules = UpnpManager.GetEnabledDisabledRules(!enable)
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

data class Message(val name : String, val msg : String)

fun _getDefaultPortMapping() : PortMapping
{
    return PortMapping("Web Server", "","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1", System.currentTimeMillis(), 0)
}

// TODO: for mock purposes
class IGDDeviceHolder
{

    var displayName: String = "Nokia"
        get(){
            return ""
        }

}

fun ToggleSelection(portMapping : PortMapping)
{
    val has = MainActivity.MultiSelectItems!!.contains(portMapping)
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
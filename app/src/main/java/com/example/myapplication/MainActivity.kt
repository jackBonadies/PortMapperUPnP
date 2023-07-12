@file:OptIn(ExperimentalMaterialApi::class)

package com.example.myapplication


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.ui.theme.AdditionalColors
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fourthline.cling.model.meta.RemoteDevice
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random


//object UpnpManager {
//    va UpnpService? : UpnpService = null
//}

class PortForwardApplication : Application() {

    override fun onCreate() {


        super.onCreate()
        PortForwardApplication.appContext = applicationContext
        this.registerReceiver(
            ConnectionReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    companion object {
        lateinit var appContext: Context
        lateinit var CurrentActivity: ComponentActivity
//        lateinit var showPopup : MutableState<Boolean>
        lateinit var currentSingleSelectedObject : MutableState<Any?>
        var PaddingBetweenCreateNewRuleRows = 4.dp

        fun ShowToast( msg : String,  toastLength : Int)
        {
            if(PortForwardApplication.CurrentActivity == null)
            {

            }
            else
            {
                PortForwardApplication.CurrentActivity.runOnUiThread { Toast.makeText(PortForwardApplication.CurrentActivity, msg, toastLength).show(); }
            }
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
            upnpElementsViewModel.addItem(UPnPViewElement(remoteDevice)) // calls LiveData.setValue i.e. must be done on UI thread
        }
    }

    fun portMappingFoundHandler(portMapping: PortMapping) {
        runOnUiThread {
            upnpElementsViewModel.addItem(UPnPViewElement(portMapping)) // calls LiveData.setValue i.e. must be done on UI thread
        }
    }

    fun deviceFinishedListingPortsHandler(remoteDevice : IGDDevice)
    {
        if(remoteDevice.portMappings.isEmpty())
        {
            runOnUiThread {
                upnpElementsViewModel.addItem(UPnPViewElement(remoteDevice, true)) // calls LiveData.setValue i.e. must be done on UI thread
            }
        }
    }

    fun updateUIFromData(o : Any?)
    {
        runOnUiThread {
            var data : MutableList<UPnPViewElement> = mutableListOf()
            for(device in UpnpManager.Companion.IGDDevices)
            {
                data.add(UPnPViewElement(device))

                for(mapping in device.portMappings)
                {
                    data.add(UPnPViewElement(mapping))
                }
            }
            upnpElementsViewModel.setData(data) // calls LiveData.setValue i.e. must be done on UI thread
        }
    }

    var upnpElementsViewModel = UPnPElementViewModel()
    var searchInProgressJob : Job? = null







    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this

        //getConnectionType(this)
        //getLocalIpAddress()

        //android router set wifi enabled

//        val future = CompletableFuture<String>()
//        future.complete("Hello")
//        var result = future.get()

        //AndroidRouter().enableWiFi()
        UpnpManager.Initialize()
        UpnpManager.DeviceFoundEvent += ::deviceFoundHandler
        UpnpManager.PortFoundEvent += ::portMappingFoundHandler
        UpnpManager.FinishedListingPortsEvent += ::deviceFinishedListingPortsHandler
        UpnpManager.UpdateUIFromData += ::updateUIFromData

        var mainSearchInProgress = mutableStateOf(!UpnpManager.HasSearched)
        if(mainSearchInProgress.value)
        {
            searchInProgressJob = GlobalScope.launch {
                delay(6000)
                mainSearchInProgress.value = false
            }
        }

        var searchStarted = UpnpManager.Search(true) // by default STAll
        //var refreshState = mutableStateOf(false)


        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                val scrollState = rememberScrollState()
                var showDialogMutable = remember { mutableStateOf(false) }
                var showDialog by showDialogMutable //mutable state binds to UI (in sense if value changes, redraw). remember says when redrawing dont discard us.

//                var showMainLoading = remember { mutableStateOf(searchStarted) }
//


                PortForwardApplication.currentSingleSelectedObject = remember { mutableStateOf(null) }

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

                if(PortForwardApplication.currentSingleSelectedObject.value != null)
                {
                    EnterContextMenu(PortForwardApplication.currentSingleSelectedObject )
                }


                if (showDialog) {

                    EnterPortDialog(showDialogMutable)

//                    Dialog(onDismissRequest = {
//                        println("dismiss request")
//                        showDialog = false })
//                    {
//                            // Dialog content here
//                            Box(modifier = Modifier.background(Color.White)) {
//                            Text("Hello, Dialog!")
//                        }
                }


                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val coroutineScope: CoroutineScope = rememberCoroutineScope()
                val anyIgdDevices = remember { mutableStateOf(!UpnpManager.IGDDevices.isEmpty()) }
                UpnpManager.AnyIgdDevices = anyIgdDevices

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {



                        if(anyIgdDevices.value)
                        {
                            FloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                onClick = {

                                    //showDialog = true

                                    coroutineScope.launch {
                                        println("show snackbar")
                                        var snackbarResult = snackbarHostState.showSnackbar(
                                            "testing",
                                            "action",
                                            true,
                                            SnackbarDuration.Indefinite
                                        )
                                        println("shown")
                                        when (snackbarResult) {
                                            SnackbarResult.Dismissed -> TODO()
                                            SnackbarResult.ActionPerformed -> TODO()
                                        }
                                    }

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
                            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                            title = { Text(text = "Port Pilot", color = AdditionalColors.TextColorStrong, fontWeight = FontWeight.Normal) },
                            actions = { OverflowMenu(showDialogMutable) }
                        )
                    },
                    content = {it  ->

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
                                .fillMaxWidth())
                        {

                            val boxHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }



                            Column(
                                Modifier
                                    .padding(it)
                                    .fillMaxHeight()
                                    .fillMaxWidth()) {
//                                    for (i in 1..10)
//                                    {
//                                        Text("test")
//                                    }
                                if(mainSearchInProgress.value)
                                {
                                    val offset = boxHeight * 0.28f
                                    LoadingIcon("Searching for devices", Modifier.offset(y = offset))
                                }
                                else if(!anyIgdDevices.value)
                                {
                                    val offset = boxHeight * 0.28f
                                    Column(modifier = Modifier.offset(y = offset))
                                    {
                                        Text("No UPnP enabled internet gateway devices found.",
                                            modifier = Modifier.padding(0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                        Text("Network Info: Data",
                                            modifier = Modifier.padding(0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                        Text("Switch to WiFi and search again",
                                            modifier = Modifier.padding(0.dp, 10.dp)
                                                .align(Alignment.CenterHorizontally),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else
                                {
                                    ConversationEntryPoint(upnpElementsViewModel)
                                }


//                                MyScreen(viewModel)
//                                Greeting("Android")
//                                Text("hello")
//                                MessageCard("hello", "message content", true)
                            }

                            PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
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

    override fun onDestroy()
    {
        super.onDestroy();
        UpnpManager.DeviceFoundEvent -= ::deviceFoundHandler
        UpnpManager.PortFoundEvent -= ::portMappingFoundHandler
        UpnpManager.FinishedListingPortsEvent -= ::deviceFinishedListingPortsHandler
        UpnpManager.UpdateUIFromData -= ::updateUIFromData
    }
    
}

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

            OverflowMenu(showDialogMutable) 
        }
    )
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
                upnpElementsViewModel.insertItem(UPnPViewElement(PortMapping("Web Server $iter", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1")),index)
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
    EnterContextMenu(showDialogMutable)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterContextMenu(singleSelectedItem : MutableState<Any?>)
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
            onDismissRequest = { singleSelectedItem.value = null },
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
                                "Edit",
                                {
                                    Toast.makeText(
                                        PortForwardApplication.appContext,
                                        "Edit clicked",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                if (portMapping.Enabled) "Disable" else "Enable",
                                {

                                    fun enableDisableCallback(result : UPnPCreateMappingResult) {
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

                                    var future = UpnpManager.DisableEnablePortMappingEntry(portMapping, !portMapping.Enabled, ::enableDisableCallback)
                                    Toast.makeText(
                                        PortForwardApplication.appContext,
                                        "Disable clicked",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Delete",
                                {
                                    var future = UpnpManager.DeletePortMappingEntry(portMapping)

//
//                                    Toast.makeText(
//                                        PortForwardApplication.appContext,
//                                        "Delete clicked",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
                                })
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>("More Info",
                                {
                                    Toast.makeText(
                                        PortForwardApplication.appContext,
                                        "More Info clicked ${portMapping.Description}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                        )
                        var index = 0
                        var lastIndex = menuItems.size - 1
                        for (menuItem in menuItems) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        menuItem.second()
                                        singleSelectedItem.value =
                                            null //this redraws the inner composable before the outer...
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
                                Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()

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
fun OverflowMenu(showDialog : MutableState<Boolean>) {
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
        items.add("About")
        items.forEach { label ->
            DropdownMenuItem(text = { Text(label) }, onClick = {
                // handle item click
                expanded = false

                when (label) {
                    "Refresh" -> {println("Item 1 pressed")}
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

                    }
                    "View Log" -> {println("Item 1 pressed")}
                    "About" -> {println("Item 1 pressed")}
                }
            })
        }
    }
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

    // get all enabled
    var rules = UpnpManager.GetEnabledDisabledRules(enable);
    UpnpManager.DisableEnablePortMappingEntries(rules, !enable, ::batchCallback)
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

@Preview
@Composable
fun PreviewConversation() {
    MyApplicationTheme() {

    //ComposeTutorialTheme {
    val msgs = mutableListOf<UPnPViewElement>()
    var pm = PortMapping("Web Server", "192.168.18.1","192.168.18.13",80,80, "UDP", true, 0, "192.168.18.1")
    var upnpViewEl = UPnPViewElement(pm)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
    msgs.add(upnpViewEl)
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

            itemsIndexed(messages) { index, message ->

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
                    PortMappingCard(message.GetUnderlyingPortMapping())
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
                "192.168.18.1"
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
            "192.168.18.1"
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
                Text("${portMapping.LocalIP}")
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
                .padding(4.dp, 4.dp)
//            .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    //PortForwardApplication.currentSingleSelectedObject.value = portMapping
                },
            elevation = CardDefaults.cardElevation(),
            colors = CardDefaults.cardColors(
                containerColor = AdditionalColors.CardSurfaceNoMappings,
            ),

            ) {

            Row(
                modifier = Modifier
                    .padding(15.dp, 20.dp),//.background(Color(0xffc5dceb)),
                //.background(MaterialTheme.colorScheme.secondaryContainer),
                verticalAlignment = Alignment.CenterVertically

            ) {
                Column(modifier = Modifier.weight(1f)) {




                    Text(
                        "No port mappings found \nfor this device",
                        fontSize = TextUnit(20f, TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(0.dp, 0.dp, 0.dp, 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap ",

                            //color = MaterialTheme.colors.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add icon",
                            //tint = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = " to add new rules",
                            //style = MaterialTheme.typography.body1,
                            //color = MaterialTheme.colors.onSurface
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
fun PortMappingCard(portMapping: PortMapping)
{
    println("external ip test ${portMapping.ExternalIP}")

    MyApplicationTheme(){
    Card(
//        onClick = {
//            if(PortForwardApplication.showPopup != null)
//            {
//                PortForwardApplication.showPopup.value = true
//            }
//                  },
        modifier = Modifier
            .fillMaxWidth()
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
                Text("${portMapping.LocalIP}", color = AdditionalColors.TextColor)

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
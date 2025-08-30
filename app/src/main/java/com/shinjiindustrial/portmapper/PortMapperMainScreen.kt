package java.com.shinjiindustrial.portmapper

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.EnterContextMenu
import com.shinjiindustrial.portmapper.MainActivity.Companion.OurSnackbarHostState
import com.shinjiindustrial.portmapper.MainActivity.Companion.showSnackBarViewLog
import com.shinjiindustrial.portmapper.OverflowMenu
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.common.NetworkType
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.ui.BottomSheetSortBy
import com.shinjiindustrial.portmapper.ui.LoadingIcon
import com.shinjiindustrial.portmapper.ui.MoreInfoDialog
import com.shinjiindustrial.portmapper.ui.PortMappingContent
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortMapperMainScreen(portViewModel : PortViewModel, themeState: ThemeUiState, navController: NavHostController) {
    // A surface container using the 'background' color from the theme
    val searchStartedRecentlyAndNothingFoundYet by portViewModel.searchStartedRecentlyAndNothingFoundYet.collectAsStateWithLifecycle()
    val anyDevices by portViewModel.anyDevices.collectAsStateWithLifecycle()
    rememberScrollState()
    val showAboutDialogState = remember { mutableStateOf(false) }
    val showMoreInfoDialogState = remember { mutableStateOf(false) }
    val showAboutDialog by showAboutDialogState //mutable state binds to UI (in sense if value changes, redraw). remember says when redrawing dont discard us.
    val inMultiSelectMode by portViewModel.inMultiSelectMode.collectAsStateWithLifecycle()
    val selectedIds by portViewModel.selectedIds.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        portViewModel.events.collect { ev ->
            if (ev is PortViewModel.UiEvent.ToastEvent) {
                Toast.makeText(
                    PortForwardApplication.appContext,
                    ev.msg,
                    ev.duration
                ).show()
            } else if (ev is PortViewModel.UiEvent.SnackBarViewLogEvent) {
                showSnackBarViewLog(ev.msg) // emit these
            }
        }
    }

    PortForwardApplication.currentSingleSelectedObject =
        remember { mutableStateOf(null) }

    if (PortForwardApplication.showContextMenu.value && PortForwardApplication.currentSingleSelectedObject.value != null) {
        EnterContextMenu(
            PortForwardApplication.currentSingleSelectedObject, // why is this not typed??
            showMoreInfoDialogState,
            navController,
            portViewModel,
            themeState
        )
    }

    if (showMoreInfoDialogState.value) {
        MoreInfoDialog(
            portMappingWithPref = PortForwardApplication.currentSingleSelectedObject.value as PortMappingWithPref,
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


    OurSnackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    var openBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState =
        rememberModalBottomSheetState(true)

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = false },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 24.dp,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            content = {
                // This is what will be shown in the bottom sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    BottomSheetSortBy(portViewModel)
                }
            }
        )
//
//            LaunchedEffect(Unit) {
//                bottomSheetState.expand()
//            }
    }

    Scaffold(

        snackbarHost = {
            SnackbarHost(OurSnackbarHostState!!) { data ->
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

            if (anyDevices) {
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
                    if (inMultiSelectMode) {
                        IconButton(onClick = {
                            portViewModel.clearSelection()
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
                    val title =
                        if (inMultiSelectMode) "${selectedIds.size} Selected" else "PortMapper"
                    Text(
                        text = title,
                        color = AdditionalColors.TextColorStrong,
                        fontWeight = FontWeight.Normal
                    )
                },
                actions = {

                    if (inMultiSelectMode) {
                        IconButton(onClick = {
                            if (inMultiSelectMode) {
                                portViewModel.deleteAll(selectedIds)
                            } else {
                                portViewModel.deleteAll()
                            }
                        })
                        {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                openBottomSheet = true
                                bottomSheetState.show()
                            }
                        })
                        {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                    }

                    OverflowMenu(
                        showAboutDialogState, portViewModel
                    )
                }
            )
        },
        content = { it ->

            // TODO can revisit this - if we already know we have some kind of device
            //   then we can forget the main loading circle and show this until we
            //   finish enumerating our first device (or have a flow which is like
            //   combine (deviceNotFound, anyDeviceStillBeingSearched))
            // still the issue of if search finishes really fast then it can hang if
            //   you set isRefreshing to false too fast (can fix by having an arbitrary delay)

            val state = rememberPullToRefreshState()
            val scope = rememberCoroutineScope()
            val isRefreshing = remember { mutableStateOf(false) }
            fun refresh() = scope.launch {

                state.animateToHidden()
                portViewModel.fullRefresh()
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                onRefresh = ::refresh,
                modifier = Modifier.padding(it),
                state = state,
                indicator = {
                    // dont show after initially pulling down
                    if (state.distanceFraction > 0f) {
                        Indicator(
                            state = state,
                            isRefreshing = isRefreshing.value,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                    }
                }
            ) {
                var heightPx by remember { mutableIntStateOf(0) }
                val boxHeight = with(LocalDensity.current) { heightPx.toDp() }
                Column(
                    Modifier
                        .onSizeChanged { heightPx = it.height }
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) {
                    if (searchStartedRecentlyAndNothingFoundYet) {
                        val offset = boxHeight * 0.28f
                        LoadingIcon(
                            "Searching for devices",
                            Modifier.offset(y = offset)
                        )
                    } else if (!anyDevices) {
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

                            // these can go through vm -> repo -> client
                            val interfacesUsedInSearch =
                                portViewModel.getInterfacesUsedInSearch()
                            val anyInterfaces =
                                portViewModel.isInitialized()
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
                                        "${interfaceUsed.networkInterface.displayName} (${interfaceUsed.networkType.networkTypeString.lowercase()})",
                                        modifier = Modifier
                                            .padding(0.dp, 0.dp)
                                            .align(Alignment.CenterHorizontally),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // if no wifi
                            if (!interfacesUsedInSearch.any { it.networkType == NetworkType.WIFI }
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
                                    portViewModel.initialize(
                                        PortForwardApplication.appContext,
                                        true
                                    )
                                    portViewModel.fullRefresh()
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
                        val uiState by portViewModel.uiState.collectAsStateWithLifecycle()
                        val selectedIds by portViewModel.selectedIds.collectAsStateWithLifecycle()
                        val isInMultiSelectMode by portViewModel.inMultiSelectMode.collectAsStateWithLifecycle()
                        PortMappingContent(
                            uiState,
                            isInMultiSelectMode,
                            portViewModel::toggle,
                            selectedIds
                        )
                    }
                }

            }

        }
    )
}

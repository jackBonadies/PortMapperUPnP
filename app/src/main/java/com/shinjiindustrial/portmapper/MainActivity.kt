package com.shinjiindustrial.portmapper


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.MainActivity.ScaffoldController
import com.shinjiindustrial.portmapper.common.MAX_PORT
import com.shinjiindustrial.portmapper.domain.ActionNames
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import org.fourthline.cling.model.meta.RemoteDevice
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

var PseudoSlotCounter: Int = MAX_PORT // as starting slot
fun GetPsuedoSlot(): Int {
    PseudoSlotCounter += 1
    return PseudoSlotCounter
}

enum class Protocol(val protocol: String) {
    TCP("TCP"),
    UDP("UDP"),
    BOTH("BOTH");

    fun str(): String {
        return protocol
    }
}


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val portViewModel: PortViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this)
        {
            if (portViewModel.inMultiSelectMode.value) {
                portViewModel.clearSelection()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        portViewModel.initialize(false)
        portViewModel.start()

        setContent {
            PortMapperMainContent(portViewModel)
        }
    }

    @Composable
    fun PortMapperMainContent(portViewModel: PortViewModel) {
        val themeState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        MyApplicationTheme(themeState) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                RootScaffold()
                { it ->
                    PortMapperNavGraph(portViewModel, themeState, Modifier.padding(it))
                }
            }
        }
    }

    @Stable
    class ScaffoldController {
        var fab: (@Composable () -> Unit)? by mutableStateOf(null)
        var topBar: (@Composable () -> Unit)? by mutableStateOf(null)
        var bottomBar: (@Composable () -> Unit)? by mutableStateOf(null)
    }

    @Composable
    fun RootScaffold(content: @Composable (PaddingValues) -> Unit) {
        val ctrl = remember { ScaffoldController() }
        CompositionLocalProvider(LocalScaffoldController provides ctrl) {
            Scaffold(
                snackbarHost = { OurSnackbarHost(portViewModel.snackbarManager) },
                topBar = { ctrl.topBar?.invoke() },
                bottomBar = { ctrl.bottomBar?.invoke() },
                floatingActionButton = { ctrl.fab?.invoke() })
            { innerPadding ->
                content(innerPadding)
            }
        }
    }
}

val LocalScaffoldController = staticCompositionLocalOf { ScaffoldController() }

fun fallbackRecursiveSearch(rootDevice: RemoteDevice) {
    // recursively look through devices
    val deviceList = mutableListOf<RemoteDevice>() //.toMutableList()
    deviceList.add(rootDevice)
    while (deviceList.isNotEmpty()) {
        val deviceInQuestion = deviceList.removeAt(0)
        for (service in deviceInQuestion.services) {
            for (action in service.actions) {
                if (ActionNames.contains(action.name)) {
                    println("Service ${service.serviceType} contains relevant action $action")
                }
            }
        }
        deviceList.addAll(deviceInQuestion.embeddedDevices)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterContextMenu(
    selectedKey: PortMappingKey?,
    getSelectedItem: (PortMappingKey) -> PortMappingWithPref,
    closeContextMenu: () -> Unit,
    showMoreInfoDialog: MutableState<PortMappingKey?>,
    navController: NavHostController,
    portViewModel: PortViewModel,
    themeState: ThemeUiState
) {
    if (selectedKey == null) {
        return
    }

    val prop = DialogProperties(
        dismissOnClickOutside = true,
        dismissOnBackPress = true,
        securePolicy = SecureFlagPolicy.SecureOff,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = true,
    )
    MyApplicationTheme(themeState) {
        Dialog(
            onDismissRequest = { closeContextMenu() },
            properties = prop,
        ) {
            Surface(
                shape = RoundedCornerShape(size = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)

                ) {
                    // it redraws starting at this inner context...
                    if (selectedKey != null) {

                        val menuItems: MutableList<Pair<String, () -> Unit>> = mutableListOf()
                        //TODO
                        val portMappingWithPref = getSelectedItem(selectedKey)
                        val portMapping = portMappingWithPref.portMapping
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Edit"
                            ) {
                                ///{description}/{internalIp}/{internalRange}/{externalIp}/{externalRange}/{protocol}/{leaseDuration}/{enabled}
                                val uriBuilder = Uri.Builder()
                                    .path("full_screen_dialog")
                                    .appendQueryParameter("description", portMapping.Description)
                                    .appendQueryParameter("internalIp", portMapping.InternalIP)
                                    .appendQueryParameter(
                                        "internalRange",
                                        portMapping.InternalPort.toString()
                                    )
                                    .appendQueryParameter(
                                        "externalIp",
                                        portMapping.DeviceIP
                                    ) // this is actual external IP as we only use it to delete the old rule...
                                    .appendQueryParameter(
                                        "externalRange",
                                        portMapping.ExternalPort.toString()
                                    )
                                    .appendQueryParameter("protocol", portMapping.Protocol)
                                    .appendQueryParameter(
                                        "leaseDuration",
                                        portMappingWithPref.getDesiredLeaseDurationOrDefault()
                                            .toString()
                                    )
                                    .appendQueryParameter("enabled", portMapping.Enabled.toString())
                                    .appendQueryParameter(
                                        "autorenew",
                                        portMappingWithPref.getAutoRenewOrDefault().toString()
                                    )
                                    .appendQueryParameter(
                                        "autorenewManualCadence",
                                        portMappingWithPref.getAutoRenewCadenceOrDefault().toString()
                                    )
                                val uri = uriBuilder.build()
                                navController.navigate(uri.toString())
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                if (portMapping.Enabled) "Disable" else "Enable"
                            ) {
                                portViewModel.enableDisable(
                                    portMappingWithPref,
                                    !portMapping.Enabled
                                )
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Renew"
                            ) {
                                portViewModel.renew(portMappingWithPref)
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "Delete"
                            ) {
                                portViewModel.delete(portMappingWithPref)
                            }
                        )
                        menuItems.add(
                            Pair<String, () -> Unit>(
                                "More Info"
                            ) {
                                showMoreInfoDialog.value = selectedKey
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
                                        closeContextMenu()
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


@Composable
fun OverflowMenu(showAboutDialogState: MutableState<Boolean>, portViewModel: PortViewModel) {
    var expanded by remember { mutableStateOf(false) }

    val isInMultiSelectMode by portViewModel.inMultiSelectMode.collectAsStateWithLifecycle()
    val selectedIds by portViewModel.selectedIds.collectAsStateWithLifecycle()

    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.semantics { testTag = "moreActionsButton" }) {
        Icon(Icons.Default.MoreVert, contentDescription = "menu")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        // this gets called on expanded, so I dont think we need to monitor additional state.

        val items: MutableList<Int> = mutableListOf()
        if (isInMultiSelectMode) {
            val anyEnabled =
                portViewModel.getSelectedItems(selectedIds).any { it -> it.portMapping.Enabled }
            val anyDisabled =
                portViewModel.getSelectedItems(selectedIds).any { it -> !it.portMapping.Enabled }
            if (anyEnabled) {
                items.add(R.string.disable_action)
            }
            if (anyDisabled) {
                items.add(R.string.enable_action)
            }
        } else {
            items.add(R.string.refresh_action)
            if (portViewModel.isInitialized()) {
                val (anyEnabled, anyDisabled) = portViewModel.getExistingRuleInfos()
                if (anyEnabled) // also get info i.e. any enabled, any disabled
                {
                    items.add(R.string.disable_all_action)
                }
                if (anyDisabled) {
                    items.add(R.string.enable_all_action)
                }
                if (anyDisabled || anyEnabled) {
                    items.add(R.string.delete_all_action)
                    items.add(R.string.renew_all_action)
                }
            }
            items.add(R.string.view_log_action)
            items.add(R.string.settings)
            items.add(R.string.about)
        }

        val context = LocalContext.current
        items.forEach { label ->
            DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                // handle item click
                expanded = false

                when (label) {
                    R.string.refresh_action -> {
                        portViewModel.fullRefresh()
                    }

                    R.string.disable_all_action -> {
                        portViewModel.enableDisableAll(false)
                    }

                    R.string.enable_all_action -> {
                        portViewModel.enableDisableAll(true)
                    }

                    R.string.disable_action -> {
                        if (isInMultiSelectMode) {
                            portViewModel.enableDisableAll(false, selectedIds)
                        } else {
                            portViewModel.enableDisableAll(false)
                        }
                    }

                    R.string.enable_action -> {
                        if (isInMultiSelectMode) {
                            portViewModel.enableDisableAll(true, selectedIds)
                        } else {
                            portViewModel.enableDisableAll(true)
                        }
                    }

                    R.string.delete_all_action -> {
                        portViewModel.deleteAll()
                    }

                    R.string.renew_all_action -> {
                        portViewModel.renewAll()
                    }

                    R.string.view_log_action -> {
                        val intent =
                            Intent(
                                context,
                                LogViewActivity::class.java
                            )
                        context.startActivity(intent)
                    }

                    R.string.settings -> {
                        val intent =
                            Intent(
                                context,
                                SettingsActivity::class.java
                            )
                        context.startActivity(intent)
                    }

                    R.string.about -> {
                        showAboutDialogState.value = true
                    }
                }
            })
        }
    }
}

fun formatIpv4(ipAddr: Int): String {
    val byteBuffer = ByteBuffer.allocate(4)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putInt(ipAddr)
    val inetAddress = InetAddress.getByAddress(null, byteBuffer.array())
    return inetAddress.hostAddress
}

data class Message(val name: String, val msg: String)

fun _getDefaultPortMapping(): PortMappingWithPref {
    return PortMappingWithPref(
        PortMapping(
            "Web Server",
            "",
            "192.168.18.13",
            80,
            80,
            "UDP",
            true,
            0,
            "192.168.18.1",
            SystemClock.elapsedRealtime(),
            0
        )
    )
}

// TODO: for mock purposes
class IGDDeviceHolder {

    var displayName: String = "Nokia"
        get() {
            return ""
        }

}
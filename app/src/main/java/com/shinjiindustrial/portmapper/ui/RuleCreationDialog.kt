package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.shinjiindustrial.portmapper.CreateRuleContents
import com.shinjiindustrial.portmapper.MainActivity
import com.shinjiindustrial.portmapper.MainActivity.Companion.OurSnackbarHostState
import com.shinjiindustrial.portmapper.MainActivity.Companion.showSnackBarShortNoAction
import com.shinjiindustrial.portmapper.MainActivity.Companion.showSnackBarViewLog
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.Protocol
import com.shinjiindustrial.portmapper.RunUIThread
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.UpnpManager.Companion.getIGDDevice
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.common.validateDescription
import com.shinjiindustrial.portmapper.common.validateEndPort
import com.shinjiindustrial.portmapper.common.validateInternalIp
import com.shinjiindustrial.portmapper.common.validateStartPort
import com.shinjiindustrial.portmapper.defaultRuleDeletedCallback
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.OurNetworkInfo
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Level


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCreationDialog(navController : NavHostController, ruleToEdit : PortMappingUserInput? = null)  {

    val isPreview = false

    val hasSubmitted = remember { mutableStateOf(false) }
    val internalPortText = rememberSaveable  { mutableStateOf(ruleToEdit?.internalRange ?: "") }
    val internalPortTextEnd = rememberSaveable  { mutableStateOf("") }
    val externalPortText = rememberSaveable  { mutableStateOf(ruleToEdit?.externalRange ?: "") }
    val externalPortTextEnd = rememberSaveable  { mutableStateOf("") }
    val leaseDuration = rememberSaveable { mutableStateOf(ruleToEdit?.leaseDuration ?: "0 (max)") }
    val description = rememberSaveable { mutableStateOf(ruleToEdit?.description ?: "") }
    val descriptionHasError = remember { mutableStateOf(validateDescription(description.value).hasError) }
    val startInternalHasError = remember { mutableStateOf(validateStartPort(internalPortText.value).hasError) }
    val endInternalHasError = remember { mutableStateOf(validateEndPort(internalPortText.value,internalPortTextEnd.value).hasError) }
    val selectedProtocolMutable = remember { mutableStateOf(ruleToEdit?.protocol ?: Protocol.TCP.str()) }
    val startExternalHasError = remember { mutableStateOf(validateStartPort(externalPortText.value).hasError) }
    val endExternalHasError = remember { mutableStateOf(validateEndPort(externalPortText.value,externalPortTextEnd.value).hasError) }
    val (ourIp, ourGatewayIp) = remember {
        if (isPreview) Pair<String, String>(
            "192.168.0.1",
            ""
        ) else OurNetworkInfo.GetLocalAndGatewayIpAddrWifi(
            PortForwardApplication.appContext,
            false
        )
    }
    val internalIp = remember { mutableStateOf(ruleToEdit?.internalIp ?: ourIp!!) }
    val internalIpHasError = remember { mutableStateOf(validateInternalIp(internalIp.value).hasError) }
    val (gatewayIps, defaultGatewayIp) = remember { UpnpManager.GetGatewayIpsWithDefault(ourGatewayIp!!) }
    val externalDeviceText = remember { mutableStateOf(defaultGatewayIp) }
    val expandedInternal = remember { mutableStateOf(false) }
    val expandedExternal = remember { mutableStateOf(false) }
    val wanIpVersionOfGatewayIsVersion1 = remember { derivedStateOf {
        val version = UpnpManager.GetDeviceByExternalIp(defaultGatewayIp)?.upnpTypeVersion ?: 2
        version == 1
    } }


    // TODO
    // var description by rememberSaveable { mutableStateOf(ruleToEdit?.description ?: "") }
    //val descriptionHasError by remember(description) {
    //  derivedStateOf { validateDescription(description).hasError }
    //}

    //
    //END

    // this can be null as its possible to start at this navhost
    if(OurSnackbarHostState == null)
    {
        MainActivity.OurSnackbarHostState = remember { SnackbarHostState() }
    }

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

                            val actualEndInternalError = expandedInternal.value && endInternalHasError.value
                            val actualEndExternalError = expandedExternal.value && endExternalHasError.value

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
                                val invalidFields = mutableListOf<String>()
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

                                val invalidFieldsStr = invalidFields.joinToString(", ")

                                MainActivity.showSnackBarLongNoAction("Invalid Fields: $invalidFieldsStr")
                                return@TextButton
                            }

                            val internalRangeStr = if(usingInternalRange) internalPortText.value + "-" + internalPortTextEnd.value else internalPortText.value
                            val externalRangeStr = if(usingExternalRange) externalPortText.value + "-" + externalPortTextEnd.value else externalPortText.value

                            val portMappingRequestInput = PortMappingUserInput(
                                description.value,
                                internalIp.value,
                                internalRangeStr,
                                externalDeviceText.value,
                                externalRangeStr,
                                selectedProtocolMutable.value,
                                leaseDuration.value.replace(" (max)",""),
                                true
                            )

                            val errorString = portMappingRequestInput.validateRange()
                            if(errorString.isNotEmpty())
                            {
                                MainActivity.showSnackBarLongNoAction(errorString)
                                return@TextButton
                            }

                            //Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()

                            val modifyCase = ruleToEdit != null


                            // TODO vm.createRule
                            // TODO vm.events -> UiEvents that are collected then launch effect

                            fun batchCallback(result: List<UPnPCreateMappingWrapperResult>) {

                                RunUIThread {
                                    //debug
                                    for (res in result) {
                                        when (res)
                                        {
                                            is UPnPCreateMappingWrapperResult.Failure -> {
                                                println("Failure")
                                                println(res.reason)
                                                println(res.response)
                                            }
                                            is UPnPCreateMappingWrapperResult.Success -> {
                                                println("Success")
                                                println(res.resultingMapping.Protocol)
                                            }
                                        }
                                    }

                                    val numFailed = result.count { it is UPnPCreateMappingWrapperResult.Failure }

                                    val anyFailed = numFailed > 0

                                    if (anyFailed) {

                                        val verbString = if(modifyCase) "modify" else "create"

                                        // all failed
                                        if (numFailed == result.size) {
                                            if (result.size == 1) {
                                                showSnackBarViewLog("Failed to $verbString rule.")
                                            } else {
                                                showSnackBarViewLog("Failed to create rules.") // emit these
                                            }
                                        } else {
                                            showSnackBarViewLog("Failed to create some rules.")
                                        }
                                    } else {
                                        showSnackBarShortNoAction("Success!")
                                    }
                                }
                            }

                            GlobalScope.launch(Dispatchers.Main) {


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


                                    val oldRulesToDelete = ruleToEdit.splitIntoRules()

                                    val device: IGDDevice = getIGDDevice(oldRulesToDelete[0].externalIp)
                                    val result = UpnpManager.GetUPnPClient().deletePortMapping(device, oldRulesToDelete[0].realize()) //TODO: handle delete multiple (when that becomes a thing)

                                    try {
                                        defaultRuleDeletedCallback(result)
                                        RunUIThread {
                                            println("delete callback")
                                            when(result)
                                            {
                                                is UPnPResult.Success ->
                                                {
                                                    // if successfully deleted original rule,
                                                    //   add new rule.
                                                    runBlocking {
                                                        val result = UpnpManager.CreatePortMappingRulesEntry(portMappingRequestInput)
                                                        batchCallback(result)
                                                    }
                                                }
                                                is UPnPResult.Failure ->
                                                {
                                                    // the old rule must still be remaining if it failed to delete
                                                    // so nothing has changed.
                                                    showSnackBarViewLog("Failed to modify entry.")
                                                }
                                            }
                                        }
                                    } catch (exception: Exception) {
                                        PortForwardApplication.OurLogger.log(
                                            Level.SEVERE,
                                            "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                                        )
                                        showSnackBarViewLog("Failed to modify entry.")
                                        throw exception
                                    }

                                    navController.popBackStack()
                                }
                            }
                            else
                            {
                                val result = UpnpManager.CreatePortMappingRulesEntry(portMappingRequestInput) //spilts into rules
                                batchCallback(result)
                                //showDialogMutable.value = false

                                navController.popBackStack()
                            }
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

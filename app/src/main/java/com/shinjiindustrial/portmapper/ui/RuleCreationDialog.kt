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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.shinjiindustrial.portmapper.CreateRuleContents
import com.shinjiindustrial.portmapper.OurSnackbarHost
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.Protocol
import com.shinjiindustrial.portmapper.common.validateDescription
import com.shinjiindustrial.portmapper.common.validateEndPort
import com.shinjiindustrial.portmapper.common.validateInternalIp
import com.shinjiindustrial.portmapper.common.validateStartPort
import com.shinjiindustrial.portmapper.domain.OurNetworkInfo
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.shinjiindustrial.portmapper.PortViewModel
import com.shinjiindustrial.portmapper.UiSnackToastEvent


@Composable
@Preview
fun RadioGroupPreview2() {
    SetupPreview()
    Text("test")
}

@Preview
@Composable
fun PreviewRuleCreationDialog() {
    //RuleCreationDialog(rememberNavController(), hiltViewModel())
    Text("test")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCreationDialog(
    navController: NavHostController,
    portViewModel: PortViewModel,
    ruleToEdit: PortMappingUserInput? = null
) {
    val isPreview = false

    val hasSubmitted = remember { mutableStateOf(false) }
    val internalPortText = rememberSaveable { mutableStateOf(ruleToEdit?.internalRange ?: "") }
    val internalPortTextEnd = rememberSaveable { mutableStateOf("") }
    val externalPortText = rememberSaveable { mutableStateOf(ruleToEdit?.externalRange ?: "") }
    val externalPortTextEnd = rememberSaveable { mutableStateOf("") }
    val leaseDuration = rememberSaveable { mutableStateOf(ruleToEdit?.leaseDuration ?: "0 (max)") }
    val description = rememberSaveable { mutableStateOf(ruleToEdit?.description ?: "") }
    val descriptionHasError =
        remember { mutableStateOf(validateDescription(description.value).hasError) }
    val startInternalHasError =
        remember { mutableStateOf(validateStartPort(internalPortText.value).hasError) }
    val endInternalHasError = remember {
        mutableStateOf(
            validateEndPort(
                internalPortText.value,
                internalPortTextEnd.value
            ).hasError
        )
    }
    val selectedProtocolMutable =
        remember { mutableStateOf(ruleToEdit?.protocol ?: Protocol.TCP.str()) }
    val startExternalHasError =
        remember { mutableStateOf(validateStartPort(externalPortText.value).hasError) }
    val endExternalHasError = remember {
        mutableStateOf(
            validateEndPort(
                externalPortText.value,
                externalPortTextEnd.value
            ).hasError
        )
    }
    val context = LocalContext.current
    val autoRenew = remember { mutableStateOf(ruleToEdit?.autoRenew ?: false) }
    val (ourIp, ourGatewayIp) = remember {
        if (isPreview) Pair<String, String>(
            "192.168.0.1",
            ""
        ) else OurNetworkInfo.GetLocalAndGatewayIpAddrWifi(
            context,
            false
        )
    }
    val internalIp = remember { mutableStateOf(ruleToEdit?.internalIp ?: ourIp!!) }
    val internalIpHasError =
        remember { mutableStateOf(validateInternalIp(internalIp.value).hasError) }
    val (gatewayIps, defaultGatewayIp) = remember {
        portViewModel.getGatewayIpsWithDefault(
            ourGatewayIp!!
        )
    }
    val externalDeviceText = remember { mutableStateOf(defaultGatewayIp) }
    val expandedInternal = remember { mutableStateOf(false) }
    val expandedExternal = remember { mutableStateOf(false) }
    val wanIpVersionOfGatewayIsVersion1 = remember {
        derivedStateOf {
            val version = portViewModel.getIGDDevice(defaultGatewayIp).getUpnpVersion()
            version == 1
        }
    }


    // TODO
    // var description by rememberSaveable { mutableStateOf(ruleToEdit?.description ?: "") }
    //val descriptionHasError by remember(description) {
    //  derivedStateOf { validateDescription(description).hasError }
    //}

    //
    //END

    Scaffold(
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
                        text = if (ruleToEdit == null) "New Rule" else "Edit Rule",
                        color = AdditionalColors.TextColorStrong,
                        fontWeight = FontWeight.Normal
                    )
                },
                actions = {
                    TextButton(
                        onClick = {

                            val actualEndInternalError =
                                expandedInternal.value && endInternalHasError.value
                            val actualEndExternalError =
                                expandedExternal.value && endExternalHasError.value

                            val usingInternalRange =
                                expandedInternal.value && internalPortTextEnd.value.isNotEmpty()
                            val usingExternalRange =
                                expandedExternal.value && externalPortTextEnd.value.isNotEmpty()

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
                                if (descriptionHasError.value) {
                                    invalidFields.add("Description")
                                }
                                if (startInternalHasError.value) {
                                    invalidFields.add(if (usingInternalRange) "Internal Port Start" else "Internal Port")
                                }
                                if (startExternalHasError.value) {
                                    invalidFields.add(if (usingExternalRange) "External Port Start" else "External Port")
                                }
                                if (actualEndInternalError) {
                                    invalidFields.add("Internal Port End")
                                }
                                if (actualEndExternalError) {
                                    invalidFields.add("External Port End")
                                }
                                if (internalIpHasError.value) {
                                    invalidFields.add("Internal IP")
                                }

                                val invalidFieldsStr = invalidFields.joinToString(", ")

                                portViewModel.snackbarManager.show(UiSnackToastEvent.SnackBarLongNoAction("Invalid Fields: $invalidFieldsStr"))
                                return@TextButton
                            }

                            val internalRangeStr =
                                if (usingInternalRange) internalPortText.value + "-" + internalPortTextEnd.value else internalPortText.value
                            val externalRangeStr =
                                if (usingExternalRange) externalPortText.value + "-" + externalPortTextEnd.value else externalPortText.value

                            val portMappingRequestInput = PortMappingUserInput(
                                description.value,
                                internalIp.value,
                                internalRangeStr,
                                externalDeviceText.value,
                                externalRangeStr,
                                selectedProtocolMutable.value,
                                leaseDuration.value.replace(" (max)", ""),
                                true,
                                autoRenew.value
                            )

                            var errorString = portMappingRequestInput.validateAutoRenew()
                            if (errorString.isNotEmpty()) {
                                portViewModel.snackbarManager.show(UiSnackToastEvent.SnackBarLongNoAction(errorString))
                                return@TextButton
                            }

                            errorString = portMappingRequestInput.validateRange()
                            if (errorString.isNotEmpty()) {
                                portViewModel.snackbarManager.show(UiSnackToastEvent.SnackBarLongNoAction(errorString))
                                return@TextButton
                            }

                            //Toast.makeText(PortForwardApplication.appContext, "Adding Rule", Toast.LENGTH_SHORT).show()

                            // TODO can be any scope with DispatchersMain.
                            GlobalScope.launch(Dispatchers.Main) {


                                if (ruleToEdit != null) {
                                    if (ruleToEdit == portMappingRequestInput) {
                                        // TODO add this back. composition local?
//                                        ourLogger.log(
//                                            Level.INFO,
//                                            "Rule has not changed. Nothing to do."
//                                        )
                                        navController.popBackStack()
                                    } else {
                                        val oldRulesToDelete = ruleToEdit.splitIntoRules()
                                        portViewModel.editRule(
                                            PortMappingWithPref(
                                                oldRulesToDelete[0].realize(),
                                                null
                                            ),
                                            portMappingRequestInput
                                        )
                                        navController.popBackStack()
                                    }
                                } else {
                                    portViewModel.createRules(portMappingRequestInput)
                                    navController.popBackStack()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (ruleToEdit == null) "CREATE" else "APPLY",
                            color = AdditionalColors.TextColorStrong,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        },
        snackbarHost = { OurSnackbarHost(portViewModel.snackbarManager) },
        content = { it ->

            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            )
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
                    wanIpVersionOfGatewayIsVersion1,
                    autoRenew
                )
            }

        })
}

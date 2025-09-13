package com.shinjiindustrial.portmapper.ui

import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.LocalScaffoldController
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
import com.shinjiindustrial.portmapper.PortViewModel
import com.shinjiindustrial.portmapper.UiSnackToastEvent
import com.shinjiindustrial.portmapper.common.AutoRenewMode
import com.shinjiindustrial.portmapper.common.ValidationError
import com.shinjiindustrial.portmapper.common.ValidationResult
import com.shinjiindustrial.portmapper.common.capLeaseDur
import com.shinjiindustrial.portmapper.common.parseCadence
import com.shinjiindustrial.portmapper.common.toMessage
import com.shinjiindustrial.portmapper.common.validateCadence
import java.util.logging.Level


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
    val autoRenewMode = remember { mutableStateOf(if(ruleToEdit != null && ruleToEdit.autoRenewManualCadence != -1) AutoRenewMode.FIXED_CADENCE else AutoRenewMode.BEFORE_EXPIRY) }
    val renewCadence = remember { mutableStateOf(if(ruleToEdit != null && ruleToEdit.autoRenewManualCadence != -1) ruleToEdit.autoRenewManualCadence.toString() else "300") }
    val validationCadenceResult = remember { derivedStateOf { validateCadence(renewCadence.value) } }
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
    val ctrl = LocalScaffoldController.current
    DisposableEffect(Unit) {
        ctrl.fab = { }
        ctrl.topBar = {
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

                            val cadenceError = autoRenewMode.value == AutoRenewMode.FIXED_CADENCE && validationCadenceResult.value.hasError
                            val cadenceValue = if (autoRenewMode.value == AutoRenewMode.FIXED_CADENCE) parseCadence(renewCadence.value).cadence else -1

                            hasSubmitted.value = true
                            if (descriptionHasError.value ||
                                cadenceError ||
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
                                if (cadenceError)
                                {
                                    invalidFields.add("Renewal Cadence")
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
                                true, //TODO ??
                                autoRenew.value,
                                cadenceValue)

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

                            if (ruleToEdit != null) {
                                if (ruleToEdit == portMappingRequestInput) {
                                    portViewModel.ourLogger.log(
                                        Level.INFO,
                                        "Rule has not changed. Nothing to do."
                                    )
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
                    ) {
                        Text(
                            text = if (ruleToEdit == null) "CREATE" else "APPLY",
                            color = AdditionalColors.TextColorStrong,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            )
        }
        onDispose { }
    }
            Column(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                RuleContents(
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
                    autoRenew,
                    autoRenewMode,
                    renewCadence,
                    validationCadenceResult.value
                )
            }
}

//In IGD release 1.0, a value of 0 was used to create a static port mapping. In version 2.0, it is no longer
//possible to create static port mappings via UPnP actions. Instead, an out-of-band mechanism is REQUIRED
//to do so (cf. WWW-administration, remote management or local management). In order to be backward
//compatible with legacy control points, the value of 0 MUST be interpreted as the maximum value (e.g.
//604800 seconds, which corresponds to one week).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.RuleContents(
    hasSubmitted: MutableState<Boolean>,
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
    gatewayIps: MutableList<String>,
    externalDeviceText: MutableState<String>,
    expandedInternal: MutableState<Boolean>,
    expandedExternal: MutableState<Boolean>,
    wanIpIsV1: State<Boolean>,
    autoRenew: MutableState<Boolean>,
    autoRenewMode: MutableState<AutoRenewMode>,
    renewCadence: MutableState<String>,
    validationCadenceResult: ValidationResult
) {

    val showLeaseDialog = remember { mutableStateOf(false) }

    if (showLeaseDialog.value) {
        DurationPickerDialog(showLeaseDialog, leaseDuration, wanIpIsV1.value)
    }

    val showRenewTimeDialog = remember { mutableStateOf(false) }

    if (showRenewTimeDialog.value) {
        DurationPickerDialog(showRenewTimeDialog, renewCadence, wanIpIsV1.value)
    }

    val descriptionErrorString =
        remember { mutableStateOf(validateDescription(description.value).validationError) }
    val interalIpErrorString =
        remember { mutableStateOf(validateInternalIp(internalIp.value).validationError) }

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
                descriptionErrorString.value =
                    validateDescription(description.value).validationError
            },
            label = { Text("Description") },
            singleLine = true,
            modifier = Modifier
                .weight(0.4f, true),//.height(60.dp),
            isError = hasSubmitted.value && descriptionHasError.value,
            trailingIcon = {
                if (hasSubmitted.value && descriptionHasError.value) {
                    Icon(
                        Icons.Filled.Error,
                        descriptionErrorString.value.toMessage(),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            supportingText = {
                if (hasSubmitted.value && descriptionHasError.value) {
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
                if (hasSubmitted.value && internalIpHasError.value) {
                    Text(
                        interalIpErrorString.value.toMessage(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (hasSubmitted.value && internalIpHasError.value) {
                    Icon(
                        Icons.Filled.Error,
                        interalIpErrorString.value.toMessage(),
                        tint = MaterialTheme.colorScheme.error
                    )
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
        val startHasErrorString =
            remember { mutableStateOf(validateStartPort(internalPortText.value).validationError) }
        StartPortExpandable(
            internalPortText,
            startInternalHasError,
            startHasErrorString,
            hasSubmitted,
            expandedInternal,
            portStartSize
        )
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
        DropDownOutline(
            externalDeviceText.value,
            {externalDeviceText.value = it },
            gatewayIps,
            stringResource(R.string.external_device),
            defaultModifier,
            {it})
        Spacer(modifier = Modifier.width(8.dp))
        val startHasErrorString =
            remember { mutableStateOf(validateStartPort(externalPortText.value).validationError) }
        StartPortExpandable(
            externalPortText,
            startExternalHasError,
            startHasErrorString,
            hasSubmitted,
            expandedExternal,
            portStartSize
        )

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
        DropDownOutline( selectedProtocolMutable.value,
            {selectedProtocolMutable.value = it },
            items = listOf(Protocol.TCP.str(), Protocol.UDP.str(), Protocol.BOTH.str()),
            "Protocol",
            defaultModifier,
            { it })

        Spacer(modifier = Modifier.width(8.dp))

        remember { mutableStateOf(false) }
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = leaseDuration.value,
            onValueChange = {
                val digitsOnly = it.filter { charIt -> charIt.isDigit() }
                if (digitsOnly.isBlank()) {
                    leaseDuration.value = digitsOnly

                } else {
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
                    if (!it.isFocused && leaseDuration.value.isBlank()) {
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

    AutoRenewSection(autoRenew.value,
        { autoRenew.value = it },
        autoRenewMode.value,
        { autoRenewMode.value = it },
        renewCadence.value,
        { renewCadence.value = it },
        validationCadenceResult,
        { showRenewTimeDialog.value = true })
}

@Composable
fun AutoRenewSection(
    autoRenew: Boolean,
    onAutoRenewChange: (Boolean) -> Unit,
    autoRenewMode: AutoRenewMode,
    onModeChange: (AutoRenewMode) -> Unit,
    cadenceSeconds: String,
    onCadenceChange: (String) -> Unit,
    validationCadenceResult : ValidationResult,
    showCadenceDialog: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = autoRenew, onCheckedChange = onAutoRenewChange, modifier = Modifier.testTag("autoRenew"))
            Text("Auto-renew")
        }

        AnimatedVisibility(visible = autoRenew) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(createNewRuleRowWidth)
                ) {
                    val defaultModifier = Modifier
                        .weight(0.5f, true)
                    DropDownOutline(
                        autoRenewMode,
                        onModeChange,
                        AutoRenewMode.entries.toList(),
                        stringResource(R.string.auto_renew_mode),
                        defaultModifier,
                        { stringResource(it.titleRes) })

                        AnimatedVisibility(visible = autoRenewMode == AutoRenewMode.FIXED_CADENCE,
                            defaultModifier.padding(start = 8.dp)) {

                            OutlinedTextField(
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                value = cadenceSeconds,
                                onValueChange = {
                                    val digitsOnly = it.filter { charIt -> charIt.isDigit() }
                                    onCadenceChange(digitsOnly)
                                },
                                label = { Text("Renewal Cadence") },
                                isError = validationCadenceResult.hasError,
                                supportingText = {
                                    if(validationCadenceResult.hasError)
                                        Text(
                                            validationCadenceResult.validationError.toMessage(),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                trailingIcon = {
                                    IconButton(onClick = { showCadenceDialog() })
                                    {
                                        if(validationCadenceResult.hasError)
                                        {
                                            Icon(
                                                Icons.Filled.Schedule,
                                                contentDescription = validationCadenceResult.validationError.toMessage(),
                                            )
                                        }
                                        else
                                        {
                                            Icon(
                                                Icons.Filled.Schedule,
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.5f, true)
                                    .onFocusChanged {
                                        // one is allowed to temporarily leave blank.
                                        // but on leaving it must be valid
                                        if (!it.isFocused && cadenceSeconds.isBlank()) {
                                            onCadenceChange("300")
                                        }
                                    },//isFocused.value = it.isFocused },
                            )
                        }
                    }
        }
    }
}

@Composable
fun StartPortExpandable(
    portText: MutableState<String>,
    hasError: MutableState<Boolean>,
    errorString: MutableState<ValidationError>,
    hasSubmitted: MutableState<Boolean>,
    expanded: MutableState<Boolean>,
    startPortSize: MutableState<IntSize>
) {
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
        label = { if (expanded.value) Text("Port Start") else Text("Port") },
        //modifier = Modifier.then(modifier),
        //.height(60.dp),
        isError = hasSubmitted.value && hasError.value,
        supportingText = {
            if (hasSubmitted.value && hasError.value) {
                Text(errorString.value.toMessage(), color = MaterialTheme.colorScheme.error)
            }
        },
        trailingIcon = {
            if (hasSubmitted.value && hasError.value) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = errorString.value.toMessage(),
                    tint = MaterialTheme.colorScheme.error
                ) //TODO set error on main theme if not already.
            } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun outlineTextWithPicker() {
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

            OutlinedTextField(
                "Port",
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

                OutlinedTextField(
                    "Port",
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

            OutlinedTextField(
                "Port",
                onValueChange = {

                },
                modifier = Modifier.weight(.5f),
                trailingIcon = {
                    IconButton(onClick = { })
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
fun PortRangeRow(
    startPortText: MutableState<String>,
    endPortText: MutableState<String>,
    startHasError: MutableState<Boolean>,
    endHasError: MutableState<Boolean>,
    hasSubmitted: MutableState<Boolean>,
    modifier: Modifier,
    portSize: IntSize
) {
    DeviceRow()
    {

        val endHasErrorString = remember {
            mutableStateOf(
                validateEndPort(
                    startPortText.value,
                    endPortText.value
                ).validationError
            )
        }

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
                endHasErrorString.value =
                    validateEndPort(startPortText.value, endPortText.value).validationError
            },
            label = { Text("Port End") },
            modifier = Modifier.width(with(LocalDensity.current) { portSize.width.toDp() }),
            isError = hasSubmitted.value && endHasError.value,
            supportingText = {
                if (hasSubmitted.value && endHasError.value) {
                    Text(
                        endHasErrorString.value.toMessage(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },


            trailingIcon =
                if (hasSubmitted.value && endHasError.value) {
                    @Composable {
                        if (hasSubmitted.value && endHasError.value) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = endHasErrorString.value.toMessage(),
                                tint = MaterialTheme.colorScheme.error
                            ) //TODO set error on main theme if not already.
                        }
                    }
                } else {
                    null
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropDownOutline(
    selected: T,
    onSelected: (T) -> Unit,
    items: List<T>,
    label: String,
    modifier: Modifier = Modifier,
    // How to render each item as text (works for enums & Strings)
    itemText: @Composable (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = itemText(selected),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()        // anchors the menu to the field and matches width
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemText(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
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

@Preview(showBackground = true)
@Composable
fun CreateRuleContentsPreview() {
    Column {
        RuleContents(
            hasSubmitted = remember { mutableStateOf(false) },
            internalPortText = remember { mutableStateOf("8080") },
            internalPortTextEnd = remember { mutableStateOf("") },
            externalPortText = remember { mutableStateOf("80") },
            externalPortTextEnd = remember { mutableStateOf("") },
            leaseDuration = remember { mutableStateOf("3600") },
            description = remember { mutableStateOf("My test rule") },
            descriptionHasError = remember { mutableStateOf(false) },
            startInternalHasError = remember { mutableStateOf(false) },
            endInternalHasError = remember { mutableStateOf(false) },
            selectedProtocolMutable = remember { mutableStateOf("TCP") },
            startExternalHasError = remember { mutableStateOf(false) },
            endExternalHasError = remember { mutableStateOf(false) },
            internalIp = remember { mutableStateOf("192.168.1.100") },
            internalIpHasError = remember { mutableStateOf(false) },
            gatewayIps = mutableListOf("192.168.1.1", "10.0.0.1"),
            externalDeviceText = remember { mutableStateOf("WAN") },
            expandedInternal = remember { mutableStateOf(false) },
            expandedExternal = remember { mutableStateOf(false) },
            wanIpIsV1 = remember { mutableStateOf(true) },
            autoRenew = remember { mutableStateOf(true) },
            autoRenewMode = remember { mutableStateOf(AutoRenewMode.BEFORE_EXPIRY) },
            renewCadence = remember { mutableStateOf("3600") },
            validationCadenceResult = ValidationResult.ok
        )
    }
}

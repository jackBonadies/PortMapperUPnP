package com.shinjiindustrial.portmapper.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.IsMultiSelectMode
import com.shinjiindustrial.portmapper.MainActivity
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.ToggleSelection
import com.shinjiindustrial.portmapper._getDefaultPortMapping
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.domain.UpnpViewRow
import com.shinjiindustrial.portmapper.domain.Urgency
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.com.shinjiindustrial.portmapper.PortUiState
import java.com.shinjiindustrial.portmapper.ThemeUiState

@OptIn(
    ExperimentalUnitApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PortMappingCard(portMappingWithPref: PortMappingWithPref, now: Long = -1, additionalModifier : Modifier = Modifier.Companion)
{
    val portMapping = portMappingWithPref.portMapping
    println("external ip test ${portMapping.DeviceIP}")

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
                            ToggleSelection(portMappingWithPref)
                        } else {
                            // TODO cleanup
                            PortForwardApplication.Companion.showContextMenu.value = true
                            PortForwardApplication.Companion.currentSingleSelectedObject.value =
                                portMappingWithPref
                        }

                    },
                    onLongClick = {
                        ToggleSelection(portMappingWithPref)
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
                modifier = Modifier.Companion
                    .padding(2.dp, 6.dp, 15.dp, 6.dp),//.background(Color(0xffc5dceb)),
                //.background(MaterialTheme.colorScheme.secondaryContainer),
                verticalAlignment = Alignment.Companion.CenterVertically

            ) {

                val multiSelectMode = IsMultiSelectMode()
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
                        MainActivity.Companion.MultiSelectItems!!.contains(portMappingWithPref),
                        true,
                        Modifier.Companion.padding(10.dp, 0.dp, 2.dp, 0.dp)
                    ) {

                        ToggleSelection(portMappingWithPref)

                    }
                }

                Column(
                    modifier = Modifier.Companion.weight(1f).padding(padLeft, 0.dp, 0.dp, 0.dp)
                ) {
                    Text(
                        portMapping.Description,
                        fontSize = TextUnit(20f, TextUnitType.Companion.Sp),
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = AdditionalColors.TextColor
                    )
                    Text(portMapping.InternalIP, color = AdditionalColors.TextColor)

                    val text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = if (portMapping.Enabled) AdditionalColors.Enabled_Green else AdditionalColors.Disabled_Red)) {
                            append("⬤")
                        }
                        withStyle(style = SpanStyle()) {
                            append(if (portMapping.Enabled) " Enabled" else " Disabled")
                        }
                    }


                    Text(text, color = AdditionalColors.TextColor)
                    val urgency = portMapping.getUrgency(portMappingWithPref.getAutoRenewOrDefault(), now)
                    val color by urgencyColor(urgency, AdditionalColors.TextColor, AdditionalColors.LogWarningText, AdditionalColors.LogErrorText)
                    Text(portMapping.getRemainingLeaseTimeRoughString(portMappingWithPref.getAutoRenewOrDefault(), now), color = color)
                }

                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    Text(
                        "${portMapping.ExternalPort} ➝ ${portMapping.InternalPort}",
                        fontSize = TextUnit(20f, TextUnitType.Companion.Sp),
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = AdditionalColors.TextColor
                    )
                    Text("${portMapping.Protocol}", color = AdditionalColors.TextColor)

                }
            }
        }
}

@Composable
fun urgencyColor(
    urgency: Urgency,
    normal: Color,
    warn: Color,
    error: Color,
    expired: Color = Color.Gray
) = animateColorAsState(
    when (urgency) {
        Urgency.Normal  -> normal
        Urgency.Warn    -> warn
        Urgency.Error   -> error
    }
)

//@Preview(showBackground = true)
//@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun PortMappingCardPreview() //TODO: rename?
//{
//    SetupPreview()
//    PortMappingCard(
//        PortMapping(
//            "Web Server",
//            "",
//            "192.168.18.13",
//            80,
//            80,
//            "UDP",
//            true,
//            0,
//            "192.168.18.1",
//            SystemClock.elapsedRealtime(),
//            0
//        )
//    )
//}

@Preview(showBackground = true)
@Composable
fun PortMappingCardAltPreview() // TODO: rename?
{
    SetupPreview()
    MyApplicationTheme(ThemeUiState(DayNightMode.FORCE_NIGHT, false)) {

        PortMappingCardAlt(
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
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun PortMappingCardAlt(portMapping: PortMapping)
{
    Card(
        modifier = Modifier.Companion
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
            modifier = Modifier.Companion
                .padding(15.dp, 6.dp),//.background(Color(0xffc5dceb)),
            //.background(MaterialTheme.colorScheme.secondaryContainer),
            verticalAlignment = Alignment.Companion.CenterVertically

        ) {
            Column(modifier = Modifier.Companion.weight(1f)) {
                Text(
                    portMapping.Description,
                    fontSize = TextUnit(20f, TextUnitType.Companion.Sp),
                    fontWeight = FontWeight.Companion.SemiBold
                )
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
            Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                Text(
                    text = "On",
                    fontSize = TextUnit(20f, TextUnitType.Companion.Sp),
                    fontWeight = FontWeight.Companion.SemiBold,
                    modifier = Modifier.Companion
                        .padding(0.dp)
                        .background(color = Color(0xFF8FCE91), shape = RoundedCornerShape(10.dp))
                        .padding(16.dp, 8.dp),
                )

            }
        }
    }
}

@Preview
@Composable
fun PreviewDeviceHeader()
{
    //TODO
    //DeviceHeader(IGDDevice(null, null))
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun DeviceHeader(device : IIGDDevice)
{
    Spacer(modifier = Modifier.Companion.padding(2.dp))
    Column(
        modifier = Modifier.Companion
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
            device.getDisplayName(),
            fontWeight = FontWeight.Companion.SemiBold,
            fontSize = TextUnit(24f, TextUnitType.Companion.Sp),
            color = AdditionalColors.TextColor

        )
        Text(device.getIpAddress(), color = AdditionalColors.TextColor)
    }
    Spacer(modifier = Modifier.Companion.padding(2.dp))
}

@Preview
@Composable
fun LoadingIcon()
{
    LoadingIcon("Searching for devices", Modifier.Companion)
}

@Composable
fun LoadingIcon(label : String, modifier : Modifier)
{
    Column(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterHorizontally)
                .size(160.dp), strokeWidth = 6.dp, color = MaterialTheme.colorScheme.secondary
        )
        Text(
            "Searching for devices", modifier = Modifier.Companion
                .align(Alignment.Companion.CenterHorizontally)
                .padding(0.dp, 30.dp, 0.dp, 0.dp)
        )
    }

}

@Composable
fun NoMappingsCard(remoteDevice : IIGDDevice) {
    NoMappingsCard()
}

@Preview
@Composable
fun NoMappingsCard()
{
    MyApplicationTheme(ThemeUiState(DayNightMode.FORCE_NIGHT, false)) {
        Card(
//        onClick = {
//            if(PortForwardApplication.showPopup != null)
//            {
//                PortForwardApplication.showPopup.value = true
//            }
//                  },
            modifier = Modifier.Companion
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
                modifier = Modifier.Companion
                    .padding(15.dp, 36.dp),//.background(Color(0xffc5dceb)),
                //.background(MaterialTheme.colorScheme.secondaryContainer),
                verticalAlignment = Alignment.Companion.CenterVertically

            ) {
                Column(modifier = Modifier.Companion.weight(1f)) {


                    // this one is awkward if one intentionally removes all port mappings
                    //var deviceHasNoPortMappings = "No port mappings found \nfor this device"
                    "Device has no UPnP port mappings"

                    Text(
                        "No port mappings found \nfor this device",
                        fontSize = TextUnit(20f, TextUnitType.Companion.Sp),
                        fontWeight = FontWeight.Companion.SemiBold,
                        modifier = Modifier.Companion
                            .align(Alignment.Companion.CenterHorizontally)
                            .padding(0.dp, 0.dp, 0.dp, 8.dp),
                        textAlign = TextAlign.Companion.Center,
                        color = AdditionalColors.TextColor
                    )
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.Companion.fillMaxWidth()
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

                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ScaffoldDemo() {
    Color(0xFF1976D2)
    Scaffold(
        topBar = {


            TopAppBar(
                modifier = Modifier.Companion.height(36.dp),  // change the height here
                title = { Text(text = "hello world") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Text("X")
            }
        },
        content = { it ->
            Column(Modifier.Companion.padding(it)) {
                Text("BodyContent")
                Text("BodyContent")
                Text("BodyContent")
                Text("BodyContent")
                Text("BodyContent")
            }
        },
    )
}
@Preview
@Composable
fun PreviewConversation() {
    SetupPreview()
    MyApplicationTheme(ThemeUiState(DayNightMode.FORCE_NIGHT, false)) {
        val msgs = mutableListOf<UpnpViewRow>()
        val pm = _getDefaultPortMapping()
        val upnpViewEl = UpnpViewRow.PortViewRow(pm)
        for (i in 0..20)
        {
            msgs.add(upnpViewEl)
        }
        Conversation(msgs)
    }
}

@Composable
fun PortMappingContent(uiState : PortUiState)
{
    Conversation(uiState.items)
}

@Composable
fun rememberTicker(periodMillis: Long): androidx.compose.runtime.State<Long> {
    Log.i("rememberTicker", "called")
    val now = remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(periodMillis)
            now.value = SystemClock.elapsedRealtime()
        }
    }
    return now
}

//lazy column IS recycler view basically. both recycle.
@OptIn(ExperimentalFoundationApi::class, ExperimentalUnitApi::class)
@Composable
fun Conversation(messages: List<UpnpViewRow>) {

    val now by rememberTicker(8_000)

    LazyColumn(
        //modifier = Modifier.background(MaterialTheme.colorScheme.background),
        modifier = Modifier
            .background(AdditionalColors.Background)
            .fillMaxHeight()
            .fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),

        ) {

        itemsIndexed(messages, key = { index, message -> message.key }) { index, message -> //, key = {indexIt, keyIt -> keyIt.hashCode() }

            when(message) {
                is UpnpViewRow.DeviceHeaderViewRow -> {
                    DeviceHeader(message.device)
                }
                is UpnpViewRow.DeviceEmptyViewRow -> {
                    NoMappingsCard(message.device)
                }
                is UpnpViewRow.PortViewRow -> {
                    PortMappingCard(message.portMapping, now, Modifier.animateItem())
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // so FAB doesnt get in way
        }


    }

}

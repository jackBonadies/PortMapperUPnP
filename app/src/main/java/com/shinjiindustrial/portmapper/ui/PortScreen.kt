package com.shinjiindustrial.portmapper.ui

import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper._getDefaultPortMapping
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.LocalRule
import com.shinjiindustrial.portmapper.domain.LocalRuleKey
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.domain.RuleSection
import com.shinjiindustrial.portmapper.domain.UpnpViewRow
import com.shinjiindustrial.portmapper.domain.Urgency
import com.shinjiindustrial.portmapper.domain.formatAgo
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import com.shinjiindustrial.portmapper.ui.theme.PortMapperTheme
import kotlinx.coroutines.delay
import com.shinjiindustrial.portmapper.PortUiState
import com.shinjiindustrial.portmapper.ThemeUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PortMappingCard(
    portMappingWithPref: PortMappingWithPref,
    now: Long = -1,
    isInMultiSelectMode: Boolean = false,
    toggleSelection: (PortMappingKey) -> Unit = {},
    onClick: (PortMappingKey) -> Unit = {},
    selectedIds: Set<PortMappingKey>,
    additionalModifier: Modifier = Modifier.Companion
) {
    val portMapping = portMappingWithPref.portMapping

    Card(
        modifier = additionalModifier
            .fillMaxWidth()
            .padding(4.dp, 4.dp)
            .combinedClickable(
                onClick = {

                    if (isInMultiSelectMode) {
                        toggleSelection(portMappingWithPref.getKey())
                    } else {
                        onClick(portMappingWithPref.getKey())
                    }

                },
                onLongClick = {
                    toggleSelection(portMappingWithPref.getKey())
                }
            ),
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = PortMapperTheme.componentColors.cardContainer,
        ),
    ) {

        Row(
            modifier = Modifier.Companion
                .padding(2.dp, 10.dp, 14.dp, 10.dp),
            verticalAlignment = Alignment.Companion.CenterVertically

        ) {

            //TODO need to do existing content slide to right and fade new element in
            val padLeft = 13.dp

            AnimatedVisibility(
                visible = isInMultiSelectMode,
            ) {
                CircleCheckbox(
                    selectedIds.contains(portMappingWithPref.getKey()),
                    true,
                    Modifier.Companion.padding(10.dp, 0.dp, 2.dp, 0.dp)
                ) {
                    toggleSelection(portMappingWithPref.getKey())
                }
            }

            Column(
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(padLeft, 0.dp, 0.dp, 0.dp)
            ) {
                // pill is centered on the title + ip block, the lease line runs full width below
                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                    Column(
                        modifier = Modifier.Companion
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        RuleTitle(portMapping.Description) {
                            // nearly every rule is enabled, so only the exception gets called out
                            if (!portMapping.Enabled) {
                                StatusBadge("Disabled", PortMapperTheme.semanticColors.disabled)
                            }
                        }
                        Text(
                            portMapping.InternalIP,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    PortPill(
                        portMapping.ExternalPort,
                        portMapping.InternalPort,
                        portMapping.Protocol,
                        active = true
                    )
                }

                val semanticColors = PortMapperTheme.semanticColors
                val urgency =
                    portMapping.getUrgency(portMappingWithPref.getAutoRenewOrDefault(), now)
                val color by urgencyColor(
                    urgency,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    semanticColors.logWarning,
                    semanticColors.logError
                )
                Text(
                    portMappingWithPref.getRemainingLeaseOrRenewTimeRoughString(now),
                    color = color
                )
            }
        }
    }
}

// mostly same as PortMappingCard, slightly ghosted / disabled feel
@Composable
fun LocalRuleCard(
    localRule: LocalRule,
    now: Long = -1,
    isInMultiSelectMode: Boolean = false,
    onClick: (LocalRuleKey) -> Unit = {},
    additionalModifier: Modifier = Modifier.Companion
) {
    val entity = localRule.entity
    val nowUtc = remember(now) { System.currentTimeMillis() }

    Card(
        modifier = additionalModifier
            .fillMaxWidth()
            .padding(4.dp, 4.dp)
            .clickable(enabled = !isInMultiSelectMode) { onClick(localRule.key) },
        elevation = CardDefaults.cardElevation(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = PortMapperTheme.componentColors.cardContainer,
        ),
    ) {
        Row(
            modifier = Modifier.Companion
                .alpha(0.7f)
                .padding(2.dp, 10.dp, 14.dp, 10.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            val padLeft = 13.dp
            Column(
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(padLeft, 0.dp, 0.dp, 0.dp)
            ) {
                Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                    Column(
                        modifier = Modifier.Companion
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        RuleTitle(entity.description) {
                            val semanticColors = PortMapperTheme.semanticColors
                            if (localRule.drifted) {
                                StatusBadge("Drifted", semanticColors.logWarning)
                            }
                            // what an activate would ask for, not anything the router said
                            if (!entity.desiredEnabled) {
                                StatusBadge("Disabled", semanticColors.disabled)
                            }
                        }
                        Text(entity.internalIp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    PortPill(
                        entity.externalPort,
                        entity.internalPort,
                        entity.protocol,
                        active = false
                    )
                }

                val lastSeen = entity.lastSeenAtUtcMs
                Text(
                    if (lastSeen == null) "Not on router" else "Last seen ${formatAgo(lastSeen, nowUtc)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// title with any status badges trailing it.  the title wraps rather than truncates (router
//   supplied descriptions can be long) and the badges stay centered on it.
@Composable
private fun RuleTitle(title: String, badges: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Companion.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.Companion.weight(1f, fill = false)
        )
        badges()
    }
}

// small tinted pill i.e. "Disabled" / "Drifted".  the theme has no container tokens for the
//   semantic colors, so the fill is the text color at low alpha, which reads in both themes.
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier.Companion) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), shape)
            .border(1.dp, color.copy(alpha = 0.4f), shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Companion.SemiBold,
        color = color
    )
}

// "5050 → 5050 TCP" on one line.  active (on the router) is primary text over a 10% primary
//   wash (M3's state-layer technique - primaryContainer read as a tonal button and was too
//   loud, secondaryContainer / surfaceContainerHighest were tried and rejected), inactive
//   (local only) is just the outline so it reads ghosted next to a live rule.  both share the
//   outlineVariant border so the fill is the only difference.
@Composable
fun PortPill(
    externalPort: Int,
    internalPort: Int,
    protocol: String,
    active: Boolean,
    modifier: Modifier = Modifier.Companion
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val fill = if (active) colors.primary.copy(alpha = 0.10f) else Color.Companion.Transparent
    val border = colors.outlineVariant
    val textColor = if (active) colors.primary else colors.onSurfaceVariant
    Row(
        modifier = modifier
            .background(fill, shape)
            .border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            // the arrow stays in the default face: the monospace fallback glyph is small and
            //   gets a full cell of side bearing either side
            buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Companion.Monospace)) {
                    append(externalPort.toString())
                }
                append(" → ")
                withStyle(SpanStyle(fontFamily = FontFamily.Companion.Monospace)) {
                    append(internalPort.toString())
                }
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Companion.SemiBold,
            color = textColor,
            maxLines = 1
        )
        Text(
            protocol,
            fontFamily = FontFamily.Companion.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Companion.Bold,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.Companion.padding(start = 6.dp)
        )
    }
}

// "ON ROUTER · refreshed 3:17 PM" / "LOCAL · inactive"
@Composable
fun SectionHeader(section: RuleSection, device: IIGDDevice, now: Long) {
    val detail = when (section) {
        // null until the device finishes enumerating
        RuleSection.OnRouter -> device.enumeratedAtUtcMs?.let { "refreshed ${formatClockTime(it, now)}" }
        RuleSection.Local -> "inactive"
    }
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Companion.SemiBold)) {
                append(section.label)
            }
            if (detail != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Companion.Normal)) {
                    append(" · $detail")
                }
            }
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 10.dp, bottom = 2.dp)
    )
}

// local time of day i.e. "3:17 PM", with the date in front once it is no longer today.
//   DateUtils honors the user's 12/24 hour setting and locale, and only adds the year when it
//   differs from the current one.  `now` is just a recompute key so the "today" check moves
//   past midnight with the list ticker.
@Composable
fun formatClockTime(utcMs: Long, now: Long): String {
    val context = LocalContext.current
    return remember(utcMs, now) {
        var flags = DateUtils.FORMAT_SHOW_TIME
        if (!DateUtils.isToday(utcMs)) {
            flags = flags or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
        }
        DateUtils.formatDateTime(context, utcMs, flags)
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
        Urgency.Normal -> normal
        Urgency.Warn -> warn
        Urgency.Error -> error
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
fun PortMappingCardAlt(portMapping: PortMapping) {
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
fun PreviewDeviceHeader() {
    //TODO
    //DeviceHeader(IGDDevice(null, null))
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun DeviceHeader(device: IIGDDevice, onInfoClick: () -> Unit) {
    Spacer(modifier = Modifier.Companion.padding(2.dp))
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(6.dp, 4.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    )
    {
        Column(modifier = Modifier.Companion.weight(1f))
        {
            Text(
                device.getDisplayName(),
                fontWeight = FontWeight.Companion.SemiBold,
                fontSize = TextUnit(24f, TextUnitType.Companion.Sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant

            )
            Text(device.getIpAddress(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Device info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.Companion.padding(2.dp))
}

@Preview
@Composable
fun LoadingIcon() {
    LoadingIcon("Searching for devices", Modifier.Companion)
}

@Composable
fun LoadingIcon(label: String, modifier: Modifier) {
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
fun NoMappingsCard(remoteDevice: IIGDDevice) {
    NoMappingsCard()
}

@Preview
@Composable
fun NoMappingsCard() {
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
                containerColor = PortMapperTheme.componentColors.cardContainer,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.Companion.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                            //color = MaterialTheme.colors.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                            //tint = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = " to add new rules",
                            //style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        for (i in 0..20) {
            msgs.add(upnpViewEl)
        }
        PortMappingsListContent(msgs, false, {}, {}, {}, emptySet())
    }
}

@Composable
fun PortMappingContent(
    uiState: PortUiState,
    isInMultiSelectMode: Boolean,
    onToggle: (PortMappingKey) -> Unit,
    onClick: (PortMappingKey) -> Unit,
    onLocalClick: (LocalRuleKey) -> Unit,
    selectedIds: Set<PortMappingKey>
) {
    PortMappingsListContent(uiState.items, isInMultiSelectMode, onToggle, onClick, onLocalClick, selectedIds)
}

@Composable
fun rememberTicker(periodMillis: Long): androidx.compose.runtime.State<Long> {
    val now = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
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
fun PortMappingsListContent(
    messages: List<UpnpViewRow>,
    isInMultiSelectMode: Boolean,
    onToggle: (PortMappingKey) -> Unit,
    onClick: (PortMappingKey) -> Unit,
    onLocalClick: (LocalRuleKey) -> Unit,
    selectedIds: Set<PortMappingKey>
) {

    val now by rememberTicker(8_000)

    // remember not rememberSaveable - IIGDDevice isnt Parcelable.
    var infoDevice by remember { mutableStateOf<IIGDDevice?>(null) }

    LazyColumn(
        //modifier = Modifier.background(MaterialTheme.colorScheme.background),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxHeight()
            .fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),

        ) {

        itemsIndexed(
            messages,
            key = { index, message -> message.key }) { index, message -> //, key = {indexIt, keyIt -> keyIt.hashCode() }

            when (message) {
                is UpnpViewRow.DeviceHeaderViewRow -> {
                    DeviceHeader(message.device) { infoDevice = message.device }
                }

                is UpnpViewRow.DeviceEmptyViewRow -> {
                    NoMappingsCard(message.device)
                }

                is UpnpViewRow.PortViewRow -> {
                    PortMappingCard(
                        message.portMapping,
                        now,
                        isInMultiSelectMode,
                        onToggle,
                        onClick,
                        selectedIds,
                        Modifier.animateItem()
                    )
                }

                is UpnpViewRow.SectionHeaderViewRow -> {
                    SectionHeader(message.section, message.device, now)
                }

                is UpnpViewRow.LocalRuleViewRow -> {
                    LocalRuleCard(
                        message.localRule,
                        now,
                        isInMultiSelectMode,
                        onLocalClick,
                        Modifier.animateItem()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // so FAB doesnt get in way
        }


    }

    infoDevice?.let { DeviceInfoBottomSheet(it) { infoDevice = null } }
}

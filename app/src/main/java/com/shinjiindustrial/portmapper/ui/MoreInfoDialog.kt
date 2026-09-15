package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.shinjiindustrial.portmapper._getDefaultPortMapping
import com.shinjiindustrial.portmapper.domain.LocalRule
import com.shinjiindustrial.portmapper.domain.LocalRuleKey
import com.shinjiindustrial.portmapper.domain.LocalRuleStatus
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO uncomment
//@Preview
//@Composable
//fun MoreInfoDialog() {
//    SetupPreview()
//    MoreInfoDialog(_getDefaultPortMapping(), remember { mutableStateOf(true) })
//}

@Composable
fun MoreInfoDialog(
    showMoreInfoDialog: MutableState<PortMappingKey?>,
    getSelectedItem: (PortMappingKey) -> PortMappingWithPref) {
    if (showMoreInfoDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showMoreInfoDialog.value = null },
            title = { Text("Info") },
            text = {
                val portMapping = getSelectedItem(showMoreInfoDialog.value!!).portMapping
                val pairs = mutableListOf<Pair<String, String>>()
                pairs.add(Pair("Internal IP", portMapping.InternalIP))
                pairs.add(Pair("Internal Port", portMapping.InternalPort.toString()))
                pairs.add(Pair("External IP", portMapping.DeviceIP))
                pairs.add(Pair("External Port", portMapping.ExternalPort.toString()))
                pairs.add(Pair("Protocol", portMapping.Protocol))
                pairs.add(Pair("Enabled", if (portMapping.Enabled) "True" else "False"))
                pairs.add(Pair("Expires", portMapping.getRemainingLeaseTimeString()))
                Column {
                    for (p in pairs) {
                        KeyValueRow(p.first, p.second)
                    }
                }

            },
            confirmButton = {
                Button(onClick = { showMoreInfoDialog.value = null }) {
                    Text("OK")
                }
            })
    }
}

@Composable
fun LocalRuleInfoDialog(
    showLocalInfoDialog: MutableState<LocalRuleKey?>,
    getSelectedLocalRule: (LocalRuleKey) -> LocalRule?
) {
    val key = showLocalInfoDialog.value ?: return
    // the rule can move back onto the router while this is up
    val localRule = getSelectedLocalRule(key)
    if (localRule == null) {
        LaunchedEffect(key) { showLocalInfoDialog.value = null }
        return
    }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    fun formatDate(utcMs: Long?): String {
        return if (utcMs == null) "Unknown" else dateFormat.format(Date(utcMs))
    }

    AlertDialog(
        onDismissRequest = { showLocalInfoDialog.value = null },
        title = { Text("Local Rule Info") },
        text = {
            val entity = localRule.entity
            val pairs = mutableListOf<Pair<String, String>>()
            pairs.add(Pair("Device", localRule.device.getDisplayName()))
            pairs.add(Pair("Internal IP", entity.internalIp))
            pairs.add(Pair("Internal Port", entity.internalPort.toString()))
            pairs.add(Pair("External Port", entity.externalPort.toString()))
            pairs.add(Pair("Protocol", entity.protocol))
            pairs.add(Pair("Lease", if (entity.desiredLeaseDuration == 0) "Never expires" else "${entity.desiredLeaseDuration} s"))
            pairs.add(Pair("Auto Renew", if (entity.autoRenew) "True" else "False"))
            pairs.add(Pair("Enabled", if (entity.desiredEnabled) "True" else "False"))
            pairs.add(Pair("Created", formatDate(entity.createdAtUtcMs)))
            pairs.add(Pair("Last Seen", formatDate(entity.lastSeenAtUtcMs)))
            pairs.add(
                Pair(
                    "Status",
                    when (localRule.status) {
                        LocalRuleStatus.Missing -> "Missing from router"
                        LocalRuleStatus.Drifted -> "Drifted - the router has a different rule at this port"
                        LocalRuleStatus.SiblingActive -> "Inactive - another of your rules is using this port"
                    }
                )
            )
            Column {
                for (p in pairs) {
                    KeyValueRow(p.first, p.second)
                }
            }
        },
        confirmButton = {
            Button(onClick = { showLocalInfoDialog.value = null }) {
                Text("OK")
            }
        })
}

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.R
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
            title = { Text(stringResource(R.string.info)) },
            text = {
                val portMapping = getSelectedItem(showMoreInfoDialog.value!!).portMapping
                val pairs = mutableListOf<Pair<String, String>>()
                pairs.add(Pair(stringResource(R.string.internal_ip), portMapping.InternalIP))
                pairs.add(Pair(stringResource(R.string.internal_port), portMapping.InternalPort.toString()))
                pairs.add(Pair(stringResource(R.string.external_ip), portMapping.DeviceIP))
                pairs.add(Pair(stringResource(R.string.external_port), portMapping.ExternalPort.toString()))
                pairs.add(Pair(stringResource(R.string.protocol), portMapping.Protocol))
                pairs.add(Pair(stringResource(R.string.enabled), booleanText(portMapping.Enabled)))
                pairs.add(Pair(stringResource(R.string.expires), portMapping.getRemainingLeaseTimeString()))
                Column {
                    for (p in pairs) {
                        KeyValueRow(p.first, p.second)
                    }
                }

            },
            confirmButton = {
                Button(onClick = { showMoreInfoDialog.value = null }) {
                    Text(stringResource(R.string.ok))
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
    val unknown = stringResource(R.string.unknown)
    fun formatDate(utcMs: Long?): String {
        return if (utcMs == null) unknown else dateFormat.format(Date(utcMs))
    }

    AlertDialog(
        onDismissRequest = { showLocalInfoDialog.value = null },
        title = { Text(stringResource(R.string.local_rule_info)) },
        text = {
            val entity = localRule.entity
            val pairs = mutableListOf<Pair<String, String>>()
            pairs.add(Pair(stringResource(R.string.device), localRule.device.getDisplayName()))
            // set only when the row was created on a different router than the one it is
            //   listed under ("show local rules from all routers")
            localRule.sourceDeviceName?.let { pairs.add(Pair(stringResource(R.string.originally_on), it)) }
            pairs.add(Pair(stringResource(R.string.internal_ip), entity.internalIp))
            pairs.add(Pair(stringResource(R.string.internal_port), entity.internalPort.toString()))
            pairs.add(Pair(stringResource(R.string.external_port), entity.externalPort.toString()))
            pairs.add(Pair(stringResource(R.string.protocol), entity.protocol))
            pairs.add(
                Pair(
                    stringResource(R.string.lease),
                    if (entity.desiredLeaseDuration == 0) {
                        stringResource(R.string.never_expires)
                    } else {
                        stringResource(R.string.seconds_short, entity.desiredLeaseDuration)
                    }
                )
            )
            pairs.add(Pair(stringResource(R.string.auto_renew), booleanText(entity.autoRenew)))
            pairs.add(Pair(stringResource(R.string.enabled), booleanText(entity.desiredEnabled)))
            pairs.add(Pair(stringResource(R.string.created), formatDate(entity.createdAtUtcMs)))
            pairs.add(Pair(stringResource(R.string.last_seen), formatDate(entity.lastSeenAtUtcMs)))
            pairs.add(
                Pair(
                    stringResource(R.string.status),
                    stringResource(
                        when (localRule.status) {
                            LocalRuleStatus.Missing -> R.string.status_inactive
                            LocalRuleStatus.Drifted -> R.string.status_inactive_drifted
                            LocalRuleStatus.SiblingActive -> R.string.status_inactive_sibling_active
                        }
                    )
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
                Text(stringResource(R.string.ok))
            }
        })
}

@Composable
private fun booleanText(value: Boolean): String {
    return stringResource(if (value) R.string.value_true else R.string.value_false)
}

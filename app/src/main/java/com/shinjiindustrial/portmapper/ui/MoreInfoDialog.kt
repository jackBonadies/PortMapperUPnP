package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shinjiindustrial.portmapper._getDefaultPortMapping
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors

@Preview
@Composable
fun MoreInfoDialog()
{
    SetupPreview()
    MoreInfoDialog(_getDefaultPortMapping(), remember { mutableStateOf(true) })
}

@Composable
fun MoreInfoDialog(portMappingWithPref : PortMappingWithPref, showDialog : MutableState<Boolean>)
{
    val portMapping = portMappingWithPref.portMapping
    if(showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Info") },
            text = {
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
                        Row()
                        {
                            Text(
                                p.first,
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 10.dp, 4.dp)
                                    .width(100.dp),
                                textAlign = TextAlign.Right,
                                color = AdditionalColors.TextColorWeak
                            )
                            Text(p.second, color = AdditionalColors.TextColorStrong)
                        }
                    }
                }

            },
            confirmButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text("OK")
                }
            })
    }
}

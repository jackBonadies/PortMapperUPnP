package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.domain.DeviceDetails
import com.shinjiindustrial.portmapper.domain.IIGDDevice

private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoBottomSheet(device: IIGDDevice, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 24.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        content = {
            DeviceInfoContent(device.deviceDetails)
        }
    )
}

@Composable
fun DeviceInfoContent(details: DeviceDetails) {
    val pairs = mutableListOf<Pair<String, String>>()
    pairs.add(Pair(stringResource(R.string.friendly_name), details.friendlyName.orDash()))
    pairs.add(Pair(stringResource(R.string.manufacturer), details.manufacturer.orDash()))
    pairs.add(Pair(stringResource(R.string.model_name), details.modelName.orDash()))
    pairs.add(Pair(stringResource(R.string.model_number), details.modelNumber.orDash()))
    pairs.add(Pair(stringResource(R.string.serial_number), details.serialNumber.orDash()))
    pairs.add(Pair(stringResource(R.string.upc), details.upc.orDash()))
    // i.e. InternetGatewayDevice:2 - this is the device type version, distinct from the spec version below
    pairs.add(
        Pair(
            stringResource(R.string.device_type),
            if (details.deviceType != null) "${details.deviceType}:${details.upnpVersion}"
            else details.upnpVersion.toString()
        )
    )
    pairs.add(Pair(stringResource(R.string.upnp_version), details.udaVersion.orDash()))
    pairs.add(Pair(stringResource(R.string.udn), details.udn.orDash()))
    pairs.add(Pair(stringResource(R.string.ip_address), details.ipAddress.orDash()))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.device_info),
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 0.dp, bottom = 6.dp)
        )
        for (p in pairs) {
            KeyValueRow(p.first, p.second, labelWidth = 130.dp)
        }
    }
}

@Preview
@Composable
fun PreviewDeviceInfoContent() {
    DeviceInfoContent(
        DeviceDetails(
            displayName = "Nokia IGD v2",
            ipAddress = "192.168.18.1",
            upnpVersion = 2,
            udn = "uuid:11111111-2222-3333-4444-555555555555",
            friendlyName = "Nokia Internet Gateway",
            manufacturer = "Nokia",
            modelName = "Nokia IGD",
            modelNumber = "A1-2000",
            serialNumber = null,
            upc = null,
            deviceType = "InternetGatewayDevice",
            udaVersion = "1.0",
        )
    )
}

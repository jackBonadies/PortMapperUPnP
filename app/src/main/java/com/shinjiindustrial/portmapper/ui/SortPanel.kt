package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.SharedPrefValues
import com.shinjiindustrial.portmapper.common.SortBy
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import java.com.shinjiindustrial.portmapper.PortViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetSortBy(portViewModel: PortViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "Sort By",
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = AdditionalColors.TextColorStrong,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        Column()
        {
            val numRow = 2
            val numCol = 3
            val sortInfo by portViewModel.sortInfo.collectAsStateWithLifecycle()
            val curIndex = sortInfo.sortBy.sortByValue

            for (i in 0 until numRow) {
                Row(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    //FilterField.values().forEach {
                    for (j in 0 until numCol) {
                        val index = i * numCol + j
                        //todo if index is out of range of sortby.values then produce an empty
                        SortSelectButton(
                            modifier = Modifier.weight(1.0f),
                            text = SortBy.from(index).getShortName(),//FilterField.from(i).name,
                            isSelected = index == curIndex,
                            onClick = {
                                portViewModel.updateSortingSortBy(SortBy.from(index))
                            })
                    }
                    //}
                }
            }

            Divider(modifier = Modifier.fillMaxWidth())

            val desc = sortInfo.sortDesc

            Row(modifier = Modifier.fillMaxWidth()) {
//                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                //FilterField.values().forEach {
                for (j in 0 until 2) {
                    val ascendingButton = j == 0
                    SortSelectButton(
                        modifier = Modifier.weight(1.0f),
                        text = if(ascendingButton) "Ascending" else "Descending",//FilterField.from(i).name,
                        isSelected = ascendingButton == !desc,
                        onClick = {
                            portViewModel.updateSortingDesc(!ascendingButton)
                        })
                }
            }
        }
    }
}
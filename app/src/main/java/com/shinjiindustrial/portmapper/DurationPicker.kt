package com.shinjiindustrial.portmapper

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


@Composable
@Preview
fun DurationPickerDialogPreview()
{
    SetupPreview()
    MyApplicationTheme() {
        var showDialog = remember { mutableStateOf(true) }
        var leaseDurationValueSeconds = remember { mutableStateOf("3661") }
        DurationPickerDialog(showDialog, leaseDurationValueSeconds)
    }
}

@Composable
fun DurationPickerDialog(showDialog : MutableState<Boolean>, leaseDurationValueSeconds : MutableState<String>)
{
    var chosenValue = remember { mutableStateOf("") }
    Dialog(onDismissRequest = { showDialog.value = false } ) {
        // Use Surface to apply elevation
        Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 24.dp) {
            Column(modifier = Modifier.padding(0.dp)) {
                Text("Duration", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(18.dp, 12.dp))

                Divider(modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth())

                Spacer(modifier = Modifier.height(10.dp))

                PickerRow(leaseDurationValueSeconds.value, chosenValue)

                Divider(modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(4.dp, 9.dp, 4.dp, 9.dp))
                    }

                    TextButton(onClick = {
                        showDialog.value = false
                        leaseDurationValueSeconds.value = chosenValue.value
                    }) {
                        Text("Set", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(4.dp, 9.dp, 4.dp, 9.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun rememberPickerState(initialValue : Int) : PickerState
{
    return remember { PickerState(initialValue) }
}

@Composable
fun PickerRow(initialSeconds : String, chosenValue : MutableState<String>) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        var initSeconds = if(initialSeconds.isBlank()) 0 else initialSeconds.replace(" (max)", "").toInt()
        var dhms = getDHMS(initSeconds)

        val infiniteScroll = true

        val dayValues = remember { getStringRange(0, 7, infiniteScroll) }
        val dayValuesPickerState = rememberPickerState(dhms.days)

        val hoursValues = remember { getStringRange(0, 23, infiniteScroll) }
        val hoursValuesPickerState = rememberPickerState(dhms.hours)

        val minsValues = remember { getStringRange(0, 59, infiniteScroll) }
        val minsValuesPickerState = rememberPickerState(dhms.mins)

        val secsValues = remember { getStringRange(0, 59, infiniteScroll) }
        val secsValuesPickerState = rememberPickerState(dhms.seconds)





        Row(modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)) {
            Picker(
                header = "Days",
                infinite = infiniteScroll,
                state = dayValuesPickerState,
                items = dayValues,
                visibleItemsCount = 3,
                modifier = Modifier.width(120.dp),
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 32.sp),
                startIndex = dhms.days,
            )
            Picker(
                header = "Hours",
                infinite = infiniteScroll,
                state = hoursValuesPickerState,
                items = hoursValues,
                visibleItemsCount = 3,
                modifier = Modifier.width(120.dp),
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 32.sp),
                startIndex = dhms.hours,
            )
            Picker(
                header = "Minutes",
                infinite = infiniteScroll,
                state = minsValuesPickerState,
                items = minsValues,
                visibleItemsCount = 3,
                modifier = Modifier.width(120.dp),
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 32.sp),
                startIndex = dhms.mins,
            )

            chosenValue.value = DayHourMinSec(dayValuesPickerState.selectedItem.toInt(),hoursValuesPickerState.selectedItem.toInt(), minsValuesPickerState.selectedItem.toInt(),0).totalSeconds().toString()
//                Picker(
//                    header = "Seconds",
//                    infinite = infiniteScroll,
//                    state = secsValuesPickerState,
//                    items = secsValues,
//                    visibleItemsCount = 3,
//                    modifier = Modifier.width(120.dp),
//                    textModifier = Modifier.padding(8.dp),
//                    textStyle = TextStyle(fontSize = 32.sp),
//                    startIndex = dhms.seconds,
//                )
        }

//            Text(
//                text = "Interval: ${dayValuesPickerState.selectedItem} ${hoursValuesPickerState.selectedItem}",
//                modifier = Modifier.padding(vertical = 16.dp)
//            )

    }
}


class PickerState(initialValue : Int) {
    var selectedItem by mutableStateOf(initialValue.toString())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.Picker(
    header: String,
    infinite: Boolean,
    items: List<String>,
    state: PickerState = rememberPickerState(0),
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    visibleItemsCount: Int = 3,
    textModifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    dividerColor: Color = AdditionalColors.TextColorWeak,
) {
    val visibleItemsMiddle = visibleItemsCount / 2
    val listScrollCount = remember { if(infinite) Integer.MAX_VALUE else (items.count() - 4) }//Integer.MAX_VALUE
    val listScrollMiddle = listScrollCount / 2
    val listStartIndex = listScrollMiddle - listScrollMiddle % items.size - visibleItemsMiddle + startIndex + 1 + (if(infinite) -1 else 0)

    fun getItem(index: Int) : String
    {
        if(infinite)
        {
            return items[index % items.size]
        }
        else
        {
            return if(index < -1) items[0] else items[index % items.size]
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightPixels = remember { mutableStateOf(0) }
    val itemHeightDp = pixelsToDp(itemHeightPixels.value)

    val fadingEdgeGradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to Color.Black,
            1f to Color.Transparent
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> getItem(index + visibleItemsMiddle) }
            .distinctUntilChanged()
            .collect { item -> state.selectedItem = item }
    }

    Column(modifier.weight(1f, true)) {

        Text(header,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(6.dp))

        Box(modifier = modifier) {

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp * visibleItemsCount)
                    .fadingEdge(fadingEdgeGradient)
            ) {
                items(listScrollCount) { index ->
                    Log.e("",index.toString())
                    var style = textStyle
                    //var color = Color.Black
                    if(state.selectedItem == getItem(index))
                    {
                        //color = Color.Blue
                    }
                    Text(
                        text = getItem(index),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        //color = color,
                        style = style,
                        modifier = Modifier
                            .onSizeChanged { size -> itemHeightPixels.value = size.height }
                            .then(textModifier)
                    )
                    style = MaterialTheme.typography.headlineLarge


                }
            }

            Divider(
                color = dividerColor,
                modifier = Modifier
                    .height(1.5.dp)
                    .offset(y = itemHeightDp * visibleItemsMiddle)
                    .padding(20.dp, 0.dp)
            )

            Divider(
                color = dividerColor,
                modifier = Modifier
                    .height(1.5.dp)
                    .offset(y = itemHeightDp * (visibleItemsMiddle + 1))
                    .padding(20.dp, 0.dp)
            )

        }
    }

}

private fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
private fun pixelsToDp(pixels: Int) = with(LocalDensity.current) { pixels.toDp() }

fun getStringRange(low : Int, high : Int, infinite: Boolean) : List<String>
{
    if(infinite)
    {
        return (low..high).map { it.toString() }
    }
    else
    {
        return ((low-1)..(high+5)).map { if(it < low || it > high) "" else it.toString() }
    }
}
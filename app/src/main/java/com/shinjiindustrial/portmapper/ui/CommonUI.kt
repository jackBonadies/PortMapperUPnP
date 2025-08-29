package com.shinjiindustrial.portmapper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircleCheckbox(selected: Boolean, enabled: Boolean = true, modifier : Modifier = Modifier, onChecked: () -> Unit) {

    val color = MaterialTheme.colorScheme
    val imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle
    val tint = if (selected) color.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f) // colorScheme.primary.copy(alpha=.8f)
    val background = if (selected) Color.White else Color.Transparent

    IconButton(onClick = {  },
        enabled = enabled, modifier = modifier.then(Modifier.size(28.dp))) {

        Icon(imageVector = imageVector, tint = tint,
            modifier = Modifier.background(background, shape = CircleShape).size(28.dp),
            contentDescription = "checkbox")
    }
}

@Preview
@Composable
fun CircleCheckboxPreview()
{
    val selected = remember { mutableStateOf(false)}
    CircleCheckbox(selected.value, true) { selected.value = !selected.value }
}

@ExperimentalMaterial3Api
@Composable
fun SortSelectButton(modifier : Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val buttonColor: Color
    val textColor: Color
    if (isSelected) {
        buttonColor = MaterialTheme.colorScheme.primary
        textColor = MaterialTheme.colorScheme.onPrimary
    } else {
        buttonColor = MaterialTheme.colorScheme.secondaryContainer
        textColor = MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = modifier.then(
            Modifier
                .height(60.dp)
                .padding(6.dp)),
        colors = CardDefaults.cardColors(containerColor = buttonColor),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Row()
//            {
//                Icon(Icons.Filled.Ascending)
            Text(
                modifier = Modifier.padding(2.dp),
                text = text,
                fontSize = 16.sp,
                fontStyle = MaterialTheme.typography.headlineMedium.fontStyle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
            )
            //}
        }
    }
}

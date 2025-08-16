package com.shinjiindustrial.portmapper.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

package com.shinjiIndustrial.portmanager

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shinjiIndustrial.portmanager.ui.theme.AdditionalColors
import com.shinjiIndustrial.portmanager.ui.theme.MyApplicationTheme

class LogViewActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this

        var logLines = PortForwardApplication.Logs.toString().split('\n')

        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = {
                                    this.finish()
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
//                                modifier = Modifier.height(40.dp),
                            colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondary),// change the height here
                            title = {
                                Text(
                                    text = "Settings",
                                    color = AdditionalColors.TextColorStrong,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            //actions = { OverflowMenu() }
                        )
                    },
                    content = { it ->

                        LazyColumn(
                            //modifier = Modifier.background(MaterialTheme.colorScheme.background),
                            modifier = Modifier
                                .background(AdditionalColors.Background)
                                .padding(it)
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),

                            ) {



                            itemsIndexed(logLines) { index, message ->

                                Text(message)

                            }
                        }
                    }
                )
            }
        }
    }
}
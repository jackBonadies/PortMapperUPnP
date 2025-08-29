package com.shinjiindustrial.portmapper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.com.shinjiindustrial.portmapper.SettingsViewModel
import java.com.shinjiindustrial.portmapper.ThemeUiState
import javax.inject.Inject

@AndroidEntryPoint
class LogViewActivity : ComponentActivity() {

    val settingsViewModel : SettingsViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this

        setContent {
            LogViewContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LogViewInner(themeUiState: ThemeUiState, logLines : List<String>, scrollToBottom : Boolean)
    {
        MyApplicationTheme(themeUiState) {
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
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AdditionalColors.TopAppBarColor),
                        title = {
                            Text(
                                text = "Logs",
                                color = AdditionalColors.TextColorStrong,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        actions = {

                            IconButton(onClick = {
                                PortForwardApplication.Logs.clear()
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                            }

                            IconButton(onClick = { CopyTextToClipboard(PortForwardApplication.Logs.joinToString("\n")) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy to Clipboard")
                            }

                            IconButton(onClick = { logsSaveAs() }) {
                                Icon(Icons.Default.SaveAs, contentDescription = "Save to File")
                            }
                        }
                    )
                },
                content = { it ->

                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()

                    if(scrollToBottom)
                    {
                        //scrollToBottom = true // not necessary?
                        // launched effect - action to take when composable is first launched / relaunched
                        //   passing in Unit ensures its only done the first time.
                        LaunchedEffect(Unit) {
                            listState.scrollToItem(logLines.size - 1)
                        }
                    }


                    LazyColumn( state = listState,
                        //modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(it)
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {

                        itemsIndexed(logLines) { index, message ->
                            val color = when
                            {
                                message.startsWith("W: ") -> AdditionalColors.LogWarningText
                                message.startsWith("E: ") -> AdditionalColors.LogErrorText
                                else -> AdditionalColors.TextColor
                            }
                            Text(message, color = color)

                        }

                    }
                }
            )
        }

    }

    @Composable
    fun LogViewContent()
    {
        val scrollToBottom = this.intent.getBooleanExtra(PortForwardApplication.ScrollToBottom, false);
        val logLines = PortForwardApplication.Logs
        val themeState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        LogViewInner(themeState, logLines, scrollToBottom)
    }

    private val CREATE_LOG_FILE = 1

    private fun logsSaveAs() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "portMapperLog.txt")
        }
        startActivityForResult(intent, CREATE_LOG_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_LOG_FILE && resultCode == Activity.RESULT_OK)
        {
            resultData?.data?.also { uri ->
                val contentResolver = applicationContext.contentResolver
                contentResolver.openOutputStream(uri)?.bufferedWriter().use { out ->
                    out?.write(PortForwardApplication.Logs.toString())
                }
            }
        }
    }


    fun CopyTextToClipboard(txt : String)
    {
        var clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;
        var clipData = ClipData.newPlainText("simple text", txt);
        clipboardManager.setPrimaryClip(clipData);
    }
}




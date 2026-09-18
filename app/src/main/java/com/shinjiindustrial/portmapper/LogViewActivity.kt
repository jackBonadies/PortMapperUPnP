package com.shinjiindustrial.portmapper

import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import com.shinjiindustrial.portmapper.ui.theme.PortMapperTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import java.util.logging.Level
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class LogViewActivity : ComponentActivity() {

    val settingsViewModel: SettingsViewModel by viewModels()
    val logViewModel: LogViewModel by viewModels()

    @Inject
    lateinit var logStoreRepository : LogStoreRepository

    @Inject
    lateinit var ourLogger : ILogger

    @Inject
    lateinit var snackbarManager : SnackbarManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LogViewContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LogViewInner(
        themeUiState: ThemeUiState,
        logLines: List<LogEntry>,
        scrollToBottom: Boolean,
        showTimestamps: Boolean,
        onToggleTimestamps: () -> Unit,
    ) {
        MyApplicationTheme(themeUiState) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = { OurSnackbarHost(snackbarManager) },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                this.finish()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = PortMapperTheme.componentColors.appBarContainer,
                            scrolledContainerColor = PortMapperTheme.componentColors.appBarScrolledContainer,
                        ),
                        scrollBehavior = scrollBehavior,
                        title = {
                            Text(
                                text = "Logs",
                                fontWeight = FontWeight.Normal
                            )
                        },
                        actions = {

                            IconButton(onClick = {
                                logStoreRepository.clear()
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                            }

                            IconButton(onClick = {
                                copyTextToClipboard(logStoreRepository.getLogsAsText())
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy to Clipboard"
                                )
                            }

                            IconButton(onClick = { logsSaveAs() }) {
                                Icon(Icons.Default.SaveAs, contentDescription = "Save to File")
                            }

                            LogViewOverflowMenu(
                                showTimestamps = showTimestamps,
                                onToggleTimestamps = onToggleTimestamps,
                            )
                        }
                    )
                },
                content = { it ->

                    val listState = rememberLazyListState()

                    if (scrollToBottom) {
                        //scrollToBottom = true // not necessary?
                        // launched effect - action to take when composable is first launched / relaunched
                        //   passing in Unit ensures its only done the first time.
                        if (logLines.isEmpty())
                        {
                            // I think this can happen on process getting recreated. // TODO is this an issue?
                            ourLogger.log(Level.SEVERE, "scrollToBottom is true but logLines is empty")
                        }
                        LaunchedEffect(Unit) {
                            listState.scrollToItem(max(logLines.size - 1, 0))
                        }
                    }

                    LazyColumn(
                        state = listState,
                        //modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(it)
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal=2.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {

                        items(logLines, key = { it.seq }) { entry ->
                            val color = when (entry.level) {
                                Level.WARNING -> PortMapperTheme.semanticColors.logWarning
                                Level.SEVERE -> PortMapperTheme.semanticColors.logError
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(LogFormatter.forView(entry, showTimestamps), color = color)
                        }

                    }
                }
            )
        }

    }

    @Composable
    fun LogViewContent() {
        val scrollToBottom =
            this.intent.getBooleanExtra(PortForwardApplication.ScrollToBottom, false)
        val logLines = logStoreRepository.entries
        val themeState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        val showTimestamps by logViewModel.showTimestamps.collectAsStateWithLifecycle()
        LogViewInner(
            themeUiState = themeState,
            logLines = logLines,
            scrollToBottom = scrollToBottom,
            showTimestamps = showTimestamps,
            onToggleTimestamps = logViewModel::toggleShowTimestamps,
        )
    }

    private val logSaveAsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            applicationScope.launch {
                result.data?.data?.also { uri ->
                    val contentResolver = applicationContext.contentResolver
                    contentResolver.openOutputStream(uri)?.bufferedWriter().use { out ->
                        out?.write(logStoreRepository.getLogsAsText())
                    }
                }
            }
        }
    }

    private fun logsSaveAs() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "portMapperLog.txt")
        }
        logSaveAsLauncher.launch(intent)
    }

    fun copyTextToClipboard(txt: String) {
        val clipboardManager = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("PortMapper Logs", txt)
        clipboardManager.setPrimaryClip(clipData)
    }
}

@Composable
private fun LogViewOverflowMenu(showTimestamps: Boolean, onToggleTimestamps: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val label = if (showTimestamps) {
                R.string.hide_timestamps_action
            } else {
                R.string.show_timestamps_action
            }
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_schedule),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onToggleTimestamps()
                }
            )
        }
    }
}

package com.shinjiindustrial.portmapper

import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        searchActive: Boolean,
        query: String,
        onQueryChange: (String) -> Unit,
        onOpenSearch: () -> Unit,
        onCloseSearch: () -> Unit,
    ) {
        // we search raw text, so if showTimestamps changes we recompute
        val filteredLines by remember(query, showTimestamps) {
            derivedStateOf {
                logLines.filter { matchesLogFilter(LogFormatter.forView(it, showTimestamps), query) }
            }
        }
        val visibleLines = if (query.isEmpty()) logLines else filteredLines

        BackHandler(enabled = searchActive) {
            onCloseSearch()
        }

        MyApplicationTheme(themeUiState) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                snackbarHost = { OurSnackbarHost(snackbarManager) },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                if (searchActive) {
                                    onCloseSearch()
                                } else {
                                    this.finish()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = PortMapperTheme.componentColors.appBarContainer,
                            scrolledContainerColor = PortMapperTheme.componentColors.appBarScrolledContainer,
                        ),
                        scrollBehavior = scrollBehavior,
                        title = {
                            if (searchActive) {
                                LogFilterField(query = query, onQueryChange = onQueryChange)
                            } else {
                                Text(
                                    text = stringResource(R.string.logs),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        },
                        actions = {

                            if (searchActive) {
                                IconButton(onClick = onCloseSearch) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_search_action))
                                }
                            } else {
                                IconButton(onClick = onOpenSearch) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_logs_action))
                                }

                                IconButton(onClick = {
                                    logStoreRepository.clear()
                                }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.clear_logs_action))
                                }

                                IconButton(onClick = {
                                    copyTextToClipboard(logStoreRepository.getLogsAsText())
                                }) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.copy_to_clipboard_action)
                                    )
                                }

                                IconButton(onClick = { logsSaveAs() }) {
                                    Icon(Icons.Default.SaveAs, contentDescription = stringResource(R.string.save_to_file_action))
                                }

                                LogViewOverflowMenu(
                                    showTimestamps = showTimestamps,
                                    onToggleTimestamps = onToggleTimestamps,
                                )
                            }
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

                    LaunchedEffect(query) {
                        if (query.isNotEmpty() && visibleLines.isNotEmpty()) {
                            listState.scrollToItem(visibleLines.lastIndex)
                        }
                    }

                    if (query.isNotEmpty() && visibleLines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .padding(it)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_matching_logs),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .padding(it)
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal=2.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {

                            items(visibleLines, key = { it.seq }) { entry ->
                                val color = when (entry.level) {
                                    Level.WARNING -> PortMapperTheme.semanticColors.logWarning
                                    Level.SEVERE -> PortMapperTheme.semanticColors.logError
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(LogFormatter.forView(entry, showTimestamps), color = color)
                            }

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
        var searchActive by rememberSaveable { mutableStateOf(false) }
        var query by rememberSaveable { mutableStateOf("") }
        LogViewInner(
            themeUiState = themeState,
            logLines = logLines,
            scrollToBottom = scrollToBottom,
            showTimestamps = showTimestamps,
            onToggleTimestamps = logViewModel::toggleShowTimestamps,
            searchActive = searchActive,
            query = query,
            onQueryChange = { query = it },
            onOpenSearch = { searchActive = true },
            onCloseSearch = {
                searchActive = false
                query = ""
            },
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
        val clipData = ClipData.newPlainText(getString(R.string.log_clip_label), txt)
        clipboardManager.setPrimaryClip(clipData)
    }
}

// Matches against the full rendered line, so "E: " finds errors
internal fun matchesLogFilter(text: String, query: String): Boolean {
    val ignoreCase = query.none { it.isUpperCase() }
    return text.contains(query, ignoreCase = ignoreCase)
}

@Composable
private fun LogFilterField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.search_logs_action)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun LogViewOverflowMenu(showTimestamps: Boolean, onToggleTimestamps: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options_action))
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

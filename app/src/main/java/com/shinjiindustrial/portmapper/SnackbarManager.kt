package com.shinjiindustrial.portmapper

import android.content.Intent
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.logging.Level
import javax.inject.Inject
import javax.inject.Singleton


suspend fun showSnackBar(
    snackbarHostState : SnackbarHostState,
    message: String,
    action: String?,
    duration: SnackbarDuration,
    onAction: () -> Unit = { }
) {
    val snackbarResult = snackbarHostState.showSnackbar(
        message,
        action,
        (duration == SnackbarDuration.Indefinite),
        duration
    )
    when (snackbarResult) {
        SnackbarResult.Dismissed -> {}
        SnackbarResult.ActionPerformed -> onAction()
    }
}

fun viewLogCallback(context : Context) {
    val intent =
        Intent(context, LogViewActivity::class.java)
    intent.putExtra(PortForwardApplication.ScrollToBottom, true)
    context.startActivity(intent)
}


suspend fun showSnackBarShortNoAction(snackbarHostState : SnackbarHostState, message: String) {
    showSnackBar(snackbarHostState, message, null, SnackbarDuration.Short)
}

suspend fun showSnackBarLongNoAction(snackbarHostState : SnackbarHostState, message: String) {
    showSnackBar(snackbarHostState, message, null, SnackbarDuration.Long)
}

suspend fun showSnackBarViewLog(snackbarHostState : SnackbarHostState, context : Context, message: String) {
    showSnackBar(snackbarHostState, message, "View Log", SnackbarDuration.Long) {
        viewLogCallback(
            context
        )
    }
}

suspend fun subscribeToSnackbarEvents(snackbarManager: SnackbarManager, snackbarHostState: SnackbarHostState)
{

}

@Composable
fun SubscribeToSnackbarManager(hostState: SnackbarHostState, snackbarManager: SnackbarManager) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        snackbarManager.events.collect { e ->
            when(e) {
                is UiSnackToastEvent.ToastEvent -> {
                    Toast.makeText(
                        context,
                        e.msg,
                        e.duration
                    ).show()
                }
//                is UiSnackToastEvent.SnackbarEvent -> {
//                    showSnackBar(hostState, e.message, e.actionLabel, e.duration, e.onAction)
//                }
                is UiSnackToastEvent.SnackBarViewShortNoEvent -> {
                    showSnackBarShortNoAction(hostState, e.msg)
                }
                is UiSnackToastEvent.SnackBarLongNoAction -> {
                    showSnackBarLongNoAction(hostState, e.msg)
                }
                is UiSnackToastEvent.SnackBarViewLogEvent -> {
                    showSnackBarViewLog(hostState, context, e.msg)
                }
            }
        }
    }
}

@Composable
fun OurSnackbarHost(snackbarManager : SnackbarManager)
{
    val hostState = remember { SnackbarHostState() }
    SubscribeToSnackbarManager(hostState, snackbarManager)
    SnackbarHost(hostState) { data ->
        Snackbar(
            data,
            actionColor = AdditionalColors.PrimaryDarkerBlue
            // according to https://m2.material.io/design/color/dark-theme.html
            // light snackbar in darkmode is good.
        )
    }
}

@Composable
fun SnackbarScaffold(
    snackbarManager: SnackbarManager,
    content: @Composable (PaddingValues) -> Unit,
) {

    Scaffold(
        snackbarHost = { OurSnackbarHost(snackbarManager) }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Singleton
class SnackbarManager @Inject constructor(
    private val ourLogger: ILogger
) {
    private val _events = MutableSharedFlow<UiSnackToastEvent>(
        extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiSnackToastEvent> = _events

    fun show(event: UiSnackToastEvent) {
        if(!_events.tryEmit(event))
        {
            ourLogger.log(Level.SEVERE, "flow is DROP_OLDEST, but tryEmit returned false. This is unexpected.")
        }
    }
}

sealed interface UiSnackToastEvent {
    data class ToastEvent(val msg: String, val duration: Int = Toast.LENGTH_SHORT) : UiSnackToastEvent
    data class SnackBarViewLogEvent(val msg: String) : UiSnackToastEvent
    data class SnackBarViewShortNoEvent(val msg: String) : UiSnackToastEvent
    data class SnackBarLongNoAction(val msg: String) : UiSnackToastEvent
    data class SnackbarEvent(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val onAction: (() -> Unit)? = null
    )
}

package java.com.shinjiindustrial.portmapper

import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.UpnpManager.Companion.CreatePortMappingRulesEntry
import com.shinjiindustrial.portmapper.UpnpManager.Companion.DeletePortMappingEntry
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Level
import javax.inject.Inject

@HiltViewModel
class PortViewModel @Inject constructor(
    //private val repo: UpnpRespository
) : ViewModel() {

    sealed interface UiEvent {
        data class ToastEvent(val msg: String, val duration: Int = Toast.LENGTH_SHORT) : UiEvent
        data class SnackBarViewLogEvent(val msg: String) : UiEvent
        data class SnackBarViewShortNoEvent(val msg: String) : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    private val applicationScope : CoroutineScope = MainScope()

    fun fullRefresh()
    {
        UpnpManager.FullRefresh()
    }

    fun enableDisable(
        portMapping: PortMapping,
        enable: Boolean) =
        applicationScope.launch {
            try {
                val res = UpnpManager.DisableEnablePortMappingEntry(portMapping, enable)
                if (res is UPnPCreateMappingWrapperResult.Success) {
                    _events.emit(UiEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                }
                else
                {
                    _events.emit(UiEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).reason}"))
                }
            } catch (e: Exception) {
                val enableDisableString = if(enable) "Enable" else "Disable"
                _events.emit(UiEvent.SnackBarViewLogEvent("$enableDisableString Port Mapping Failed"))
            }
        }

    fun enableDisableAll(enable : Boolean, chosenRulesOnly : List<PortMapping>? = null) =
        applicationScope.launch {

            try {
                val result = when {
                    chosenRulesOnly != null -> {
                        val rules = chosenRulesOnly.filter { it -> it.Enabled != enable }
                        UpnpManager.DisableEnablePortMappingEntries(rules, enable)
                    }
                    else -> {
                        val rules = UpnpManager.GetEnabledDisabledRules(!enable)
                        UpnpManager.DisableEnablePortMappingEntries(rules, enable)
                    }
                }

                result.forEach { res ->
                    when (res) {
                        is UPnPCreateMappingWrapperResult.Success -> {
                            print("success")
                            print(res.requestInfo.Description)
                        }

                        is UPnPCreateMappingWrapperResult.Failure -> {
                            print("failure")
                            print(res.reason)
                            print(res.response)
                        }
                    }
                }

                val anyFailed = result.any { it is UPnPCreateMappingWrapperResult.Failure }

                if(anyFailed) {
                    val res = result.first { it is UPnPCreateMappingWrapperResult.Failure }
                    _events.emit(UiEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).reason}"))
                }
                else
                {
                    _events.emit(UiEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                }
            } catch(e : Exception)
            {
                val enableDisableString = if(enable) "Enable" else "Disable"
                _events.emit(UiEvent.SnackBarViewLogEvent("$enableDisableString Port Mappings Failed"))
            }
    }

    fun deleteAll(chosen: List<PortMapping>? = null) = applicationScope.launch {
        try {
            // get all enabled. note: need to clone.
            val rules = chosen?.toList() ?: UpnpManager.GetAllRules()
            val result = UpnpManager.DeletePortMappingsEntry(rules)
            result.forEach { res ->
                when (res) {
                    is UPnPResult.Success -> {
                        print("success")
                        print(res.requestInfo.Description)
                    }

                    is UPnPResult.Failure -> {
                        print("failure")
                        print(res.reason)
                        print(res.response)
                    }
                }
            }

                val anyFailed = result.any { it is UPnPResult.Failure }

                if(anyFailed) {
                    val res = result.first { it is UPnPResult.Failure }
                    _events.emit(UiEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).reason}"))
                }
                else
                {
                    _events.emit(UiEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                }
        } catch (e : Exception) {
            _events.emit(UiEvent.SnackBarViewLogEvent("Delete Port Mappings Failed"))
        }
    }

    fun delete(portMapping: PortMapping) = applicationScope.launch {
        try {
            val res = UpnpManager.DeletePortMappingEntry(portMapping)
            if (res is UPnPResult.Success) {
                _events.emit(UiEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            } else {
                _events.emit(UiEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).reason}"))
            }
    } catch (e : Exception) {
        _events.emit(UiEvent.SnackBarViewLogEvent("Delete Port Mapping Failed"))
    }
    }

    fun editRule(oldRule : PortMapping, portMappingRequestInput : PortMappingUserInput) = applicationScope.launch {
        try {
            val res = DeletePortMappingEntry(oldRule)
            if (res is UPnPResult.Failure) {
                _events.emit(UiEvent.SnackBarViewLogEvent("Failed to modify entry."))
                return@launch
            }
        } catch (exception: Exception) {
            PortForwardApplication.OurLogger.log(
                Level.SEVERE,
                "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
            )
            _events.emit(UiEvent.SnackBarViewLogEvent("Failed to modify entry."))
            return@launch
        }
        // delete was successful, create new rules
        createRules(portMappingRequestInput, true)
    }

    fun createRules(portMappingUserInput: PortMappingUserInput, modifyCase : Boolean = false) = applicationScope.launch {
        try {
            val result = CreatePortMappingRulesEntry(portMappingUserInput)
            result.forEach { res ->
                when (res) {
                    is UPnPCreateMappingWrapperResult.Success -> {
                        print("success")
                        print(res.requestInfo.Description)
                    }

                    is UPnPCreateMappingWrapperResult.Failure -> {
                        print("failure")
                        print(res.reason)
                        print(res.response)
                    }
                }

            }

            val numFailed = result.count { it is UPnPCreateMappingWrapperResult.Failure }

            val anyFailed = numFailed > 0

            if (anyFailed) {

                val verbString = if(modifyCase) "modify" else "create"

                // all failed
                if (numFailed == result.size) {
                    if (result.size == 1) {
                        _events.emit(UiEvent.SnackBarViewLogEvent("Failed to $verbString rule."))
                    } else {
                        _events.emit(UiEvent.SnackBarViewLogEvent("Failed to $verbString rules."))
                    }
                } else {
                    _events.emit(UiEvent.SnackBarViewLogEvent("Failed to $verbString some rules."))
                }
            } else {
                _events.emit(UiEvent.SnackBarViewShortNoEvent("Success"))
            }
        } catch (exception: Exception) {
            PortForwardApplication.OurLogger.log(
                Level.SEVERE,
                "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
            )
            _events.emit(UiEvent.SnackBarViewLogEvent("Failed to modify entry."))
            return@launch
        }
    }
}

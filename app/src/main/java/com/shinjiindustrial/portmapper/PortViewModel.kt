package java.com.shinjiindustrial.portmapper

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.UpnpManager
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.domain.IGDDevice
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.UPnPViewElement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.logging.Level
import javax.inject.Inject

data class PortUiState(
    val items: List<UPnPViewElement> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: Int? = null
)

@HiltViewModel
class PortViewModel @Inject constructor(
    private val upnpRepository: UpnpManager
) : ViewModel() {

    sealed interface UiEvent {
        data class ToastEvent(val msg: String, val duration: Int = Toast.LENGTH_SHORT) : UiEvent
        data class SnackBarViewLogEvent(val msg: String) : UiEvent
        data class SnackBarViewShortNoEvent(val msg: String) : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    val searchStartedRecently: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val applicationScope : CoroutineScope = MainScope()

    val anyDevices: StateFlow<Boolean> = upnpRepository.devices.map { devices -> devices.isNotEmpty() }.stateIn( scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val searchStartedRecentlyAndNothingFoundYet: StateFlow<Boolean> = combine(upnpRepository.devices, searchStartedRecently)
    {
        devices, searchStartedRecently ->
        devices.isEmpty() && searchStartedRecently
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val uiState: StateFlow<PortUiState> = combine( // !! todo
        upnpRepository.devices, upnpRepository.portMappings
    ) { devices, portMappings ->


        val upnpElements = mutableListOf<UPnPViewElement>()
        if (devices.isEmpty())
        {
        }
        else
        {
            var index = 0
            val curDevice : IIGDDevice? = devices.elementAt(0)
            val portMappingsList = portMappings.toList()
            for (curDevice in devices)
            {
                // do we have any port mappings
                if (index >= portMappingsList.size || portMappingsList.elementAt(index).ActualExternalIP != curDevice.getIpAddress())
                {
                    // emit empty and continue
                    upnpElements.add(UPnPViewElement(curDevice, false))
                    upnpElements.add(UPnPViewElement(curDevice, true))
                    continue
                }
                upnpElements.add(UPnPViewElement(curDevice))
                while (index < portMappingsList.size)
                {
                    if (portMappingsList.elementAt(index).ActualExternalIP == curDevice.getIpAddress())
                    {
                        upnpElements.add(UPnPViewElement(portMappingsList.elementAt(index)))
                    }
                    index++
                }
            }
        }

        PortUiState(upnpElements)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortUiState(isLoading = true)
        )

    fun initialize(context : Context, force : Boolean)
    {
        upnpRepository.Initialize(context, force)
    }

    fun getExistingRuleInfos(): Pair<Boolean, Boolean> {
        return upnpRepository.GetExistingRuleInfos()
    }

    fun getInterfacesUsedInSearch(): MutableList<NetworkInterfaceInfo> {
        return upnpRepository.GetUPnPClient().getInterfacesUsedInSearch()
    }

    fun isInitialized(): Boolean {
        return upnpRepository.GetUPnPClient().isInitialized()
    }

    fun updateSorting() {
        upnpRepository.UpdateSorting()
    }

    fun getIGDDevice(ipAddress: String): IIGDDevice {
        return upnpRepository.getIGDDevice(ipAddress)
    }

    fun GetGatewayIpsWithDefault(deviceGateway: String): Pair<MutableList<String>, String> {
        return upnpRepository.GetGatewayIpsWithDefault(deviceGateway)
    }

    fun fullRefresh()
    {
        upnpRepository.FullRefresh()
    }

    fun devices(): StateFlow<List<IIGDDevice>> {
        return upnpRepository.devices
    }

    fun renew( portMapping: PortMapping ) = applicationScope.launch {
            try {
                val res = upnpRepository.RenewRule(portMapping)
                if (res is UPnPCreateMappingWrapperResult.Success) {
                    _events.emit(UiEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                }
                else
                {
                    _events.emit(UiEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).reason}"))
                }
            } catch (e: Exception) {
                _events.emit(UiEvent.SnackBarViewLogEvent("Renew Port Mapping Failed"))
            }
        }

    fun renewAll(chosen: List<PortMapping>? = null) = applicationScope.launch {
        try {
            val portMappings = chosen?.toList() ?: upnpRepository.GetAllRules()
            val result = upnpRepository.RenewRules(portMappings)
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
        } catch (e: Exception) {
            _events.emit(UiEvent.SnackBarViewLogEvent("Renew Port Mapping Failed"))
        }
    }

    fun enableDisable(
        portMapping: PortMapping,
        enable: Boolean) =
        applicationScope.launch {
            try {
                val res = upnpRepository.DisableEnablePortMappingEntry(portMapping, enable)
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
                        upnpRepository.DisableEnablePortMappingEntries(rules, enable)
                    }
                    else -> {
                        val rules = upnpRepository.GetEnabledDisabledRules(!enable)
                        upnpRepository.DisableEnablePortMappingEntries(rules, enable)
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



    fun start()
    {
        if (upnpRepository.FailedToInitialize) {
            searchStartedRecently.value = false
        } else {
            searchStartedRecently.value = !upnpRepository.HasSearched
            upnpRepository.Search(true) // by default STAll
        }
    }

    init {
        upnpRepository.SearchStarted += { o -> searchStarted(o) }
    }

    override fun onCleared() {
        upnpRepository.SearchStarted -= { o -> searchStarted(o) }
        super.onCleared()
    }

    // move to viewmodel
    var searchInProgressJob: Job? = null
    fun searchStarted(o: Any?) {
        searchStartedRecently.value = true // controls when loading bar is there
        searchInProgressJob?.cancel() // cancel old search timer
        if (searchStartedRecently.value) {
            searchInProgressJob = viewModelScope.launch {
                delay(6000)
                searchStartedRecently.value = false
            }
        }
    }

    fun deleteAll(chosen: List<PortMapping>? = null) = applicationScope.launch {
        try {
            // get all enabled. note: need to clone.
            val rules = chosen?.toList() ?: upnpRepository.GetAllRules()
            val result = upnpRepository.DeletePortMappingsEntry(rules)
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
            val res = upnpRepository.DeletePortMappingEntry(portMapping)
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
            val res = upnpRepository.DeletePortMappingEntry(oldRule)
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
            val result = upnpRepository.CreatePortMappingRulesEntry(portMappingUserInput)
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

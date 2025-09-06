package com.shinjiindustrial.portmapper

import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinjiindustrial.portmapper.client.UPnPCreateMappingWrapperResult
import com.shinjiindustrial.portmapper.client.UPnPResult
import com.shinjiindustrial.portmapper.common.SortBy
import com.shinjiindustrial.portmapper.common.SortInfo
import com.shinjiindustrial.portmapper.domain.DeviceStatus
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.domain.UpnpViewRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.logging.Level
import javax.inject.Inject

data class PortUiState(
    val items: List<UpnpViewRow> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: Int? = null
)

data class ContextMenuUiState(
    val selectedId: PortMappingKey? = null)
{
    fun isOpen() : Boolean
    {
        return selectedId != null
    }
}

@HiltViewModel
class PortViewModel @Inject constructor(
    private val upnpRepository: UpnpRepository,
    private val preferencesRepository: PreferencesManager,
    private val savedStateHandle: SavedStateHandle,
    val ourLogger: ILogger,
    val snackbarManager : SnackbarManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _events = MutableSharedFlow<UiSnackToastEvent>()
    val events: SharedFlow<UiSnackToastEvent> = _events

    val searchStartedRecently: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _contextMenuUiState = MutableStateFlow(ContextMenuUiState())
    val contextMenuUiState = _contextMenuUiState.asStateFlow()

    fun openContextMenu(id: PortMappingKey) {
        _contextMenuUiState.update { cur -> ContextMenuUiState(id) }
    }

    fun closeContextMenu() {
        _contextMenuUiState.update { cur -> ContextMenuUiState(null) }
    }

    // we want to use key for selections.  so if a rule renews while the user is in multi select
    //   mode, don't deselect that rule.  but if we lose a rule (i.e. it gets deleted) then
    //   we still want to deselect.
    private val _selectedIds = MutableStateFlow<Set<PortMappingKey>>(
        savedStateHandle["selected_ids"] ?: emptySet()
    )
    val selectedIds: StateFlow<Set<PortMappingKey>> = _selectedIds

    val inMultiSelectMode: StateFlow<Boolean> =
        _selectedIds.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggle(id: PortMappingKey) {
        _selectedIds.update { s -> if (id in s) s - id else s + id }
    }

    fun getSelectedItems(selectedIds: Set<PortMappingKey>): List<PortMappingWithPref> {
        return upnpRepository.portMappingsFromIds(selectedIds)
    }

    fun getSelectedItem(selectedId: PortMappingKey): PortMappingWithPref {
        val listOfMappings = upnpRepository.portMappingsFromIds(setOf(selectedId))
        return listOfMappings[0]
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    val anyDevices: StateFlow<Boolean> =
        upnpRepository.devices.map { devices -> devices.isNotEmpty() }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val searchStartedRecentlyAndNothingFoundYet: StateFlow<Boolean> =
        combine(upnpRepository.devices, searchStartedRecently)
        { devices, searchStartedRecently ->
            devices.isEmpty() && searchStartedRecently
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val sortInfo: StateFlow<SortInfo> = preferencesRepository.sortInfo.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SortInfo(SortBy.ExternalPort, false)
    )

    val uiState: StateFlow<PortUiState> = combine(
        sortInfo,
        upnpRepository.devices,
        upnpRepository.portMappings
    ) { sortInfo, devices, portMappings ->

        val upnpElements = mutableListOf<UpnpViewRow>()
        if (!devices.isEmpty()) {
            val portMappingsList =
                portMappings.values.sortedWith(sortInfo.sortBy.getComparer(ascending = !sortInfo.sortDesc))
            for (curDevice in devices) {
                upnpElements.add(UpnpViewRow.DeviceHeaderViewRow(curDevice))
                var anyFound = false
                for (portMapping in portMappingsList) {
                    if (curDevice.getIpAddress() == portMapping.portMapping.DeviceIP)
                    {
                        upnpElements.add(UpnpViewRow.PortViewRow(portMapping))
                        anyFound = true
                    }
                }
                if (!anyFound && curDevice.status == DeviceStatus.FinishedEnumeratingMappings)
                {
                    upnpElements.add(UpnpViewRow.DeviceEmptyViewRow(curDevice))
                }
            }
        }

        PortUiState(upnpElements)
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortUiState(isLoading = true)
        )

    fun initialize(force: Boolean) {
        upnpRepository.initialize(force)
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

    fun updateSortingDesc(sortDesc: Boolean) = viewModelScope.launch {
        preferencesRepository.updateSortDesc(sortDesc)
    }

    fun updateSortingSortBy(sortBy: SortBy) = viewModelScope.launch {
        preferencesRepository.updateSortBy(sortBy)
    }

    fun getIGDDevice(ipAddress: String): IIGDDevice {
        return upnpRepository.getIGDDevice(ipAddress)
    }

    fun getGatewayIpsWithDefault(deviceGateway: String): Pair<MutableList<String>, String> {
        return upnpRepository.getGatewayIpsWithDefault(deviceGateway)
    }

    fun fullRefresh() {
        upnpRepository.fullRefresh()
    }

    fun renew(portMapping: PortMappingWithPref) = applicationScope.launch {
        try {
            val res = upnpRepository.renewRule(portMapping)
            if (res is UPnPCreateMappingWrapperResult.Success) {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            } else {
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Renew Port Mapping Failed"))
        }
    }

    fun renewAll(chosen: List<PortMappingWithPref>? = null) = applicationScope.launch {
        try {
            val portMappings = chosen?.toList() ?: upnpRepository.getAllRules()
            val result = upnpRepository.renewRules(portMappings)
            result.forEach { res ->
                when (res) {
                    is UPnPCreateMappingWrapperResult.Success -> {
                        print("success")
                        print(res.requestInfo.Description)
                    }

                    is UPnPCreateMappingWrapperResult.Failure -> {
                        print("failure")
                        print(res.details.reason)
                        print(res.details.response)
                    }
                }
            }

            val anyFailed = result.any { it is UPnPCreateMappingWrapperResult.Failure }

            if (anyFailed) {
                val res = result.first { it is UPnPCreateMappingWrapperResult.Failure }
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
            } else {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Renew Port Mapping Failed"))
        }
    }

    fun enableDisable(
        portMapping: PortMappingWithPref,
        enable: Boolean
    ) =
        applicationScope.launch {
            try {
                val res = upnpRepository.disableEnablePortMappingEntry(portMapping, enable)
                if (res is UPnPCreateMappingWrapperResult.Success) {
                    snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                } else {
                    snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
                }
            } catch (e: Exception) {
                val enableDisableString = if (enable) "Enable" else "Disable"
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("$enableDisableString Port Mapping Failed"))
            }
        }

    fun enableDisableAll(enable: Boolean, selectedIds: Set<PortMappingKey>) {
        enableDisableAll(enable, upnpRepository.portMappingsFromIds(selectedIds))
    }

    fun enableDisableAll(enable: Boolean, chosenRulesOnly: List<PortMappingWithPref>? = null) =
        applicationScope.launch {

            try {
                val result = when {
                    chosenRulesOnly != null -> {
                        val rules =
                            chosenRulesOnly.filter { it -> it.portMapping.Enabled != enable }
                        upnpRepository.disableEnablePortMappingEntries(rules, enable)
                    }

                    else -> {
                        val rules = upnpRepository.getEnabledDisabledRules(!enable)
                        upnpRepository.disableEnablePortMappingEntries(rules, enable)
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
                            print(res.details.reason)
                            print(res.details.response)
                        }
                    }
                }

                val anyFailed = result.any { it is UPnPCreateMappingWrapperResult.Failure }

                if (anyFailed) {
                    val res = result.first { it is UPnPCreateMappingWrapperResult.Failure }
                    snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
                } else {
                    snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                }
            } catch (e: Exception) {
                val enableDisableString = if (enable) "Enable" else "Disable"
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("$enableDisableString Port Mappings Failed"))
            }
        }


    fun start() {
        if (upnpRepository.failedToInitialize) {
            searchStartedRecently.value = false
        } else {
            searchStartedRecently.value = !upnpRepository.hasSearched
            upnpRepository.search(true) // by default STAll
        }
    }

    fun tickerFlow(
        periodMillis: Long,
        initialDelayMillis: Long = 0L
    ): Flow<Unit> = flow {
        if (initialDelayMillis > 0) delay(initialDelayMillis)
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(periodMillis)
        }
    }

    // Example usage (in a ViewModel):
    val data: StateFlow<Unit> =
        tickerFlow(periodMillis = 60_000L)       // ticks every 60s
            .onStart { emit(Unit) }               // run immediately on subscribe
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Unit
            )

    private val onSearchStarted: (Any?) -> Unit = { o -> searchStarted(o) }

    init {
        upnpRepository.SearchStarted += onSearchStarted
        combine(_selectedIds, upnpRepository.portMappings) { selectedIds, currentMappings ->
            val cur = currentMappings.keys
            val sel = selectedIds
            // map to the intersection of the two sets, in most cases this should be the same.
            //   if a rule got deleted then it will be different. in which case we update selectedIds
            //   to be the new pruned set.
            sel intersect cur
        }
            .distinctUntilChanged()
            .onEach { filtered ->
                println("filtered changed ...")
                if (filtered != _selectedIds.value) _selectedIds.value = filtered
                savedStateHandle["selected_ids"] = filtered
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        upnpRepository.SearchStarted -= onSearchStarted
        super.onCleared()
    }


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

    fun deleteAll(selectedIds: Set<PortMappingKey>) {
        deleteAll(upnpRepository.portMappingsFromIds(selectedIds))
    }

    fun deleteAll(chosen: List<PortMappingWithPref>? = null) = applicationScope.launch {
        try {
            // get all enabled. note: need to clone.
            val rules = chosen?.toList() ?: upnpRepository.getAllRules()
            val result = upnpRepository.deletePortMappingsEntry(rules)
            result.forEach { res ->
                when (res) {
                    is UPnPResult.Success -> {
                        print("success")
                        print(res.requestInfo.Description)
                    }

                    is UPnPResult.Failure -> {
                        print("failure")
                        print(res.details.reason)
                        print(res.details.response)
                    }
                }
            }

            val anyFailed = result.any { it is UPnPResult.Failure }

            if (anyFailed) {
                val res = result.first { it is UPnPResult.Failure }
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).details.reason}"))
            } else {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Delete Port Mappings Failed"))
        }
    }

    fun delete(portMapping: PortMappingWithPref) = applicationScope.launch {
        try {
            val res = upnpRepository.deletePortMappingEntry(portMapping)
            if (res is UPnPResult.Success) {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            } else {
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).details.reason}"))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Delete Port Mapping Failed"))
        }
    }

    fun editRule(oldRule: PortMappingWithPref, portMappingRequestInput: PortMappingUserInput) =
        applicationScope.launch {
            try {
                val res = upnpRepository.deletePortMappingEntry(oldRule)
                if (res is UPnPResult.Failure) {
                    snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to modify entry."))
                    return@launch
                }
            } catch (exception: Exception) {
                ourLogger.log(
                    Level.SEVERE,
                    "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to modify entry."))
                return@launch
            }
            // delete was successful, create new rules
            createRules(portMappingRequestInput, true)
        }

    fun createRules(portMappingUserInput: PortMappingUserInput, modifyCase: Boolean = false) =
        applicationScope.launch {
            try {
                val result = upnpRepository.createPortMappingRulesEntry(portMappingUserInput)
                result.forEach { res ->
                    when (res) {
                        is UPnPCreateMappingWrapperResult.Success -> {
                            print("success")
                            print(res.requestInfo.Description)
                        }

                        is UPnPCreateMappingWrapperResult.Failure -> {
                            print("failure")
                            print(res.details.reason)
                            print(res.details.response)
                        }
                    }

                }

                val numFailed = result.count { it is UPnPCreateMappingWrapperResult.Failure }

                val anyFailed = numFailed > 0

                if (anyFailed) {

                    val verbString = if (modifyCase) "modify" else "create"

                    // all failed
                    if (numFailed == result.size) {
                        if (result.size == 1) {
                            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to $verbString rule."))
                        } else {
                            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to $verbString rules."))
                        }
                    } else {
                        snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to $verbString some rules."))
                    }
                } else {
                    snackbarManager.show(UiSnackToastEvent.SnackBarViewShortNoEvent("Success"))
                }
            } catch (exception: Exception) {
                ourLogger.log(
                    Level.SEVERE,
                    "Delete Original Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to modify entry."))
                return@launch
            }
        }
}

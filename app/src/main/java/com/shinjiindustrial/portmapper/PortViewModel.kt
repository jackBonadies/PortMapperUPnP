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
import com.shinjiindustrial.portmapper.domain.LocalRule
import com.shinjiindustrial.portmapper.domain.LocalRuleKey
import com.shinjiindustrial.portmapper.domain.NetworkInterfaceInfo
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.domain.RuleSection
import com.shinjiindustrial.portmapper.domain.UpnpViewRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

// mutually exclusive
data class ContextMenuUiState(
    val selectedId: PortMappingKey? = null,
    val selectedLocalId: LocalRuleKey? = null)
{
    fun isOpen() : Boolean
    {
        return selectedId != null
    }

    fun isLocalOpen() : Boolean
    {
        return selectedLocalId != null
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
        _contextMenuUiState.update { cur -> ContextMenuUiState(selectedId = id) }
    }

    fun openLocalContextMenu(id: LocalRuleKey) {
        _contextMenuUiState.update { cur -> ContextMenuUiState(selectedLocalId = id) }
    }

    fun closeContextMenu() {
        _contextMenuUiState.update { cur -> ContextMenuUiState() }
    }

    // we want to use key for selections.  so if a rule renews while the user is in multi select
    //   mode, don't deselect that rule.  but if we lose a rule (i.e. it gets deleted) then
    //   we still want to deselect.
    private val _selectedIds = MutableStateFlow<Set<PortMappingKey>>(savedStateHandle.get<List<PortMappingKey>>("selected_ids")?.toSet() ?: emptySet())
    val selectedIds: StateFlow<Set<PortMappingKey>> = _selectedIds

    // local rules are a separate set with their own key type
    private val _selectedLocalIds = MutableStateFlow<Set<LocalRuleKey>>(savedStateHandle.get<List<LocalRuleKey>>("selected_local_ids")?.toSet() ?: emptySet())
    val selectedLocalIds: StateFlow<Set<LocalRuleKey>> = _selectedLocalIds

    val inMultiSelectMode: StateFlow<Boolean> =
        combine(_selectedIds, _selectedLocalIds) { router, local ->
            router.isNotEmpty() || local.isNotEmpty()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggle(id: PortMappingKey) {
        _selectedIds.update { s -> if (id in s) s - id else s + id }
    }

    fun toggleLocal(id: LocalRuleKey) {
        _selectedLocalIds.update { s -> if (id in s) s - id else s + id }
    }

    fun getSelectedItems(selectedIds: Set<PortMappingKey>): List<PortMappingWithPref> {
        return upnpRepository.portMappingsFromIds(selectedIds)
    }

    fun getSelectedLocalRules(selectedLocalIds: Set<LocalRuleKey>): List<LocalRule> {
        return upnpRepository.localRulesFromIds(selectedLocalIds)
    }

    fun getSelectedItem(selectedId: PortMappingKey): PortMappingWithPref {
        val listOfMappings = upnpRepository.portMappingsFromIds(setOf(selectedId))
        return listOfMappings[0]
    }

    // can be null since a re-enumeration or background activate can remove the rule from local
    fun getSelectedLocalRule(selectedId: LocalRuleKey): LocalRule? {
        return upnpRepository.localRules.value[selectedId]
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectedLocalIds.value = emptySet()
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
            // this is cheap to compute and SharingState.WhileSubscribed(5000) had bug where combine
            //   did not get ran even after updating sharedStartedRecently for the first time.
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val sortInfo: StateFlow<SortInfo> = preferencesRepository.sortInfo.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SortInfo(SortBy.ExternalPort, false)
    )

    // We will always have the real value before the first frame
    val showEnableDisable: StateFlow<Boolean> = preferencesRepository.showEnableDisable.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    val uiState: StateFlow<PortUiState> = combine(
        sortInfo,
        upnpRepository.devices,
        upnpRepository.portMappings,
        upnpRepository.localRules
    ) { sortInfo, devices, portMappings, localRules ->

        val upnpElements = mutableListOf<UpnpViewRow>()
        if (!devices.isEmpty()) {
            val comparer = sortInfo.sortBy.getComparer(ascending = !sortInfo.sortDesc)
            val portMappingsList = portMappings.values.sortedWith(comparer)
            for (curDevice in devices) {
                upnpElements.add(UpnpViewRow.DeviceHeaderViewRow(curDevice))
                upnpElements.add(UpnpViewRow.SectionHeaderViewRow(curDevice, RuleSection.OnRouter))
                var anyFound = false
                for (portMapping in portMappingsList) {
                    if (curDevice.getIpAddress() == portMapping.portMapping.DeviceIP)
                    {
                        upnpElements.add(UpnpViewRow.PortViewRow(portMapping, curDevice.udn))
                        anyFound = true
                    }
                }
                if (!anyFound && curDevice.status == DeviceStatus.FinishedEnumeratingMappings)
                {
                    upnpElements.add(UpnpViewRow.DeviceEmptyViewRow(curDevice))
                }
                // for local when show all rules is enabled, each device gets the full set of local
                //   rules set to its device (so device A will have the full set of local rules with
                //   device A, device B will have the full set with device B)
                val localForDevice = localRules.values
                    .filter { it.device.udn == curDevice.udn }
                    .sortedWith { a, b -> comparer.compare(a.toPortMappingWithPref(), b.toPortMappingWithPref()) }
                if (localForDevice.isNotEmpty())
                {
                    upnpElements.add(UpnpViewRow.SectionHeaderViewRow(curDevice, RuleSection.Local))
                    for (localRule in localForDevice) {
                        upnpElements.add(UpnpViewRow.LocalRuleViewRow(localRule))
                    }
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

    fun activate(localRule: LocalRule) = applicationScope.launch {
        try {
            val res = upnpRepository.activateLocalRule(localRule)
            if (res is UPnPCreateMappingWrapperResult.Success) {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            } else {
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Activate Port Mapping Failed"))
        }
    }

    fun deactivate(portMapping: PortMappingWithPref) = applicationScope.launch {
        try {
            val res = upnpRepository.deactivatePortMappingEntry(portMapping)
            if (res is UPnPResult.Success) {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            } else {
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).details.reason}"))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Deactivate Port Mapping Failed"))
        }
    }

    fun activateAll(selectedLocalIds: Set<LocalRuleKey>) = applicationScope.launch {
        try {
            val result = upnpRepository.activateLocalRules(
                upnpRepository.localRulesFromIds(selectedLocalIds)
            )
            val anyFailed = result.any { it is UPnPCreateMappingWrapperResult.Failure }
            if (anyFailed) {
                val res = result.first { it is UPnPCreateMappingWrapperResult.Failure }
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
            } else {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Activate Port Mappings Failed"))
        }
    }

    fun deactivateAll(selectedIds: Set<PortMappingKey>) = applicationScope.launch {
        try {
            val result = upnpRepository.deactivatePortMappingEntries(
                upnpRepository.portMappingsFromIds(selectedIds)
            )
            val anyFailed = result.any { it is UPnPResult.Failure }
            if (anyFailed) {
                val res = result.first { it is UPnPResult.Failure }
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPResult.Failure).details.reason}"))
            } else {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            }
        } catch (e: Exception) {
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Deactivate Port Mappings Failed"))
        }
    }

    fun delete(localRule: LocalRule) = applicationScope.launch {
        try {
            upnpRepository.deleteLocalRule(localRule)
        } catch (e: Exception) {
            ourLogger.log(
                Level.SEVERE,
                "Delete Local Rule Failed: " + e.message + e.stackTraceToString()
            )
            snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to delete local rule"))
        }
    }

    fun renewAll(selectedIds: Set<PortMappingKey>) {
        renewAll(upnpRepository.portMappingsFromIds(selectedIds))
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
                    if (res.enabledMatchesRequest) {
                        snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
                    } else {
                        snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("The router ignored the change. Many routers do not support disabling rules."))
                    }
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
                val ignoredCount =
                    result.count { it is UPnPCreateMappingWrapperResult.Success && !it.enabledMatchesRequest }
                val allIgnored = ignoredCount == result.size;
                val anyIgnored = ignoredCount > 0;

                if (anyFailed) {
                    val res = result.first { it is UPnPCreateMappingWrapperResult.Failure }
                    snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failure - ${(res as UPnPCreateMappingWrapperResult.Failure).details.reason}"))
                } else if (anyIgnored) {
                    if (allIgnored) {
                        snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("The router ignored the change. Many routers do not support disabling rules."))
                    } else {
                        snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("The router ignored the change for some rules. Many routers do not support disabling rules."))
                    }
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
                if (filtered != _selectedIds.value) _selectedIds.value = filtered
                savedStateHandle["selected_ids"] = filtered.toList()
            }
            .launchIn(viewModelScope)
        // same for local rules: an activate (or a re-enumeration) moves one back onto the router
        //   and it leaves the selection with it.
        combine(_selectedLocalIds, upnpRepository.localRules) { selectedLocalIds, currentLocal ->
            selectedLocalIds intersect currentLocal.keys
        }
            .distinctUntilChanged()
            .onEach { filtered ->
                if (filtered != _selectedLocalIds.value) _selectedLocalIds.value = filtered
                savedStateHandle["selected_local_ids"] = filtered.toList()
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

    // the trash icon in multi select.  local rules are just rows, so they go first and cannot
    //   fail against the router; the router batch is then reported the same way deleteAll is.
    fun deleteSelected(selectedIds: Set<PortMappingKey>, selectedLocalIds: Set<LocalRuleKey>) =
        applicationScope.launch {
            try {
                upnpRepository.deleteLocalRules(upnpRepository.localRulesFromIds(selectedLocalIds))
            } catch (e: Exception) {
                ourLogger.log(
                    Level.SEVERE,
                    "Delete Local Rules Failed: " + e.message + e.stackTraceToString()
                )
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to delete local rules"))
                return@launch
            }
            if (selectedIds.isNotEmpty()) {
                deleteAll(upnpRepository.portMappingsFromIds(selectedIds)).join()
            } else {
                snackbarManager.show(UiSnackToastEvent.ToastEvent("Success", Toast.LENGTH_SHORT))
            }
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
            val verbString = if (modifyCase) "modify" else "create"
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
                    "Create Port Mappings Failed: " + exception.message + exception.stackTraceToString()
                )
                snackbarManager.show(UiSnackToastEvent.SnackBarViewLogEvent("Failed to $verbString rule."))
                return@launch
            }
        }
}

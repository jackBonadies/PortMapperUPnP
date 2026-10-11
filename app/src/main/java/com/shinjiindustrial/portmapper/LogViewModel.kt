package com.shinjiindustrial.portmapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val preferencesRepository: PreferencesManager
) : ViewModel() {

    // Eagerly: needed on the first frame
    val showTimestamps: StateFlow<Boolean> = preferencesRepository.showLogTimestamps.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    fun toggleShowTimestamps() {
        viewModelScope.launch {
            preferencesRepository.updateShowLogTimestamps(!showTimestamps.value)
        }
    }
}

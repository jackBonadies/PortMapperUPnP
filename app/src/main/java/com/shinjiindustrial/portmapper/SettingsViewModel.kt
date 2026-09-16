package com.shinjiindustrial.portmapper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeUiState(val dayNightMode: DayNightMode, val materialYou: Boolean)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesManager
) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = combine(
        preferencesRepository.dayNight,
        preferencesRepository.materialYou
    ) { dayNight, materialYou ->
        ThemeUiState(dayNight, materialYou)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeUiState(DayNightMode.FOLLOW_SYSTEM, false)
    )

    val showEnableDisable: StateFlow<Boolean> = preferencesRepository.showEnableDisable.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val showAllLocalRules: StateFlow<Boolean> = preferencesRepository.showAllLocalRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    fun updateDayNight(dayNight: DayNightMode) {
        viewModelScope.launch {
            preferencesRepository.updateDayNight(dayNight)
        }
    }

    fun updateMaterialYou(materialYou: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateMaterialYou(materialYou)
        }
    }

    fun updateShowEnableDisable(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowEnableDisable(show)
        }
    }

    fun updateShowAllLocalRules(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowAllLocalRules(show)
        }
    }
}

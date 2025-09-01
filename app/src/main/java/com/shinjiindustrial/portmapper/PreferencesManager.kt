package com.shinjiindustrial.portmapper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shinjiindustrial.portmapper.PreferencesManager.Keys.SORT_DESC_KEY
import com.shinjiindustrial.portmapper.common.SortBy
import com.shinjiindustrial.portmapper.common.SortInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore("preferences")

    private object Keys {
        val SORT_BY_KEY = intPreferencesKey("sortOrderPref")
        val SORT_DESC_KEY = booleanPreferencesKey("descAscPref")
        val DAY_NIGHT_KEY = intPreferencesKey("dayNightPref")
        val MATERIAL_YOU_KEY = booleanPreferencesKey("materialYouPref")
    }

    val materialYou: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[Keys.MATERIAL_YOU_KEY] ?: false
        }
        .distinctUntilChanged()

    val dayNight: Flow<DayNightMode> = context.dataStore.data
        .map { preferences ->
            DayNightMode.from(preferences[Keys.DAY_NIGHT_KEY] ?: 0)
        }
        .distinctUntilChanged()

    val sortInfo: Flow<SortInfo> = context.dataStore.data
        .map { preferences ->
            SortInfo(
                SortBy.from(preferences[Keys.SORT_BY_KEY] ?: SortBy.ExternalPort.sortByValue),
                preferences[SORT_DESC_KEY] ?: false
            )
        }
        .distinctUntilChanged()

    suspend fun updateDayNight(dayNightMode: DayNightMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DAY_NIGHT_KEY] = dayNightMode.intVal
        }
    }

    suspend fun updateMaterialYou(materialYou: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MATERIAL_YOU_KEY] = materialYou
        }
    }

    suspend fun updateSortBy(sortBy: SortBy) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_BY_KEY] = sortBy.sortByValue
        }
    }

    suspend fun updateSortDesc(sortDesc: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SORT_DESC_KEY] = sortDesc
        }
    }
}

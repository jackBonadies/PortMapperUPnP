package java.com.shinjiindustrial.portmapper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shinjiindustrial.portmapper.DayNightMode
import com.shinjiindustrial.portmapper.SharedPrefKeys
import com.shinjiindustrial.portmapper.SharedPrefValues
import com.shinjiindustrial.portmapper.common.SortBy
import com.shinjiindustrial.portmapper.common.SortInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.com.shinjiindustrial.portmapper.PreferencesManager.Keys.SORT_DESC_KEY
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
    }

    val sortInfo: StateFlow<SortInfo> = context.dataStore.data
        .map { preferences ->
            SortInfo(
                SortBy.from(preferences[Keys.SORT_BY_KEY] ?: SortBy.ExternalPort.sortByValue),
                preferences[SORT_DESC_KEY] ?: false)
        }
        .distinctUntilChanged()
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = SortInfo(SortBy.ExternalPort, false)
        )

    suspend fun updateSortBy(sortBy: SortBy) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_BY_KEY] = sortBy.sortByValue
        }
    }

    suspend fun updateSortDesc(sortDesc: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_DESC_KEY] = sortDesc
        }
    }

    // TODO move these
    fun restoreSharedPrefs()
    {
        runBlocking {
            val preferences = context.dataStore.data.first()
            val nightModeKey = intPreferencesKey(SharedPrefKeys.dayNightPref)
            SharedPrefValues.DayNightPref = DayNightMode.from(preferences[nightModeKey] ?: 0)
            val materialYouKey = booleanPreferencesKey(SharedPrefKeys.materialYouPref)
            SharedPrefValues.MaterialYouTheme = preferences[materialYouKey] ?: false
        }
    }

    fun saveSharedPrefs() {
        GlobalScope.launch(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                val nightModeKey = intPreferencesKey(SharedPrefKeys.dayNightPref)
                preferences[nightModeKey] = SharedPrefValues.DayNightPref.intVal
                val materialYouKey = booleanPreferencesKey(SharedPrefKeys.materialYouPref)
                preferences[materialYouKey] = SharedPrefValues.MaterialYouTheme
            }
        }
    }
}

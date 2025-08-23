package java.com.shinjiindustrial.portmapper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shinjiindustrial.portmapper.SharedPrefValues
import com.shinjiindustrial.portmapper.common.SortBy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val sortBy: StateFlow<SortBy> = context.dataStore.data
        .map { preferences ->
            SortBy.from(preferences[Keys.SORT_BY_KEY] ?: SortBy.ExternalPort.sortByValue)
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = SortBy.Slot
        )

    suspend fun updateSortBy(sortBy: SortBy) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_BY_KEY] = sortBy.sortByValue
        }
    }
}

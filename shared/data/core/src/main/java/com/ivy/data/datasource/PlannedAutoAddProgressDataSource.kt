package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last due date recorded automatically for each planned payment, so a payment the user
 * deleted (e.g. a SIP that failed) isn't recorded again.
 */
@Singleton
class PlannedAutoAddProgressDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun lastAdded(ruleId: UUID): Instant? =
        dataStore.data.first()[key(ruleId)]?.let(Instant::ofEpochMilli)

    suspend fun setLastAdded(ruleId: UUID, dueDate: Instant) {
        dataStore.edit { it[key(ruleId)] = dueDate.toEpochMilli() }
    }

    private fun key(ruleId: UUID): Preferences.Key<Long> = longPreferencesKey("planned_auto_add_last_$ruleId")
}

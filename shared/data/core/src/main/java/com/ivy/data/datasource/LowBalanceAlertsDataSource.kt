package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ivy.data.model.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Alert me when this account's balance is below X", per account, and which accounts were
 * already alerted (so the alert fires once per drop). A setting, so it isn't part of backups.
 */
@Singleton
class LowBalanceAlertsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** The alert amount per account; accounts without an alert aren't in the map. */
    val thresholds: Flow<Map<AccountId, Double>> = dataStore.data.map { prefs ->
        prefs[ThresholdsKey].orEmpty().mapNotNull(::parseThreshold).toMap()
    }

    suspend fun setThreshold(accountId: AccountId, threshold: Double?) {
        dataStore.edit { prefs ->
            val others = prefs[ThresholdsKey].orEmpty()
                .filterNot { it.startsWith("${accountId.value}=") }
                .toSet()
            prefs[ThresholdsKey] = if (threshold == null) others else others + "${accountId.value}=$threshold"
            // a new limit is checked afresh
            prefs[AlertedKey] = prefs[AlertedKey].orEmpty() - accountId.value.toString()
        }
    }

    suspend fun alerted(): Set<AccountId> =
        dataStore.data.first()[AlertedKey].orEmpty().mapNotNull(::accountId).toSet()

    suspend fun setAlerted(accountId: AccountId, alerted: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[AlertedKey].orEmpty()
            val id = accountId.value.toString()
            prefs[AlertedKey] = if (alerted) current + id else current - id
        }
    }

    private fun parseThreshold(entry: String): Pair<AccountId, Double>? {
        val id = accountId(entry.substringBefore("="))
        val amount = entry.substringAfter("=").toDoubleOrNull()
        return if (id != null && amount != null) id to amount else null
    }

    private fun accountId(raw: String): AccountId? = runCatching { AccountId(UUID.fromString(raw)) }.getOrNull()

    private companion object {
        val ThresholdsKey = stringSetPreferencesKey("low_balance_thresholds")
        val AlertedKey = stringSetPreferencesKey("low_balance_alerted")
    }
}

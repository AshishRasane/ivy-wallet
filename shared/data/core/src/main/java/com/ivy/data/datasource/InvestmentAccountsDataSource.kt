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
 * The accounts the user counts as investments (e.g. Zerodha, Kuvera). Money moved into them
 * is "invested", not spent. A setting, so it isn't part of backups.
 */
@Singleton
class InvestmentAccountsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val ids: Flow<Set<AccountId>> = dataStore.data.map { prefs ->
        prefs[Key].orEmpty()
            .mapNotNull { runCatching { AccountId(UUID.fromString(it)) }.getOrNull() }
            .toSet()
    }

    suspend fun get(): Set<AccountId> = ids.first()

    suspend fun set(accountId: AccountId, isInvestment: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Key].orEmpty()
            val id = accountId.value.toString()
            prefs[Key] = if (isInvestment) current + id else current - id
        }
    }

    private companion object {
        val Key = stringSetPreferencesKey("investment_account_ids")
    }
}

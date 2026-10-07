package com.ivy.autobackup

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivy.data.datastore.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class AutoBackupState(
    val enabled: Boolean,
    /** Storage Access Framework tree URI of the backup folder. */
    val folderUri: String?,
    val lastSuccessAt: Instant?,
    /** Set when the most recent attempt failed; cleared by the next success. */
    val lastFailure: AutoBackupFailure?,
    val lastHash: String?,
)

/**
 * Persists the automatic backup configuration and the outcome of the last run.
 */
@Singleton
class AutoBackupSettings @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {
    val state: Flow<AutoBackupState> = context.dataStore.data.map { prefs ->
        AutoBackupState(
            enabled = prefs[ENABLED] ?: false,
            folderUri = prefs[FOLDER_URI],
            lastSuccessAt = prefs[LAST_SUCCESS_EPOCH_MS]?.let(Instant::ofEpochMilli),
            lastFailure = prefs[LAST_FAILURE]?.let { name ->
                AutoBackupFailure.entries.firstOrNull { it.name == name }
            },
            lastHash = prefs[LAST_HASH],
        )
    }

    suspend fun current(): AutoBackupState = state.first()

    suspend fun setEnabled(enabled: Boolean): Unit = edit { it[ENABLED] = enabled }

    suspend fun setFolder(folderUri: String): Unit = edit {
        it[FOLDER_URI] = folderUri
        // a new folder must receive a fresh backup even if the data didn't change
        it.remove(LAST_HASH)
        it.remove(LAST_FAILURE)
    }

    suspend fun recordSuccess(at: Instant, hash: String): Unit = edit {
        it[LAST_SUCCESS_EPOCH_MS] = at.toEpochMilli()
        it[LAST_HASH] = hash
        it.remove(LAST_FAILURE)
    }

    suspend fun recordUnchanged(): Unit = edit { it.remove(LAST_FAILURE) }

    suspend fun recordFailure(failure: AutoBackupFailure): Unit = edit { it[LAST_FAILURE] = failure.name }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("auto_backup_enabled")
        val FOLDER_URI: Preferences.Key<String> = stringPreferencesKey("auto_backup_folder_uri")
        val LAST_SUCCESS_EPOCH_MS: Preferences.Key<Long> = longPreferencesKey("auto_backup_last_success")
        val LAST_FAILURE: Preferences.Key<String> = stringPreferencesKey("auto_backup_last_failure")
        val LAST_HASH: Preferences.Key<String> = stringPreferencesKey("auto_backup_last_hash")
    }
}

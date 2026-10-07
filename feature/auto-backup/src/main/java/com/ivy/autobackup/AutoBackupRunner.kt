package com.ivy.autobackup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.ivy.base.time.TimeProvider
import com.ivy.data.backup.BackupDataUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

enum class AutoBackupFailure {
    /** No folder was chosen yet. */
    NoFolder,

    /** The folder was deleted, or the app lost access to it. */
    FolderUnavailable,
    WriteFailed,
}

sealed interface AutoBackupResult {
    data class Saved(val fileName: String) : AutoBackupResult
    data object Unchanged : AutoBackupResult
    data class Failed(val failure: AutoBackupFailure) : AutoBackupResult
}

/**
 * Writes one backup zip (same format as the manual "Backup data") into the chosen folder
 * and keeps only the newest [AutoBackupFiles.KEEP_LAST] automatic backups there.
 */
@Singleton
class AutoBackupRunner @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val backupDataUseCase: BackupDataUseCase,
    private val settings: AutoBackupSettings,
    private val timeProvider: TimeProvider,
) {
    // "Back up now" and the daily job must not write at the same time.
    private val mutex = Mutex()

    /**
     * @param force write a new file even if nothing changed since the last backup.
     */
    suspend fun backup(force: Boolean): AutoBackupResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val result = try {
                backupInternal(force)
            } catch (e: Exception) {
                Timber.e(e, "Automatic backup failed")
                AutoBackupResult.Failed(AutoBackupFailure.WriteFailed)
            }
            if (result is AutoBackupResult.Failed) settings.recordFailure(result.failure)
            result
        }
    }

    private suspend fun backupInternal(force: Boolean): AutoBackupResult = either {
        val state = settings.current()
        val folderUri = ensureNotNull(state.folderUri?.let(Uri::parse)) { AutoBackupFailure.NoFolder }
        val folder = ensureNotNull(
            DocumentFile.fromTreeUri(context, folderUri)
                ?.takeIf { hasWriteAccess(folderUri) && it.isDirectory && it.canWrite() }
        ) { AutoBackupFailure.FolderUnavailable }

        val json = backupDataUseCase.generateJsonBackup()
        val hash = AutoBackupFiles.contentHash(json)
        if (!force && hash == state.lastHash) {
            settings.recordUnchanged()
            AutoBackupResult.Unchanged
        } else {
            val fileName = AutoBackupFiles.fileName(timeProvider.localNow())
            val file = ensureNotNull(folder.createFile(AutoBackupFiles.MIME_TYPE, fileName)) {
                AutoBackupFailure.WriteFailed
            }
            backupDataUseCase.exportToFile(file.uri, json)
            if (file.length() <= 0L) {
                file.delete()
                raise(AutoBackupFailure.WriteFailed)
            }

            deleteOldBackups(folder)
            settings.recordSuccess(at = timeProvider.utcNow(), hash = hash)
            AutoBackupResult.Saved(file.name ?: fileName)
        }
    }.fold(ifLeft = { AutoBackupResult.Failed(it) }, ifRight = { it })

    private fun deleteOldBackups(folder: DocumentFile) {
        val files = folder.listFiles().associateBy { it.name.orEmpty() }
        AutoBackupFiles.filesToDelete(files.keys.toList()).forEach { name ->
            files[name]?.delete()
        }
    }

    private fun hasWriteAccess(folderUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == folderUri && it.isWritePermission }

    /**
     * Keeps access to [folderUri] across app restarts and releases the previous folder.
     */
    fun takeFolderAccess(folderUri: Uri, previousFolderUri: Uri?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(folderUri, flags)
        if (previousFolderUri != null && previousFolderUri != folderUri) {
            runCatching { context.contentResolver.releasePersistableUriPermission(previousFolderUri, flags) }
        }
    }
}

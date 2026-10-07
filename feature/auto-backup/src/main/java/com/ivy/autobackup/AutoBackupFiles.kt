package com.ivy.autobackup

import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Naming, rotation and change-detection rules for automatic backups.
 * Pure Kotlin so it's easy to unit-test.
 */
object AutoBackupFiles {
    const val MIME_TYPE = "application/zip"
    const val KEEP_LAST = 10

    private const val PREFIX = "IvyWallet_AutoBackup_"
    private const val EXTENSION = ".zip"
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")

    // Only files matching exactly this pattern are ever deleted by the rotation.
    private val autoBackupName = Regex("^IvyWallet_AutoBackup_\\d{4}-\\d{2}-\\d{2}_\\d{6}\\.zip$")

    fun fileName(localTime: LocalDateTime): String =
        PREFIX + timestampFormat.format(localTime) + EXTENSION

    fun isAutoBackup(fileName: String): Boolean = autoBackupName.matches(fileName)

    /**
     * @return the automatic backups that should be deleted so only the newest [keep] remain.
     * Files not created by automatic backup are never returned.
     */
    fun filesToDelete(fileNames: List<String>, keep: Int = KEEP_LAST): List<String> =
        fileNames
            .filter(::isAutoBackup)
            // the timestamp format sorts chronologically as text
            .sortedDescending()
            .drop(keep)

    /** Used to skip writing a new file when the data hasn't changed since the last backup. */
    fun contentHash(backupJson: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(backupJson.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /**
     * Turns a Storage Access Framework tree document id into a readable path,
     * e.g. "primary:Documents/IvyBackups" -> "Documents/IvyBackups".
     */
    fun folderDisplayName(treeDocumentId: String): String {
        val volume = treeDocumentId.substringBefore(":", missingDelimiterValue = "")
        val path = treeDocumentId.substringAfter(":")
        return when {
            volume == "primary" || volume.isEmpty() -> path.ifBlank { "Internal storage" }
            path.isBlank() -> "SD card"
            else -> "SD card/$path"
        }
    }
}

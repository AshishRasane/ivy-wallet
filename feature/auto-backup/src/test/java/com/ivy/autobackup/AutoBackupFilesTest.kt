package com.ivy.autobackup

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import java.time.LocalDateTime

class AutoBackupFilesTest {

    @Test
    fun `file name contains a sortable local timestamp`() {
        // When
        val name = AutoBackupFiles.fileName(LocalDateTime.of(2026, 10, 7, 2, 14, 5))

        // Then
        name shouldBe "IvyWallet_AutoBackup_2026-10-07_021405.zip"
        AutoBackupFiles.isAutoBackup(name) shouldBe true
    }

    @Test
    fun `only exact automatic backup names are recognized`() {
        AutoBackupFiles.isAutoBackup("IvyWalletBackup_2026-10-07T02:14:05.zip") shouldBe false
        AutoBackupFiles.isAutoBackup("IvyWallet_AutoBackup_2026-10-07_021405 (1).zip") shouldBe false
        AutoBackupFiles.isAutoBackup("IvyWallet_AutoBackup_2026-10-07_021405.zip.tmp") shouldBe false
        AutoBackupFiles.isAutoBackup("notes.txt") shouldBe false
    }

    @Test
    fun `keeps the newest backups and deletes the rest`() {
        // Given
        val backups = (1..12).map { day -> AutoBackupFiles.fileName(LocalDateTime.of(2026, 9, day, 2, 0)) }

        // When
        val toDelete = AutoBackupFiles.filesToDelete(backups.shuffled(), keep = 10)

        // Then
        toDelete.toSet() shouldBe setOf(backups[0], backups[1])
    }

    @Test
    fun `never deletes files it didn't create`() {
        // Given
        val names = listOf(
            "IvyWalletBackup_manual.zip",
            "bank-statement.pdf",
            AutoBackupFiles.fileName(LocalDateTime.of(2026, 10, 1, 2, 0)),
            AutoBackupFiles.fileName(LocalDateTime.of(2026, 10, 2, 2, 0)),
        )

        // When
        val toDelete = AutoBackupFiles.filesToDelete(names, keep = 1)

        // Then
        toDelete shouldBe listOf(AutoBackupFiles.fileName(LocalDateTime.of(2026, 10, 1, 2, 0)))
    }

    @Test
    fun `nothing is deleted while under the limit`() {
        val names = listOf(AutoBackupFiles.fileName(LocalDateTime.of(2026, 10, 1, 2, 0)))

        AutoBackupFiles.filesToDelete(names, keep = 10) shouldBe emptyList()
    }

    @Test
    fun `content hash changes only when the data changes`() {
        val hash = AutoBackupFiles.contentHash("""{"transactions":[1]}""")

        AutoBackupFiles.contentHash("""{"transactions":[1]}""") shouldBe hash
        AutoBackupFiles.contentHash("""{"transactions":[1,2]}""") shouldNotBe hash
    }

    @Test
    fun `folder display name is readable`() {
        AutoBackupFiles.folderDisplayName("primary:Documents/IvyBackups") shouldBe "Documents/IvyBackups"
        AutoBackupFiles.folderDisplayName("primary:") shouldBe "Internal storage"
        AutoBackupFiles.folderDisplayName("1A2B-3C4D:Backups") shouldBe "SD card/Backups"
    }
}

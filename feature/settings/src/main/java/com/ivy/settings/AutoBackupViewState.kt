package com.ivy.settings

import androidx.compose.runtime.Immutable

@Immutable
data class AutoBackupViewState(
    val enabled: Boolean,
    val hasFolder: Boolean,
    /** e.g. "Documents/IvyBackups" */
    val folder: String?,
    /** e.g. "Last backup: Today, 2:14 AM" */
    val status: String,
    val inProgress: Boolean,
)

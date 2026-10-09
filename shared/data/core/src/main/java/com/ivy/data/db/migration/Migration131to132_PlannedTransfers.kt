package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Planned payments can be transfers (toAccountId) and can be recorded automatically (autoAdd). */
@Suppress("ClassNaming", "MagicNumber") // named like the other migrations
class Migration131to132_PlannedTransfers : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE planned_payment_rules ADD COLUMN toAccountId TEXT")
        db.execSQL("ALTER TABLE planned_payment_rules ADD COLUMN autoAdd INTEGER NOT NULL DEFAULT 0")
    }
}

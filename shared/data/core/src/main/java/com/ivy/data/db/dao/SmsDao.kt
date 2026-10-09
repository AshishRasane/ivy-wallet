package com.ivy.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ivy.data.db.entity.SmsAccountLinkEntity
import com.ivy.data.db.entity.SmsCategoryLinkEntity
import com.ivy.data.db.entity.SmsTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SmsDao {
    /** @return -1 when a transaction with the same fingerprint already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: SmsTransactionEntity): Long

    @Query("SELECT * FROM sms_transactions WHERE id = :id")
    suspend fun findById(id: UUID): SmsTransactionEntity?

    @Query("SELECT * FROM sms_transactions WHERE status = :status ORDER BY dateTime DESC")
    fun observeByStatus(status: String): Flow<List<SmsTransactionEntity>>

    @Query("SELECT COUNT(*) FROM sms_transactions WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("UPDATE sms_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: UUID, status: String)

    @Query("DELETE FROM sms_transactions WHERE status = :status AND createdAt < :beforeEpochMs")
    suspend fun deleteOlderThan(status: String, beforeEpochMs: Long)

    /** Whether a saved transaction already mentions this reference (e.g. added before SMS v2). */
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE description LIKE '%' || :reference || '%')")
    suspend fun isReferenceInTransactions(reference: String): Boolean

    @Query("SELECT * FROM sms_account_links WHERE `key` = :key")
    suspend fun findAccountLink(key: String): SmsAccountLinkEntity?

    @Upsert
    suspend fun saveAccountLink(link: SmsAccountLinkEntity)

    @Query("DELETE FROM sms_account_links WHERE `key` = :key")
    suspend fun deleteAccountLink(key: String)

    @Query("SELECT * FROM sms_category_links WHERE merchantKey = :merchantKey")
    suspend fun findCategoryLink(merchantKey: String): SmsCategoryLinkEntity?

    @Upsert
    suspend fun saveCategoryLink(link: SmsCategoryLinkEntity)
}

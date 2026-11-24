// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal interface DonatedItemDao {
    @Upsert suspend fun upsert(items: List<DonatedItemEntity>)

    @Query("DELETE FROM donated_items WHERE packageName = :packageName AND itemId IN (:itemIds)")
    suspend fun delete(packageName: String, itemIds: List<String>)

    @Query("DELETE FROM donated_items WHERE packageName = :packageName")
    suspend fun deleteAll(packageName: String)

    @Query("DELETE FROM donated_items WHERE expiresAt IS NOT NULL AND expiresAt <= :now")
    suspend fun deleteExpired(now: Long): Int

    @Query("SELECT COUNT(*) FROM donated_items WHERE packageName = :packageName")
    suspend fun count(packageName: String): Int

    @Query(
        "SELECT COUNT(*) FROM donated_items WHERE packageName = :packageName AND itemId IN (:itemIds)"
    )
    suspend fun countExisting(packageName: String, itemIds: List<String>): Int

    @Query("SELECT packageName, type, sourceLabel FROM donated_items GROUP BY packageName, type")
    suspend fun packageTypes(): List<PackageType>

    @Query(
        """
        SELECT d.* FROM donated_items d JOIN donated_items_fts f ON d.rowid = f.rowid
        WHERE donated_items_fts MATCH :match AND d.packageName = :packageName
          AND (d.expiresAt IS NULL OR d.expiresAt > :now) AND d.timestamp >= :since
          AND (:anyType OR d.type IN (:types))
        ORDER BY d.timestamp DESC LIMIT :limit
        """
    )
    suspend fun search(
        packageName: String,
        match: String,
        now: Long,
        since: Long,
        anyType: Boolean,
        types: List<String>,
        limit: Int,
    ): List<DonatedItemEntity>

    @Query(
        """
        SELECT * FROM donated_items
        WHERE packageName = :packageName AND (expiresAt IS NULL OR expiresAt > :now) AND timestamp >= :since
          AND (:anyType OR type IN (:types))
        ORDER BY timestamp DESC LIMIT :limit
        """
    )
    suspend fun recent(
        packageName: String,
        now: Long,
        since: Long,
        anyType: Boolean,
        types: List<String>,
        limit: Int,
    ): List<DonatedItemEntity>

    @Transaction
    suspend fun replace(packageName: String, items: List<DonatedItemEntity>) {
        deleteAll(packageName)
        upsert(items)
    }
}

internal data class PackageType(val packageName: String, val type: String, val sourceLabel: String)

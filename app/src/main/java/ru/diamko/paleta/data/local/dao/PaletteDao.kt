package ru.diamko.paleta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.diamko.paleta.data.local.entity.PaletteEntity

@Dao
interface PaletteDao {

    @Query("SELECT * FROM palettes WHERE pendingAction IS NULL OR pendingAction != 'delete' ORDER BY createdAtIso DESC")
    suspend fun getVisiblePalettes(): List<PaletteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(palettes: List<PaletteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(palette: PaletteEntity)

    @Query("DELETE FROM palettes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM palettes")
    suspend fun deleteAll()

    @Query("SELECT * FROM palettes WHERE isSynced = 0 OR pendingAction IS NOT NULL")
    suspend fun getPendingChanges(): List<PaletteEntity>

    @Query("UPDATE palettes SET isSynced = 1, pendingAction = NULL WHERE id = :id")
    suspend fun markSynced(id: Long)
}

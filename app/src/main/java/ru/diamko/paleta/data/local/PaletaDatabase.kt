package ru.diamko.paleta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.diamko.paleta.data.local.dao.PaletteDao
import ru.diamko.paleta.data.local.entity.PaletteEntity

@Database(entities = [PaletteEntity::class], version = 1, exportSchema = false)
abstract class PaletaDatabase : RoomDatabase() {
    abstract fun paletteDao(): PaletteDao
}

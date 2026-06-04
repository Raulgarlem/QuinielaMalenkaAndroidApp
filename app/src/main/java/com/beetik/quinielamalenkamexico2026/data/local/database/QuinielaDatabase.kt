package com.beetik.quinielamalenkamexico2026.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beetik.quinielamalenkamexico2026.data.local.dao.QuinielaDao
import com.beetik.quinielamalenkamexico2026.data.local.dao.RankingConfigDao
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.data.local.entity.RankingConfigEntity

@Database(entities = [QuinielaEntity::class, RankingConfigEntity::class], version = 6, exportSchema = false)
abstract class QuinielaDatabase : RoomDatabase() {
    abstract fun quinielaDao(): QuinielaDao
    abstract fun rankingConfigDao(): RankingConfigDao

    companion object {
        @Volatile
        private var INSTANCE: QuinielaDatabase? = null

        fun getDatabase(context: Context): QuinielaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuinielaDatabase::class.java,
                    "quiniela_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

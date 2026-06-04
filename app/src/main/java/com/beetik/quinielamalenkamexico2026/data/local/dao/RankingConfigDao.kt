package com.beetik.quinielamalenkamexico2026.data.local.dao

import androidx.room.*
import com.beetik.quinielamalenkamexico2026.data.local.entity.RankingConfigEntity

@Dao
interface RankingConfigDao {
    @Query("SELECT * FROM ranking_configs")
    suspend fun getAllConfigs(): List<RankingConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: RankingConfigEntity)

    @Delete
    suspend fun deleteConfig(config: RankingConfigEntity)

    @Query("UPDATE ranking_configs SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ranking_configs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)
}

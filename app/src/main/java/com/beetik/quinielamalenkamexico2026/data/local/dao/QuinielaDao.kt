package com.beetik.quinielamalenkamexico2026.data.local.dao

import androidx.room.*
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity

@Dao
interface QuinielaDao {
    @Query("SELECT * FROM quinielas")
    suspend fun getAllQuinielas(): List<QuinielaEntity>

    @Query("SELECT * FROM quinielas WHERE quinielaName = :name AND propietarioName = :owner LIMIT 1")
    suspend fun getQuinielaByNameAndOwner(name: String, owner: String): QuinielaEntity?

    @Query("SELECT * FROM quinielas LIMIT 1")
    suspend fun getFirstQuiniela(): QuinielaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiniela(quiniela: QuinielaEntity)

    @Delete
    suspend fun deleteQuiniela(quiniela: QuinielaEntity)
}

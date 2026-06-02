package com.beetik.quinielamalenkamexico2026.data.local.dao

import androidx.room.*
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuinielaDao {
    @Query("SELECT * FROM quinielas")
    fun getAllQuinielasFlow(): Flow<List<QuinielaEntity>>

    @Query("SELECT * FROM quinielas")
    suspend fun getAllQuinielas(): List<QuinielaEntity>

    @Query("SELECT * FROM quinielas WHERE quinielaName = :name AND propietarioName = :owner LIMIT 1")
    suspend fun getQuinielaByNameAndOwner(name: String, owner: String): QuinielaEntity?

    @Query("SELECT * FROM quinielas WHERE id = :id LIMIT 1")
    suspend fun getQuinielaById(id: Int): QuinielaEntity?

    @Query("SELECT * FROM quinielas LIMIT 1")
    suspend fun getFirstQuiniela(): QuinielaEntity?

    @Query("UPDATE quinielas SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Transaction
    suspend fun toggleFavorite(id: Int) {
        val quiniela = getQuinielaById(id)
        val wasFavorite = quiniela?.isFavorite ?: false
        
        clearAllFavorites()
        
        if (!wasFavorite) {
            quiniela?.let {
                insertQuiniela(it.copy(isFavorite = true))
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiniela(quiniela: QuinielaEntity)

    @Delete
    suspend fun deleteQuiniela(quiniela: QuinielaEntity)
}

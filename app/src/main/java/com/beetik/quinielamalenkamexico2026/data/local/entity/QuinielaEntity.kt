package com.beetik.quinielamalenkamexico2026.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quinielas")
data class QuinielaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quinielaName: String,
    val propietarioName: String,
    val userEmail: String = "",
    val resultsJson: String, // Serialized Map<String, MatchResult>
    val winnersJson: String = "{}", // Serialized Map<String, String> (Group -> TeamName)
    val isSent: Boolean = false
)

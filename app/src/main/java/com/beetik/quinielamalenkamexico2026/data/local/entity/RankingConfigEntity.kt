package com.beetik.quinielamalenkamexico2026.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ranking_configs")
data class RankingConfigEntity(
    @PrimaryKey val id: String,
    val configName: String,
    val resultsJson: String,
    val addedParticipantsJson: String,
    val pinnedCategoriesJson: String,
    val pinnedIdsJson: String,
    val categoryNamesJson: String,
    val isLiveRanking: Boolean,
    val comparisonParticipantId: String?,
    val isActive: Boolean = false
)

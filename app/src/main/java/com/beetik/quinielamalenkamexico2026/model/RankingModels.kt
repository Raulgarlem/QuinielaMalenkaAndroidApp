package com.beetik.quinielamalenkamexico2026.model

import androidx.compose.ui.graphics.Color

data class PinCategory(
    val id: Int,
    val name: String,
    val color: Color
)

data class RankingConfig(
    val id: String,
    val configName: String,
    val resultsMap: Map<String, MatchScore>,
    val addedParticipants: List<Participant>,
    val pinnedParticipantCategories: Map<String, Int>,
    val pinnedParticipantIds: List<String>,
    val categoryNames: Map<Int, String>,
    val isLiveRanking: Boolean,
    val comparisonParticipantId: String?
)

data class MatchScore(
    val home: Int,
    val away: Int
)

data class Participant(
    val id: String,
    val quinielaName: String,
    val ownerName: String,
    val isUser: Boolean = false,
    val predictions: Map<String, Pair<Int, Int>>,
    val groupWinnerPredictions: Map<String, String>,
    val prevPosition: Int,
    val isKnockout: Boolean = false
)

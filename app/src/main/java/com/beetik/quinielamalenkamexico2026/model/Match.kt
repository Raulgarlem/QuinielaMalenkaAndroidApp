package com.beetik.quinielamalenkamexico2026.model

data class Match(
    val id: String,
    val group: String,
    val date: String,
    val time: String,
    val homeTeam: String,
    val homeFlag: String,
    val awayTeam: String,
    val awayFlag: String,
    val realHomeScore: Int? = null,
    val realAwayScore: Int? = null,
    val started: Boolean = false,
    val finished: Boolean = false,
    val isActive: Boolean = false,
    val firebaseId: String? = null
)

data class MatchResult(
    val homeScore: String = "",
    val awayScore: String = ""
)

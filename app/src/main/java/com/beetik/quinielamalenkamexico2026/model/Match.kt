package com.beetik.quinielamalenkamexico2026.model

data class Match(
    val id: String,
    val group: String,
    val date: String,
    val homeTeam: String,
    val homeFlag: String,
    val awayTeam: String,
    val awayFlag: String
)

data class MatchResult(
    val homeScore: String = "",
    val awayScore: String = ""
)

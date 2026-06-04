package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.ui.graphics.Color
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore

fun calculatePoints(pred: Pair<Int, Int>, actual: MatchScore): Int {
    if (pred.first == actual.home && pred.second == actual.away) return 2
    val pW = when { pred.first > pred.second -> 1; pred.first < pred.second -> 2; else -> 0 }
    val aW = when { actual.home > actual.away -> 1; actual.home < actual.away -> 2; else -> 0 }
    return if (pW == aW) 1 else 0
}

data class TeamStats(val name: String, val flag: String, var points: Int = 0, var gs: Int = 0, var gc: Int = 0)

fun getGroupWinner(groupName: String, matches: List<Match>, results: Map<String, MatchScore>): Pair<String, String>? {
    val gM = matches.filter { it.group == groupName }
    val teams = gM.flatMap { listOf(it.homeTeam to it.homeFlag, it.awayTeam to it.awayFlag) }.distinctBy { it.first }
    val std = teams.associate { it.first to TeamStats(it.first, it.second) }.toMutableMap()
    gM.forEach { m ->
        results[m.id]?.let { score ->
            val h = std[m.homeTeam]!!; val a = std[m.awayTeam]!!
            h.gs += score.home; h.gc += score.away; a.gs += score.away; a.gc += score.home
            when { score.home > score.away -> h.points += 3; score.home < score.away -> a.points += 3; else -> { h.points += 1; a.points += 1 } }
        }
    }
    if (results.keys.none { id -> gM.any { it.id == id } }) return null
    return std.values.sortedWith(compareByDescending<TeamStats> { it.points }.thenByDescending { it.gs - it.gc }.thenByDescending { it.gs }).firstOrNull()?.let { it.name to it.flag }
}

fun getPointColor(points: Int): Color = when (points) { 2 -> Color(0xFF4CAF50); 1 -> Color(0xFF2196F3); else -> Color.Gray }

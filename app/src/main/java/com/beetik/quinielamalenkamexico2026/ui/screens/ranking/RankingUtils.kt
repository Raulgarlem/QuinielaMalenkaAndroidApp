package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.ui.graphics.Color
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore

fun calculatePoints(pred: Pair<Int, Int>, actual: MatchScore): Int {
    if (pred.first == actual.home && pred.second == actual.away) return 2
    val pW = when { pred.first > pred.second -> 1; pred.first < pred.second -> 2; else -> 0 }
    val aW = when { actual.home > actual.away -> 1; actual.home < actual.away -> 2; else -> 0 }
    return if (pW == aW) 1 else 0
}

/**
 * Obtiene el marcador "efectivo" para un partido.
 * Prioriza: 1. Simulación (resultsMap), 2. Marcador Real si el partido está en vivo o terminado.
 */
fun getEffectiveScore(match: Match, results: Map<String, MatchScore>): MatchScore? {
    // 1. Prioridad: Simulación manual del usuario
    if (results.containsKey(match.id)) return results[match.id]
    
    // 2. Prioridad: Marcador real de Firebase si el partido ya empezó (vivo o terminado)
    if (match.started && match.realHomeScore != null && match.realAwayScore != null) {
        return MatchScore(match.realHomeScore, match.realAwayScore)
    }
    
    return null
}

data class TeamStats(val name: String, val flag: String, var points: Int = 0, var gs: Int = 0, var gc: Int = 0)

fun getGroupWinner(groupName: String, matches: List<Match>, results: Map<String, MatchScore>): Pair<String, String>? {
    val gM = matches.filter { it.group == groupName }
    val teams = gM.flatMap { listOf(it.homeTeam to it.homeFlag, it.awayTeam to it.awayFlag) }.distinctBy { it.first }
    val std = teams.associate { it.first to TeamStats(it.first, it.second) }.toMutableMap()
    
    gM.forEach { m ->
        getEffectiveScore(m, results)?.let { score ->
            val h = std[m.homeTeam]!!; val a = std[m.awayTeam]!!
            h.gs += score.home; h.gc += score.away; a.gs += score.away; a.gc += score.home
            when { score.home > score.away -> h.points += 3; score.home < score.away -> a.points += 3; else -> { h.points += 1; a.points += 1 } }
        }
    }
    
    val hasAnyValidScore = gM.any { m -> getEffectiveScore(m, results) != null }
    if (!hasAnyValidScore) return null

    return std.values.sortedWith(compareByDescending<TeamStats> { it.points }.thenByDescending { it.gs - it.gc }.thenByDescending { it.gs }).firstOrNull()?.let { it.name to it.flag }
}

fun getPointColor(points: Int): Color = when (points) { 
    2 -> Color(0xFF4CAF50) // Verde
    1 -> Color(0xFF2196F3) // Azul
    5 -> Color(0xFFFFD700) // Oro
    8 -> Color(0xFFFF9800) // Naranja/Ambar
    else -> Color.Gray 
}

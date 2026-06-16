package com.beetik.quinielamalenkamexico2026.util

import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchResult

object ScoreCalculator {

    data class ScoreStats(
        val totalPoints: Int,
        val hits: Int,
        val exacts: Int
    )

    fun calculateStats(
        allMatches: List<Match>,
        matchPreds: Map<String, MatchResult>,
        winnerPreds: Map<String, String>
    ): ScoreStats {
        var points = 0
        var hits = 0
        var exacts = 0

        val matchesByGroup = allMatches.groupBy { it.group }
        
        // 1. Determine real winners for fully finished groups
        val realGroupWinners = matchesByGroup.mapValues { (groupName, matches) ->
            val allGroupFinished = matches.all { it.finished }
            if (!allGroupFinished) return@mapValues null
            
            val finishedMatchesInGroup = matches.filter { it.realHomeScore != null && it.realAwayScore != null }
            if (finishedMatchesInGroup.isEmpty()) return@mapValues null
            
            val table = mutableMapOf<String, Int>()
            val goals = mutableMapOf<String, Int>()
            
            finishedMatchesInGroup.forEach { m ->
                val h = m.realHomeScore!!
                val a = m.realAwayScore!!
                goals[m.homeTeam] = (goals[m.homeTeam] ?: 0) + h
                goals[m.awayTeam] = (goals[m.awayTeam] ?: 0) + a
                when {
                    h > a -> table[m.homeTeam] = (table[m.homeTeam] ?: 0) + 3
                    h < a -> table[m.awayTeam] = (table[m.awayTeam] ?: 0) + 3
                    else -> {
                        table[m.homeTeam] = (table[m.homeTeam] ?: 0) + 1
                        table[m.awayTeam] = (table[m.awayTeam] ?: 0) + 1
                    }
                }
            }
            table.keys.sortedWith(compareByDescending<String> { table[it] ?: 0 }.thenByDescending { goals[it] ?: 0 }).firstOrNull()
        }

        // 2. Match points (Only for finished matches)
        allMatches.forEach { match ->
            if (match.finished) {
                val rh = match.realHomeScore
                val ra = match.realAwayScore
                if (rh != null && ra != null) {
                    val pred = matchPreds[match.id]
                    val uh = pred?.homeScore?.toIntOrNull()
                    val ua = pred?.awayScore?.toIntOrNull()
                    
                    if (uh != null && ua != null) {
                        if (uh == rh && ua == ra) {
                            exacts++
                            hits++
                            points += 2
                        } else {
                            val rW = when { rh > ra -> 1; rh < ra -> 2; else -> 0 }
                            val uW = when { uh > ua -> 1; uh < ua -> 2; else -> 0 }
                            if (rW == uW) {
                                hits++
                                points += 1
                            }
                        }
                    }
                }
            }
        }

        // 3. Group winner points
        realGroupWinners.forEach { (group, realWinner) ->
            if (realWinner != null) {
                if (winnerPreds[group] == realWinner) {
                    points += 2
                }
            }
        }

        return ScoreStats(points, hits, exacts)
    }
}

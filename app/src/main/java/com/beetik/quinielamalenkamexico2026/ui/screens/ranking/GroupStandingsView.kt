package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

@Composable
fun GroupStandingsView(
    allMatches: List<Match>,
    resultsMap: Map<String, Pair<Int, Int>>,
    userGroupWinners: Map<String, String>,
    headerContent: (@Composable () -> Unit)? = null
) {
    val groups = remember(allMatches) { allMatches.map { it.group }.distinct().sorted() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }
        
        items(groups) { groupName ->
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // User prediction header
                val predictedWinner = userGroupWinners[groupName] ?: "-"
                
                // Calcular estadísticas para saber quién va en primer lugar
                val stats = remember(groupName, resultsMap) {
                    calculateGroupStats(groupName, allMatches, resultsMap)
                }
                val currentLeader = stats.firstOrNull()?.name
                val isFavoriteLeading = currentLeader != null && currentLeader == predictedWinner

                Surface(
                    color = Gold.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tu favorito: ", fontSize = 12.sp, color = Color.Gray)
                        Text(predictedWinner, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold)
                        if (isFavoriteLeading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Líder",
                                tint = Gold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "GRUPO $groupName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StandingsTable(groupName, allMatches, resultsMap)
            }
        }
    }
}

@Composable
fun StandingsTable(groupName: String, allMatches: List<Match>, results: Map<String, Pair<Int, Int>>) {
    val stats = remember(groupName, results) {
        calculateGroupStats(groupName, allMatches, results)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.background(Color(0xFF333333)).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pos", modifier = Modifier.width(25.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("Equipo", modifier = Modifier.weight(1f), fontSize = 9.sp, color = Color.Gray)
            Text("PJ", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("G", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("E", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("P", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("GF", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("GC", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("DG", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color.Gray)
            Text("Pts", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Gold)
        }
        
        stats.forEachIndexed { index, team ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", modifier = Modifier.width(25.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(team.flag, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(team.name, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                val pj = team.wins + team.draws + team.losses
                Text("$pj", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.wins}", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.draws}", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.losses}", modifier = Modifier.width(20.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.gs}", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.gc}", modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                val dg = team.gs - team.gc
                val dgText = if (dg > 0) "+$dg" else "$dg"
                Text(dgText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                Text("${team.points}", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold)
            }
            if (index < stats.size - 1) HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
        }
    }
}

private data class TeamStandings(
    val name: String,
    val flag: String,
    var points: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var gs: Int = 0,
    var gc: Int = 0
)

private fun calculateGroupStats(groupName: String, matches: List<Match>, results: Map<String, Pair<Int, Int>>): List<TeamStandings> {
    val groupMatches = matches.filter { it.group == groupName }
    val teams = groupMatches.flatMap { listOf(it.homeTeam to it.homeFlag, it.awayTeam to it.awayFlag) }.distinctBy { it.first }
    val std = teams.associate { it.first to TeamStandings(it.first, it.second) }.toMutableMap()
    
    groupMatches.forEach { m ->
        results[m.id]?.let { (hS, aS) ->
            val h = std[m.homeTeam]!!
            val a = std[m.awayTeam]!!
            h.gs += hS; h.gc += aS; a.gs += aS; a.gc += hS
            when {
                hS > aS -> { h.points += 3; h.wins++; a.losses++ }
                hS < aS -> { a.points += 3; a.wins++; h.losses++ }
                else -> { h.points += 1; a.points += 1; h.draws++; a.draws++ }
            }
        }
    }
    
    return std.values.sortedWith(
        compareByDescending<TeamStandings> { it.points }
            .thenByDescending { it.gs - it.gc }
            .thenByDescending { it.gs }
    )
}

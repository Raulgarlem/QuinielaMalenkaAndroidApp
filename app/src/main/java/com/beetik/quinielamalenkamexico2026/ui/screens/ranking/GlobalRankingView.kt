package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.beetik.quinielamalenkamexico2026.ui.theme.Success
import com.beetik.quinielamalenkamexico2026.ui.theme.Pending
import com.beetik.quinielamalenkamexico2026.ui.theme.Error as ErrorColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlobalRankingView(
    participants: List<Participant>,
    scores: Map<String, Int>, // Ignored for scoped calculation
    ranks: Map<String, Int>,  // Ignored for scoped calculation
    baseRanks: Map<String, Int>, // Ignored for scoped calculation
    allMatches: List<Match>,
    resultsMap: Map<String, MatchScore>,
    onSimulateResult: (Match, Int, Int) -> Unit,
    onMatchClick: (Match) -> Unit
) {
    var showTop5Mode by remember { mutableStateOf(true) }

    // Filter out "Added" quinielas (id starting with loaded_)
    val officialParticipants = remember(participants) {
        participants.filter { !it.id.startsWith("loaded_") }
    }

    // 1. Identify "featured" matches (Live or Next)
    val matchesToShow = remember(allMatches, resultsMap.size) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val now = Date()
        val calendar = Calendar.getInstance()
        
        val liveMatches = allMatches.filter { match ->
            try {
                val matchDate = sdf.parse("${match.date} ${match.time}")
                matchDate != null && now.after(matchDate) && {
                    calendar.time = matchDate
                    calendar.add(Calendar.HOUR_OF_DAY, 2)
                    now.before(calendar.time)
                }()
            } catch (e: Exception) { false }
        }

        if (liveMatches.isNotEmpty()) liveMatches
        else {
            val nextMatch = allMatches.firstOrNull { it.realHomeScore == null }
            if (nextMatch != null) listOf(nextMatch) else emptyList()
        }
    }

    // 2. Calculate Scoped Scores and Ranks (Up to simulated match only)
    val scopedData = remember(officialParticipants, allMatches, resultsMap.entries.toList(), matchesToShow) {
        val lastMatch = matchesToShow.lastOrNull()
        if (lastMatch == null) {
            val emptyMap = officialParticipants.associate { it.id to 0 }
            val emptyRank = officialParticipants.associate { it.id to 1 }
            return@remember Triple(emptyMap, emptyRank, emptyRank) to emptyMap
        }

        val timelineMatches = allMatches.filter { m ->
            (m.date < lastMatch.date) || (m.date == lastMatch.date && m.time <= lastMatch.time)
        }
        val timelineIds = timelineMatches.map { it.id }.toSet()
        val featuredIds = matchesToShow.map { it.id }.toSet()

        fun calculateScoped(useFeatured: Boolean): Triple<Map<String, Int>, Map<String, Int>, Map<String, Int>> {
            val activeResults = resultsMap.filter { (id, _) -> 
                timelineIds.contains(id) && (useFeatured || !featuredIds.contains(id))
            }
            
            val scores = officialParticipants.associate { p ->
                var pts = 0
                timelineMatches.forEach { match ->
                    activeResults[match.id]?.let { actual ->
                        pts += calculatePoints(p.predictions[match.id] ?: (0 to 0), actual)
                    }
                }
                allMatches.groupBy { it.group }.forEach { (gName, gMatches) ->
                    val gMatchesInTimeline = gMatches.filter { timelineIds.contains(it.id) }
                    if (gMatchesInTimeline.isNotEmpty() && gMatches.all { activeResults.containsKey(it.id) }) {
                        val winner = getGroupWinner(gName, allMatches, activeResults)?.first
                        if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                    }
                }
                p.id to pts
            }

            val sortedScores = scores.values.distinct().sortedDescending()
            val scoreToRank = sortedScores.withIndex().associate { it.value to it.index + 1 }
            val ranks = officialParticipants.associate { it.id to (scoreToRank[scores[it.id] ?: 0] ?: 1) }
            
            return Triple(scores, ranks, scores)
        }

        val current = calculateScoped(true)
        val base = calculateScoped(false)
        
        (Triple(current.first, current.second, base.second)) to base.first
    }

    val currentScores = scopedData.first.first
    val currentRanks = scopedData.first.second
    val baseRanks = scopedData.first.third
    val baseScores = scopedData.second

    val sortedByRank = officialParticipants.sortedBy { currentRanks[it.id] ?: 999 }
    val userIndex = sortedByRank.indexOfFirst { it.isUser }
    
    val displayParticipants = if (showTop5Mode) {
        val top5 = sortedByRank.take(5)
        if (userIndex >= 5 && userIndex != -1) top5 + sortedByRank[userIndex] else top5
    } else {
        if (userIndex != -1) {
            val total = sortedByRank.size
            val count = 6
            val idealStart = userIndex - 3
            val start = idealStart.coerceIn(0, (total - count).coerceAtLeast(0))
            sortedByRank.subList(start, (start + count).coerceAtMost(total))
        } else {
            sortedByRank.take(6)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF070707)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. RESULTADOS SIMULADOS
        items(matchesToShow) { match ->
            SimulatedMatchHeader(
                match = match,
                score = resultsMap[match.id],
                onClick = { onMatchClick(match) }
            )
        }

        // 2. CAMBIO EN EL RANKING
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CAMBIO EN EL RANKING", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E2228))
                            .clickable { showTop5Mode = !showTop5Mode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (showTop5Mode) "Top 5" else "Cerca de mí", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111418)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        displayParticipants.forEach { p ->
                            ParticipantRankItem(
                                participant = p,
                                rank = currentRanks[p.id] ?: 1,
                                score = currentScores[p.id] ?: 0,
                                prevRank = baseRanks[p.id] ?: currentRanks[p.id] ?: 1,
                                activeMatches = matchesToShow
                            )
                        }
                    }
                }
            }
        }

        // 3. BENEFICIADOS Y PERJUDICADOS
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val participantsData = officialParticipants.map { 
                    val curR = currentRanks[it.id] ?: 999
                    val basR = baseRanks[it.id] ?: curR
                    val ptsG = (currentScores[it.id] ?: 0) - (baseScores[it.id] ?: 0)
                    Triple(it, basR - curR, ptsG)
                }
                
                val rankUp = participantsData.filter { it.second > 0 }.sortedByDescending { it.second }
                val rankDown = participantsData.filter { it.second < 0 }.sortedBy { it.second }
                
                val showPointsInsteadOfRank = rankUp.isEmpty()
                
                val beneficiados = if (!showPointsInsteadOfRank) {
                    rankUp.take(4).map { it.first to it.second }
                } else {
                    participantsData.filter { it.third > 0 }.sortedByDescending { it.third }.take(4).map { it.first to it.third }
                }
                
                val perjudicados = rankDown.take(4).map { it.first to it.second }

                ImpactCard(
                    title = "BENEFICIADOS",
                    items = beneficiados,
                    color = Success,
                    modifier = Modifier.weight(1f),
                    usePoints = showPointsInsteadOfRank
                )
                ImpactCard(
                    title = "PERJUDICADOS",
                    items = perjudicados,
                    color = ErrorColor,
                    modifier = Modifier.weight(1f),
                    usePoints = false
                )
            }
        }

        // 4. ESCENARIOS RÁPIDOS
        item {
            Column {
                Text("ESCENARIOS RÁPIDOS", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                val targetMatch = matchesToShow.firstOrNull()
                val targetScore = targetMatch?.let { resultsMap[it.id] }
                val scenarios = listOf(1 to 0, 2 to 1, 3 to 1, 2 to 0, 1 to 1, 0 to 1)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                    items(scenarios) { (h, a) ->
                        val isSelected = targetScore?.home == h && targetScore?.away == a
                        Surface(
                            modifier = Modifier.width(65.dp).height(42.dp).clickable { targetMatch?.let { onSimulateResult(it, h, a) } },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Gold else Color(0xFF1E2228),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$h-$a", color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. INFO FOOTER
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF001A33).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF003366))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF3399FF), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("¿CÓMO SE CALCULAN LOS PUNTOS?", color = Color(0xFF3399FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Exacto: 2 pts | Resultado: 1 pt | Fallo: 0 pts", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedMatchHeader(match: Match, score: MatchScore?, onClick: () -> Unit) {
    val isSimulated = score != null && (score.home != match.realHomeScore || score.away != match.realAwayScore)
    val statusColor = if (isSimulated) Pending else Success
    val backgroundColor = if (isSimulated) Color(0xFF1A1408) else Color(0xFF081A08)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isSimulated) "RESULTADO SIMULADO" else "RESULTADO REAL", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(match.homeFlag, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(match.homeTeam.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${score?.home ?: match.realHomeScore ?: "-"}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Text("-", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${score?.away ?: match.realAwayScore ?: "-"}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Text(match.awayTeam.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Start, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.width(10.dp))
                Text(match.awayFlag, fontSize = 26.sp)
            }
        }
    }
}

@Composable
fun ParticipantRankItem(participant: Participant, rank: Int, score: Int, prevRank: Int, activeMatches: List<Match>) {
    val rankChange = prevRank - rank
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(28.dp).then(if (rank <= 3) Modifier.background(when (rank) { 1 -> Gold; 2 -> Color(0xFFC0C0C0); else -> Color(0xFFCD7F32) }, CircleShape) else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)), contentAlignment = Alignment.Center) {
            Text("$rank", color = if (rank <= 3) Color.Black else Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF24292E)), contentAlignment = Alignment.Center) {
            Text(participant.ownerName.take(1).uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = participant.quinielaName, color = if (participant.isUser) Gold else Color.White, fontWeight = if (participant.isUser) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "(${participant.ownerName}${if (participant.isUser) " - Tú" else ""})", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
            activeMatches.forEach { match ->
                val pred = participant.predictions[match.id]
                Text(
                    text = if (pred != null) "(${pred.first}-${pred.second})" else "(-)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text("$score pts", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(42.dp), horizontalArrangement = Arrangement.End) {
            if (rankChange != 0) {
                Icon(imageVector = if (rankChange > 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = if (rankChange > 0) Success else ErrorColor, modifier = Modifier.size(20.dp))
                Text("${if (rankChange > 0) rankChange else -rankChange}", color = if (rankChange > 0) Success else ErrorColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else { Text("-", color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(end = 4.dp)) }
        }
    }
}

@Composable
fun ImpactCard(title: String, items: List<Pair<Participant, Int>>, color: Color, modifier: Modifier = Modifier, usePoints: Boolean = false) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.04f)), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(10.dp))
            items.forEach { (p, valToShow) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF24292E)), contentAlignment = Alignment.Center) {
                        Text(p.ownerName.take(1).uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(p.ownerName, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    
                    if (usePoints) {
                        Text("+${valToShow} pts", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = if (valToShow > 0) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Text("${if (valToShow > 0) valToShow else -valToShow}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (items.isEmpty()) {
                Text("Sin cambios", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
    }
}

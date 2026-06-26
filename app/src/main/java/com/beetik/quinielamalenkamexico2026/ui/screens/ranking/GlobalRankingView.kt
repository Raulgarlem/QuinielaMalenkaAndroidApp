package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.*
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

enum class RankingFilter { TOP_5, NEAR_ME, GENERAL }

@Composable
fun GlobalRankingView(
    participants: List<Participant>,
    scores: Map<String, Int>, // Ignored for scoped calculation
    ranks: Map<String, Int>,  // Ignored for scoped calculation
    baseRanks: Map<String, Int>, // Ignored for scoped calculation
    allMatches: List<Match>,
    resultsMap: Map<String, MatchScore>,
    onSimulateResult: (Match, Int, Int) -> Unit,
    onClearSimulation: (Match) -> Unit,
    onMatchClick: (Match) -> Unit
) {
    var rankingFilter by remember { mutableStateOf(RankingFilter.TOP_5) }

    // Filter out "Added" quinielas (id starting with loaded_)
    val officialParticipants = remember(participants) {
        participants.filter { !it.id.startsWith("loaded_") }
    }

    // 1. Identify "featured" matches (Live or Next)
    val matchesToShow = remember(allMatches) {
        // 1. Prioridad: Partidos en VIVO
        val liveMatches = allMatches.filter { it.started && it.isActive }
        
        if (liveMatches.isNotEmpty()) {
            liveMatches
        } else {
            // 2. Si no hay en vivo, buscar el bloque de tiempo más próximo (no finalizado)
            val upcomingMatches = allMatches.filter { !it.finished }
                .sortedWith(compareBy({ it.date }, { it.time }))
            
            if (upcomingMatches.isNotEmpty()) {
                val firstUpcoming = upcomingMatches.first()
                upcomingMatches.filter { it.date == firstUpcoming.date && it.time == firstUpcoming.time }
            } else {
                if (allMatches.isNotEmpty()) listOf(allMatches.last()) else emptyList()
            }
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
            // Pre-calculate group winners for this scope
            val groups = allMatches.groupBy { it.group }
            val groupWinnersForScope = groups.keys.associateWith { gName ->
                val gMatches = groups[gName]!!
                val gMatchesInTimeline = gMatches.filter { timelineIds.contains(it.id) }
                
                val allHaveResults = gMatches.all { m ->
                    val isFeatured = featuredIds.contains(m.id)
                    if (isFeatured) {
                        if (useFeatured) getEffectiveScore(m, resultsMap) != null else false
                    } else {
                        getEffectiveScore(m, resultsMap) != null
                    }
                }
                
                if (allHaveResults) {
                    @Suppress("UNCHECKED_CAST")
                    val scopedResults = gMatches.associate { m ->
                        val isFeatured = featuredIds.contains(m.id)
                        val score = if (isFeatured) {
                            if (useFeatured) getEffectiveScore(m, resultsMap) else null
                        } else {
                            getEffectiveScore(m, resultsMap)
                        }
                        m.id to score
                    }.filterValues { it != null } as Map<String, MatchScore>
                    
                    getGroupWinner(gName, allMatches, scopedResults)?.first
                } else null
            }

            val scores = officialParticipants.associate { p ->
                var pts = 0
                timelineMatches.forEach { match ->
                    val isFeatured = featuredIds.contains(match.id)
                    val actual = if (isFeatured) {
                        if (useFeatured) getEffectiveScore(match, resultsMap) else null
                    } else {
                        getEffectiveScore(match, resultsMap)
                    }
                    
                    actual?.let { score ->
                        pts += calculatePoints(p.predictions[match.id] ?: (0 to 0), score)
                    }
                }
                
                groupWinnersForScope.forEach { (gName, winner) ->
                    if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
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
    
    val displayParticipants = when (rankingFilter) {
        RankingFilter.TOP_5 -> {
            val top5 = sortedByRank.take(5)
            if (userIndex >= 5 && userIndex != -1) top5 + sortedByRank[userIndex] else top5
        }
        RankingFilter.NEAR_ME -> {
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
        RankingFilter.GENERAL -> sortedByRank
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF070707)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. RESULTADOS SIMULADOS
        items(matchesToShow) { match ->
            val distribution = remember(match, resultsMap, officialParticipants) {
                val score = getEffectiveScore(match, resultsMap)
                if (score == null) null
                else {
                    var exact = 0
                    var result = 0
                    var miss = 0
                    officialParticipants.forEach { p ->
                        val pts = calculatePoints(p.predictions[match.id] ?: (0 to 0), score)
                        when (pts) {
                            2 -> exact++
                            1 -> result++
                            0 -> miss++
                        }
                    }
                    Triple(exact, result, miss)
                }
            }

            SimulatedMatchHeader(
                match = match,
                resultsMap = resultsMap,
                onClearSimulation = { onClearSimulation(match) },
                onClick = { onMatchClick(match) },
                distribution = distribution
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
                            .clickable {
                                rankingFilter = when (rankingFilter) {
                                    RankingFilter.TOP_5 -> RankingFilter.NEAR_ME
                                    RankingFilter.NEAR_ME -> RankingFilter.GENERAL
                                    RankingFilter.GENERAL -> RankingFilter.TOP_5
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val filterText = when (rankingFilter) {
                            RankingFilter.TOP_5 -> "Top 5"
                            RankingFilter.NEAR_ME -> "Cerca de mí"
                            RankingFilter.GENERAL -> "General"
                        }
                        Text(filterText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111418)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .heightIn(max = 330.dp)
                            .verticalScroll(scrollState)
                    ) {
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
                
                val rankUp = participantsData.filter { it.second > 0 }
                    .sortedWith(compareByDescending<Triple<Participant, Int, Int>> { it.second }
                        .thenBy { currentRanks[it.first.id] ?: 999 })
                
                val rankDown = participantsData.filter { it.second < 0 }
                    .sortedWith(compareBy<Triple<Participant, Int, Int>> { it.second }
                        .thenByDescending { currentRanks[it.first.id] ?: 0 })
                
                val showPointsInsteadOfRank = rankUp.isEmpty()
                
                val beneficiados = if (!showPointsInsteadOfRank) {
                    rankUp.map { it.first to it.second }
                } else {
                    participantsData.filter { it.third > 0 }
                        .sortedWith(compareByDescending<Triple<Participant, Int, Int>> { it.third }
                            .thenBy { currentRanks[it.first.id] ?: 999 })
                        .map { it.first to it.third }
                }
                
                val perjudicados = rankDown.map { it.first to it.second }

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
fun SimulatedMatchHeader(
    match: Match, 
    resultsMap: Map<String, MatchScore>, 
    onClearSimulation: () -> Unit, 
    onClick: () -> Unit,
    distribution: Triple<Int, Int, Int>? = null
) {
    val isSimulated = resultsMap.containsKey(match.id)
    val isLive = match.started && match.isActive
    val isFinished = match.finished
    
    val effectiveScore = getEffectiveScore(match, resultsMap)
    
    val statusColor = if (isSimulated) Pending else Success
    val backgroundColor = if (isSimulated) Color(0xFF1A1408) else Color(0xFF081A08)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Botón para borrar simulación (solo si existe)
            if (isSimulated) {
                TextButton(
                    onClick = { onClearSimulation() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isLive) "VOLVER A EN VIVO" else "BORRAR SIMULACIÓN",
                        color = Gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val label = when {
                    isSimulated -> "RESULTADO SIMULADO"
                    isFinished -> "RESULTADO FINAL"
                    isLive -> "EN CURSO (VIVO)"
                    else -> "PRÓXIMO PARTIDO"
                }
                Text(label, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(match.homeFlag, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(match.homeTeam.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${effectiveScore?.home ?: "-"}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 10.dp))
                    Text("-", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${effectiveScore?.away ?: "-"}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 10.dp))
                    Text(match.awayTeam.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Start, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(match.awayFlag, fontSize = 26.sp)
                }

                if (distribution != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DistributionItem(points = 2, count = distribution.first)
                        Spacer(modifier = Modifier.width(16.dp))
                        DistributionItem(points = 1, count = distribution.second)
                        Spacer(modifier = Modifier.width(16.dp))
                        DistributionItem(points = 0, count = distribution.third)
                    }
                }
            }
        }
    }
}

@Composable
fun DistributionItem(points: Int, count: Int) {
    val color = getPointColor(points)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("$count", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .heightIn(max = 160.dp)
                    .verticalScroll(scrollState)
            ) {
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
}

package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.beetik.quinielamalenkamexico2026.data.MatchRepository

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun KnockoutBracketView(
    allMatches: List<Match>,
    resultsMap: Map<String, Pair<Int, Int>>,
    matchResults: Map<String, com.beetik.quinielamalenkamexico2026.model.MatchResult> = emptyMap(),
    headerContent: (@Composable () -> Unit)? = null
) {
    var selectedRound by remember { mutableStateOf("16avos") }
    
    val rounds = listOf(
        "16avos" to "16AVOS",
        "Octavos" to "OCTAVOS",
        "Cuartos" to "CUARTOS",
        "Semis" to "SEMIFINALES",
        "Final" to "FINAL"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        if (headerContent != null) {
            headerContent()
        }
        ScrollableTabRow(
            selectedTabIndex = rounds.indexOfFirst { it.first == selectedRound }.coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = Gold,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                val index = rounds.indexOfFirst { it.first == selectedRound }.coerceAtLeast(0)
                if (index < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = Gold,
                        height = 2.dp
                    )
                }
            },
            modifier = Modifier.height(36.dp)
        ) {
            rounds.forEach { (id, label) ->
                Tab(
                    selected = selectedRound == id,
                    onClick = { selectedRound = id },
                    text = { 
                        Text(
                            label, 
                            fontWeight = if (selectedRound == id) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        ) 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val knockoutMatches = remember(allMatches) { allMatches.filter { !it.group.startsWith("Grupo") } }
            when (selectedRound) {
                "16avos" -> RoundMatchesList(knockoutMatches, "16avos de Final", matchResults)
                "Octavos" -> TreeBracketView(knockoutMatches, "Octavos de Final", "Cuartos de Final", matchResults)
                "Cuartos" -> TreeBracketView(knockoutMatches, "Cuartos de Final", "Semifinales", matchResults)
                "Semis" -> TreeBracketView(knockoutMatches, "Semifinales", "Final", matchResults)
                "Final" -> FinalBracketView(knockoutMatches, matchResults)
            }
        }
        
        // Champion Section
        val finalMatch = allMatches.find { it.id == "FIN" }
        val championName = remember(finalMatch) {
            if (finalMatch?.finished == true && finalMatch.realHomeScore != null && finalMatch.realAwayScore != null) {
                if (finalMatch.realHomeScore > finalMatch.realAwayScore) finalMatch.homeTeam else finalMatch.awayTeam
            } else "Por definir"
        }

        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).fillMaxWidth(),
            color = Gold.copy(alpha = 0.05f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, Gold.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("CAMPEÓN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Gold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (championName != "Por definir") {
                            Text(MatchRepository.getFlag(championName), fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Box(modifier = Modifier.size(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(championName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 0.dp).fillMaxWidth(),
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Gold.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Las llaves se actualizan conforme avanzan los partidos reales",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun RoundMatchesList(
    allMatches: List<Match>, 
    groupName: String,
    matchResults: Map<String, com.beetik.quinielamalenkamexico2026.model.MatchResult>
) {
    val matches = remember(allMatches) { allMatches.filter { it.group == groupName } }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(matches) { match ->
            BracketMatchCard(match, matchResults[match.id])
        }
    }
}

@Composable
fun TreeBracketView(
    allMatches: List<Match>, 
    currentStage: String, 
    nextStage: String,
    matchResults: Map<String, com.beetik.quinielamalenkamexico2026.model.MatchResult>
) {
    val currentMatches = remember(allMatches) { allMatches.filter { it.group == currentStage } }
    val nextMatches = remember(allMatches) { allMatches.filter { it.group == nextStage } }
    
    // Group current matches in pairs (each pair leads to one next match)
    val matchPairs = remember(currentMatches) {
        currentMatches.chunked(2)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        items(matchPairs.size) { index ->
            val pair = matchPairs[index]
            val nextMatch = nextMatches.getOrNull(index)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: The two matches of current stage
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    pair.forEach { match ->
                        BracketMatchCard(match, matchResults[match.id])
                    }
                }
                
                // Connecting lines representation
                Box(modifier = Modifier.width(30.dp).height(120.dp), contentAlignment = Alignment.Center) {
                    // Vertical line connecting the two matches
                    Box(modifier = Modifier.fillMaxHeight(0.6f).width(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
                    // Horizontal line leading to the next match
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.5f)))
                }

                // Right Column: The resulting match in next stage
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    if (nextMatch != null) {
                        BracketMatchCard(nextMatch, matchResults[nextMatch.id])
                    } else {
                        Box(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FinalBracketView(
    allMatches: List<Match>,
    matchResults: Map<String, com.beetik.quinielamalenkamexico2026.model.MatchResult>
) {
    val finalMatch = allMatches.find { it.id == "FIN" }
    val thirdPlace = allMatches.find { it.id == "3RD" }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (finalMatch != null) {
            Text("FINAL", style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
            BracketMatchCard(finalMatch, matchResults[finalMatch.id])
        }
        
        if (thirdPlace != null) {
            Text("TERCER LUGAR", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
            BracketMatchCard(thirdPlace, matchResults[thirdPlace.id])
        }
    }
}

@Composable
fun BracketMatchCard(match: Match, prediction: com.beetik.quinielamalenkamexico2026.model.MatchResult? = null) {
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            val roundDisplay = remember(match.group) {
                when {
                    match.group.contains("16avos") -> "16AVOS"
                    match.group.contains("Octavos") -> "OCTAVOS"
                    match.group.contains("Cuartos") -> "CUARTOS"
                    match.group.contains("Semifinales") -> "SEMIS"
                    match.group.contains("Tercer") -> "3ER LUGAR"
                    match.group.contains("Final") -> "FINAL"
                    else -> match.group.uppercase()
                }
            }
            
            Text(
                text = "$roundDisplay • ${match.time}",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TeamRow(flag = match.homeFlag, name = match.homeTeam, score = match.realHomeScore, isVisible = match.started || match.finished)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TeamRow(flag = match.awayFlag, name = match.awayTeam, score = match.realAwayScore, isVisible = match.started || match.finished)
            
            if (prediction != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.Gray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tú:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                    Text(
                        "${prediction.homeScore} - ${prediction.awayScore}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = match.date.split("-").let { if(it.size==3) "${it[2]} de ${when(it[1]){"06"->"Jun";"07"->"Jul";else->""}}" else match.date },
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 8.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TeamRow(flag: String, name: String, score: Int?, isVisible: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (name != "Por definir") {
            Text(flag, fontSize = 16.sp)
        } else {
            Box(modifier = Modifier.size(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            name, 
            style = MaterialTheme.typography.bodySmall, 
            fontWeight = FontWeight.Bold,
            color = if (name == "Por definir") Color.Gray else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = if (isVisible) (score?.toString() ?: "0") else "-", 
            style = MaterialTheme.typography.bodySmall, 
            fontWeight = FontWeight.ExtraBold, 
            color = if (isVisible) Gold else Color.Gray.copy(alpha = 0.5f)
        )
    }
}

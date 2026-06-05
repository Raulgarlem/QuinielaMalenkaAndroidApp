package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.R
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import java.text.SimpleDateFormat

import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.RankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(rankingViewModel: RankingViewModel = viewModel()) {
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    val allMatches = MatchRepository.allMatches
    val groupCount = remember { allMatches.groupBy { it.group }.size }
    
    val officialParticipants = rankingViewModel.baseParticipants.filter { !it.id.startsWith("loaded_") }
    
    var savedQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var selectedQuiniela by remember { mutableStateOf<QuinielaEntity?>(null) }
    var showQuinielaDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val all = database.quinielaDao().getAllQuinielas()
        savedQuinielas = all
        
        // Logic: Favorite -> Sent -> First Available
        selectedQuiniela = all.find { it.isFavorite } 
            ?: all.find { it.isSent }
            ?: all.firstOrNull()
    }

    // Timezone handling
    val mexicoCityZone = "America/Mexico_City"
    
    // Find next match logic
    val nextMatchData = remember(allMatches) {
        val now = Calendar.getInstance().time
        val sdfCDMX = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(mexicoCityZone)
        }
        val sdfLocal = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDayMonth = SimpleDateFormat("d MMM", Locale.getDefault())
        
        val match = allMatches.find { m ->
            try {
                val matchDate = sdfCDMX.parse("${m.date} ${m.time}")
                matchDate != null && matchDate.after(now)
            } catch (_: Exception) {
                false
            }
        } ?: allMatches.first()

        // Convert match time to local for display
        val matchDate = try {
            sdfCDMX.parse("${match.date} ${match.time}")
        } catch (_: Exception) {
            null
        }
        
        val localTime = matchDate?.let { sdfLocal.format(it) } ?: match.time
        val localDate = matchDate?.let { sdfDayMonth.format(it) } ?: "11 Jun"
        
        Triple(match, localTime, localDate)
    }
    
    val nextMatch = nextMatchData.first
    val displayTime = nextMatchData.second
    val displayDate = nextMatchData.third

    // --- Lógica de Estadísticas y Ranking ---
    val statsInfo = remember(selectedQuiniela, officialParticipants, allMatches) {
        val totalParticipantsGeneral = officialParticipants.size

        val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
        
        // 1. Determinar ganadores reales de grupos finalizados
        val matchesByGroup = allMatches.groupBy { it.group }
        val realGroupWinners = matchesByGroup.mapValues { (groupName, matches) ->
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

        fun calculateTotalPoints(matchPreds: Map<String, Any>, winnerPreds: Map<String, String>): Triple<Int, Int, Int> {
            var p = 0
            var h = 0
            var e = 0
            
            allMatches.forEach { match ->
                val rh = match.realHomeScore
                val ra = match.realAwayScore
                if (rh != null && ra != null) {
                    val pred = matchPreds[match.id]
                    val (uh, ua) = when (pred) {
                        is Pair<*, *> -> (pred.first as? Int) to (pred.second as? Int)
                        is MatchResult -> pred.homeScore.toIntOrNull() to pred.awayScore.toIntOrNull()
                        else -> null to null
                    }
                    
                    if (uh != null && ua != null) {
                        if (uh == rh && ua == ra) {
                            e++
                            h++
                            p += 2
                        } else {
                            val rW = when { rh > ra -> 1; rh < ra -> 2; else -> 0 }
                            val uW = when { uh > ua -> 1; uh < ua -> 2; else -> 0 }
                            if (rW == uW) {
                                h++
                                p += 1
                            }
                        }
                    }
                }
            }
            
            realGroupWinners.forEach { (group, realWinner) ->
                val isGroupFinished = matchesByGroup[group]?.all { it.realHomeScore != null } == true
                if (isGroupFinished && realWinner != null) {
                    if (winnerPreds[group] == realWinner) p += 2
                }
            }
            return Triple(p, h, e)
        }

        // 1. Calcular puntos de la quiniela seleccionada
        val currentQ = selectedQuiniela
        val userMatchPreds: Map<String, MatchResult> = if (currentQ != null) {
            try { gson.fromJson(currentQ.resultsJson, resultsType) } catch (_: Exception) { emptyMap() }
        } else emptyMap()
        val userWinnerPreds: Map<String, String> = if (currentQ != null) {
            val winnersType = object : TypeToken<Map<String, String>>() {}.type
            try { gson.fromJson(currentQ.winnersJson, winnersType) } catch (_: Exception) { emptyMap() }
        } else emptyMap()
        
        val currentStats = calculateTotalPoints(userMatchPreds, userWinnerPreds)
        
        // 2. Calcular puntos de los participantes oficiales de la tabla general
        val officialScores = officialParticipants.map { participant ->
            calculateTotalPoints(participant.predictions, participant.groupWinnerPredictions).first
        }
        
        // 3. Determinar posición (Igual que en RankingScreen para quinielas añadidas)
        val officialScoreToRank = officialScores.distinct().sortedByDescending { it }
            .withIndex().associate { it.value to it.index + 1 }
            
        val rank = if (currentQ != null) {
            val score = currentStats.first
            val matchScore = officialScoreToRank.keys.firstOrNull { it <= score }
            if (matchScore != null) {
                officialScoreToRank[matchScore] ?: 1
            } else {
                officialScoreToRank.size + 1
            }
        } else 0
        
        // 4. Efectividad (Puntos reales * 100 / Puntos máximos posibles hasta ahora)
        val finishedMatches = allMatches.count { it.realHomeScore != null }
        val finishedGroups = matchesByGroup.count { (group, matches) -> matches.all { it.realHomeScore != null } }
        val maxPointsPossible = (finishedMatches * 2) + (finishedGroups * 2)

        val effectiveness = if (maxPointsPossible > 0) (currentStats.first.toFloat() / maxPointsPossible * 100).toInt() else 0
        
        object {
            val points = currentStats.first
            val hits = currentStats.second
            val exacts = currentStats.third
            val eff = effectiveness
            val position = rank
            val total = totalParticipantsGeneral
        }
    }

    val selectedQuinielaStatus = remember(selectedQuiniela, statsInfo) {
        val quiniela = selectedQuiniela ?: return@remember Triple("Borrador", Color(0xFF9C27B0), false)
        
        val isComplete = try {
            val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
            val winnersType = object : TypeToken<Map<String, String>>() {}.type
            
            val results: Map<String, MatchResult> = gson.fromJson(quiniela.resultsJson, resultsType)
            val winners: Map<String, String> = gson.fromJson(quiniela.winnersJson, winnersType)
            
            val matchesDone = results.values.count { it.homeScore.isNotEmpty() && it.awayScore.isNotEmpty() }
            val winnersDone = winners.size
            val emailValid = quiniela.userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(quiniela.userEmail).matches()
            
            matchesDone == allMatches.size && winnersDone == groupCount && emailValid
        } catch (_: Exception) {
            false
        }

        when {
            quiniela.isSent -> Triple("Enviada", Gold, true)
            isComplete -> Triple("Completa", Color(0xFFFF9800), true)
            else -> Triple("Borrador", Color(0xFF9C27B0), false)
        }
    }

    // Days until start logic
    val daysUntilStart = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val startDate = sdf.parse("2026-06-11")
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            if (startDate != null && today.before(startDate)) {
                val diff = startDate.time - today.time
                diff / (1000 * 60 * 60 * 24)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "QUINIELA MALENKA 2026",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Gold,
                    navigationIconContentColor = Gold,
                    actionIconContentColor = Gold
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        "Hola Raúl 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (daysUntilStart > 0) "Faltan $daysUntilStart días para el inicio" else "¡El mundial ha comenzado!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "MI QUINIELAS OFICIAL",
                        subtitle = selectedQuiniela?.quinielaName ?: "(Sin Quiniela)",
                        status = selectedQuinielaStatus.first,
                        statusColor = selectedQuinielaStatus.second,
                        points = "${statsInfo?.points ?: 0} pts",
                        rank = if (statsInfo != null) "#${statsInfo.position} de ${statsInfo.total}" else "-",
                        isOfficial = selectedQuiniela?.isFavorite ?: false,
                        onSelectorClick = { showQuinielaDropdown = true }
                    )

                    // Alineamos el Dropdown a la derecha
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 8.dp)) {
                        DropdownMenu(
                            expanded = showQuinielaDropdown,
                            onDismissRequest = { showQuinielaDropdown = false }
                        ) {
                            if (savedQuinielas.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay quinielas") },
                                    onClick = { showQuinielaDropdown = false }
                                )
                            } else {
                                savedQuinielas.forEach { quiniela ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(quiniela.quinielaName)
                                                if (quiniela.isFavorite) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        painter = painterResource(id = android.R.drawable.star_on),
                                                        contentDescription = null,
                                                        tint = Gold,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedQuiniela = quiniela
                                            showQuinielaDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "PRÓXIMO PARTIDO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }

            item {
                MatchPreviewCard(nextMatch, displayDate, displayTime)
            }

            item {
                Text(
                    "ESTADÍSTICAS PERSONALES",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }
            
            item {
                StatsCard(
                    position = if (statsInfo != null) "#${statsInfo.position}" else "-",
                    total = if (statsInfo != null) "de ${statsInfo.total}" else "",
                    hits = "${statsInfo?.hits ?: 0}",
                    exacts = "${statsInfo?.exacts ?: 0}",
                    efficiency = "${statsInfo?.eff ?: 0}%"
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    points: String,
    rank: String,
    isOfficial: Boolean,
    onSelectorClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onSelectorClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOfficial) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.star_on),
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar quiniela", tint = Gold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Estado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        status, 
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Puntaje", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(points, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Posición", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rank, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MatchPreviewCard(match: Match, displayDate: String, displayTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(match.homeFlag, fontSize = 32.sp)
                Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(displayDate, style = MaterialTheme.typography.labelSmall)
                Text(displayTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(match.awayFlag, fontSize = 32.sp)
                Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun StatsCard(
    position: String,
    total: String,
    hits: String,
    exacts: String,
    efficiency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("Posición actual", position, total)
            StatItem("Aciertos", hits, "")
            StatItem("Exactos", exacts, "")
            StatItem("Efectividad", efficiency, "")
        }
    }
}

@Composable
fun StatItem(label: String, value: String, subValue: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subValue.isNotEmpty()) {
            Text(subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

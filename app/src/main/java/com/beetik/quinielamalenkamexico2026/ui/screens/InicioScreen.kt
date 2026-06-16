package com.beetik.quinielamalenkamexico2026.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.RankingViewModel
import com.beetik.quinielamalenkamexico2026.util.ScoreCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    rankingViewModel: RankingViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    val allMatches = rankingViewModel.allMatches
    val groupCount = remember(allMatches) { allMatches.groupBy { it.group }.size }
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
    
    // Find next matches logic (Live or upcoming)
    val nextMatchesData = remember(allMatches) {
        val now = Calendar.getInstance().time
        val mexicoCityZone = "America/Mexico_City"
        val sdfCDMX = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(mexicoCityZone)
        }
        val sdfLocal = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDayMonth = SimpleDateFormat("d MMM", Locale.getDefault())

        // 1. Prioridad: Partidos en VIVO
        val liveMatches = allMatches.filter { it.started && it.isActive }
        
        if (liveMatches.isNotEmpty()) {
            return@remember liveMatches.map { m ->
                val matchDate = try { sdfCDMX.parse("${m.date} ${m.time}") } catch (_: Exception) { null }
                val localTime = matchDate?.let { sdfLocal.format(it) } ?: m.time
                val localDate = matchDate?.let { sdfDayMonth.format(it) } ?: "11 Jun"
                Triple(m, localTime, localDate)
            }
        }

        // 2. Si no hay en vivo, buscar el bloque de tiempo más próximo (no finalizado)
        val upcomingMatches = allMatches.filter { !it.finished }
            .sortedWith(compareBy({ it.date }, { it.time }))
        
        if (upcomingMatches.isNotEmpty()) {
            val firstUpcoming = upcomingMatches.first()
            val simultaneousMatches = upcomingMatches.filter { it.date == firstUpcoming.date && it.time == firstUpcoming.time }
            
            return@remember simultaneousMatches.map { m ->
                val matchDate = try { sdfCDMX.parse("${m.date} ${m.time}") } catch (_: Exception) { null }
                val localTime = matchDate?.let { sdfLocal.format(it) } ?: m.time
                val localDate = matchDate?.let { sdfDayMonth.format(it) } ?: "11 Jun"
                Triple(m, localTime, localDate)
            }
        }

        // 3. Si todo terminó, mostrar los últimos
        listOf(Triple(allMatches.last(), allMatches.last().time, "Final"))
    }

    // --- Lógica de Estadísticas ---
    val statsInfo = remember(selectedQuiniela, allMatches, officialParticipants) {
        val currentQ = selectedQuiniela ?: return@remember null

        val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
        val winnersType = object : TypeToken<Map<String, String>>() {}.type
        
        // 1. Obtener predicciones de la quiniela seleccionada
        val userMatchPreds: Map<String, MatchResult> = try { 
            gson.fromJson(currentQ.resultsJson, resultsType) 
        } catch (_: Exception) { emptyMap() }
        
        val userWinnerPreds: Map<String, String> = try { 
            gson.fromJson(currentQ.winnersJson, winnersType) 
        } catch (_: Exception) { emptyMap() }
        
        // 2. Calcular puntos usando el ScoreCalculator (usa resultados reales de allMatches)
        val currentStats = ScoreCalculator.calculateStats(allMatches, userMatchPreds, userWinnerPreds)
        val currentPoints = currentStats.totalPoints

        // 3. Calcular puntos de todos los oficiales para determinar posición
        val officialScores = officialParticipants.map { p ->
            val pPreds = p.predictions.mapValues { (_, v) -> MatchResult(v.first.toString(), v.second.toString()) }
            ScoreCalculator.calculateStats(allMatches, pPreds, p.groupWinnerPredictions).totalPoints
        }

        val betterCount = officialScores.count { it > currentPoints }
        val positionReal = betterCount + 1
        
        val distinctScores = officialScores.distinct().sortedDescending()
        val positionTabla = (distinctScores.indexOfFirst { it <= currentPoints }.takeIf { it != -1 } ?: distinctScores.size) + 1
        
        val totalOfficial = officialScores.size

        // 4. Calcular efectividad basada en el progreso real del torneo
        val matchesByGroup = allMatches.groupBy { it.group }
        val finishedMatches = allMatches.count { it.finished }
        val finishedGroups = matchesByGroup.count { (group, matches) -> matches.all { it.finished } }
        val maxPointsPossible = (finishedMatches * 2) + (finishedGroups * 2)

        val effectiveness = if (maxPointsPossible > 0) (currentPoints.toFloat() / maxPointsPossible * 100).toInt() else 0
        
        object {
            val points = currentPoints
            val hits = currentStats.hits
            val exacts = currentStats.exacts
            val eff = effectiveness
            val posReal = positionReal
            val posTabla = positionTabla
            val total = totalOfficial
            val matches = finishedMatches
            val totalMatches = allMatches.size
            val maxPoints = maxPointsPossible
        }
    }

    val selectedQuinielaStatus = remember(selectedQuiniela) {
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
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications, 
                            contentDescription = "Notifications", 
                            tint = Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Gold,
                    navigationIconContentColor = Gold,
                    actionIconContentColor = Gold
                ),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (isLandscape) 24.dp else 56.dp,
                    bottom = 12.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 10.dp)
        ) {
            item {
                Column {
                    Text(
                        if (userViewModel.isLoggedIn) "Hola ${userViewModel.name} 👋" else "¡Hola! 👋",
                        style = if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (daysUntilStart > 0) "Faltan $daysUntilStart días para el inicio" else "¡El mundial ha comenzado!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        title = "MI QUINIELA",
                        subtitle = selectedQuiniela?.quinielaName ?: "(Sin Quiniela)",
                        status = selectedQuinielaStatus.first,
                        statusColor = selectedQuinielaStatus.second,
                        points = "${statsInfo?.points ?: 0} pts",
                        rank = if (statsInfo != null) "${statsInfo.posReal} / ${statsInfo.total}" else "-",
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
                    if (nextMatchesData.any { it.first.started && it.first.isActive }) "EN CURSO" else "PRÓXIMO PARTIDO",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }

            items(nextMatchesData.size) { index ->
                val (match, time, date) = nextMatchesData[index]
                MatchPreviewCard(match, date, time)
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
                    posReal = if (statsInfo != null) "${statsInfo.posReal}" else "-",
                    total = if (statsInfo != null) "/ ${statsInfo.total}" else "",
                    posTabla = if (statsInfo != null) "${statsInfo.posTabla}" else "-",
                    hits = "${statsInfo?.hits ?: 0}",
                    exacts = "${statsInfo?.exacts ?: 0}",
                    efficiency = "${statsInfo?.eff ?: 0}%",
                    matches = if (statsInfo != null) "${statsInfo.matches}" else "-",
                    totalMatches = if (statsInfo != null) "/ ${statsInfo.totalMatches}" else "",
                    points = if (statsInfo != null) "${statsInfo.points}" else "0",
                    maxPoints = if (statsInfo != null) "/ ${statsInfo.maxPoints}" else ""
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
        Column(modifier = Modifier.padding(12.dp)) {
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar quiniela", tint = Gold, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Estado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        status, 
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Column {
                    Text("Puntaje", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(points, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Posición", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(rank, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MatchPreviewCard(match: Match, displayDate: String, displayTime: String) {
    val isLive = match.started && match.isActive
    val isFinished = match.finished && !match.isActive

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
                if (isLive) {
                    Surface(color = Color(0xFFE91E63), shape = RoundedCornerShape(4.dp)) {
                        Text("VIVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${match.realHomeScore ?: 0} - ${match.realAwayScore ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE91E63))
                } else if (isFinished) {
                    Text("FINAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${match.realHomeScore ?: 0} - ${match.realAwayScore ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                } else {
                    Text(displayDate, style = MaterialTheme.typography.labelSmall)
                    Text(displayTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
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
    posReal: String,
    total: String,
    posTabla: String,
    hits: String,
    exacts: String,
    efficiency: String,
    matches: String,
    totalMatches: String,
    points: String,
    maxPoints: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem("Posición real", posReal, total)
                    StatItem("Aciertos", hits, "")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem("En la tabla", posTabla, "")
                    StatItem("Exactos", exacts, "")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem("Partidos", matches, totalMatches)
                    StatItem("Puntaje", points, maxPoints)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatItem("Efectividad", efficiency, "")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, subValue: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subValue.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(subValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

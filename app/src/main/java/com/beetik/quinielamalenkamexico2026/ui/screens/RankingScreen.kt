package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

data class Participant(
    val id: String,
    val quinielaName: String,
    val ownerName: String,
    val isUser: Boolean = false,
    val predictions: Map<String, Pair<Int, Int>>,
    val groupWinnerPredictions: Map<String, String>,
    val prevPosition: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen() {
    var selectedView by remember { mutableStateOf("Tabla") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    var isLiveRanking by remember { mutableStateOf(false) }
    
    val pinnedParticipantIds = remember { mutableStateListOf<String>() }
    var comparisonParticipantId by remember { mutableStateOf<String?>(null) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
    
    val allMatches = remember { 
        MatchRepository.allMatches.sortedWith(compareBy({ it.date }, { it.time })) 
    }
    
    // Key State: Official and Simulated results
    val resultsMap = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val confirmedIds = remember { mutableStateListOf<String>() }
    var matchToEdit by remember { mutableStateOf<Match?>(null) }

    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val savedQuinielas = remember { mutableStateListOf<QuinielaEntity>() }
    LaunchedEffect(Unit) {
        database.quinielaDao().getAllQuinielas().forEach { q ->
            if (savedQuinielas.none { it.id == q.id }) savedQuinielas.add(q)
        }
    }
    var showLoadQuinielaDialog by remember { mutableStateOf(false) }
    val baseParticipants = remember {
        mutableStateListOf(
            Participant("1", "Mi Quiniela", "Raúl (Tú)", true, mapOf("A1" to (3 to 1), "A2" to (1 to 0), "C1" to (3 to 0)), mapOf("Grupo A" to "México", "Grupo C" to "Brasil"), 5),
            Participant("2", "La Favorita", "Tania", false, mapOf("A1" to (2 to 1), "A2" to (1 to 1), "C1" to (2 to 0)), mapOf("Grupo A" to "México", "Grupo C" to "Brasil"), 1),
            Participant("3", "El Crack", "Roberto", false, mapOf("A1" to (1 to 1), "A2" to (2 to 1), "C1" to (1 to 1)), mapOf("Grupo A" to "Sudáfrica"), 9),
            Participant("4", "Gol Gana", "Luis", false, mapOf("A1" to (2 to 0), "A2" to (1 to 1), "C1" to (4 to 0)), mapOf("Grupo A" to "México", "Grupo C" to "Brasil"), 2),
            Participant("5", "Futbolera", "Karla", false, mapOf("A1" to (2 to 1), "A2" to (1 to 1), "C1" to (3 to 0)), mapOf("Grupo A" to "Corea del Sur", "Grupo C" to "Brasil"), 4),
            Participant("6", "El Experto", "Miguel", false, mapOf("A1" to (0 to 0), "A2" to (1 to 2), "C1" to (2 to 0)), mapOf("Grupo C" to "Brasil"), 6),
            Participant("7", "La Suertuda", "Ana", false, mapOf("A1" to (1 to 2), "A2" to (0 to 1), "C1" to (1 to 0)), mapOf("Grupo A" to "República Checa"), 7),
            Participant("8", "Invicto", "Diego", false, mapOf("A1" to (2 to 1), "A2" to (1 to 3), "C1" to (3 to 1)), mapOf("Grupo A" to "México", "Grupo C" to "Brasil"), 2),
            Participant("9", "El Mago", "Carlos", false, mapOf("A1" to (1 to 1), "A2" to (2 to 2), "C1" to (0 to 0)), emptyMap(), 10),
            Participant("10", "Estrella", "Sofía", false, emptyMap(), emptyMap(), 5),
            Participant("11", "Campeón", "Javier", false, emptyMap(), emptyMap(), 8),
            Participant("12", "Líder", "Elena", false, emptyMap(), emptyMap(), 9)
        )
    }

    val showAddButton by remember {
        derivedStateOf {
            savedQuinielas.isNotEmpty() && savedQuinielas.any { q -> baseParticipants.none { p -> p.id == "loaded_${q.id}" } }
        }
    }

    // ULTRA REACTIVE CALCULATION
    // derivedStateOf tracks every single change in resultsMap automatically
    val scoresAndRanks by remember {
        derivedStateOf {
            // Force tracking by reading map size
            resultsMap.size
            isLiveRanking
            
            val scores = baseParticipants.associate { p ->
                var pts = 0
                // Match points - Check EVERY match that has a result
                allMatches.forEach { match ->
                    resultsMap[match.id]?.let { actual ->
                        val pred = p.predictions[match.id] ?: (0 to 0) // Treat as 0-0 if not predicted
                        pts += calculatePoints(pred, actual)
                    }
                }
                // Group winner points
                allMatches.groupBy { it.group }.forEach { (gName, gMatches) ->
                    val hasResults = gMatches.any { it.id in resultsMap }
                    val isFinished = gMatches.all { it.id in resultsMap }
                    
                    if (hasResults && (isFinished || isLiveRanking)) {
                        val winner = getGroupWinner(gName, allMatches, resultsMap)?.first
                        if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                    }
                }
                p.id to pts
            }

            val officialParticipants = baseParticipants.filter { !it.id.startsWith("loaded_") }
            val officialScores = officialParticipants.associateWith { scores[it.id] ?: 0 }
            
            // Calculate official ranks
            val sortedOfficial = officialScores.toList().sortedByDescending { it.second }
            val ranks = mutableMapOf<String, Int>()
            var currentRank = 1
            for (i in sortedOfficial.indices) {
                if (i > 0 && sortedOfficial[i].second < sortedOfficial[i - 1].second) currentRank++
                ranks[sortedOfficial[i].first.id] = currentRank
            }

            // Calculate ranks for loaded participants (simulated)
            baseParticipants.filter { it.id.startsWith("loaded_") }.forEach { lp ->
                val score = scores[lp.id] ?: 0
                ranks[lp.id] = 1 + sortedOfficial.count { it.second > score }
            }

            // Calculate Match History Ranks
            val matchHistoryRanks = mutableMapOf<String, Map<String, Int>>()
            val runningScores = baseParticipants.associate { it.id to 0 }.toMutableMap()
            
            // Iterate through matches that have results in order
            allMatches.forEach { match ->
                val result = resultsMap[match.id]
                if (result != null) {
                    // Update scores for this match
                    baseParticipants.forEach { p ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        runningScores[p.id] = (runningScores[p.id] ?: 0) + calculatePoints(pred, result)
                    }
                    
                    // Check group bonus in chronological order
                    val groupMatches = allMatches.filter { it.group == match.group }
                    if (groupMatches.last().id == match.id && groupMatches.all { it.id in resultsMap }) {
                        val winner = getGroupWinner(match.group, allMatches, resultsMap)?.first
                        if (winner != null) {
                            baseParticipants.forEach { p ->
                                if (p.groupWinnerPredictions[match.group] == winner) {
                                    runningScores[p.id] = (runningScores[p.id] ?: 0) + 2
                                }
                            }
                        }
                    }
                    
                    // Calculate ranks at this point
                    val sortedOfficialStep = officialParticipants.associateWith { runningScores[it.id] ?: 0 }
                        .toList().sortedByDescending { it.second }
                    
                    val ranksStep = mutableMapOf<String, Int>()
                    var cR = 1
                    for (i in sortedOfficialStep.indices) {
                        if (i > 0 && sortedOfficialStep[i].second < sortedOfficialStep[i-1].second) cR++
                        ranksStep[sortedOfficialStep[i].first.id] = cR
                    }

                    // Simulated ranks for loaded ones
                    baseParticipants.filter { it.id.startsWith("loaded_") }.forEach { lp ->
                        val score = runningScores[lp.id] ?: 0
                        ranksStep[lp.id] = 1 + sortedOfficialStep.count { it.second > score }
                    }
                    
                    matchHistoryRanks[match.id] = ranksStep
                }
            }

            Triple(scores, ranks, matchHistoryRanks)
        }
    }

    val currentScores = scoresAndRanks.first
    val currentRanks = scoresAndRanks.second
    val matchHistoryRanks = scoresAndRanks.third

    val filteredParticipants by remember {
        derivedStateOf {
            val user = baseParticipants.first { it.isUser }
            val loaded = baseParticipants.filter { it.id.startsWith("loaded_") }
            val others = baseParticipants.filter { !it.isUser && !it.id.startsWith("loaded_") }
            
            val pinnedOthers = others.filter { it.id in pinnedParticipantIds }
                .sortedBy { pinnedParticipantIds.indexOf(it.id) }
            
            val remainingOthers = others.filter { it.id !in pinnedParticipantIds }
                .sortedByDescending { currentScores[it.id] ?: 0 }
            
            val list = when (selectedFilter) {
                "Top 5" -> {
                    val combined = (loaded + listOf(user) + pinnedOthers).distinctBy { it.id }
                    (combined + remainingOthers).take(5)
                }
                "Top 10" -> {
                    val combined = (loaded + listOf(user) + pinnedOthers).distinctBy { it.id }
                    (combined + remainingOthers).take(10)
                }
                else -> {
                    loaded + listOf(user) + pinnedOthers + remainingOthers
                }
            }

            if (searchQuery.isEmpty()) {
                list
            } else {
                list.filter {
                    it.quinielaName.contains(searchQuery, ignoreCase = true) || 
                    it.ownerName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    if (matchToEdit != null) {
        val match = matchToEdit!!
        var homeS by remember { mutableStateOf(resultsMap[match.id]?.first?.toString() ?: "") }
        var awayS by remember { mutableStateOf(resultsMap[match.id]?.second?.toString() ?: "") }
        val homeFocusRequester = remember { FocusRequester() }
        val awayFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            homeFocusRequester.requestFocus()
        }

        val onConfirm = {
            val h = homeS.toIntOrNull()
            val a = awayS.toIntOrNull()
            if (h != null && a != null) {
                resultsMap[match.id] = h to a
            } else {
                resultsMap.remove(match.id)
            }
            matchToEdit = null
        }

        AlertDialog(
            onDismissRequest = { matchToEdit = null },
            title = { Text("Actualizar Resultado", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(match.homeFlag, fontSize = 40.sp); Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                        Text("VS", fontWeight = FontWeight.Bold, color = Gold)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text(match.awayFlag, fontSize = 40.sp); Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = homeS, onValueChange = { if (it.length <= 2) homeS = it.filter { c -> c.isDigit() } }, modifier = Modifier.width(64.dp).focusRequester(homeFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { awayFocusRequester.requestFocus() }), singleLine = true)
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = awayS, onValueChange = { if (it.length <= 2) awayS = it.filter { c -> c.isDigit() } }, modifier = Modifier.width(64.dp).focusRequester(awayFocusRequester), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onConfirm() }), singleLine = true)
                    }
                }
            },
            confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { matchToEdit = null }) { Text("Cancelar") } }
        )
    }

    if (showLoadQuinielaDialog) {
        LoadQuinielaDialog(
            savedQuinielas = savedQuinielas,
            onDismiss = { showLoadQuinielaDialog = false },
            onQuinielaSelected = { entity ->
                val newParticipant = entity.toParticipant()
                if (baseParticipants.none { it.id == newParticipant.id }) {
                    baseParticipants.add(0, newParticipant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF121212)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar", tint = Gold, modifier = Modifier.size(18.dp)) 
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar quiniela o usuario...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Gold,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) { 
                                Icon(Icons.Default.Close, "Limpiar", tint = Color.Gray, modifier = Modifier.size(16.dp)) 
                            }
                        }
                    } else {
                        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Gold, modifier = Modifier.size(18.dp)) 
                        }
                        Text("QUINIELA MALENKA 2026", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        IconButton(onClick = { isSearchActive = true }, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.Default.Search, "Buscar", tint = Color.White, modifier = Modifier.size(18.dp)) 
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.Default.Settings, "Ajustes", tint = Color.White, modifier = Modifier.size(16.dp)) 
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(2.dp)) {
                    ViewTabSmall("Tabla", selectedView == "Tabla") { selectedView = "Tabla" }
                    ViewTabSmall("Tarjetas", selectedView == "Tarjetas") { selectedView = "Tarjetas" }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Cálculo en vivo",
                        tint = if (isLiveRanking) Gold else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Switch(
                        checked = isLiveRanking,
                        onCheckedChange = { isLiveRanking = it },
                        modifier = Modifier.scale(0.5f).height(20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold,
                            checkedTrackColor = Gold.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filtro: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                    FilterDropdown(selectedFilter) { selectedFilter = it }
                }
            }

            if (selectedView == "Tabla") {
                TableView(
                    allMatches, 
                    filteredParticipants, 
                    resultsMap, 
                    confirmedIds, 
                    currentScores, 
                    currentRanks, 
                    matchHistoryRanks,
                    pinnedParticipantIds,
                    comparisonParticipantId = comparisonParticipantId,
                    showAddButton = showAddButton,
                    isLiveRanking = isLiveRanking,
                    onEditResult = { matchToEdit = it },
                    onAddParticipant = { showLoadQuinielaDialog = true },
                    onRemoveParticipant = { id -> 
                        if (comparisonParticipantId == id) comparisonParticipantId = null
                        baseParticipants.removeAll { it.id == id } 
                    },
                    onToggleComparison = { id ->
                        comparisonParticipantId = if (comparisonParticipantId == id) null else id
                    }
                )
            } else {
                CardsView(
                    allMatches.first(), 
                    filteredParticipants, 
                    resultsMap, 
                    currentScores, 
                    currentRanks,
                    pinnedParticipantIds,
                    showAddButton = showAddButton,
                    isLiveRanking = isLiveRanking,
                    onAddParticipant = { showLoadQuinielaDialog = true },
                    onRemoveParticipant = { id -> baseParticipants.removeAll { it.id == id } }
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Todas", "Top 10", "Top 5")
    Box {
        Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1E1E1E)).clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(selected, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Gold, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E1E1E))) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option, fontSize = 12.sp, color = if(option == selected) Gold else Color.White) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun ViewTabSmall(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selected) Gold else Color.Transparent).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
fun TableView(
    matches: List<Match>, 
    participants: List<Participant>, 
    resultsMap: Map<String, Pair<Int, Int>>,
    confirmedIds: List<String>,
    scores: Map<String, Int>,
    ranks: Map<String, Int>,
    matchHistoryRanks: Map<String, Map<String, Int>>,
    pinnedIds: SnapshotStateList<String>,
    comparisonParticipantId: String?,
    showAddButton: Boolean,
    isLiveRanking: Boolean,
    onEditResult: (Match) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onToggleComparison: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val addButtonWidthPx = with(density) { 100.dp.toPx() } // Increased width for longer pull

    val compP = participants.find { it.id == comparisonParticipantId }
    val scrollableParticipants = participants.filter { it.id != comparisonParticipantId }

    LaunchedEffect(participants.size) {
        scrollState.scrollTo(addButtonWidthPx.toInt())
    }

    // Snapping logic for "Add" button reveal
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            val current = scrollState.value.toFloat()
            if (current > 0 && current < addButtonWidthPx) {
                // Ultra high threshold: Must pull 95% of the distance to stay revealed
                if (current < addButtonWidthPx * 0.05f) {
                    scrollState.animateScrollTo(0)
                } else {
                    scrollState.animateScrollTo(addButtonWidthPx.toInt())
                }
            }
        }
    }

    val lastMatchIdsByGroup = remember(matches) { matches.groupBy { it.group }.mapValues { it.value.last().id }.values.toSet() }
    
    // Smooth Drag and Drop state
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.background(Color(0xFF121212)).padding(vertical = 4.dp)) {
            Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) { Text("Partido", color = Color.Gray, fontSize = 10.sp) }
            Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) { Text("Resultado", color = Color.Gray, fontSize = 10.sp) }
            
            compP?.let { p ->
                ParticipantColumnHeader(
                    p = p, 
                    isPinned = false, 
                    draggingId = null, 
                    dragOffset = 0f, 
                    density = density, 
                    pinnedIds = pinnedIds,
                    isComparison = true,
                    onToggleComparison = onToggleComparison,
                    onRemoveParticipant = onRemoveParticipant
                )
            }

            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                // Add button column at the start
                if (showAddButton) {
                    Column(
                        modifier = Modifier
                            .width(100.dp)
                            .clickable { onAddParticipant() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Gold, modifier = Modifier.size(16.dp))
                        }
                        Text("Añadir", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                scrollableParticipants.forEach { p ->
                    key(p.id) {
                        val isPinned = pinnedIds.contains(p.id)
                        val isReorderable = isPinned && !p.isUser
                        var offsetY by remember { mutableFloatStateOf(0f) }
                        
                        ParticipantColumnHeader(
                            p = p,
                            isPinned = isPinned,
                            draggingId = draggingId,
                            dragOffset = dragOffset,
                            offsetY = offsetY,
                            isReorderable = isReorderable,
                            density = density,
                            pinnedIds = pinnedIds,
                            isComparison = false,
                            onSetDraggingId = { draggingId = it },
                            onSetDragOffset = { dragOffset = it },
                            onSetOffsetY = { offsetY = it },
                            onToggleComparison = onToggleComparison,
                            onRemoveParticipant = onRemoveParticipant
                        )
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            matches.forEach { match ->
                item(key = match.id) {
                    val actual = resultsMap[match.id]
                    val isConfirmed = confirmedIds.contains(match.id)
                    val isSimulated = actual != null && !isConfirmed
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).background(Color(0xFF121212).copy(alpha = 0.5f)), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.width(90.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(match.homeFlag, fontSize = 14.sp); Text(" vs ", color = Color.Gray, fontSize = 10.sp); Text(match.awayFlag, fontSize = 14.sp)
                        }
                        Box(modifier = Modifier.width(60.dp).clickable(enabled = !isConfirmed) { onEditResult(match) }, contentAlignment = Alignment.Center) {
                            Surface(color = when { isConfirmed -> Color(0xFF004D40); isSimulated -> Color(0xFFFF9800).copy(alpha = 0.2f); else -> Color(0xFF333333) }, shape = RoundedCornerShape(4.dp), border = if (isSimulated) BorderStroke(1.dp, Color(0xFFFF9800)) else null) {
                                Text(text = actual?.let { "${it.first}-${it.second}" } ?: "-", color = when { isConfirmed -> Color(0xFF4CAF50); isSimulated -> Color(0xFFFF9800); else -> Color.White }, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        compP?.let { p ->
                            PredictionCell(p, match, actual, matchHistoryRanks)
                        }

                        Row(modifier = Modifier.horizontalScroll(scrollState)) {
                            // Placeholder for Add column at start
                            if (showAddButton) {
                                Box(modifier = Modifier.width(100.dp))
                            }
                            scrollableParticipants.forEach { p ->
                                PredictionCell(p, match, actual, matchHistoryRanks)
                            }
                        }
                    }
                }
                if (lastMatchIdsByGroup.contains(match.id)) {
                    item(key = "winner_${match.group}") {
                        val actualWinner = remember(resultsMap.size, resultsMap.values.toList(), match.group) { getGroupWinner(match.group, matches, resultsMap) }
                        val groupMatches = matches.filter { it.group == match.group }
                        val isGroupFinished = groupMatches.all { it.id in resultsMap }
                        val showWinnerResult = isGroupFinished || isLiveRanking

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Gold.copy(alpha = 0.1f)), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp)); Text(actualWinner?.second ?: "🏳️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp)); Text("Ganador ${match.group}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            compP?.let { p ->
                                GroupWinnerPredictionCell(p, match.group, actualWinner?.first, showWinnerResult)
                            }

                            Row(modifier = Modifier.horizontalScroll(scrollState)) { 
                                if (showAddButton) {
                                    Box(modifier = Modifier.width(100.dp)) // Placeholder for Add column at start
                                }
                                scrollableParticipants.forEach { p ->
                                    GroupWinnerPredictionCell(p, match.group, actualWinner?.first, showWinnerResult)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.background(Color(0xFF121212))) {
            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) { Text("TOTAL PUNTOS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                
                compP?.let { p ->
                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) { Text((scores[p.id] ?: 0).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }

                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    if (showAddButton) {
                        Box(modifier = Modifier.width(100.dp)) // Placeholder for Add column
                    }
                    scrollableParticipants.forEach { p ->
                        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) { Text((scores[p.id] ?: 0).toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) { Text("POSICIÓN", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                
                compP?.let { p ->
                    RankBadge(ranks[p.id] ?: 1, p.id.startsWith("loaded_"))
                }

                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    if (showAddButton) {
                        Box(modifier = Modifier.width(100.dp)) // Placeholder for Add column
                    }
                    scrollableParticipants.forEach { p ->
                        RankBadge(ranks[p.id] ?: 1, p.id.startsWith("loaded_"))
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            LegendItem(2, "Marcador Exacto"); LegendItem(2, "Ganador Grupo"); LegendItem(1, "Resultado Correcto"); LegendItem(0, "Resultado Incorrecto")
        }
    }
}

@Composable
fun LegendItem(pts: Int, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { PointTagSmall(pts); Text(" $label", color = Color.Gray, fontSize = 8.sp) } }

@Composable
fun PointTagSmall(points: Int) { Surface(color = getPointColor(points).copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) { Text("+$points", color = getPointColor(points), modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun CardsView(
    match: Match, 
    participants: List<Participant>, 
    resultsMap: Map<String, Pair<Int, Int>>, 
    scores: Map<String, Int>, 
    ranks: Map<String, Int>,
    pinnedIds: SnapshotStateList<String>,
    showAddButton: Boolean,
    isLiveRanking: Boolean,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit
) {
    val actual = resultsMap[match.id]
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val addButtonWidthPx = with(density) { 140.dp.toPx() } // Card width

    LaunchedEffect(participants.size) {
        lazyListState.scrollToItem(1)
    }

    // Snapping logic for "Add" card reveal
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val offset = lazyListState.firstVisibleItemScrollOffset.toFloat()
            val index = lazyListState.firstVisibleItemIndex
            
            if (index == 0) {
                // Ultra high threshold: Must pull 95% of the card to stay revealed
                if (offset > addButtonWidthPx * 0.05f) {
                    // Mostly hidden, snap to hide
                    lazyListState.animateScrollToItem(1)
                } else {
                    // Snap to reveal
                    lazyListState.animateScrollToItem(0)
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Card(modifier = Modifier.width(120.dp).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("PARTIDO", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(match.homeFlag, fontSize = 24.sp); Text("VS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(match.awayFlag, fontSize = 24.sp)
                Text("Resultado", color = Color.Gray, fontSize = 9.sp); Text(actual?.let{"${it.first}-${it.second}"}?:"-", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f)); Button(onClick = {}, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(0.dp)) { Text("Ver", fontSize = 10.sp) }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        LazyRow(state = lazyListState, modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showAddButton) {
                item {
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight()
                            .clickable { onAddParticipant() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)), 
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Add, null, tint = Gold, modifier = Modifier.size(32.dp))
                            Text("Cargar Quiniela", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(participants, key = { it.id }) { p ->
                val isPinned = pinnedIds.contains(p.id)
                val isDragging = draggingId == p.id
                val isReorderable = isPinned && !p.isUser
                val isLoaded = p.id.startsWith("loaded_")
                var offsetY by remember { mutableFloatStateOf(0f) }

                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .offset { IntOffset(0, offsetY.roundToInt()) }
                        .zIndex(if (isDragging || offsetY != 0f) 10f else 1f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffset else 0f
                            if (isDragging) {
                                scaleX = 1.1f
                                scaleY = 1.1f
                                shadowElevation = 12f
                                alpha = 0.8f
                            }
                        }
                        .pointerInput(p.id, isPinned) {
                            detectTapGestures(
                                onTap = {
                                    if (!p.isUser && !isLoaded) {
                                        if (isPinned) pinnedIds.remove(p.id)
                                        else pinnedIds.add(p.id)
                                    }
                                }
                            )
                        }
                        .pointerInput(p.id) {
                            if (isLoaded) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount.y
                                        if (kotlin.math.abs(offsetY) > 150f) {
                                            onRemoveParticipant(p.id)
                                        }
                                    },
                                    onDragEnd = { offsetY = 0f },
                                    onDragCancel = { offsetY = 0f }
                                )
                            }
                        }
                        .pointerInput(p.id, isReorderable) {
                            if (isReorderable) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        draggingId = p.id
                                        dragOffset = 0f 
                                        offsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.x
                                        offsetY += dragAmount.y
                                        
                                        if (kotlin.math.abs(offsetY) > 120f) {
                                            pinnedIds.remove(p.id)
                                            draggingId = null
                                            offsetY = 0f
                                        } else {
                                            val currentIdx = pinnedIds.indexOf(p.id)
                                            if (currentIdx != -1) {
                                                // Card width is 140.dp. Using roughly 420px as threshold (3x factor)
                                                val step = 420f 
                                                if (dragOffset > step / 2 && currentIdx < pinnedIds.size - 1) {
                                                    val movedItem = pinnedIds.removeAt(currentIdx)
                                                    pinnedIds.add(currentIdx + 1, movedItem)
                                                    dragOffset -= step
                                                } else if (dragOffset < -step / 2 && currentIdx > 0) {
                                                    val movedItem = pinnedIds.removeAt(currentIdx)
                                                    pinnedIds.add(currentIdx - 1, movedItem)
                                                    dragOffset += step
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = { draggingId = null; dragOffset = 0f; offsetY = 0f },
                                    onDragCancel = { draggingId = null; dragOffset = 0f; offsetY = 0f }
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), 
                    border = if (p.isUser || isPinned) BorderStroke(1.dp, Gold) else if (isLoaded) BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f)) else null
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val r = ranks[p.id] ?: 1
                        val bgColor = when {
                            r == 1 -> Gold
                            r == 2 -> Color(0xFFC0C0C0) // Silver
                            isLoaded -> Color(0xFF2196F3).copy(alpha = 0.5f)
                            else -> Color(0xFF333333)
                        }
                        Surface(color = bgColor, shape = CircleShape, modifier = Modifier.size(20.dp).align(Alignment.Start)) { 
                            Box(contentAlignment = Alignment.Center) { 
                                Text("$r", color = if (r <= 2 && !isLoaded) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) 
                            } 
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isPinned) Gold.copy(alpha = 0.2f) else if (isLoaded) Color(0xFF2196F3).copy(alpha = 0.2f) else Color.Gray)
                                .let { if (isPinned) it.border(1.dp, Gold, CircleShape) else if (isLoaded) it.border(1.dp, Color(0xFF2196F3), CircleShape) else it }, 
                            contentAlignment = Alignment.Center
                        ) { Text(p.ownerName.first().toString(), fontSize = 18.sp, color = Color.White) }
                        
                        val textColor = if (p.isUser || isPinned) Gold else if (isLoaded) Color(0xFF2196F3) else Color.White
                        Text(
                            text = p.quinielaName, 
                            color = textColor, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "(${p.ownerName})", 
                            color = textColor.copy(alpha = 0.8f), 
                            fontSize = 10.sp, 
                            textAlign = TextAlign.Center
                        )

                        if (isPinned) Text("FIJADO", color = Gold, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        if (isLoaded) Text("SIMULACIÓN", color = Color(0xFF2196F3), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp)); Text("${scores[p.id] ?: 0} pts", color = if (isLoaded) Color(0xFF2196F3) else Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

fun calculatePoints(pred: Pair<Int, Int>, actual: Pair<Int, Int>): Int {
    if (pred == actual) return 2
    val pW = when { pred.first > pred.second -> 1; pred.first < pred.second -> 2; else -> 0 }
    val aW = when { actual.first > actual.second -> 1; actual.first < actual.second -> 2; else -> 0 }
    return if (pW == aW) 1 else 0
}

@Composable
fun GroupWinnerPredictionCell(
    p: Participant,
    groupName: String,
    actualWinnerName: String?,
    isGroupFinished: Boolean
) {
    val teamName = p.groupWinnerPredictions[groupName] ?: "-"
    val flag = if (teamName == "-") "-" else MatchRepository.getFlag(teamName)
    val isCorrect = isGroupFinished && actualWinnerName != null && teamName == actualWinnerName
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = flag, 
                fontSize = 16.sp
            )
            if (isCorrect) {
                Spacer(modifier = Modifier.width(2.dp))
                PointTagSmall(2)
            } else if (isGroupFinished && teamName != "-") {
                Spacer(modifier = Modifier.width(2.dp))
                PointTagSmall(0)
            }
        }
    }
}

@Composable
fun ParticipantColumnHeader(
    p: Participant,
    isPinned: Boolean,
    density: androidx.compose.ui.unit.Density,
    pinnedIds: SnapshotStateList<String>,
    isComparison: Boolean,
    onToggleComparison: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    draggingId: String? = null,
    dragOffset: Float = 0f,
    offsetY: Float = 0f,
    isReorderable: Boolean = false,
    onSetDraggingId: (String?) -> Unit = {},
    onSetDragOffset: (Float) -> Unit = {},
    onSetOffsetY: (Float) -> Unit = {}
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentOffsetY by rememberUpdatedState(offsetY)
    
    Column(
        modifier = Modifier
            .width(80.dp)
            .offset { IntOffset(0, currentOffsetY.roundToInt()) }
            .zIndex(if (draggingId == p.id || currentOffsetY != 0f) 10f else 1f)
            .graphicsLayer {
                translationX = if (draggingId == p.id) currentDragOffset else 0f
                if (draggingId == p.id) {
                    scaleX = 1.15f
                    scaleY = 1.15f
                    shadowElevation = 12f
                    alpha = 0.85f
                }
            }
            .pointerInput(p.id, isPinned, isComparison) {
                detectTapGestures(
                    onTap = {
                        if (p.isUser || p.id.startsWith("loaded_")) {
                            onToggleComparison(p.id)
                        } else {
                            if (isPinned) pinnedIds.remove(p.id)
                            else pinnedIds.add(p.id)
                        }
                    }
                )
            }
            .pointerInput(p.id) {
                if (p.id.startsWith("loaded_")) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onSetOffsetY(currentOffsetY + dragAmount.y)
                            if (kotlin.math.abs(currentOffsetY + dragAmount.y) > 150f) {
                                onRemoveParticipant(p.id)
                            }
                        },
                        onDragEnd = { onSetOffsetY(0f) },
                        onDragCancel = { onSetOffsetY(0f) }
                    )
                }
            }
            .pointerInput(p.id, isReorderable) {
                if (isReorderable) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { 
                            onSetDraggingId(p.id)
                            onSetDragOffset(0f)
                            onSetOffsetY(0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newDragOffset = currentDragOffset + dragAmount.x
                            val newOffsetY = currentOffsetY + dragAmount.y
                            onSetDragOffset(newDragOffset)
                            onSetOffsetY(newOffsetY)
                            
                            if (kotlin.math.abs(newOffsetY) > 120f) { // Desacoplar con un tirón vertical
                                pinnedIds.remove(p.id)
                                onSetDraggingId(null)
                                onSetOffsetY(0f)
                            } else {
                                val currentIdx = pinnedIds.indexOf(p.id)
                                if (currentIdx != -1) {
                                    val step = with(density) { 80.dp.toPx() }
                                    if (newDragOffset > step / 2 && currentIdx < pinnedIds.size - 1) {
                                        val movedItem = pinnedIds.removeAt(currentIdx)
                                        pinnedIds.add(currentIdx + 1, movedItem)
                                        onSetDragOffset(newDragOffset - step)
                                    } else if (newDragOffset < -step / 2 && currentIdx > 0) {
                                        val movedItem = pinnedIds.removeAt(currentIdx)
                                        pinnedIds.add(currentIdx - 1, movedItem)
                                        onSetDragOffset(newDragOffset + step)
                                    }
                                }
                            }
                        },
                        onDragEnd = { onSetDraggingId(null); onSetDragOffset(0f); onSetOffsetY(0f) },
                        onDragCancel = { onSetDraggingId(null); onSetDragOffset(0f); onSetOffsetY(0f) }
                    )
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComparison -> Gold.copy(alpha = 0.6f)
                        isPinned -> Gold.copy(alpha = 0.3f)
                        p.id.startsWith("loaded_") -> Color(0xFF2196F3).copy(alpha = 0.3f)
                        else -> Color.Gray
                    }
                ), 
            contentAlignment = Alignment.Center
        ) { 
            Text(p.ownerName.first().toString(), color = Color.White, fontSize = 10.sp) 
            if (isPinned || isComparison) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(if (isComparison) 2.dp else 1.dp, Gold, CircleShape)
                )
            } else if (p.id.startsWith("loaded_")) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.8f), CircleShape)
                )
            }
        }
        val textColor = if (p.isUser || isComparison) Gold else if (isPinned) Gold.copy(alpha = 0.8f) else if (p.id.startsWith("loaded_")) Color(0xFF2196F3) else Color.White
        Text(
            text = p.quinielaName, 
            color = textColor, 
            fontSize = 9.sp, 
            textAlign = TextAlign.Center,
            fontWeight = if (isPinned || isComparison) FontWeight.Bold else FontWeight.Normal,
            lineHeight = 10.sp
        )
        Text(
            text = "(${p.ownerName})", 
            color = textColor.copy(alpha = 0.7f), 
            fontSize = 7.sp, 
            textAlign = TextAlign.Center,
            lineHeight = 8.sp
        )
    }
}

@Composable
fun PredictionCell(
    p: Participant,
    match: Match,
    actual: Pair<Int, Int>?,
    matchHistoryRanks: Map<String, Map<String, Int>>
) {
    val pred = p.predictions[match.id] ?: (0 to 0)
    val pts = if (actual != null) calculatePoints(pred, actual) else 0
    
    // Historical highlight logic
    val historicalRank = matchHistoryRanks[match.id]?.get(p.id)
    val cellBg = when (historicalRank) {
        1 -> Gold.copy(alpha = 0.15f)
        2 -> Color(0xFF2196F3).copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .width(80.dp)
            .background(cellBg)
            .padding(vertical = 2.dp), 
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${pred.first}-${pred.second}", color = Color.White, fontSize = 11.sp)
        if (actual != null) { Spacer(modifier = Modifier.width(2.dp)); PointTagSmall(pts) }
    }
}

@Composable
fun RankBadge(rank: Int, isLoaded: Boolean) {
    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
        val bgColor = when {
            rank == 1 -> Gold
            rank == 2 -> Color(0xFFC0C0C0) // Silver
            isLoaded -> Color(0xFF2196F3).copy(alpha = 0.5f)
            else -> Color(0xFF333333)
        }
        Surface(color = bgColor, shape = RoundedCornerShape(3.dp)) { 
            Text("${rank}°", color = if (rank <= 2 && !isLoaded) Color.Black else Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp) 
        }
    }
}

fun getPointColor(points: Int): Color = when (points) { 2 -> Color(0xFF4CAF50); 1 -> Color(0xFF2196F3); else -> Color.Gray }

private data class TeamStats(val name: String, val flag: String, var points: Int = 0, var gs: Int = 0, var gc: Int = 0)

private fun getGroupWinner(groupName: String, matches: List<Match>, results: Map<String, Pair<Int, Int>>): Pair<String, String>? {
    val gM = matches.filter { it.group == groupName }
    val teams = gM.flatMap { listOf(it.homeTeam to it.homeFlag, it.awayTeam to it.awayFlag) }.distinctBy { it.first }
    val std = teams.associate { it.first to TeamStats(it.first, it.second) }.toMutableMap()
    gM.forEach { m ->
        results[m.id]?.let { (hS, aS) ->
            val h = std[m.homeTeam]!!; val a = std[m.awayTeam]!!
            h.gs += hS; h.gc += aS; a.gs += aS; a.gc += hS
            when { hS > aS -> h.points += 3; hS < aS -> a.points += 3; else -> { h.points += 1; a.points += 1 } }
        }
    }
    if (results.keys.none { id -> gM.any { it.id == id } }) return null
    return std.values.sortedWith(compareByDescending<TeamStats> { it.points }.thenByDescending { it.gs - it.gc }.thenByDescending { it.gs }).firstOrNull()?.let { it.name to it.flag }
}

@Composable
fun LoadQuinielaDialog(
    savedQuinielas: List<QuinielaEntity>,
    onDismiss: () -> Unit,
    onQuinielaSelected: (QuinielaEntity) -> Unit
) {
    var dialogSearchQuery by remember { mutableStateOf("") }
    val filteredSaved = remember(savedQuinielas, dialogSearchQuery) {
        if (dialogSearchQuery.isEmpty()) savedQuinielas
        else savedQuinielas.filter { 
            it.quinielaName.contains(dialogSearchQuery, ignoreCase = true) || 
            it.propietarioName.contains(dialogSearchQuery, ignoreCase = true) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Cargar Quiniela Guardada", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogSearchQuery,
                    onValueChange = { dialogSearchQuery = it },
                    placeholder = { Text("Buscar...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        text = {
            if (filteredSaved.isEmpty()) {
                Text(
                    if (savedQuinielas.isEmpty()) "No hay quinielas guardadas." 
                    else "No se encontraron resultados.", 
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(filteredSaved) { q ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuinielaSelected(q); onDismiss() }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(q.quinielaName, color = Gold, fontWeight = FontWeight.Bold)
                            Text(q.propietarioName, color = Color.White, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Gold) }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

fun QuinielaEntity.toParticipant(): Participant {
    val gson = Gson()
    val predictionsType = object : TypeToken<Map<String, MatchResult>>() {}.type
    val predictionsMap: Map<String, MatchResult> = gson.fromJson(this.resultsJson, predictionsType) ?: emptyMap()
    
    val predictions = predictionsMap.mapValues { (_, res) ->
        (res.homeScore.toIntOrNull() ?: 0) to (res.awayScore.toIntOrNull() ?: 0)
    }

    val winnersType = object : TypeToken<Map<String, String>>() {}.type
    val winners: Map<String, String> = gson.fromJson(this.winnersJson, winnersType) ?: emptyMap()

    return Participant(
        id = "loaded_${this.id}",
        quinielaName = this.quinielaName,
        ownerName = this.propietarioName,
        isUser = false,
        predictions = predictions,
        groupWinnerPredictions = winners,
        prevPosition = 0
    )
}

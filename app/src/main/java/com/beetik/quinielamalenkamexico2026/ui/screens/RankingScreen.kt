package com.beetik.quinielamalenkamexico2026.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.model.RankingConfig
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.*
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(viewModel: RankingViewModel = viewModel()) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // UI state that doesn't need persistence
    var selectedView by remember { mutableStateOf("Tabla") }
    var selectedFilter by remember { mutableStateOf("Todas") }
    
    // ViewModel driven state
    val savedConfigs = viewModel.savedConfigs
    val currentConfigId = viewModel.currentConfigId
    val pinnedCategories = viewModel.pinnedCategories
    val resultsMap = viewModel.resultsMap
    val baseParticipants = viewModel.baseParticipants

    val pinnedParticipantCategories = viewModel.pinnedParticipantCategories
    val pinnedParticipantIds = viewModel.pinnedParticipantIds
    val comparisonParticipantId = viewModel.comparisonParticipantId
    val isLiveRanking = viewModel.isLiveRanking

    // UI HELPER STATES
    var showSettingsManager by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    val allMatches = remember { 
        MatchRepository.allMatches.sortedWith(compareBy({ it.date }, { it.time })) 
    }
    
    val confirmedIds = remember { mutableStateListOf<String>() }
    var matchToEdit by remember { mutableStateOf<Match?>(null) }

    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val savedQuinielas: List<QuinielaEntity> by database.quinielaDao().getAllQuinielasFlow().collectAsState(initial = emptyList())
    
    var showLoadQuinielaDialog by remember { mutableStateOf(false) }

    val showAddButton by remember(savedQuinielas, baseParticipants.size) {
        derivedStateOf {
            savedQuinielas.isNotEmpty() && savedQuinielas.any { q -> baseParticipants.none { p -> p.id == "loaded_${q.id}" } }
        }
    }

    val scoresAndRanks by remember(isLiveRanking, selectedFilter) {
        derivedStateOf {
            val matchesByGroup = allMatches.groupBy { it.group }
            
            val scores = baseParticipants.associate { p ->
                var pts = 0
                allMatches.forEach { match ->
                    resultsMap[match.id]?.let { actual ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        pts += calculatePoints(pred, actual)
                    }
                }
                matchesByGroup.forEach { (gName, gMatches) ->
                    val hasResults = gMatches.any { it.id in resultsMap }
                    val isFinished = gMatches.all { it.id in resultsMap }
                    if (hasResults && (isFinished || isLiveRanking)) {
                        val winner = getGroupWinner(gName, allMatches, resultsMap)?.first
                        if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                    }
                }
                p.id to pts
            }

            val ranks = mutableMapOf<String, Int>()
            val officialParticipants = baseParticipants.filter { !it.id.startsWith("loaded_") }
            
            if (selectedFilter == "Añadidas") {
                val addedParticipants = baseParticipants.filter { it.id.startsWith("loaded_") }
                val addedScores = addedParticipants.associateWith { scores[it.id] ?: 0 }
                val sortedAdded = addedScores.toList().sortedByDescending { it.second }
                
                var currentRank = 1
                for (i in sortedAdded.indices) {
                    if (i > 0 && sortedAdded[i].second < sortedAdded[i - 1].second) currentRank++
                    ranks[sortedAdded[i].first.id] = currentRank
                }
            } else {
                val officialScores = officialParticipants.associateWith { scores[it.id] ?: 0 }
                val sortedOfficial = officialScores.toList().sortedByDescending { it.second }
                var currentRank = 1
                for (i in sortedOfficial.indices) {
                    if (i > 0 && sortedOfficial[i].second < sortedOfficial[i - 1].second) currentRank++
                    ranks[sortedOfficial[i].first.id] = currentRank
                }

                val officialScoreToRank = officialScores.values.distinct().sortedByDescending { it }
                    .withIndex().associate { it.value to it.index + 1 }

                baseParticipants.filter { it.id.startsWith("loaded_") }.forEach { lp ->
                    val score = scores[lp.id] ?: 0
                    val matchScore = officialScoreToRank.keys.firstOrNull { it <= score }
                    ranks[lp.id] = if (matchScore != null) {
                        officialScoreToRank[matchScore] ?: 1
                    } else {
                        officialScoreToRank.size + 1
                    }
                }
            }

            val matchHistoryRanks = mutableMapOf<String, Map<String, Int>>()
            val runningScores = baseParticipants.associate { it.id to 0 }.toMutableMap()
            
            allMatches.forEach { match ->
                val result = resultsMap[match.id]
                if (result != null) {
                    baseParticipants.forEach { p ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        runningScores[p.id] = (runningScores[p.id] ?: 0) + calculatePoints(pred, result)
                    }
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
                    val currentOfficialScores = officialParticipants.associate { it.id to (runningScores[it.id] ?: 0) }
                    val sortedOfficialPoints = currentOfficialScores.values.distinct().sortedByDescending { it }
                    val scoreToRankAtStep = sortedOfficialPoints.withIndex().associate { it.value to it.index + 1 }
                    val ranksStep = mutableMapOf<String, Int>()
                    officialParticipants.forEach { p ->
                        ranksStep[p.id] = scoreToRankAtStep[runningScores[p.id] ?: 0] ?: 1
                    }
                    baseParticipants.filter { it.id.startsWith("loaded_") }.forEach { lp ->
                        val score = runningScores[lp.id] ?: 0
                        val matchScore = sortedOfficialPoints.firstOrNull { it <= score }
                        ranksStep[lp.id] = if (matchScore != null) {
                            scoreToRankAtStep[matchScore] ?: 1
                        } else {
                            sortedOfficialPoints.size + 1
                        }
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

    val filteredParticipants by remember(selectedFilter, searchQuery) {
        derivedStateOf {
            val scores = scoresAndRanks.first
            val user = baseParticipants.first { it.isUser }
            val loaded = baseParticipants.filter { it.id.startsWith("loaded_") }
            val others = baseParticipants.filter { !it.isUser && !it.id.startsWith("loaded_") }
            
            val pinnedOthers = others.filter { it.id in pinnedParticipantCategories }
                .sortedBy { pinnedParticipantIds.indexOf(it.id) }
            
            val remainingOthers = others.filter { it.id !in pinnedParticipantCategories }
            val allInTodasOrder = (loaded + listOf(user) + pinnedOthers + remainingOthers).distinctBy { it.id }

            val list = when {
                selectedFilter == "Añadidas" -> {
                    loaded.sortedWith(compareByDescending<Participant> { scores[it.id] ?: 0 }.thenBy { it.quinielaName })
                }
                selectedFilter != "Todas" && selectedFilter != "Top 10" && selectedFilter != "Top 5" -> {
                    val catId = pinnedCategories.values.find { it.name == selectedFilter }?.id
                    if (catId != null) {
                        val pinnedInCat = others.filter { pinnedParticipantCategories[it.id] == catId }
                        (loaded + listOf(user) + pinnedInCat).distinctBy { it.id }
                    } else { allInTodasOrder }
                }
                selectedFilter == "Top 5" || selectedFilter == "Top 10" -> {
                    val n = if (selectedFilter == "Top 5") 5 else 10
                    val othersPool = baseParticipants.filter { !it.isUser && !it.id.startsWith("loaded_") }
                    val sortedOthers = othersPool.sortedWith(compareByDescending<Participant> { scores[it.id] ?: 0 }.thenBy { allInTodasOrder.indexOf(it) })
                    (loaded + listOf(user) + sortedOthers.take(n)).distinctBy { it.id }
                }
                else -> allInTodasOrder
            }

            if (searchQuery.isEmpty()) list
            else list.filter { it.quinielaName.contains(searchQuery, ignoreCase = true) || it.ownerName.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (matchToEdit != null) {
        val match = matchToEdit!!
        val initialHome = resultsMap[match.id]?.home?.toString() ?: ""
        val initialAway = resultsMap[match.id]?.away?.toString() ?: ""
        
        var homeS by remember { mutableStateOf(TextFieldValue(initialHome, selection = TextRange(0, initialHome.length))) }
        var awayS by remember { mutableStateOf(TextFieldValue(initialAway)) }
        val homeFocusRequester = remember { FocusRequester() }
        val awayFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { homeFocusRequester.requestFocus() }

        val onConfirm = {
            val h = homeS.text.toIntOrNull()
            val a = awayS.text.toIntOrNull()
            if (h != null && a != null) resultsMap[match.id] = MatchScore(h, a)
            else resultsMap.remove(match.id)
            viewModel.saveCurrentState()
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
                        OutlinedTextField(
                            value = homeS, 
                            onValueChange = { homeS = it.copy(text = it.text.filter { c -> c.isDigit() }.take(2)) }, 
                            modifier = Modifier.width(64.dp).focusRequester(homeFocusRequester), 
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), 
                            keyboardActions = KeyboardActions(onNext = { 
                                awayS = awayS.copy(selection = TextRange(0, awayS.text.length))
                                awayFocusRequester.requestFocus() 
                            }), 
                            singleLine = true
                        )
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = awayS, 
                            onValueChange = { awayS = it.copy(text = it.text.filter { c -> c.isDigit() }.take(2)) }, 
                            modifier = Modifier.width(64.dp).focusRequester(awayFocusRequester), 
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), 
                            keyboardActions = KeyboardActions(onDone = { onConfirm() }), 
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = { 
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { homeS = TextFieldValue(""); awayS = TextFieldValue(""); homeFocusRequester.requestFocus() }) {
                        Text("Borrar", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Row {
                        TextButton(onClick = { matchToEdit = null }) { Text("Cancelar", fontSize = 12.sp) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black), modifier = Modifier.height(36.dp)) { Text("Aceptar", fontSize = 12.sp) }
                    }
                }
            },
            dismissButton = null
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
                    viewModel.saveCurrentState()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF121212)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = if (isLandscape) 1.dp else 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar", tint = Gold, modifier = Modifier.size(18.dp)) 
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar quiniela o usuario...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Gold, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
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
                        
                        if (!isLandscape) {
                            Text("QUINIELA MALENKA 2026", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f).padding(start = 4.dp))
                        } else {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Row(modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(1.dp)) {
                                    ViewTabSmall("Tabla", selectedView == "Tabla") { selectedView = "Tabla" }
                                    ViewTabSmall("Tarjetas", selectedView == "Tarjetas") { selectedView = "Tarjetas" }
                                    ViewTabSmall("Ranking", selectedView == "Ranking") { selectedView = "Ranking" }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Cálculo en vivo", tint = if (isLiveRanking) Gold else Color.Gray, modifier = Modifier.size(14.dp))
                                Switch(checked = isLiveRanking, onCheckedChange = { viewModel.isLiveRanking = it; viewModel.saveCurrentState() }, modifier = Modifier.scale(0.4f).height(16.dp), colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = Gold.copy(alpha = 0.5f)))
                                Spacer(modifier = Modifier.width(16.dp))
                                val hasAnyLoaded = baseParticipants.any { it.id.startsWith("loaded_") }
                                FilterDropdown(selectedFilter, pinnedCategories.values.filter { cat -> pinnedParticipantCategories.values.contains(cat.id) }.map { it.name }, hasAnyLoaded) { selectedFilter = it }
                            }
                        }

                        IconButton(onClick = { isSearchActive = true }, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.Default.Search, "Buscar", tint = Color.White, modifier = Modifier.size(18.dp)) 
                        }
                        IconButton(onClick = { showSettingsManager = true }, modifier = Modifier.size(28.dp)) { 
                            Icon(Icons.Default.Settings, "Configuraciones", tint = Color.White, modifier = Modifier.size(16.dp)) 
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0A)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()).fillMaxSize()) {
            if (!isLandscape) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(2.dp)) {
                        ViewTabSmall("Tabla", selectedView == "Tabla") { selectedView = "Tabla" }
                        ViewTabSmall("Tarjetas", selectedView == "Tarjetas") { selectedView = "Tarjetas" }
                        ViewTabSmall("Ranking", selectedView == "Ranking") { selectedView = "Ranking" }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Cálculo en vivo", tint = if (isLiveRanking) Gold else Color.Gray, modifier = Modifier.size(14.dp))
                        Switch(checked = isLiveRanking, onCheckedChange = { viewModel.isLiveRanking = it; viewModel.saveCurrentState() }, modifier = Modifier.scale(0.5f).height(20.dp), colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = Gold.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Filtro: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                        val hasAnyLoaded = baseParticipants.any { it.id.startsWith("loaded_") }
                        FilterDropdown(selectedFilter, pinnedCategories.values.filter { cat -> pinnedParticipantCategories.values.contains(cat.id) }.map { it.name }, hasAnyLoaded) { selectedFilter = it }
                    }
                }
            }

            if (showSettingsManager) {
                SettingsManagerDialog(
                    currentConfigName = savedConfigs.find { it.id == currentConfigId }?.configName ?: "",
                    allConfigs = savedConfigs,
                    categories = pinnedCategories,
                    onDismiss = { showSettingsManager = false; viewModel.saveCurrentState() },
                    onSaveCurrentAsNew = { viewModel.onSaveCurrentAsNew(it) },
                    onSwitchConfig = { viewModel.loadConfigIntoState(it) },
                    onDeleteConfig = { viewModel.onDeleteConfig(it) },
                    onResetAll = { viewModel.onResetAll() },
                    onClearResults = { viewModel.onClearResults() },
                    onClearParticipants = { viewModel.onClearParticipants() },
                    onClearCategories = { viewModel.onClearCategories() }
                )
            }

            when(selectedView) {
                "Tabla" -> TableView(allMatches, filteredParticipants, resultsMap, confirmedIds, currentScores, currentRanks, matchHistoryRanks, pinnedParticipantCategories, pinnedParticipantIds, pinnedCategories, comparisonParticipantId, showAddButton, isLiveRanking, { matchToEdit = it }, { showLoadQuinielaDialog = true }, { id -> viewModel.onRemoveParticipant(id) }, { id -> viewModel.onToggleComparison(id) })
                "Tarjetas" -> CardsView(allMatches.first(), filteredParticipants, resultsMap, currentScores, currentRanks, pinnedParticipantCategories, pinnedParticipantIds, pinnedCategories, showAddButton, isLiveRanking, { showLoadQuinielaDialog = true }, { id -> viewModel.onRemoveParticipant(id) })
                "Ranking" -> GlobalRankingView(filteredParticipants, currentScores, currentRanks)
            }
        }
    }
}

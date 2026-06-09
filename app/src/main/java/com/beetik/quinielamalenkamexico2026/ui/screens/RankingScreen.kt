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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.*
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Trigger reload when access code changes
    LaunchedEffect(userViewModel.accessCode, userViewModel.isLoggedIn) {
        viewModel.loadOfficialParticipants(userViewModel.accessCode)
    }
    
    // UI state that needs persistence across rotation
    var selectedView by rememberSaveable { mutableStateOf("Tabla") }
    var selectedFilter by rememberSaveable { mutableStateOf("Todas") }
    
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

    val allMatches = remember(viewModel.allMatches) { 
        viewModel.allMatches.sortedWith(compareBy({ it.date }, { it.time })) 
    }
    
    val confirmedIds = viewModel.confirmedIds
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

    val scoresAndRanks by remember(isLiveRanking, selectedFilter, resultsMap.size, confirmedIds.size) {
        derivedStateOf {
            val matchesByGroup = allMatches.groupBy { it.group }
            
            val scores = baseParticipants.associate { p ->
                var pts = 0
                allMatches.forEach { match ->
                    getEffectiveScore(match, resultsMap)?.let { actual ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        pts += calculatePoints(pred, actual)
                    }
                }
                matchesByGroup.forEach { (gName, gMatches) ->
                    val hasResults = gMatches.any { getEffectiveScore(it, resultsMap) != null }
                    val isFinished = gMatches.all { getEffectiveScore(it, resultsMap) != null }
                    if (hasResults && (isFinished || isLiveRanking)) {
                        val winner = getGroupWinner(gName, allMatches, resultsMap)?.first
                        if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                    }
                }
                p.id to pts
            }

            // Calculate base scores (Confirmed only)
            val baseScores = baseParticipants.associate { p ->
                var pts = 0
                allMatches.forEach { match ->
                    if (match.id in confirmedIds) {
                        resultsMap[match.id]?.let { actual ->
                            val pred = p.predictions[match.id] ?: (0 to 0)
                            pts += calculatePoints(pred, actual)
                        }
                    }
                }
                matchesByGroup.forEach { (gName, gMatches) ->
                    val allConfirmed = gMatches.all { it.id in confirmedIds }
                    if (allConfirmed) {
                        val confirmedResults = resultsMap.filterKeys { it in confirmedIds }
                        val winner = getGroupWinner(gName, allMatches, confirmedResults)?.first
                        if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                    }
                }
                p.id to pts
            }

            val ranks = mutableMapOf<String, Int>()
            val baseRanks = mutableMapOf<String, Int>()
            val officialParticipants = baseParticipants.filter { !it.id.startsWith("loaded_") }
            
            if (selectedFilter == "Añadidas") {
                val addedParticipants = baseParticipants.filter { it.id.startsWith("loaded_") }
                
                // Current Ranks
                val addedScores = addedParticipants.associateWith { scores[it.id] ?: 0 }
                val sortedAdded = addedScores.toList().sortedByDescending { it.second }
                var currentRank = 1
                for (i in sortedAdded.indices) {
                    if (i > 0 && sortedAdded[i].second < sortedAdded[i - 1].second) currentRank++
                    ranks[sortedAdded[i].first.id] = currentRank
                }
                
                // Base Ranks
                val addedBaseScores = addedParticipants.associateWith { baseScores[it.id] ?: 0 }
                val sortedBase = addedBaseScores.toList().sortedByDescending { it.second }
                var cBaseRank = 1
                for (i in sortedBase.indices) {
                    if (i > 0 && sortedBase[i].second < sortedBase[i - 1].second) cBaseRank++
                    baseRanks[sortedBase[i].first.id] = cBaseRank
                }
            } else {
                // Official Ranks
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
                
                // Base Official Ranks
                val officialBaseScores = officialParticipants.associateWith { baseScores[it.id] ?: 0 }
                val sortedBase = officialBaseScores.toList().sortedByDescending { it.second }
                var cBaseRank = 1
                for (i in sortedBase.indices) {
                    if (i > 0 && sortedBase[i].second < sortedBase[i - 1].second) cBaseRank++
                    baseRanks[sortedBase[i].first.id] = cBaseRank
                }
                
                val officialBaseScoreToRank = officialBaseScores.values.distinct().sortedByDescending { it }
                    .withIndex().associate { it.value to it.index + 1 }
                
                baseParticipants.filter { it.id.startsWith("loaded_") }.forEach { lp ->
                    val score = baseScores[lp.id] ?: 0
                    val matchScore = officialBaseScoreToRank.keys.firstOrNull { it <= score }
                    baseRanks[lp.id] = if (matchScore != null) {
                        officialBaseScoreToRank[matchScore] ?: 1
                    } else {
                        officialBaseScoreToRank.size + 1
                    }
                }
            }

            val matchHistoryRanks = mutableMapOf<String, Map<String, Int>>()
            val runningScores = baseParticipants.associate { it.id to 0 }.toMutableMap()
            
            allMatches.forEach { match ->
                val result = getEffectiveScore(match, resultsMap)
                if (result != null) {
                    baseParticipants.forEach { p ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        runningScores[p.id] = (runningScores[p.id] ?: 0) + calculatePoints(pred, result)
                    }
                    val groupMatches = allMatches.filter { it.group == match.group }
                    if (groupMatches.last().id == match.id && groupMatches.all { getEffectiveScore(it, resultsMap) != null }) {
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
            listOf(scores, ranks, baseRanks, matchHistoryRanks)
        }
    }

    val currentScores = scoresAndRanks[0] as Map<String, Int>
    val currentRanks = scoresAndRanks[1] as Map<String, Int>
    val baseRanks = scoresAndRanks[2] as Map<String, Int>
    val matchHistoryRanks = scoresAndRanks[3] as Map<String, Map<String, Int>>

    val filteredParticipants by remember(selectedFilter, searchQuery) {
        derivedStateOf {
            val scores = currentScores
            val users = baseParticipants.filter { it.isUser }
            val loaded = baseParticipants.filter { it.id.startsWith("loaded_") && !it.isUser }
            val others = baseParticipants.filter { !it.isUser && !it.id.startsWith("loaded_") }
            
            // Re-order based on pins
            val pinnedOthers = others.filter { it.id in pinnedParticipantCategories }
                .sortedBy { pinnedParticipantIds.indexOf(it.id) }
            val remainingOthers = others.filter { it.id !in pinnedParticipantCategories }
            
            val pinnedLoaded = loaded.filter { it.id in pinnedParticipantCategories }
                .sortedBy { pinnedParticipantIds.indexOf(it.id) }
            val remainingLoaded = loaded.filter { it.id !in pinnedParticipantCategories }

            // Base order: [Loaded] [Mis Quinielas] [Others]
            val allInTodasOrder = (pinnedLoaded + remainingLoaded + 
                                   users + 
                                   pinnedOthers + remainingOthers).distinctBy { it.id }

            val list = when {
                selectedFilter == "Añadidas" -> {
                    val addedOnly = baseParticipants.filter { it.id.startsWith("loaded_") }
                    addedOnly.sortedWith(compareByDescending<Participant> { currentScores[it.id] ?: 0 }.thenBy { it.quinielaName })
                }
                selectedFilter != "Todas" && selectedFilter != "Top 10" && selectedFilter != "Top 5" -> {
                    val catId = pinnedCategories.values.find { it.name == selectedFilter }?.id
                    if (catId != null) {
                        val pinnedInCat = baseParticipants
                            .filter { pinnedParticipantCategories[it.id] == catId }
                            .sortedBy { pinnedParticipantIds.indexOf(it.id) }
                        (loaded.filter { it.id !in pinnedParticipantCategories } + 
                         users.filter { it.id !in pinnedParticipantCategories } + 
                         pinnedInCat).distinctBy { it.id }
                    } else { allInTodasOrder }
                }
                selectedFilter == "Top 5" || selectedFilter == "Top 10" -> {
                    val n = if (selectedFilter == "Top 5") 5 else 10
                    val othersPool = baseParticipants.filter { !it.isUser && !it.id.startsWith("loaded_") }
                    val sortedOthers = othersPool.sortedWith(compareByDescending<Participant> { currentScores[it.id] ?: 0 }.thenBy { allInTodasOrder.indexOf(it) })
                    (users + loaded + sortedOthers.take(n)).distinctBy { it.id }
                }
                else -> allInTodasOrder
            }

            if (searchQuery.isEmpty()) list
            else list.filter { it.quinielaName.contains(searchQuery, ignoreCase = true) || it.ownerName.contains(searchQuery, ignoreCase = true) }
        }
    }

    val distinctDates = remember(allMatches) { allMatches.map { it.date }.distinct().sorted() }
    
    var selectedCardsDate by remember(distinctDates) {
        mutableStateOf(
            run {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val todayStr = sdf.format(Date())
                distinctDates.minByOrNull { date ->
                    try {
                        val d = sdf.parse(date)
                        val t = sdf.parse(todayStr)
                        Math.abs(d.time!! - t.time!!)
                    } catch (e: Exception) { Long.MAX_VALUE }
                } ?: (if (distinctDates.isNotEmpty()) distinctDates[0] else "")
            }
        )
    }

    var showOnlyDayPoints by remember { mutableStateOf(false) }

    val dayMatches = remember(allMatches, selectedCardsDate) {
        allMatches.filter { it.date == selectedCardsDate }
    }

    val datesWithSimulations = remember(resultsMap.size, confirmedIds.size) {
        allMatches.filter { it.id in resultsMap && it.id !in confirmedIds }
            .map { it.date }
            .toSet()
    }

    val cardsScoresAndRanks by remember(isLiveRanking, selectedCardsDate, baseParticipants.size, resultsMap.size, showOnlyDayPoints) {
        derivedStateOf {
            val matchesForCalculation = if (showOnlyDayPoints) {
                allMatches.filter { it.date == selectedCardsDate }
            } else {
                allMatches.filter { it.date <= selectedCardsDate }
            }
            
            val resultsForCalculation = resultsMap.filterKeys { id -> 
                val match = allMatches.find { it.id == id }
                if (showOnlyDayPoints) match?.date == selectedCardsDate
                else match?.date?.let { it <= selectedCardsDate } ?: false
            }
            
            val scores = baseParticipants.associate { p ->
                var pts = 0
                matchesForCalculation.forEach { match ->
                    resultsForCalculation[match.id]?.let { actual ->
                        val pred = p.predictions[match.id] ?: (0 to 0)
                        pts += calculatePoints(pred, actual)
                    }
                }
                
                if (!showOnlyDayPoints) {
                    allMatches.groupBy { it.group }.forEach { (gName, gMatches) ->
                        val gMatchesUpToDate = gMatches.filter { it.date <= selectedCardsDate }
                        if (gMatchesUpToDate.isNotEmpty()) {
                            val hasResults = gMatchesUpToDate.any { it.id in resultsForCalculation }
                            val isGroupFullyFinished = gMatches.all { it.id in resultsMap && it.id in confirmedIds } // Group points usually only count if fully confirmed or if live is on
                            
                            // Re-evaluating group points logic for cards view
                            val groupResultsForWinner = resultsMap.filterKeys { id -> allMatches.find { it.id == id }?.let { it.group == gName && it.date <= selectedCardsDate } ?: false }
                            val hasAnyResultInGroup = groupResultsForWinner.isNotEmpty()
                            val isFinished = gMatches.all { it.id in resultsMap }
                            
                            if (hasAnyResultInGroup && (isFinished || isLiveRanking)) {
                                val winner = getGroupWinner(gName, allMatches, groupResultsForWinner)?.first
                                if (winner != null && winner == p.groupWinnerPredictions[gName]) pts += 2
                            }
                        }
                    }
                }
                p.id to pts
            }

            val ranks = mutableMapOf<String, Int>()
            val officialParticipants = baseParticipants.filter { !it.id.startsWith("loaded_") }
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
                ranks[lp.id] = if (matchScore != null) officialScoreToRank[matchScore] ?: 1 else officialScoreToRank.size + 1
            }

            scores to ranks
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
                val newParticipant = entity.toParticipant(userViewModel.email)
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
                "Tarjetas" -> CardsView(
                    allMatches = allMatches,
                    dayMatches = dayMatches,
                    participants = filteredParticipants, 
                    resultsMap = resultsMap, 
                    scores = cardsScoresAndRanks.first, 
                    ranks = cardsScoresAndRanks.second, 
                    confirmedIds = confirmedIds,
                    pinnedParticipantCategories = pinnedParticipantCategories, 
                    pinnedParticipantIds = pinnedParticipantIds, 
                    pinnedCategories = pinnedCategories, 
                    availableDates = distinctDates,
                    selectedDate = selectedCardsDate,
                    datesWithSimulations = datesWithSimulations,
                    comparisonParticipantId = comparisonParticipantId,
                    showOnlyDayPoints = showOnlyDayPoints,
                    showAddButton = showAddButton, 
                    isLiveRanking = isLiveRanking, 
                    onSimularMatch = { matchToEdit = it },
                    onClearSimulation = { match ->
                        resultsMap.remove(match.id)
                        viewModel.saveCurrentState()
                    },
                    onDateSelected = { selectedCardsDate = it },
                    onToggleDayPoints = { showOnlyDayPoints = it },
                    onAddParticipant = { showLoadQuinielaDialog = true }, 
                    onRemoveParticipant = { id -> viewModel.onRemoveParticipant(id) },
                    onToggleComparison = { id -> viewModel.onToggleComparison(id) }
                )
                "Ranking" -> {
                    // We get all participants from baseParticipants to ignore the UI filters (search/dropdown)
                    // only for the Ranking view
                    GlobalRankingView(
                        participants = baseParticipants,
                        scores = currentScores,
                        ranks = currentRanks,
                        baseRanks = baseRanks,
                        allMatches = allMatches,
                        resultsMap = resultsMap,
                        onSimulateResult = { match, h, a ->
                            resultsMap[match.id] = com.beetik.quinielamalenkamexico2026.model.MatchScore(h, a)
                            viewModel.saveCurrentState()
                        },
                        onClearSimulation = { match ->
                            resultsMap.remove(match.id)
                            viewModel.saveCurrentState()
                        },
                        onMatchClick = { matchToEdit = it }
                    )
                }
            }
        }
    }
}

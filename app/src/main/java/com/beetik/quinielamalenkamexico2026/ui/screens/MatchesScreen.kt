package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.GroupStandingsView
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.MatchesListView
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.getGroupWinner
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.KnockoutBracketView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.RankingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private fun String.normalize(): String {
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}

@Composable
fun PartidosScreen(rankingViewModel: RankingViewModel = viewModel()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    
    var selectedView by rememberSaveable { mutableStateOf("Partidos") }
    var isSimulationMode by rememberSaveable { mutableStateOf(false) }

    // Search state
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }
    
    // Timezone handling
    val sdfCDMX = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val sdfLocalTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfLocalDisplayDate = remember { SimpleDateFormat("d 'de' MMMM", Locale.getDefault()) }
    
    var savedQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var selectedQuinielaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedQuiniela by remember { mutableStateOf<QuinielaEntity?>(null) }
    
    val allMatches = rankingViewModel.allMatches
    val hasLiveMatches = remember(allMatches) { allMatches.any { it.started && it.isActive } }
    val secondaryTab = if (hasLiveMatches) "En Vivo" else "Próximos"
    
    val matchGroups = remember(allMatches, secondaryTab) { 
        listOf("Todos", secondaryTab) + allMatches.map { it.group }.distinct().sorted() 
    }
    val standingsGroups = remember(allMatches) { 
        listOf("Todos") + allMatches.map { it.group }.distinct().sorted() 
    }
    
    var selectedMatchGroupIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedStandingsGroupIndex by rememberSaveable { mutableIntStateOf(0) }
    
    val matchResults = remember(selectedQuiniela) {
        selectedQuiniela?.let {
            val type = object : TypeToken<Map<String, MatchResult>>() {}.type
            gson.fromJson<Map<String, MatchResult>>(it.resultsJson, type)
        } ?: emptyMap()
    }

    val userGroupWinners = remember(selectedQuiniela) {
        selectedQuiniela?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(it.winnersJson, type)
        } ?: emptyMap()
    }

    LaunchedEffect(Unit) {
        savedQuinielas = database.quinielaDao().getAllQuinielas()
        if (savedQuinielas.isNotEmpty()) {
            selectedQuiniela = if (selectedQuinielaId != null) {
                savedQuinielas.find { it.id == selectedQuinielaId } ?: savedQuinielas.first()
            } else {
                savedQuinielas.first()
            }
            selectedQuinielaId = selectedQuiniela?.id
        }
    }

    val filteredMatches by remember(selectedMatchGroupIndex, rankingViewModel.rawMatches, hasLiveMatches, searchQuery) {
        derivedStateOf {
            val baseFiltered = when (selectedMatchGroupIndex) {
                0 -> rankingViewModel.rawMatches.sortedWith(compareBy({ it.date }, { it.time }))
                1 -> {
                    if (hasLiveMatches) {
                        rankingViewModel.rawMatches.filter { it.started && it.isActive }
                    } else {
                        val sorted = rankingViewModel.rawMatches.sortedWith(compareBy({ it.date }, { it.time }))
                        val firstUpcomingIndex = sorted.indexOfFirst { !it.finished }
                        if (firstUpcomingIndex != -1) {
                            sorted.subList(firstUpcomingIndex, (firstUpcomingIndex + (if (sorted.size - firstUpcomingIndex < 5) sorted.size - firstUpcomingIndex else 5)).coerceAtMost(sorted.size))
                        } else {
                            emptyList()
                        }
                    }
                }
                else -> {
                    val groupName = matchGroups.getOrNull(selectedMatchGroupIndex) ?: "Todos"
                    rankingViewModel.rawMatches.filter { it.group == groupName }
                }
            }

            if (searchQuery.isBlank()) {
                baseFiltered
            } else {
                val query = searchQuery.normalize()
                rankingViewModel.rawMatches.filter { 
                    it.homeTeam.normalize().contains(query) || it.awayTeam.normalize().contains(query)
                }.sortedWith(compareBy({ it.date }, { it.time }))
            }
        }
    }

    val convertedResults by remember(isSimulationMode, allMatches) {
        derivedStateOf {
            if (isSimulationMode) {
                val results = mutableMapOf<String, Pair<Int, Int>>()
                allMatches.forEach { match ->
                    val simScore = rankingViewModel.resultsMap[match.id]
                    if (simScore != null) {
                        results[match.id] = simScore.home to simScore.away
                    } else if (match.started || match.finished) {
                        results[match.id] = (match.realHomeScore ?: 0) to (match.realAwayScore ?: 0)
                    }
                }
                results
            } else {
                allMatches.filter { it.started || it.finished }
                    .associate { it.id to ((it.realHomeScore ?: 0) to (it.realAwayScore ?: 0)) }
            }
        }
    }

    val showBracket by remember(selectedStandingsGroupIndex, rankingViewModel.isVisibleGroups, rankingViewModel.isVisibleFinal) {
        derivedStateOf {
            val isKnockoutTab = if (selectedStandingsGroupIndex == 0) false else !standingsGroups.getOrNull(selectedStandingsGroupIndex).orEmpty().startsWith("Grupo")
            (!rankingViewModel.isVisibleGroups && rankingViewModel.isVisibleFinal) || isKnockoutTab
        }
    }

    val standingsMatches by remember(selectedStandingsGroupIndex, allMatches) {
        derivedStateOf {
            if (selectedStandingsGroupIndex == 0) {
                allMatches
            } else {
                val groupName = standingsGroups.getOrNull(selectedStandingsGroupIndex)
                allMatches.filter { it.group == groupName }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // TOP BAR
        if (!isLandscape) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            placeholder = { Text("Buscar equipo...", color = Color.Gray, fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Gold
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        )
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Gold, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Text(
                            "PARTIDOS", 
                            fontWeight = FontWeight.ExtraBold, 
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Gold, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
        
        TabRow(
            selectedTabIndex = if (selectedView == "Partidos") 0 else 1,
            containerColor = Color.Transparent,
            contentColor = Gold,
            modifier = Modifier.height(36.dp),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (selectedView == "Partidos") 0 else 1]),
                    color = Gold,
                    height = 2.dp
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedView == "Partidos",
                onClick = { selectedView = "Partidos" },
                text = { Text("Partidos", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedView == "Posiciones",
                onClick = { selectedView = "Posiciones" },
                text = { Text("Posiciones", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        // CONTENT
        QuinielaSelector(
            selectedQuiniela = selectedQuiniela,
            savedQuinielas = savedQuinielas,
            isLandscape = isLandscape,
            onQuinielaSelected = { 
                selectedQuiniela = it
                selectedQuinielaId = it.id
            }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedView == "Partidos") {
                MatchesListView(
                    filteredMatches = filteredMatches,
                    matchResults = matchResults,
                    sdfCDMX = sdfCDMX,
                    sdfLocalTime = sdfLocalTime,
                    sdfLocalDisplayDate = sdfLocalDisplayDate,
                    headerContent = {
                        Column {
                            ScrollableTabRow(
                                selectedTabIndex = selectedMatchGroupIndex,
                                containerColor = Color.Transparent,
                                contentColor = Gold,
                                edgePadding = 16.dp,
                                modifier = Modifier.height(if (isLandscape) 36.dp else 48.dp),
                                divider = {},
                                indicator = { tabPositions ->
                                    if (selectedMatchGroupIndex < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMatchGroupIndex]),
                                            color = Gold
                                        )
                                    }
                                }
                            ) {
                                matchGroups.forEachIndexed { index, group ->
                                    Tab(
                                        selected = selectedMatchGroupIndex == index,
                                        onClick = { selectedMatchGroupIndex = index },
                                        text = { 
                                            Text(
                                                group, 
                                                modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 4.dp),
                                                style = if (selectedMatchGroupIndex == index) 
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = if (isLandscape) 11.sp else 14.sp
                                                    )
                                                else 
                                                    MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = if (isLandscape) 11.sp else 12.sp
                                                    )
                                            ) 
                                        },
                                        selectedContentColor = Gold,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!isLandscape) Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                )
            } else {
                if (showBracket) {
                    val knockoutMatches = remember(allMatches) { allMatches.filter { !it.group.startsWith("Grupo") } }
                    KnockoutBracketView(
                        allMatches = knockoutMatches, 
                        resultsMap = convertedResults,
                        matchResults = matchResults,
                        headerContent = null
                    )
                } else {
                    GroupStandingsView(
                        allMatches = standingsMatches,
                        resultsMap = convertedResults,
                        userGroupWinners = userGroupWinners,
                        headerContent = {
                            Column {
                                // Simulation Toggle
                                Surface(
                                    color = Gold.copy(alpha = 0.05f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isSimulationMode) "MODO SIMULACIÓN" else "MODO REAL",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSimulationMode) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = if (isSimulationMode) "Incluye tus pronósticos para partidos futuros" else "Solo resultados oficiales",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        Switch(
                                            checked = isSimulationMode,
                                            onCheckedChange = { isSimulationMode = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Gold,
                                                checkedTrackColor = Gold.copy(alpha = 0.3f),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }

                                if (rankingViewModel.isVisibleGroups) {
                                    ScrollableTabRow(
                                        selectedTabIndex = selectedStandingsGroupIndex,
                                        containerColor = Color.Transparent,
                                        contentColor = Gold,
                                        edgePadding = 16.dp,
                                        modifier = Modifier.height(if (isLandscape) 36.dp else 48.dp),
                                        divider = {},
                                        indicator = { tabPositions ->
                                            if (selectedStandingsGroupIndex < tabPositions.size) {
                                                TabRowDefaults.SecondaryIndicator(
                                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedStandingsGroupIndex]),
                                                    color = Gold
                                                )
                                            }
                                        }
                                    ) {
                                        standingsGroups.forEachIndexed { index, group ->
                                            Tab(
                                                selected = selectedStandingsGroupIndex == index,
                                                onClick = { selectedStandingsGroupIndex = index },
                                                text = { 
                                                    Text(
                                                        group, 
                                                        modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 4.dp),
                                                        style = if (selectedStandingsGroupIndex == index) 
                                                            MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = if (isLandscape) 11.sp else 14.sp
                                                            )
                                                        else 
                                                            MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = if (isLandscape) 11.sp else 12.sp
                                                            )
                                                    ) 
                                                },
                                                selectedContentColor = Gold,
                                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (!isLandscape) Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuinielaSelector(
    selectedQuiniela: QuinielaEntity?,
    savedQuinielas: List<QuinielaEntity>,
    isLandscape: Boolean,
    onQuinielaSelected: (QuinielaEntity) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedCard(
            onClick = { showDropdown = true },
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = null, 
                        tint = Gold, 
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedQuiniela?.let { "${it.quinielaName} - ${it.propietarioName}" } ?: "Seleccionar Quiniela",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null, 
                    tint = Gold, 
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (savedQuinielas.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay quinielas guardadas") },
                    onClick = { showDropdown = false }
                )
            } else {
                savedQuinielas.forEach { quiniela ->
                    DropdownMenuItem(
                        text = { 
                            Column {
                                Text(quiniela.quinielaName, fontWeight = FontWeight.Bold)
                                Text(quiniela.propietarioName, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            onQuinielaSelected(quiniela)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

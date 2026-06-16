package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.ui.screens.ranking.RankingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private fun String.normalize(): String {
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidosScreen(rankingViewModel: RankingViewModel = viewModel()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
    val mexicoCityZone = "America/Mexico_City"
    val sdfCDMX = remember { 
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(mexicoCityZone)
        }
    }
    val sdfLocalTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfLocalDisplayDate = remember { SimpleDateFormat("d 'de' MMMM", Locale.getDefault()) }
    
    var savedQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var selectedQuinielaId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedQuiniela by remember { mutableStateOf<QuinielaEntity?>(null) }
    
    val allMatches = rankingViewModel.allMatches
    val hasLiveMatches = remember(allMatches) { allMatches.any { it.started && it.isActive } }
    val secondaryTab = if (hasLiveMatches) "En Vivo" else "Próximos"
    
    // Categorías diferenciadas para cada vista
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

    val filteredMatches = remember(selectedMatchGroupIndex, allMatches, hasLiveMatches, searchQuery) {
        val baseFiltered = when (selectedMatchGroupIndex) {
            0 -> allMatches.sortedWith(compareBy({ it.date }, { it.time }))
            1 -> {
                if (hasLiveMatches) {
                    allMatches.filter { it.started && it.isActive }
                } else {
                    val sorted = allMatches.sortedWith(compareBy({ it.date }, { it.time }))
                    val firstUpcomingIndex = sorted.indexOfFirst { !it.finished }
                    if (firstUpcomingIndex != -1) {
                        sorted.subList(firstUpcomingIndex, (firstUpcomingIndex + (if (sorted.size - firstUpcomingIndex < 5) sorted.size - firstUpcomingIndex else 5)).coerceAtMost(sorted.size))
                    } else {
                        emptyList()
                    }
                }
            }
            else -> {
                val groupName = matchGroups[selectedMatchGroupIndex]
                allMatches.filter { it.group == groupName }
            }
        }

        if (searchQuery.isBlank()) {
            baseFiltered
        } else {
            val query = searchQuery.normalize()
            allMatches.filter { 
                it.homeTeam.normalize().contains(query) || it.awayTeam.normalize().contains(query)
            }.sortedWith(compareBy({ it.date }, { it.time }))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (!isLandscape) {
                    CenterAlignedTopAppBar(
                        title = { 
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                    placeholder = { Text("Buscar equipo...", color = Color.Gray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = Gold
                                    ),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = Gold, fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Text("PARTIDOS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        },
                        actions = {
                            if (isSearchActive) {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", tint = Gold)
                                }
                            } else {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Gold)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Gold),
                        scrollBehavior = scrollBehavior
                    )
                }
                TabRow(
                    selectedTabIndex = if (selectedView == "Partidos") 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = Gold,
                    modifier = Modifier.height(if (isLandscape) 40.dp else 48.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (selectedView == "Partidos") 0 else 1]),
                            color = Gold
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedView == "Partidos",
                        onClick = { selectedView = "Partidos" },
                        text = { Text("Partidos", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp) }
                    )
                    Tab(
                        selected = selectedView == "Posiciones",
                        onClick = { selectedView = "Posiciones" },
                        text = { Text("Posiciones", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedView == "Partidos") {
                MatchesListView(
                    filteredMatches = filteredMatches,
                    matchResults = matchResults,
                    sdfCDMX = sdfCDMX,
                    sdfLocalTime = sdfLocalTime,
                    sdfLocalDisplayDate = sdfLocalDisplayDate,
                    headerContent = {
                        Column {
                            QuinielaSelector(
                                selectedQuiniela = selectedQuiniela,
                                savedQuinielas = savedQuinielas,
                                isLandscape = isLandscape,
                                onQuinielaSelected = { 
                                    selectedQuiniela = it
                                    selectedQuinielaId = it.id
                                }
                            )

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
                val convertedResults = remember(isSimulationMode, rankingViewModel.resultsMap.size, allMatches) {
                    if (isSimulationMode) {
                        val results = mutableMapOf<String, Pair<Int, Int>>()
                        
                        allMatches.forEach { match ->
                            val simScore = rankingViewModel.resultsMap[match.id]
                            if (simScore != null) {
                                // 1. Prioridad: Simulación manual (del RankingView)
                                results[match.id] = simScore.home to simScore.away
                            } else if (match.started || match.finished) {
                                // 2. Si no hay simulación, pero el partido ya empezó/terminó, usar real
                                results[match.id] = (match.realHomeScore ?: 0) to (match.realAwayScore ?: 0)
                            }
                        }
                        results
                    } else {
                        // Modo Real: Solo partidos que han empezado (Vivo o Finalizado)
                        allMatches.filter { it.started || it.finished }
                            .associate { it.id to ((it.realHomeScore ?: 0) to (it.realAwayScore ?: 0)) }
                    }
                }

                // Sticky Header for Posiciones
                Column {
                    QuinielaSelector(
                        selectedQuiniela = selectedQuiniela,
                        savedQuinielas = savedQuinielas,
                        isLandscape = isLandscape,
                        onQuinielaSelected = { 
                            selectedQuiniela = it
                            selectedQuinielaId = it.id
                        }
                    )

                    // Simulation Toggle - Ahora FUERA del scroll de GroupStandingsView
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
                    if (!isLandscape) Spacer(modifier = Modifier.height(8.dp))
                }

                val standingsMatches = remember(searchQuery, selectedStandingsGroupIndex, allMatches) {
                    if (searchQuery.isNotBlank()) {
                        val query = searchQuery.normalize()
                        val relevantGroups = allMatches.filter { 
                            it.homeTeam.normalize().contains(query) || it.awayTeam.normalize().contains(query)
                        }.map { it.group }.toSet()
                        allMatches.filter { it.group in relevantGroups }
                    } else if (selectedStandingsGroupIndex == 0) {
                        allMatches
                    } else {
                        allMatches.filter { it.group == standingsGroups[selectedStandingsGroupIndex] }
                    }
                }

                GroupStandingsView(
                    allMatches = standingsMatches,
                    resultsMap = convertedResults,
                    userGroupWinners = userGroupWinners,
                    headerContent = null // Ya no pasamos el header aquí para que no haga scroll
                )
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
            .padding(horizontal = 16.dp, vertical = if (isLandscape) 2.dp else 8.dp)
    ) {
        OutlinedCard(
            onClick = { showDropdown = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isLandscape) 6.dp else 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = null, 
                        tint = Gold, 
                        modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedQuiniela?.let { "${it.quinielaName} - ${it.propietarioName}" } ?: "Seleccionar Quiniela",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isLandscape) 11.sp else 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null, 
                    tint = Gold, 
                    modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
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

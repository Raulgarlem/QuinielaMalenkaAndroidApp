package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidosScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var selectedView by rememberSaveable { mutableStateOf("Partidos") }
    var isSimulationMode by rememberSaveable { mutableStateOf(false) }
    
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
    var rankingResults by remember { mutableStateOf<Map<String, MatchResult>>(emptyMap()) }
    
    val allMatches = MatchRepository.allMatches
    val groups = listOf("Todos") + allMatches.map { it.group }.distinct().sorted()
    var selectedGroupIndex by rememberSaveable { mutableIntStateOf(0) }
    
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

        // Load active ranking results for simulation
        val activeConfig = database.rankingConfigDao().getAllConfigs().find { it.isActive }
        if (activeConfig != null) {
            val type = object : TypeToken<Map<String, com.beetik.quinielamalenkamexico2026.model.MatchScore>>() {}.type
            val scoresMap: Map<String, com.beetik.quinielamalenkamexico2026.model.MatchScore> = gson.fromJson(activeConfig.resultsJson, type) ?: emptyMap()
            rankingResults = scoresMap.mapValues { (_, score) ->
                MatchResult(score.home.toString(), score.away.toString())
            }
        }
    }

    val filteredMatches = remember(selectedGroupIndex) {
        if (selectedGroupIndex == 0) {
            allMatches.sortedWith(compareBy({ it.date }, { it.time }))
        } else {
            val groupName = groups[selectedGroupIndex]
            allMatches.filter { it.group == groupName }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                if (!isLandscape) {
                    CenterAlignedTopAppBar(
                        title = { Text("PARTIDOS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
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
                                selectedTabIndex = selectedGroupIndex,
                                containerColor = Color.Transparent,
                                contentColor = Gold,
                                edgePadding = 16.dp,
                                modifier = Modifier.height(if (isLandscape) 36.dp else 48.dp),
                                divider = {},
                                indicator = { tabPositions ->
                                    if (selectedGroupIndex < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedGroupIndex]),
                                            color = Gold
                                        )
                                    }
                                }
                            ) {
                                groups.forEachIndexed { index, group ->
                                    Tab(
                                        selected = selectedGroupIndex == index,
                                        onClick = { selectedGroupIndex = index },
                                        text = { 
                                            Text(
                                                group, 
                                                modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 4.dp),
                                                style = if (selectedGroupIndex == index) 
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
                val convertedResults = remember(isSimulationMode, matchResults, rankingResults, allMatches) {
                    if (isSimulationMode) {
                        // Use simulated results from General Quiniela (Ranking)
                        rankingResults.mapValues { (_, res) ->
                            (res.homeScore.toIntOrNull() ?: 0) to (res.awayScore.toIntOrNull() ?: 0)
                        }
                    } else {
                        // Real mode: Show only matches that have a recorded real score
                        allMatches.filter { it.realHomeScore != null && it.realAwayScore != null }
                            .associate { it.id to (it.realHomeScore!! to it.realAwayScore!!) }
                    }
                }

                GroupStandingsView(
                    allMatches = if (selectedGroupIndex == 0) allMatches else allMatches.filter { it.group == groups[selectedGroupIndex] },
                    resultsMap = convertedResults,
                    userGroupWinners = userGroupWinners,
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

                            ScrollableTabRow(
                                selectedTabIndex = selectedGroupIndex,
                                containerColor = Color.Transparent,
                                contentColor = Gold,
                                edgePadding = 16.dp,
                                modifier = Modifier.height(if (isLandscape) 36.dp else 48.dp),
                                divider = {},
                                indicator = { tabPositions ->
                                    if (selectedGroupIndex < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedGroupIndex]),
                                            color = Gold
                                        )
                                    }
                                }
                            ) {
                                groups.forEachIndexed { index, group ->
                                    Tab(
                                        selected = selectedGroupIndex == index,
                                        onClick = { selectedGroupIndex = index },
                                        text = { 
                                            Text(
                                                group, 
                                                modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 4.dp),
                                                style = if (selectedGroupIndex == index) 
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

package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidosScreen() {
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    
    // Timezone handling
    val mexicoCityZone = "America/Mexico_City"
    val sdfCDMX = remember { 
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(mexicoCityZone)
        }
    }
    val sdfLocalTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val sdfLocalDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfLocalDisplayDate = remember { SimpleDateFormat("d 'de' MMMM", Locale.getDefault()) }
    
    var savedQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var selectedQuiniela by remember { mutableStateOf<QuinielaEntity?>(null) }
    var showDropdown by remember { mutableStateOf(false) }
    
    val allMatches = MatchRepository.allMatches
    val groups = listOf("Todos") + allMatches.map { it.group }.distinct().sorted()
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    
    val matchResults = remember(selectedQuiniela) {
        selectedQuiniela?.let {
            val type = object : TypeToken<Map<String, MatchResult>>() {}.type
            gson.fromJson<Map<String, MatchResult>>(it.resultsJson, type)
        } ?: emptyMap()
    }

    LaunchedEffect(Unit) {
        savedQuinielas = database.quinielaDao().getAllQuinielas()
        if (savedQuinielas.isNotEmpty() && selectedQuiniela == null) {
            selectedQuiniela = savedQuinielas.first()
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
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PARTIDOS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Gold)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quiniela Selector Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedCard(
                    onClick = { showDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Viendo pronósticos de:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedQuiniela?.let { "${it.quinielaName} - ${it.propietarioName}" } ?: "Seleccionar Quiniela",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Gold)
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
                                    selectedQuiniela = quiniela
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedGroupIndex,
                containerColor = Color.Transparent,
                contentColor = Gold,
                edgePadding = 16.dp,
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
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = if (selectedGroupIndex == index) 
                                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                else 
                                    MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        selectedContentColor = Gold,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(filteredMatches) { match ->
                    val result = matchResults[match.id]
                    
                    // Convert match time to local
                    val matchDateTime = remember(match) {
                        try {
                            sdfCDMX.parse("${match.date} ${match.time}")
                        } catch (_: Exception) {
                            null
                        }
                    }
                    
                    val displayTime = remember(matchDateTime) {
                        matchDateTime?.let { sdfLocalTime.format(it) } ?: match.time
                    }
                    val displayDate = remember(matchDateTime) {
                        matchDateTime?.let { sdfLocalDisplayDate.format(it) } ?: match.date
                    }

                    MatchItem(match, result, displayDate, displayTime)
                }
            }
        }
    }
}

@Composable
fun MatchItem(match: Match, prediction: MatchResult?, displayDate: String, displayTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Reverted Top Row: Group, Date and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.group} • $displayDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$displayTime hrs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.homeFlag, fontSize = 28.sp)
                    Text(
                        match.homeTeam,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        "VS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold.copy(alpha = 0.7f)
                    )
                    // Display actual result if available (placeholder for now)
                    Text(
                        "-", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Away Team
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.awayFlag, fontSize = 28.sp)
                    Text(
                        match.awayTeam,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Section: Prediction and Points (Visible when a quiniela is selected)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Tu pronóstico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val scoreText = if (prediction != null && prediction.homeScore.isNotEmpty() && prediction.awayScore.isNotEmpty()) {
                        "${prediction.homeScore} - ${prediction.awayScore}"
                    } else {
                        "-  -"
                    }
                    Text(
                        text = scoreText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Puntos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // When match is finished, show points. For now, show "-" if not finished.
                    // If we had a 'isFinished' property, we'd use it here.
                    Text(
                        text = "-", 
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

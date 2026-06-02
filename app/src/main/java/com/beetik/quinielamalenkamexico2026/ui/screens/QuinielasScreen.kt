package com.beetik.quinielamalenkamexico2026.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.navigation.Screen
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuinielasScreen(navController: NavController) {
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val allMatches = remember { MatchRepository.allMatches }
    val groupCount = remember { allMatches.groupBy { it.group }.size }
    
    // Using a simple state and LaunchedEffect to collect from the flow
    var allQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var quinielaToDelete by remember { mutableStateOf<QuinielaEntity?>(null) }
    var quinielaForOptions by remember { mutableStateOf<QuinielaEntity?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    LaunchedEffect(database) {
        database.quinielaDao().getAllQuinielasFlow().collect {
            allQuinielas = it
        }
    }

    var selectedTab by remember { mutableStateOf("Mis Quinielas") }

    val filteredQuinielas = remember(allQuinielas, selectedTab) {
        when (selectedTab) {
            "Creadas" -> allQuinielas.filter { !it.isSent }
            "Enviadas" -> allQuinielas.filter { it.isSent }
            else -> allQuinielas
        }
    }

    if (showBottomSheet && quinielaForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false 
                quinielaForOptions = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Gold.copy(alpha = 0.5f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = quinielaForOptions?.quinielaName ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Gold
                )
                
                ListItem(
                    headlineContent = { Text("Duplicar") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickable {
                        quinielaForOptions?.let { entity ->
                            coroutineScope.launch {
                                var suffix = 1
                                var newName: String
                                var uniqueFound = false
                                
                                val baseName = entity.quinielaName.replace(Regex(" \\(\\d+\\)$"), "")
                                
                                do {
                                    newName = "$baseName ($suffix)"
                                    val existing = database.quinielaDao().getQuinielaByNameAndOwner(newName, entity.propietarioName)
                                    if (existing == null) {
                                        uniqueFound = true
                                    } else {
                                        suffix++
                                    }
                                } while (!uniqueFound)

                                val newEntity = entity.copy(
                                    id = 0,
                                    quinielaName = newName,
                                    isSent = false,
                                    isFavorite = false
                                )
                                database.quinielaDao().insertQuiniela(newEntity)
                                showBottomSheet = false
                                quinielaForOptions = null
                                Toast.makeText(context, "Quiniela duplicada.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                ListItem(
                    headlineContent = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        quinielaToDelete = quinielaForOptions
                        showBottomSheet = false
                        quinielaForOptions = null
                    }
                )
            }
        }
    }

    if (quinielaToDelete != null) {
        AlertDialog(
            onDismissRequest = { quinielaToDelete = null },
            title = { Text("¿Eliminar quiniela?") },
            text = { 
                val name = quinielaToDelete?.quinielaName?.ifEmpty { "(Sin nombre)" }
                val owner = quinielaToDelete?.propietarioName?.ifEmpty { "Anónimo" }
                Text("¿Estás seguro de que deseas borrar la quiniela \"$name\" de $owner? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        quinielaToDelete?.let { entity ->
                            coroutineScope.launch {
                                database.quinielaDao().deleteQuiniela(entity)
                                quinielaToDelete = null
                                Toast.makeText(context, "Quiniela eliminada.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { quinielaToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.FillQuiniela.createRoute(-1)) },
                containerColor = Gold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear nueva quiniela")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MIS QUINIELAS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crea, gestiona y envía tus quinielas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterTab(
                    text = "Mis Quinielas",
                    selected = selectedTab == "Mis Quinielas",
                    onClick = { selectedTab = "Mis Quinielas" }
                )
                FilterTab(
                    text = "Creadas",
                    selected = selectedTab == "Creadas",
                    onClick = { selectedTab = "Creadas" }
                )
                FilterTab(
                    text = "Enviadas",
                    selected = selectedTab == "Enviadas",
                    onClick = { selectedTab = "Enviadas" }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    count = filteredQuinielas.size,
                    key = { filteredQuinielas[it].id }
                ) { index ->
                    val entity = filteredQuinielas[index]
                    
                    val isComplete = remember(entity.resultsJson, entity.winnersJson, entity.userEmail) {
                        try {
                            val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                            val winnersType = object : TypeToken<Map<String, String>>() {}.type
                            
                            val results: Map<String, MatchResult> = gson.fromJson(entity.resultsJson, resultsType)
                            val winners: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                            
                            val matchesDone = results.values.count { it.homeScore.isNotEmpty() && it.awayScore.isNotEmpty() }
                            val winnersDone = winners.size
                            val emailValid = entity.userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(entity.userEmail).matches()
                            
                            matchesDone == allMatches.size && winnersDone == groupCount && emailValid
                        } catch (_: Exception) {
                            false
                        }
                    }

                    val statusText = when {
                        entity.isSent -> "Enviada"
                        isComplete -> "Completa"
                        else -> "Borrador"
                    }

                    val statusColor = when {
                        entity.isSent -> Gold // Gold for Sent
                        isComplete -> Color(0xFFFF9800) // Orange for Complete
                        else -> Color(0xFF9C27B0) // Purple for Draft
                    }

                    val dismissState = rememberSwipeToDismissBoxState()
                    
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        SideEffect {
                            quinielaToDelete = entity
                            coroutineScope.launch {
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }, label = "dismissColor"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        QuinielaListItem(
                            name = entity.quinielaName.ifEmpty { "Sin nombre" },
                            status = statusText,
                            statusColor = statusColor,
                            date = "Propietario: ${entity.propietarioName}",
                            participants = entity.userEmail,
                            points = if (entity.isSent) "0" else null, // Placeholder points
                            isFavorite = entity.isFavorite,
                            onFavoriteClick = {
                                coroutineScope.launch {
                                    database.quinielaDao().toggleFavorite(entity.id)
                                }
                            },
                            onLongClick = {
                                quinielaForOptions = entity
                                showBottomSheet = true
                            },
                            onClick = { navController.navigate(Screen.FillQuiniela.createRoute(entity.id)) }
                        )
                    }
                }
                
                item {
                    // Create New Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Transparent)
                            .border(1.dp, Gold.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                            .clickable { navController.navigate(Screen.FillQuiniela.createRoute(-1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Gold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Crear nueva quiniela", fontWeight = FontWeight.Bold, color = Gold)
                                Text("Comienza a hacer tus pronósticos", style = MaterialTheme.typography.bodySmall, color = Gold.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Gold else Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        border = if (selected) null else borderStroke(),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuinielaListItem(
    name: String,
    status: String,
    statusColor: Color,
    date: String,
    participants: String,
    points: String?,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite Star (Now on the left side)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Icon or Trophy
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(4.dp))

                // Status Tag on its own line
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        status.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(participants, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (points != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(points, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Gold)
                    Text("pts", style = MaterialTheme.typography.labelSmall, color = Gold)
                }
            } else {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

package com.beetik.quinielamalenkamexico2026.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.navigation.Screen
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.beetik.quinielamalenkamexico2026.util.ScoreCalculator
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuinielasScreen(navController: NavController, userViewModel: UserViewModel = viewModel()) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val allMatchesFlow = remember { MatchRepository.getMatchesFlow() }
    val currentMatches by allMatchesFlow.collectAsState(initial = MatchRepository.allMatches)
    val groupCount = remember { MatchRepository.allMatches.groupBy { it.group }.size }
    
    // Using a simple state and LaunchedEffect to collect from the flow
    var allQuinielas by remember { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var quinielaToDeleteLocal by remember { mutableStateOf<QuinielaEntity?>(null) }
    var quinielaToDeleteServer by remember { mutableStateOf<QuinielaEntity?>(null) }
    var showDeleteOptions by remember { mutableStateOf<QuinielaEntity?>(null) }
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
                        showDeleteOptions = quinielaForOptions
                        showBottomSheet = false
                        quinielaForOptions = null
                    }
                )
            }
        }
    }

    if (showDeleteOptions != null) {
        AlertDialog(
            onDismissRequest = { showDeleteOptions = null },
            title = { Text("¿Cómo deseas eliminar?") },
            text = { Text("Elige si deseas eliminar esta quiniela solo de este dispositivo o también de los servidores de la nube.") },
            confirmButton = {
                val entity = showDeleteOptions!!
                val isOwner = userViewModel.isLoggedIn && 
                    userViewModel.email.lowercase().trim() == entity.userEmail.lowercase().trim()

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            quinielaToDeleteLocal = showDeleteOptions
                            showDeleteOptions = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Solo de este Dispositivo")
                    }
                    
                    if (isOwner) {
                        Button(
                            onClick = {
                                quinielaToDeleteServer = showDeleteOptions
                                showDeleteOptions = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("De este Dispositivo y del Servidor")
                        }
                    }

                    TextButton(
                        onClick = { showDeleteOptions = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            },
            dismissButton = null // We included Cancel in the confirmButton Column for better layout control
        )
    }

    if (quinielaToDeleteLocal != null) {
        AlertDialog(
            onDismissRequest = { quinielaToDeleteLocal = null },
            title = { Text("¿Eliminar del dispositivo?") },
            text = { 
                val name = quinielaToDeleteLocal?.quinielaName?.ifEmpty { "(Sin nombre)" }
                Text("¿Estás seguro de que deseas borrar la quiniela \"$name\" de este teléfono? Esta acción no se puede deshacer localmente.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        quinielaToDeleteLocal?.let { entity ->
                            coroutineScope.launch {
                                database.quinielaDao().deleteQuiniela(entity)
                                quinielaToDeleteLocal = null
                                Toast.makeText(context, "Quiniela eliminada del dispositivo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { quinielaToDeleteLocal = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (quinielaToDeleteServer != null) {
        val initialUser = remember(quinielaToDeleteServer) {
            quinielaToDeleteServer?.userEmail?.lowercase()?.trim()
                ?.replace("@", "_")?.replace(".", "_") ?: ""
        }
        var userField by remember { mutableStateOf(initialUser) }
        var passField by remember { mutableStateOf("") }
        var isVerifying by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { quinielaToDeleteServer = null },
            title = { Text("Eliminar del Servidor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Para eliminar esta quiniela de la nube, por favor ingresa tus credenciales de acceso.")
                    OutlinedTextField(
                        value = userField,
                        onValueChange = { userField = it },
                        label = { Text("Username (Correo formateado)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ej. usuario_gmail_com") }
                    )
                    OutlinedTextField(
                        value = passField,
                        onValueChange = { passField = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entity = quinielaToDeleteServer ?: return@Button
                        val expectedUser = entity.userEmail.lowercase().trim()
                            .replace("@", "_").replace(".", "_")
                        
                        if (userField.trim() != expectedUser) {
                            Toast.makeText(context, "El username no coincide con el correo de la quiniela.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        isVerifying = true
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("codigos").document("correos").get()
                            .addOnSuccessListener { doc ->
                                val cloudPass = doc.getString(userField.trim())
                                if (cloudPass != null && cloudPass == passField) {
                                    // Credentials OK, Proceed to delete from all places
                                    val email = entity.userEmail.lowercase().trim()
                                    val docId = email.replace("@", "_").replace(".", "_")
                                    val mapKey = "${entity.quinielaName.trim()} - ${entity.propietarioName.trim()}"
                                    
                                    val batch = firestore.batch()
                                    
                                    // 1. Delete from "guardadas"
                                    val guardadasRef = firestore.collection("guardadas").document(docId)
                                    batch.update(guardadasRef, FieldPath.of(mapKey), FieldValue.delete())
                                    
                                    // 2. Delete from "quinielas" if sent
                                    val quinielaDocId = "${email}_${entity.quinielaName}_${entity.propietarioName}"
                                        .lowercase().trim().replace(" ", "_").replace(".", "_").replace("@", "_")
                                    val quinielaRef = firestore.collection("quinielas").document(quinielaDocId)
                                    batch.delete(quinielaRef)
                                    
                                    batch.commit().addOnCompleteListener {
                                        coroutineScope.launch {
                                            database.quinielaDao().deleteQuiniela(entity)
                                            quinielaToDeleteServer = null
                                            Toast.makeText(context, "Quiniela eliminada de todos lados.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isVerifying = false
                                    Toast.makeText(context, "Password incorrecta o usuario no encontrado.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                isVerifying = false
                                Toast.makeText(context, "Error al verificar: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    enabled = !isVerifying && userField.isNotBlank() && passField.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Confirmar Eliminación Total")
                }
            },
            dismissButton = {
                TextButton(onClick = { quinielaToDeleteServer = null }, enabled = !isVerifying) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.RoundSelection.createRoute(-1)) },
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
                .padding(
                    top = if (isLandscape) 0.dp else innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
                .padding(horizontal = 16.dp)
        ) {
            if (!isLandscape) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "MIS QUINIELAS",
                        style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLandscape) {
                        Text(
                            "Crea, gestiona y envía tus quinielas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

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

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

            if (userViewModel.isLoggedIn) {
                Button(
                    onClick = {
                        if (!userViewModel.isSyncing) {
                            userViewModel.syncQuinielasFromCloud(database) {
                                Toast.makeText(context, "Sincronización completada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold.copy(alpha = 0.1f),
                        contentColor = Gold
                    ),
                    shape = MaterialTheme.shapes.medium,
                    border = borderStroke(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        if (userViewModel.isSyncing) Icons.Default.Sync else Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (userViewModel.isSyncing) "Sincronizando..." else "Cargar mis quinielas (Cloud)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    // Points Calculation Info Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF001A33).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF003366))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF3399FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "CÁLCULO DE PUNTOS",
                                    color = Color(0xFF3399FF),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Los puntos de esta sección NO consideran juegos en vivo, solo juegos finalizados",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                items(
                    count = filteredQuinielas.size,
                    key = { filteredQuinielas[it].id }
                ) { index ->
                    val entity = filteredQuinielas[index]
                    
                    val scoreStats = remember(entity, currentMatches) {
                        try {
                            val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                            val winnersType = object : TypeToken<Map<String, String>>() {}.type
                            
                            val results: Map<String, MatchResult> = gson.fromJson(entity.resultsJson, resultsType)
                            val winners: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                            
                            ScoreCalculator.calculateStats(currentMatches, results, winners)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val isComplete = remember(entity.resultsJson, entity.winnersJson, entity.userEmail, entity.isKnockout) {
                        try {
                            val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                            val results: Map<String, MatchResult> = gson.fromJson(entity.resultsJson, resultsType)
                            
                            val emailValid = entity.userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(entity.userEmail).matches()

                            if (entity.isKnockout) {
                                // For knockout, it's "complete" if at least one match is filled and email is valid
                                // (Since we don't know all matches yet)
                                val matchesDone = results.values.count { it.homeScore.isNotEmpty() && it.awayScore.isNotEmpty() }
                                matchesDone > 0 && emailValid
                            } else {
                                val winnersType = object : TypeToken<Map<String, String>>() {}.type
                                val winners: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                                
                                val groupStageMatches = MatchRepository.allMatches.filter { it.group.startsWith("Grupo") }
                                val matchesDone = results.filterKeys { k -> groupStageMatches.any { it.id == k } }
                                    .values.count { it.homeScore.isNotEmpty() && it.awayScore.isNotEmpty() }
                                
                                val winnersDone = winners.size
                                
                                matchesDone == groupStageMatches.size && winnersDone == groupCount && emailValid
                            }
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
                            showDeleteOptions = entity
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
                            points = scoreStats?.totalPoints?.toString(),
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
                            onClick = { 
                                navController.navigate(Screen.RoundSelection.createRoute(entity.id))
                            }
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
                            .clickable { navController.navigate(Screen.RoundSelection.createRoute(-1)) },
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

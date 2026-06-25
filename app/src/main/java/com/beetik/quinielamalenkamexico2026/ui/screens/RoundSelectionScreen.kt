package com.beetik.quinielamalenkamexico2026.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.beetik.quinielamalenkamexico2026.R
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.components.MatchCard
import com.beetik.quinielamalenkamexico2026.ui.navigation.Screen
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class RoundItem(
    val id: String,
    val title: String,
    val matchCount: Int,
    val icon: ImageVector,
    val isKnockout: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSelectionScreen(
    quinielaId: Int,
    userViewModel: UserViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }
    
    val knockoutMatchesRaw = remember { MatchRepository.allMatches.filter { !it.group.startsWith("Grupo") } }
    val groupMatches = remember { MatchRepository.allMatches.filter { it.group.startsWith("Grupo") } }
    
    val allMatchesFlow = remember { MatchRepository.getMatchesFlow() }
    val currentMatches by allMatchesFlow.collectAsState(initial = MatchRepository.allMatches)

    var expandedRounds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var quinielaName by rememberSaveable { mutableStateOf("") }
    var propietarioName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var quinielaCode by rememberSaveable { mutableStateOf("") }
    var isSentByServer by rememberSaveable { mutableStateOf(false) }
    
    val matchResultsSaver = remember {
        Saver<Map<String, MatchResult>, Map<String, List<String>>>(
            save = { map -> map.mapValues { listOf(it.value.homeScore, it.value.awayScore) } },
            restore = { saved -> saved.mapValues { MatchResult(it.value[0], it.value[1]) } }
        )
    }
    var matchResults by rememberSaveable(stateSaver = matchResultsSaver) {
        mutableStateOf(MatchRepository.allMatches.associate { it.id to MatchResult() })
    }

    var showLoadDialog by rememberSaveable { mutableStateOf(false) }
    var savedQuinielas by rememberSaveable { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var currentId by rememberSaveable { mutableIntStateOf(quinielaId) }
    var originalData by remember { mutableStateOf<QuinielaEntity?>(null) }

    LaunchedEffect(Unit) {
        userViewModel.fetchFirebaseStats()
    }

    LaunchedEffect(currentId) {
        if (currentId != -1) {
            val entity = withContext(Dispatchers.IO) { database.quinielaDao().getQuinielaById(currentId) }
            if (entity != null) {
                originalData = entity
                quinielaName = entity.quinielaName
                propietarioName = entity.propietarioName
                userEmail = entity.userEmail
                quinielaCode = entity.quinielaCode
                val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                matchResults = gson.fromJson(entity.resultsJson, resultsType)
                isSentByServer = entity.isSent
            }
        } else if (userViewModel.isLoggedIn) {
            propietarioName = userViewModel.name
            userEmail = userViewModel.email
            quinielaCode = userViewModel.accessCode
        }
    }

    fun fetchQuinielas() {
        if (!userViewModel.isLoggedIn) return
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) { database.quinielaDao().getQuinielasByUser(userViewModel.email) }
            savedQuinielas = list.filter { it.isKnockout }
            showLoadDialog = true
        }
    }

    fun saveQuiniela(forceOverwrite: Boolean = true) {
        coroutineScope.launch {
            val resultsJson = gson.toJson(matchResults)
            val entity = QuinielaEntity(
                id = if (currentId != -1) currentId else 0,
                quinielaName = quinielaName,
                propietarioName = propietarioName,
                userEmail = userEmail,
                quinielaCode = quinielaCode,
                resultsJson = resultsJson,
                winnersJson = originalData?.winnersJson ?: "{}",
                isSent = isSentByServer,
                isKnockout = true
            )
            val newId = withContext(Dispatchers.IO) { database.quinielaDao().insertQuiniela(entity).toInt() }
            currentId = newId
            
            // Sync to "guardadas"
            val quinielaData = hashMapOf(
                "quinielaName" to quinielaName,
                "propietarioName" to propietarioName,
                "userEmail" to userEmail,
                "quinielaCode" to quinielaCode,
                "results" to matchResults.mapValues { mapOf("homeScore" to it.value.homeScore, "awayScore" to it.value.awayScore) },
                "isKnockout" to true,
                "isGroups" to false,
                "status" to "saved"
            )
            val docId = userEmail.lowercase().replace("@", "_").replace(".", "_")
            val mapKey = "$quinielaName - $propietarioName (KO)"
            firestore.collection("guardadas").document(docId).set(mapOf(mapKey to quinielaData), SetOptions.merge())
            
            Toast.makeText(context, "Quiniela guardada", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendQuiniela() {
        if (quinielaName.isBlank() || propietarioName.isBlank() || userEmail.isBlank()) {
            Toast.makeText(context, "Faltan datos (Nombre, Propietario o Correo)", Toast.LENGTH_LONG).show()
            return
        }
        coroutineScope.launch {
            val documentId = "${userEmail}_${quinielaName}_${propietarioName}_KO"
                .lowercase().replace(" ", "_").replace(".", "_").replace("@", "_")
            val quinielaData = hashMapOf(
                "quinielaName" to quinielaName,
                "propietarioName" to propietarioName,
                "userEmail" to userEmail,
                "quinielaCode" to quinielaCode,
                "results" to matchResults.mapValues { mapOf("homeScore" to it.value.homeScore, "awayScore" to it.value.awayScore) },
                "isKnockout" to true,
                "isGroups" to false,
                "status" to "received"
            )
            firestore.collection("quinielas").document(documentId).set(quinielaData).await()
            isSentByServer = true
            saveQuiniela()
            Toast.makeText(context, "Quiniela enviada", Toast.LENGTH_LONG).show()
        }
    }

    val rounds = listOf(
        RoundItem("grupos", "Fase de Grupos", groupMatches.size, Icons.Default.SportsSoccer, false),
        RoundItem("16avos de Final", "Dieciseisavos de Final", 16, Icons.Default.EmojiEvents, true),
        RoundItem("Octavos de Final", "Octavos de Final", 8, Icons.Default.EmojiEvents, true),
        RoundItem("Cuartos de Final", "Cuartos de Final", 4, Icons.Default.EmojiEvents, true),
        RoundItem("Semifinales", "Semifinales", 2, Icons.Default.EmojiEvents, true),
        RoundItem("Tercer Lugar", "Tercer Lugar", 1, Icons.Default.EmojiEvents, true),
        RoundItem("Final", "Final", 1, Icons.Default.EmojiEvents, true)
    )

    fun isRoundUnlocked(round: RoundItem): Boolean {
        if (!round.isKnockout) return userViewModel.isFaseGruposActive
        if (!userViewModel.isFaseFinalActive) return false
        if (round.id == "16avos de Final") return true
        val matchesInRound = currentMatches.filter { it.group == round.id }
        return matchesInRound.any { it.homeTeam != "Por definir" || it.awayTeam != "Por definir" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_quiniela),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.15f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MI QUINIELA", fontWeight = FontWeight.Bold, color = Gold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Gold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (userViewModel.isFaseFinalActive) {
                    Surface(tonalElevation = 2.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = { saveQuiniela() }, modifier = Modifier.weight(1f), border = BorderStroke(2.dp, Color(0xFF2A398D))) {
                                Text("Guardar", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { sendQuiniela() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE61D25)), enabled = !isSentByServer) {
                                Text(if (isSentByServer) "Enviada ✓" else "Enviar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val visibleRounds = remember(rounds, userViewModel.isFaseGruposActive, userViewModel.isFaseFinalActive) {
                rounds.filter { round -> if (!round.isKnockout) userViewModel.isFaseGruposActive else userViewModel.isFaseFinalActive }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = { fetchQuinielas() },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continuar llenando", fontWeight = FontWeight.Bold)
                    }
                }

                if (userViewModel.isFaseFinalActive) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2128)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("DATOS DE LA QUINIELA", fontWeight = FontWeight.Bold, color = Gold, fontSize = 14.sp)
                                
                                DarkTextField(value = quinielaName, onValueChange = { quinielaName = it }, label = "Nombre de la Quiniela", placeholder = "Ej: Mi Quiniela 2026")
                                DarkTextField(value = propietarioName, onValueChange = { propietarioName = it }, label = "Nombre del Propietario")
                                DarkTextField(value = userEmail, onValueChange = { userEmail = it }, label = "Correo Electrónico")
                                DarkTextField(value = quinielaCode, onValueChange = { quinielaCode = it }, label = "Código de Quiniela")
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("SELECCIÓN DE RONDA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
                        Text("Elige cada ronda para ir llenando tu quiniela.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                items(visibleRounds) { round ->
                    val unlocked = isRoundUnlocked(round)
                    val isExpanded = expandedRounds.contains(round.id)
                    
                    RoundExpandableCard(
                        round = round,
                        isUnlocked = unlocked,
                        isExpanded = isExpanded,
                        onToggle = { if (unlocked) expandedRounds = if (isExpanded) expandedRounds - round.id else expandedRounds + round.id },
                        onGroupClick = { navController.navigate(Screen.FillQuiniela.createRoute(currentId)) }
                    ) {
                        Column(modifier = Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val roundMatches = currentMatches.filter { it.group == round.id }
                            roundMatches.forEach { match ->
                                val res = matchResults[match.id] ?: MatchResult()
                                MatchCard(
                                    match = match,
                                    homeScore = res.homeScore,
                                    awayScore = res.awayScore,
                                    onHomeScoreChange = { s -> matchResults = matchResults.toMutableMap().apply { this[match.id] = res.copy(homeScore = s) } },
                                    onAwayScoreChange = { s -> matchResults = matchResults.toMutableMap().apply { this[match.id] = res.copy(awayScore = s) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Seleccionar Quiniela") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedQuinielas) { q ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { currentId = q.id; showLoadDialog = false }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(q.quinielaName, fontWeight = FontWeight.Bold)
                                Text("Propietario: ${q.propietarioName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLoadDialog = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                focusedBorderColor = Gold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun RoundExpandableCard(
    round: RoundItem,
    isUnlocked: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onGroupClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Column {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = isUnlocked) { if (round.isKnockout) onToggle() else onGroupClick() },
            colors = CardDefaults.cardColors(containerColor = if (isUnlocked) Color(0xFF1E2128) else Color(0xFF15181D)),
            shape = RoundedCornerShape(12.dp),
            border = if (isUnlocked) BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (isUnlocked) Gold.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Icon(round.icon, null, tint = if (isUnlocked) Gold else Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(round.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isUnlocked) Color.White else Color.Gray)
                    Text("${round.matchCount} partidos", style = MaterialTheme.typography.labelSmall, color = if (isUnlocked) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f))
                }
                if (!isUnlocked) {
                    Icon(Icons.Default.Lock, null, tint = Gold.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                } else if (round.isKnockout) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Gold, modifier = Modifier.size(24.dp).rotate(rotation))
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                }
            }
        }
        if (round.isKnockout) {
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Box(modifier = Modifier.padding(top = 8.dp)) { content() }
            }
        }
    }
}

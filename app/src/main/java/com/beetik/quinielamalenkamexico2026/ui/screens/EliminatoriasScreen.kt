package com.beetik.quinielamalenkamexico2026.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.R
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.components.MatchCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliminatoriasScreen(
    modifier: Modifier = Modifier,
    quinielaId: Int = -1,
    userViewModel: UserViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val editState = LocalQuinielaEditState.current
    val firestore = remember { FirebaseFirestore.getInstance() }

    // Filter matches for knockout stages
    val knockoutGroups = listOf("16avos de Final", "Octavos de Final", "Cuartos de Final", "Semifinales", "Tercer Lugar", "Final")
    var allMatches by remember { mutableStateOf(MatchRepository.allMatches.filter { it.group in knockoutGroups }) }
    
    LaunchedEffect(Unit) {
        MatchRepository.getMatchesFlow().collect { updated ->
            allMatches = updated.filter { it.group in knockoutGroups }
        }
    }

    val groups = remember(allMatches) { allMatches.groupBy { it.group } }
    val sortedGroupNames = remember(groups) { 
        knockoutGroups.filter { groups.containsKey(it) }
    }

    // Savers
    val matchResultsSaver = remember {
        Saver<Map<String, MatchResult>, Map<String, List<String>>>(
            save = { map -> map.mapValues { listOf(it.value.homeScore, it.value.awayScore) } },
            restore = { saved -> saved.mapValues { MatchResult(it.value[0], it.value[1]) } }
        )
    }

    val quinielaEntitySaver = remember(gson) {
        Saver<MutableState<QuinielaEntity?>, String>(
            save = { state -> if (state.value == null) "" else gson.toJson(state.value) },
            restore = { json -> mutableStateOf(if (json.isEmpty()) null else gson.fromJson(json, QuinielaEntity::class.java)) }
        )
    }

    var matchResults by rememberSaveable(stateSaver = matchResultsSaver) {
        mutableStateOf(allMatches.associate { it.id to MatchResult() })
    }

    var quinielaName by rememberSaveable { mutableStateOf("") }
    var propietarioName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var quinielaCode by rememberSaveable { mutableStateOf("") }
    var isSentByServer by rememberSaveable { mutableStateOf(false) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }
    var showOverwriteDialog by rememberSaveable { mutableStateOf(false) }
    var showSendConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var originalData by rememberSaveable(saver = quinielaEntitySaver) { mutableStateOf<QuinielaEntity?>(null) }
    var currentId by rememberSaveable { mutableStateOf(quinielaId) }
    var isDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(quinielaId) {
        if (isDataLoaded && (currentId == quinielaId || (quinielaId == -1 && currentId != -1))) return@LaunchedEffect

        if (quinielaId != -1) {
            withContext(Dispatchers.IO) {
                val entity = database.quinielaDao().getQuinielaById(quinielaId)
                if (entity != null) {
                    originalData = entity
                    withContext(Dispatchers.Main) {
                        currentId = entity.id
                        quinielaName = entity.quinielaName
                        propietarioName = entity.propietarioName
                        userEmail = entity.userEmail
                        quinielaCode = entity.quinielaCode
                        val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                        matchResults = gson.fromJson(entity.resultsJson, resultsType)
                        isSentByServer = entity.isSent
                        isDataLoaded = true
                    }
                }
            }
        } else {
            if (userViewModel.isLoggedIn) {
                propietarioName = userViewModel.name
                userEmail = userViewModel.email
                quinielaCode = userViewModel.accessCode
            }
            isDataLoaded = true
        }
    }

    val hasChanges = remember(isDataLoaded, quinielaName, propietarioName, userEmail, quinielaCode, matchResults, originalData, currentId) {
        if (!isDataLoaded) return@remember false
        if (currentId == -1) {
             quinielaName.isNotBlank() || propietarioName.isNotBlank() || userEmail.isNotBlank() || quinielaCode.isNotBlank() ||
             matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() }
        } else {
            val currentResultsJson = gson.toJson(matchResults)
            quinielaName != originalData?.quinielaName ||
            propietarioName != originalData?.propietarioName ||
            userEmail != originalData?.userEmail ||
            quinielaCode != originalData?.quinielaCode ||
            currentResultsJson != originalData?.resultsJson
        }
    }

    DisposableEffect(hasChanges) {
        editState.hasUnsavedChanges = hasChanges
        onDispose { editState.hasUnsavedChanges = false }
    }

    fun saveQuinielaToRoom(forceOverwrite: Boolean = false, onComplete: (() -> Unit)? = null) {
        val hasAnyScore = matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() }
        if (!hasAnyScore && quinielaName.isBlank() && propietarioName.isBlank()) {
            if (!forceOverwrite) Toast.makeText(context, "No hay datos para guardar.", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        coroutineScope.launch {
            val finalCode = quinielaCode.trim().ifBlank { originalData?.quinielaCode?.ifBlank { "Male2026" } ?: "Male2026" }
            val codesSnap = firestore.collection("codigos").document("creados").get().await()
            val validCodes = codesSnap.data?.values?.map { it.toString().trim() } ?: emptyList()

            if (!validCodes.contains(finalCode)) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "El código de quiniela no existe.", Toast.LENGTH_LONG).show() }
                return@launch
            }

            val existing = database.quinielaDao().getQuinielaByNameAndOwner(quinielaName, propietarioName)
            val resultsJson = gson.toJson(matchResults)

            if (!forceOverwrite && existing != null && existing.id != currentId) {
                showOverwriteDialog = true
                return@launch
            }

            val entity = QuinielaEntity(
                id = if (currentId != -1) currentId else (existing?.id ?: 0),
                quinielaName = quinielaName,
                propietarioName = propietarioName,
                userEmail = userEmail,
                quinielaCode = finalCode,
                resultsJson = resultsJson,
                winnersJson = "{}",
                isSent = isSentByServer,
                isKnockout = true
            )
            val newId = database.quinielaDao().insertQuiniela(entity).toInt()
            withContext(Dispatchers.Main) {
                currentId = newId
                originalData = entity.copy(id = newId)
                if (!forceOverwrite) Toast.makeText(context, "¡Quiniela de Eliminatorias guardada!", Toast.LENGTH_SHORT).show()
                showOverwriteDialog = false
                onComplete?.invoke()
            }
        }
    }

    fun sendQuinielaToFirebase(collectionPath: String) {
        val resultsForFirebase = matchResults.mapValues { (_, result) ->
            mapOf("homeScore" to result.homeScore, "awayScore" to result.awayScore)
        }

        val quinielaData = hashMapOf(
            "quinielaName" to quinielaName.trim(),
            "propietarioName" to propietarioName.trim(),
            "userEmail" to userEmail.lowercase().trim(),
            "quinielaCode" to quinielaCode.trim().ifBlank { "Male2026" },
            "results" to resultsForFirebase,
            "isKnockout" to true,
            "updatedAt" to System.currentTimeMillis(),
            "status" to if (collectionPath == "quinielas") "received" else "saved"
        )

        if (collectionPath == "quinielas") {
            val documentId = "${userEmail}_${quinielaName}_${propietarioName}_KO"
                .lowercase().trim().replace(" ", "_").replace(".", "_").replace("@", "_")

            firestore.collection("quinielas").document(documentId).set(quinielaData)
                .addOnSuccessListener {
                    isSentByServer = true
                    saveQuinielaToRoom(forceOverwrite = true)
                    Toast.makeText(context, "¡Quiniela de Eliminatorias enviada!", Toast.LENGTH_LONG).show()
                }
        } else {
            val documentId = userEmail.lowercase().trim().replace("@", "_").replace(".", "_")
            val mapKey = "${quinielaName.trim()} - ${propietarioName.trim()} (KO)"
            firestore.collection("guardadas").document(documentId).set(mapOf(mapKey to quinielaData), SetOptions.merge())
                .addOnSuccessListener { Toast.makeText(context, "¡Copia en la nube actualizada!", Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eliminatorias 2026", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Gold, navigationIconContentColor = Gold)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { saveQuinielaToRoom { sendQuinielaToFirebase("guardadas") } },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.8f), contentColor = Color(0xFF2A398D)),
                        border = BorderStroke(2.dp, Color(0xFF2A398D))
                    ) { Text("Guardar", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = {
                            val emailValid = userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()
                            if (emailValid && quinielaName.isNotBlank() && propietarioName.isNotBlank()) {
                                showSendConfirmDialog = true
                            } else {
                                Toast.makeText(context, "Completa nombre, propietario y correo válido.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSentByServer,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE61D25))
                    ) { Text(if (isSentByServer) "Enviada ✓" else "Enviar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Image(painter = painterResource(id = R.drawable.background_quiniela), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.1f)
            
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Title Section
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("ELIMINATORIAS", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, drawStyle = Stroke(width = 4f, join = StrokeJoin.Round)), color = Color.Black)
                                Text("ELIMINATORIAS", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2A398D))
                            }
                        }

                        OutlinedTextField(value = propietarioName, onValueChange = { propietarioName = it; isSentByServer = false }, label = { Text("Propietario") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = quinielaName, onValueChange = { quinielaName = it; isSentByServer = false }, label = { Text("Nombre de Quiniela") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = userEmail, onValueChange = { userEmail = it; isSentByServer = false }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = quinielaCode, onValueChange = { quinielaCode = it; isSentByServer = false }, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }

                sortedGroupNames.forEach { groupName ->
                    item {
                        Text(text = groupName, style = MaterialTheme.typography.titleLarge, color = Gold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    val groupMatches = groups[groupName] ?: emptyList()
                    items(groupMatches.size) { index ->
                        val match = groupMatches[index]
                        val result = matchResults[match.id] ?: MatchResult()
                        MatchCard(
                            match = match,
                            homeScore = result.homeScore,
                            awayScore = result.awayScore,
                            onHomeScoreChange = { newScore ->
                                matchResults = matchResults.toMutableMap().apply { this[match.id] = result.copy(homeScore = newScore) }
                                isSentByServer = false
                            },
                            onAwayScoreChange = { newScore ->
                                matchResults = matchResults.toMutableMap().apply { this[match.id] = result.copy(awayScore = newScore) }
                                isSentByServer = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSendConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSendConfirmDialog = false },
            title = { Text("Confirmar Envío") },
            text = { Text("¿Deseas enviar tus pronósticos de eliminatorias?") },
            confirmButton = {
                Button(onClick = { showSendConfirmDialog = false; saveQuinielaToRoom(true) { sendQuinielaToFirebase("quinielas") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE61D25))) {
                    Text("Enviar")
                }
            },
            dismissButton = { TextButton(onClick = { showSendConfirmDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { showOverwriteDialog = false },
            title = { Text("Sobreescribir") },
            text = { Text("Ya existe una quiniela con este nombre. ¿Sobreescribir?") },
            confirmButton = { Button(onClick = { saveQuinielaToRoom(true) }) { Text("Sí") } },
            dismissButton = { TextButton(onClick = { showOverwriteDialog = false }) { Text("No") } }
        )
    }
}

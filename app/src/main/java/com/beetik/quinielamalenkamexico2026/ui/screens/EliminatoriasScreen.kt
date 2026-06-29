package com.beetik.quinielamalenkamexico2026.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.R
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.components.MatchCard
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.beetik.quinielamalenkamexico2026.ui.theme.Success
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
    val knockoutGroups = remember { listOf("16avos de Final", "Octavos de Final", "Cuartos de Final", "Semifinales", "Tercer Lugar", "Final") }
    val allMatchesFlow = remember { MatchRepository.getMatchesFlow() }
    val currentMatches by allMatchesFlow.collectAsState(initial = MatchRepository.allMatches)
    
    val allMatches = remember(currentMatches) { 
        currentMatches.filter { it.group in knockoutGroups } 
    }

    // Lógica para bloquear el Tercer Lugar basada en el estado visual (bordes verdes) de los primeros partidos
    val isThirdPlaceEnabled = remember(currentMatches) {
        val m1 = currentMatches.find { it.id == "R32_1" }
        val m2 = currentMatches.find { it.id == "R32_2" }
        
        // "Borde verde" significa que started, finished o isActive son true
        val isM1Green = m1?.let { it.started || it.finished || it.isActive } ?: false
        val isM2Green = m2?.let { it.started || it.finished || it.isActive } ?: false
        
        // Bloqueamos si AMBOS están en verde (según tu lógica de "verificar inputs")
        !(isM1Green && isM2Green)
    }

    val isChampionEnabled = currentMatches.find { it.group == "Final" }?.let { !it.started && !it.finished && !it.isActive } ?: true

    // Evento onChange: Reacciona específicamente cuando el segundo partido cambia a verde
    val isSecondMatchGreen = remember(currentMatches) {
        currentMatches.find { it.id == "R32_2" }?.let { it.started || it.finished || it.isActive } ?: false
    }
    
    LaunchedEffect(isSecondMatchGreen) {
        if (isSecondMatchGreen) {
            // Aquí puedes poner cualquier acción que deba ocurrir cuando el borde cambie a verde
            // Toast.makeText(context, "El segundo partido ha comenzado, bloqueando secciones...", Toast.LENGTH_SHORT).show()
        }
    }

    val qualifiedTeams = remember(allMatches) {
        val teams = allMatches
            .filter { it.group == "16avos de Final" }
            .flatMap { listOf(it.homeTeam, it.awayTeam) }
            .filter { it.isNotBlank() && it != "Por definir" }
            .distinct()
            .sorted()
        // Fallback simple si aún no hay equipos en 16avos
        if (teams.isEmpty()) {
            MatchRepository.allMatches
                .flatMap { listOf(it.homeTeam, it.awayTeam) }
                .filter { it.isNotBlank() && it != "Por definir" }
                .distinct()
                .sorted()
        } else teams
    }
    
    /* Borramos el LaunchedEffect que actualizaba allMatches manualmente */
    
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
    var championWinner by rememberSaveable { mutableStateOf("") }
    var thirdPlaceWinner by rememberSaveable { mutableStateOf("") }
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
                        val winnersType = object : TypeToken<Map<String, String>>() {}.type
                        val winnersMap: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                        championWinner = winnersMap["Final"] ?: ""
                        thirdPlaceWinner = winnersMap["Tercer Lugar"] ?: ""
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

    val hasChanges = remember(isDataLoaded, quinielaName, propietarioName, userEmail, quinielaCode, matchResults, championWinner, thirdPlaceWinner, originalData, currentId) {
        if (!isDataLoaded) return@remember false
        if (currentId == -1) {
             quinielaName.isNotBlank() || propietarioName.isNotBlank() || userEmail.isNotBlank() || quinielaCode.isNotBlank() ||
             championWinner.isNotBlank() || thirdPlaceWinner.isNotBlank() ||
             matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() }
        } else {
            val currentResultsJson = gson.toJson(matchResults)
            val currentWinnersJson = gson.toJson(mapOf("Final" to championWinner, "Tercer Lugar" to thirdPlaceWinner))
            quinielaName != originalData?.quinielaName ||
            propietarioName != originalData?.propietarioName ||
            userEmail != originalData?.userEmail ||
            quinielaCode != originalData?.quinielaCode ||
            currentResultsJson != originalData?.resultsJson ||
            currentWinnersJson != originalData?.winnersJson
        }
    }

    DisposableEffect(hasChanges) {
        editState.hasUnsavedChanges = hasChanges
        onDispose { editState.hasUnsavedChanges = false }
    }

    fun saveQuinielaToRoom(forceOverwrite: Boolean = false, onComplete: (() -> Unit)? = null) {
        val hasAnyScore = matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() }
        if (!hasAnyScore && quinielaName.isBlank() && propietarioName.isBlank() && championWinner.isBlank() && thirdPlaceWinner.isBlank()) {
            if (!forceOverwrite) Toast.makeText(context, "No hay datos para guardar.", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        coroutineScope.launch {
            val finalCode = quinielaCode.trim().ifBlank { originalData?.quinielaCode?.ifBlank { "Male2026" } ?: "Male2026" }
            
            // Use shared codes map to avoid Firestore read call on every save
            val validCodes = MatchRepository.codesMapFlow.value.values.map { it.trim() }

            if (validCodes.isNotEmpty() && !validCodes.contains(finalCode)) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "El código de quiniela no existe.", Toast.LENGTH_LONG).show() }
                return@launch
            }

            val existing = database.quinielaDao().getQuinielaByNameAndOwner(quinielaName, propietarioName)
            val resultsJson = gson.toJson(matchResults)
            val winnersJson = gson.toJson(mapOf("Final" to championWinner, "Tercer Lugar" to thirdPlaceWinner))

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
                winnersJson = winnersJson,
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
        val knockoutIds = MatchRepository.allMatches
            .filter { it.group in knockoutGroups }
            .map { it.id }
            .toSet()

        val matchesById = allMatches.associateBy { it.id }
        val resultsForFirebase = matchResults
            .filterKeys { it in knockoutIds }
            .mapValues { (matchId, result) ->
                val match = matchesById[matchId]
                mapOf(
                    "homeTeam" to (match?.homeTeam ?: ""),
                    "awayTeam" to (match?.awayTeam ?: ""),
                    "homeFlag" to (match?.homeFlag ?: ""),
                    "awayFlag" to (match?.awayFlag ?: ""),
                    "homeScore" to result.homeScore,
                    "awayScore" to result.awayScore
                )
            }

        val winnersMap = mapOf("Final" to championWinner, "Tercer Lugar" to thirdPlaceWinner)

        val quinielaData = hashMapOf(
            "quinielaName" to quinielaName.trim(),
            "propietarioName" to propietarioName.trim(),
            "userEmail" to userEmail.lowercase().trim(),
            "quinielaCode" to quinielaCode.trim().ifBlank { "Male2026" },
            "results" to resultsForFirebase,
            "winners" to winnersMap,
            "isKnockout" to true,
            "updatedAt" to System.currentTimeMillis(),
            "emailStatus" to "pending",
            "paymentReceived" to false,
            "status" to if (collectionPath == "quinielas") "received" else "saved"
        )

        if (collectionPath == "quinielas") {
            val documentId = userEmail.lowercase().trim().replace("@", "_").replace(".", "_")
            val mapKey = "${quinielaName.trim()} - ${propietarioName.trim()} (KO)"

            // Check if payment was already received in the cloud
            firestore.collection("quinielas").document(documentId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val existingQuiniela = doc.get(mapKey) as? Map<*, *>
                    if (existingQuiniela?.get("paymentReceived") == true) {
                        quinielaData["paymentReceived"] = true
                    }
                }

                firestore.collection("quinielas").document(documentId).set(mapOf(mapKey to quinielaData), SetOptions.merge())
                    .addOnSuccessListener {
                        isSentByServer = true
                        saveQuinielaToRoom(forceOverwrite = true)
                        
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = URL("https://n8n.beetikmx.com/webhook/quiniela-finales-recibida")
                                val connection = url.openConnection() as HttpURLConnection
                                connection.requestMethod = "POST"
                                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                                connection.doOutput = true

                                val payload = quinielaData.toMutableMap()
                                payload["documentId"] = documentId
                                payload["mapKey"] = mapKey
                                val jsonPayload = gson.toJson(payload)

                                connection.outputStream.use { os ->
                                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                                }

                                val responseCode = connection.responseCode
                                connection.disconnect()

                                withContext(Dispatchers.Main) {
                                    if (responseCode in 200..299) {
                                        Toast.makeText(context, "¡Quiniela de Eliminatorias enviada!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "¡Registrada! (Error en Webhook)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "¡Registrada en Nube!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
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
                            
                            // 1. Verificar campos básicos
                            if (!emailValid || propietarioName.isBlank() || quinielaName.isBlank()) {
                                Toast.makeText(context, "Correo, Propietario y Nombre de Quiniela son obligatorios.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            
                            // 2. Verificar Tercer Lugar (Solo si el campo aún está habilitado para edición)
                            if (thirdPlaceWinner.isBlank() && isThirdPlaceEnabled) {
                                Toast.makeText(context, "Debes seleccionar un favorito para el Tercer Lugar.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            
                            // 3. Verificar marcadores completos (ambos o ninguno)
                            val hasIncompleteScores = matchResults.values.any { 
                                (it.homeScore.isBlank() && it.awayScore.isNotBlank()) || 
                                (it.homeScore.isNotBlank() && it.awayScore.isBlank()) 
                            }
                            
                            if (hasIncompleteScores) {
                                Toast.makeText(context, "Los partidos deben tener marcador completo (ambos equipos) o estar totalmente vacíos.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            showSendConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        enabled = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE61D25))
                    ) { Text(if (isSentByServer) "Re-enviar" else "Enviar", fontWeight = FontWeight.Bold) }
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

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "FAVORITOS", style = MaterialTheme.typography.titleLarge, color = Gold, fontWeight = FontWeight.Bold)
                        
                        WinnerSelector(
                            label = "Campeón del Mundo",
                            selectedTeam = championWinner,
                            allTeams = qualifiedTeams,
                            onTeamSelected = { championWinner = it; isSentByServer = false },
                            enabled = isChampionEnabled
                        )

                        WinnerSelector(
                            label = "Tercer Lugar",
                            selectedTeam = thirdPlaceWinner,
                            allTeams = qualifiedTeams,
                            onTeamSelected = { thirdPlaceWinner = it; isSentByServer = false },
                            enabled = isThirdPlaceEnabled
                        )
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
                                val isBlocked = match.started || match.finished || match.isActive
                                if (!isBlocked) {
                                    matchResults = matchResults.toMutableMap().apply { this[match.id] = result.copy(homeScore = newScore) }
                                    isSentByServer = false
                                }
                            },
                            onAwayScoreChange = { newScore ->
                                val isBlocked = match.started || match.finished || match.isActive
                                if (!isBlocked) {
                                    matchResults = matchResults.toMutableMap().apply { this[match.id] = result.copy(awayScore = newScore) }
                                    isSentByServer = false
                                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinnerSelector(
    label: String,
    selectedTeam: String,
    allTeams: List<String>,
    onTeamSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTeam,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = {
                    if (selectedTeam.isNotEmpty()) {
                        Text(
                            text = MatchRepository.getFlag(selectedTeam),
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 20.sp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                    focusedLabelColor = Gold,
                    disabledBorderColor = Success,
                    disabledLabelColor = Success.copy(alpha = 0.7f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                    disabledTrailingIconColor = Color.Transparent
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                allTeams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(MatchRepository.getFlag(team), fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(team, style = MaterialTheme.typography.bodyLarge)
                            }
                        },
                        onClick = {
                            onTeamSelected(team)
                            expanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

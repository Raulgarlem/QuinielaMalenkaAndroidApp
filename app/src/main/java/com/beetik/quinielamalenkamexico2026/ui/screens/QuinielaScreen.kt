package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import com.beetik.quinielamalenkamexico2026.R
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.components.MatchCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.tasks.await

import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.Saver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuinielaScreen(
    modifier: Modifier = Modifier,
    quinielaId: Int = -1,
    userViewModel: UserViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    var allMatches by remember { mutableStateOf(MatchRepository.allMatches) }
    
    LaunchedEffect(Unit) {
        MatchRepository.getMatchesFlow().collect { updated ->
            allMatches = updated
        }
    }

    val groups = remember(allMatches) { allMatches.groupBy { it.group } }
    val groupNames = remember { groups.keys.toList() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val editState = LocalQuinielaEditState.current

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val database = remember { QuinielaDatabase.getDatabase(context) }
    val gson = remember { Gson() }

    // Savers for rememberSaveable
    val matchResultsSaver = remember {
        Saver<Map<String, MatchResult>, Map<String, List<String>>>(
            save = { map -> map.mapValues { listOf(it.value.homeScore, it.value.awayScore) } },
            restore = { saved -> saved.mapValues { MatchResult(it.value[0], it.value[1]) } }
        )
    }

    val groupWinnersSaver = remember {
        Saver<Map<String, String>, Map<String, String>>(
            save = { it },
            restore = { it }
        )
    }

    val quinielaEntitySaver = remember(gson) {
        Saver<MutableState<QuinielaEntity?>, String>(
            save = { state -> if (state.value == null) "" else gson.toJson(state.value) },
            restore = { json -> mutableStateOf(if (json.isEmpty()) null else gson.fromJson(json, QuinielaEntity::class.java)) }
        )
    }

    val savedQuinielasSaver = remember(gson) {
        Saver<List<QuinielaEntity>, String>(
            save = { list -> gson.toJson(list) },
            restore = { json -> gson.fromJson(json, object : TypeToken<List<QuinielaEntity>>() {}.type) }
        )
    }
    
    var matchResults by rememberSaveable(stateSaver = matchResultsSaver) {
        mutableStateOf(allMatches.associate { it.id to MatchResult() })
    }

    var groupWinners by rememberSaveable(stateSaver = groupWinnersSaver) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

    var quinielaName by rememberSaveable { mutableStateOf("") }
    var propietarioName by rememberSaveable { mutableStateOf("") }
    var userEmail by rememberSaveable { mutableStateOf("") }
    var quinielaCode by rememberSaveable { mutableStateOf("") }
    var isSentByServer by rememberSaveable { mutableStateOf(false) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var showLoadDialog by rememberSaveable { mutableStateOf(false) }
    var showEmailDialog by rememberSaveable { mutableStateOf(false) }
    var targetEmailToLoad by rememberSaveable { mutableStateOf("") }
    var isFetchingQuinielas by rememberSaveable { mutableStateOf(false) }
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    var showOverwriteDialog by rememberSaveable { mutableStateOf(false) }
    var showSendConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var savedQuinielas by rememberSaveable(stateSaver = savedQuinielasSaver) { mutableStateOf<List<QuinielaEntity>>(emptyList()) }
    var quinielaToDeleteLocal by rememberSaveable(saver = quinielaEntitySaver) { mutableStateOf<QuinielaEntity?>(null) }
    var quinielaToDeleteServer by rememberSaveable(saver = quinielaEntitySaver) { mutableStateOf<QuinielaEntity?>(null) }
    var showDeleteOptions by rememberSaveable(saver = quinielaEntitySaver) { mutableStateOf<QuinielaEntity?>(null) }

    var originalData by rememberSaveable(saver = quinielaEntitySaver) { mutableStateOf<QuinielaEntity?>(null) }
    var currentId by rememberSaveable { mutableStateOf(quinielaId) }
    var isDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(quinielaId) {
        // Avoid reloading if we already have data (e.g., after a rotation)
        if (isDataLoaded && (currentId == quinielaId || (quinielaId == -1 && currentId != -1))) {
            return@LaunchedEffect
        }

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
                        val winnersType = object : TypeToken<Map<String, String>>() {}.type

                        matchResults = gson.fromJson(entity.resultsJson, resultsType)
                        groupWinners = try {
                            gson.fromJson(entity.winnersJson, winnersType)
                        } catch (_: Exception) {
                            emptyMap()
                        }
                        isSentByServer = entity.isSent
                        isDataLoaded = true
                    }
                }
            }
        } else {
            // Pre-fill for new quiniela if logged in
            if (userViewModel.isLoggedIn) {
                propietarioName = userViewModel.name
                userEmail = userViewModel.email
                // Only autocomplete if we are creating a new one and logged in.
                // We keep it as is, but logic later will handle empty vs loaded.
                quinielaCode = userViewModel.accessCode
            }
            isDataLoaded = true
        }
    }

    val hasChanges = remember(isDataLoaded, quinielaName, propietarioName, userEmail, quinielaCode, matchResults, groupWinners, originalData, currentId) {
        if (!isDataLoaded) return@remember false
        
        if (currentId == -1) {
             quinielaName.isNotBlank() || propietarioName.isNotBlank() || userEmail.isNotBlank() || quinielaCode.isNotBlank() ||
             matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() } || 
             groupWinners.isNotEmpty()
        } else {
            val currentResultsJson = gson.toJson(matchResults)
            val currentWinnersJson = gson.toJson(groupWinners)
            
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
        onDispose {
            editState.hasUnsavedChanges = false
        }
    }

    fun saveQuinielaToRoom(forceOverwrite: Boolean = false, onComplete: (() -> Unit)? = null) {
        val hasAnyScore = matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() }
        val hasAnyWinner = groupWinners.isNotEmpty()
        val hasName = quinielaName.isNotBlank()
        val hasPropietario = propietarioName.isNotBlank()
        val hasEmail = userEmail.isNotBlank()
        val hasCode = quinielaCode.isNotBlank()

        if (!hasAnyScore && !hasAnyWinner && !hasName && !hasPropietario && !hasEmail && !hasCode) {
            if (!forceOverwrite) Toast.makeText(context, "No hay datos para guardar.", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        coroutineScope.launch {
            // Logic for code if empty:
            // Use same code as loaded, if never had one use "Male2026"
            val finalCode = quinielaCode.trim().ifBlank { 
                originalData?.quinielaCode?.ifBlank { "Male2026" } ?: "Male2026" 
            }

            // Validation: Check if finalCode exists in Firebase
            val codesSnap = firestore.collection("codigos").document("creados").get().await()
            val validCodes = codesSnap.data?.values?.map { it.toString().trim() } ?: emptyList()

            if (!validCodes.contains(finalCode)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "El código de quiniela no existe.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val existing = database.quinielaDao().getQuinielaByNameAndOwner(quinielaName, propietarioName)
            
            val resultsJson = gson.toJson(matchResults)
            val winnersJson = gson.toJson(groupWinners)

            if (!forceOverwrite && existing != null && existing.id != currentId) {
                // Check if the content is different before showing overwrite dialog
                val isIdentical = existing.quinielaName == quinielaName &&
                                  existing.propietarioName == propietarioName &&
                                  existing.userEmail == userEmail &&
                                  existing.quinielaCode == finalCode &&
                                  existing.resultsJson == resultsJson &&
                                  existing.winnersJson == winnersJson
                
                if (!isIdentical) {
                    withContext(Dispatchers.Main) {
                        showOverwriteDialog = true
                    }
                    return@launch
                } else {
                    // It's identical, just adopt this ID and consider it saved
                    withContext(Dispatchers.Main) {
                        currentId = existing.id
                        originalData = existing
                        onComplete?.invoke()
                    }
                    return@launch
                }
            }

            val entity = QuinielaEntity(
                id = if (currentId != -1) currentId else (existing?.id ?: 0),
                quinielaName = quinielaName,
                propietarioName = propietarioName,
                userEmail = userEmail,
                quinielaCode = finalCode,
                resultsJson = resultsJson,
                winnersJson = winnersJson,
                isSent = isSentByServer
            )
            val newId = database.quinielaDao().insertQuiniela(entity).toInt()
            withContext(Dispatchers.Main) {
                currentId = newId
                originalData = entity.copy(id = newId)
                if (!forceOverwrite) {
                    Toast.makeText(context, "¡Quiniela guardada!", Toast.LENGTH_SHORT).show()
                }
                showOverwriteDialog = false
                onComplete?.invoke()
            }
        }
    }

    LaunchedEffect(Unit) {
        editState.onSaveAndExit = { onConfirm ->
            saveQuinielaToRoom(forceOverwrite = true) {
                onConfirm()
            }
        }
        editState.onDiscardAndExit = { onConfirm ->
            onConfirm()
        }
    }

    androidx.activity.compose.BackHandler(enabled = hasChanges) {
        onBack()
    }

    fun sendQuinielaToFirebase(collectionPath: String) {
        val resultsForFirebase = matchResults.mapValues { (_, result) ->
            mapOf(
                "homeScore" to result.homeScore,
                "awayScore" to result.awayScore
            )
        }

        val quinielaData = hashMapOf(
            "quinielaName" to quinielaName.trim(),
            "propietarioName" to propietarioName.trim(),
            "userEmail" to userEmail.lowercase().trim(),
            "quinielaCode" to quinielaCode.trim().ifBlank { "Male2026" },
            "results" to resultsForFirebase,
            "groupWinners" to groupWinners,
            "updatedAt" to System.currentTimeMillis(),
            "status" to if (collectionPath == "quinielas") "received" else "saved",
            "emailStatus" to "pending",
            "paymentReceived" to false
        )

        if (collectionPath == "quinielas") {
            val documentId = "${userEmail}_${quinielaName}_${propietarioName}"
                .lowercase()
                .trim()
                .replace(" ", "_")
                .replace(".", "_")
                .replace("@", "_")

            firestore.collection("quinielas")
                .document(documentId)
                .set(quinielaData)
                .addOnSuccessListener {
                    isSentByServer = true
                    saveQuinielaToRoom(forceOverwrite = true) // Update local status as well
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val url = URL("https://n8n.beetikmx.com/webhook/quiniela-recibida")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "POST"
                            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                            connection.doOutput = true

                            val payload = quinielaData.toMutableMap()
                            payload["documentId"] = documentId
                            val jsonPayload = gson.toJson(payload)

                            connection.outputStream.use { os ->
                                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                            }

                            val responseCode = connection.responseCode
                            connection.disconnect()

                            withContext(Dispatchers.Main) {
                                if (responseCode in 200..299) {
                                    Toast.makeText(context, "¡Quiniela enviada correctamente!", Toast.LENGTH_LONG).show()
                                } else {
                                    // Webhook failed but Firestore succeeded
                                    Toast.makeText(context, "¡Quiniela registrada! (Error en Webhook)", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "¡Quiniela registrada!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(context, "Error al sincronizar con la nube: ${error.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // For "guardadas"
            val documentId = userEmail.lowercase().trim().replace("@", "_").replace(".", "_")
            val mapKey = "${quinielaName.trim()} - ${propietarioName.trim()}"

            firestore.collection("guardadas")
                .document(documentId)
                .set(mapOf(mapKey to quinielaData), SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(context, "¡Copia en la nube actualizada!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(context, "Error al sincronizar con la nube: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    fun fetchQuinielasFromServer(email: String) {
        val currentEmail = email.lowercase().trim()
        if (currentEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            Toast.makeText(context, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
            return
        }

        isFetchingQuinielas = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1. Get sent quinielas
                val sentSnap = firestore.collection("quinielas")
                    .whereEqualTo("userEmail", currentEmail)
                    .get().await()
                
                // 2. Get saved quinielas
                val docId = currentEmail.replace("@", "_").replace(".", "_")
                val savedSnap = firestore.collection("guardadas")
                    .document(docId)
                    .get().await()

                val tempList = mutableListOf<QuinielaEntity>()

                // Parse sent quinielas
                sentSnap.documents.forEach { doc ->
                    val qName = doc.getString("quinielaName") ?: ""
                    val oName = doc.getString("propietarioName") ?: ""
                    val qCode = doc.getString("quinielaCode") ?: ""
                    val resultsRaw = doc.get("results") as? Map<String, Map<String, String>>
                    val winnersRaw = doc.get("groupWinners") as? Map<String, String>
                    
                    if (qName.isNotBlank() && oName.isNotBlank() && resultsRaw != null) {
                        val results = resultsRaw.mapValues { (_, v) -> 
                            MatchResult(v["homeScore"] ?: "", v["awayScore"] ?: "")
                        }
                        
                        tempList.add(QuinielaEntity(
                            id = -1, // Not saved locally yet
                            quinielaName = qName,
                            propietarioName = oName,
                            userEmail = currentEmail,
                            quinielaCode = qCode,
                            resultsJson = gson.toJson(results),
                            winnersJson = gson.toJson(winnersRaw ?: emptyMap<String, String>()),
                            isSent = true
                        ))
                    }
                }

                // Parse saved quinielas
                if (savedSnap.exists()) {
                    val data = savedSnap.data
                    data?.forEach { (_, qData) ->
                        val qMap = qData as? Map<String, Any>
                        if (qMap != null) {
                            val qName = qMap["quinielaName"] as? String ?: ""
                            val oName = qMap["propietarioName"] as? String ?: ""
                            val qCode = qMap["quinielaCode"] as? String ?: ""
                            val resultsRaw = qMap["results"] as? Map<String, Map<String, String>>
                            val winnersRaw = qMap["groupWinners"] as? Map<String, String>

                            if (qName.isNotBlank() && oName.isNotBlank() && resultsRaw != null) {
                                val results = resultsRaw.mapValues { (_, v) -> 
                                    MatchResult(v["homeScore"] ?: "", v["awayScore"] ?: "")
                                }

                                tempList.add(QuinielaEntity(
                                    id = -1,
                                    quinielaName = qName,
                                    propietarioName = oName,
                                    userEmail = currentEmail,
                                    quinielaCode = qCode,
                                    resultsJson = gson.toJson(results),
                                    winnersJson = gson.toJson(winnersRaw ?: emptyMap<String, String>()),
                                    isSent = (qMap["status"] as? String) == "received"
                                ))
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isFetchingQuinielas = false
                    if (tempList.isEmpty()) {
                        Toast.makeText(context, "No se encontraron quinielas para este correo.", Toast.LENGTH_LONG).show()
                    } else {
                        savedQuinielas = tempList
                        showLoadDialog = true
                        showEmailDialog = false
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isFetchingQuinielas = false
                    Toast.makeText(context, "Error al cargar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun loadFirstQuinielaFromRoom() {
        if (userViewModel.isLoggedIn) {
            targetEmailToLoad = userViewModel.email
        }
        showEmailDialog = true
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { if (!isFetchingQuinielas) showEmailDialog = false },
            title = { Text("Cargar desde la Nube") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el correo electrónico para buscar tus quinielas en el servidor.")
                    OutlinedTextField(
                        value = targetEmailToLoad,
                        onValueChange = { targetEmailToLoad = it },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isFetchingQuinielas
                    )
                    if (isFetchingQuinielas) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { fetchQuinielasFromServer(targetEmailToLoad) },
                    enabled = !isFetchingQuinielas && targetEmailToLoad.isNotBlank()
                ) {
                    Text("Buscar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmailDialog = false },
                    enabled = !isFetchingQuinielas
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Seleccionar Quiniela") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedQuinielas.size) { index ->
                        val entity = savedQuinielas[index]
                        
                        // Check if the quiniela is complete (all markers filled and group winners selected)
                        val isComplete = remember(entity.resultsJson, entity.winnersJson, entity.userEmail) {
                            try {
                                val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                                val winnersType = object : TypeToken<Map<String, String>>() {}.type
                                
                                val results: Map<String, MatchResult> = gson.fromJson(entity.resultsJson, resultsType)
                                val winners: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                                
                                val matchesComplete = results.size == allMatches.size && results.values.all { 
                                    it.homeScore.isNotEmpty() && it.awayScore.isNotEmpty() 
                                }
                                
                                val winnersComplete = groupNames.all { groupName ->
                                    winners.containsKey(groupName)
                                }
                                
                                val emailComplete = entity.userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(entity.userEmail).matches()
                                
                                matchesComplete && winnersComplete && emailComplete
                            } catch (_: Exception) {
                                false
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isComplete) {
                                    Color(0xFFE3F2FD) // Light Blue Background
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = if (isComplete) {
                                BorderStroke(1.dp, Color(0xFF2196F3)) // Blue Border
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentId = entity.id
                                        originalData = entity
                                        quinielaName = entity.quinielaName
                                        propietarioName = entity.propietarioName
                                        userEmail = entity.userEmail
                                        quinielaCode = entity.quinielaCode
                                        
                                        val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                                        val winnersType = object : TypeToken<Map<String, String>>() {}.type
                                        
                                        matchResults = gson.fromJson(entity.resultsJson, resultsType)
                                        groupWinners = try {
                                            gson.fromJson(entity.winnersJson, winnersType)
                                        } catch (_: Exception) {
                                            emptyMap()
                                        }
                                        isSentByServer = entity.isSent
                                        
                                        showLoadDialog = false
                                        Toast.makeText(context, "Quiniela cargada.", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entity.quinielaName.ifEmpty { "(Sin nombre)" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isComplete) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "De: ${entity.propietarioName.ifEmpty { "Anónimo" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isComplete) Color(0xFF1565C0).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                        if (isComplete) {
                                        Text(
                                            text = "✓ Completa",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1565C0),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
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
            dismissButton = null
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
                                val updatedEntities = database.quinielaDao().getAllQuinielas()
                                withContext(Dispatchers.Main) {
                                    savedQuinielas = updatedEntities
                                    quinielaToDeleteLocal = null
                                    if (updatedEntities.isEmpty()) {
                                        showLoadDialog = false
                                    }
                                    Toast.makeText(context, "Quiniela eliminada del dispositivo.", Toast.LENGTH_SHORT).show()
                                }
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
                                            val updatedEntities = database.quinielaDao().getAllQuinielas()
                                            withContext(Dispatchers.Main) {
                                                savedQuinielas = updatedEntities
                                                quinielaToDeleteServer = null
                                                if (updatedEntities.isEmpty()) {
                                                    showLoadDialog = false
                                                }
                                                Toast.makeText(context, "Quiniela eliminada de todos lados.", Toast.LENGTH_SHORT).show()
                                            }
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

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("¿Borrar todo?") },
            text = { Text("Se eliminarán todos los marcadores, los ganadores de grupo, el nombre, el propietario y el correo. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        quinielaName = ""
                        propietarioName = ""
                        userEmail = ""
                        matchResults = allMatches.associate { it.id to MatchResult() }
                        groupWinners = emptyMap()
                        currentId = -1
                        originalData = null
                        showValidationErrors = false
                        showClearAllDialog = false
                        Toast.makeText(context, "Contenido borrado.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Borrado")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showOverwriteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showOverwriteDialog = false
            },
            title = { Text("Quiniela existente") },
            text = { Text("Ya existe una quiniela con el mismo nombre y propietario. ¿Deseas sobreescribirla?") },
            confirmButton = {
                Button(
                    onClick = {
                        saveQuinielaToRoom(forceOverwrite = true) {
                            sendQuinielaToFirebase("guardadas")
                        }
                    }
                ) {
                    Text("Sobreescribir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showOverwriteDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSendConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSendConfirmDialog = false },
            title = { Text("Confirmar Envío") },
            text = { Text("¿Estás seguro de que deseas enviar tu quiniela oficial? Una vez enviada, se registrará con tus datos actuales.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSendConfirmDialog = false
                        saveQuinielaToRoom(forceOverwrite = true) {
                            sendQuinielaToFirebase("quinielas")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE61D25))
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Use a Set of expanded group names to fix the rememberSaveable crash.
    // Sets of Strings are natively serializable in a Bundle.
    var expandedGroupNames by rememberSaveable {
        mutableStateOf(groupNames.toSet())
    }

    val isSaveEnabled = remember(quinielaName, propietarioName, userEmail, matchResults, groupWinners) {
        quinielaName.isNotBlank() || 
        propietarioName.isNotBlank() || 
        userEmail.isNotBlank() ||
        matchResults.values.any { it.homeScore.isNotEmpty() || it.awayScore.isNotEmpty() } ||
        groupWinners.isNotEmpty()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_quiniela),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.15f
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Llenar Quiniela", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Gold,
                        navigationIconContentColor = Gold
                    )
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                saveQuinielaToRoom {
                                    sendQuinielaToFirebase("guardadas")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isSaveEnabled,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.8f),
                                contentColor = Color(0xFF2A398D)
                            ),
                            border = BorderStroke(2.dp, Color(0xFF2A398D))
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val matchesComplete = allMatches.all { match ->
                                    val res = matchResults[match.id]
                                    res != null && res.homeScore.isNotEmpty() && res.awayScore.isNotEmpty()
                                }
                                
                                val winnersComplete = groupNames.all { groupName ->
                                    groupWinners[groupName] != null
                                }

                                val emailComplete = userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()

                                if (matchesComplete && winnersComplete && emailComplete) {
                                    showValidationErrors = false
                                    showSendConfirmDialog = true
                                } else {
                                    showValidationErrors = true
                                    val message = when {
                                        !emailComplete -> "Por favor ingresa un correo electrónico válido."
                                        !matchesComplete && !winnersComplete -> "Por favor llena todos los marcadores y elige los ganadores de grupo."
                                        !matchesComplete -> "Por favor llena todos los marcadores."
                                        else -> "Por favor elige el ganador de cada grupo."
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSentByServer,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE61D25),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE61D25).copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                text = if (isSentByServer) "Enviada ✓" else "Enviar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Custom Title
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Black Outline
                                Text(
                                    text = "Quiniela Malenka",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        drawStyle = Stroke(
                                            width = 4f,
                                            join = StrokeJoin.Round
                                        )
                                    ),
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                // Fill color that combines with the World Cup theme (Dark Blue)
                                Text(
                                    text = "Quiniela Malenka",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF2A398D),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Box(contentAlignment = Alignment.Center) {
                                val mexicoText = buildAnnotatedString {
                                    // Green: Méx
                                    withStyle(style = SpanStyle(color = Color(0xFF006847))) {
                                        append("Méx")
                                    }
                                    // White: ico 
                                    withStyle(style = SpanStyle(color = Color.White)) {
                                        append("ico ")
                                    }
                                    // Red: 2026
                                    withStyle(style = SpanStyle(color = Color(0xFFCE1126))) {
                                        append("2026")
                                    }
                                }
                                
                                // Black Outline
                                Text(
                                    text = mexicoText.text,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        drawStyle = Stroke(
                                            width = 10f,
                                            join = StrokeJoin.Round
                                        )
                                    ),
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                
                                // Colored Fill
                                Text(
                                    text = mexicoText,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            TextButton(
                                onClick = { loadFirstQuinielaFromRoom() },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Cargar una quiniela", style = MaterialTheme.typography.labelMedium)
                            }

                            TextButton(
                                onClick = { showClearAllDialog = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "Borrar todo",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        OutlinedTextField(
                            value = propietarioName,
                            onValueChange = { 
                                propietarioName = it
                                isSentByServer = false 
                            },
                            label = { Text("Propietario de esta quiniela") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        )

                        OutlinedTextField(
                            value = quinielaName,
                            onValueChange = { 
                                quinielaName = it
                                isSentByServer = false 
                            },
                            label = { Text("Nombre de la quiniela") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        )

                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = { 
                                userEmail = it
                                isSentByServer = false 
                            },
                            label = { Text("Correo electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        )

                        OutlinedTextField(
                            value = quinielaCode,
                            onValueChange = { 
                                quinielaCode = it
                                isSentByServer = false 
                            },
                            label = { Text("Código de Quiniela") },
                            placeholder = { Text("Opcional") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    val firstGroup = groupNames.firstOrNull()
                                    if (firstGroup != null) {
                                        val wasExpanded = expandedGroupNames.contains(firstGroup)
                                        if (!wasExpanded) {
                                            expandedGroupNames = expandedGroupNames + firstGroup
                                        }
                                        
                                        coroutineScope.launch {
                                            if (!wasExpanded) {
                                                delay(150)
                                            }
                                            focusManager.moveFocus(FocusDirection.Next)
                                        }
                                    } else {
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                groupNames.forEachIndexed { index, groupName ->
                    val groupMatches = groups[groupName] ?: emptyList()
                    val nextGroupName = if (index < groupNames.size - 1) groupNames[index + 1] else null

                    item {
                        GroupSection(
                            groupName = groupName,
                            matches = groupMatches,
                            matchResults = matchResults,
                            groupWinner = groupWinners[groupName],
                            showErrors = showValidationErrors,
                            isExpanded = expandedGroupNames.contains(groupName),
                            onToggleExpansion = {
                                expandedGroupNames = if (expandedGroupNames.contains(groupName)) {
                                    expandedGroupNames - groupName
                                } else {
                                    expandedGroupNames + groupName
                                }
                            },
                            onResultChange = { id, result ->
                                matchResults = matchResults.toMutableMap().apply {
                                    this[id] = result
                                }
                                isSentByServer = false
                            },
                            onWinnerChange = { winner ->
                                groupWinners = groupWinners.toMutableMap().apply {
                                    this[groupName] = winner
                                }
                                isSentByServer = false
                            },
                            onLastMatchNext = {
                                nextGroupName?.let { nextGroup ->
                                    val wasExpanded = expandedGroupNames.contains(nextGroup)
                                    expandedGroupNames = expandedGroupNames + nextGroup
                                    
                                    coroutineScope.launch {
                                        if (!wasExpanded) {
                                            delay(150)
                                        }
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSection(
    groupName: String,
    matches: List<Match>,
    matchResults: Map<String, MatchResult>,
    groupWinner: String?,
    showErrors: Boolean,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onResultChange: (String, MatchResult) -> Unit,
    onWinnerChange: (String) -> Unit,
    onLastMatchNext: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 0f else -90f, label = "rotation")

    val teams = remember(matches) {
        matches.flatMap { listOf(it.homeTeam to it.homeFlag, it.awayTeam to it.awayFlag) }
            .distinctBy { it.first }
    }

    val hasErrors = remember(matches, matchResults, groupWinner, showErrors) {
        showErrors && (
            groupWinner == null ||
            matches.any { match ->
                val res = matchResults[match.id]
                res == null || res.homeScore.isEmpty() || res.awayScore.isEmpty()
            }
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpansion() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.titleLarge,
                color = if (hasErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = if (hasErrors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // First Place Selection
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    border = if (showErrors && groupWinner == null) BorderStroke(2.dp, Color(0xFFFF9800)) else null
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¿Quién quedará en 1er Lugar del $groupName?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (showErrors && groupWinner == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = groupWinner?.let { winner ->
                                    val flag = teams.find { it.first == winner }?.second ?: ""
                                    "$flag $winner"
                                } ?: "Seleccionar ganador",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                                    .focusProperties { canFocus = false }
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                teams.forEach { (team, flag) ->
                                    DropdownMenuItem(
                                        text = { Text("$flag $team") },
                                        onClick = {
                                            onWinnerChange(team)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                matches.forEachIndexed { index, match ->
                    val result = matchResults[match.id] ?: MatchResult()
                    val isLastMatch = index == matches.size - 1
                    val isMatchInvalid = showErrors && (result.homeScore.isEmpty() || result.awayScore.isEmpty())
                    
                    MatchCard(
                        match = match,
                        homeScore = result.homeScore,
                        awayScore = result.awayScore,
                        isError = isMatchInvalid,
                        onHomeScoreChange = { newScore ->
                            onResultChange(match.id, result.copy(homeScore = newScore))
                        },
                        onAwayScoreChange = { newScore ->
                            onResultChange(match.id, result.copy(awayScore = newScore))
                        },
                        onLastImeAction = if (isLastMatch) onLastMatchNext else null
                    )
                }
            }
        }
    }
}

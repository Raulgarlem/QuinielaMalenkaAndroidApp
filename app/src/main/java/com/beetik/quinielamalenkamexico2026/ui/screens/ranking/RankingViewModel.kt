package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.RankingConfigEntity
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.model.RankingConfig
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RankingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = QuinielaDatabase.getDatabase(application)
    private val configDao = database.rankingConfigDao()
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)

    private val defaultCategoryNames = mapOf(
        1 to "Amigos", 2 to "Familia", 3 to "Trabajo", 4 to "Némesis", 5 to "Afectados por la Mayición"
    )

    /*
    private val officialParticipants = listOf(
        Participant("1", "Mi Quiniela", "Raúl (Tú)", true, mapOf("A1" to (3 to 1), "A2" to (1 to 0), "C1" to (3 to 0)), mapOf("Grupo A" to "México", "Grupo C" to "Brasil"), 5),
        ...
    )
    */

    var savedConfigs = mutableStateListOf<RankingConfig>()
    var currentConfigId by mutableStateOf("default")
    
    // UI state
    var resultsMap = mutableStateMapOf<String, MatchScore>()
    val baseParticipants = mutableStateListOf<Participant>()
    val officialParticipants = mutableStateListOf<Participant>()
    val pinnedParticipantCategories = mutableStateMapOf<String, Int>()
    val pinnedParticipantIds = mutableStateListOf<String>()
    var pinnedCategories = mutableStateMapOf<Int, PinCategory>()
    var isLiveRanking by mutableStateOf(false)
    var comparisonParticipantId by mutableStateOf<String?>(null)

    var isVisibleGroups by mutableStateOf(true)
    var isVisibleFinal by mutableStateOf(false)
    private var currentAccessCode: String = prefs.getString("access_code", "") ?: ""

    // Match state from Firestore
    var allMatches by mutableStateOf<List<Match>>(MatchRepository.allMatches)
        private set
    val confirmedIds = mutableStateListOf<String>()

    init {
        // PRE-INITIALIZE state
        defaultCategoryNames.forEach { (id, name) ->
            val color = when(id) {
                1 -> Gold; 2 -> Color(0xFF2196F3); 3 -> Color(0xFF9C27B0); 4 -> Color(0xFF4CAF50); else -> Color(0xFFFF4081)
            }
            pinnedCategories[id] = PinCategory(id, name, color)
        }
        loadAllConfigs()
        syncWithDatabase()
        observeMatches()
        
        // Initial load of official participants based on current session
        loadOfficialParticipants(currentAccessCode)
    }

    fun loadOfficialParticipants(accessCode: String) {
        currentAccessCode = accessCode
        if (accessCode.isBlank()) {
            officialParticipants.clear()
            updateBaseParticipants()
            return
        }

        val includeGroups = isVisibleGroups
        val includeFinal = isVisibleFinal
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("quinielas")
                    .whereEqualTo("quinielaCode", accessCode.trim())
                    .whereEqualTo("paymentReceived", true)
                    .get().await()

                val newOfficial = snapshot.documents.mapNotNull { doc ->
                    val isKnockout = doc.getBoolean("isKnockout") ?: false
                    if (!shouldIncludePhase(isKnockout, includeGroups, includeFinal)) return@mapNotNull null
                    val qName = doc.getString("quinielaName") ?: "Sin nombre"
                    val oName = doc.getString("propietarioName") ?: "Anónimo"
                    val resultsRaw = doc.get("results") as? Map<String, Map<String, String>>
                    val winnersRaw = doc.get("groupWinners") as? Map<String, String>
                    
                    val predictions = resultsRaw?.mapValues { (_, v) ->
                        (v["homeScore"]?.toIntOrNull() ?: 0) to (v["awayScore"]?.toIntOrNull() ?: 0)
                    } ?: emptyMap()

                    Participant(
                        id = doc.id,
                        quinielaName = qName,
                        ownerName = oName,
                        isUser = doc.getString("userEmail") == prefs.getString("user_email", ""),
                        predictions = predictions,
                        groupWinnerPredictions = winnersRaw ?: emptyMap(),
                        prevPosition = 1, // Logic for prev position could be added if needed
                        isKnockout = isKnockout
                    )
                }

                launch(Dispatchers.Main) {
                    officialParticipants.clear()
                    officialParticipants.addAll(newOfficial)
                    updateBaseParticipants()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateBaseParticipants() {
        // Find added participants from current state (only those starting with loaded_)
        val added = baseParticipants.filter { it.id.startsWith("loaded_") && shouldIncludePhase(it.isKnockout) }
        baseParticipants.clear()
        baseParticipants.addAll(officialParticipants)
        
        // Add "added" participants ensuring no ID duplicates with official ones
        val officialIds = officialParticipants.map { it.id }.toSet()
        added.forEach { lp ->
            if (lp.id !in officialIds) {
                baseParticipants.add(lp)
            }
        }
    }

    private fun shouldIncludePhase(isKnockout: Boolean): Boolean {
        return shouldIncludePhase(isKnockout, isVisibleGroups, isVisibleFinal)
    }

    private fun shouldIncludePhase(isKnockout: Boolean, includeGroups: Boolean, includeFinal: Boolean): Boolean {
        if (includeFinal) return isKnockout
        if (includeGroups) return !isKnockout
        return false
    }

    private var rawMatches: List<Match> = MatchRepository.allMatches

    private fun observeMatches() {
        viewModelScope.launch {
            // Monitor visibility flags
            firestore.collection("codigos").document("quinielaActiva")
                .addSnapshotListener { snapshot, _ ->
                    isVisibleGroups = snapshot?.getBoolean("visibleGroups") ?: false
                    isVisibleFinal = snapshot?.getBoolean("visibleFinal") ?: false
                    updateFilteredMatches()
                    loadOfficialParticipants(currentAccessCode)
                }

            MatchRepository.getMatchesFlow().collectLatest { updatedMatches ->
                Log.d("RankingViewModel", "Received ${updatedMatches.size} updated matches from Repository")
                updateFilteredMatches(updatedMatches)
                
                updatedMatches.forEach { match ->
                    val isLive = match.started && match.isActive
                    val isFinished = match.finished && !match.isActive
                    
                    if (isLive || isFinished) {
                        if (match.realHomeScore != null && match.realAwayScore != null) {
                            val realScore = MatchScore(match.realHomeScore, match.realAwayScore)
                            
                            // Si el partido está FINALIZADO, sincronizamos con resultsMap y confirmedIds
                            if (isFinished) {
                                var changed = false
                                if (resultsMap[match.id] != realScore) {
                                    Log.d("RankingViewModel", "Updating finished match ${match.id}: ${realScore.home}-${realScore.away}")
                                    resultsMap[match.id] = realScore
                                    changed = true
                                }
                                if (!confirmedIds.contains(match.id)) {
                                    confirmedIds.add(match.id)
                                    changed = true
                                }
                                if (changed) saveCurrentState()
                            }

                            // Si el partido volvió a estar en VIVO pero estaba como confirmado, lo quitamos de confirmados
                            if (isLive && confirmedIds.contains(match.id)) {
                                confirmedIds.remove(match.id)
                                // Opcionalmente podríamos quitarlo de resultsMap si queremos que use el real-time puro
                                // pero si el usuario lo simuló, quizá quiera mantener su simulación.
                                // Por ahora solo quitamos la "confirmación" para que el cálculo base lo ignore.
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateFilteredMatches(updatedMatches: List<Match>? = null) {
        if (updatedMatches != null) rawMatches = updatedMatches
        allMatches = rawMatches.filter { match ->
            val isGroup = match.group.startsWith("Grupo")
            if (isVisibleFinal) !isGroup else isVisibleGroups && isGroup
        }
    }

    private fun syncWithDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            database.quinielaDao().getAllQuinielasFlow().collect { allSaved ->
                val activeLoadedIds = allSaved.map { "loaded_${it.id}" }.toSet()
                launch(Dispatchers.Main) {
                    var currentConfigChanged = false
                    
                    // Iterate through ALL saved configurations to ensure consistency
                    savedConfigs.indices.forEach { i ->
                        val config = savedConfigs[i]
                        val originalCount = config.addedParticipants.size
                        val filteredAdded = config.addedParticipants.filter { it.id in activeLoadedIds }
                        
                        if (filteredAdded.size != originalCount) {
                            // Something was removed from this configuration
                            val removedIds = config.addedParticipants.filter { it.id !in activeLoadedIds }.map { it.id }.toSet()
                            
                            val updatedConfig = config.copy(
                                addedParticipants = filteredAdded,
                                pinnedParticipantCategories = config.pinnedParticipantCategories.filterKeys { it !in removedIds },
                                pinnedParticipantIds = config.pinnedParticipantIds.filter { it !in removedIds },
                                comparisonParticipantId = if (config.comparisonParticipantId in removedIds) null else config.comparisonParticipantId
                            )
                            
                            savedConfigs[i] = updatedConfig
                            saveConfigToDb(updatedConfig, updatedConfig.id == currentConfigId)
                            
                            if (updatedConfig.id == currentConfigId) {
                                currentConfigChanged = true
                            }
                        }
                    }
                    
                    // If the active configuration was affected, update the live UI state
                    if (currentConfigChanged) {
                        baseParticipants.removeAll { it.id.startsWith("loaded_") && it.id !in activeLoadedIds }
                        pinnedParticipantCategories.keys.toList().forEach { id ->
                            if (id.startsWith("loaded_") && id !in activeLoadedIds) {
                                pinnedParticipantCategories.remove(id)
                            }
                        }
                        pinnedParticipantIds.removeAll { it.startsWith("loaded_") && it !in activeLoadedIds }
                        
                        val compId = comparisonParticipantId
                        if (compId != null && compId.startsWith("loaded_") && compId !in activeLoadedIds) {
                            comparisonParticipantId = null
                        }
                    }
                }
            }
        }
    }

    private fun loadAllConfigs() {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = configDao.getAllConfigs()
            launch(Dispatchers.Main) {
                if (entities.isEmpty()) {
                    // Create default
                    val defaultConfig = RankingConfig(
                        id = "default",
                        configName = "Configuración Predeterminada",
                        resultsMap = emptyMap(),
                        addedParticipants = emptyList(),
                        pinnedParticipantCategories = emptyMap(),
                        pinnedParticipantIds = emptyList(),
                        categoryNames = defaultCategoryNames,
                        isLiveRanking = false,
                        comparisonParticipantId = null
                    )
                    savedConfigs.add(defaultConfig)
                    loadConfigIntoState(defaultConfig)
                    saveConfigToDb(defaultConfig, true)
                } else {
                    savedConfigs.clear()
                    entities.forEach { entity ->
                        savedConfigs.add(entity.toModel())
                    }
                    val active = entities.find { it.isActive } ?: entities[0]
                    loadConfigIntoState(active.toModel())
                }
            }
        }
    }

    fun loadConfigIntoState(config: RankingConfig) {
        currentConfigId = config.id
        resultsMap.clear()
        resultsMap.putAll(config.resultsMap)
        
        baseParticipants.clear()
        baseParticipants.addAll(officialParticipants)
        baseParticipants.addAll(config.addedParticipants.filter { shouldIncludePhase(it.isKnockout) })
        
        pinnedParticipantCategories.clear()
        pinnedParticipantCategories.putAll(config.pinnedParticipantCategories)
        
        pinnedParticipantIds.clear()
        pinnedParticipantIds.addAll(config.pinnedParticipantIds)
        
        pinnedCategories.clear()
        config.categoryNames.forEach { (id, name) ->
            val color = when(id) {
                1 -> Gold; 2 -> Color(0xFF2196F3); 3 -> Color(0xFF9C27B0); 4 -> Color(0xFF4CAF50); else -> Color(0xFFFF4081)
            }
            pinnedCategories[id] = PinCategory(id, name, color)
        }
        
        isLiveRanking = config.isLiveRanking
        comparisonParticipantId = config.comparisonParticipantId
        
        // Sync active state in DB
        viewModelScope.launch(Dispatchers.IO) {
            configDao.deactivateAll()
            configDao.setActive(config.id)
        }
    }

    fun saveCurrentState() {
        val currentIdx = savedConfigs.indexOfFirst { it.id == currentConfigId }
        if (currentIdx != -1) {
            val updated = savedConfigs[currentIdx].copy(
                resultsMap = resultsMap.toMap(),
                addedParticipants = baseParticipants.filter { it.id.startsWith("loaded_") },
                pinnedParticipantCategories = pinnedParticipantCategories.toMap(),
                pinnedParticipantIds = pinnedParticipantIds.toList(),
                categoryNames = pinnedCategories.values.associate { it.id to it.name },
                isLiveRanking = isLiveRanking,
                comparisonParticipantId = comparisonParticipantId
            )
            savedConfigs[currentIdx] = updated
            saveConfigToDb(updated, true)
        }
    }

    private fun saveConfigToDb(config: RankingConfig, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            configDao.insertConfig(config.toEntity(isActive))
        }
    }

    fun onResetAll() {
        resultsMap.clear()
        baseParticipants.removeAll { it.id.startsWith("loaded_") }
        pinnedParticipantCategories.clear()
        pinnedParticipantIds.clear()
        comparisonParticipantId = null
        isLiveRanking = false
        saveCurrentState()
    }

    fun onClearResults() { resultsMap.clear(); saveCurrentState() }
    fun onClearParticipants() { baseParticipants.removeAll { it.id.startsWith("loaded_") }; saveCurrentState() }
    fun onClearCategories() { pinnedParticipantCategories.clear(); pinnedParticipantIds.clear(); saveCurrentState() }

    fun onRemoveParticipant(id: String) {
        if (comparisonParticipantId == id) comparisonParticipantId = null
        baseParticipants.removeAll { it.id == id }
        pinnedParticipantCategories.remove(id)
        pinnedParticipantIds.remove(id)
        saveCurrentState()
    }

    fun onToggleComparison(id: String) {
        comparisonParticipantId = if (comparisonParticipantId == id) null else id
        saveCurrentState()
    }

    fun onSaveCurrentAsNew(name: String) {
        saveCurrentState()
        val newId = UUID.randomUUID().toString()
        val current = savedConfigs.find { it.id == currentConfigId } ?: savedConfigs[0]
        val newConfig = current.copy(id = newId, configName = name)
        savedConfigs.add(newConfig)
        loadConfigIntoState(newConfig)
    }

    fun onDeleteConfig(id: String) {
        if (id != "default") {
            val configToDelete = savedConfigs.find { it.id == id }
            if (configToDelete != null) {
                if (currentConfigId == id) loadConfigIntoState(savedConfigs[0])
                savedConfigs.remove(configToDelete)
                viewModelScope.launch(Dispatchers.IO) {
                    configDao.deleteConfig(configToDelete.toEntity(false))
                }
            }
        }
    }

    // Mapping extensions
    private fun RankingConfigEntity.toModel(): RankingConfig {
        val resultsType = object : TypeToken<Map<String, MatchScore>>() {}.type
        val participantsType = object : TypeToken<List<Participant>>() {}.type
        val pinnedCatsType = object : TypeToken<Map<String, Int>>() {}.type
        val pinnedIdsType = object : TypeToken<List<String>>() {}.type
        val catNamesType = object : TypeToken<Map<Int, String>>() {}.type

        return RankingConfig(
            id = id,
            configName = configName,
            resultsMap = gson.fromJson(resultsJson, resultsType) ?: emptyMap(),
            addedParticipants = gson.fromJson(addedParticipantsJson, participantsType) ?: emptyList(),
            pinnedParticipantCategories = gson.fromJson(pinnedCategoriesJson, pinnedCatsType) ?: emptyMap(),
            pinnedParticipantIds = gson.fromJson(pinnedIdsJson, pinnedIdsType) ?: emptyList(),
            categoryNames = gson.fromJson(categoryNamesJson, catNamesType) ?: defaultCategoryNames,
            isLiveRanking = isLiveRanking,
            comparisonParticipantId = comparisonParticipantId
        )
    }

    private fun RankingConfig.toEntity(isActive: Boolean): RankingConfigEntity {
        return RankingConfigEntity(
            id = id,
            configName = configName,
            resultsJson = gson.toJson(resultsMap),
            addedParticipantsJson = gson.toJson(addedParticipants),
            pinnedCategoriesJson = gson.toJson(pinnedParticipantCategories),
            pinnedIdsJson = gson.toJson(pinnedParticipantIds),
            categoryNamesJson = gson.toJson(categoryNames),
            isLiveRanking = isLiveRanking,
            comparisonParticipantId = comparisonParticipantId,
            isActive = isActive
        )
    }
}

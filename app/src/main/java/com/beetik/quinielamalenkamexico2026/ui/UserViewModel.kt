package com.beetik.quinielamalenkamexico2026.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.util.ScoreCalculator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()
    private val database = QuinielaDatabase.getDatabase(application)

    var name by mutableStateOf(prefs.getString("user_name", "") ?: "")
        private set

    var email by mutableStateOf(prefs.getString("user_email", "") ?: "")
        private set

    var accessCode by mutableStateOf(prefs.getString("access_code", "") ?: "")
        private set

    var isLoggedIn by mutableStateOf(prefs.getBoolean("is_logged_in", false))
        private set

    var quinielaCount by mutableIntStateOf(0)
        private set

    var bestScore by mutableIntStateOf(0)
        private set

    var rankingPosition by mutableStateOf("-")
        private set

    var globalHits by mutableStateOf("-")
        private set

    var rankTitle by mutableStateOf("Sin Código")
        private set

    var codeError by mutableStateOf<String?>(null)
        private set

    var isValidatingCode by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    var isAdmin by mutableStateOf(false)
        private set

    var isFaseGruposActive by mutableStateOf(true)
        private set

    var isFaseFinalActive by mutableStateOf(true)
        private set

    var isVisibleGroups by mutableStateOf(true)
        private set
    var isVisibleFinal by mutableStateOf(false)
        private set

    private val officialParticipantsFlow = mutableStateOf<List<Pair<Map<String, MatchResult>, Map<String, String>>>>(emptyList())

    init {
        observeLocalStats()
        if (isLoggedIn && email.isNotBlank()) {
            fetchFirebaseStats()
        }
    }

    private fun observeLocalStats() {
        viewModelScope.launch {
            val matchesFlow = MatchRepository.getMatchesFlow()
                .onStart { emit(MatchRepository.allMatches) }
            val quinielasFlow = database.quinielaDao().getAllQuinielasFlow()
            val emailFlow = snapshotFlow { email }
            val officialFlow = snapshotFlow { officialParticipantsFlow.value }
            val visibilityFlow = snapshotFlow { isVisibleGroups to isVisibleFinal }

            combine(matchesFlow, quinielasFlow, emailFlow, officialFlow, visibilityFlow) { matches, quinielas, currentEmail, officialPreds, visibility ->
                val (includeGroups, includeFinal) = visibility
                // Filter matches based on visibility flags
                val filteredMatches = matches.filter { match ->
                    val isGroup = match.group.startsWith("Grupo")
                    if (includeFinal) !isGroup else includeGroups && isGroup
                }

                val myQuinielas = quinielas.filter {
                    it.userEmail.lowercase().trim() == currentEmail.lowercase().trim() &&
                        shouldIncludePhase(it.isKnockout, includeGroups, includeFinal)
                }
                
                val finishedMatches = filteredMatches.count { it.finished }
                val matchesByGroup = filteredMatches.groupBy { it.group }
                val finishedGroups = matchesByGroup.filterKeys { it.startsWith("Grupo") }.count { (_, gMatches) ->
                    gMatches.all { it.finished } 
                }
                val totalItems = finishedMatches + finishedGroups

                var maxP = -1
                var bestHits = 0
                myQuinielas.forEach { entity ->
                    try {
                        val resultsType = object : TypeToken<Map<String, MatchResult>>() {}.type
                        val winnersType = object : TypeToken<Map<String, String>>() {}.type
                        val results: Map<String, MatchResult> = gson.fromJson(entity.resultsJson, resultsType)
                        val winners: Map<String, String> = gson.fromJson(entity.winnersJson, winnersType)
                        
                        val stats = ScoreCalculator.calculateStats(filteredMatches, results, winners)
                        if (stats.totalPoints > maxP) {
                            maxP = stats.totalPoints
                            bestHits = stats.hits
                        }
                    } catch (_: Exception) {}
                }

                val officialScores = officialPreds.map { (res, winners) ->
                    ScoreCalculator.calculateStats(filteredMatches, res, winners).totalPoints
                }

                val posText = if (officialScores.isNotEmpty() && maxP >= 0) {
                    val betterCount = officialScores.count { it > maxP }
                    "${betterCount + 1} / ${officialScores.size}"
                } else "-"

                val hitsText = if (totalItems > 0 && maxP >= 0) {
                    "$bestHits / $totalItems"
                } else "-"

                listOf(myQuinielas.size, if (maxP < 0) 0 else maxP, posText, hitsText)
            }.collect { (count, maxP, pos, hits) ->
                quinielaCount = count as Int
                bestScore = maxP as Int
                rankingPosition = pos as String
                globalHits = hits as String
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

    fun login(userName: String, userEmail: String, code: String) {
        name = userName
        email = userEmail
        accessCode = code
        isLoggedIn = true
        codeError = null
        prefs.edit {
            putString("user_name", userName)
            putString("user_email", userEmail)
            putString("access_code", code)
            putBoolean("is_logged_in", true)
        }
        if (code.isBlank()) {
            rankTitle = "Sin Código"
            isValidatingCode = false
        } else {
            fetchFirebaseStats()
        }
    }

    fun updateAccessCode(newCode: String) {
        accessCode = newCode
        codeError = null
        prefs.edit {
            putString("access_code", newCode)
        }
        if (newCode.isBlank()) {
            rankTitle = "Sin Código"
            isValidatingCode = false
        } else {
            fetchFirebaseStats()
        }
    }

    fun logout() {
        name = ""
        email = ""
        accessCode = ""
        isLoggedIn = false
        quinielaCount = 0
        bestScore = 0
        rankTitle = "Sin Código"
        codeError = null
        prefs.edit { clear() }
    }

    fun clearCodeError() {
        codeError = null
    }

    fun syncQuinielasFromCloud(database: QuinielaDatabase, onComplete: () -> Unit = {}) {
        val currentEmail = email.lowercase().trim()
        if (currentEmail.isBlank()) return
        
        isSyncing = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sentSnap = firestore.collection("quinielas")
                    .whereEqualTo("userEmail", currentEmail)
                    .get().await()
                
                val docId = currentEmail.replace("@", "_").replace(".", "_")
                val savedSnap = firestore.collection("guardadas")
                    .document(docId)
                    .get().await()

                val dao = database.quinielaDao()

                // Process sent quinielas
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
                        
                        val existing = dao.getQuinielaByNameAndOwner(qName, oName)
                        val entity = QuinielaEntity(
                            id = existing?.id ?: 0,
                            quinielaName = qName,
                            propietarioName = oName,
                            userEmail = currentEmail,
                            quinielaCode = qCode,
                            resultsJson = gson.toJson(results),
                            winnersJson = gson.toJson(winnersRaw ?: emptyMap<String, String>()),
                            isSent = true,
                            isFavorite = existing?.isFavorite ?: false,
                            isKnockout = doc.getBoolean("isKnockout") ?: false
                        )
                        dao.insertQuiniela(entity)
                    }
                }

                // Process saved quinielas
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

                                val existing = dao.getQuinielaByNameAndOwner(qName, oName)
                                val entity = QuinielaEntity(
                                    id = existing?.id ?: 0,
                                    quinielaName = qName,
                                    propietarioName = oName,
                                    userEmail = currentEmail,
                                    quinielaCode = qCode,
                                    resultsJson = gson.toJson(results),
                                    winnersJson = gson.toJson(winnersRaw ?: emptyMap<String, String>()),
                                    isSent = (qMap["status"] as? String) == "received",
                                    isFavorite = existing?.isFavorite ?: false,
                                    isKnockout = qMap["isKnockout"] as? Boolean ?: false
                                )
                                dao.insertQuiniela(entity)
                            }
                        }
                    }
                }

                launch(Dispatchers.Main) {
                    isSyncing = false
                    fetchFirebaseStats() // Refresh counts
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    isSyncing = false
                    onComplete()
                }
            }
        }
    }

    fun fetchFirebaseStats() {
        val currentCode = accessCode.trim()
        isValidatingCode = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check Admin status
                val userDocId = email.lowercase().trim().replace("@", "_").replace(".", "_")
                if (userDocId.isNotBlank()) {
                    val userDoc = firestore.collection("guardadas").document(userDocId).get().await()
                    isAdmin = userDoc.getBoolean("isAdmin") ?: false
                }

                // Get active phases
                val activeSnap = firestore.collection("codigos").document("quinielaActiva").get().await()
                isFaseGruposActive = activeSnap.getBoolean("faseGrupos") ?: false
                isFaseFinalActive = activeSnap.getBoolean("faseFinal") ?: false
                isVisibleGroups = activeSnap.getBoolean("visibleGroups") ?: false
                isVisibleFinal = activeSnap.getBoolean("visibleFinal") ?: false
                val includeGroups = isVisibleGroups
                val includeFinal = isVisibleFinal

                // Get access codes / titles
                val codesSnap = firestore.collection("codigos")
                    .document("creados")
                    .get().await()

                // Determine rankTitle based on accessCode
                var newTitle = "Sin Código"
                var error: String? = null
                
                if (currentCode.isNotBlank() && codesSnap.exists()) {
                    val codesData = codesSnap.data
                    var found = false
                    if (codesData != null) {
                        for ((title, codeValue) in codesData) {
                            if (codeValue.toString().trim() == currentCode) {
                                // Add space before each capital letter (except the first one)
                                newTitle = title.replace(Regex("(?<=.)(?=\\p{Lu})"), " ")
                                found = true
                                break
                            }
                        }
                    }
                    if (!found) {
                        error = "Código erróneo, verifique el código proporcionado, mantenga Mayúsculas, minúsculas y cuide no poner espacios al final. O deje en blanco el campo para continuar"
                    }
                }

                // Fetch official participants for the code to calculate global ranking
                val officialList: List<Pair<Map<String, MatchResult>, Map<String, String>>> = if (currentCode.isNotBlank() && error == null) {
                    val snapshot = firestore.collection("quinielas")
                        .whereEqualTo("quinielaCode", currentCode.trim())
                        .whereEqualTo("paymentReceived", true)
                        .get().await()
                    
                    snapshot.documents.mapNotNull { doc ->
                        val isKnockout = doc.getBoolean("isKnockout") ?: false
                        if (!shouldIncludePhase(isKnockout, includeGroups, includeFinal)) return@mapNotNull null

                        val resultsRaw = doc.get("results") as? Map<String, Map<String, String>>
                        val winnersRaw = doc.get("groupWinners") as? Map<String, String>
                        val res = resultsRaw?.mapValues { (_, v) ->
                            MatchResult(v["homeScore"] ?: "", v["awayScore"] ?: "")
                        } ?: emptyMap()
                        res to (winnersRaw ?: emptyMap())
                    }
                } else emptyList()

                launch(Dispatchers.Main) {
                    rankTitle = newTitle
                    codeError = error
                    isValidatingCode = false
                    officialParticipantsFlow.value = officialList
                }

            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    isValidatingCode = false
                }
            }
        }
    }
}

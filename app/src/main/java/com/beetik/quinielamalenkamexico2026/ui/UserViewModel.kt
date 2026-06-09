package com.beetik.quinielamalenkamexico2026.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()

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

    var rankTitle by mutableStateOf("Sin Código")
        private set

    var codeError by mutableStateOf<String?>(null)
        private set

    var isValidatingCode by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    init {
        if (isLoggedIn && email.isNotBlank()) {
            fetchFirebaseStats()
        }
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
                            isFavorite = existing?.isFavorite ?: false
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
                                    isFavorite = existing?.isFavorite ?: false
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
        val currentEmail = email.lowercase().trim()
        val currentCode = accessCode.trim()
        isValidatingCode = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get sent quinielas
                val sentSnap = if (currentEmail.isNotBlank()) {
                    firestore.collection("quinielas")
                        .whereEqualTo("userEmail", currentEmail)
                        .get().await()
                } else null
                
                // 2. Get saved quinielas
                val savedSnap = if (currentEmail.isNotBlank()) {
                    val docId = currentEmail.replace("@", "_").replace(".", "_")
                    firestore.collection("guardadas")
                        .document(docId)
                        .get().await()
                } else null

                // 3. Get access codes / titles
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

                var totalCount = sentSnap?.size() ?: 0
                val allQuinielaResults = mutableListOf<Pair<Map<String, MatchResult>, Map<String, String>>>()

                // Parse sent quinielas
                sentSnap?.documents?.forEach { doc ->
                    val resultsRaw = doc.get("results") as? Map<String, Map<String, String>>
                    val winnersRaw = doc.get("groupWinners") as? Map<String, String>
                    
                    if (resultsRaw != null) {
                        val results = resultsRaw.mapValues { (_, v) -> 
                            MatchResult(v["homeScore"] ?: "", v["awayScore"] ?: "")
                        }
                        allQuinielaResults.add(results to (winnersRaw ?: emptyMap()))
                    }
                }

                // Parse saved quinielas
                if (savedSnap != null && savedSnap.exists()) {
                    val data = savedSnap.data
                    if (data != null) {
                        totalCount += data.size
                        data.values.forEach { qData ->
                            @Suppress("UNCHECKED_CAST")
                            val qMap = qData as? Map<String, Any>
                            if (qMap != null) {
                                val resultsRaw = qMap["results"] as? Map<String, Map<String, String>>
                                val winnersRaw = qMap["groupWinners"] as? Map<String, String>
                                
                                if (resultsRaw != null) {
                                    val results = resultsRaw.mapValues { (_, v) -> 
                                        MatchResult(v["homeScore"] ?: "", v["awayScore"] ?: "")
                                    }
                                    allQuinielaResults.add(results to (winnersRaw ?: emptyMap()))
                                }
                            }
                        }
                    }
                }

                // Calculate best score
                val matches = MatchRepository.allMatches
                var maxP = 0
                allQuinielaResults.forEach { (res, winners) ->
                    val stats = ScoreCalculator.calculateStats(matches, res, winners)
                    if (stats.totalPoints > maxP) maxP = stats.totalPoints
                }

                launch(Dispatchers.Main) {
                    quinielaCount = totalCount
                    bestScore = maxP
                    rankTitle = newTitle
                    codeError = error
                    isValidatingCode = false
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

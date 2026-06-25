package com.beetik.quinielamalenkamexico2026.data

import android.util.Log
import com.beetik.quinielamalenkamexico2026.model.Match
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

object MatchRepository {
    private val countryFlags = mapOf(
        "México" to "🇲🇽", "Sudáfrica" to "🇿🇦", "Corea del Sur" to "🇰🇷", "República Checa" to "🇨🇿",
        "Canadá" to "🇨🇦", "Bosnia y Herzegovina" to "🇧🇦", "Qatar" to "🇶🇦", "Suiza" to "🇨🇭",
        "Brasil" to "🇧🇷", "Haití" to "🇭🇹", "Marruecos" to "🇲🇦", "Escocia" to "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
        "Estados Unidos" to "🇺🇸", "Paraguay" to "🇵🇾", "Turquía" to "🇹🇷", "Australia" to "🇦🇺",
        "Alemania" to "🇩🇪", "Ecuador" to "🇪🇨", "Costa de Marfil" to "🇨🇮", "Curazao" to "🇨🇼",
        "Japón" to "🇯🇵", "Túnez" to "🇹🇳", "Países Bajos" to "🇳🇱", "Suecia" to "🇸🇪",
        "Bélgica" to "🇧🇪", "Egipto" to "🇪🇬", "Irán" to "🇮🇷", "Nueva Zelanda" to "🇳🇿",
        "España" to "🇪🇸", "Cabo Verde" to "🇨🇻", "Uruguay" to "🇺🇾", "Arabia Saudita" to "🇸🇦",
        "Francia" to "🇫🇷", "Senegal" to "🇸🇳", "Irak" to "🇮🇶", "Noruega" to "🇳🇴",
        "Argentina" to "🇦🇷", "Austria" to "🇦🇹", "Jordania" to "🇯🇴", "Argelia" to "🇩🇿",
        "Portugal" to "🇵🇹", "Congo DR" to "🇨🇩", "Uzbekistán" to "🇺🇿", "Colombia" to "🇨🇴",
        "Inglaterra" to "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "Croacia" to "🇭🇷", "Ghana" to "🇬🇭", "Panamá" to "🇵🇦",
    )

    fun getFlag(country: String): String {
        val normalized = country.trim()
        return countryFlags[normalized] 
            ?: countryFlags.entries.find { it.key.equals(normalized, ignoreCase = true) }?.value 
            ?: "🏳️"
    }

    val allMatches = listOf(
        // Grupo A
        Match("A1", "Grupo A", "2026-06-11", "13:00", "México", getFlag("México"), "Sudáfrica", getFlag("Sudáfrica")),
        Match("A2", "Grupo A", "2026-06-11", "20:00", "Corea del Sur", getFlag("Corea del Sur"), "República Checa", getFlag("República Checa")),
        Match("A3", "Grupo A", "2026-06-18", "10:00", "República Checa", getFlag("República Checa"), "Sudáfrica", getFlag("Sudáfrica")),
        Match("A4", "Grupo A", "2026-06-18", "19:00", "México", getFlag("México"), "Corea del Sur", getFlag("Corea del Sur")),
        Match("A5", "Grupo A", "2026-06-24", "19:00", "República Checa", getFlag("República Checa"), "México", getFlag("México")),
        Match("A6", "Grupo A", "2026-06-24", "19:00", "Sudáfrica", getFlag("Sudáfrica"), "Corea del Sur", getFlag("Corea del Sur")),
        // Grupo B
        Match("B1", "Grupo B", "2026-06-12", "13:00", "Canadá", getFlag("Canadá"), "Bosnia y Herzegovina", getFlag("Bosnia y Herzegovina")),
        Match("B2", "Grupo B", "2026-06-13", "13:00", "Qatar", getFlag("Qatar"), "Suiza", getFlag("Suiza")),
        Match("B3", "Grupo B", "2026-06-18", "13:00", "Suiza", getFlag("Suiza"), "Bosnia y Herzegovina", getFlag("Bosnia y Herzegovina")),
        Match("B4", "Grupo B", "2026-06-18", "16:00", "Canadá", getFlag("Canadá"), "Qatar", getFlag("Qatar")),
        Match("B5", "Grupo B", "2026-06-24", "13:00", "Suiza", getFlag("Suiza"), "Canadá", getFlag("Canadá")),
        Match("B6", "Grupo B", "2026-06-24", "13:00", "Bosnia y Herzegovina", getFlag("Bosnia y Herzegovina"), "Qatar", getFlag("Qatar")),
        // Grupo C
        Match("C1", "Grupo C", "2026-06-13", "16:00", "Brasil", getFlag("Brasil"), "Marruecos", getFlag("Marruecos")),
        Match("C2", "Grupo C", "2026-06-13", "19:00", "Haití", getFlag("Haití"), "Escocia", getFlag("Escocia")),
        Match("C3", "Grupo C", "2026-06-19", "16:00", "Escocia", getFlag("Escocia"), "Marruecos", getFlag("Marruecos")),
        Match("C4", "Grupo C", "2026-06-19", "18:30", "Brasil", getFlag("Brasil"), "Haití", getFlag("Haití")),
        Match("C5", "Grupo C", "2026-06-24", "16:00", "Escocia", getFlag("Escocia"), "Brasil", getFlag("Brasil")),
        Match("C6", "Grupo C", "2026-06-24", "16:00", "Marruecos", getFlag("Marruecos"), "Haití", getFlag("Haití")),
        // Grupo D
        Match("D1", "Grupo D", "2026-06-12", "19:00", "Estados Unidos", getFlag("Estados Unidos"), "Paraguay", getFlag("Paraguay")),
        Match("D2", "Grupo D", "2026-06-13", "22:00", "Australia", getFlag("Australia"), "Turquía", getFlag("Turquía")),
        Match("D3", "Grupo D", "2026-06-25", "20:00", "Turquía", getFlag("Turquía"), "Estados Unidos", getFlag("Estados Unidos")),
        Match("D4", "Grupo D", "2026-06-25", "20:00", "Paraguay", getFlag("Paraguay"), "Australia", getFlag("Australia")),
        Match("D5", "Grupo D", "2026-06-19", "13:00", "Estados Unidos", getFlag("Estados Unidos"), "Australia", getFlag("Australia")),
        Match("D6", "Grupo D", "2026-06-19", "21:00", "Turquía", getFlag("Turquía"), "Paraguay", getFlag("Paraguay")),
        // Grupo E
        Match("E1", "Grupo E", "2026-06-14", "11:00", "Alemania", getFlag("Alemania"), "Curazao", getFlag("Curazao")),
        Match("E2", "Grupo E", "2026-06-14", "17:00", "Costa de Marfil", getFlag("Costa de Marfil"), "Ecuador", getFlag("Ecuador")),
        Match("E3", "Grupo E", "2026-06-20", "14:00", "Alemania", getFlag("Alemania"), "Costa de Marfil", getFlag("Costa de Marfil")),
        Match("E4", "Grupo E", "2026-06-20", "18:00", "Ecuador", getFlag("Ecuador"), "Curazao", getFlag("Curazao")),
        Match("E5", "Grupo E", "2026-06-25", "14:00", "Curazao", getFlag("Curazao"), "Costa de Marfil", getFlag("Costa de Marfil")),
        Match("E6", "Grupo E", "2026-06-25", "14:00", "Ecuador", getFlag("Ecuador"), "Alemania", getFlag("Alemania")),
        // Grupo F
        Match("F1", "Grupo F", "2026-06-14", "20:00", "Suecia", getFlag("Suecia"), "Túnez", getFlag("Túnez")),
        Match("F2", "Grupo F", "2026-06-14", "14:00", "Países Bajos", getFlag("Países Bajos"), "Japón", getFlag("Japón")),
        Match("F3", "Grupo F", "2026-06-20", "11:00", "Países Bajos", getFlag("Países Bajos"), "Suecia", getFlag("Suecia")),
        Match("F4", "Grupo F", "2026-06-20", "22:00", "Túnez", getFlag("Túnez"), "Japón", getFlag("Japón")),
        Match("F5", "Grupo F", "2026-06-25", "17:00", "Japón", getFlag("Japón"), "Suecia", getFlag("Suecia")),
        Match("F6", "Grupo F", "2026-06-25", "17:00", "Túnez", getFlag("Túnez"), "Países Bajos", getFlag("Países Bajos")),
        // Grupo G
        Match("G1", "Grupo G", "2026-06-15", "13:00", "Bélgica", getFlag("Bélgica"), "Egipto", getFlag("Egipto")),
        Match("G2", "Grupo G", "2026-06-15", "19:00", "Irán", getFlag("Irán"), "Nueva Zelanda", getFlag("Nueva Zelanda")),
        Match("G3", "Grupo G", "2026-06-21", "13:00", "Bélgica", getFlag("Bélgica"), "Irán", getFlag("Irán")),
        Match("G4", "Grupo G", "2026-06-21", "17:00", "Nueva Zelanda", getFlag("Nueva Zelanda"), "Egipto", getFlag("Egipto")),
        Match("G5", "Grupo G", "2026-06-26", "21:00", "Egipto", getFlag("Egipto"), "Irán", getFlag("Irán")),
        Match("G6", "Grupo G", "2026-06-26", "21:00", "Nueva Zelanda", getFlag("Nueva Zelanda"), "Bélgica", getFlag("Bélgica")),
        // Grupo H
        Match("H1", "Grupo H", "2026-06-15", "10:00", "España", getFlag("España"), "Cabo Verde", getFlag("Cabo Verde")),
        Match("H2", "Grupo H", "2026-06-15", "16:00", "Arabia Saudita", getFlag("Arabia Saudita"), "Uruguay", getFlag("Uruguay")),
        Match("H3", "Grupo H", "2026-06-21", "16:00", "Uruguay", getFlag("Uruguay"), "Cabo Verde", getFlag("Cabo Verde")),
        Match("H4", "Grupo H", "2026-06-21", "10:00", "España", getFlag("España"), "Arabia Saudita", getFlag("Arabia Saudita")),
        Match("H5", "Grupo H", "2026-06-26", "18:00", "Cabo Verde", getFlag("Cabo Verde"), "Arabia Saudita", getFlag("Arabia Saudita")),
        Match("H6", "Grupo H", "2026-06-26", "18:00", "Uruguay", getFlag("Uruguay"), "España", getFlag("España")),
        // Grupo I
        Match("I1", "Grupo I", "2026-06-16", "13:00", "Francia", getFlag("Francia"), "Senegal", getFlag("Senegal")),
        Match("I2", "Grupo I", "2026-06-16", "16:00", "Irak", getFlag("Irak"), "Noruega", getFlag("Noruega")),
        Match("I3", "Grupo I", "2026-06-22", "18:00", "Noruega", getFlag("Noruega"), "Senegal", getFlag("Senegal")),
        Match("I4", "Grupo I", "2026-06-22", "15:00", "Francia", getFlag("Francia"), "Irak", getFlag("Irak")),
        Match("I5", "Grupo I", "2026-06-26", "13:00", "Noruega", getFlag("Noruega"), "Francia", getFlag("Francia")),
        Match("I6", "Grupo I", "2026-06-26", "13:00", "Senegal", getFlag("Senegal"), "Irak", getFlag("Irak")),
        // Grupo J
        Match("J1", "Grupo J", "2026-06-16", "19:00", "Argentina", getFlag("Argentina"), "Argelia", getFlag("Argelia")),
        Match("J2", "Grupo J", "2026-06-16", "22:00", "Austria", getFlag("Austria"), "Jordania", getFlag("Jordania")),
        Match("J3", "Grupo J", "2026-06-22", "11:00", "Argentina", getFlag("Argentina"), "Austria", getFlag("Austria")),
        Match("J4", "Grupo J", "2026-06-22", "21:00", "Jordania", getFlag("Jordania"), "Argelia", getFlag("Argelia")),
        Match("J5", "Grupo J", "2026-06-27", "20:00", "Jordania", getFlag("Jordania"), "Argentina", getFlag("Argentina")),
        Match("J6", "Grupo J", "2026-06-27", "20:00", "Argelia", getFlag("Argelia"), "Austria", getFlag("Austria")),
        // Grupo K
        Match("K1", "Grupo K", "2026-06-17", "11:00", "Portugal", getFlag("Portugal"), "Congo DR", getFlag("Congo DR")),
        Match("K2", "Grupo K", "2026-06-17", "20:00", "Uzbekistán", getFlag("Uzbekistán"), "Colombia", getFlag("Colombia")),
        Match("K3", "Grupo K", "2026-06-23", "20:00", "Colombia", getFlag("Colombia"), "Congo DR", getFlag("Congo DR")),
        Match("K4", "Grupo K", "2026-06-23", "11:00", "Portugal", getFlag("Portugal"), "Uzbekistán", getFlag("Uzbekistán")),
        Match("K5", "Grupo K", "2026-06-27", "17:30", "Colombia", getFlag("Colombia"), "Portugal", getFlag("Portugal")),
        Match("K6", "Grupo K", "2026-06-27", "17:30", "Congo DR", getFlag("Congo DR"), "Uzbekistán", getFlag("Uzbekistán")),
        // Grupo L
        Match("L1", "Grupo L", "2026-06-17", "14:00", "Inglaterra", getFlag("Inglaterra"), "Croacia", getFlag("Croacia")),
        Match("L2", "Grupo L", "2026-06-17", "17:00", "Ghana", getFlag("Ghana"), "Panamá", getFlag("Panamá")),
        Match("L3", "Grupo L", "2026-06-23", "14:00", "Inglaterra", getFlag("Inglaterra"), "Ghana", getFlag("Ghana")),
        Match("L4", "Grupo L", "2026-06-23", "17:00", "Panamá", getFlag("Panamá"), "Croacia", getFlag("Croacia")),
        Match("L5", "Grupo L", "2026-06-27", "15:00", "Panamá", getFlag("Panamá"), "Inglaterra", getFlag("Inglaterra")),
        Match("L6", "Grupo L", "2026-06-27", "15:00", "Croacia", getFlag("Croacia"), "Ghana", getFlag("Ghana")),

        // Eliminatorias - 16avos de Final (Round of 32)
        Match("R32_1", "16avos de Final", "2026-06-28", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_2", "16avos de Final", "2026-06-28", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_3", "16avos de Final", "2026-06-29", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_4", "16avos de Final", "2026-06-29", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_5", "16avos de Final", "2026-06-30", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_6", "16avos de Final", "2026-06-30", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_7", "16avos de Final", "2026-07-01", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_8", "16avos de Final", "2026-07-01", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_9", "16avos de Final", "2026-07-02", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_10", "16avos de Final", "2026-07-02", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_11", "16avos de Final", "2026-07-03", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_12", "16avos de Final", "2026-07-03", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_13", "16avos de Final", "2026-07-03", "20:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_14", "16avos de Final", "2026-07-04", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_15", "16avos de Final", "2026-07-04", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R32_16", "16avos de Final", "2026-07-04", "20:00", "Por definir", "🏳️", "Por definir", "🏳️"),

        // Octavos de Final (Round of 16)
        Match("R16_1", "Octavos de Final", "2026-07-05", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_2", "Octavos de Final", "2026-07-05", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_3", "Octavos de Final", "2026-07-06", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_4", "Octavos de Final", "2026-07-06", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_5", "Octavos de Final", "2026-07-07", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_6", "Octavos de Final", "2026-07-07", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_7", "Octavos de Final", "2026-07-08", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("R16_8", "Octavos de Final", "2026-07-08", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),

        // Cuartos de Final
        Match("QF_1", "Cuartos de Final", "2026-07-09", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("QF_2", "Cuartos de Final", "2026-07-09", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("QF_3", "Cuartos de Final", "2026-07-10", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("QF_4", "Cuartos de Final", "2026-07-10", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),

        // Semifinales
        Match("SF_1", "Semifinales", "2026-07-14", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),
        Match("SF_2", "Semifinales", "2026-07-15", "17:00", "Por definir", "🏳️", "Por definir", "🏳️"),

        // Tercer Lugar
        Match("3RD", "Tercer Lugar", "2026-07-18", "13:00", "Por definir", "🏳️", "Por definir", "🏳️"),

        // Final
        Match("FIN", "Final", "2026-07-19", "17:00", "Por definir", "🏳️", "Por definir", "🏳️")
    )

    fun getMatchesFlow(): Flow<List<Match>> = callbackFlow {
        val db = FirebaseFirestore.getInstance()
        Log.d("MatchRepository", "Starting getMatchesFlow collection listener on 'matches'")
        
        val listener = db.collection("matches")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MatchRepository", "Firebase Listen failed!", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("MatchRepository", "Firebase snapshot received with ${snapshot.size()} documents")
                    
                    // Mapa de resultados finales, inicializado con los estáticos
                    val finalMatches = allMatches.associateBy { it.id }.toMutableMap()
                    
                    snapshot.documents.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        
                        // Extraer el mapa 'elements' de forma segura
                        @Suppress("UNCHECKED_CAST")
                        fun asMap(value: Any?): Map<String, Any>? = value as? Map<String, Any>
                        val elements = asMap(data["elements"])
                        
                        // 1. Determinar el matchCode (M73, R32_1, etc.)
                        val mCodeRaw = (elements?.get("matchCode") ?: data["matchCode"] ?: doc.id).toString()
                        var mCode = mCodeRaw
                        
                        // Mapeo M73 -> R32_1 (Índice 72)
                        if (mCode.startsWith("M") && mCode.length <= 4) {
                            val num = mCode.substring(1).toIntOrNull()
                            if (num != null && num >= 1 && num <= allMatches.size) {
                                mCode = allMatches[num - 1].id
                            }
                        }
                        
                        val static = finalMatches[mCode] ?: return@forEach

                        // 2. Extraer equipos con fallback directo (Prioridad: elements > raíz)
                        fun Map<String, Any>.findValue(key: String): Any? =
                            entries.find { it.key.equals(key, ignoreCase = true) }?.value

                        fun resolveString(value: Any?): String? {
                            val resolved = when (value) {
                                null -> null
                                is Map<*, *> -> {
                                    listOf("name", "team", "teamName", "nombre", "displayName", "shortName", "value")
                                        .firstNotNullOfOrNull { nestedKey ->
                                            value.entries.find { it.key?.toString()?.equals(nestedKey, ignoreCase = true) == true }
                                                ?.value
                                                ?.let(::resolveString)
                                        }
                                }
                                else -> value.toString().trim()
                            }
                            return resolved?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        }

                        fun getTeam(vararg keys: String): String? {
                            val sources = listOfNotNull(elements, data)
                            for (key in keys) {
                                sources.firstNotNullOfOrNull { source ->
                                    resolveString(source.findValue(key))
                                }?.let { return it }
                            }
                            return null
                        }

                        fun getNestedTeam(vararg paths: List<String>): String? {
                            val sources = listOfNotNull(elements, data)
                            for (path in paths) {
                                for (source in sources) {
                                    var current: Any? = source
                                    path.forEach { key ->
                                        current = asMap(current)?.findValue(key)
                                    }
                                    resolveString(current)?.let { return it }
                                }
                            }
                            return null
                        }

                        fun resolvedTeam(current: String, candidate: String?): String =
                            candidate?.takeIf { !it.equals("Por definir", ignoreCase = true) } ?: current

                        val hTeam = resolvedTeam(
                            static.homeTeam,
                            getTeam(
                                "homeTeam", "homeTeamName", "home", "localTeam", "localTeamName",
                                "equipoLocal", "local", "teamHome", "team1", "homeName", "localName"
                            ) ?: getNestedTeam(
                                listOf("home", "team"), listOf("home", "name"),
                                listOf("local", "team"), listOf("local", "name"),
                                listOf("teams", "home"), listOf("teams", "local"),
                                listOf("participants", "home"), listOf("participants", "local")
                            )
                        )
                        val aTeam = resolvedTeam(
                            static.awayTeam,
                            getTeam(
                                "awayTeam", "awayTeamName", "away", "visitorTeam", "visitorTeamName",
                                "visitanteTeam", "visitanteTeamName", "equipoVisitante", "visitante",
                                "teamAway", "team2", "awayName", "visitorName", "visitanteName"
                            ) ?: getNestedTeam(
                                listOf("away", "team"), listOf("away", "name"),
                                listOf("visitor", "team"), listOf("visitor", "name"),
                                listOf("visitante", "team"), listOf("visitante", "name"),
                                listOf("teams", "away"), listOf("teams", "visitor"), listOf("teams", "visitante"),
                                listOf("participants", "away"), listOf("participants", "visitor"), listOf("participants", "visitante")
                            )
                        )
                        
                        if (doc.id == "M73" || mCode == "R32_1") {
                            Log.d("MatchRepository", "DEBUG M73: docId=${doc.id}, h=$hTeam, a=$aTeam, hasElements=${elements != null}")
                        }

                        // 3. Actualizar objeto
                        finalMatches[mCode] = static.copy(
                            homeTeam = hTeam,
                            homeFlag = getFlag(hTeam),
                            awayTeam = aTeam,
                            awayFlag = getFlag(aTeam),
                            group = getTeam("group", "grupo") ?: static.group,
                            date = getTeam("date", "fecha", "api_local_date") ?: static.date,
                            time = getTeam("time", "hora") ?: static.time,
                            realHomeScore = getTeam("homeScore", "golesLocal")?.toDoubleOrNull()?.toInt(),
                            realAwayScore = getTeam("awayScore", "golesVisitante")?.toDoubleOrNull()?.toInt(),
                            started = (elements?.get("started") ?: data["started"]) as? Boolean ?: static.started,
                            finished = (elements?.get("finished") ?: data["finished"]) as? Boolean ?: static.finished,
                            isActive = (elements?.get("isActive") ?: data["isActive"]) as? Boolean ?: static.isActive,
                            firebaseId = doc.id
                        )
                    }
                    
                    val sortedList = finalMatches.values.sortedWith(compareBy({ it.date }, { it.time }, { it.id }))
                    Log.d("MatchRepository", "Emitting ${sortedList.size} matches to UI")
                    this.trySend(sortedList)
                }
            }
        awaitClose { 
            Log.d("MatchRepository", "Closing getMatchesFlow listener")
            listener.remove() 
        }
    }.flowOn(Dispatchers.IO)
}

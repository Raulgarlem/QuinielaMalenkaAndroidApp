package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.data.local.entity.QuinielaEntity
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun CategorySettingsDialog(
    categories: SnapshotStateMap<Int, PinCategory>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Categorías", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.values.sortedBy { it.id }.forEach { cat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = cat.color, shape = CircleShape, modifier = Modifier.size(16.dp)) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        var nameText by remember { mutableStateOf(cat.name) }
                        OutlinedTextField(
                            value = nameText,
                            onValueChange = { 
                                nameText = it
                                categories[cat.id] = cat.copy(name = it)
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cat.color,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("Listo", color = Color.Black)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun LoadQuinielaDialog(
    savedQuinielas: List<QuinielaEntity>,
    onDismiss: () -> Unit,
    onQuinielaSelected: (QuinielaEntity) -> Unit
) {
    var dialogSearchQuery by remember { mutableStateOf("") }
    val filteredSaved = remember(savedQuinielas, dialogSearchQuery) {
        if (dialogSearchQuery.isEmpty()) savedQuinielas
        else savedQuinielas.filter { 
            it.quinielaName.contains(dialogSearchQuery, ignoreCase = true) || 
            it.propietarioName.contains(dialogSearchQuery, ignoreCase = true) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Cargar Quiniela Guardada", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dialogSearchQuery,
                    onValueChange = { dialogSearchQuery = it },
                    placeholder = { Text("Buscar...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        text = {
            if (filteredSaved.isEmpty()) {
                Text(
                    if (savedQuinielas.isEmpty()) "No hay quinielas guardadas." 
                    else "No se encontraron resultados.", 
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(filteredSaved) { q ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuinielaSelected(q); onDismiss() }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(q.quinielaName, color = Gold, fontWeight = FontWeight.Bold)
                            Text(q.propietarioName, color = Color.White, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = Gold) }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

fun QuinielaEntity.toParticipant(currentUserEmail: String = ""): Participant {
    val gson = Gson()
    val predictionsType = object : TypeToken<Map<String, MatchResult>>() {}.type
    val predictionsMap: Map<String, MatchResult> = gson.fromJson(this.resultsJson, predictionsType) ?: emptyMap()
    
    val predictions = predictionsMap.mapValues { (_, res) ->
        (res.homeScore.toIntOrNull() ?: 0) to (res.awayScore.toIntOrNull() ?: 0)
    }

    val winnersType = object : TypeToken<Map<String, String>>() {}.type
    val winners: Map<String, String> = gson.fromJson(this.winnersJson, winnersType) ?: emptyMap()

    return Participant(
        id = "loaded_${this.id}",
        quinielaName = this.quinielaName,
        ownerName = this.propietarioName,
        isUser = this.userEmail.isNotBlank() && this.userEmail.lowercase().trim() == currentUserEmail.lowercase().trim(),
        predictions = predictions,
        groupWinnerPredictions = winners,
        prevPosition = 0
    )
}

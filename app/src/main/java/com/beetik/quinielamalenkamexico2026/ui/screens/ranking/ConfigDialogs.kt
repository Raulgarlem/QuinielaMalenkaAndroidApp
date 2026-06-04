package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.model.RankingConfig
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

// Use explicit color values for better compatibility if predefined ones fail
val MaintenanceOrange = Color(0xFFFFA500)
val MaintenanceCyan = Color(0xFF00FFFF)
val MaintenanceMagenta = Color(0xFFFF00FF)
val MaintenanceRed = Color(0xFFFF0000)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsManagerDialog(
    currentConfigName: String,
    allConfigs: List<RankingConfig>,
    categories: SnapshotStateMap<Int, PinCategory>,
    onDismiss: () -> Unit,
    onSaveCurrentAsNew: (String) -> Unit,
    onSwitchConfig: (RankingConfig) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onResetAll: () -> Unit,
    onClearResults: () -> Unit,
    onClearParticipants: () -> Unit,
    onClearCategories: () -> Unit
) {
    var showNewConfigInput by remember { mutableStateOf(false) }
    var newConfigName by remember { mutableStateOf("") }
    val newConfigFocusRequester = remember { FocusRequester() }
    
    var activeTab by remember { mutableStateOf("Configuraciones") }
    var configToDelete by remember { mutableStateOf<RankingConfig?>(null) }

    LaunchedEffect(showNewConfigInput) {
        if (showNewConfigInput) {
            newConfigFocusRequester.requestFocus()
        }
    }

    if (configToDelete != null) {
        AlertDialog(
            onDismissRequest = { configToDelete = null },
            title = { Text("Eliminar Configuración") },
            text = { Text("¿Deseas eliminar la configuración \"${configToDelete?.configName}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        configToDelete?.let { onDeleteConfig(it.id) }
                        configToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaintenanceRed)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { configToDelete = null }) { Text("Cancelar") } },
            containerColor = Color(0xFF2A2A2A)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración del Ranking", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                // Tabs
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF333333), RoundedCornerShape(8.dp)).padding(4.dp)) {
                    TabItem("Configuraciones", activeTab == "Configuraciones") { activeTab = "Configuraciones" }
                    TabItem("Categorías", activeTab == "Categorías") { activeTab = "Categorías" }
                    TabItem("Mantenimiento", activeTab == "Mantenimiento") { activeTab = "Mantenimiento" }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                when (activeTab) {
                    "Configuraciones" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allConfigs) { config ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onSwitchConfig(config); onDismiss() },
                                            onLongClick = {
                                                if (config.id != "default") {
                                                    configToDelete = config
                                                }
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (config.configName == currentConfigName) Gold.copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                                    ),
                                    border = if (config.configName == currentConfigName) BorderStroke(1.dp, Gold) else null
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(config.configName, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (config.configName == currentConfigName) {
                                            Icon(Icons.Default.Check, null, tint = Gold, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            
                            item {
                                if (showNewConfigInput) {
                                    val onSave = {
                                        if (newConfigName.isNotBlank()) {
                                            onSaveCurrentAsNew(newConfigName)
                                            showNewConfigInput = false
                                            newConfigName = ""
                                        }
                                    }
                                    OutlinedTextField(
                                        value = newConfigName,
                                        onValueChange = { newConfigName = it },
                                        label = { Text("Nombre de nueva configuración") },
                                        modifier = Modifier.fillMaxWidth().focusRequester(newConfigFocusRequester),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { onSave() }),
                                        trailingIcon = {
                                            IconButton(onClick = { onSave() }) { Icon(Icons.Default.Save, null, tint = Gold) }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                } else {
                                    Button(
                                        onClick = { showNewConfigInput = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guardar actual como nueva")
                                    }
                                }
                            }
                        }
                    }
                    "Categorías" -> {
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
                    }
                    "Mantenimiento" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MaintenanceButton("Borrar Marcadores", Icons.Default.Clear, MaintenanceOrange) { onClearResults() }
                            MaintenanceButton("Borrar Añadidas", Icons.Default.GroupRemove, MaintenanceCyan) { onClearParticipants() }
                            MaintenanceButton("Eliminar Todas las Categorías", Icons.Default.LabelOff, MaintenanceMagenta) { onClearCategories() }
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            MaintenanceButton("RESET TOTAL CONFIGURACIÓN", Icons.Default.Refresh, MaintenanceRed) { onResetAll() }
                            Text("El Reset Total limpia marcadores, las quinielas añadidas, categorías y comparaciones.",
                                fontSize = 10.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                Text("Cerrar", color = Color.Black)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
private fun RowScope.TabItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
            .background(if (selected) Gold else Color.Transparent)
            .clickable { onClick() }.padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun MaintenanceButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp)
    }
}

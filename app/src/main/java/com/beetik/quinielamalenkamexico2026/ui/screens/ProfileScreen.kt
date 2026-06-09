package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.data.local.database.QuinielaDatabase
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(userViewModel: UserViewModel = viewModel()) {
    if (userViewModel.codeError != null) {
        AlertDialog(
            onDismissRequest = { userViewModel.clearCodeError() },
            title = { Text("Error de Código") },
            text = { Text(userViewModel.codeError ?: "") },
            confirmButton = {
                Button(
                    onClick = { userViewModel.clearCodeError() },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (userViewModel.isLoggedIn) {
        LoggedProfileScreen(userViewModel)
    } else {
        NotLoggedInScreen(userViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotLoggedInScreen(userViewModel: UserViewModel) {
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        LoginDialog(
            userViewModel = userViewModel,
            onDismiss = { showLoginDialog = false },
            onLogin = { name, email, code ->
                userViewModel.login(name, email, code)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MI PERFIL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Gold)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = { showLoginDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Profile Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                }
            }

            item {
                Text(
                    "Aún no has iniciado sesión", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Inicia sesión o crea una cuenta para personalizar tu perfil y participar en la quiniela.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item { LockedItem("Tu información", "Edita tu nombre, foto y más", Icons.Default.Person) { showLoginDialog = true } }
            item { LockedItem("Tu rendimiento", "Consulta tus estadísticas y precisión", Icons.Default.BarChart) { showLoginDialog = true } }
            item { LockedItem("Tus logros", "Revisa tus trofeos y reconocimientos", Icons.Default.EmojiEvents) { showLoginDialog = true } }
            item { LockedItem("Tus amigos", "Gestiona tus amigos y listas personalizadas", Icons.Default.Group) { showLoginDialog = true } }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun LockedItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.Lock, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LoginDialog(userViewModel: UserViewModel, onDismiss: () -> Unit, onLogin: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var codeValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var wasValidating by remember { mutableStateOf(false) }

    // Auto-close on success
    LaunchedEffect(userViewModel.isValidatingCode, userViewModel.codeError) {
        if (userViewModel.isValidatingCode) {
            wasValidating = true
        } else if (wasValidating) {
            if (userViewModel.codeError == null) {
                onDismiss()
            }
            wasValidating = false
        }
    }

    // Keep dialog open and select text if error occurs
    LaunchedEffect(userViewModel.codeError) {
        if (userViewModel.codeError != null) {
            codeValue = codeValue.copy(
                selection = TextRange(0, codeValue.text.length)
            )
            focusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciar Sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = codeValue,
                    onValueChange = { codeValue = it },
                    label = { Text("Código de Acceso a Quiniela") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                onLogin(name, email, codeValue.text)
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onLogin(name, email, codeValue.text)
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
            ) {
                Text("Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                userViewModel.clearCodeError()
                onDismiss()
            }) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedProfileScreen(userViewModel: UserViewModel) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val database = remember { QuinielaDatabase.getDatabase(context) }

    LaunchedEffect(Unit) {
        userViewModel.fetchFirebaseStats()
    }

    if (showSettingsDialog) {
        SettingsDialog(
            userViewModel = userViewModel,
            currentCode = userViewModel.accessCode,
            onDismiss = { showSettingsDialog = false },
            onUpdate = { newCode ->
                userViewModel.updateAccessCode(newCode)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MI PERFIL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Gold)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ProfileHeader(userViewModel.name, userViewModel.email, userViewModel.rankTitle)
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatBox("Quinielas", userViewModel.quinielaCount.toString(), "creadas")
                    StatBox("Mejor Puntaje", userViewModel.bestScore.toString(), "puntos")
                    StatBox("Posición", "#---", "en ranking")
                    StatBox("Aciertos", "---", "globales")
                }
            }
            
            item {
                Column {
                    Text("Mis logros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AchievementItem("Primer envío", "Completado")
                        AchievementItem("10 aciertos", "Completado")
                        AchievementItem("Participante", "Completado")
                    }
                }
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuItem(
                        text = if (userViewModel.isSyncing) "Sincronizando..." else "Cargar mis quinielas (Cloud)",
                        icon = if (userViewModel.isSyncing) Icons.Default.Sync else Icons.Default.CloudDownload
                    ) {
                        if (!userViewModel.isSyncing) {
                            userViewModel.syncQuinielasFromCloud(database) {
                                Toast.makeText(context, "Sincronización completada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    MenuItem("Configuración", Icons.Default.ChevronRight) {
                        showSettingsDialog = true
                    }
                    MenuItem("Cerrar sesión", Icons.Default.Logout, color = Color.Red) {
                        userViewModel.logout()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(userViewModel: UserViewModel, currentCode: String, onDismiss: () -> Unit, onUpdate: (String) -> Unit) {
    var codeValue by remember { mutableStateOf(TextFieldValue(currentCode)) }
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var wasValidating by remember { mutableStateOf(false) }

    // Auto-close on success
    LaunchedEffect(userViewModel.isValidatingCode, userViewModel.codeError) {
        if (userViewModel.isValidatingCode) {
            wasValidating = true
        } else if (wasValidating) {
            if (userViewModel.codeError == null) {
                isEditing = false
                onDismiss()
            }
            wasValidating = false
        }
    }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            // Select all text and request focus
            codeValue = codeValue.copy(
                selection = TextRange(0, codeValue.text.length)
            )
            focusRequester.requestFocus()
        }
    }

    // Keep editing and select all if an error occurs
    LaunchedEffect(userViewModel.codeError) {
        if (userViewModel.codeError != null) {
            isEditing = true
            codeValue = codeValue.copy(
                selection = TextRange(0, codeValue.text.length)
            )
            focusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Código de Acceso a Quiniela:", style = MaterialTheme.typography.labelMedium)
                if (isEditing) {
                    OutlinedTextField(
                        value = codeValue,
                        onValueChange = { codeValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                onUpdate(codeValue.text)
                                if (codeValue.text.isBlank()) {
                                    isEditing = false
                                }
                            }
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(codeValue.text.ifBlank { "(Sin código)" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Gold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = { 
                        onUpdate(codeValue.text)
                        // Don't close editing yet, wait to see if there's an error
                        if (codeValue.text.isBlank()) {
                            isEditing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text("Guardar")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { 
                    isEditing = false
                    codeValue = TextFieldValue(currentCode) 
                    userViewModel.clearCodeError()
                }) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun ProfileHeader(name: String, email: String, rankTitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Gold),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.toString()?.uppercase() ?: "", 
                    style = MaterialTheme.typography.headlineLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text("$name 👑", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(rankTitle, style = MaterialTheme.typography.bodyMedium, color = Gold)
                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, subLabel: String) {
    Card(
        modifier = Modifier.width(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Gold)
            Text(subLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AchievementItem(title: String, status: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("🏆", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(status, style = MaterialTheme.typography.labelSmall, color = Gold)
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, color = color)
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.5f))
        }
    }
}

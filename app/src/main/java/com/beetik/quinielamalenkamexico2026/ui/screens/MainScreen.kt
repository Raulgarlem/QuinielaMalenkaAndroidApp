package com.beetik.quinielamalenkamexico2026.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beetik.quinielamalenkamexico2026.ui.UserViewModel
import com.beetik.quinielamalenkamexico2026.ui.navigation.Screen
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

class QuinielaEditState {
    var hasUnsavedChanges by mutableStateOf(false)
    var onSaveAndExit: (() -> Unit) -> Unit = {}
    var onDiscardAndExit: (() -> Unit) -> Unit = {}
}

val LocalQuinielaEditState = staticCompositionLocalOf { QuinielaEditState() }

@Composable
fun MainScreen() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val userViewModel: UserViewModel = viewModel()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val editState = remember { QuinielaEditState() }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val items = listOf(
        Screen.Inicio,
        Screen.Quinielas,
        Screen.Ranking,
        Screen.Partidos,
        Screen.Perfil
    )

    CompositionLocalProvider(LocalQuinielaEditState provides editState) {
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("Cambios sin guardar") },
                text = { Text("Tienes cambios sin guardar en tu quiniela. ¿Deseas guardarlos antes de salir?") },
                confirmButton = {
                    Button(onClick = {
                        editState.onSaveAndExit {
                            showUnsavedDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            editState.onDiscardAndExit {
                                showUnsavedDialog = false
                                pendingAction?.invoke()
                                pendingAction = null
                            }
                        }) {
                            Text("Descartar", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { 
                            showUnsavedDialog = false 
                            pendingAction = null
                        }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = if (isLandscape) Modifier.height(48.dp) else Modifier.fillMaxWidth()
                ) {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = if (isLandscape) Modifier.size(20.dp) else Modifier.size(24.dp)
                                )
                            },
                            label = if (isLandscape) null else { 
                                {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        minLines = 2,
                                        lineHeight = 12.sp,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(72.dp)
                                    ) 
                                }
                            },
                            selected = selected,
                            alwaysShowLabel = !isLandscape,
                            onClick = {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                val navigateAction: () -> Unit = {
                                    if (currentRoute?.startsWith("fill_quiniela") == true && screen.route == Screen.Quinielas.route) {
                                        // If we are in the editing screen and click the "Quinielas" tab, 
                                        // we simply pop back to the list.
                                        navController.popBackStack(Screen.Quinielas.route, false)
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }

                                if (editState.hasUnsavedChanges) {
                                    pendingAction = navigateAction
                                    showUnsavedDialog = true
                                } else {
                                    navigateAction()
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gold,
                                selectedTextColor = Gold,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Inicio.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Inicio.route) { InicioScreen(userViewModel = userViewModel) }
                composable(Screen.Quinielas.route) { QuinielasScreen(navController, userViewModel = userViewModel) }
                composable(Screen.Partidos.route) { PartidosScreen() }
                composable(Screen.Ranking.route) { RankingScreen(userViewModel = userViewModel) }
                composable(Screen.Perfil.route) { PerfilScreen(userViewModel = userViewModel) }
                composable(Screen.FillQuiniela.route) { backStackEntry ->
                    val quinielaId = backStackEntry.arguments?.getString("quinielaId")?.toIntOrNull() ?: -1
                    QuinielaScreen(
                        quinielaId = quinielaId,
                        userViewModel = userViewModel,
                        onBack = {
                            val backAction: () -> Unit = { navController.popBackStack() }
                            if (editState.hasUnsavedChanges) {
                                pendingAction = backAction
                                showUnsavedDialog = true
                            } else {
                                backAction()
                            }
                        }
                    )
                }
            }
        }
    }
}

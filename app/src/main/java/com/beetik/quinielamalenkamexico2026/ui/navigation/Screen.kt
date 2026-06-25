package com.beetik.quinielamalenkamexico2026.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Inicio : Screen("inicio", "Inicio", Icons.Filled.Home, Icons.Outlined.Home)
    object Quinielas : Screen("quinielas", "Quinielas", Icons.Filled.ListAlt, Icons.Outlined.ListAlt)
    object Partidos : Screen("partidos", "Partidos", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer)
    object Ranking : Screen("ranking", "Quiniela General", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Perfil : Screen("perfil", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
    
    // Screens without bottom bar icons
    object FillQuiniela : Screen("fill_quiniela/{quinielaId}", "Llenar Quiniela", Icons.Filled.ListAlt, Icons.Outlined.ListAlt) {
        fun createRoute(quinielaId: Int) = "fill_quiniela/$quinielaId"
    }

    object FillEliminatorias : Screen("fill_eliminatorias/{quinielaId}", "Llenar Eliminatorias", Icons.Filled.ListAlt, Icons.Outlined.ListAlt) {
        fun createRoute(quinielaId: Int) = "fill_eliminatorias/$quinielaId"
    }

    object RoundSelection : Screen("round_selection/{quinielaId}", "Selección de Ronda", Icons.Filled.ListAlt, Icons.Outlined.ListAlt) {
        fun createRoute(quinielaId: Int) = "round_selection/$quinielaId"
    }
}

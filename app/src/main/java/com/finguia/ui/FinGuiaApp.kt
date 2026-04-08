package com.finguia.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.finguia.R
import com.finguia.ui.home.DashboardScreen

// import com.example.finguia.ui.home.HomeScreen

@Composable
fun FinGuiaApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painterResource(destination.icon),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.HOME -> Text("Bem-vindo à Home!", modifier)
                AppDestinations.DASHBOARD -> DashboardScreen(modifier = modifier)
                AppDestinations.THEME -> Text("Configurações de Tema", modifier)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    DASHBOARD("Dashboard", R.drawable.ic_dashboard),
    THEME("Theme", R.drawable.ic_palette),
}
package com.example.sampleproject.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sampleproject.ui.screen.ConnectionScreen
import com.example.sampleproject.ui.screen.MainScreen
import com.example.sampleproject.viewmodel.ConnectionViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    connectionViewModel: ConnectionViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connection.route
    ) {
        composable(Screen.Connection.route) {
            ConnectionScreen(
                viewModel = connectionViewModel,
                onConnectionSuccess = {
                    navController.navigate(Screen.Main.route) {
                        // Remove connection screen from back stack
                        popUpTo(Screen.Connection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}


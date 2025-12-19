package com.example.sampleproject.navigation

sealed class Screen(val route: String) {
    object Connection : Screen("connection")
    object Main : Screen("main")
}


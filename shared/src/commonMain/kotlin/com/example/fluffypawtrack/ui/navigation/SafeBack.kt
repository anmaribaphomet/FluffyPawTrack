package com.example.fluffypawtrack.ui.navigation

import androidx.navigation.NavHostController

/**
 * Back that never empties the NavHost (REQUIREMENTS §4 "Navegación"): pops when there is a
 * previous entry, otherwise leaves the app like the system back on the first screen.
 */
internal inline fun navigateBack(hasPreviousEntry: Boolean, pop: () -> Unit, exit: () -> Unit) {
    if (hasPreviousEntry) pop() else exit()
}

internal fun NavHostController.popOrExit(onExitApp: () -> Unit) =
    navigateBack(previousBackStackEntry != null, pop = { popBackStack() }, exit = onExitApp)

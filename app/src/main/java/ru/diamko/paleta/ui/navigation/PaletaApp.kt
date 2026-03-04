package ru.diamko.paleta.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.diamko.paleta.R
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.ui.auth.AuthViewModel
import ru.diamko.paleta.ui.auth.LoginScreen
import ru.diamko.paleta.ui.auth.RegisterScreen
import ru.diamko.paleta.ui.palettes.PaletteEditorScreen
import ru.diamko.paleta.ui.palettes.PaletteGenerateScreen
import ru.diamko.paleta.ui.palettes.PaletteListScreen
import ru.diamko.paleta.ui.palettes.PaletteViewModel
import ru.diamko.paleta.ui.settings.SettingsScreen

@Composable
fun PaletaApp(
    container: AppContainer,
) {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val paletteViewModel: PaletteViewModel = viewModel(factory = PaletteViewModel.factory(container))

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val paletteState by paletteViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.isCheckingSession, authState.user?.id) {
        if (authState.isCheckingSession) {
            return@LaunchedEffect
        }

        val target = if (authState.user == null) Routes.LOGIN else Routes.PALETTES
        navController.navigate(target) {
            launchSingleTop = true
            restoreState = false
        }
    }

    if (authState.isCheckingSession) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(id = R.string.loading))
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.user == null) Routes.LOGIN else Routes.PALETTES,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                state = authState,
                onLoginClick = authViewModel::login,
                onGoRegisterClick = { navController.navigate(Routes.REGISTER) },
                onClearError = authViewModel::clearError,
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                state = authState,
                onRegisterClick = authViewModel::register,
                onGoLoginClick = { navController.navigate(Routes.LOGIN) },
                onClearError = authViewModel::clearError,
            )
        }

        composable(Routes.PALETTES) {
            PaletteListScreen(
                state = paletteState,
                onReload = paletteViewModel::loadPalettes,
                onCreateClick = { navController.navigate(Routes.paletteEditor("new")) },
                onOpenGenerator = { navController.navigate(Routes.GENERATE) },
                onEditClick = { id -> navController.navigate(Routes.paletteEditor(id.toString())) },
                onDeleteClick = { id -> paletteViewModel.deletePalette(id) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.GENERATE) {
            PaletteGenerateScreen(
                paletteViewModel = paletteViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                authState = authState,
                onLogout = authViewModel::logout,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PALETTE_EDITOR,
            arguments = listOf(navArgument("paletteId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val paletteIdRaw = backStackEntry.arguments?.getString("paletteId") ?: "new"
            val paletteId = paletteIdRaw.toLongOrNull()
            PaletteEditorScreen(
                paletteId = paletteId,
                paletteViewModel = paletteViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

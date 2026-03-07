package ru.diamko.paleta.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.diamko.paleta.R
import ru.diamko.paleta.core.di.AppContainer
import ru.diamko.paleta.ui.auth.AuthViewModel
import ru.diamko.paleta.ui.auth.ForgotPasswordScreen
import ru.diamko.paleta.ui.auth.LoginScreen
import ru.diamko.paleta.ui.auth.RegisterScreen
import ru.diamko.paleta.ui.auth.ResetPasswordScreen
import ru.diamko.paleta.ui.components.PaletaGradientBackground
import ru.diamko.paleta.ui.palettes.PaletteEditorScreen
import ru.diamko.paleta.ui.palettes.PaletteGenerateScreen
import ru.diamko.paleta.ui.palettes.PaletteGenerateScreenMode
import ru.diamko.paleta.ui.palettes.PaletteListScreen
import ru.diamko.paleta.ui.palettes.PaletteViewModel
import ru.diamko.paleta.ui.settings.FaqScreen
import ru.diamko.paleta.ui.settings.PasswordChangeScreen
import ru.diamko.paleta.ui.settings.ProfileEditScreen
import ru.diamko.paleta.ui.settings.SettingsScreen

@Composable
fun PaletaApp(
    container: AppContainer,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val paletteViewModel: PaletteViewModel = viewModel(factory = PaletteViewModel.factory(container))

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val paletteState by paletteViewModel.uiState.collectAsStateWithLifecycle()
    var resetEmailPrefill by rememberSaveable { mutableStateOf("") }
    var currentLanguageTag by rememberSaveable { mutableStateOf("ru") }

    LaunchedEffect(Unit) {
        val stored = container.localeStore.readLanguageTag()
        val tag = stored
            ?.takeIf { it == "ru" || it == "en" }
            ?: "ru"
        currentLanguageTag = tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    LaunchedEffect(authState.isCheckingSession, authState.user?.id) {
        if (authState.isCheckingSession) {
            return@LaunchedEffect
        }

        val current = navController.currentBackStackEntry?.destination?.route
        val authFlow = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD, Routes.RESET_PASSWORD)
        val guestAllowed = setOf(Routes.GENERATE_RANDOM, Routes.GENERATE_IMAGE)
        val target = if (authState.user == null) {
            if (current in authFlow || current in guestAllowed) null else Routes.LOGIN
        } else {
            if (current in authFlow || current == null) Routes.PALETTES else null
        }
        if (target != null) {
            navController.navigate(target) {
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    if (authState.isCheckingSession) {
        PaletaGradientBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(text = stringResource(id = R.string.loading))
            }
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
                onGoForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onContinueAsGuestClick = { navController.navigate(Routes.GENERATE_RANDOM) },
                onClearError = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                state = authState,
                onRegisterClick = authViewModel::register,
                onGoLoginClick = { navController.navigate(Routes.LOGIN) },
                onClearError = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                state = authState,
                onRequestCode = { email ->
                    authViewModel.requestPasswordResetCode(email)
                },
                onGoReset = { email ->
                    resetEmailPrefill = email
                    navController.navigate(Routes.RESET_PASSWORD)
                },
                onBack = { navController.popBackStack() },
                onClearMessages = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                state = authState,
                prefillEmail = resetEmailPrefill,
                onResetPassword = { email, code, newPassword, confirmPassword ->
                    authViewModel.resetPassword(
                        email = email,
                        code = code,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                    ) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onClearMessages = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.PALETTES) {
            PaletteListScreen(
                state = paletteState,
                onReload = paletteViewModel::loadPalettes,
                onCreateClick = { navController.navigate(Routes.paletteEditor("new")) },
                onOpenRandomGenerator = { navController.navigate(Routes.GENERATE_RANDOM) },
                onOpenImageGenerator = { navController.navigate(Routes.GENERATE_IMAGE) },
                onEditClick = { id -> navController.navigate(Routes.paletteEditor(id.toString())) },
                onDeleteClick = { id -> paletteViewModel.deletePalette(id) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onExportPalette = { palette, format, onDone, onError ->
                    paletteViewModel.exportPalette(
                        name = palette.name,
                        colors = palette.colors,
                        format = format,
                        onDone = onDone,
                        onError = onError,
                    )
                },
            )
        }

        composable(Routes.GENERATE_RANDOM) {
            PaletteGenerateScreen(
                paletteViewModel = paletteViewModel,
                mode = PaletteGenerateScreenMode.RANDOM,
                onBack = { navController.popBackStack() },
                isAuthenticated = authState.user != null,
                onRequireLogin = {
                    navController.navigate(Routes.LOGIN) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.GENERATE_IMAGE) {
            PaletteGenerateScreen(
                paletteViewModel = paletteViewModel,
                mode = PaletteGenerateScreenMode.IMAGE,
                onBack = { navController.popBackStack() },
                isAuthenticated = authState.user != null,
                onRequireLogin = {
                    navController.navigate(Routes.LOGIN) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                authState = authState,
                currentLanguageTag = currentLanguageTag,
                onChangeLanguage = { languageTag ->
                    val safeTag = if (languageTag == "en") "en" else "ru"
                    currentLanguageTag = safeTag
                    scope.launch {
                        container.localeStore.saveLanguageTag(safeTag)
                    }
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(safeTag))
                },
                onOpenProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenPasswordChange = { navController.navigate(Routes.PASSWORD_CHANGE) },
                onOpenFaq = { navController.navigate(Routes.FAQ) },
                onLogout = authViewModel::logout,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE_EDIT) {
            ProfileEditScreen(
                authState = authState,
                onSave = { username, email, currentPassword ->
                    authViewModel.updateProfile(username, email, currentPassword)
                },
                onBack = { navController.popBackStack() },
                onClearMessages = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.PASSWORD_CHANGE) {
            PasswordChangeScreen(
                authState = authState,
                onSendCode = { authViewModel.sendProfilePasswordCode() },
                onChangePassword = { code, newPassword, confirmPassword ->
                    authViewModel.changeProfilePassword(code, newPassword, confirmPassword)
                },
                onBack = { navController.popBackStack() },
                onClearMessages = {
                    authViewModel.clearError()
                    authViewModel.clearInfoMessage()
                },
            )
        }

        composable(Routes.FAQ) {
            FaqScreen(onBack = { navController.popBackStack() })
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

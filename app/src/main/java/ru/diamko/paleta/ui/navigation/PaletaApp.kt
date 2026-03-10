package ru.diamko.paleta.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
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
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val paletteViewModel: PaletteViewModel = viewModel(factory = PaletteViewModel.factory(container))

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val paletteState by paletteViewModel.uiState.collectAsStateWithLifecycle()
    var resetEmailPrefill by rememberSaveable { mutableStateOf("") }
    var currentLanguageTag by rememberSaveable { mutableStateOf("ru") }
    var isApplyingLanguage by rememberSaveable { mutableStateOf(false) }
    var isGuest by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val stored = container.localeStore.readLanguageTag()
        val tag = stored
            ?.takeIf { it == "ru" || it == "en" }
            ?: "ru"
        currentLanguageTag = tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    LaunchedEffect(authState.user?.id) {
        if (authState.user != null) {
            isGuest = false
        }
    }

    LaunchedEffect(isGuest, authState.user?.id) {
        paletteViewModel.setGuestMode(
            isGuest = isGuest,
            isAuthenticated = authState.user != null,
        )
    }

    fun requireLogin() {
        navController.navigate(Routes.LOGIN) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(authState.isCheckingSession, authState.user?.id, isGuest) {
        if (authState.isCheckingSession) {
            return@LaunchedEffect
        }

        val isAuthenticated = authState.user != null
        val current = navController.currentBackStackEntry?.destination?.route
        val authFlow = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD, Routes.RESET_PASSWORD)
        val guestAllowed = setOf(
            Routes.PALETTES,
            Routes.GENERATE_RANDOM,
            Routes.GENERATE_IMAGE,
            Routes.SETTINGS,
            Routes.FAQ,
            Routes.PALETTE_EDITOR,
        )
        val guestForbidden = setOf(Routes.PROFILE_EDIT, Routes.PASSWORD_CHANGE)

        val target = when {
            isAuthenticated -> {
                if (current in authFlow || current == null) Routes.PALETTES else null
            }
            isGuest -> {
                when {
                    current == null -> Routes.PALETTES
                    current in guestForbidden -> Routes.SETTINGS
                    current in authFlow || current in guestAllowed -> null
                    else -> Routes.PALETTES
                }
            }
            else -> {
                if (current in authFlow) null else Routes.LOGIN
            }
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
        startDestination = if (authState.user == null && !isGuest) Routes.LOGIN else Routes.PALETTES,
    ) {
        composable(Routes.LOGIN) {
            if (isGuest) {
                BackHandler {
                    navController.navigate(Routes.PALETTES) {
                        launchSingleTop = true
                    }
                }
            }
            LoginScreen(
                state = authState,
                onLoginClick = authViewModel::login,
                onGoRegisterClick = { navController.navigate(Routes.REGISTER) },
                onGoForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onContinueAsGuestClick = {
                    isGuest = true
                    navController.navigate(Routes.PALETTES) {
                        launchSingleTop = true
                    }
                },
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
                isAuthenticated = authState.user != null,
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
                onRequireLogin = { requireLogin() },
            )
        }

        composable(Routes.GENERATE_IMAGE) {
            PaletteGenerateScreen(
                paletteViewModel = paletteViewModel,
                mode = PaletteGenerateScreenMode.IMAGE,
                onBack = { navController.popBackStack() },
                isAuthenticated = authState.user != null,
                onRequireLogin = { requireLogin() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                authState = authState,
                currentLanguageTag = currentLanguageTag,
                isApplyingLanguage = isApplyingLanguage,
                onChangeLanguage = { languageTag ->
                    val safeTag = if (languageTag == "en") "en" else "ru"
                    if (safeTag != currentLanguageTag && !isApplyingLanguage) {
                        isApplyingLanguage = true
                        currentLanguageTag = safeTag
                        scope.launch {
                            runCatching {
                                container.localeStore.saveLanguageTag(safeTag)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(safeTag))
                                delay(250)
                                findActivity(context)?.recreate()
                            }
                            isApplyingLanguage = false
                        }
                    }
                },
                onOpenProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenPasswordChange = { navController.navigate(Routes.PASSWORD_CHANGE) },
                onOpenFaq = { navController.navigate(Routes.FAQ) },
                onOpenLogin = { navController.navigate(Routes.LOGIN) },
                onOpenRegister = { navController.navigate(Routes.REGISTER) },
                onLogout = {
                    isGuest = false
                    authViewModel.logout()
                },
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
                onBack = {
                    if (paletteId == null) {
                        navController.navigate(Routes.PALETTES) {
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                isAuthenticated = authState.user != null,
                onRequireLogin = { requireLogin() },
            )
        }
    }
}

private tailrec fun findActivity(context: Context): Activity? {
    return when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}

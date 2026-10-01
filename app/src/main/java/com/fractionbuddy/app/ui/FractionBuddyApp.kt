package com.fractionbuddy.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fractionbuddy.app.AppContainer
import com.fractionbuddy.app.data.repository.AppPreferences
import com.fractionbuddy.app.ui.calculator.CalculatorScreen
import com.fractionbuddy.app.ui.calculator.CalculatorViewModel
import com.fractionbuddy.app.ui.history.HistoryScreen
import com.fractionbuddy.app.ui.history.HistoryViewModel
import com.fractionbuddy.app.ui.practice.PracticeSessionScreen
import com.fractionbuddy.app.ui.practice.PracticeSessionViewModel
import com.fractionbuddy.app.ui.practice.PracticeSetupScreen
import com.fractionbuddy.app.ui.practice.PracticeSetupViewModel
import com.fractionbuddy.app.ui.practice.ResultsScreen
import com.fractionbuddy.app.ui.practice.ResultsViewModel
import com.fractionbuddy.app.ui.practice.RetryScreen
import com.fractionbuddy.app.ui.practice.RetryViewModel
import com.fractionbuddy.app.ui.settings.PrivacyScreen
import com.fractionbuddy.app.ui.settings.SettingsScreen
import com.fractionbuddy.app.ui.settings.SettingsViewModel
import com.fractionbuddy.app.ui.theme.FractionBuddyTheme
import com.fractionbuddy.app.ui.theme.LocalReducedMotion
import com.fractionbuddy.app.ui.workbench.WorkbenchScreen
import com.fractionbuddy.app.ui.workbench.WorkbenchViewModel

private object Routes {
    const val WORKBENCH = "workbench"
    const val PRACTICE = "practice"
    const val SESSION = "session/{id}"
    const val RESULTS = "results/{id}"
    const val RETRY = "retry/{id}"
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"

    fun session(id: Long) = "session/$id"
    fun results(id: Long) = "results/$id"
    fun retry(id: Long) = "retry/$id"
}

/** Creates a ViewModel with access to the container and a SavedStateHandle (manual DI). */
@Composable
private inline fun <reified VM : ViewModel> appViewModel(
    container: AppContainer,
    noinline create: (AppContainer, SavedStateHandle) -> VM,
): VM = viewModel(
    factory = viewModelFactory {
        initializer { create(container, createSavedStateHandle()) }
    },
)

@Composable
fun FractionBuddyApp(container: AppContainer) {
    val prefs by container.preferences.preferences.collectAsStateWithLifecycle(AppPreferences())
    FractionBuddyTheme {
        CompositionLocalProvider(LocalReducedMotion provides prefs.reducedMotion) {
            val nav = rememberNavController()
            AppNavHost(nav, container)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, container: AppContainer) {
    val idArg = remember { listOf(navArgument("id") { type = NavType.LongType }) }
    fun back() { nav.popBackStack() }
    fun toWorkbench() { nav.popBackStack(Routes.WORKBENCH, inclusive = false) }

    NavHost(navController = nav, startDestination = Routes.WORKBENCH) {
        composable(Routes.WORKBENCH) {
            val vm = appViewModel(container) { c, _ -> WorkbenchViewModel(c.preferences) }
            WorkbenchScreen(
                viewModel = vm,
                onPractice = { nav.navigate(Routes.PRACTICE) { launchSingleTop = true } },
                onCalculator = { nav.navigate(Routes.CALCULATOR) { launchSingleTop = true } },
                onHistory = { nav.navigate(Routes.HISTORY) { launchSingleTop = true } },
                onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.PRACTICE) {
            val vm = appViewModel(container) { c, _ -> PracticeSetupViewModel(c.practice, c.preferences, c.randomProvider) }
            PracticeSetupScreen(
                vm = vm,
                onBack = ::back,
                onOpenSession = { id -> nav.navigate(Routes.session(id)) },
            )
        }
        composable(Routes.SESSION, arguments = idArg) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm = appViewModel(container) { c, h -> PracticeSessionViewModel(id, c.practice, h) }
            PracticeSessionScreen(
                vm = vm,
                onBack = ::back, // progress is already saved; the session can be resumed later
                onResults = { sid ->
                    nav.navigate(Routes.results(sid)) { popUpTo(Routes.WORKBENCH) }
                },
                onEndedWithoutAnswers = ::back,
            )
        }
        composable(Routes.RESULTS, arguments = idArg) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm = appViewModel(container) { c, _ -> ResultsViewModel(id, c.practice, c.preferences, c.randomProvider) }
            ResultsScreen(
                vm = vm,
                onBack = ::back,
                onBackToWorkbench = ::toWorkbench,
                onRetry = { sid -> nav.navigate(Routes.retry(sid)) },
                onOpenSession = { sid -> nav.navigate(Routes.session(sid)) { popUpTo(Routes.WORKBENCH) } },
            )
        }
        composable(Routes.RETRY, arguments = idArg) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm = appViewModel(container) { c, h -> RetryViewModel(id, c.practice, h) }
            RetryScreen(vm = vm, onDone = ::back)
        }
        composable(Routes.CALCULATOR) {
            val vm = appViewModel(container) { c, _ -> CalculatorViewModel(c.calculator, c.preferences) }
            CalculatorScreen(vm = vm, onBack = ::back)
        }
        composable(Routes.HISTORY) {
            val vm = appViewModel(container) { c, _ -> HistoryViewModel(c.practice) }
            HistoryScreen(vm = vm, onBack = ::back, onOpen = { id -> nav.navigate(Routes.results(id)) })
        }
        composable(Routes.SETTINGS) {
            val vm = appViewModel(container) { c, _ -> SettingsViewModel(c) }
            SettingsScreen(vm = vm, onBack = ::back, onPrivacy = { nav.navigate(Routes.PRIVACY) })
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = ::back)
        }
    }
}

package com.belajar.cpns.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.belajar.cpns.data.prefs.AppPreferences
import com.belajar.cpns.ui.home.HomeScreen
import com.belajar.cpns.ui.latihan.LatihanScreen
import com.belajar.cpns.ui.materi.MateriDetailScreen
import com.belajar.cpns.ui.materi.MateriScreen
import com.belajar.cpns.ui.onboarding.OnboardingScreen
import com.belajar.cpns.ui.progres.ProgresScreen
import com.belajar.cpns.ui.quiz.QuizScreen
import com.belajar.cpns.ui.result.ResultScreen
import com.belajar.cpns.ui.review.ReviewScreen
import com.belajar.cpns.ui.settings.SettingsScreen
import com.belajar.cpns.ui.theme.Muted
import com.belajar.cpns.ui.tryout.TryoutScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LATIHAN = "latihan"
    const val MATERI = "materi"
    const val PROGRES = "progres"
    const val SETTINGS = "settings"
    const val TRYOUT = "tryout/{sessionId}"
    const val RESULT = "result/{sessionId}"
    const val REVIEW = "review/{sessionId}/{index}"
    const val QUIZ = "quiz/{sessionId}"
    const val MATERI_DETAIL = "materi/{pageId}"

    fun tryout(sessionId: Long) = "tryout/$sessionId"
    fun result(sessionId: Long) = "result/$sessionId"
    fun review(sessionId: Long, index: Int = 0) = "review/$sessionId/$index"
    fun quiz(sessionId: Long) = "quiz/$sessionId"
    fun materi(pageId: String) = "materi/$pageId"
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Beranda", Icons.Default.Home),
    BottomItem(Routes.LATIHAN, "Latihan", Icons.Outlined.Quiz),
    BottomItem(Routes.MATERI, "Materi", Icons.Outlined.MenuBook),
    BottomItem(Routes.PROGRES, "Progres", Icons.Outlined.BarChart)
)

@Composable
fun SiapSkdNavGraph(preferences: AppPreferences) {
    val navController = rememberNavController()
    val onboardingDone by preferences.onboardingDone.collectAsStateWithLifecycle()
    val startDestination = remember { if (onboardingDone) Routes.HOME else Routes.ONBOARDING }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (bottomItems.any { it.route == currentRoute }) BottomBar(navController, currentRoute) }
    ) { innerPadding ->
        NavHost(navController, startDestination, Modifier.padding(innerPadding)) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(preferences = preferences, onDone = {
                    preferences.markOnboardingDone()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onStartTryout = { id -> navController.navigate(Routes.tryout(id)) },
                    onOpenResult = { id -> navController.navigate(Routes.result(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenProgres = { navController.navigate(Routes.PROGRES) { launchSingleTop = true } },
                    onOpenLatihan = { navController.navigate(Routes.LATIHAN) { launchSingleTop = true } },
                    onOpenMateri = { id -> navController.navigate(Routes.materi(id)) }
                )
            }
            composable(Routes.LATIHAN) { LatihanScreen(onStartQuiz = { id -> navController.navigate(Routes.quiz(id)) }) }
            composable(Routes.MATERI) { MateriScreen(onOpen = { id -> navController.navigate(Routes.materi(id)) }) }
            composable(Routes.PROGRES) { ProgresScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MATERI_DETAIL, arguments = listOf(navArgument("pageId") { type = NavType.StringType })) {
                MateriDetailScreen(onBack = { navController.popBackStack() }, onStartQuiz = { id -> navController.navigate(Routes.quiz(id)) })
            }
            composable(Routes.QUIZ, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
                QuizScreen(onExit = { navController.popBackStack() })
            }
            composable(Routes.TRYOUT, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
                TryoutScreen(
                    onFinished = { id -> navController.navigate(Routes.result(id)) { popUpTo(Routes.HOME) } },
                    onExit = { navController.popBackStack() }
                )
            }
            composable(Routes.RESULT, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) {
                ResultScreen(
                    onBackHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                    onReview = { id, index -> navController.navigate(Routes.review(id, index)) }
                )
            }
            composable(
                Routes.REVIEW,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }, navArgument("index") { type = NavType.IntType })
            ) { ReviewScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        bottomItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

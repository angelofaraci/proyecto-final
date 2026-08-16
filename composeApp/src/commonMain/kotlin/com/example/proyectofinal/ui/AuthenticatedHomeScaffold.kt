package com.example.proyectofinal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.proyectofinal.ui.activities.LessonMapScreen
import com.example.proyectofinal.ui.home.HomeDashboardScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.nav_tab_activities
import proyectofinal.composeapp.generated.resources.nav_tab_home
import proyectofinal.composeapp.generated.resources.nav_tab_profile
import proyectofinal.composeapp.generated.resources.nav_tab_progress
import proyectofinal.composeapp.generated.resources.tab_activities
import proyectofinal.composeapp.generated.resources.tab_home
import proyectofinal.composeapp.generated.resources.tab_profile
import proyectofinal.composeapp.generated.resources.tab_progress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthenticatedHomeScaffold(
    onLogout: () -> Unit,
    router: MainRouterViewModel = koinViewModel<MainRouterViewModel>()
) {
    val selectedTab by router.target.collectAsState()
    var isExercisePlayerActive by remember { mutableStateOf(false) }
    val onLogoutAndReset = {
        isExercisePlayerActive = false
        logoutFromAuthenticatedHome(router, onLogout)
    }

    Scaffold(
        bottomBar = {
            if (homeBottomNavigationVisible(isExercisePlayerActive)) {
                NavigationBar {
                    mainDestinations().forEach { destination ->
                        NavigationBarItem(
                            selected = selectedTab == destination.tab,
                            onClick = { router.select(destination.tab) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.icon),
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeDashboardScreen(router = router, onLogout = onLogoutAndReset)
                MainTab.ACTIVITIES -> LessonMapScreen(
                    onShowHome = router::showHome,
                    onExercisePlayerActiveChanged = { isExercisePlayerActive = it }
                )
                MainTab.PROGRESS -> PlaceholderScreen(
                    title = stringResource(Res.string.nav_tab_progress),
                    onExploreActivities = router::showActivities
                )
                MainTab.PROFILE -> ProfileScreen(onLogout = onLogoutAndReset)
            }
        }
    }
}

internal fun homeBottomNavigationVisible(isExercisePlayerActive: Boolean): Boolean = !isExercisePlayerActive

internal fun logoutFromAuthenticatedHome(
    router: MainRouterViewModel,
    onLogout: () -> Unit
) {
    router.showHome()
    onLogout()
}

private data class MainDestination(
    val tab: MainTab,
    val label: String,
    val icon: DrawableResource
)

@Composable
private fun mainDestinations() = listOf(
    MainDestination(MainTab.HOME, stringResource(Res.string.nav_tab_home), Res.drawable.tab_home),
    MainDestination(MainTab.ACTIVITIES, stringResource(Res.string.nav_tab_activities), Res.drawable.tab_activities),
    MainDestination(MainTab.PROGRESS, stringResource(Res.string.nav_tab_progress), Res.drawable.tab_progress),
    MainDestination(MainTab.PROFILE, stringResource(Res.string.nav_tab_profile), Res.drawable.tab_profile)
)

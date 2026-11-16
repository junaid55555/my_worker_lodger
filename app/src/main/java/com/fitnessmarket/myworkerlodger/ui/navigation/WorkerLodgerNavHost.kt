package com.fitnessmarket.myworkerlodger.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitnessmarket.myworkerlodger.R
import com.fitnessmarket.myworkerlodger.ui.bed.BedDetailScreen
import com.fitnessmarket.myworkerlodger.ui.employer.EmployerDetailScreen
import com.fitnessmarket.myworkerlodger.ui.employers.EmployersScreen
import com.fitnessmarket.myworkerlodger.ui.overview.OverviewScreen
import com.fitnessmarket.myworkerlodger.ui.properties.PropertiesScreen
import com.fitnessmarket.myworkerlodger.ui.property.PropertyDetailScreen
import com.fitnessmarket.myworkerlodger.ui.room.RoomDetailScreen
import com.fitnessmarket.myworkerlodger.ui.worker.WorkerDetailScreen
import com.fitnessmarket.myworkerlodger.ui.workers.WorkersScreen

/** The app's screens and the arguments they take. */
object Routes {
    const val PROPERTY_ID = "propertyId"
    const val ROOM_ID = "roomId"
    const val BED_ID = "bedId"
    const val EMPLOYER_ID = "employerId"
    const val WORKER_ID = "workerId"

    const val OVERVIEW = "overview"
    const val PROPERTIES = "properties"
    const val PROPERTY = "property/{$PROPERTY_ID}"
    const val ROOM = "room/{$ROOM_ID}"
    const val BED = "bed/{$BED_ID}"
    const val WORKERS = "workers"
    const val WORKER = "worker/{$WORKER_ID}"
    const val EMPLOYERS = "employers"
    const val EMPLOYER = "employer/{$EMPLOYER_ID}"

    fun property(id: String) = "property/$id"
    fun room(id: String) = "room/$id"
    fun bed(id: String) = "bed/$id"
    fun worker(id: String) = "worker/$id"
    fun employer(id: String) = "employer/$id"
}

/** The sections reachable from the bottom navigation bar. */
private enum class Section(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Overview(Routes.OVERVIEW, R.string.overview_title, Icons.Filled.Home),
    Properties(Routes.PROPERTIES, R.string.properties_title, Icons.AutoMirrored.Filled.List),
    Workers(Routes.WORKERS, R.string.workers_title, Icons.Filled.Person),
    Employers(Routes.EMPLOYERS, R.string.employers_title, Icons.Filled.AccountBox),
}

@Composable
fun WorkerLodgerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showSections = Section.entries.any { it.route == destination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showSections) {
                NavigationBar {
                    Section.entries.forEach { section ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == section.route } == true,
                            onClick = { navController.openSection(section.route) },
                            icon = { Icon(section.icon, contentDescription = null) },
                            label = { Text(stringResource(section.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.OVERVIEW,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.OVERVIEW) {
                OverviewScreen(onOpenProperty = { navController.navigate(Routes.property(it.value)) })
            }
            composable(Routes.PROPERTIES) {
                PropertiesScreen(onOpenProperty = { navController.navigate(Routes.property(it.value)) })
            }
            composable(Routes.PROPERTY, arguments = listOf(stringArgument(Routes.PROPERTY_ID))) {
                PropertyDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoom = { navController.navigate(Routes.room(it.value)) },
                )
            }
            composable(Routes.ROOM, arguments = listOf(stringArgument(Routes.ROOM_ID))) {
                RoomDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBed = { navController.navigate(Routes.bed(it.value)) },
                )
            }
            composable(Routes.BED, arguments = listOf(stringArgument(Routes.BED_ID))) {
                BedDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWorker = { navController.navigate(Routes.worker(it.value)) },
                )
            }
            composable(Routes.WORKERS) {
                WorkersScreen(onOpenWorker = { navController.navigate(Routes.worker(it.value)) })
            }
            composable(Routes.WORKER, arguments = listOf(stringArgument(Routes.WORKER_ID))) {
                WorkerDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EMPLOYERS) {
                EmployersScreen(onOpenEmployer = { navController.navigate(Routes.employer(it.value)) })
            }
            composable(Routes.EMPLOYER, arguments = listOf(stringArgument(Routes.EMPLOYER_ID))) {
                EmployerDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWorker = { navController.navigate(Routes.worker(it.value)) },
                )
            }
        }
    }
}

private fun stringArgument(name: String) = navArgument(name) { type = NavType.StringType }

/** Switches to a bottom-bar section, keeping each section's own back stack. */
private fun NavHostController.openSection(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

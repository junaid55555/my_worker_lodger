package com.fitnessmarket.myworkerlodger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitnessmarket.myworkerlodger.ui.properties.PropertiesScreen
import com.fitnessmarket.myworkerlodger.ui.property.PropertyDetailScreen
import com.fitnessmarket.myworkerlodger.ui.room.RoomDetailScreen

/** The app's screens and the arguments they take. */
object Routes {
    const val PROPERTY_ID = "propertyId"
    const val ROOM_ID = "roomId"

    const val PROPERTIES = "properties"
    const val PROPERTY = "property/{$PROPERTY_ID}"
    const val ROOM = "room/{$ROOM_ID}"

    fun property(id: String) = "property/$id"
    fun room(id: String) = "room/$id"
}

@Composable
fun WorkerLodgerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.PROPERTIES) {
        composable(Routes.PROPERTIES) {
            PropertiesScreen(onOpenProperty = { navController.navigate(Routes.property(it.value)) })
        }
        composable(Routes.PROPERTY, arguments = listOf(navArgument(Routes.PROPERTY_ID) { type = NavType.StringType })) {
            PropertyDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenRoom = { navController.navigate(Routes.room(it.value)) },
            )
        }
        composable(Routes.ROOM, arguments = listOf(navArgument(Routes.ROOM_ID) { type = NavType.StringType })) {
            RoomDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

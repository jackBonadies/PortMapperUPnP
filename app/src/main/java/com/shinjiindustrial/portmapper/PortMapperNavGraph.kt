package com.shinjiindustrial.portmapper

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shinjiindustrial.portmapper.common.parseCadence
import com.shinjiindustrial.portmapper.common.validateCadence
import com.shinjiindustrial.portmapper.domain.PortMappingUserInput
import com.shinjiindustrial.portmapper.ui.RuleCreationDialog
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PortMapperNavGraph(portViewModel : PortViewModel, themeState : ThemeUiState, modifier : Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main_screen", modifier = modifier) {
        composable(
            "main_screen",
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                //this is whats used when going back from "create rule"
                slideInVertically(
                    initialOffsetY = { it / 2 }, animationSpec = tween(
                        durationMillis = 200, easing = FastOutSlowInEasing
                    )
                ) + fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300))
            },
        )
        {
            PortMapperMainScreen(portViewModel, themeState, navController = navController)
        }
        composable(
            "full_screen_dialog?description={description}&internalIp={internalIp}&internalRange={internalRange}&externalIp={externalIp}&externalRange={externalRange}&protocol={protocol}&leaseDuration={leaseDuration}&enabled={enabled}&autorenew={autorenew}&autorenewManualCadence={autorenewManualCadence}",
            arguments = listOf(
                navArgument("description") {
                    nullable = true; type = NavType.StringType
                }, // only if nullable (or default) are they optional
                navArgument("internalIp") { nullable = true; type = NavType.StringType },
                navArgument("internalRange") { nullable = true; type = NavType.StringType },
                navArgument("externalIp") { nullable = true; type = NavType.StringType },
                navArgument("externalRange") { nullable = true; type = NavType.StringType },
                navArgument("protocol") { nullable = true; type = NavType.StringType },
                navArgument("leaseDuration") { nullable = true; type = NavType.StringType },
                navArgument("enabled") { type = NavType.BoolType; defaultValue = false },
                navArgument("autorenew") { type = NavType.BoolType; defaultValue = false },
                navArgument("autorenewManualCadence") { type = NavType.StringType; nullable = true; },
            ),
            popExitTransition = {

                slideOutVertically(
                    targetOffsetY = { it / 2 }, animationSpec = tween(
                        durationMillis = 200, easing = FastOutSlowInEasing
                    )
                ) + fadeOut(animationSpec = tween(100))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it }, animationSpec = tween(
                        durationMillis = 5000, easing = FastOutSlowInEasing
                    )
                )
            },
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it }, animationSpec = tween(
                        durationMillis = 200, easing = FastOutSlowInEasing
                    )
                ) + fadeIn(animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(150))
            }
        )
        { backStackEntry ->
            val arguments = backStackEntry.arguments
            val desc = arguments?.getString("description")
            val internalIp = arguments?.getString("internalIp")
            val internalRange = arguments?.getString("internalRange")
            val externalIp = arguments?.getString("externalIp")
            val externalRange = arguments?.getString("externalRange")
            val protocol = arguments?.getString("protocol")
            val leaseDuration = arguments?.getString("leaseDuration")
            val enabled = arguments?.getBoolean("enabled")
            val autorenew = arguments?.getBoolean("autorenew")
            val autorenewManualCadence = arguments?.getString("autorenewManualCadence")

            var portMappingUserInputToEdit: PortMappingUserInput? = null
            if (desc != null) {
                val cadence = parseCadence(autorenewManualCadence!!)
                portMappingUserInputToEdit = PortMappingUserInput(
                    desc,
                    internalIp!!,
                    internalRange!!,
                    externalIp!!,
                    externalRange!!,
                    protocol!!,
                    leaseDuration!!,
                    enabled!!,
                    autorenew!!,
                    cadence.cadence)
            }

            RuleCreationDialog(
                navController = navController,
                portViewModel,
                ruleToEdit = portMappingUserInputToEdit
            )
        }
    }
}

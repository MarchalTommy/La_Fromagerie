package com.mtdevelopment.lafromagerie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.mtdevelopment.core.util.Screen


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
//        splashScreen(navController = navController)
//        onBoardingScreen(navController = navController)
//        authenticationScreen(navController = navController)
//        signInScreen(navController = navController)
//        signUpScreen(navController = navController)
//        homeScreen(navController = navController)
//        detailScreen(navController = navController)
//        videoScreen(navController = navController)
//        watchList(navController = navController)
//        searchScreen(navController = navController)
//        profile(navController = navController)
    }
}
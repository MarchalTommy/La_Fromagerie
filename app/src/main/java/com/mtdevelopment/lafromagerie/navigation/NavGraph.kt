package com.mtdevelopment.lafromagerie.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mtdevelopment.checkout.presentation.screen.CheckoutScreen
import com.mtdevelopment.checkout.presentation.screen.DeliveryOptionScreen
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.details.presentation.composable.DetailScreen
import com.mtdevelopment.home.presentation.composable.HomeScreen


@Composable
fun NavGraph(
    paddingValues: PaddingValues,
    navController: NavHostController,
    onGooglePayButtonClick: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreen,
        modifier = Modifier.padding(paddingValues)
    ) {

        composable<HomeScreen> {
            HomeScreen(
                navigateToDetail = { product ->
                    navController.navigate(DetailDestination(product))
                }, navigateToDelivery = { items ->
                    navController.navigate(
                        DeliveryOptionScreen(items)
                    )
                })
        }

        composable<DetailDestination>(
            typeMap = UiProductObject.typeMap,
            enterTransition = {
                scaleIntoContainer()
            },
            exitTransition = {
                scaleOutOfContainer(ScaleTransitionDirection.INWARDS)
            },
            popEnterTransition = {
                scaleIntoContainer(ScaleTransitionDirection.OUTWARDS)
            },
            popExitTransition = {
                scaleOutOfContainer()
            }
        ) {
            val args = it.toRoute<DetailDestination>()
            DetailScreen(
                detailProductObject = args.productObject
            )
        }

        composable<DeliveryOptionScreen>(
            enterTransition = {
                scaleIntoContainer()
            },
            exitTransition = {
                scaleOutOfContainer(ScaleTransitionDirection.INWARDS)
            },
            popEnterTransition = {
                scaleIntoContainer(ScaleTransitionDirection.OUTWARDS)
            },
            popExitTransition = {
                scaleOutOfContainer()
            }
        ) {
            DeliveryOptionScreen(navigateToCheckout = { checkoutData ->
                navController.navigate(
                    CheckoutScreen(
                        checkoutData = checkoutData
                    )
                )
            })
        }

        composable<CheckoutScreen>(
            enterTransition = {
                scaleIntoContainer()
            },
            exitTransition = {
                scaleOutOfContainer(ScaleTransitionDirection.INWARDS)
            },
            popEnterTransition = {
                scaleIntoContainer(ScaleTransitionDirection.OUTWARDS)
            },
            popExitTransition = {
                scaleOutOfContainer()
            }
        ) {
            CheckoutScreen(
                onGooglePayButtonClick = onGooglePayButtonClick
            )
        }

    }
}
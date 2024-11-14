package com.mtdevelopment.lafromagerie.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.screen.CheckoutScreen
import com.mtdevelopment.checkout.presentation.screen.DeliveryOptionScreen
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.details.presentation.composable.DetailScreen
import com.mtdevelopment.home.presentation.composable.HomeScreen


@Composable
fun NavGraph(
    paddingValues: PaddingValues,
    navController: NavHostController,
    cartViewModel: CartViewModel,
    onGooglePayButtonClick: (priceCents: Long) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreen,
        modifier = Modifier.padding(paddingValues)
    ) {

        composable<HomeScreen> {
            HomeScreen(
                cartViewModel = cartViewModel,
                navigateToDetail = {
                    navController.navigate(DetailDestination)
                }, navigateToDelivery = {
                    navController.navigate(
                        DeliveryOptionScreen
                    )
                })
        }

        composable<DetailDestination>(
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
                viewModel = cartViewModel
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
            DeliveryOptionScreen(navigateToCheckout = {
                navController.navigate(
                    CheckoutScreen
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
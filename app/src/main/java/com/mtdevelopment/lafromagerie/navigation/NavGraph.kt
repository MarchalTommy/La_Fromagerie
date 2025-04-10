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
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.delivery.presentation.screen.DeliveryOptionScreen
import com.mtdevelopment.details.presentation.composable.DetailScreen
import com.mtdevelopment.home.presentation.composable.HomeScreen


@Composable
fun NavGraph(
    paddingValues: PaddingValues,
    navController: NavHostController,
    cartViewModel: CartViewModel,
    mainViewModel: MainViewModel,
    onGooglePayButtonClick: (priceCents: Long) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreenDestination(),
        modifier = Modifier.padding(paddingValues)
    ) {

        composable<HomeScreenDestination> {
            val args = it.toRoute<HomeScreenDestination>()
            HomeScreen(
                mainViewModel = mainViewModel,
                cartViewModel = cartViewModel,
                shouldRefresh = args.shouldRefresh,
                navigateToDetail = {
                    navController.navigate(DetailScreenDestination)
                }, navigateToDelivery = {
                    navController.navigate(
                        DeliveryOptionScreenDestination
                    )
                })
        }

        composable<DetailScreenDestination>(
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
            DetailScreen(
                viewModel = cartViewModel,
                mainViewModel = mainViewModel,
                onProductEdited = {
                    navController.navigate(HomeScreenDestination(shouldRefresh = true))
                },
                onProductDeleted = {
                    navController.navigate(HomeScreenDestination(shouldRefresh = true))
                }
            )
        }

        composable<DeliveryOptionScreenDestination>(
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
            DeliveryOptionScreen(
                mainViewModel = mainViewModel,
                navigateToCheckout = {
                    navController.navigate(
                        CheckoutScreenDestination
                    )
                },
                navigateBack = {
                    navController.navigateUp()
                })
        }

        composable<CheckoutScreenDestination>(
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
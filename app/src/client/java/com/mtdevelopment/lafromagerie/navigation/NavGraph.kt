package com.mtdevelopment.lafromagerie.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.domain.usecase.ConsumePaymentOutcomeUseCase
import com.mtdevelopment.checkout.presentation.screen.AfterPaymentScreen
import com.mtdevelopment.checkout.presentation.screen.CheckoutScreen
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.delivery.presentation.screen.DeliveryOptionScreen
import com.mtdevelopment.details.presentation.composable.DetailScreen
import com.mtdevelopment.home.presentation.composable.HomeScreen
import org.koin.compose.koinInject


@Composable
fun NavGraph(
    paddingValues: PaddingValues,
    navController: NavHostController,
    cartViewModel: CartViewModel,
    mainViewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreenDestination(),
        modifier = Modifier.padding(paddingValues)
    ) {

        composable<HomeScreenDestination> {
            val args = it.toRoute<HomeScreenDestination>()
            val sumUpCallback by mainViewModel.sumUpCallbackTrigger.collectAsState()
            LaunchedEffect(sumUpCallback) {
                if (sumUpCallback) {
                    navController.navigate(CheckoutScreenDestination)
                }
            }

            // A payment reconciled in the background (app killed during hosted checkout)
            // leaves an outcome to surface exactly once: on success we route to the same
            // success screen a normal return would reach; on failure we keep the cart and
            // tell the customer they can retry.
            val consumePaymentOutcome = koinInject<ConsumePaymentOutcomeUseCase>()
            LaunchedEffect(Unit) {
                consumePaymentOutcome()?.let { outcome ->
                    if (outcome.isPaid) {
                        navController.navigate(
                            AfterPaymentScreenDestination(clientName = outcome.clientName)
                        )
                    } else {
                        mainViewModel.setError(
                            "Votre dernier paiement n'a pas abouti. " +
                                    "Votre panier est conservé, vous pouvez réessayer."
                        )
                    }
                }
            }

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
                }
            )
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
            BackHandler {
                cartViewModel.setCartVisibility(false)
                navController.navigateUp()
            }

            DeliveryOptionScreen(
                navigateToCheckout = {
                    navController.navigate(
                        CheckoutScreenDestination
                    )
                },
                navigateBack = {
                    cartViewModel.setCartVisibility(false)
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
                mainViewModel = mainViewModel,
                onNavigatePaymentSuccess = {
                    navController.navigate(
                        AfterPaymentScreenDestination(
                            clientName = it
                        )
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable<AfterPaymentScreenDestination>(
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
            val args = it.toRoute<AfterPaymentScreenDestination>()
            AfterPaymentScreen(
                clientName = args.clientName,
                onHomeClick = {
                    navController.navigate(
                        HomeScreenDestination(shouldRefresh = false)
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                            inclusive = false
                        }
                    }
                }
            )
        }
    }
}
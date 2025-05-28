package com.mtdevelopment.lafromagerie.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mtdevelopment.admin.presentation.screen.DeliveryHelperScreen
import com.mtdevelopment.admin.presentation.screen.OrderPreparationScreen
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.screen.AfterPaymentScreen
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
    launchDeliveryTracking: () -> Unit,
    stopDeliveryTracking: () -> Unit
) {
    val shouldGoToDeliveryHelper = mainViewModel.shouldGoToDeliveryHelper.collectAsState()

    LaunchedEffect(shouldGoToDeliveryHelper.value) {
        if (shouldGoToDeliveryHelper.value) {
            navController.navigate(DeliveryHelperScreenDestination)
            mainViewModel.setShouldGoToDeliveryHelper(false)
        }
    }

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
                }, navigateToOrders = {
                    navController.navigate(
                        OrdersScreenDestination
                    )
                }
            )
        }

        composable<DeliveryHelperScreenDestination> {
            DeliveryHelperScreen(
                launchDeliveryTracking = {
                    launchDeliveryTracking.invoke()
                },
                stopDeliveryTracking = {
                    stopDeliveryTracking.invoke()
                })
        }

        composable<OrdersScreenDestination> {
            OrderPreparationScreen()
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
                cartViewModel.loadCart(withVisibility = false)
                navController.navigateUp()
            }

            DeliveryOptionScreen(
                navigateToCheckout = {
                    navController.navigate(
                        CheckoutScreenDestination
                    )
                },
                navigateBack = {
                    cartViewModel.loadCart(withVisibility = false)
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
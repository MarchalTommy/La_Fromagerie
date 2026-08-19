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
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.screen.AfterPaymentScreen
import com.mtdevelopment.checkout.presentation.screen.CheckoutScreen
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.core.util.koinViewModel
import com.mtdevelopment.delivery.presentation.screen.DeliveryOptionScreen
import com.mtdevelopment.delivery.presentation.screen.PATH_LIST_NEEDS_REFRESH
import com.mtdevelopment.delivery.presentation.screen.PICKUP_LIST_NEEDS_REFRESH
import com.mtdevelopment.delivery.presentation.screen.PathEditScreen
import com.mtdevelopment.delivery.presentation.screen.PickupPointEditScreen
import com.mtdevelopment.delivery.presentation.screen.PickupPointsScreen
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
    val adminViewModel = koinViewModel<AdminViewModel>()
    val shouldGoToDeliveryHelper = mainViewModel.shouldGoToDeliveryHelper.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.getTrackingStatusOnce() {
            if (it) {
                navController.navigate(DeliveryHelperScreenDestination)
            }
        }
    }

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
                    navController.navigate(HomeScreenDestination(shouldRefresh = false))
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
        ) { backStackEntry ->
            BackHandler {
                cartViewModel.setCartVisibility(false)
                navController.navigateUp()
            }

            // Set by the path editor on its way back. The two screens own separate ViewModel
            // instances, so the list cannot otherwise know a path was written.
            val pathsChanged = backStackEntry.savedStateHandle
                .getStateFlow(PATH_LIST_NEEDS_REFRESH, false)
                .collectAsState()

            DeliveryOptionScreen(
                pathsChanged = pathsChanged.value,
                onPathsChangeHandled = {
                    backStackEntry.savedStateHandle[PATH_LIST_NEEDS_REFRESH] = false
                },
                navigateToCheckout = {
                    navController.navigate(
                        CheckoutScreenDestination
                    )
                },
                navigateToPathEdit = { pathId ->
                    navController.navigate(PathEditScreenDestination(pathId))
                },
                navigateToPickupPoints = {
                    navController.navigate(PickupPointsScreenDestination)
                },
                navigateBack = {
                    cartViewModel.setCartVisibility(false)
                    navController.navigateUp()
                })
        }

        composable<PickupPointsScreenDestination>(
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(ScaleTransitionDirection.INWARDS) },
            popEnterTransition = { scaleIntoContainer(ScaleTransitionDirection.OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) { backStackEntry ->
            // Set by the editor on its way back, same handshake as the path list: the two
            // screens own separate ViewModel instances, so the list cannot otherwise know a
            // point was written.
            val pointsChanged = backStackEntry.savedStateHandle
                .getStateFlow(PICKUP_LIST_NEEDS_REFRESH, false)
                .collectAsState()

            PickupPointsScreen(
                pointsChanged = pointsChanged.value,
                onPointsChangeHandled = {
                    backStackEntry.savedStateHandle[PICKUP_LIST_NEEDS_REFRESH] = false
                },
                navigateToPointEdit = { pointId ->
                    navController.navigate(PickupPointEditScreenDestination(pointId))
                }
            )
        }

        composable<PickupPointEditScreenDestination>(
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(ScaleTransitionDirection.INWARDS) },
            popEnterTransition = { scaleIntoContainer(ScaleTransitionDirection.OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<PickupPointEditScreenDestination>()

            PickupPointEditScreen(
                pointId = args.pointId,
                onSaved = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(PICKUP_LIST_NEEDS_REFRESH, true)
                    navController.navigateUp()
                }
            )
        }

        composable<PathEditScreenDestination>(
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
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<PathEditScreenDestination>()

            PathEditScreen(
                pathId = args.pathId,
                navigateBack = { navController.navigateUp() },
                onSaved = {
                    // The list screen owns its own ViewModel, so it cannot see the write happen.
                    // Flag it on the way back rather than re-fetching on every resume.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(PATH_LIST_NEEDS_REFRESH, true)
                    navController.navigateUp()
                }
            )
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
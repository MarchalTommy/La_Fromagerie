package com.mtdevelopment.lafromagerie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mtdevelopment.checkout.presentation.composable.CheckoutScreen
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.core.presentation.theme.ui.ScaleTransitionDirection
import com.mtdevelopment.core.presentation.theme.ui.scaleIntoContainer
import com.mtdevelopment.core.presentation.theme.ui.scaleOutOfContainer
import com.mtdevelopment.details.presentation.composable.DetailScreen
import com.mtdevelopment.home.presentation.composable.HomeScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val cartViewModel: com.mtdevelopment.cart.presentation.viewmodel.CartViewModel by viewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            modifier = Modifier,
                            colors = TopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                                actionIconContentColor = MaterialTheme.colorScheme.primary,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            title = {
                                Text("La Fromagerie")
                            }
                        )
                    }
                ) { paddingValues ->

                    NavHost(
                        navController = navController,
                        startDestination = CheckoutScreen,
                        modifier = Modifier.padding(paddingValues)
                    ) {

                        composable<HomeScreen> {
                            HomeScreen(
                                cartViewModel,
                                navigateToDetail = { product ->
                                    navController.navigate(DetailDestination(product))
                                }, navigateToCheckout = {
                                    navController.navigate(CheckoutScreen)
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
                                detailProductObject = args.productObject,
                                viewModel = cartViewModel
                            )
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
                            CheckoutScreen(cartViewModel)
                        }

                    }
                }
            }
        }
    }
}

@Serializable
object HomeScreen

@Serializable
data class DetailDestination(
    val productObject: UiProductObject
)

@Serializable
object CheckoutScreen
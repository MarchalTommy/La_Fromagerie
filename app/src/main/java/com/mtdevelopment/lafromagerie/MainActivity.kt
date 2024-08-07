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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.home.presentation.composable.HomeScreen
import com.mtdevelopment.home.presentation.viewmodel.MainViewModel
import com.mtdevelopment.lafromagerie.navigation.CheeseScreens
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModel()

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
                        startDestination = CheeseScreens.Home.name,
                        modifier = Modifier.padding(paddingValues)
                    ) {

                        composable(route = CheeseScreens.Home.name) {
                            HomeScreen(mainViewModel,
                                navigateToDetail = {
                                    navController.navigate(CheeseScreens.Detail.name)
                                }, navigateToCheckout = {
                                    navController.navigate(CheeseScreens.Checkout.name)
                                })
                        }

                    }
                }
            }
        }
    }
}
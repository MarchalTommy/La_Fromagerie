package com.mtdevelopment.lafromagerie

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.lafromagerie.navigation.HomeScreen
import com.mtdevelopment.lafromagerie.navigation.NavGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    // TODO: Add splashscreen
    // TODO: Add app Icon

    private val checkoutViewModel: CheckoutViewModel by viewModel()
    private val cartViewModel: CartViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel()

    val paymentDataLauncher =
        registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
            when (taskResult.status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    taskResult.result!!.let {
                        Log.i("Google Pay result", it.toJson())
                        checkoutViewModel.setPaymentData(it)
                    }
                }
                //CommonStatusCodes.CANCELED -> The user canceled
                //CommonStatusCodes.DEVELOPER_ERROR -> The API returned an error (it.status: Status)
                //else -> Handle internal and other unexpected errors
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val currentBackStackEntry = navController.currentBackStackEntryAsState()
                var homeEntry: NavDestination? = null

                val coroutineScope: CoroutineScope = rememberCoroutineScope()

                val errorState = mainViewModel.errorState

                val snackHostState = remember {
                    SnackbarHostState()
                }

                LaunchedEffect(errorState.shouldShowError) {
                    if (errorState.shouldShowError) {
                        coroutineScope.launch {
                            val result = snackHostState.showSnackbar(
                                message = errorState.message,
                                actionLabel = errorState.actionLabel,
                                duration = if (errorState.actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
                                withDismissAction = errorState.duration == SnackbarDuration.Indefinite && errorState.actionLabel == null
                            )

                            when (result) {
                                SnackbarResult.Dismissed -> mainViewModel.clearError()
                                SnackbarResult.ActionPerformed -> errorState.action()
                            }
                        }
                    }
                }

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
                            },
                            navigationIcon = {
                                AnimatedVisibility(
                                    visible = currentBackStackEntry.value?.destination != homeEntry,
                                    exit = fadeOut(animationSpec = tween(300)),
                                    enter = fadeIn(animationSpec = tween(500))
                                ) {
                                    IconButton(
                                        onClick = {
                                            navController.navigateUp()
                                        },
                                        content = {
                                            Icon(Icons.AutoMirrored.Sharp.ArrowBack, "Back")
                                        }
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        mainViewModel.setError(
                                            "Pas encore implémenté ! Un peu de patience :)",
                                            actionLabel = "Je comprends",
                                            action = { mainViewModel.clearError() })
//                                        TODO("Navigate To Notifications Screen, or Open a Modal Sheet with notifications")
                                    },
                                    content = {
                                        BadgedBox(
                                            modifier = Modifier,
                                            badge = {
                                                // TODO: Notification amount not 0
                                                if (
                                                    false
                                                ) {
                                                    Badge(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White
                                                    ) {
                                                        // TODO: Notification amount
                                                        val notificationsNumber = 4
                                                        Text("$notificationsNumber")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Sharp.Notifications, "Notifications")
                                        }
                                    }
                                )
                            }
                        )
                    },
                    snackbarHost = {
                        SnackbarHost(
                            modifier = Modifier.wrapContentSize(Alignment.BottomCenter),
                            hostState = snackHostState,
                            snackbar = { data ->
                                if (errorState.message.isNotEmpty()) {
                                    errorState.message
                                    Snackbar(
                                        snackbarData = data,
                                        modifier = Modifier.padding(16.dp),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    NavGraph(
                        paddingValues = paddingValues,
                        navController = navController,
                        cartViewModel = cartViewModel,
                        onGooglePayButtonClick = { priceCents ->
                            Log.e("PAYMENT", "BUTTON CLICKED")
                            requestPayment(priceCents)
                        }
                    )
                    homeEntry = navController.getBackStackEntry(HomeScreen).destination
                }
            }
        }
    }

    private fun requestPayment(priceCents: Long) {
        val task = checkoutViewModel.getLoadPaymentDataTask(priceCents)
        task.addOnCompleteListener(paymentDataLauncher::launch)
    }
}
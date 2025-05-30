package com.mtdevelopment.lafromagerie

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
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
import androidx.compose.material.icons.sharp.LocationOn
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
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.lafromagerie.navigation.AfterPaymentScreenDestination
import com.mtdevelopment.lafromagerie.navigation.DeliveryHelperScreenDestination
import com.mtdevelopment.lafromagerie.navigation.DeliveryOptionScreenDestination
import com.mtdevelopment.lafromagerie.navigation.HomeScreenDestination
import com.mtdevelopment.lafromagerie.navigation.NavGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : ComponentActivity() {

    private val cartViewModel: CartViewModel by viewModel()
    private val mainViewModel: MainViewModel by viewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen().apply {
                setOnExitAnimationListener { screen ->
                    val zoomX = ObjectAnimator.ofFloat(
                        screen.iconView,
                        View.SCALE_X,
                        1.0f,
                        0.0f
                    )
                    val zoomY = ObjectAnimator.ofFloat(
                        screen.iconView,
                        View.SCALE_Y,
                        1.0f,
                        0.0f
                    )
                    zoomX.interpolator = OvershootInterpolator()
                    zoomY.interpolator = OvershootInterpolator()
                    zoomX.duration = 500L
                    zoomY.duration = 500L

                    val screenAnimX = ObjectAnimator.ofFloat(
                        screen.view,
                        View.SCALE_X,
                        1.0f,
                        0.0f
                    )
                    val screenAnimY = ObjectAnimator.ofFloat(
                        screen.view,
                        View.SCALE_Y,
                        1.0f,
                        0.0f
                    )
                    screenAnimX.interpolator = OvershootInterpolator()
                    screenAnimY.interpolator = OvershootInterpolator()
                    screenAnimX.duration = 500L
                    screenAnimY.duration = 500L

                    fun removeScreen(coroutineScope: CoroutineScope) {
                        zoomX.doOnEnd {
                            screen.remove()
                            coroutineScope.cancel()
                        }
                        zoomX.start()
                        zoomY.start()
                        screenAnimX.start()
                        screenAnimY.start()
                    }

                    lifecycleScope.launch {
                        mainViewModel.canRemoveSplash.collect {
                            if (it) {
                                removeScreen(this)
                                return@collect
                            }

                            lifecycleScope.launch {
                                delay(5000)
                                if (!mainViewModel.canRemoveSplash.value) {
                                    mainViewModel.setError("Nous avons du mal à charger les fromages...")
                                }
                                removeScreen(this)
                            }
                        }
                    }
                }
            }
        } else {
            setTheme(R.style.Theme_LaFromagerie)
        }
        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val currentBackStackEntry = navController.currentBackStackEntryAsState()

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
                                    visible = currentBackStackEntry.value?.destination?.route?.replace(
                                        "?shouldRefresh={shouldRefresh}",
                                        ""
                                    ) != HomeScreenDestination::class.java.name &&
                                            currentBackStackEntry.value?.destination?.route != AfterPaymentScreenDestination::class.java.name,
                                    exit = fadeOut(animationSpec = tween(300)),
                                    enter = fadeIn(animationSpec = tween(500))
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentBackStackEntry.value?.destination?.route == DeliveryOptionScreenDestination::class.java.name) {
                                                cartViewModel.loadCart(withVisibility = false)
                                            }
                                            navController.navigateUp()
                                        },
                                        content = {
                                            Icon(Icons.AutoMirrored.Sharp.ArrowBack, "Back")
                                        }
                                    )
                                }
                            },
                            actions = {
                                if (currentBackStackEntry.value?.destination?.route != DeliveryHelperScreenDestination::class.java.name) {
                                    IconButton(
                                        modifier = Modifier.size(64.dp),
                                        onClick = {
                                            mainViewModel.setShouldGoToDeliveryHelper(true)
                                        },
                                        content = {
                                            Icon(Icons.Sharp.LocationOn, "Livraison")
                                        }
                                    )
                                }
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
                        mainViewModel = mainViewModel,
                        cartViewModel = cartViewModel
                    )
                }
            }
        }
    }

}
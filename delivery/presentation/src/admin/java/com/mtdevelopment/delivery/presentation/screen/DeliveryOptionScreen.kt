package com.mtdevelopment.delivery.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.delivery.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.delivery.presentation.composable.AdminContent
import com.mtdevelopment.delivery.presentation.composable.DatePickerComposable
import com.mtdevelopment.delivery.presentation.composable.MapBoxComposable

import com.mtdevelopment.delivery.presentation.model.toAdminUiDeliveryPath
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    pathsChanged: Boolean = false,
    onPathsChangeHandled: () -> Unit = {},
    navigateToCheckout: () -> Unit = {},
    navigateToPathEdit: (String?) -> Unit = {},
    navigateToPickupPoints: () -> Unit = {},
    navigateBack: () -> Unit = {}
) {

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()

    val context = LocalContext.current

    val state = remember(deliveryViewModel.deliveryUiDataState) {
        derivedStateOf {
            deliveryViewModel.deliveryUiDataState
        }
    }


    val isConnected = deliveryViewModel.isConnected.collectAsState()

    val scrollState = rememberScrollState()

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    LaunchedEffect(Unit) {
        deliveryViewModel.loadAdminData()
        Rive.init(context)
    }

    LaunchedEffect(pathsChanged) {
        if (pathsChanged) {
            deliveryViewModel.loadAdminData(forceRefresh = true)
            onPathsChangeHandled()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                state = scrollState,
                enabled = state.value.columnScrollingEnabled && state.value.isError.isEmpty()
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Map Card
            MapBoxComposable(
                userLocation = state.value.userCityLocation,
                chosenPath = state.value.selectedPath,
                allPaths = state.value.deliveryPaths,
                isConnectedToInternet = isConnected.value,
                setIsLoading = {
                },
                setColumnScrollingEnabled = {
                    deliveryViewModel.setColumnScrollingEnabled(it)
                },
                onError = {
                    deliveryViewModel.setIsError("Une erreur est survenue lors du chargement de la carte.")
                }
            )

            AdminContent(
                pathList = state.value.deliveryPaths.map { it.toAdminUiDeliveryPath() },
                onPathSelected = { path ->
                    val isExisting = state.value.deliveryPaths.any { it.id == path.id }
                    navigateToPathEdit(path.id.takeIf { isExisting })
                },
                onPathPreSelected = { preselected ->
                    if (preselected != null) {
                        state.value.deliveryPaths.find { it.name == preselected.name }
                            ?.let { deliveryViewModel.updateSelectedPath(it) }
                    } else {
                        deliveryViewModel.updateSelectedPath(null)
                    }
                }
            )

            // Pickup points live here rather than on the admin home: this screen is already
            // the "where and when do I sell" configuration, and the home screen keeps the
            // three buttons it has.
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = navigateToPickupPoints
            ) {
                Icon(Icons.Rounded.Storefront, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Points de retrait (boutique et marchés)")
            }

            Spacer(modifier = Modifier.imePadding())
        }

        // Loading animation
        RiveAnimation(
            isLoading = state.value.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )

        // Error Composable
        ErrorOverlay(
            isShown = state.value.isError.isNotBlank(),
            message = state.value.isError.ifBlank { "Une erreur inconnue est survenue.\nSi le problème persiste merci de nous contacter !" },
            onDismiss = {
                deliveryViewModel.setIsError("")
            }
        )

        if (state.value.datePickerVisibility) {
            DatePickerComposable(
                paths = listOfNotNull(state.value.selectedPath),
                shouldRemoveDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(false)
                },
                onDateSelected = { date, path ->
                    deliveryViewModel.updateSelectedPath(path)
                    deliveryViewModel.saveSelectedDate(date)
                }
            )
        }
    }
}
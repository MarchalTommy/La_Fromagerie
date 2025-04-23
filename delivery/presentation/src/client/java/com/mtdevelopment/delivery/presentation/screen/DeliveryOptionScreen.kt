package com.mtdevelopment.delivery.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.mtdevelopment.delivery.presentation.composable.CustomerContent
import com.mtdevelopment.delivery.presentation.composable.DatePickerComposable
import com.mtdevelopment.delivery.presentation.composable.MapBoxComposable
import com.mtdevelopment.delivery.presentation.composable.PermissionManagerComposable
import com.mtdevelopment.delivery.presentation.composable.getDatePickerState
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    navigateToCheckout: () -> Unit = {},
    navigateBack: () -> Unit = {}
) {

    // TODO: Manage that selected address is on a Path ->
    // Fetch all paths WITHOUT geoJson, allow it to be smaller as we just need cities + latlng
    // Compare city to assert it's in it. If it's not, check if it's far with some latlng calculus
    // If it's in a city less than 10km away from a city of a path, propose to ask the EARL to take it into account automatically
    // Else if it's too far, still warns EARL but say sorry, not available for now, but keep the app up to date as it may change !


    val context = LocalContext.current

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val isConnected = deliveryViewModel.isConnected.collectAsState()

    val state = remember(deliveryViewModel.deliveryUiDataState) {
        derivedStateOf {
            deliveryViewModel.deliveryUiDataState
        }
    }
    val datePickerState = remember(state.value.selectedPath) {
        derivedStateOf {
            getDatePickerState(state.value.selectedPath)
        }
    }
    val scrollState = rememberScrollState()

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
        deliveryViewModel.loadClientData()
    }

    LaunchedEffect(state.value.selectedPath) {
        deliveryViewModel.setIsDatePickerClickable(state.value.selectedPath != null)
        deliveryViewModel.setDateFieldText("")
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
                isConnectedToInternet = isConnected.value,
                setIsLoading = {
                    deliveryViewModel.setIsLoading(it)
                },
                setColumnScrollingEnabled = {
                    deliveryViewModel.setColumnScrollingEnabled(it)
                },
                onError = {
                    deliveryViewModel.setIsError("Une erreur est survenue lors du chargement de la carte.")
                }
            )

            CustomerContent(
                deliveryViewModel,
                navigateToCheckout,
                state,
                datePickerState,
                scrollState,
                onError = {
                    deliveryViewModel.setIsError("Erreur lors de la sauvegarde de vos informations")
                }
            )

            Spacer(modifier = Modifier.imePadding())
        }

        // Localisation permission
        if (state.value.shouldShowLocalisationPermission) {
            PermissionManagerComposable(
                allPaths = state.value.deliveryPaths,
                onUpdateUserCity = {
                    deliveryViewModel.updateUserCity(it)
                },
                onUpdateSelectedPath = {
                    deliveryViewModel.updateSelectedPath(it)
                },
                onUpdateUserIsOnPath = {
                    deliveryViewModel.updateUserLocationOnPath(it)
                },
                onUpdateUserCityLocation = {
                    deliveryViewModel.updateUserCityLocation(it)
                },
                onUpdateLocalisationState = {
                    deliveryViewModel.updateLocalisationState(it)
                },
                onUpdateShouldShowLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(it)
                },
                setIsLoading = {
                    deliveryViewModel.setIsLoading(it)
                }
            )
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
                navigateBack.invoke()
            }
        )

        if (state.value.datePickerVisibility) {
            DatePickerComposable(
                datePickerState = datePickerState.value,
                shouldRemoveDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(false)
                },
                newDateFieldText = {
                    deliveryViewModel.setDateFieldText(it)
                },
                onDateSelected = {
                    deliveryViewModel.saveSelectedDate(it)
                }
            )
        }

    }
}
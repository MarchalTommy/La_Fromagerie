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
import com.mtdevelopment.delivery.presentation.composable.DeliveryEligibility
import com.mtdevelopment.delivery.presentation.composable.MapBoxComposable
import com.mtdevelopment.delivery.presentation.composable.PermissionManagerComposable
import com.mtdevelopment.delivery.presentation.composable.getDatePickerState
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Main screen for customers to configure their delivery options.
 * It guides the user through identifying their location, matching it to a delivery path,
 * and selecting an available delivery date.
 * 
 * The screen features:
 * 1. An interactive MapBox map to visualize delivery areas and the user's location.
 * 2. A multi-step form (CustomerContent) for personal info and address.
 * 3. Automatic delivery path matching based on geographic proximity or manual selection.
 * 4. A specialized DatePicker constrained by the rules of the selected delivery path.
 */
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    navigateToCheckout: () -> Unit = {},
    navigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val isConnected = deliveryViewModel.isConnected.collectAsState()

    // UI state derived from ViewModel
    val state = remember(deliveryViewModel.deliveryUiDataState) {
        derivedStateOf {
            deliveryViewModel.deliveryUiDataState
        }
    }

    /**
     * The DatePicker state is sensitive to the selected delivery path (different paths have different schedules).
     */
    val datePickerState = remember(state.value.selectedPath) {
        derivedStateOf {
            getDatePickerState(state.value.selectedPath)
        }
    }
    val scrollState = rememberScrollState()

    // Initialize MapBox access token
    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
        deliveryViewModel.loadClientData()
    }

    // Reset date selection if the delivery path changes
    LaunchedEffect(state.value.selectedPath) {
        deliveryViewModel.setIsDatePickerClickable(state.value.selectedPath != null)
        deliveryViewModel.setDateFieldText("")
    }

    Surface(
        modifier = Modifier
            .imePadding()
            .fillMaxSize()
            .verticalScroll(
                state = scrollState,
                // Disable scrolling if an overlay (Error) is shown or if a child component is intercepting touch events
                enabled = state.value.columnScrollingEnabled && state.value.isError.isEmpty()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // MAP SECTION: Visualizes delivery zones
            MapBoxComposable(
                userLocation = state.value.userCityLocation,
                isConnectedToInternet = isConnected.value,
                setIsLoading = {
                    deliveryViewModel.setIsLoading(it)
                },
                setColumnScrollingEnabled = {
                    // Important: Disables parent scroll when user is panning/zooming the map
                    deliveryViewModel.setColumnScrollingEnabled(it)
                },
                onError = {
                    deliveryViewModel.setIsError("Une erreur est survenue lors du chargement de la carte.")
                }
            )

            // FORM SECTION: Collects user data and selections
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

        /**
         * PERMISSION & LOCALISATION SECTION:
         * This non-visual component handles the logic for requesting GPS permissions
         * and determining the user's delivery eligibility based on their coordinates.
         */
        if (state.value.shouldShowLocalisationPermission) {
            PermissionManagerComposable(
                allPaths = state.value.deliveryPaths,
                onUpdateEligibility = { eligibility, city, userAddress, path ->
                    // Logic to handle location-based delivery matching
                    if (city != null) {
                        deliveryViewModel.updateUserCity(city)
                    }
                    if (userAddress != null) {
                        deliveryViewModel.setAddressFieldText(userAddress)
                    }
                    deliveryViewModel.updateSelectedPath(path)
                    deliveryViewModel.updateUserLocationOnPath(eligibility == DeliveryEligibility.DELIVERABLE)
                    deliveryViewModel.updateUserLocationCloseFromPath(eligibility == DeliveryEligibility.ASK_FOR_SUPPORT)
                },
                onUpdateUserLocation = {
                    if (it != null) {
                        deliveryViewModel.updateUserCityLocation(it)
                    }
                },
                onUpdateLocalisationState = {
                    deliveryViewModel.updateLocalisationState(it)
                },
                onUpdateShouldShowLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(it)
                },
                setIsLoading = {
                    deliveryViewModel.setIsLoading(it)
                },
            )
        }

        // OVERLAY: Global loading state (Rive animation)
        RiveAnimation(
            isLoading = state.value.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )

        // OVERLAY: Error handling
        ErrorOverlay(
            isShown = state.value.isError.isNotBlank(),
            message = state.value.isError.ifBlank { "Une erreur inconnue est survenue.\nSi le problème persiste merci de nous contacter !" },
            onDismiss = {
                deliveryViewModel.setIsError("")
                navigateBack.invoke()
            }
        )

        // DIALOG: Date selection
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
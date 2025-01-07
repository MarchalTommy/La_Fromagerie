package com.mtdevelopment.checkout.presentation.screen

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
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.composable.AdminContent
import com.mtdevelopment.checkout.presentation.composable.CustomerContent
import com.mtdevelopment.checkout.presentation.composable.DatePickerComposable
import com.mtdevelopment.checkout.presentation.composable.DeliveryPathPickerComposable
import com.mtdevelopment.checkout.presentation.composable.MapBoxComposable
import com.mtdevelopment.checkout.presentation.composable.PermissionManagerComposable
import com.mtdevelopment.checkout.presentation.composable.getDatePickerState
import com.mtdevelopment.checkout.presentation.model.toAdminUiDeliveryPath
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.presentation.util.VARIANT
import org.koin.androidx.compose.koinViewModel
import com.mtdevelopment.admin.presentation.composable.PathEditDialog
import androidx.compose.runtime.mutableStateOf
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    mainViewModel: MainViewModel,
    navigateToCheckout: () -> Unit = {}
) {

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val context = LocalContext.current

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

    val isConnected = deliveryViewModel.isConnected.collectAsState()

    val scrollState = rememberScrollState()

    val showEditDialog = remember { mutableStateOf(false) }
    val selectedPath = remember { mutableStateOf<AdminUiDeliveryPath?>(null) }

    if (MapboxOptions.accessToken != MAPBOX_PUBLIC_TOKEN) {
        MapboxOptions.accessToken = MAPBOX_PUBLIC_TOKEN
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    LaunchedEffect(state.value.selectedPath) {
        deliveryViewModel.setIsDatePickerClickable(state.value.selectedPath != null)
        deliveryViewModel.setDateFieldText("")
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState, enabled = state.value.columnScrollingEnabled)
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
                    deliveryViewModel.setIsLoading(it)
                },
                setColumnScrollingEnabled = {
                    deliveryViewModel.setColumnScrollingEnabled(it)
                },
                onError = {
                    mainViewModel.setError(it)
                }
            )

            if (VARIANT != "admin") {
                CustomerContent(
                    deliveryViewModel,
                    mainViewModel,
                    navigateToCheckout,
                    state,
                    datePickerState,
                    scrollState
                )
            } else {
                if (state.value.deliveryPaths.isNotEmpty()) {
                    AdminContent(
                        pathList = state.value.deliveryPaths.map { it.toAdminUiDeliveryPath() },
                        onPathSelected = { path ->
                            selectedPath.value = path
                            showEditDialog.value = true
                        }
                    )
                }
            }

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

        if (state.value.showDeliveryPathPicker) {
            DeliveryPathPickerComposable(
                allPaths = state.value.deliveryPaths,
                selectedPath = state.value.selectedPath,
                onPathSelected = {
                    deliveryViewModel.updateSelectedPath(it)
                },
                onDismiss = {
                    deliveryViewModel.updateShowDeliveryPathPicker(false)
                })
        }

        if (showEditDialog.value) {
            PathEditDialog(
                path = selectedPath.value,
                onValidate = {
                    showEditDialog.value = false
                },
                onDelete = {
                    showEditDialog.value = false
                },
                onDismiss = {
                    showEditDialog.value = false
                },
                onError = {
                    mainViewModel.setError(it)
                }
            )
        }
    }
}
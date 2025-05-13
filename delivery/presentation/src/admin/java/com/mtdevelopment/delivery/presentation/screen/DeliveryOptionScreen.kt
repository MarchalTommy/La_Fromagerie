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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.admin.presentation.model.toDomainDeliveryPath
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.delivery.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.delivery.presentation.composable.AdminContent
import com.mtdevelopment.delivery.presentation.composable.DatePickerComposable
import com.mtdevelopment.delivery.presentation.composable.MapBoxComposable
import com.mtdevelopment.delivery.presentation.composable.PathEditDialog
import com.mtdevelopment.delivery.presentation.composable.getDatePickerState
import com.mtdevelopment.delivery.presentation.model.toAdminUiDeliveryPath
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    navigateToCheckout: () -> Unit = {},
    navigateBack: () -> Unit = {}
) {

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val adminViewModel = koinViewModel<AdminViewModel>()

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
        deliveryViewModel.loadAdminData()
        Rive.init(context)
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

            if (state.value.deliveryPaths.isNotEmpty()) {
                AdminContent(
                    pathList = state.value.deliveryPaths.map { it.toAdminUiDeliveryPath() },
                    onPathSelected = { path ->
                        selectedPath.value = path
                        showEditDialog.value = true
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

        if (showEditDialog.value) {
            PathEditDialog(
                path = selectedPath.value,
                searchQuery = state.value.addressSearchQuery,
                onValidate = { newPath ->
                    if (state.value.deliveryPaths.any { it.id == newPath.id }) {
                        deliveryViewModel.setIsLoading(true)
                        adminViewModel.updateDeliveryPath(
                            newPath.toDomainDeliveryPath(),
                            onSuccess = {
                                deliveryViewModel.loadAdminData(true)
                            },
                            onFailure = {
                                deliveryViewModel.setIsError("Une erreur est survenue lors de la mise à jour du chemin.")
                                deliveryViewModel.setIsLoading(false)
                            })
                    } else {
                        adminViewModel.addNewDeliveryPath(
                            newPath.toDomainDeliveryPath(),
                            onSuccess = {
                                deliveryViewModel.loadAdminData(true)
                            },
                            onFailure = {
                                deliveryViewModel.setIsError("Une erreur est survenue lors de l'ajout du chemin.")
                                deliveryViewModel.setIsLoading(false)
                            })
                    }
                    showEditDialog.value = false
                },
                onDelete = { newPath ->
                    adminViewModel.deleteDeliveryPath(
                        newPath.toDomainDeliveryPath(),
                        onSuccess = {
                            deliveryViewModel.loadAdminData(true)
                        },
                        onFailure = {
                            deliveryViewModel.setIsError("Une erreur est survenue lors de la suppression du chemin.")
                            deliveryViewModel.setIsLoading(false)
                        })
                    showEditDialog.value = false
                },
                onDismiss = {
                    showEditDialog.value = false
                },
                onError = {
                    deliveryViewModel.setIsError(it)
                },
                autoCompleteSuggestion = state.value.addressSuggestions,
                showAutocompleteDropdown = state.value.showAddressSuggestions,
                onAutoCompleteDropdownDismiss = {
                    deliveryViewModel.setShowAddressesSuggestions(false)
                },
                onAutocompleteQueryChange = {
                    deliveryViewModel.setAddressFieldText(it)
                },
                parentScrollState = scrollState
            )
        }
    }
}
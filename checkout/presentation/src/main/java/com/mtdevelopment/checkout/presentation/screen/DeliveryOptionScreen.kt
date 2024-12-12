package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.composable.DatePickerComposable
import com.mtdevelopment.checkout.presentation.composable.DateTextField
import com.mtdevelopment.checkout.presentation.composable.DeliveryPathPickerComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTextComposable
import com.mtdevelopment.checkout.presentation.composable.LocalisationTypePicker
import com.mtdevelopment.checkout.presentation.composable.MapBoxComposable
import com.mtdevelopment.checkout.presentation.composable.PermissionManagerComposable
import com.mtdevelopment.checkout.presentation.composable.UserInfoComposable
import com.mtdevelopment.checkout.presentation.composable.getDatePickerState
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

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

    val isButtonEnabled = remember(
        state.value.userNameFieldText,
        state.value.userAddressFieldText,
        state.value.dateFieldText
    ) {
        state.value.userNameFieldText.isNotBlank()
                && state.value.userAddressFieldText.isNotBlank()
                && state.value.dateFieldText.isNotBlank()
    }

    val datePickerState = remember(state.value.selectedPath) {
        derivedStateOf {
            getDatePickerState(state.value.selectedPath)
        }
    }

    val isConnected = deliveryViewModel.isConnected.collectAsState()

    val scrollState = rememberScrollState()
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

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

            // Localisation Type Picker
            LocalisationTypePicker(
                showDeliverySelection = {
                    deliveryViewModel.updateShowDeliveryPathPicker(true)
                },
                selectedPath = state.value.selectedPath,
                localisationSuccess = state.value.localisationSuccess,
                shouldAskLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(true)
                }
            )

            if (state.value.localisationSuccess || state.value.selectedPath != null || state.value.userLocationOnPath) {
                LocalisationTextComposable(
                    selectedPath = state.value.selectedPath,
                    geolocIsOnPath = state.value.userLocationOnPath,
                    userCity = state.value.userCity
                )
            }

            DateTextField(
                shouldBeClickable = state.value.shouldDatePickerBeClickable,
                dateFieldText = state.value.dateFieldText,
                datePickerState = datePickerState.value,
                shouldShowDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(true)
                },
                newDateFieldText = {
                    deliveryViewModel.setDateFieldText(it)
                }
            )

            UserInfoComposable(
                fieldText = state.value.userNameFieldText,
                label = "Nom complet",
                imeAction = ImeAction.Next,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = {
                    deliveryViewModel.setUserNameFieldText(it)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Person, "")
                },
                onFocusChange = {
                    if (it) {
                        scope.launch {
                            delay(500)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            )

            UserInfoComposable(
                fieldText = state.value.userAddressFieldText,
                label = "Adresse exacte",
                imeAction = ImeAction.Done,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = {
                    deliveryViewModel.setUserAddressFieldText(it)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Place, "")
                },
                onFocusChange = {
                    if (it) {
                        scope.launch {
                            delay(500)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            )

            if (!isButtonEnabled) {
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    text = "Veuillez remplir les champs ci-dessus pour pouvoir aller plus loin.",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Button(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                contentPadding = PaddingValues(16.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isButtonEnabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.inverseSurface
                    }
                ),
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                shape = RoundedCornerShape(8.dp),
                enabled = isButtonEnabled,
                onClick = {
                    deliveryViewModel.saveUserInfo(onError = {
                        mainViewModel.setError("Erreur lors de la sauvegarde des informations")
                    })
                    navigateToCheckout.invoke()
                }) {
                Text("Valider et passer au paiement")
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
    }
}
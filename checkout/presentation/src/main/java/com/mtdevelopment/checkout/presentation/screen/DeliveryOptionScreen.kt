package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionScreen(
    mainViewModel: MainViewModel,
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToCheckout: () -> Unit = {}
) {

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val context = LocalContext.current

    val state = deliveryViewModel.deliveryUiDataState
    val datePickerState = getDatePickerState(state.selectedPath)
    val isButtonEnabled = state.userAddressFieldText.isNotBlank() &&
            state.userNameFieldText.isNotBlank() &&
            state.dateFieldText.isNotBlank()
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

    LaunchedEffect(state.selectedPath) {
        deliveryViewModel.setIsDatePickerClickable(state.selectedPath != null)
        deliveryViewModel.setDateFieldText("")
    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(state = scrollState, enabled = state.columnScrollingEnabled)
            .imePadding()
    ) {

        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            // Map Card
            MapBoxComposable(
                userLocation = state.userCityLocation,
                chosenPath = state.selectedPath,
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
                selectedPath = state.selectedPath,
                localisationSuccess = state.localisationSuccess,
                shouldAskLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(true)
                }
            )

            if (state.localisationSuccess || state.selectedPath != null || state.userLocationOnPath) {
                LocalisationTextComposable(
                    selectedPath = state.selectedPath,
                    geolocIsOnPath = state.userLocationOnPath,
                    userCity = state.userCity
                )
            }

            DateTextField(
                shouldBeClickable = state.shouldDatePickerBeClickable,
                dateFieldText = state.dateFieldText,
                datePickerState = datePickerState,
                shouldShowDatePicker = {
                    deliveryViewModel.setIsDatePickerShown(true)
                },
                newDateFieldText = {
                    deliveryViewModel.setDateFieldText(it)
                }
            )

            UserInfoComposable(
                fieldText = state.userNameFieldText,
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
                            delay(300)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            )

            UserInfoComposable(
                fieldText = state.userAddressFieldText,
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
                            delay(300)
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                }
            )

            if (!isButtonEnabled) {
                Text(
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
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
                        // TODO: Manage error
                    })
                    navigateToCheckout.invoke()
                }) {
                Text("Valider et passer au paiement")
            }
        }

        // Localisation permission
        if (state.shouldShowLocalisationPermission) {
            PermissionManagerComposable(
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
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )

        if (state.datePickerVisibility) {
            DatePickerComposable(
                datePickerState = datePickerState,
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

        if (state.showDeliveryPathPicker) {
            DeliveryPathPickerComposable(
                selectedPath = state.selectedPath,
                onPathSelected = {
                    deliveryViewModel.updateSelectedPath(it)
                },
                onDismiss = {
                    deliveryViewModel.updateShowDeliveryPathPicker(false)
                })
        }
    }
}
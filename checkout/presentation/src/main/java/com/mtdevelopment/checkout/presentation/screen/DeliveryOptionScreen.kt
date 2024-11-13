package com.mtdevelopment.checkout.presentation.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import com.mapbox.common.MapboxOptions
import com.mtdevelopment.checkout.presentation.BuildConfig.MAPBOX_PUBLIC_TOKEN
import com.mtdevelopment.checkout.presentation.R
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
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import org.koin.androidx.compose.koinViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DeliveryOptionScreen(
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToCheckout: () -> Unit = {}
) {

    // TODO: FIX LOADER ->
    // Make it appear ONLY if it has time to show for 200ms. Else do not show, to avoid flicker

    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val context = LocalContext.current

    val state = deliveryViewModel.deliveryUiDataState
    val datePickerState = getDatePickerState(state.selectedPath)
    val isButtonEnabled = state.userAddressFieldText.isNotBlank() &&
            state.userNameFieldText.isNotBlank() &&
            state.dateFieldText.isNotBlank()

    val scrollState = rememberScrollState()

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
    ) {

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            // Map Card
            MapBoxComposable(
                userLocation = state.userCityLocation,
                chosenPath = state.selectedPath,
                setIsLoading = {
                    deliveryViewModel.setIsLoading(it)
                },
                setColumnScrollingEnabled = {
                    deliveryViewModel.setColumnScrollingEnabled(it)
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
                state.userNameFieldText,
                "Nom complet",
                updateText = {
                    deliveryViewModel.setUserNameFieldText(it)
                }
            ) {
                Icon(Icons.Rounded.Person, "")
            }

            UserInfoComposable(
                state.userAddressFieldText,
                "Adresse exacte",
                updateText = {
                    deliveryViewModel.setUserAddressFieldText(it)
                }
            ) {
                Icon(Icons.Rounded.Place, "")
            }

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
        if (state.isLoading) {
            RiveAnimation(
                modifier = Modifier.fillMaxSize(),
                resId = R.raw.goat_loading,
                contentDescription = "Loading animation"
            )
        }

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
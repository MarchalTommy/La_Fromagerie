package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerContent(
    deliveryViewModel: DeliveryViewModel,
    navigateToCheckout: () -> Unit,
    state: State<DeliveryUiDataState>,
    datePickerState: State<DatePickerState>,
    scrollState: ScrollState,
    onError: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        val isButtonEnabled = remember(
            state.value.userNameFieldText,
            state.value.addressSearchQuery,
            state.value.dateFieldText
        ) {
            state.value.userNameFieldText.isNotBlank()
                    && state.value.addressSearchQuery.isNotBlank()
                    && state.value.dateFieldText.isNotBlank()
        }

        val focusRequester = remember {
            FocusRequester()
        }

        val focusManager = LocalFocusManager.current
        val scope = rememberCoroutineScope()

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        AddressAutocompleteTextField(
            searchQuery = state.value.addressSearchQuery,
            suggestions = state.value.addressSuggestions,
            isLoading = state.value.addressSuggestionsLoading,
            showDropdown = state.value.showAddressSuggestions,
            focusRequester = focusRequester,
            focusManager = focusManager,
            onDropDownDismiss = {
                deliveryViewModel.setShowAddressesSuggestions(false)
            },
            onSuggestionSelected = {
                deliveryViewModel.onSuggestionSelected(it)
            },
            onValueChange = {
                deliveryViewModel.setAddressFieldText(it)
            },
            onFocusChange = {
                if (it) {
                    scope.launch {
                        delay(500)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.value.localisationSuccess || state.value.selectedPath != null || state.value.userLocationOnPath) {
            LocalisationTextComposable(
                selectedPath = state.value.selectedPath,
                geolocIsOnPath = state.value.userLocationOnPath,
                userCity = state.value.userCity
            )
        } else {
            LocalisationTypePicker(
                selectedPath = state.value.selectedPath,
                localisationSuccess = state.value.localisationSuccess,
                shouldAskLocalisationPermission = {
                    deliveryViewModel.updateShouldShowLocalisationPermission(true)
                }
            )
        }

        AnimatedVisibility(
            visible = state.value.selectedPath != null
        ) {
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
        }

        if (!isButtonEnabled) {
            Text(
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                text = "Veuillez remplir les champs ci-dessus pour continuer.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        Button(
            modifier = Modifier
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
                    onError.invoke("Erreur lors de la sauvegarde des informations")
                })
                navigateToCheckout.invoke()
            }) {
            Text("Valider et passer au paiement")
        }
    }
}
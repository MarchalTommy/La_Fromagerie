package com.mtdevelopment.delivery.presentation.composable

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.presentation.composable.AddressAutocompleteTextField
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.delivery.domain.usecase.DeliveryEligibility
import com.mtdevelopment.delivery.domain.usecase.DetermineDeliveryEligibilityUseCase
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import com.mtdevelopment.delivery.presentation.model.toDomainDeliveryPath
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerContent(
    deliveryViewModel: DeliveryViewModel,
    onContinue: () -> Unit,
    state: State<DeliveryUiDataState>,
    scrollState: ScrollState,
    onError: (String) -> Unit
) {
    val isButtonEnabled = remember(
        state.value.userNameFieldText,
        state.value.deliveryAddressSearchQuery
    ) {
        state.value.userNameFieldText.isNotBlank()
                && state.value.deliveryAddressSearchQuery.isNotBlank()
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasPerformedInitialCheck by remember { mutableStateOf(false) }

    LaunchedEffect(state.value.deliveryAddressSearchQuery, state.value.deliveryPaths) {
        if (!hasPerformedInitialCheck && state.value.deliveryAddressSearchQuery.isNotEmpty() && state.value.deliveryPaths.isNotEmpty()) {
            hasPerformedInitialCheck = true
            checkLocationEligibility(
                context = context,
                address = state.value.deliveryAddressSearchQuery,
                location = null,
                allPaths = state.value.deliveryPaths,
                onResult = { eligibility, city, userLocation, selectedPath ->
                    if (city != null) {
                        deliveryViewModel.updateUserCity(city)
                    }
                    if (userLocation != null) {
                        deliveryViewModel.updateUserCityLocation(userLocation)
                    }
                    deliveryViewModel.updateSelectedPath(selectedPath)
                    deliveryViewModel.updateEligibility(eligibility)
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // The lower "form" section lives on its own harmonizing surface so it reads as a
    // sheet tucked under the (now shorter) map card, mirroring the pre-payment screen.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .focusable(true),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {

            val focusRequester = remember {
                FocusRequester()
            }

            val focusManager = LocalFocusManager.current

            // Tightened gap between the map card above and the first form field.
            Spacer(modifier = Modifier.height(12.dp))

            if (state.value.deliveryAddressSearchQuery.isBlank() || state.value.userNameFieldText.isBlank()) {
                Text(
                    modifier = Modifier
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    text = "Veuillez remplir les champs ci-dessous pour continuer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            UserInfoComposable(
                fieldText = state.value.userNameFieldText,
                label = "Nom et prénom",
                imeAction = ImeAction.Next,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = {
                    deliveryViewModel.setUserNameFieldText(it)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Person, "")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.value.isBillingDifferent,
                    onCheckedChange = {
                        deliveryViewModel.setIsBillingDifferent(it)
                    }
                )
                Text(
                    text = "Adresse de facturation différente de l'adresse de livraison",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }


            AddressAutocompleteTextField(
                label = "Adresse de livraison",
                searchQuery = state.value.deliveryAddressSearchQuery,
                suggestions = state.value.deliveryAddressSuggestions,
                isLoading = state.value.addressSuggestionsLoading,
                showDropdown = state.value.showAddressSuggestions,
                focusRequester = focusRequester,
                focusManager = focusManager,
                onDropDownDismiss = {
                    deliveryViewModel.setShowAddressesSuggestions(false, isBilling = false)
                },
                onValueChange = {
                    deliveryViewModel.setAddressFieldText(it)
                },
                onAddressValidated = { string, suggestion ->
                    if (suggestion != null) {
                        deliveryViewModel.onSuggestionSelected(suggestion)
                    } else {
                        deliveryViewModel.setAddressFieldText(string)
                    }

                    coroutineScope.launch {
                        checkLocationEligibility(
                            context = context,
                            address = string,
                            location = suggestion,
                            allPaths = state.value.deliveryPaths,
                            onResult = { eligibility, city, userLocation, selectedPath ->
                                if (city != null) {
                                    deliveryViewModel.updateUserCity(city)
                                }
                                if (userLocation != null) {
                                    deliveryViewModel.updateUserCityLocation(userLocation)
                                }
                                deliveryViewModel.updateSelectedPath(selectedPath)
                                deliveryViewModel.updateUserLocationOnPath(eligibility == DeliveryEligibility.DELIVERABLE)
                                deliveryViewModel.updateUserLocationCloseFromPath(eligibility == DeliveryEligibility.ASK_FOR_SUPPORT)
                            }
                        )
                    }
                },
                onClick = {
                    deliveryViewModel.startAutocomplete(isBilling = false)
                }
            )

            if (state.value.isBillingDifferent) {
                Spacer(modifier = Modifier.height(8.dp))
                AddressAutocompleteTextField(
                    label = "Adresse de facturation",
                    searchQuery = state.value.billingAddressSearchQuery,
                    suggestions = state.value.billingAddressSuggestions,
                    isLoading = state.value.addressSuggestionsLoading,
                    showDropdown = state.value.showBillingAddressSuggestions,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    onDropDownDismiss = {
                        deliveryViewModel.setShowAddressesSuggestions(false, isBilling = true)
                    },
                    onValueChange = {
                        deliveryViewModel.setAddressFieldText(it, isBilling = true)
                    },
                    onAddressValidated = { string, suggestion ->
                        if (suggestion != null) {
                            deliveryViewModel.onSuggestionSelected(suggestion, isBilling = true)
                        } else {
                            deliveryViewModel.setAddressFieldText(string, isBilling = true)
                        }
                    },
                    onClick = {
                        deliveryViewModel.startAutocomplete(isBilling = true)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.value.localisationSuccess || state.value.selectedPath != null || state.value.userLocationOnPath || state.value.deliveryAddressSearchQuery != "") {
                LocalisationTextComposable(
                    selectedPath = state.value.selectedPath,
                    geolocIsOnPath = state.value.userLocationOnPath && state.value.localisationSuccess,
                    canAskForDelivery = state.value.userLocationCloseFromPath,
                    streetNotCovered = state.value.streetNotCovered,
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

            ErrorOverlay(
                isShown = state.value.isError.isNotBlank(),
                duration = 3000L,
                message = state.value.isError,
            )
        }
    }

    when {
        state.value.userLocationCloseFromPath -> {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                text = if (state.value.streetNotCovered) {
                    "Demander mon jour de livraison"
                } else {
                    "Demander une prise en charge"
                },
                trailingIcon = null,
                onClick = {
                    // A split city needs the customer's street to tell the two tournées apart, so
                    // the email asks for exactly that rather than for the city to be added.
                    val emailIntent =
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:".toUri()
                            putExtra(
                                Intent.EXTRA_EMAIL,
                                arrayOf("marchal.gilles25560@gmail.com")
                            )
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                if (state.value.streetNotCovered) {
                                    "Demande de jour de livraison"
                                } else {
                                    "Demande d'ajout aux livraisons"
                                }
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                if (state.value.streetNotCovered) {
                                    "Bonjour Mr. Marchal.\n\nJ'habite à ${state.value.userCity}, " +
                                            "à l'adresse suivante :\n${state.value.deliveryAddressSearchQuery}" +
                                            "\n\nL'application n'a pas réussi à déterminer ma tournée. " +
                                            "Pourriez-vous me dire quel jour vous passez dans ma rue ?" +
                                            "\n\nMerci d'avance !"
                                } else {
                                    "Bonjour Mr. Marchal.\n\nJ'habite à une " +
                                            "adresse proche d'un de vos points de livraison et j'aurais aimé être livré aussi. " +
                                            "\nEst-ce possible pour vous d'ajouter ${state.value.userCity} à une de vos livraison ?" +
                                            "\n\nMerci d'avance !"
                                }
                            )
                        }

                    try {
                        context.startActivity(emailIntent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        deliveryViewModel.setIsError("Aucune application de messagerie n'a été trouvée.")
                    }
                }
            )
        }


        state.value.selectedPath != null -> {
            // Step 1 → Step 2: "Continuer" opens the delivery-date calendar.
            // Persisting the date and navigating to the payment screen happens once
            // the customer confirms a date (handled by the screen's date dialog).
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                text = "Continuer",
                enabled = isButtonEnabled,
                onClick = {
                    onContinue.invoke()
                }
            )
        }

        else -> {}
    }
}

/**
 * Resolves the customer's typed (or autocompleted) address into a city, street and coordinates,
 * then hands the decision to [DetermineDeliveryEligibilityUseCase].
 *
 * Only the geocoding lives here; the path-matching rules are in the use case, shared with the GPS
 * flow in `PermissionManagerComposable` and covered by unit tests.
 */
private suspend fun checkLocationEligibility(
    context: Context,
    address: String? = null,
    location: AutoCompleteSuggestion? = null,
    allPaths: List<UiDeliveryPath>,
    onResult: (eligibility: DeliveryEligibility, city: String?, userLocation: Pair<Double, Double>?, selectedPath: UiDeliveryPath?) -> Unit
) {
    withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context)
        var userCity: String? = null
        var userLocation: Pair<Double, Double>? = null
        var userStreet: String? = null

        // 1. Resolve user city and location
        if (address != null && location == null) {
            // Manual Address Input
            try {
                val addresses: List<Address>? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCoroutine { continuation ->
                            geocoder.getFromLocationName(
                                address,
                                1
                            ) { addressList ->
                                continuation.resume(addressList)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(address, 1)
                    }
                userCity = addresses?.firstOrNull()?.locality
                userLocation = addresses?.firstOrNull()?.let {
                    Pair(it.latitude, it.longitude)
                }
                userStreet = addresses?.firstOrNull()?.thoroughfare
            } catch (e: IOException) {
                userCity = null
            } catch (e: IllegalArgumentException) {
                userCity = null
            }

            // Fallback for manually typed city name extraction if geocoding failed
            if (userCity.isNullOrBlank()) {
                userCity = extractCityFromAddress(address)
            }
        } else {
            // Autocomplete Suggestion Selected
            userCity = location?.city
            val fullText = location?.fulltext
            if (userCity.isNullOrBlank() && fullText != null) {
                userCity = extractCityFromAddress(fullText)
            }
            if (location?.lat != null && location.long != null) {
                userLocation = Pair(location.lat!!, location.long!!)
            }
        }

        // 2. Match against the delivery paths
        val addressText = address ?: location?.fulltext
        val result = DetermineDeliveryEligibilityUseCase().invoke(
            paths = allPaths.map { it.toDomainDeliveryPath() },
            userCity = userCity,
            userStreet = userStreet,
            addressText = addressText,
            userLocation = userLocation
        )

        // Return results to UI
        withContext(Dispatchers.Main) {
            onResult(
                result.eligibility,
                result.resolvedCity,
                result.resolvedLocation,
                result.matchingPath?.let { matched -> allPaths.find { it.id == matched.id } }
            )
        }
    }
}

private fun extractCityFromAddress(address: String): String? {
    // Match a 5-digit postal code and capture everything following it
    val regex = "\\b(\\d{5})\\b\\s+([^,]+)".toRegex()
    val matchResult = regex.find(address) ?: return null
    val cityPart = matchResult.groupValues[2].trim()

    // Remove country suffixes like ", France"
    var city = cityPart.split(",").first().trim()
    if (city.endsWith(" France", ignoreCase = true)) {
        city = city.substring(0, city.length - 7).trim()
    } else if (city.equals("France", ignoreCase = true)) {
        return null
    }
    return if (city.isNotEmpty()) city else null
}


package com.mtdevelopment.delivery.presentation.composable

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.mtdevelopment.core.domain.calculateDistance
import com.mtdevelopment.core.domain.isSameCity
import com.mtdevelopment.core.domain.normalizeCityName
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.presentation.composable.AddressAutocompleteTextField
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
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
    navigateToCheckout: () -> Unit,
    state: State<DeliveryUiDataState>,
    datePickerState: State<DatePickerState>,
    scrollState: ScrollState,
    onError: (String) -> Unit
) {

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
                    deliveryViewModel.updateUserLocationOnPath(eligibility == DeliveryEligibility.DELIVERABLE)
                    deliveryViewModel.updateUserLocationCloseFromPath(eligibility == DeliveryEligibility.ASK_FOR_SUPPORT)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        val isButtonEnabled = remember(
            state.value.userNameFieldText,
            state.value.deliveryAddressSearchQuery,
            state.value.dateFieldText
        ) {
            state.value.userNameFieldText.isNotBlank()
                    && state.value.deliveryAddressSearchQuery.isNotBlank()
                    && state.value.dateFieldText.isNotBlank()
        }

        val focusRequester = remember {
            FocusRequester()
        }

        val focusManager = LocalFocusManager.current
        val scope = rememberCoroutineScope()

        Spacer(modifier = Modifier.height(32.dp))

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

        when {
            state.value.userLocationCloseFromPath -> {
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    contentPadding = PaddingValues(16.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        MaterialTheme.colorScheme.secondary
                    ),
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        val emailIntent =
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(
                                    Intent.EXTRA_EMAIL,
                                    arrayOf("marchal.gilles25560@gmail.com")
                                )
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Demande d'ajout aux livraisons"
                                )
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Bonjour Mr. Marchal.\n\nJ'habite à une " +
                                            "adresse proche d'un de vos points de livraison et j'aurais aimé être livré aussi. " +
                                            "\nEst-ce possible pour vous d'ajouter ${state.value.userCity} à une de vos livraison ?" +
                                            "\n\nMerci d'avance !"
                                )
                            }

                        try {
                            context.startActivity(emailIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            deliveryViewModel.setIsError("Aucune application de messagerie n'a été trouvée.")
                        }
                    }
                ) {
                    Text("Demander une prise en charge")
                }
            }


            state.value.selectedPath != null -> {
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
                    }
                ) {
                    Text("Valider et passer au paiement")
                }
            }

            else -> {}
        }

        ErrorOverlay(
            isShown = state.value.isError.isNotBlank(),
            duration = 3000L,
            message = state.value.isError,
        )

    }
}

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
        val isNearPathCity: Boolean
        var closestDistance = Double.MAX_VALUE
        var matchingPathForCity: UiDeliveryPath? = null

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

        // 2. Filter paths that cover the user's city (pathsInCity)
        val pathsInCity = mutableListOf<UiDeliveryPath>()
        val addressText = address ?: location?.fulltext

        for (path in allPaths) {
            var matchedCityName: String? = null
            var matchedCityLocation: Pair<Double, Double>? = null

            for (cityInfo in path.cities) {
                val cityName = cityInfo.first
                val cityIndex = path.cities.indexOf(cityInfo)
                val cityLocation = if (path.locations != null && path.locations.size > cityIndex) {
                    path.locations[cityIndex]
                } else null

                // Proximity calculations (only if we have resolved user location)
                if (userLocation != null && cityLocation != null) {
                    val distance = calculateDistance(
                        userLocation.first,
                        userLocation.second,
                        cityLocation.first,
                        cityLocation.second
                    )
                    if (distance < closestDistance) {
                        closestDistance = distance.toDouble()
                    }
                }

                // Check city match
                val isCityMatch =
                    isSameCity(userCity, cityName) || isAddressInCity(addressText, cityName)
                if (isCityMatch) {
                    matchedCityName = cityName
                    matchedCityLocation = cityLocation
                }
            }

            if (matchedCityName != null) {
                if (!pathsInCity.contains(path)) {
                    pathsInCity.add(path)
                }
                // Fallback / recovered coordinates and city name:
                // If user location or userCity was null/failed, recover them using the matched city's info from the path
                if (userCity.isNullOrBlank()) {
                    userCity = matchedCityName
                }
                if (userLocation == null && matchedCityLocation != null) {
                    userLocation = matchedCityLocation
                    closestDistance =
                        0.0 // Force proximity check to succeed since exact city matched
                }
            }
        }

        // 3. Granular street-level matching
        if (pathsInCity.isNotEmpty()) {
            // Step 1: Try exact street match using thoroughfare from geocoder
            if (userStreet != null) {
                matchingPathForCity = pathsInCity.find { path ->
                    path.streets.any { it.equals(userStreet, ignoreCase = true) }
                }
            }

            // Step 2: Try robust fallback street check in the full address text
            if (matchingPathForCity == null && addressText != null) {
                matchingPathForCity = pathsInCity.find { path ->
                    path.streets.any { street ->
                        isStreetInAddress(addressText, street)
                    }
                }
            }

            // Step 3: Fall back to generic paths covering the whole city (no street restrictions)
            if (matchingPathForCity == null) {
                matchingPathForCity = pathsInCity.find { it.streets.isEmpty() }
            }
        }

        isNearPathCity = closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS

        // 4. Determine eligibility
        val eligibility = when {
            matchingPathForCity != null -> DeliveryEligibility.DELIVERABLE
            isNearPathCity -> DeliveryEligibility.ASK_FOR_SUPPORT
            else -> DeliveryEligibility.NOT_ELIGIBLE
        }

        // Return results to UI
        withContext(Dispatchers.Main) {
            onResult(
                eligibility,
                userCity,
                userLocation,
                if (eligibility == DeliveryEligibility.DELIVERABLE) matchingPathForCity else null
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

private fun isAddressInCity(address: String?, cityName: String?): Boolean {
    if (address == null || cityName == null) return false
    val normalizedAddress = address.normalizeCityName()
    val normalizedCity = cityName.normalizeCityName()
    if (normalizedAddress.isEmpty() || normalizedCity.isEmpty()) return false
    val regex = "\\b${Regex.escape(normalizedCity)}\\b".toRegex()
    return regex.containsMatchIn(normalizedAddress)
}

private fun isStreetInAddress(address: String?, streetName: String): Boolean {
    if (address == null) return false
    val normalizedAddress = address.normalizeCityName()
    val normalizedStreet = streetName.normalizeCityName()
    if (normalizedAddress.isEmpty() || normalizedStreet.isEmpty()) return false
    return normalizedAddress.contains(normalizedStreet)
}

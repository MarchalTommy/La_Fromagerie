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
import androidx.core.net.toUri
import com.mtdevelopment.core.domain.calculateDistance
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion
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

    LaunchedEffect(state.value.deliveryAddressSearchQuery == "" || state.value.deliveryPaths.isNotEmpty()) {
        if (state.value.deliveryAddressSearchQuery != "") {
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
        val isNearPathCity: Boolean
        var closestDistance = Double.MAX_VALUE
        var matchingPathForCity: UiDeliveryPath? = null

        if (address != null && location == null) {
            // 1. Géocodage pour obtenir le nom de la ville
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
            } catch (e: IOException) {
                userCity = null // ou "Erreur Geocoder"
            } catch (e: IllegalArgumentException) {
                userCity = null // ou "Erreur Coordonnées"
            }
        } else {
            userCity = location?.city
            if (location?.lat != null && location.long != null) {
                userLocation = Pair(location.lat!!, location.long!!)
            }
        }

        // 2. Vérifier la proximité et si la ville est dans un parcours
        for (path in allPaths) {
            for (cityInfo in path.cities) {
                val cityName = cityInfo.first
                val cityLocation = path.locations!![path.cities.indexOf(cityInfo)]

                // Calculer la distance
                val distance = calculateDistance(
                    userLocation?.first ?: 0.0,
                    userLocation?.second ?: 0.0,
                    cityLocation.first,
                    cityLocation.second
                )

                if (distance < closestDistance) {
                    closestDistance = distance.toDouble()
                }

                // Vérifier si la ville géocodée correspond à une ville du parcours
                if (userCity != null && userCity.equals(cityName, ignoreCase = true)) {
                    matchingPathForCity = path
                }
            }
        }

        isNearPathCity = closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS

        // 3. Déterminer l'éligibilité
        val eligibility = when {
            matchingPathForCity != null -> DeliveryEligibility.DELIVERABLE // Priorité si livrable
            isNearPathCity -> DeliveryEligibility.ASK_FOR_SUPPORT // Ensuite si trop loin
            else -> DeliveryEligibility.NOT_ELIGIBLE // Ensuite si vraiment trop loin
        }

        // Retourner le résultat sur le thread principal si nécessaire pour l'UI
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

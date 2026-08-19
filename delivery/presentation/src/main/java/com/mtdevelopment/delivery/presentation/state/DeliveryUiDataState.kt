package com.mtdevelopment.delivery.presentation.state

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.delivery.domain.usecase.SelectablePickupDate
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath

/**
 * UI State for the Delivery module.
 * 
 * @property datePickerVisibility Controls the visibility of the date picker dialog.
 * @property userNameFieldText The customer's full name.
 * @property deliveryAddressSearchQuery Current text in the delivery address search field.
 * @property deliveryAddressSuggestions List of suggestions for the delivery address.
 * @property billingAddressSearchQuery Current text in the billing address search field.
 * @property billingAddressSuggestions List of suggestions for the billing address.
 * @property addressSuggestionsLoading True when an API call for address suggestions is in progress.
 * @property showAddressSuggestions Controls the visibility of delivery address suggestions dropdown.
 * @property showBillingAddressSuggestions Controls the visibility of billing address suggestions dropdown.
 * @property shouldShowLocalisationPermission True if the app should ask for location permissions.
 * @property localisationSuccess True if the user's location was successfully acquired.
 * @property userLocationOnPath True if the user's location matches an exact delivery city.
 * @property userLocationCloseFromPath True if the user's location is geographically close to a delivery path.
 * @property streetNotCovered True when the city IS covered, but only by paths that restrict it to
 *   street lists none of which matched this address. The customer has no manual path picker, so
 *   rather than guess a tournée we route them to the same support request as a near-miss.
 * @property isLoading Global loading state for the module.
 * @property isError Error message to display, if any.
 * @property columnScrollingEnabled Used to disable parent scroll when interacting with child components (like Map).
 * @property userCity The name of the city extracted from the user's location or selection.
 * @property userCityLocation The (Lat, Lng) coordinates of the user's city.
 * @property selectedPath The delivery path chosen by the user or matched by the system.
 * @property candidatePaths Every path that serves the customer's address. Holds one entry in the
 *   ordinary case and several when the address is genuinely covered by more than one tournée — the
 *   date picker then merges their dates and the customer's pick decides the path. Empty until an
 *   address has been matched.
 * @property streetSuggestions Street names proposed while the shop restricts a path to part of a
 *   commune (admin path editor only). Empty when nothing is being looked up.
 * @property deliveryPaths List of all available delivery paths.
 * @property isBillingDifferent Flag indicating if the user wants to provide a different billing address.
 * @property fulfillmentType How the customer wants the order: delivered, or collected at the
 *   shop or on a market. Everything below only applies to the two collected modes.
 * @property userPhoneFieldText Contact number, asked only for a collected order: there is no
 *   address to fall back on when the customer does not turn up.
 * @property pickupPoints Points matching the selected mode, loaded from Firestore.
 * @property pickupDates Dates offered for those points, cut-off already applied.
 * @property pickupPointsUnavailable True when the points could not be read. Distinct from an
 *   empty list on purpose: "we could not check" must never be shown as "you cannot collect".
 * @property shopPickupSavingInCents What this basket would cost less if collected at the shop
 *   rather than delivered. Zero when there is nothing to say — an empty basket, or products
 *   priced the same either way — and the banner is hidden rather than showing "0,00 €".
 * @property selectedPickupDate The date the customer picked, and the point it belongs to. Held
 *   here rather than in the composable because it is what the order is built from: kept in the
 *   view it survived a change of mode — the list redrew, nothing looked selected, and the
 *   Continue button still carried the previous mode's date — and it was lost on rotation.
 */
data class DeliveryUiDataState(
    val datePickerVisibility: Boolean = false,
    val userNameFieldText: String = "",

    val deliveryAddressSearchQuery: String = "",
    val deliveryAddressSuggestions: List<AutoCompleteSuggestion> = emptyList(),

    val billingAddressSearchQuery: String = "",
    val billingAddressSuggestions: List<AutoCompleteSuggestion> = emptyList(),

    val addressSuggestionsLoading: Boolean = false,
    val showAddressSuggestions: Boolean = false,
    val showBillingAddressSuggestions: Boolean = false,

    val shouldShowLocalisationPermission: Boolean = false,
    val localisationSuccess: Boolean = false,
    val userLocationOnPath: Boolean = false,
    val userLocationCloseFromPath: Boolean = false,
    val streetNotCovered: Boolean = false,
    val isLoading: Boolean = false,
    val isError: String = "",
    val columnScrollingEnabled: Boolean = true,

    val userCity: String = "",
    val userCityLocation: Pair<Double, Double>? = null,
    val selectedPath: UiDeliveryPath? = null,
    val candidatePaths: List<UiDeliveryPath> = emptyList(),
    val streetSuggestions: List<String> = emptyList(),

    val deliveryPaths: List<UiDeliveryPath> = emptyList(),
    val isBillingDifferent: Boolean = false,

    val fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
    val userPhoneFieldText: String = "",
    val pickupPoints: List<PickupPoint> = emptyList(),
    val pickupDates: List<SelectablePickupDate> = emptyList(),
    val pickupPointsUnavailable: Boolean = false,
    val selectedPickupDate: SelectablePickupDate? = null,
    val shopPickupSavingInCents: Long = 0L
)
package com.mtdevelopment.delivery.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.composable.ProductEditField
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.domain.frenchDayName
import com.mtdevelopment.core.domain.toLocalDate
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import com.mtdevelopment.core.presentation.composable.AddressAutocompleteTextField
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek

/**
 * Survives configuration changes the same way the path editor's draft does: a half-filled
 * market date is annoying enough to retype once, infuriating on every rotation.
 */
private val PickupPointSaver: Saver<PickupPoint, String> = Saver(
    save = { Json.encodeToString(PickupPoint.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(PickupPoint.serializer(), it) }.getOrNull() }
)

/**
 * Creates or edits one pickup point.
 *
 * The form changes shape with the type, because the two kinds genuinely differ: the shop
 * recurs on weekdays and needs a way to be closed, a market happens once on one date. Both
 * halves stay in the draft when the type is switched mid-edit, so nothing typed is lost;
 * the fields that no longer apply are dropped at the mapping boundary instead.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PickupPointEditScreen(
    pointId: String? = null,
    onSaved: () -> Unit = {}
) {
    val viewModel = koinViewModel<AdminViewModel>()
    val state by viewModel.pickupPointsState.collectAsState()
    val orderState by viewModel.orderScreenState.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var draft by rememberSaveable(stateSaver = PickupPointSaver) {
        mutableStateOf(PickupPoint())
    }
    var draftInitialised by rememberSaveable { mutableStateOf(false) }

    // Which date picker is open: the market's own date, or a closure being added.
    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }

    LaunchedEffect(Unit) {
        if (state.points.isEmpty()) viewModel.getAllPickupPoints()
    }

    // Editing waits for the point to come back from Firestore; creating starts immediately.
    // Guarded so a later refresh of the list never overwrites edits in progress.
    LaunchedEffect(state.points, pointId) {
        if (draftInitialised) return@LaunchedEffect
        if (pointId == null) {
            draftInitialised = true
            return@LaunchedEffect
        }
        state.points.find { it.id == pointId }?.let { existing ->
            draft = existing
            viewModel.setAddressText(existing.address)
            draftInitialised = true
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (pointId == null) {
                        "Nouveau point de retrait"
                    } else {
                        "Modifier le point de retrait"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickupPointType.entries.forEach { entry ->
                        FilterChip(
                            selected = draft.type == entry,
                            onClick = { draft = draft.copy(type = entry) },
                            label = { Text(entry.frenchLabel()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProductEditField(
                    title = "Nom affiché au client",
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it) },
                    isError = draft.label.isBlank(),
                    imeAction = ImeAction.Next,
                    placeholder = when (draft.type) {
                        PickupPointType.SHOP -> "La Fromagerie"
                        PickupPointType.MARKET -> "Marché de Pontarlier"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AddressAutocompleteTextField(
                    label = "Adresse",
                    searchQuery = orderState.searchQuery,
                    suggestions = orderState.suggestions,
                    isLoading = orderState.suggestionsLoading,
                    showDropdown = orderState.showSuggestions,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    onDropDownDismiss = { viewModel.setShowAddressesSuggestions(false) },
                    onAddressValidated = { validated, suggestion ->
                        // The suggestion already carries coordinates, so the map pin costs
                        // no extra geocoding call. They stay null on a hand-typed address:
                        // the point still works, it simply cannot be mapped.
                        draft = draft.copy(
                            address = validated,
                            latitude = suggestion?.lat,
                            longitude = suggestion?.long
                        )
                        suggestion?.let { viewModel.onSuggestionSelected(it) }
                    },
                    onValueChange = {
                        draft = draft.copy(address = it)
                        viewModel.setAddressText(it)
                    },
                    onClick = { viewModel.setShowAddressesSuggestions(true) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProductEditField(
                    title = "Horaires affichés",
                    value = draft.timeRange,
                    onValueChange = { draft = draft.copy(timeRange = it) },
                    imeAction = ImeAction.Done,
                    placeholder = "8h-13h"
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (draft.type) {
                    PickupPointType.SHOP -> {
                        Text(
                            text = "Jours d'ouverture",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                val isSelected = draft.openingDays.contains(day.name)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draft = draft.copy(
                                            openingDays = if (isSelected) {
                                                draft.openingDays - day.name
                                            } else {
                                                draft.openingDays + day.name
                                            }
                                        )
                                    },
                                    label = { Text(frenchDayName(day.name).orEmpty()) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Fermetures exceptionnelles",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            IconButton(onClick = { datePickerTarget = DatePickerTarget.CLOSURE }) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Ajouter une fermeture"
                                )
                            }
                        }
                        Text(
                            text = "Congés, jours fériés : ces dates disparaissent des " +
                                    "retraits proposés même si elles tombent un jour d'ouverture.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        draft.closedDates.sortedBy { it.toLocalDate() }.forEach { closed ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(modifier = Modifier.weight(1f), text = closed)
                                IconButton(
                                    onClick = {
                                        draft = draft.copy(
                                            closedDates = draft.closedDates - closed
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Clear,
                                        contentDescription = "Retirer cette fermeture"
                                    )
                                }
                            }
                        }
                    }

                    PickupPointType.MARKET -> {
                        Text(
                            text = "Date du marché",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { datePickerTarget = DatePickerTarget.MARKET_DATE }
                        ) {
                            Text(draft.date ?: "Choisir une date")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(
                    text = "Enregistrer",
                    enabled = draft.canBeSaved,
                    onClick = { viewModel.savePickupPoint(draft) { onSaved.invoke() } }
                )

                if (pointId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DeletePickupPointButton(
                        onConfirmed = {
                            viewModel.deletePickupPoint(draft) { onSaved.invoke() }
                        }
                    )
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
            }

            RiveAnimation(
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Loading animation"
            )

            ErrorOverlay(
                isShown = state.error != null,
                message = state.error,
                onDismiss = { viewModel.clearPickupPointError() }
            )
        }
    }

    datePickerTarget?.let { target ->
        PickupDatePickerDialog(
            onDismiss = { datePickerTarget = null },
            onDateSelected = { date ->
                draft = when (target) {
                    DatePickerTarget.MARKET_DATE -> draft.copy(date = date)
                    // Adding the same closure twice is a slip, not something worth an
                    // error dialog: the duplicate is simply dropped.
                    DatePickerTarget.CLOSURE ->
                        if (draft.closedDates.contains(date)) {
                            draft
                        } else {
                            draft.copy(closedDates = draft.closedDates + date)
                        }
                }
                datePickerTarget = null
            }
        )
    }
}

private enum class DatePickerTarget { MARKET_DATE, CLOSURE }

private fun PickupPointType.frenchLabel(): String = when (this) {
    PickupPointType.SHOP -> "Boutique"
    PickupPointType.MARKET -> "Marché"
}

/**
 * Plain calendar, deliberately not the customer-facing [DatePickerComposable]: that one only
 * offers the dates a delivery path actually serves, which is the opposite of what the shop
 * needs when declaring a market or a week of holiday.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val pickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    // selectedDateMillis is UTC midnight and toStringDate formats in UTC,
                    // which is the convention every stored date in this app follows.
                    pickerState.selectedDateMillis?.let { onDateSelected(it.toStringDate()) }
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

/**
 * Two-step delete, matching the confirmation pattern the admin dialogs already use: a point
 * removed by a stray tap would take its market date with it, and there is no undo.
 */
@Composable
private fun DeletePickupPointButton(onConfirmed: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (confirming) onConfirmed.invoke() else confirming = true }
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (confirming) "Confirmer la suppression" else "Supprimer ce point",
            color = MaterialTheme.colorScheme.error
        )
    }
}

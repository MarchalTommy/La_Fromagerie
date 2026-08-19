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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
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
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

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
            // Shown, not searched: see AdminViewModel.prefillAddressText.
            viewModel.prefillAddressText(existing.address)
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

                // Labelled once. The whole form recomposes on every keystroke below.
                val typeChips = remember { PickupPointType.entries.map { it to it.frenchLabel() } }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeChips.forEach { (entry, label) ->
                        FilterChip(
                            selected = draft.type == entry,
                            onClick = { draft = draft.copy(type = entry) },
                            label = { Text(label) }
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
                        // Seven locale lookups, done once rather than on every keystroke.
                        val dayChips = remember {
                            DayOfWeek.entries.map { it.name to frenchDayName(it.name).orEmpty() }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayChips.forEach { (dayName, dayLabel) ->
                                val isSelected = draft.openingDays.contains(dayName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draft = draft.copy(
                                            openingDays = if (isSelected) {
                                                draft.openingDays - dayName
                                            } else {
                                                draft.openingDays + dayName
                                            }
                                        )
                                    },
                                    label = { Text(dayLabel) }
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

                        // Sorting parses every stored date; only redo it when the list moves.
                        val sortedClosures = remember(draft.closedDates) {
                            draft.closedDates.sortedBy { it.toLocalDate() }
                        }

                        sortedClosures.forEach { closed ->
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
            // A market in the past can never be ordered from, so offering it is offering a
            // mistake. A closure in the past is merely a record of one, and the shop is
            // entitled to enter it late.
            allowPastDates = target == DatePickerTarget.CLOSURE,
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

/**
 * Today included: the cut-off in `BuildSelectablePickupDatesUseCase` decides whether a market
 * happening today is still orderable, and that is not this calendar's call to make.
 *
 * Compared in UTC because that is the zone the picker reports its selection in and the zone
 * [toStringDate] formats it back out of — mixing in the device zone here would put the boundary
 * a few hours off.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object FromTodayOnwards : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= LocalDate.now(ZoneOffset.UTC)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now(ZoneOffset.UTC).year
}

private fun PickupPointType.frenchLabel(): String = when (this) {
    PickupPointType.SHOP -> "Boutique"
    PickupPointType.MARKET -> "Marché"
}

/**
 * Plain calendar, deliberately not the customer-facing [DatePickerComposable]: that one only
 * offers the dates a delivery path actually serves, which is the opposite of what the shop
 * needs when declaring a market or a week of holiday.
 *
 * @param allowPastDates True for closures, which may legitimately be recorded after the fact;
 *   false for a market date, which is a date orders will be taken for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickupDatePickerDialog(
    allowPastDates: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val pickerState = rememberDatePickerState(
        selectableDates = if (allowPastDates) DatePickerDefaults.AllDates else FromTodayOnwards
    )

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

/** How long the button stays armed before it goes back to being a plain delete button. */
private const val DELETE_CONFIRMATION_WINDOW_MS = 4000L

/**
 * Two-step delete, matching the confirmation pattern the admin dialogs already use: a point
 * removed by a stray tap would take its market date with it, and there is no undo.
 *
 * The armed state expires. Left latched it turned the screen into one where a single tap
 * deletes, indefinitely and with nothing on screen to say the meaning of that tap had changed
 * minutes ago — which is the exact accident the confirmation exists to prevent.
 */
@Composable
private fun DeletePickupPointButton(onConfirmed: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    LaunchedEffect(confirming) {
        if (confirming) {
            delay(DELETE_CONFIRMATION_WINDOW_MS)
            confirming = false
        }
    }

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (confirming) onConfirmed.invoke() else confirming = true }
    ) {
        Icon(Icons.Default.Delete, contentDescription = null)
        // Row content: height() would separate nothing. DeliveryOptionScreen gets this right
        // two files over.
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (confirming) "Confirmer la suppression" else "Supprimer ce point",
            color = MaterialTheme.colorScheme.error
        )
    }
}

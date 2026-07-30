package com.mtdevelopment.delivery.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.composable.ProductEditField
import com.mtdevelopment.admin.presentation.model.toDomainDeliveryPath
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.delivery.presentation.composable.CityPostalCodeAutocompleteTextField
import com.mtdevelopment.delivery.presentation.composable.CityStreetsBottomSheet
import com.mtdevelopment.delivery.presentation.model.MoveDirection
import com.mtdevelopment.delivery.presentation.model.PathDraft
import com.mtdevelopment.delivery.presentation.model.canBeSaved
import com.mtdevelopment.delivery.presentation.model.toAdminUiDeliveryPath
import com.mtdevelopment.delivery.presentation.model.toDraft
import com.mtdevelopment.delivery.presentation.model.withCityAdded
import com.mtdevelopment.delivery.presentation.model.withCityMoved
import com.mtdevelopment.delivery.presentation.model.withCityRemovedAt
import com.mtdevelopment.delivery.presentation.model.withStreetsAt
import com.mtdevelopment.delivery.presentation.model.toAdminUiDeliveryPath as pathToAdminUi
import com.mtdevelopment.delivery.presentation.viewmodel.DeliveryViewModel
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Saved-state key the editor sets on the path list's back-stack entry after a write, so the list
 * refreshes once instead of re-fetching on every resume. Lives here rather than in the navigation
 * graph because the two screens on either side of it are what agree on the key.
 */
const val PATH_LIST_NEEDS_REFRESH = "path_list_needs_refresh"

private val FREQUENCIES = listOf(
    "WEEKLY" to "Chaque semaine",
    "BIWEEKLY_EVEN" to "Semaines paires",
    "BIWEEKLY_ODD" to "Semaines impaires"
)

/**
 * Serializes the whole draft so an edit in progress survives rotation and process death. The draft
 * is nested (cities carry street lists), which the primitive-oriented savers cannot express.
 */
private val PathDraftSaver: Saver<PathDraft, String> = Saver(
    save = { Json.encodeToString(PathDraft.serializer(), it) },
    restore = { runCatching { Json.decodeFromString(PathDraft.serializer(), it) }.getOrNull() }
)

/**
 * Full-screen editor for a delivery path, replacing the dialog that used to host the same job.
 *
 * The dialog crammed the identity, the schedule, the city list and a comma-separated street field
 * into one scrolling card, which made the most consequential setting — whether a city is covered
 * whole or only on some streets — the least visible thing on screen. Here each city states its own
 * coverage as a chip, and editing streets opens [CityStreetsBottomSheet].
 *
 * It deliberately does not declare a `Scaffold`: the admin app has exactly one, at the activity
 * level, which already supplies the top bar, the back arrow and the content padding.
 *
 * @param pathId Path to edit, or null to create one.
 */
@Composable
fun PathEditScreen(
    pathId: String? = null,
    navigateBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val deliveryViewModel = koinViewModel<DeliveryViewModel>()
    val adminViewModel = koinViewModel<AdminViewModel>()

    val state = remember(deliveryViewModel.deliveryUiDataState) {
        derivedStateOf { deliveryViewModel.deliveryUiDataState }
    }

    LaunchedEffect(Unit) {
        // This screen owns its ViewModel (one per nav entry), so it loads the paths it needs — and
        // with them the city autocomplete pipeline.
        deliveryViewModel.loadAdminData()
    }

    val existingPath = state.value.deliveryPaths.firstOrNull { it.id == pathId }

    var draft by rememberSaveable(stateSaver = PathDraftSaver) {
        mutableStateOf(PathDraft(id = "", name = "", isNew = true))
    }
    var draftInitialised by rememberSaveable { mutableStateOf(false) }

    // Editing waits for the path to come back from cache; creating starts immediately.
    LaunchedEffect(existingPath, pathId) {
        if (!draftInitialised && (pathId == null || existingPath != null)) {
            draft = existingPath?.pathToAdminUi().toDraft()
            draftInitialised = true
        }
    }

    PathEditContent(
        draft = draft,
        isReady = draftInitialised,
        isLoading = state.value.isLoading,
        errorMessage = state.value.isError,
        citySearchQuery = state.value.deliveryAddressSearchQuery,
        citySuggestions = state.value.deliveryAddressSuggestions,
        showCitySuggestions = state.value.showAddressSuggestions,
        streetSuggestions = state.value.streetSuggestions,
        onStreetQueryChange = { query, city ->
            deliveryViewModel.searchStreets(query, city.name, city.postcode)
        },
        onDraftChange = { draft = it },
        onCitySearchQueryChange = { deliveryViewModel.setAddressFieldText(it) },
        onCitySuggestionsDismiss = {
            deliveryViewModel.setShowAddressesSuggestions(shouldShow = false, isBilling = false)
        },
        onErrorDismissed = { deliveryViewModel.setIsError("") },
        onCancel = navigateBack,
        onSave = {
            deliveryViewModel.setIsLoading(true)
            val domainPath = draft.toAdminUiDeliveryPath().toDomainDeliveryPath()
            val onFailure = { message: String ->
                deliveryViewModel.setIsError(message)
                deliveryViewModel.setIsLoading(false)
            }
            if (draft.isNew) {
                adminViewModel.addNewDeliveryPath(
                    domainPath,
                    onSuccess = onSaved,
                    onFailure = { onFailure("Une erreur est survenue lors de l'ajout du parcours.") }
                )
            } else {
                adminViewModel.updateDeliveryPath(
                    domainPath,
                    onSuccess = onSaved,
                    onFailure = { onFailure("Une erreur est survenue lors de la mise à jour du parcours.") }
                )
            }
        },
        onDelete = {
            deliveryViewModel.setIsLoading(true)
            adminViewModel.deleteDeliveryPath(
                draft.toAdminUiDeliveryPath().toDomainDeliveryPath(),
                onSuccess = onSaved,
                onFailure = {
                    deliveryViewModel.setIsError("Une erreur est survenue lors de la suppression du parcours.")
                    deliveryViewModel.setIsLoading(false)
                }
            )
        }
    )
}

@Composable
fun PathEditContent(
    draft: PathDraft,
    isReady: Boolean = true,
    isLoading: Boolean = false,
    errorMessage: String = "",
    citySearchQuery: String = "",
    citySuggestions: List<AutoCompleteSuggestion> = emptyList(),
    showCitySuggestions: Boolean = false,
    streetSuggestions: List<String> = emptyList(),
    onStreetQueryChange: (query: String, city: DeliveryCity) -> Unit = { _, _ -> },
    onDraftChange: (PathDraft) -> Unit = {},
    onCitySearchQueryChange: (String) -> Unit = {},
    onCitySuggestionsDismiss: () -> Unit = {},
    onErrorDismissed: () -> Unit = {},
    onCancel: () -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var deleteArmed by remember { mutableStateOf(false) }
    var pendingCity by remember { mutableStateOf(DeliveryCity(name = "", postcode = 0)) }
    var showCityPicker by remember { mutableStateOf(false) }
    var streetsEditIndex by remember { mutableStateOf<Int?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        // `imePadding` sits on the Box, not on the scrolled Column: it has to shrink the viewport
        // so the text field being filled is scrolled above the keyboard. Applied inside the scroll
        // it would only pad the content, leaving the field — and its autocomplete dropdown — under
        // the IME.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 160.dp)
            ) {
                if (!draft.isNew) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (deleteArmed) onDelete() else deleteArmed = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = if (deleteArmed) "CONFIRMER ?" else "SUPPRIMER",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                SectionCard(title = "Détails du parcours") {
                    ProductEditField(
                        title = "Nom du parcours",
                        value = draft.name,
                        onValueChange = { onDraftChange(draft.copy(name = it)) },
                        isError = draft.name.isBlank(),
                        placeholder = "Ex : Tournée du mardi",
                        imeAction = ImeAction.Next,
                        focusRequester = focusRequester,
                        focusManager = focusManager
                    )

                    FieldLabel("Jour de livraison")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = draft.deliveryDay == day.name,
                                onClick = { onDraftChange(draft.copy(deliveryDay = day.name)) },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.FRANCE)) }
                            )
                        }
                    }

                    FieldLabel("Fréquence")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FREQUENCIES.forEach { (value, label) ->
                            FilterChip(
                                selected = draft.deliveryFrequency == value,
                                onClick = { onDraftChange(draft.copy(deliveryFrequency = value)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 8.dp))

                SectionCard(title = "Composition du parcours") {
                    if (draft.cities.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = "Aucune ville pour l'instant. Ajoutez le premier arrêt ci-dessous.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    draft.cities.forEachIndexed { index, city ->
                        CityStopCard(
                            city = city,
                            canMoveUp = index > 0,
                            canMoveDown = index < draft.cities.lastIndex,
                            onMove = { onDraftChange(draft.withCityMoved(index, it)) },
                            onRemove = { onDraftChange(draft.withCityRemovedAt(index)) },
                            onEditStreets = { streetsEditIndex = index }
                        )
                        Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    if (showCityPicker) {
                        CityPostalCodeAutocompleteTextField(
                            searchQuery = citySearchQuery,
                            suggestions = citySuggestions,
                            showDropdown = showCitySuggestions,
                            focusRequester = focusRequester,
                            focusManager = focusManager,
                            onDropDownDismiss = onCitySuggestionsDismiss,
                            onSuggestionSelected = { suggestion ->
                                val label = buildString {
                                    append(suggestion.city ?: "Ville inconnue")
                                    suggestion.postCode?.let { append(" ($it)") }
                                }.trim()
                                onCitySearchQueryChange(label)
                                pendingCity = DeliveryCity(
                                    name = suggestion.city ?: "",
                                    postcode = suggestion.postCode?.toIntOrNull() ?: 0
                                )
                            },
                            onValueChange = onCitySearchQueryChange,
                            onFocusChange = {}
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    showCityPicker = false
                                    pendingCity = DeliveryCity(name = "", postcode = 0)
                                    onCitySearchQueryChange("")
                                }
                            ) { Text("Annuler") }
                            TextButton(
                                enabled = pendingCity.name.isNotBlank() && pendingCity.postcode != 0,
                                onClick = {
                                    onDraftChange(draft.withCityAdded(pendingCity))
                                    pendingCity = DeliveryCity(name = "", postcode = 0)
                                    onCitySearchQueryChange("")
                                    showCityPicker = false
                                }
                            ) { Text("Ajouter") }
                        }
                    } else {
                        AddStopButton(onClick = { showCityPicker = true })
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Valider le parcours",
                    trailingIcon = null,
                    enabled = isReady && draft.canBeSaved,
                    onClick = onSave
                )
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancel
                ) { Text("Annuler") }
            }

            RiveAnimation(
                isLoading = isLoading,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Loading animation"
            )

            ErrorOverlay(
                isShown = errorMessage.isNotBlank(),
                duration = 3000L,
                message = errorMessage,
                onDismiss = onErrorDismissed
            )
        }
    }

    streetsEditIndex?.let { index ->
        draft.cities.getOrNull(index)?.let { city ->
            CityStreetsBottomSheet(
                city = city,
                suggestions = streetSuggestions,
                onStreetQueryChange = { query -> onStreetQueryChange(query, city) },
                onConfirm = { streets ->
                    onDraftChange(draft.withStreetsAt(index, streets))
                    streetsEditIndex = null
                },
                onDismiss = { streetsEditIndex = null }
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * One stop of the path. The coverage chip is the point of the card: it is what tells the shop, at a
 * glance, that a commune is split between two tournées.
 */
@Composable
private fun CityStopCard(
    city: DeliveryCity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (MoveDirection) -> Unit,
    onRemove: () -> Unit,
    onEditStreets: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            IconButton(
                onClick = { onMove(MoveDirection.UP) },
                enabled = canMoveUp
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Monter ${city.name}")
            }
            IconButton(
                onClick = { onMove(MoveDirection.DOWN) },
                enabled = canMoveDown
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descendre ${city.name}")
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = city.postcode.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.padding(vertical = 2.dp))

            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onEditStreets() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = if (city.coversWholeCity) {
                    "Toute la ville"
                } else {
                    "${city.streets.size} rue${if (city.streets.size > 1) "s" else ""}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Clear,
                contentDescription = "Retirer ${city.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddStopButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = "Ajouter une ville (ville / code postal)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun PathEditContentPreview() {
    AppTheme {
        PathEditContent(
            draft = PathDraft(
                id = "path-a",
                name = "Parcours A",
                cities = listOf(
                    DeliveryCity("Levier", 25270),
                    DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue"))
                ),
                deliveryDay = "TUESDAY",
                isNew = false
            )
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun PathEditContentNewPreview() {
    AppTheme {
        PathEditContent(draft = null.toDraft())
    }
}

package com.mtdevelopment.delivery.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gigamole.composescrollbars.Scrollbars
import com.gigamole.composescrollbars.config.ScrollbarsConfig
import com.gigamole.composescrollbars.config.ScrollbarsOrientation
import com.gigamole.composescrollbars.config.layercontenttype.ScrollbarsLayerContentType
import com.gigamole.composescrollbars.config.layersType.ScrollbarsLayersType
import com.gigamole.composescrollbars.rememberScrollbarsState
import com.gigamole.composescrollbars.scrolltype.ScrollbarsScrollType
import com.gigamole.composescrollbars.scrolltype.knobtype.ScrollbarsStaticKnobType
import com.mtdevelopment.admin.presentation.composable.ProductEditField
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.domain.move
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.presentation.theme.ui.black70
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

enum class LIST_POSITION {
    FIRST, LAST, NONE
}

enum class MOVE_DIRECTION {
    UP, DOWN
}

@Composable
fun PathEditDialog(
    path: AdminUiDeliveryPath?,
    searchQuery: String,
    autoCompleteSuggestion: List<AutoCompleteSuggestion>,
    showAutocompleteDropdown: Boolean,
    parentScrollState: ScrollState,
    onAutocompleteQueryChange: (String) -> Unit,
    onAutoCompleteDropdownDismiss: () -> Unit,
    onValidate: (AdminUiDeliveryPath) -> Unit,
    onDelete: ((AdminUiDeliveryPath) -> Unit)?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val deleteFirstClick = remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()

    val tempPath = remember {
        mutableStateOf(
            AdminUiDeliveryPath(
                id = path?.id ?: UUID.randomUUID().toString(),
                name = if (path?.name == "Ajouter un parcours") "Nouveau Parcours" else path?.name
                    ?: "",
                cities = path?.cities ?: emptyList(),
                deliveryDay = path?.deliveryDay ?: ""
            )
        )
    }

    val tempNewCity = remember { mutableStateOf(Pair("", 0)) }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showAutocompleteDropdown) {
        isDropdownExpanded = showAutocompleteDropdown
    }

    BackHandler(true) {
        onDismiss.invoke()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = black70
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .imePadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .imePadding()
            ) {
                // Delete button
                if (onDelete != null) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (!deleteFirstClick.value) {
                                    deleteFirstClick.value = true
                                } else {
                                    onDelete.invoke(tempPath.value)
                                    onDismiss.invoke()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.padding(start = 8.dp),
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            modifier = Modifier
                                .padding(top = 16.dp, end = 8.dp, bottom = 16.dp),
                            text = if (!deleteFirstClick.value) "SUPPRIMER" else "CONFIRMER ?",
                            maxLines = 2,
                            softWrap = false,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Nom du parcours
                ProductEditField(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Nom du parcours",
                    value = tempPath.value.name,
                    onValueChange = { tempPath.value = tempPath.value.copy(name = it) },
                    isError = tempPath.value.name.isEmpty(),
                    imeAction = ImeAction.Next,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )

                // Sélection du jour
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    for (i in DayOfWeek.entries) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilterChip(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                selected = tempPath.value.deliveryDay.contains(i.name),
                                onClick = {
                                    tempPath.value = tempPath.value.copy(deliveryDay = i.name)
                                },
                                label = {
                                    Text(
                                        i.getDisplayName(TextStyle.SHORT, Locale.FRANCE)
                                    )
                                }
                            )
                        }
                    }
                }

                // --- Liste des Villes Réordonnable ---
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .align(Alignment.TopStart),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Villes existantes
                        itemsIndexed(
                            items = tempPath.value.cities,
                            key = { _, item -> item.first + item.second }
                        ) { index, city ->

                            CityFields(
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = tween(350),
                                        fadeOutSpec = tween(350),
                                        placementSpec = tween(800)
                                    ),
                                city = city,
                                position = if (index == 0) LIST_POSITION.FIRST else if (index == tempPath.value.cities.lastIndex) LIST_POSITION.LAST else LIST_POSITION.NONE,
                                onRemove = { cityToRemove ->
                                    val updatedCities =
                                        tempPath.value.cities.filterNot { currentCityPair ->
                                            currentCityPair.first == cityToRemove.first && currentCityPair.second == cityToRemove.second
                                        }
                                    tempPath.value = tempPath.value.copy(cities = updatedCities)
                                },
                                onReorganisedClicked = { direction ->
                                    val newOrderList = tempPath.value.cities.toMutableList()
                                    val targetIndex =
                                        if (direction == MOVE_DIRECTION.UP) index - 1 else index + 1

                                    if (targetIndex >= 0 && targetIndex < newOrderList.size) {
                                        newOrderList.move(
                                            index,
                                            targetIndex
                                        )

                                        tempPath.value =
                                            tempPath.value.copy(
                                                cities = newOrderList
                                            )
                                    }
                                }
                            )
                        }

                        // Champ pour nouvelle ville
                        item {
                            CityPostalCodeAutocompleteTextField(
                                searchQuery = searchQuery,
                                suggestions = autoCompleteSuggestion,
                                showDropdown = showAutocompleteDropdown,
                                focusRequester = focusRequester,
                                focusManager = focusManager,
                                onDropDownDismiss = onAutoCompleteDropdownDismiss,
                                onSuggestionSelected = { suggestion ->
                                    val displayText = buildString {
                                        append(suggestion.city ?: "Ville inconnue")
                                        suggestion.postCode?.let { append(" ($it)") }
                                    }.trim()
                                    onAutocompleteQueryChange.invoke(displayText)
                                    tempNewCity.value = Pair(
                                        suggestion.city ?: "",
                                        suggestion.postCode?.toIntOrNull() ?: 0
                                    )
                                },
                                onValueChange = onAutocompleteQueryChange,
                                onFocusChange = {
                                    if (it) {
                                        scope.launch {
                                            delay(500)
                                            parentScrollState.animateScrollTo(parentScrollState.maxValue - 100)
                                            scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
                                        }
                                    }
                                }
                            )
                        }

                        // Boutons Ajouter/Effacer
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    modifier = Modifier.padding(end = 8.dp),
                                    shape = MaterialTheme.shapes.large,
                                    enabled = tempNewCity.value.first.isNotBlank() && tempNewCity.value.second != 0,
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 16.dp
                                    ),
                                    onClick = {
                                        // Ajouter la nouvelle ville à la liste existante
                                        val updatedCities =
                                            tempPath.value.cities + tempNewCity.value
                                        tempPath.value = tempPath.value.copy(cities = updatedCities)
                                        tempNewCity.value = Pair("", 0) // Réinitialiser
                                        onAutocompleteQueryChange.invoke("") // Vider le champ texte
                                    }
                                ) {
                                    Text("Ajouter la ville")
                                }
                            }
                        }
                    }

                    Scrollbars(
                        modifier = Modifier
                            .width(width = 16.dp)
                            .heightIn(max = 300.dp)
                            .align(Alignment.TopEnd),
                        state = rememberScrollbarsState(
                            config = ScrollbarsConfig(
                                orientation = ScrollbarsOrientation.Vertical,
                                paddingValues = PaddingValues(),
                                layersType = ScrollbarsLayersType.Wrap(
                                    paddingValues = PaddingValues(
                                        all = 2.dp
                                    )
                                ),
                                backgroundLayerContentType = ScrollbarsLayerContentType.Custom {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(50),
                                                clip = true,
                                                spotColor = Color.DarkGray.copy(alpha = 0.5F),
                                                ambientColor = Color.DarkGray.copy(alpha = 0.5F)
                                            )
                                            .background(
                                                color = Color(255, 255, 255, 255),
                                                shape = RoundedCornerShape(50)
                                            )
                                    )
                                },
                                knobLayerContentType = ScrollbarsLayerContentType.Default.Colored.Idle(
                                    shape = RoundedCornerShape(50),
                                    idleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7F)
                                )
                            ),
                            scrollType = ScrollbarsScrollType.Lazy.List.Static(
                                state = scrollState,
                                knobType = ScrollbarsStaticKnobType.Auto()
                            )
                        )
                    )
                }

                // Boutons Valider/Annuler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        enabled = (tempPath.value.name.isNotBlank() &&
                                tempPath.value.cities.isNotEmpty() &&
                                tempPath.value.deliveryDay.isNotBlank()),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        onClick = {
                            if (tempPath.value != path) {
                                onValidate.invoke(tempPath.value)
                            }
                            onDismiss.invoke()
                        }
                    ) {
                        Text("Valider le parcours")
                    }

                    TextButton(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        onClick = { onDismiss.invoke() }
                    ) {
                        Text("Annuler")
                    }
                }
            }
        }
    }
}

@Composable
fun CityFields(
    modifier: Modifier = Modifier,
    city: Pair<String, Int>?,
    position: LIST_POSITION,
    onRemove: (Pair<String, Int>) -> Unit = {},
    onReorganisedClicked: (MOVE_DIRECTION) -> Unit = {},
    onRenderedHeight: (Int) -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (city != null) onRemove(city)
                }

                SwipeToDismissBoxValue.EndToStart -> {}
                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
            }
            return@rememberSwipeToDismissBoxState true
        },
        positionalThreshold = { it * .20f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { DismissBackground(dismissState) },
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Icon d'organisation ---
            if (position != LIST_POSITION.NONE) {
                IconButton(onClick = {
                    if (position == LIST_POSITION.FIRST) {
                        onReorganisedClicked.invoke(MOVE_DIRECTION.DOWN)
                    } else {
                        onReorganisedClicked.invoke(MOVE_DIRECTION.UP)
                    }
                }) {
                    Icon(
                        imageVector = if (position == LIST_POSITION.FIRST) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = "Déplacer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                        onReorganisedClicked.invoke(MOVE_DIRECTION.UP)
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Déplacer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                        )
                    }
                    IconButton(onClick = {
                        onReorganisedClicked.invoke(MOVE_DIRECTION.DOWN)
                    }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Déplacer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // --- Champs Ville et Code Postal ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .onGloballyPositioned { onRenderedHeight(it.size.height) },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductEditField(
                    modifier = Modifier.weight(0.6f),
                    title = "Ville",
                    value = city?.first ?: "",
                    isReadOnly = true
                )

                ProductEditField(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(end = 8.dp),
                    title = "Code postal",
                    value = city?.second?.toString() ?: "",
                    isReadOnly = true
                )
            }
        }
    }
}

// DismissBackground
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.EndToStart -> Color.Transparent
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "delete",
                tint = MaterialTheme.colorScheme.onError
            )
        }
        Spacer(modifier = Modifier)
    }
}
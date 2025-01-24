package com.mtdevelopment.admin.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
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
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.presentation.theme.ui.black70
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun PathEditDialog(
    path: AdminUiDeliveryPath?,
    onValidate: (AdminUiDeliveryPath) -> Unit,
    onDelete: ((AdminUiDeliveryPath) -> Unit)?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
) {
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current

    val deleteFirstClick = remember {
        mutableStateOf(false)
    }
    val scrollState = rememberLazyListState(0)

    val tempPath = remember {
        mutableStateOf(
            AdminUiDeliveryPath(
                id = path?.id ?: UUID.randomUUID().toString(),
                name = if (path?.name == "Ajouter un parcours") {
                    "Nouveau Parcours"
                } else {
                    path?.name ?: ""
                },
                cities = path?.cities ?: emptyList(),
                deliveryDay = path?.deliveryDay ?: ""
            )
        )
    }

    val tempCities = remember {
        mutableListOf<Pair<String, Int>>()
    }
    val tempNewCity = remember { mutableStateOf(Pair("", 0)) }

    LaunchedEffect(Unit) {
        path?.cities?.let { tempCities.addAll(it) }
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
                            text = if (!deleteFirstClick.value) {
                                "SUPPRIMER"
                            } else {
                                "CONFIRMER ?"
                            },
                            maxLines = 2,
                            softWrap = false,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                ProductEditField(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Nom du parcours",
                    value = tempPath.value.name,
                    onValueChange = {
                        tempPath.value = tempPath.value.copy(name = it)
                    },
                    isError = tempPath.value.name.isEmpty(),
                    imeAction = ImeAction.Next,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    for (i in DayOfWeek.entries) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(i.getDisplayName(TextStyle.SHORT, Locale.FRANCE))
                            FilterChip(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                selected = tempPath.value.deliveryDay.contains(i.name),
                                onClick = {
                                    tempPath.value =
                                        tempPath.value.copy(deliveryDay = i.name)
                                },
                                label = {
                                    i.name
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .align(Alignment.TopStart),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Existing cities
                        items(tempPath.value.cities, key = { it.first }) { city ->
                            CityFields(
                                city = city,
                                focusManager = focusManager,
                                focusRequester = focusRequester,
                                onCityChange = { newCity ->
                                    val updatedCities = tempCities.map { currentCity ->
                                        if (currentCity.first == city.first) { // Compare with the 'city' from the function parameter
                                            Pair(newCity, currentCity.second)
                                        } else {
                                            currentCity
                                        }
                                    }
                                    tempPath.value =
                                        tempPath.value.copy(cities = updatedCities ?: emptyList())
                                    (tempCities as MutableList).clear()
                                    (tempCities as MutableList).addAll(updatedCities)
                                },
                                onPostcodeChange = { newPostcode ->
                                    val updatedCities = tempCities.map { currentCity ->
                                        if (currentCity.first == city.first) { // Compare with the 'city' from the function parameter
                                            Pair(currentCity.first, newPostcode ?: 0)
                                        } else {
                                            currentCity
                                        }
                                    }
                                    tempPath.value =
                                        tempPath.value.copy(cities = updatedCities ?: emptyList())
                                    (tempCities as MutableList).clear()
                                    (tempCities as MutableList).addAll(updatedCities)
                                }
                            )
                        }

                        // New city fields
                        item {
                            CityFields(
                                city = tempNewCity.value,
                                focusManager = focusManager,
                                focusRequester = focusRequester,
                                onCityChange = {
                                    tempNewCity.value = tempNewCity.value.copy(first = it)
                                },
                                onPostcodeChange = {
                                    tempNewCity.value = tempNewCity.value.copy(second = it ?: 0)
                                }
                            )
                        }

                        // Add/Clear city buttons
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    shape = MaterialTheme.shapes.large,
                                    enabled = tempNewCity.value.second != 0 && tempNewCity.value.first != "",
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 16.dp
                                    ),
                                    onClick = {
                                        // Update tempCities first
                                        val updatedCities = tempCities + tempNewCity.value
                                        (tempCities as MutableList).clear()
                                        (tempCities as MutableList).addAll(updatedCities)

                                        // Then update tempPath to trigger recomposition
                                        tempPath.value = tempPath.value.copy(cities = updatedCities)

                                        tempNewCity.value = Pair("", 0)
                                    }
                                ) {
                                    Text("Ajouter la ville")
                                }

                                TextButton(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    shape = MaterialTheme.shapes.large,
                                    enabled = tempNewCity.value.second != 0 && tempNewCity.value.first != "",
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 16.dp
                                    ),
                                    onClick = {
                                        tempNewCity.value = Pair("", 0)
                                    }
                                ) {
                                    Text("Vider les champs")
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
                                    // Set shadowed background.
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
                        onClick = {
                            onDismiss.invoke()
                        }
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
    city: Pair<String, Int>?,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    onCityChange: (String) -> Unit,
    onPostcodeChange: (Int?) -> Unit,
    onRenderedHeight: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .onGloballyPositioned {
                onRenderedHeight.invoke(it.size.height)
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProductEditField(
            modifier = Modifier.weight(0.5f),
            title = "Ville",
            value = city?.first ?: "",
            onValueChange = {
                onCityChange.invoke(it)
            },
            imeAction = ImeAction.Next,
            focusRequester = focusRequester,
            focusManager = focusManager,
        )

        ProductEditField(
            modifier = Modifier.weight(0.5f),
            title = "Code postal",
            value = (city?.second ?: "").toString(),
            onValueChange = {
                try {
                    onPostcodeChange.invoke(it.toInt())
                } catch (e: Exception) {
                    onPostcodeChange.invoke(null)
                }
            },
            isNumberOnly = true,
            imeAction = ImeAction.Next,
            focusRequester = focusRequester,
            focusManager = focusManager,
        )
    }
}
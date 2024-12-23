package com.mtdevelopment.admin.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.core.presentation.theme.ui.black70
import java.time.DayOfWeek
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

    val scrollState = rememberScrollState()
    val deleteFirstClick = remember {
        mutableStateOf(false)
    }

    val tempPath = remember {
        mutableStateOf(
            AdminUiDeliveryPath(
                id = path?.id ?: UUID.randomUUID().toString(),
                name = path?.name ?: "",
                cities = path?.cities ?: emptyList(),
                deliveryDay = path?.deliveryDay ?: ""
            )
        )
    }

    val tempCities = remember {
        path?.cities?.toMutableList()
    }
    var tempNewCity = Pair("", 0)

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
                .padding(48.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .wrapContentHeight()
                    .focusable(true),
                verticalArrangement = Arrangement.Center
            ) {

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
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (i in DayOfWeek.entries) {
                        FilterChip(
                            modifier = Modifier,
                            selected = tempPath.value.deliveryDay.contains(i.name),
                            onClick = {
                                tempPath.value = tempPath.value.copy(deliveryDay = i.name)
                            },
                            label = {
                                i.name
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    state = LazyListState()
                ) {
                    items(items = tempPath.value.cities, key = { it }) { city ->
                        CityFields(city,
                            focusManager,
                            focusRequester,
                            onCityChange = {
                                tempCities?.firstOrNull { it.first == city.first }?.let {
                                    tempCities[tempCities.indexOf(it)] = Pair(it.first, it.second)
                                }
                            },
                            onPostcodeChange = {
                                tempCities?.firstOrNull { it.first == city.first }?.let {
                                    tempCities[tempCities.indexOf(it)] = Pair(it.first, it.second)
                                }
                            })
                    }

                    item {
                        CityFields(tempNewCity,
                            focusManager,
                            focusRequester,
                            onCityChange = {
                                tempNewCity = tempNewCity.copy(first = it)
                            },
                            onPostcodeChange = {
                                tempNewCity = tempNewCity.copy(second = it ?: 0)
                            })

                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp, start = 8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp, start = 8.dp),
                                shape = MaterialTheme.shapes.large,
                                enabled = tempNewCity.second != 0 && tempNewCity.first != "",
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                                onClick = {
                                    tempCities?.add(tempNewCity)
                                    tempNewCity = Pair("", 0)
                                }
                            ) {
                                Text(
                                    "Ajouter la ville"
                                )
                            }

                            TextButton(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp, start = 8.dp),
                                shape = MaterialTheme.shapes.large,
                                enabled = tempNewCity.second != 0 && tempNewCity.first != "",
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                                onClick = {
                                    tempNewCity = Pair("", 0)
                                }
                            ) {
                                Text(
                                    "Vider les champs"
                                )
                            }
                        }

                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp, start = 8.dp),
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
                        },
                    ) {
                        Text(
                            "Valider le parcours"
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(top = 8.dp, end = 8.dp, start = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        onClick = {
                            onDismiss.invoke()
                        },
                    ) {
                        Text(
                            "Annuler"
                        )
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
    onPostcodeChange: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ProductEditField(
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
            title = "Code postal",
            value = (city?.second ?: "").toString(),
            onValueChange = {
                try {
                    onPostcodeChange.invoke(it.toInt())
                } catch (e: Exception) {
                    onPostcodeChange.invoke(null)
                }
            },
            imeAction = ImeAction.Next,
            focusRequester = focusRequester,
            focusManager = focusManager,
        )
    }
}
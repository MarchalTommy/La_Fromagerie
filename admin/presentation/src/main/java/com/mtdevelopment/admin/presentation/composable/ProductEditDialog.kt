package com.mtdevelopment.admin.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.theme.ui.black70
import com.mtdevelopment.core.util.toLongPrice
import com.mtdevelopment.core.util.toUiPrice

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditDialog(
    onValidate: (UiProductObject) -> Unit,
    onDelete: (UiProductObject) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    product: UiProductObject
) {
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()
    val deleteFirstClick = remember {
        mutableStateOf(false)
    }

    val tempProduct = remember {
        mutableStateOf(
            UiProductObject(
                id = product.id,
                name = product.name,
                priceInCents = product.priceInCents,
                imageUrl = product.imageUrl,
                description = product.description,
                allergens = product.allergens,
                type = product.type
            )
        )
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
                .padding(48.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .wrapContentHeight()
                    .focusable(true),
                verticalArrangement = Arrangement.Center
            ) {
                ProductEditField(
                    title = "Nom du produit",
                    value = tempProduct.value.name,
                    onValueChange = {
                        tempProduct.value = tempProduct.value.copy(name = it)
                    },
                    isError = tempProduct.value.name.isEmpty(),
                    imeAction = ImeAction.Next,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )
                ProductEditField(
                    title = "Prix",
                    value = if (tempProduct.value.priceInCents != 0L) {
                        tempProduct.value.priceInCents.toUiPrice().replace("€", "")
                    } else {
                        ""
                    },
                    onValueChange = {
                        try {
                            if (it.toLongPrice() < 10000) {
                                tempProduct.value =
                                    tempProduct.value.copy(priceInCents = it.toLongPrice())
                            }
                        } catch (e: NumberFormatException) {
                            focusManager.clearFocus()
                            onError.invoke("Vous ne pouvez pas mettre plusieurs virgules / points")
                        }

                    },
                    isError = tempProduct.value.priceInCents == 0L,
                    isNumberOnly = true,
                    imeAction = ImeAction.Next,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )
                ProductEditField(
                    title = "Description",
                    value = tempProduct.value.description,
                    onValueChange = {
                        tempProduct.value = tempProduct.value.copy(description = it)
                    },
                    isError = tempProduct.value.description.isEmpty(),
                    isBigText = true,
                    imeAction = ImeAction.Next,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )
                ProductEditField(
                    title = "Allergènes",
                    value = tempProduct.value.allergens?.joinToString { it } ?: "",
                    onValueChange = {
                        tempProduct.value = tempProduct.value.copy(allergens =
                        it.split(",").map { allergen ->
                            allergen.trim()
                                .replaceFirstChar { firstChar -> firstChar.uppercaseChar() }
                        }
                        )
                    },
                    imeAction = ImeAction.Done,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (!deleteFirstClick.value) {
                                    deleteFirstClick.value = true
                                } else {
                                    onDelete.invoke(product)
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("Valider",
                            Modifier
                                .clickable {
                                    if (tempProduct.value != product) {
                                        onValidate.invoke(tempProduct.value)
                                    }
                                    onDismiss.invoke()
                                }
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        )
                        Text("Annuler",
                            Modifier
                                .clickable {
                                    onDismiss.invoke()
                                }
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductEditField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    isNumberOnly: Boolean = false,
    isBigText: Boolean = false,
    imeAction: ImeAction,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
    OutlinedTextField(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = value,
        onValueChange = {
            onValueChange.invoke(it)
        },
        label = {
            Text(title)
        },
        prefix = {
            if (isNumberOnly) {
                Text("€")
            }
        },
        singleLine = !isBigText,
        maxLines = if (isBigText) Int.MAX_VALUE else 1,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction,
            keyboardType = if (isNumberOnly) {
                KeyboardType.Number
            } else {
                KeyboardType.Text
            },
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus(force = true)
            }
        )
    )
}
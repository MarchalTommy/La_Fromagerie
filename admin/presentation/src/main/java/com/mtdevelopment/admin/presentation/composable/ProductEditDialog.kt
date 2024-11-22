package com.mtdevelopment.admin.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.toLongPrice
import com.mtdevelopment.core.util.toUiPrice

@Composable
fun ProductEditDialog(
    onValidate: (UiProductObject) -> Unit,
    onDelete: (UiProductObject) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    product: UiProductObject
) {
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

    // TODO: Fix focus
    // TODO: Fix scroll
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()

    val deleteFirstClick = remember {
        mutableStateOf(false)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .scrollable(scrollState, orientation = Orientation.Vertical)
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically)
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
                        allergen.trim().replaceFirstChar { firstChar -> firstChar.uppercaseChar() }
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
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            if (!deleteFirstClick.value) {
                                deleteFirstClick.value = true
                            } else {
                                onDelete.invoke(product)
                                onDismiss.invoke()
                            }
                        },
                    text = if (!deleteFirstClick.value) {
                        "SUPPRIMER"
                    } else {
                        "CONFIRMER ?"
                    },
                    color = MaterialTheme.colorScheme.error
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("Valider",
                        Modifier
                            .padding(16.dp)
                            .clickable {
                                if (tempProduct.value != product) {
                                    onValidate.invoke(tempProduct.value)
                                }
                                onDismiss.invoke()
                            })
                    Text("Annuler",
                        Modifier
                            .padding(16.dp)
                            .clickable {
                                onDismiss.invoke()
                            })
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
            keyboardType = if (isNumberOnly) KeyboardType.Number else KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus()
            }
        )
    )
}
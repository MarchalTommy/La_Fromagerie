package com.mtdevelopment.admin.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mtdevelopment.core.model.ProductType
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.theme.ui.black70
import com.mtdevelopment.core.util.toLongPrice
import com.mtdevelopment.core.util.toStringPrice

// TODO: Check why bug when picture + data, but not when just picture or just data
@Preview(showBackground = true)
@Composable
fun ProductEditDialog(
    onValidate: (UiProductObject) -> Unit = {},
    onDelete: ((UiProductObject) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onError: (String) -> Unit = {},
    shouldShowLoading: (Boolean) -> Unit = {},
    product: UiProductObject? = null
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
                id = product?.id ?: "",
                name = product?.name ?: "",
                priceInCents = product?.priceInCents ?: 0L,
                imageUrl = product?.imageUrl ?: "",
                description = product?.description ?: "",
                allergens = product?.allergens ?: listOf(),
                type = product?.type ?: ProductType.FROMAGE
            )
        )
    }
    var allergensInputText by remember(tempProduct.value.allergens) {
        mutableStateOf(tempProduct.value.allergens?.joinToString(", ") ?: "")
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
                .padding(vertical = 32.dp, horizontal = 24.dp)
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
                                    onDelete.invoke(product!!)
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

                ImagePickerButton(
                    existingImageUri = tempProduct.value.imageUrl?.toUri(),
                    onImagePicked = {
                        tempProduct.value = tempProduct.value.copy(imageUrl = it.toString())
                    },
                    shouldShowLoading = shouldShowLoading,
                    onError = onError
                )

                ProductEditField(
                    modifier = Modifier,
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
                    modifier = Modifier,
                    title = "Prix",
                    value = if (tempProduct.value.priceInCents != 0L) {
                        tempProduct.value.priceInCents.toStringPrice()
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
                    prefix = {
                        Text("€")
                    }
                )
                ProductEditField(
                    modifier = Modifier,
                    title = "Description",
                    value = tempProduct.value.description,
                    onValueChange = {
                        tempProduct.value = tempProduct.value.copy(description = it)
                    },
                    isError = tempProduct.value.description.isEmpty(),
                    isBigText = true,
                    imeAction = ImeAction.Default,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                )
                ProductEditField(
                    modifier = Modifier,
                    title = "Allergènes",
                    value = allergensInputText,
                    onValueChange = {
                        allergensInputText = it.replace(".", "")
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
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp, start = 8.dp),
                            enabled = (tempProduct.value.name.isNotBlank() && tempProduct.value.priceInCents in 50..3000),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            onClick = {
                                tempProduct.value = tempProduct.value.copy(
                                    allergens = allergensInputText.split(',')
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                )
                                if (tempProduct.value != product) {
                                    onValidate.invoke(tempProduct.value)
                                }
                                onDismiss.invoke()
                            },
                        ) {
                            Text(
                                "Valider"
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
}

@Composable
fun ProductEditField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    isError: Boolean = false,
    isNumberOnly: Boolean = false,
    isBigText: Boolean = false,
    isReadOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    focusRequester: FocusRequester? = null,
    focusManager: FocusManager? = null,
    prefix: @Composable() (() -> Unit)? = null
) {
    val requester = focusRequester ?: remember {
        FocusRequester()
    }

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .focusRequester(requester),
        value = value,
        readOnly = isReadOnly,
        onValueChange = {
            onValueChange.invoke(it)
        },
        label = {
            Text(title)
        },
        prefix = prefix,
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
                focusManager?.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager?.clearFocus()
            }
        )
    )
}
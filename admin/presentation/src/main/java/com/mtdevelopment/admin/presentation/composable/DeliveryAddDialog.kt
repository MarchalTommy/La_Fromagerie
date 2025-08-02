package com.mtdevelopment.admin.presentation.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.presentation.composable.AddressAutocompleteTextField
import com.mtdevelopment.core.presentation.theme.ui.black70
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import java.time.LocalDate

@Preview(showBackground = true)
@Composable
fun DeliveryAddDialog(
    suggestions: List<AutoCompleteSuggestion?> = emptyList(),
    searchQuery: String = "",
    showDropdown: Boolean = false,
    onValueChange: (String) -> Unit = {},
    onDropDownDismiss: () -> Unit = {},
    onSuggestionSelected: (AutoCompleteSuggestion) -> Unit = {},
    onConfirm: (Order) -> Unit = {},
    onDismiss: () -> Unit = {},
    onError: (String) -> Unit = {},
) {

    val screenSize: ScreenSize = rememberScreenSize()
    val focusRequester = remember {
        FocusRequester()
    }
    val focusManager = LocalFocusManager.current

    val scrollState = rememberScrollState()
    val deleteFirstClick = remember {
        mutableStateOf(false)
    }

    val todayDate =
        LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            .toStringDate()

    val tempOrder = remember {
        mutableStateOf(
            Order(
                id = "manual_order_",
                customerName = "",
                customerAddress = searchQuery,
                customerBillingAddress = searchQuery,
                deliveryDate = todayDate,
                orderDate = todayDate,
                products = emptyMap(),
                status = OrderStatus.IN_PREPARATION,
                note = "",
                isManuallyAdded = true
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
                .padding(vertical = 32.dp, horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .wrapContentHeight()
                    .focusable(true)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp)
                        .focusRequester(focusRequester),
                    value = tempOrder.value.customerName,
                    onValueChange = {
                        tempOrder.value = tempOrder.value.copy(
                            customerName = it,
                            id = "manual_order_$it"
                        )
                    },
                    label = {
                        Text(
                            text = "Nom du client"
                        )
                    },
                    shape = ShapeDefaults.Medium,
                )

                AddressAutocompleteTextField(
                    label = "Adresse de livraison",
                    searchQuery = searchQuery,
                    onValueChange = {
                        onValueChange.invoke(it)
                    },
                    suggestions = suggestions,
                    isLoading = false,
                    showDropdown = showDropdown,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    onDropDownDismiss = onDropDownDismiss,
                    onAddressValidated = { address, suggestions ->
                        tempOrder.value = tempOrder.value.copy(
                            customerAddress = address,
                            customerBillingAddress = address
                        )
                        suggestions?.let {
                            onSuggestionSelected.invoke(it)
                        }
                    },
                    onClick = {

                    }
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenSize.height / 5))
                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        .focusRequester(focusRequester),
                    label = {
                        Text(
                            text = "Note de livraison"
                        )
                    },
                    value = tempOrder.value.note.toString(),
                    onValueChange = {
                        tempOrder.value = tempOrder.value.copy(
                            note = it
                        )
                    },
                    shape = ShapeDefaults.Medium,
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
                            enabled = (searchQuery.isNotBlank()),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            onClick = {
                                onConfirm.invoke(tempOrder.value)
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
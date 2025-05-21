package com.mtdevelopment.admin.presentation.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.koinViewModel

@Composable
fun OrderPreparationScreen() {
    val viewModel = koinViewModel<AdminViewModel>()
    val state = viewModel.orderScreenState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAllOrders()
    }

    OrderPreparationList(
        orders = state.value.orders
    )

    RiveAnimation(
        isLoading = state.value.isLoading,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Loading animation"
    )

    ErrorOverlay(
        isShown = state.value.error != null,
        message = state.value.error,
    )

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderPreparationList(
    orders: List<Order>
) {
    val nextDeliveryDates = orders.map { it.deliveryDate }.sorted().toSet()
    val ordersByDeliveryDate = orders.groupBy { it.deliveryDate }

    LazyColumn {

        for (deliveryDate in nextDeliveryDates) {
            if (ordersByDeliveryDate[deliveryDate] != null && ordersByDeliveryDate[deliveryDate]!!.isNotEmpty()) {

                // This is the way to get a good final map like {Beurre=4, Banane=13, Fromage=2, Lait=1}
                // Doing it here and using the ordersByDeliveryDate allows us to only show the orders dedicated to this date.
                val quantityForProducts: Map<String, Int> =
                    ordersByDeliveryDate[deliveryDate]!!.fold(mutableMapOf()) { accumulator, currentMap ->
                        currentMap.products.forEach { (product, quantity) ->
                            accumulator.merge(product, quantity, Int::plus)
                        }
                        accumulator
                    }

                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .background(color = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 32.dp, vertical = 16.dp)
                                .align(Alignment.CenterStart),
                            text = deliveryDate,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                items(items = quantityForProducts.toList()) { item ->
                    OrderPreparationListItem(
                        product = item.first,
                        quantity = item.second,
                        itemsPerClients = ordersByDeliveryDate[deliveryDate] ?: listOf(),
                    )
                }
            }
        }
    }
}

@Composable
fun OrderPreparationListItem(
    product: String,
    quantity: Int,
    itemsPerClients: List<Order>,
) {

    val mapClientToProducts = mutableMapOf<String, MutableList<Pair<String, Int>>>()
    itemsPerClients.forEach { order ->
        order.products.forEach { (product, quantity) ->
            if (mapClientToProducts[order.customerName] == null) {
                mapClientToProducts[order.customerName] = mutableListOf(Pair(product, quantity))
            } else {
                mapClientToProducts[order.customerName]!!.add(Pair(product, quantity))
            }
        }
    }

    val showDetails = remember { mutableStateOf(false) }
    val isDone = remember { mutableStateOf(false) }

    val rotation = animateFloatAsState(
        targetValue = if (showDetails.value) 180f else 0f
    )

    val itemColor = animateColorAsState(
        targetValue = if (isDone.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardColors(
            containerColor = itemColor.value,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = product,
                    style = MaterialTheme.typography.titleSmall
                )

                Box(
                    modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        modifier = Modifier,
                        onClick = { showDetails.value = !showDetails.value },
                    ) {
                        Icon(
                            modifier = Modifier.rotate(rotation.value),
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showDetails.value) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    mapClientToProducts.forEach { (client, items) ->
                        val goodProduct = items.find { it.first == product }
                        if (goodProduct != null && goodProduct.second > 0) {
                            Text("${goodProduct.second} -> $client")
                        }

                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
                    ) {
                        Checkbox(
                            checked = isDone.value, onCheckedChange = {
                                isDone.value = it
                            })
                    }
                }
            }
        }
    }
}
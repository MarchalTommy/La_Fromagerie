package com.mtdevelopment.admin.presentation.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.koinViewModel

@Composable
fun OrderPreparationScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<AdminViewModel>()
    val state = viewModel.orderScreenState.collectAsState()

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
    val nextDeliveryDates = orders.map { it.deliveryDate }.sortedDescending().toSet()
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
                    Text(
                        text = deliveryDate,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // TODO: Find a way to manage Orders Number to identify them (or like, name#123 ?)
                // TODO: I MIGHT need to test and check that, but it may display all the orders, I can't think straight right now but I see no filter for the delivery date applied.
                // TODO: NOW THAT I HAVE THE BASE IDEA FOR DESIGN, I NEED TO DO DATA BEFORE MORE TO BE ABLE TO SEE WHAT I DO
                items(items = quantityForProducts.toList()) {
//                    OrderPreparationListItem(
//                        product = it.first,
//                        quantity = it.second,
//                        itemsPerClients = ordersByDeliveryDate[deliveryDate]!!.associate { it.quantityPerProducts }
//                    )
                }
            }
        }
    }
}

@Composable
fun OrderPreparationListItem(
    product: Product,
    quantity: Int,
    itemsPerClients: Map<Int, Int>
) {

    val showDetails = remember { mutableStateOf(false) }
    val isDone = remember { mutableStateOf(false) }

    val rotation = animateFloatAsState(
        targetValue = if (showDetails.value) 0f else 180f
    )

    val itemColor = animateColorAsState(
        targetValue = if (isDone.value) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .animateContentSize()
            .background(color = itemColor.value),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    isDone.value = !isDone.value
                },
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall
                )

                IconButton(
                    onClick = { showDetails.value = !showDetails.value },
                    modifier = Modifier,
                ) {
                    Icon(
                        modifier = Modifier.rotate(rotation.value),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = showDetails.value) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    for ((items, orderNb) in itemsPerClients) {
                        Text("$items pour la commande $orderNb")
                    }
                }

            }
        }
    }
}
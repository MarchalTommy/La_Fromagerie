package com.mtdevelopment.admin.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.domain.toLocalDate
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.PaymentMode
import com.mtdevelopment.core.model.PreparationGroup
import com.mtdevelopment.core.model.PreparationStatus
import com.mtdevelopment.core.model.preparationGroup
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.koinViewModel
import java.time.LocalDate

/**
 * Screen for administrative order preparation.
 * It displays a list of orders grouped by delivery date.
 * For each date, it aggregates the total quantity needed for each product,
 * helping the administrator to prepare the right amount of goods.
 */
@Composable
fun OrderPreparationScreen() {
    val viewModel = koinViewModel<AdminViewModel>()
    val state = viewModel.orderScreenState.collectAsState()

    // Fetch orders and statuses when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.getAllOrders()
    }

    OrderPreparationList(
        orders = state.value.orders,
        preparationStatuses = state.value.preparationStatuses,
        onUpdateStatus = { viewModel.updatePreparationStatus(it) },
        onMarkPaidOnSite = { viewModel.markOrderPaidOnSite(it) }
    )

    // Loading and Error overlays
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

/**
 * Displays the list of delivery dates as sticky headers, with aggregated products under each date.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderPreparationList(
    orders: List<Order>,
    preparationStatuses: List<PreparationStatus>,
    onUpdateStatus: (PreparationStatus) -> Unit,
    onMarkPaidOnSite: (Order) -> Unit = {}
) {
    // Batches, newest first. Grouping on the date alone would merge a tournée, the shop
    // pickups and a market of the same day into one aggregate, leaving no way to tell what
    // goes in the van — hence the group, which also carries the pickup point so two
    // markets on one day stay apart.
    val ordersByGroup = orders.groupBy { it.preparationGroup }
    val groups = ordersByGroup.keys.sortedWith(
        compareByDescending<PreparationGroup> { it.deliveryDate.toTimeStamp() }
            .thenBy { it.fulfillmentType }
            .thenBy { it.pickupLabel.orEmpty() }
    )

    // Captured once so every item of a frame compares against the same day and
    // scrolling does not re-evaluate the clock.
    val today = remember { LocalDate.now() }

    LazyColumn {

        for (group in groups) {
            val deliveryDate = group.deliveryDate
            val ordersForDate = ordersByGroup[group] ?: emptyList()
            val isPastDate = deliveryDate.toLocalDate()?.isBefore(today) == true
            if (ordersForDate.isNotEmpty()) {

                // Aggregate product quantities for the current batch.
                // e.g., if Order A has 2 Milk and Order B has 3 Milk, results in {Milk=5}
                val quantityForProducts: Map<String, Int> =
                    ordersForDate.fold(mutableMapOf()) { accumulator, currentMap ->
                        currentMap.products.forEach { (product, quantity) ->
                            accumulator.merge(product, quantity, Int::plus)
                        }
                        accumulator
                    }

                stickyHeader {
                    // Past dates get a muted header so expired deliveries read as history
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isPastDate) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 32.dp, vertical = 16.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = deliveryDate,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isPastDate) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSecondary
                                }
                            )
                            // Deliveries keep a date-only header, exactly as before. Only a
                            // pickup batch needs naming, because that is the only case where
                            // one date carries several destinations.
                            group.pickupLabel?.takeIf { group.fulfillmentType.isPickup }
                                ?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isPastDate) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSecondary
                                        }
                                    )
                                }
                        }
                    }
                }

                items(items = quantityForProducts.toList()) { item ->
                    // Handle padding for first, last, or single items
                    val modifier = when {
                        quantityForProducts.toList().size == 1 ->
                            Modifier.padding(vertical = 16.dp)

                        quantityForProducts.toList().indexOf(item) == 0 ->
                            Modifier.padding(top = 16.dp)

                        quantityForProducts.toList()
                            .indexOf(item) == quantityForProducts.toList().size - 1 ->
                            Modifier.padding(bottom = 16.dp)

                        else -> Modifier
                    }
                    OrderPreparationListItem(
                        modifier = modifier,
                        product = item.first,
                        quantity = item.second,
                        itemsPerClients = ordersForDate,
                        group = group,
                        isPastDate = isPastDate,
                        preparationStatuses = preparationStatuses,
                        onUpdateStatus = onUpdateStatus,
                        onMarkPaidOnSite = onMarkPaidOnSite
                    )
                }
            }
        }
    }
}

/**
 * Individual item representing an aggregated product.
 * It can be expanded to see which customers ordered this specific product and their respective quantities.
 * Orders are keyed by order ID so multiple orders by the same client name are counted separately.
 */
@Composable
fun OrderPreparationListItem(
    modifier: Modifier = Modifier,
    product: String,
    quantity: Int,
    itemsPerClients: List<Order>,
    group: PreparationGroup,
    isPastDate: Boolean,
    preparationStatuses: List<PreparationStatus>,
    onUpdateStatus: (PreparationStatus) -> Unit,
    onMarkPaidOnSite: (Order) -> Unit = {}
) {

    // Map each order to its products for this delivery date
    // Key is the Order ID to avoid name collisions
    val mapOrderIdToOrderAndProducts = remember(itemsPerClients) {
        mutableMapOf<String, Pair<Order, Map<String, Int>>>().apply {
            itemsPerClients.forEach { order ->
                this[order.id] = order to order.products
            }
        }
    }

    val showDetails = remember { mutableStateOf(false) }
    // Unique per (batch, product): deliveries keep the historical id so ticks already
    // stored in Firestore keep matching, pickups get the point appended.
    val statusId = group.statusIdFor(product)
    val isDone = preparationStatuses.find { it.id == statusId }?.isPrepared ?: false

    val rotation = animateFloatAsState(
        targetValue = if (showDetails.value) 180f else 0f
    )

    // Stitch surface container background: animated if prepared or not
    val itemColor = animateColorAsState(
        targetValue = if (isDone) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            // Past/expired deliveries are faded out as a whole, keeping their inner
            // colors and layout untouched
            .alpha(if (isPastDate) 0.6f else 1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardColors(
            containerColor = itemColor.value,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox relocated to the far left of the card header as per Stitch mockup
                Checkbox(
                    checked = isDone,
                    onCheckedChange = { isChecked ->
                        onUpdateStatus(
                            PreparationStatus(
                                id = statusId,
                                date = group.deliveryDate,
                                productName = product,
                                isPrepared = isChecked
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Product details aggregated vertically
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = product,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$quantity ${if (quantity > 1) "unités" else "unité"}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { showDetails.value = !showDetails.value },
                ) {
                    Icon(
                        modifier = Modifier.rotate(rotation.value),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Afficher les détails"
                    )
                }
            }

            AnimatedVisibility(visible = showDetails.value) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail list per customer with customized left border and notes
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        mapOrderIdToOrderAndProducts.forEach { (_, pair) ->
                            val (order, productsMap) = pair
                            val productQuantity = productsMap[product]
                            if (productQuantity != null && productQuantity > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Vertical colored indicator strip on the left
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = order.customerName,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$productQuantity ${if (productQuantity > 1) "unités" else "unité"}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        // Money owed at the counter. Shown only for the
                                        // orders that carry it, so a delivery round is never
                                        // cluttered with a button that cannot apply.
                                        if (order.paymentMode == PaymentMode.ON_SITE) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (order.status == OrderStatus.PAID) {
                                                Text(
                                                    text = "Encaissé",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                TextButton(
                                                    contentPadding = PaddingValues(0.dp),
                                                    onClick = { onMarkPaidOnSite(order) }
                                                ) {
                                                    Text(
                                                        text = "À encaisser • marquer payé",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }

                                        // Highlighted handwritten-style custom instruction/comment note
                                        if (!order.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\"${order.note}\"",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontStyle = FontStyle.Italic,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
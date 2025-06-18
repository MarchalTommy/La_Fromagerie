package com.mtdevelopment.admin.presentation.composable

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteCircularList(
    itemHeight: Dp,
    itemWidth: Dp,
    items: List<T>,
    initialItem: T,
    textColor: Color,
    onItemSelected: (index: Int, item: AdminUiDeliveryPath?) -> Unit = { _, _ -> },
    onItemPreSelected: (item: T?) -> Unit = {}
) {
    val scrollState = rememberLazyListState(0)
    var lastSelectedIndex by remember { mutableIntStateOf(0) }
    var itemsState by remember { mutableStateOf(items) }

    val lastItem = AdminUiDeliveryPath(
        id = UUID.randomUUID().toString(),
        name = "Ajouter un parcours",
        cities = emptyList(),
        deliveryDay = ""
    )

    var hasScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            var targetIndex = items.indexOf(initialItem)
            targetIndex += ((Int.MAX_VALUE / 2) / (items.size + 1)) * (items.size + 1)
            itemsState = items
            lastSelectedIndex = targetIndex
            scrollState.scrollToItem(targetIndex)
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState),
        userScrollEnabled = true
    ) {
        items(count = Int.MAX_VALUE) { i ->
            val adjustedIndex = i % (itemsState.size + 1)
            val secondItemIndex = (scrollState.firstVisibleItemIndex + 1) % (itemsState.size + 1)

            val shouldBeBigger = adjustedIndex == secondItemIndex && hasScrolled

            var scale by remember { mutableFloatStateOf(1f) }

            LaunchedEffect(shouldBeBigger) {
                if (shouldBeBigger) {
                    animate(
                        initialValue = 0.8f,
                        targetValue = 1f,
                        animationSpec = tween(250)
                    ) { value, _ ->
                        scale = value
                    }
                    if (secondItemIndex - 1 >= 0) {
                        onItemPreSelected.invoke(items[secondItemIndex - 1])
                    } else {
                        onItemPreSelected.invoke(null)
                    }
                } else {
                    animate(
                        initialValue = 1f,
                        targetValue = 0.8f,
                        animationSpec = tween(250)
                    ) { value, _ ->
                        scale = value
                    }
                }
            }

            if (adjustedIndex == 0) {
                // First item is the "Add new" item
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .width(itemHeight)
                        .padding(end = 8.dp, top = 8.dp)
                        .clickable {
                            onItemSelected(0, lastItem)
                        }
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .height(itemHeight - 16.dp)
                            .width(itemHeight - 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = lastItem.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                hasScrolled = true
                // Regular items
                val item = itemsState[adjustedIndex - 1] as AdminUiDeliveryPath
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .width(itemHeight)
                        .padding(end = 8.dp, top = 8.dp)
                        .clickable {
                            onItemSelected(adjustedIndex, item)
                        }
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .height(itemHeight - 16.dp)
                            .width(itemHeight - 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = item.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = (DayOfWeek.valueOf(item.deliveryDay)
                                    .getDisplayName(TextStyle.FULL, Locale.FRANCE)).uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = item.cities.joinToString(", ") { it.first.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
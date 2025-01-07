package com.mtdevelopment.admin.presentation.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> InfiniteCircularList(
    itemHeight: Dp,
    numberOfDisplayedItems: Int = 3,
    items: List<T>,
    initialItem: T,
    textColor: Color,
    onItemSelected: (index: Int, item: AdminUiDeliveryPath?) -> Unit = { _, _ -> }
) {
    val scrollState = rememberLazyListState(0)
    var lastSelectedIndex = remember {
        mutableIntStateOf(0)
    }
    var itemsState = remember {
        mutableStateOf(items)
    }

    val lastItem = AdminUiDeliveryPath(
        id = "new",
        name = "",
        cities = emptyList(),
        deliveryDay = ""
    )

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            var targetIndex = items.indexOf(initialItem) - 1
            targetIndex += ((Int.MAX_VALUE / 2) / items.size) * items.size
            itemsState.value = items
            lastSelectedIndex.intValue = targetIndex
            scrollState.scrollToItem(targetIndex)
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(
            lazyListState = scrollState
        ),
        userScrollEnabled = true
    ) {
        items(
            count = Int.MAX_VALUE,
            itemContent = { i ->
                val item = itemsState.value[i % itemsState.value.size] as AdminUiDeliveryPath
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .width(itemHeight)
                        .padding(end = 8.dp, top = 8.dp)
                        .clickable {
                            onItemSelected(i % itemsState.value.size, item)
                        },
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
                                text = item.deliveryDay,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
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
        )
    }
}
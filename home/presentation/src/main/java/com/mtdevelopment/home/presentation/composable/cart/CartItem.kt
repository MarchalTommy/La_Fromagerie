package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.coroutines.launch

@Preview
@Composable
fun CartItem(
    modifier: Modifier = Modifier,
    item: UiProductObject? = null,
    onRemoveOne: () -> Unit = {},
    onAddMore: () -> Unit = {},
    onRemoveAll: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRemoveAll.invoke()
                }

                SwipeToDismissBoxValue.EndToStart -> {}

                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
            }
            return@rememberSwipeToDismissBoxState true
        },
        // positional threshold of 20%
        positionalThreshold = { it * .20f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DismissBackground(dismissState)
        },
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        gesturesEnabled = true
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (item != null) {
                Row {
                    IconButton(modifier = Modifier
                        .padding(4.dp),
                        onClick = {
                            coroutineScope.launch {
                                dismissState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterVertically),
                            imageVector = Icons.Default.Delete,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "Remove product"
                        )
                    }
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = item.name
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { onRemoveOne.invoke() }) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(4.dp)
                                .size(32.dp),
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Remove one"
                        )
                    }
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = item.quantity.toString()
                    )
                    IconButton(onClick = { onAddMore.invoke() }) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(4.dp)
                                .size(32.dp),
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Add more"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.EndToStart -> Color(0xFF1DE9B6)
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "delete",
            tint = MaterialTheme.colorScheme.onError
        )
        Spacer(modifier = Modifier)
    }
}
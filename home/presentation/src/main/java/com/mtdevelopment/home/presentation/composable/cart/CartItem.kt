package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.model.CartItem
import kotlinx.coroutines.launch

/**
 * Composable representing a single item in the shopping cart.
 * It features controls to increment/decrement quantity and supports swipe-to-dismiss for removal.
 * 
 * @param modifier Modifier for the item layout.
 * @param item The cart item to display.
 * @param onRemoveOne Callback to decrement item quantity.
 * @param onAddMore Callback to increment item quantity.
 * @param onRemoveAll Callback to remove the item entirely.
 */
@Preview
@Composable
fun CartItem(
    modifier: Modifier = Modifier,
    item: CartItem? = null,
    onRemoveOne: () -> Unit = {},
    onAddMore: () -> Unit = {},
    onRemoveAll: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    // State management for swipe-to-dismiss functionality
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Trigger total removal on swipe from start to end
                    onRemoveAll.invoke()
                }

                SwipeToDismissBoxValue.EndToStart -> {}

                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
            }
            return@rememberSwipeToDismissBoxState true
        },
        // positional threshold of 20% to trigger dismiss
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
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(50)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (item != null) {
                Row {
                    // Manual delete button
                    IconButton(
                        modifier = Modifier
                            .padding(4.dp),
                        onClick = {
                            coroutineScope.launch {
                                // Trigger swipe animation programmatically
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity controls
                    FilledTonalIconButton(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        onClick = { onRemoveOne.invoke() }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Remove one"
                        )
                    }
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .widthIn(min = 20.dp),
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    FilledTonalIconButton(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        onClick = { onAddMore.invoke() }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add more"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Background UI displayed during the swipe-to-dismiss gesture.
 */
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
            .background(color = color, shape = RoundedCornerShape(50))
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
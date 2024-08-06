package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.home.presentation.model.UiProductObject

@Preview
@Composable
fun CartItem(
    modifier: Modifier = Modifier,
    item: UiProductObject? = null,
    onRemoveOne: () -> Unit = {},
    onAddMore: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (item != null) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = item.name
            )
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
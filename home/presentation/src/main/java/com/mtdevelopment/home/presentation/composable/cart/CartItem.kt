package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mtdevelopment.home.presentation.model.UiProductObject

@Preview
@Composable
fun CartItem(
    item: UiProductObject? = null
) {
    Row {
        { TODO("Row with name, quantity, and + and - buttons.") }
    }
}
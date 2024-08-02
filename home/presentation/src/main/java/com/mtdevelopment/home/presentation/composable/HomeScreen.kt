package com.mtdevelopment.home.presentation.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.home.presentation.model.UiProductObject

@Preview
@Composable
fun HomeScreen(
    paddingValues: PaddingValues = PaddingValues(64.dp),
) {
    val testList = listOf(
        UiProductObject(
            id = "1",
            name = "Brushetta",
            price = 1.0,
            imageUrl = null,
            description = ""
        ),
        UiProductObject(
            id = "2",
            name = "Nature frais",
            price = 2.0,
            imageUrl = null,
            description = ""
        ),
        UiProductObject(
            id = "3",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            description = ""
        ),
        UiProductObject(
            id = "4",
            name = "Ail et Fines Herbes",
            price = 4.99,
            imageUrl = "https://www.france-mineraux.fr/wp-content/uploads/2023/11/aliment-fromage-de-chevre.jpg",
            description = ""
        ),
        UiProductObject(
            id = "5",
            name = "Raz-El-Hanout",
            price = 1.0,
            imageUrl = null,
            description = ""
        )
    )
    LazyVerticalGrid(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 8.dp),
        columns = GridCells.Adaptive(minSize = 168.dp)
    ) {
        items(items = testList) {
            ProductItem(
                product = it
            )
        }
    }
}
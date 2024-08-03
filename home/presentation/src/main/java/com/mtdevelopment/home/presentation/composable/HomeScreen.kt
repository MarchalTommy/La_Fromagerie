package com.mtdevelopment.home.presentation.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.home.presentation.R
import com.mtdevelopment.home.presentation.composable.cart.CartView
import com.mtdevelopment.home.presentation.model.ProductType
import com.mtdevelopment.home.presentation.model.UiProductObject
import com.mtdevelopment.home.presentation.viewmodel.MainViewModel

@Preview
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel? = null
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val testList = listOf(
        UiProductObject(
            id = "1",
            name = "Ail et Fines Herbes",
            price = 3.50,
            imageRes = R.drawable.cheese_afh,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "2",
            name = "Fromage inconnu",
            price = 3.50,
            imageRes = R.drawable.cheese_todefine9,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "3",
            name = "Saveur du Jardin",
            price = 3.54,
            imageRes = R.drawable.cheese_garden,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "4",
            name = "Duo de moutarde",
            price = 3.54,
            imageRes = R.drawable.cheese_mustard_duo,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "5",
            name = "Poivre",
            price = 3.54,
            imageRes = R.drawable.cheese_pepper,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "6",
            name = "Pavot bleu",
            price = 3.54,
            imageRes = R.drawable.cheese_pavot,
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "7",
            name = "Faisselle",
            price = 3.54,
            imageRes = R.drawable.cheese_faisselle,
            type = ProductType.FAISSELLE,
            description = ""
        ),
        UiProductObject(
            id = "8",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "9",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "10",
            name = "Tomme de Chèvre",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.TOMME,
            description = ""
        ),
        UiProductObject(
            id = "11",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "12",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "13",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "14",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "15",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "16",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "17",
            name = "Cendré crèmeux",
            price = 3.54,
            imageUrl = "https://api-drive.drive.supermarchesmatch.fr/api/sdk/cms/media/image/11865.webp",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "18",
            name = "Ail et Fines Herbes",
            price = 4.99,
            imageUrl = "https://www.france-mineraux.fr/wp-content/uploads/2023/11/aliment-fromage-de-chevre.jpg",
            type = ProductType.FROMAGE,
            description = ""
        ),
        UiProductObject(
            id = "19",
            name = "Raz-El-Hanout",
            price = 1.0,
            imageUrl = null,
            type = ProductType.FROMAGE,
            description = ""
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            columns = GridCells.Adaptive(minSize = 168.dp)
        ) {
            items(items = testList) {
                ProductItem(
                    product = it,
                    mainViewModel = mainViewModel
                )
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.BottomEnd)
                .border(
                    2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            onClick = { showBottomSheet = true }
        ) {
            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
        }

        if (showBottomSheet) {
            CartView(mainViewModel = mainViewModel) {
                showBottomSheet = false
            }
        }
    }
}
package com.mtdevelopment.home.presentation.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.sharedModels.ProductType
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.home.presentation.R
import com.mtdevelopment.home.presentation.composable.cart.CartView
import kotlinx.coroutines.launch

@Preview
@Composable
fun HomeScreen(
    cartViewModel: CartViewModel? = null,
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToDetail: (UiProductObject) -> Unit = {},
    navigateToCheckout: () -> Unit = {}
) {

    val coroutineScope = rememberCoroutineScope()

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

    val scaleCart = remember { Animatable(1f) }

    val cartContent =
        cartViewModel?.cartObjects?.collectAsState()?.value?.content?.collectAsState(emptyList())

    fun animateAddingToCart() {
        coroutineScope.launch {
            scaleCart.animateTo(
                1.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleCart.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            columns = GridCells.Adaptive(minSize = 168.dp)
        ) {
            items(items = testList, key = { it.id }) {
                ProductItem(
                    product = it,
                    onDetailClick = {
                        navigateToDetail.invoke(it)
                    }
                ) {
                    cartViewModel?.addCartObject(it)
                    animateAddingToCart()
                }
            }
        }

        BadgedBox(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    scaleX = scaleCart.value
                    scaleY = scaleCart.value
                }
                .padding(32.dp),
            badge = {
                if ((cartContent?.value?.size ?: 0) > 0) {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        val cartItemsQuantity = cartContent?.value?.sumOf { it.quantity }
                        Text("$cartItemsQuantity")
                    }
                }
            }
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .border(
                        3.dp,
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(16.dp)
                    ),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
                onClick = { showBottomSheet = true }
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }

        if (showBottomSheet) {
            CartView(cartViewModel = cartViewModel, {
                showBottomSheet = false
            }, {
                navigateToCheckout.invoke()
            })
        }
    }
}
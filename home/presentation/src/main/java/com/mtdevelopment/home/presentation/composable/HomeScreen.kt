package com.mtdevelopment.home.presentation.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.home.presentation.R
import com.mtdevelopment.home.presentation.composable.cart.CartView
import com.mtdevelopment.home.presentation.model.ProductType
import com.mtdevelopment.home.presentation.model.UiProductObject
import com.mtdevelopment.home.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Preview
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel? = null,
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToDetail: (String) -> Unit = {},
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

    val showMiniAnimated = remember { mutableStateOf(false) }
    val miniItem = remember { mutableStateOf<UiProductObject?>(null) }

    val translationMini = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    translationMini.updateBounds(
        upperBound = Offset(
            x = screenSize.widthPx / 2f,
            y = screenSize.heightPx.toFloat()
        ),
        lowerBound = Offset(
            x = -screenSize.widthPx / 2f,
            y = -screenSize.heightPx.toFloat()
        )
    )

    val scaleCart = remember { Animatable(1f) }

    fun animateAddingToCart() {
        coroutineScope.launch {
            showMiniAnimated.value = true
            translationMini.animateTo(
                targetValue = Offset(
                    screenSize.width.value,
                    screenSize.height.value * 2
                ),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            showMiniAnimated.value = false

            scaleCart.animateTo(
                1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleCart.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            translationMini.snapTo(
                targetValue = Offset(0f, 0f)
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
                    product = it
                ) {
                    mainViewModel?.addCartObject(it)
                    miniItem.value = it
                    animateAddingToCart()
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    scaleX = scaleCart.value
                    scaleY = scaleCart.value
                }
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

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    this.translationX = translationMini.value.x
                    this.translationY = translationMini.value.y
                },
            visible = showMiniAnimated.value,
            enter = expandIn(
                animationSpec = tween(150),
                expandFrom = Alignment.TopCenter
            ),
            exit = shrinkOut(
                animationSpec = tween(200),
                shrinkTowards = Alignment.TopCenter
            )
        ) {
            MiniProductItem(
                miniItem.value
            )
        }

        if (showBottomSheet) {
            CartView(mainViewModel = mainViewModel, {
                showBottomSheet = false
            }, {
                navigateToCheckout.invoke()
            })
        }
    }
}
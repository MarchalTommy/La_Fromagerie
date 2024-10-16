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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.presentation.testList
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.home.presentation.composable.cart.CartView
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Preview(
    name = "Default device",
    device = Devices.DEFAULT,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Preview(
    name = "Small device",
    device = Devices.PIXEL_2,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Preview(
    name = "Smaller device",
    device = "spec:width=720px,height=980px,dpi=320",
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun HomeScreen(
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToDetail: (UiProductObject) -> Unit = {},
    navigateToDelivery: (UiBasketObject) -> Unit = {}
) {

    val cartViewModel = koinViewModel<CartViewModel>()
    val coroutineScope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }
    val scaleCart = remember { Animatable(1f) }

    val cartContent =
        cartViewModel.cartObjects.collectAsState().value.content.collectAsState(emptyList())

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
            items(items = testList, key = { it.id }) { productListItem ->
                ProductItem(
                    product = productListItem,
                    onDetailClick = {
                        navigateToDetail.invoke(it)
                    }
                ) {
                    cartViewModel.addCartObject(productListItem)
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
                if ((cartContent.value.size) > 0) {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        val cartItemsQuantity = cartContent.value.sumOf { it.quantity }
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
                onClick = {
                    showBottomSheet = true
                }
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }

        if (showBottomSheet) {
            CartView(cartViewModel = cartViewModel, {
                showBottomSheet = false
            }, {
                cartViewModel.cartObjects.value.let { navigateToDelivery.invoke(it) }
            })
        }
    }
}
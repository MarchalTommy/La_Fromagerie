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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.koinViewModel
import com.mtdevelopment.home.presentation.composable.cart.CartView
import com.mtdevelopment.home.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    cartViewModel: CartViewModel,
    shouldRefresh: Boolean,
    navigateToDetail: () -> Unit = {},
    navigateToDelivery: () -> Unit = {},
    navigateToOrders: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val homeViewModel = koinViewModel<HomeViewModel>()

    val scaleCart = remember { Animatable(1f) }

    val cartState = cartViewModel.cartUiState
    val homeState = homeViewModel.homeUiState
    var showEditDialog by remember { mutableStateOf(false) }
    var editedProduct by remember { mutableStateOf<UiProductObject?>(null) }

    var hasLoadedFirstPic by remember { mutableStateOf(false) }

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

    LaunchedEffect(hasLoadedFirstPic) {
        if (hasLoadedFirstPic) {
            mainViewModel.setCanRemoveSplash()
        }
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            homeViewModel.refreshProducts()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp),
            columns = GridCells.Adaptive(minSize = 168.dp)
        ) {
            items(items = homeState.products, key = { it.id }) { productListItem ->

                ProductItem(
                    modifier = if (productListItem.id == homeState.products.last().id) {
                        Modifier.padding(
                            bottom = 64.dp
                        )
                    } else {
                        Modifier
                    },
                    product = productListItem,
                    onDetailClick = {
                        cartViewModel.saveClickedItem(it)
                        navigateToDetail.invoke()
                    },
                    onAddClick = {
                        cartViewModel.addCartObject(valueAsUiObject = productListItem)
                        animateAddingToCart()
                    },
                    onEditClick = {
                        showEditDialog = true
                        editedProduct = it
                    },
                    isLoadingFinished = {
                        if (!hasLoadedFirstPic) {
                            hasLoadedFirstPic = true
                        }
                    }
                )
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
                if ((cartState.cartItems?.cartItems) != null && (cartState.cartItems?.cartItems?.size)!! > 0) {
                    Badge(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        val cartItemsQuantity =
                            cartState.cartItems?.cartItems!!.sumOf { it?.quantity ?: 0 }
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
                    cartViewModel.setCartVisibility(true)
                }
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }

        if (cartState.isCartVisible) {
            CartView(
                cartViewModel = cartViewModel,
                onDismiss = {
                    cartViewModel.setCartVisibility(false)
                },
                onNavigateToCheckout = {
                    cartState.cartItems.let {
                        cartViewModel.resetCart(withVisibility = true)
                        navigateToDelivery.invoke()
                    }
                })
        }

        // Loading animation
        RiveAnimation(
            isLoading = homeState.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )
    }
}
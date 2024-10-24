package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.util.vibratePhoneClick
import com.mtdevelopment.core.util.vibratePhoneClickBig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CartView(
    cartViewModel: CartViewModel? = null,
    onDismiss: () -> Unit = {},
    onNavigateToCheckout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val cartItems = cartViewModel?.cartObjects?.collectAsState()
    val cartItemsContent = cartItems?.value?.content?.collectAsState(emptyList())
    val cartTotalPrice = cartItems?.value?.totalPrice?.collectAsState(initial = "")

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = { onDismiss.invoke() }
    ) {

        Text(
            "Ici, c'est votre panier !",
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                val alphaAnimation = remember {
                    Animatable(1f)
                }

                AnimatedVisibility(
                    visible = cartItemsContent?.value?.isEmpty() == true,
                    enter = fadeIn(animationSpec = tween(800))
                ) {
                    CartEmptyMessage(alphaAnimation = alphaAnimation)
                }
            }
            items(items = cartItemsContent?.value ?: emptyList(), key = { it.id }) {
                val itemVisibility = remember {
                    Animatable(1f)
                }
                CartItem(
                    Modifier
                        .animateItemPlacement(
                            animationSpec = tween(800)
                        )
                        .alpha(itemVisibility.value),
                    it,
                    onAddMore = {
                        vibratePhoneClick(context)
                        cartViewModel?.addCartObject(it)
                    },
                    onRemoveOne = {
                        vibratePhoneClick(context)
                        coroutineScope.launch {
                            if (it.quantity <= 1) {
                                itemVisibility.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(200)
                                )
                                cartViewModel?.totallyRemoveObject(it)
                            } else {
                                cartViewModel?.removeCartObject(it)
                            }
                        }
                    }
                )
            }
            item("Footer") {
                CartFooter(
                    modifier = Modifier
                        .animateItemPlacement(
                            animationSpec = tween(300)
                        )
                        .fillMaxWidth(),
                    totalAmount = cartTotalPrice?.value ?: "",
                    hasItems = cartItemsContent?.value?.isNotEmpty() == true,
                ) {
                    vibratePhoneClickBig(context)
//                    onNavigateToCheckout.invoke()
                }
            }
        }
    }
}
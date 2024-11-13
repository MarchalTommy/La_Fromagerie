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

    val isNetworkConnected = cartViewModel?.isConnected?.collectAsState(false)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val state = cartViewModel?.cartUiState

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
            }
            onDismiss.invoke()
        }
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
                    visible = state?.cartObject?.content?.isEmpty() == true,
                    enter = fadeIn(animationSpec = tween(800))
                ) {
                    CartEmptyMessage(alphaAnimation = alphaAnimation)
                }
            }
            items(items = state?.cartObject?.content ?: emptyList(), key = { it.id }) {
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
                    totalAmount = state?.cartObject?.totalPrice ?: "",
                    hasItems = state?.cartObject?.content?.isNotEmpty() == true,
                    canShowDelivery = isNetworkConnected?.value ?: false
                ) {
                    coroutineScope.launch {
                        sheetState.hide()
                    }
                    onNavigateToCheckout.invoke()
                }
            }
        }
    }
}
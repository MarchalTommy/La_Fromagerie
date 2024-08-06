package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.home.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CartView(
    mainViewModel: MainViewModel? = null,
    onDismiss: () -> Unit = {},
) {

    val coroutineScope = rememberCoroutineScope()

    val alphaAnimation = remember {
        androidx.compose.animation.core.Animatable(1f)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val cartItems = mainViewModel?.cartObjects?.collectAsState()
    val cartItemsContent = cartItems?.value?.content?.collectAsState(emptyList())

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

        LazyColumn(modifier = Modifier.padding(16.dp)) {


            item {
                AnimatedVisibility(
                    visible = cartItemsContent?.value?.isEmpty() == true,
                    enter = fadeIn(animationSpec = tween(800))
                ) {
                    Text(
                        "C'est bien vide d'ailleurs...\nOn commande un p'tit fromage ?",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .graphicsLayer {
                                this.alpha = alphaAnimation.value
                            },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            items(cartItemsContent?.value ?: emptyList(), key = { it.id }) {
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
                        mainViewModel?.addCartObject(it)
                    },
                    onRemoveOne = {
                        coroutineScope.launch {
                            if (it.quantity <= 1) {
                                itemVisibility.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(300)
                                )
                                mainViewModel?.totallyRemoveObject(it)
                            } else {
                                mainViewModel?.removeCartObject(it)
                            }
                        }
                    }
                )

            }
        }
    }
}
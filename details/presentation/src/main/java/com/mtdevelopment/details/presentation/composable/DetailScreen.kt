package com.mtdevelopment.details.presentation.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toSize
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtd.presentation.R
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.core.util.vibratePhoneClick
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    detailProductObject: UiProductObject,
    viewModel: CartViewModel,
    screenSize: ScreenSize = rememberScreenSize(),
    navigateToDetail: (String) -> Unit = {},
    navigateToCheckout: () -> Unit = {}
) {

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val cartContent =
        viewModel.cartObjects.collectAsState().value.content.collectAsState(emptyList())
    val scaleCart = remember { Animatable(1f) }

    val product: UiProductObject = detailProductObject

    val nameWidth = remember { mutableFloatStateOf(0f) }
    val nameHeightDp = remember { mutableIntStateOf(0) }
    val priceWidth = remember { mutableFloatStateOf(0f) }
    val priceHeightDp = remember { mutableIntStateOf(0) }
    val nameBackgroundTint = MaterialTheme.colorScheme.primaryContainer

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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GlideImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f),
                imageModel = {
                    product.imageUrl ?: product.imageRes ?: R.drawable.placeholder
                },
                imageOptions = ImageOptions(contentScale = ContentScale.FillWidth),
                requestBuilder = {
                    Glide.with(LocalContext.current)
                        .asBitmap()
                        .apply(
                            RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                        ).thumbnail(0.6f)
                        .transition(withCrossFade())
                },
                loading = {
                    Box(modifier = Modifier.matchParentSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                failure = {
                    Text(text = "image request failed.")
                })

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((nameHeightDp.intValue / 2).dp)
                ) {

                    drawRoundRect(
                        color = nameBackgroundTint,
                        style = Fill,
                        alpha = 0.8f,
                        topLeft = Offset(x = -80.dp.value, y = 0f),
                        size = Size(nameWidth.floatValue + 256.dp.value, this.size.height),
                        cornerRadius = CornerRadius(256.dp.value)
                    )

                    drawRoundRect(
                        color = Color.Black,
                        style = Fill,
                        alpha = 0.5f,
                        topLeft = Offset(x = -80.dp.value, y = 0f),
                        size = Size(nameWidth.floatValue + 256.dp.value, this.size.height),
                        cornerRadius = CornerRadius(256.dp.value)
                    )
                }

                Text(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 16.dp),
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    onTextLayout = { textLayoutResult ->
                        nameWidth.floatValue = textLayoutResult.size.toIntRect().size.toSize().width
                        nameHeightDp.intValue =
                            textLayoutResult.size.toIntRect().size.height
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((priceHeightDp.intValue / 2).dp)
                ) {

                    drawRoundRect(
                        color = nameBackgroundTint,
                        style = Fill,
                        alpha = 0.8f,
                        topLeft = Offset(x = -80.dp.value, y = 0f),
                        size = Size(priceWidth.floatValue + 256.dp.value, this.size.height),
                        cornerRadius = CornerRadius(256.dp.value)
                    )

                    drawRoundRect(
                        color = Color.White,
                        style = Fill,
                        alpha = 0.4f,
                        topLeft = Offset(x = -80.dp.value, y = 0f),
                        size = Size(priceWidth.floatValue + 256.dp.value, this.size.height),
                        cornerRadius = CornerRadius(256.dp.value)
                    )
                }

                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = product.toPrice(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    onTextLayout = { textLayoutResult ->
                        priceWidth.floatValue =
                            textLayoutResult.size.toIntRect().size.toSize().width
                        priceHeightDp.intValue =
                            textLayoutResult.size.toIntRect().size.height
                    }
                )
            }
        }

        Text(
            modifier = Modifier.padding(16.dp),
            text = product.description,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            text = "Liste des allèrgenes :",
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = TextDecoration.Underline
        )

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            text = "Ail, herbes",
            style = MaterialTheme.typography.bodyMedium
        )

        BadgedBox(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 32.dp)
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
                        val cartItemsQuantity =
                            cartContent.value.find { it.id == product.id }?.quantity
                        Text("$cartItemsQuantity")
                    }
                }
            }
        ) {
            Button(
                modifier = Modifier,
                onClick = {
                    vibratePhoneClick(context = context)
                    animateAddingToCart()
                    viewModel.addCartObject(product)
                },
                shape = Shapes().medium
            ) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")

                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = "Ajouter au panier"
                )
            }
        }
    }

}
package com.mtdevelopment.details.presentation.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toSize
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import com.mtdevelopment.core.util.toStringPrice
import com.mtdevelopment.core.util.vibratePhoneClick
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    viewModel: CartViewModel,
    mainViewModel: MainViewModel,
    onProductEdited: () -> Unit = {},
    onProductDeleted: () -> Unit = {},
    screenSize: ScreenSize = rememberScreenSize()
) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val state = viewModel.cartUiState
    val scaleCart = remember { Animatable(1f) }

    val nameWidth = remember { mutableFloatStateOf(0f) }
    val nameHeightDp = remember { mutableIntStateOf(0) }
    val priceWidth = remember { mutableFloatStateOf(0f) }
    val priceHeightDp = remember { mutableIntStateOf(0) }
    val nameBackgroundTint = MaterialTheme.colorScheme.primaryContainer

    val scrollState = rememberScrollState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }

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
                    state.currentItem?.imageUrl
                        ?: "https://res.cloudinary.com/dzgaywpmz/image/upload/v1748253326/goats4_vroahb.jpg"
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
                    GlideImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.3f),
                        imageModel = {
                            "https://res.cloudinary.com/dzgaywpmz/image/upload/v1748253326/goats4_vroahb.jpg"
                        },
                        imageOptions = ImageOptions(contentScale = ContentScale.FillWidth),
                    )
                })

            if (state.currentItem?.isAvailable != true && state.currentItem?.imageUrl?.isBlank() == true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.3f)
                        .alpha(0.8f)
                        .background(MaterialTheme.colorScheme.secondary),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .padding(32.dp),
                        text = "La photo arrive bientôt !",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

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

                state.currentItem?.name?.let {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 16.dp),
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        onTextLayout = { textLayoutResult ->
                            nameWidth.floatValue =
                                textLayoutResult.size.toIntRect().size.toSize().width
                            nameHeightDp.intValue =
                                textLayoutResult.size.toIntRect().size.height
                        }
                    )
                }
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

                state.currentItem?.priceInCents?.toStringPrice()?.let {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = it,
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
        }

        if (state.currentItem?.description?.isBlank() == false) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = ShapeDefaults.Medium
                    )
                    .heightIn(min = 0.dp, max = (screenSize.height / 4))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(state = scrollState, enabled = true)
                        .padding(8.dp)
                ) {
                    state.currentItem?.description?.let {
                        Text(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Icon(
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    scrollState.scrollTo(scrollState.value - 80)
                                }
                            }
                            .alpha(if (scrollState.canScrollBackward) 1f else 0.2f),
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Scroll top",
                    )
                    Icon(
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    scrollState.scrollTo(scrollState.value + 80)
                                }
                            }
                            .alpha(if (scrollState.canScrollForward) 1f else 0.2f),
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Scroll down"
                    )
                }
            }
        }

        if (!state.currentItem?.allergens.isNullOrEmpty()) {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                text = "Liste des allèrgenes :",
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = TextDecoration.Underline
            )

            Text(
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                text = state.currentItem?.allergens?.joinToString(", ") ?: "Aucun !",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }

        if (state.currentItem?.isAvailable != true) {
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                text = "Ce produit est temporairement indisponible, nous faisons notre possible pour vous le ramener le plus vite possible !",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        } else {
            BadgedBox(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        vertical = if (state.currentItem?.allergens.isNullOrEmpty()) {
                            32.dp
                        } else {
                            8.dp
                        }
                    )
                    .graphicsLayer {
                        scaleX = scaleCart.value
                        scaleY = scaleCart.value
                    }
                    .padding(32.dp),
                badge = {
                    if (state.cartItems?.cartItems?.find { it?.name == state.currentItem?.name } != null) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            val cartItemsQuantity =
                                state.cartItems?.cartItems?.find { it?.name == state.currentItem?.name }?.quantity
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
                        state.currentItem?.let { viewModel.addCartObject(valueAsUiObject = it) }
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

    // Loading animation
    RiveAnimation(
        isLoading = showLoading,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Loading animation"
    )

    if (showError) {
        ErrorOverlay(
            isShown = true,
            duration = 3000L,
            message = "Une erreur semble être survenue lors de la mise à jour du produit.",
            onDismiss = {
                showError = false
                onProductEdited.invoke()
            }
        )
    }

}
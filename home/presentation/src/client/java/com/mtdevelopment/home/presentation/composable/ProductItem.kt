package com.mtdevelopment.home.presentation.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtdevelopment.core.presentation.R
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.toStringPrice
import com.mtdevelopment.core.util.vibratePhoneClick
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

@Preview
@Composable
fun ProductItem(
    modifier: Modifier = Modifier,
    product: UiProductObject? = null,
    onDetailClick: (UiProductObject) -> Unit = {},
    onAddClick: () -> Unit = {},
    onEditClick: (UiProductObject) -> Unit = {},
    isLoadingFinished: () -> Unit = {}
) {

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val scaleItemTile = remember { Animatable(1f) }

    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(hasLoaded) {
        if (hasLoaded) {
            coroutineScope.launch {
                isLoadingFinished.invoke()
            }
        }
    }

    fun animateAddToCart() {
        coroutineScope.launch {
            scaleItemTile.animateTo(
                1.2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            scaleItemTile.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Card(
        modifier = modifier
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scaleItemTile.value
                scaleY = scaleItemTile.value
            },
        onClick = {
            if (product != null) {
                onDetailClick.invoke(product)
            }
        },
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            GlideImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(8.dp)),
                imageModel = {
                    if (!product?.imageUrl.isNullOrBlank()) {
                        product.imageUrl
                    } else {
                        R.drawable.goats_picture_placeholder
                    }
                },
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
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
                    hasLoaded = true
                },
                success = { data, painter ->
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = "product Image",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        )

                        if (product?.isAvailable != true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.8f)
                                    .background(MaterialTheme.colorScheme.secondary),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(8.dp),
                                    text = "Produit indisponible temporairement !",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    hasLoaded = true
                })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                text = product?.name ?: "Product Name",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                product?.priceInCents?.toStringPrice()?.let {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp),
                        text = it,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                if (product?.isAvailable == true) {
                    Button(
                        modifier = Modifier
                            .width(32.dp)
                            .height(32.dp),
                        onClick = {
                            vibratePhoneClick(context)
                            animateAddToCart()
                            onAddClick.invoke()
                        },
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary,
                            disabledContentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        contentPadding = PaddingValues(4.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add"
                        )
                    }
                }
            }
        }
    }
}

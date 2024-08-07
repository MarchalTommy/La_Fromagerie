package com.mtdevelopment.home.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtdevelopment.core.util.vibratePhone
import com.mtdevelopment.home.presentation.R
import com.mtdevelopment.home.presentation.model.UiProductObject
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Preview
@Composable
fun ProductItem(
    product: UiProductObject? = null,
    onAddClick: () -> Unit = {}
) {

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp),
        onClick = { /*TODO : navigate to product details*/ },
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
                    product?.imageUrl ?: product?.imageRes ?: R.drawable.placeholder
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
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = product?.price.toString().replace(".", ",") + "€",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    onClick = {
                        vibratePhone(context)
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

@Preview
@Composable
fun MiniProductItem(
    product: UiProductObject? = null
) {
    Card(
        modifier = Modifier
            .padding(8.dp),
        onClick = {},
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
                .padding(8.dp)
        ) {
            GlideImage(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp)),
                imageModel = {
                    product?.imageUrl ?: product?.imageRes ?: R.drawable.placeholder
                },
                // TODO: FIX IMAGE SCALING
                imageOptions = ImageOptions(contentScale = ContentScale.FillHeight),
                requestBuilder = {
                    Glide.with(LocalContext.current)
                        .asBitmap()
                        .apply(
                            RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                        )
                        .transition(withCrossFade())
                },
                loading = {
                    Box(modifier = Modifier.matchParentSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                failure = {
                    Text(text = "image request failed.")
                })

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(0.2f),
                text = product?.name ?: "Product Name",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = product?.price.toString().replace(".", ",") + "€",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp),
                    onClick = {
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

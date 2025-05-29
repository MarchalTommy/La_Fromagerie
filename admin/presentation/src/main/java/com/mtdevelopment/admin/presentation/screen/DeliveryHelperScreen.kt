package com.mtdevelopment.admin.presentation.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtdevelopment.admin.presentation.BuildConfig.GOOGLE_API
import com.mtdevelopment.admin.presentation.composable.DeliveryHelperItem
import com.mtdevelopment.admin.presentation.composable.RequestPermissionsScreen
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.koinViewModel
import com.mtdevelopment.core.util.vibratePhoneClick
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DeliveryHelperScreen(
    launchDeliveryTracking: () -> Unit,
    stopDeliveryTracking: () -> Unit
) {
    // TODO: Maybe add a button to stop the delivery when it's started

    val viewModel = koinViewModel<AdminViewModel>()
    val context = LocalContext.current

    var showPermissionScreen by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val todayDate = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())

    val state = viewModel.orderScreenState.collectAsState()
    val dailyOrders = remember(state.value) {
        mutableStateOf(
            state.value.orders.filter {
                it.deliveryDate.toTimeStamp() == todayDate.toInstant().toEpochMilli()
            }
        )
    }

    val titleText = remember(dailyOrders.value) {
        if (dailyOrders.value.isEmpty()) {
            "Aucune livraison aujourd'hui !"
        } else {
            "Voici la livraison du jour !"
        }
    }

    val subtitleText = remember(dailyOrders.value) {
        if (dailyOrders.value.isEmpty()) {
            if (state.value.orders.isNotEmpty()) {
                "Prochaine livraison le ${
                    state.value.orders.map { it.deliveryDate }
                        .minByOrNull {
                            java.time.Duration.between(
                                Instant.ofEpochMilli(it.toTimeStamp()),
                                todayDate.toInstant()
                            )
                        }
                }"
            } else {
                "Aucune date de livraison n'est prévue pour l'instant."
            }
        } else {
            "${
                todayDate.dayOfWeek.getDisplayName(
                    TextStyle.FULL_STANDALONE,
                    Locale.FRANCE
                )
            } ${todayDate.toInstant().toEpochMilli().toStringDate()}"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getAllOrders()
    }

    fun onClick() {
        viewModel.onLoading(true)
        viewModel.getOptimisedPath(
            dailyOrders.value.map { it.customerAddress }
        ) { optimizedOrdersList ->
            launchDeliveryTracking.invoke()

            var link =
                "https://www.google.com/maps/dir/8_la_vessoye_25560_boujailles/"
            optimizedOrdersList.optimizedRoute.forEach {
                link =
                    link.plus(it.first).plus(",").plus(it.second).plus("/")
            }
            viewModel.onLoading(false)
            val intent = Intent(Intent.ACTION_VIEW, link.toUri())
            context.startActivity(intent)
        }
    }

    if (showPermissionScreen && !permissionsGranted) {
        RequestPermissionsScreen(
            shouldShowOptimization = state.value.shouldShowBatterieOptimization,
            onPermissionsGrantedAndConfigured = {
                permissionsGranted = true
                showPermissionScreen = false
                viewModel.updateShouldShowBatterieOptimization(false)
                onClick()
            },
            onPermissionsProcessSkippedOrDenied = {
                showPermissionScreen = false // User chose not to grant or skip
                viewModel.onError("Permission refusée, nous ne pourrons pas vous aider dans la livraison.")
                // Optionally show a message that tracking won't work
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitleText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    if (dailyOrders.value.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                text = "Pas de commande à livrer.\n\nUn jour calme, parfois, c'est pas mal !"
                            )
                        }
                    } else {
                        dailyOrders.value.groupBy { it.customerAddress }
                            .forEach { address, orders ->
                                item {
                                    // Avoiding duplicates in case of multiple orders by 1 person
                                    val names = orders.map { it.customerName }.toSet()
                                    val stringNames = names.joinToString(", ")
                                        .replace("[", "")
                                        .replace("]", "")
                                        .trim()

                                    // Grouping orders by address to the same entry in the list
                                    DeliveryHelperItem(
                                        name = stringNames,
                                        address = address
                                    )
                                }
                            }
                    }
                }
            }
            if (dailyOrders.value.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    val markersPositions = dailyOrders.value.map { it.customerAddress }.toSet()
                    val markersString = markersPositions.joinToString("&") { markerAddress ->
                        "markers=label:${dailyOrders.value.find { it.customerAddress == markerAddress }?.customerName?.trim()}|${markerAddress}"
                    }

                    GlideImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        imageModel = {
                            "https://maps.googleapis.com/maps/api/staticmap?" +
                                    "size=400x260&" +
                                    "scale=2&" +
                                    "maptype=roadmap&" +
                                    markersString +
                                    "&key=${GOOGLE_API}"
                        },
                        imageOptions = ImageOptions(contentScale = ContentScale.Fit),
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
                }

                Button(
                    modifier = Modifier,
                    onClick = {
                        vibratePhoneClick(context = context)
                        showPermissionScreen = true
                        if (permissionsGranted) {
                            onClick()
                        }
                    },
                    shape = Shapes().medium
                ) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = "Livraison")

                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        text = "Démarrer la livraison"
                    )
                }
            }
        }

        // Loading animation
        RiveAnimation(
            isLoading = state.value.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )

        // Error
        ErrorOverlay(
            isShown = !state.value.error.isNullOrEmpty(),
            message = state.value.error ?: "Une erreur est survenue",
            onDismiss = {
                viewModel.onError("")
            }
        )
    }
}
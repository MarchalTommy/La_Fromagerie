package com.mtdevelopment.admin.presentation.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.common.util.CollectionUtils.listOf
import com.mtdevelopment.admin.presentation.BuildConfig.GOOGLE_API
import com.mtdevelopment.admin.presentation.composable.DeliveryAddDialog
import com.mtdevelopment.admin.presentation.composable.DeliveryHelperItem
import com.mtdevelopment.admin.presentation.model.DeliverHelperDialogState
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.domain.toTimeStamp
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.koinViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Collections.emptyList
import java.util.Locale

// --- 1. Smart Composable (Controller) ---
// This composable handles logic, state, and ViewModel interactions.
@Composable
fun DeliveryHelperScreen(
    launchDeliveryTracking: () -> Unit = {},
    stopDeliveryTracking: () -> Unit = {}
) {
    val viewModel = koinViewModel<AdminViewModel>()
    val context = LocalContext.current
    val state = viewModel.orderScreenState.collectAsState()

    // State derived from the ViewModel state
    val todayDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault())

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

    val nextOrderDate = remember(state.value.orders) {
        if (state.value.orders.isNotEmpty()) {
            val filteredValue = state.value.orders.filter {
                it.deliveryDate.toTimeStamp() > todayDate.toInstant().toEpochMilli()
            }
            if (filteredValue.isNotEmpty()) {
                filteredValue.sortedBy { it.deliveryDate.toTimeStamp() }[0]
            } else {
                null
            }
        } else {
            null
        }
    }

    val subtitleText = remember(dailyOrders.value, state.value.orders) {
        if (dailyOrders.value.isEmpty()) {
            if (state.value.orders.isNotEmpty() && nextOrderDate != null
            ) {
                "Prochaine livraison le ${
                    nextOrderDate.deliveryDate
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

    // Effect to fetch initial data
    LaunchedEffect(Unit) {
        viewModel.getAllOrders()
        viewModel.getTrackingStatus()
    }

    fun startDelivery() {
        viewModel.onLoading(true)
        viewModel.getCurrentLocationToStart()
        viewModel.getOptimisedPath(dailyOrders.value.map { it.customerAddress }) { optimizedOrdersList ->
            launchDeliveryTracking()

            var link =
                "https://www.google.com/maps/dir/${state.value.currentAdminLocation?.latitude},${state.value.currentAdminLocation?.longitude}/"

            optimizedOrdersList.optimizedRoute.forEach {
                link =
                    link.plus(it.first).plus(",").plus(it.second).plus("/")
            }
            viewModel.onLoading(false)
            val intent = Intent(Intent.ACTION_VIEW, link.toUri())
            context.startActivity(intent)
        }
    }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startDelivery()
            } else {
                viewModel.onError("Permission de localisation refusée")
            }
        }

    // --- Event Handlers ---
    fun onStartDeliveryClick() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                startDelivery()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun onStopDeliveryClick() {
        stopDeliveryTracking()
    }

    // --- UI ---
    // Pass all the calculated state and event handlers to the dumb composable
    DeliveryHelperScreenContent(
        titleText = titleText,
        subtitleText = subtitleText,
        dailyOrders = dailyOrders.value,
        isInTracking = state.value.isInTrackingMode,
        isLoading = state.value.isLoading,
        error = state.value.error,
        onStartDeliveryClick = ::onStartDeliveryClick,
        onStopDeliveryClick = ::onStopDeliveryClick,
        dialogState = DeliverHelperDialogState(
            onDismissError = { viewModel.onError("") },
            autocompleteSearchQuery = state.value.searchQuery,
            suggestions = state.value.suggestions,
            autocompleteShowDropdown = state.value.showSuggestions,
            autocompleteOnValueChange = { viewModel.setAddressText(it) },
            autocompleteOnDropDownDismiss = { viewModel.setShowAddressesSuggestions(false) },
            autocompleteOnSuggestionSelected = { viewModel.onSuggestionSelected(it) },
            dialogOnConfirm = { viewModel.addOrder(it) },
            dialogOnShow = { viewModel.setDialogVisibility(true) },
            dialogOnDismiss = { viewModel.setDialogVisibility(false) },
            shouldShowDialog = state.value.shouldShowDialog
        )
    )
}

// --- 2. Dumb Composable (View) ---
// This composable only displays UI based on the parameters it receives.
// It has no ViewModel, no context (except for Glide), and no complex logic.
@Composable
fun DeliveryHelperScreenContent(
    titleText: String,
    subtitleText: String,
    dailyOrders: List<Order>,
    isInTracking: Boolean,
    isLoading: Boolean,
    error: String?,
    onStartDeliveryClick: () -> Unit,
    onStopDeliveryClick: () -> Unit,
    dialogState: DeliverHelperDialogState? = null
) {
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
            Box {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    contentPadding = PaddingValues(bottom = 92.dp)
                ) {
                    if (dailyOrders.isEmpty()) {
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
                        dailyOrders.groupBy { it.customerAddress }
                            .forEach { (address, orders) ->
                                item {
                                    val names =
                                        orders.map { it.customerName }.distinct().joinToString(", ")
                                    DeliveryHelperItem(
                                        name = names,
                                        address = address,
                                        note = orders.firstNotNullOfOrNull { it.note }
                                    )
                                }
                            }
                    }
                }

                FloatingActionButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { dialogState?.dialogOnShow?.invoke() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter un arrêt"
                    )
                }
            }
        }

        if (dailyOrders.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                val markersPositions = dailyOrders.map { it.customerAddress }.toSet()
                val markersString = markersPositions.joinToString("&") { markerAddress ->
                    "markers=label:${dailyOrders.find { it.customerAddress == markerAddress }?.customerName?.trim()}|${markerAddress}"
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
                onClick = {
                    if (isInTracking) onStopDeliveryClick() else onStartDeliveryClick()
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Default.Place, contentDescription = "Livraison")
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = if (isInTracking) "Arrêter la livraison" else "Démarrer la livraison"
                )
            }
        }
    }

    // Loading animation
    RiveAnimation(
        isLoading = isLoading,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Loading animation"
    )

    // Error
    ErrorOverlay(
        isShown = !error.isNullOrEmpty(),
        message = error ?: "Une erreur est survenue",
        onDismiss = {
            dialogState?.onDismissError()
        }
    )

    if (dialogState?.shouldShowDialog == true) {
        DeliveryAddDialog(
            suggestions = dialogState.suggestions,
            searchQuery = dialogState.autocompleteSearchQuery,
            showDropdown = dialogState.autocompleteShowDropdown,
            onValueChange = { value ->
                dialogState.autocompleteOnValueChange(value)
            },
            onSuggestionSelected = {
                dialogState.autocompleteOnSuggestionSelected(it)
            },
            onDropDownDismiss = {
                dialogState.autocompleteOnDropDownDismiss()
            },
            onDismiss = {
                dialogState.dialogOnDismiss()
            },
            onConfirm = { tempOrder ->
                dialogState.dialogOnConfirm(tempOrder)
            },
            onError = {
//                dialogState.onDismissError.invoke()
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun DeliveryHelperScreenPreview_WithOrders() {
    MaterialTheme {
        DeliveryHelperScreenContent(
            titleText = "Voici la livraison du jour !",
            subtitleText = "mardi 1 juillet 2025",
            dailyOrders = listOf(
                Order(
                    id = "order-001",
                    customerName = "Jean Dupont",
                    customerAddress = "1 Rue de la Paix, 75002 Paris",
                    customerBillingAddress = "1 Rue de la Paix, 75002 Paris",
                    deliveryDate = "01/07/2025",
                    orderDate = "28/06/2025",
                    products = mapOf("product-a" to 2, "product-b" to 1),
                    status = OrderStatus.PENDING,
                    note = "Code d'entrée 1234. Laisser le colis devant la porte."
                ),
                Order(
                    id = "order-002",
                    customerName = "Marie Curie",
                    customerAddress = "5 Rue Cuvier, 75005 Paris",
                    customerBillingAddress = "22 Rue de l'Estrapade, 75005 Paris",
                    deliveryDate = "01/07/2025",
                    orderDate = "29/06/2025",
                    products = mapOf("product-c" to 5),
                    status = OrderStatus.PENDING,
                    note = null
                ),
                Order(
                    id = "order-003",
                    customerName = "Gustave Eiffel",
                    customerAddress = "Champ de Mars, 75007 Paris",
                    customerBillingAddress = "Champ de Mars, 75007 Paris",
                    deliveryDate = "03/07/2025",
                    orderDate = "30/06/2025",
                    products = mapOf("product-a" to 10, "product-d" to 1),
                    status = OrderStatus.PENDING,
                    note = "Livraison pour le chantier."
                ),
                Order(
                    id = "order-004",
                    customerName = "Victor Hugo",
                    customerAddress = "6 Place des Vosges, 75004 Paris",
                    customerBillingAddress = "6 Place des Vosges, 75004 Paris",
                    deliveryDate = "25/06/2025",
                    orderDate = "22/06/2025",
                    products = mapOf("product-e" to 1),
                    status = OrderStatus.DELIVERED,
                    note = "Colis remis en main propre."
                ),
                Order(
                    id = "order-005",
                    customerName = "Édith Piaf",
                    customerAddress = "5 Rue Crespin du Gast, 75011 Paris",
                    customerBillingAddress = "5 Rue Crespin du Gast, 75011 Paris",
                    deliveryDate = "08/07/2025",
                    orderDate = "01/07/2025",
                    products = mapOf("product-b" to 3),
                    status = OrderStatus.IN_PREPARATION,
                    note = "Commande annulée par le client."
                )
            ),
            isInTracking = false,
            isLoading = false,
            error = null,
            onStartDeliveryClick = {},
            onStopDeliveryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun DeliveryHelperScreenPreview_NoOrders() {
    MaterialTheme {
        DeliveryHelperScreenContent(
            titleText = "Aucune livraison aujourd'hui !",
            subtitleText = "Prochaine livraison le 2025-07-05",
            dailyOrders = emptyList(),
            isInTracking = false,
            isLoading = false,
            error = "This is an error",
            onStartDeliveryClick = {},
            onStopDeliveryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun DeliveryHelperScreenPreview_Loading() {
    MaterialTheme {
        DeliveryHelperScreenContent(
            titleText = "Aucune livraison aujourd'hui !",
            subtitleText = "mardi 1 juillet 2025",
            dailyOrders = emptyList(),
            isInTracking = false,
            isLoading = true,
            error = null,
            onStartDeliveryClick = {},
            onStopDeliveryClick = {},
        )
    }
}
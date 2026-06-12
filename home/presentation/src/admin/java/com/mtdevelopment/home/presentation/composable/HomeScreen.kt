package com.mtdevelopment.home.presentation.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.rive.runtime.kotlin.core.Rive
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtdevelopment.admin.presentation.composable.ProductEditDialog
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.util.koinViewModel
import com.mtdevelopment.home.presentation.viewmodel.HomeViewModel

/**
 * Main dashboard screen for the administrator.
 * Displays the product catalog with advanced management capabilities:
 * 1. Product Management: Add, Edit, Delete, and toggle Availability of products.
 * 2. Order Management: Navigate to the order preparation screen.
 * 3. Navigation: Quick access to the delivery/path management screen.
 */
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    cartViewModel: CartViewModel,
    shouldRefresh: Boolean,
    navigateToDetail: () -> Unit = {},
    navigateToDelivery: () -> Unit = {},
    navigateToOrders: () -> Unit = {}
) {
    val context = LocalContext.current

    val homeViewModel = koinViewModel<HomeViewModel>()
    val adminViewModel = koinViewModel<AdminViewModel>()

    val homeState by homeViewModel.homeUiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var editedProduct by remember { mutableStateOf<UiProductObject?>(null) }

    // Track image loading to signal splash screen removal
    var hasLoadedFirstPic by remember { mutableStateOf(false) }

    LaunchedEffect(hasLoadedFirstPic) {
        if (hasLoadedFirstPic) {
            mainViewModel.setCanRemoveSplash()
        }
    }

    LaunchedEffect(Unit) {
        Rive.init(context)
    }

    // Manual refresh handling
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            homeViewModel.refreshProducts()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Product Catalog Grid
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
                            bottom = 64.dp // Avoid FAB coverage
                        )
                    } else {
                        Modifier
                    },
                    product = productListItem,
                    onDetailClick = {
                        cartViewModel.saveClickedItem(it)
                        navigateToDetail.invoke()
                    },
                    onEditClick = {
                        // Open edit dialog with selected product info
                        showEditDialog = true
                        editedProduct = it
                    },
                    isLoadingFinished = {
                        if (!hasLoadedFirstPic) {
                            hasLoadedFirstPic = true
                        }
                    },
                    onAvailabilityChange = {
                        // Quick toggle for product availability
                        adminViewModel.updateProduct(
                            product = it.copy(isAvailable = !it.isAvailable), onSuccess = {
                                editedProduct = null
                                homeViewModel.refreshProducts()
                            }, onError = {
                                mainViewModel.setError(
                                    "Une erreur est survenue lors de la modification de disponibilité du produit"
                                )
                            }, onLoading = { isLoading ->
                                homeViewModel.setIsLoading(isLoading)
                            }
                        )
                    }
                )
            }
        }

        // ADD PRODUCT BUTTON
        FloatingActionButton(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.BottomStart)
                .border(
                    3.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(16.dp)
                ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiary,
            onClick = {
                editedProduct = null // Ensure dialog starts in "Add" mode
                showEditDialog = true
            }
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add a product")
        }

        // PREPARE ORDERS BUTTON: Navigation to OrderPreparationScreen
        ExtendedFloatingActionButton(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.BottomCenter)
                .border(
                    3.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(16.dp)
                ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiary,
            onClick = {
                navigateToOrders.invoke()
            }
        ) {
            Text(
                text = "Commandes",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // NEXT SCREEN BUTTON: Navigation to path management/delivery helper
        FloatingActionButton(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.BottomEnd)
                .border(
                    3.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(16.dp)
                ),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiary,
            onClick = {
                navigateToDelivery.invoke()
            }
        ) {
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
        }


        /**
         * Unified Dialog for Adding and Editing products.
         */
        if (showEditDialog) {
            ProductEditDialog(
                product = editedProduct,
                onValidate = {
                    if (editedProduct != null) {
                        // Edit existing product
                        adminViewModel.updateProduct(product = it, onSuccess = {
                            editedProduct = null
                            homeViewModel.refreshProducts()
                        }, onError = {
                            mainViewModel.setError(
                                "Une erreur est survenue lors de la modification du produit"
                            )
                        }, onLoading = { isLoading ->
                            homeViewModel.setIsLoading(isLoading)
                        })
                    } else {
                        // Add new product
                        adminViewModel.addNewProduct(product = it, onSuccess = {
                            editedProduct = null
                            homeViewModel.refreshProducts()
                        }, onError = {
                            mainViewModel.setError(
                                "Une erreur est survenue lors de l'ajout du produit"
                            )
                        }, onLoading = { isLoading ->
                            homeViewModel.setIsLoading(isLoading)
                        })

                    }
                },
                onDelete =
                    if (editedProduct != null) {
                        {
                            adminViewModel.deleteProduct(product = it)
                            editedProduct = null
                            homeViewModel.refreshProducts()
                        }
                    } else {
                        null // Delete not available for new products
                    },
                onDismiss = {
                    showEditDialog = false
                    editedProduct = null
                },
                onError = {
                    mainViewModel.setError(it)
                },
                shouldShowLoading = {
                    homeViewModel.setIsLoading(it)
                },
            )
        }

        // Loading Overlay
        RiveAnimation(
            isLoading = homeState.isLoading,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Loading animation"
        )
    }
}
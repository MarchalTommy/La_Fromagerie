package com.mtdevelopment.delivery.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.domain.frenchDayName
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import org.koin.androidx.compose.koinViewModel

/**
 * Saved-state key the editor sets on this screen's back-stack entry after a write, so the
 * list refreshes once instead of re-fetching on every resume. Mirrors
 * [PATH_LIST_NEEDS_REFRESH]; the two screens either side of the editor are what agree on it.
 */
const val PICKUP_LIST_NEEDS_REFRESH = "pickup_list_needs_refresh"

/**
 * Lists the places an order can be collected: the shop, then every market date.
 *
 * Reached from the delivery-path screen rather than from the admin home, so all of the
 * "where and when do I sell" configuration sits together and the home screen keeps the
 * three buttons it already has.
 */
@Composable
fun PickupPointsScreen(
    pointsChanged: Boolean = false,
    onPointsChangeHandled: () -> Unit = {},
    navigateToPointEdit: (String?) -> Unit = {}
) {
    val viewModel = koinViewModel<AdminViewModel>()
    val state by viewModel.pickupPointsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAllPickupPoints()
    }

    LaunchedEffect(pointsChanged) {
        if (pointsChanged) {
            viewModel.getAllPickupPoints()
            onPointsChangeHandled()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (state.points.isEmpty() && !state.isLoading && state.error == null) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    text = "Aucun point de retrait pour l'instant.\n" +
                            "Ajoutez la boutique ou une date de marché.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = state.points, key = { it.id }) { point ->
                    PickupPointCard(
                        point = point,
                        onClick = { navigateToPointEdit(point.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp),
                onClick = { navigateToPointEdit(null) }
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Ajouter un point de retrait")
            }

            RiveAnimation(
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Loading animation"
            )

            ErrorOverlay(
                isShown = state.error != null,
                message = state.error,
                onDismiss = { viewModel.clearPickupPointError() }
            )
        }
    }
}

@Composable
private fun PickupPointCard(
    point: PickupPoint,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = point.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = when (point.type) {
                        PickupPointType.SHOP -> "Boutique"
                        PickupPointType.MARKET -> "Marché"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = point.address,
                style = MaterialTheme.typography.bodyMedium
            )

            // A locale lookup per opening day, plus two joins, for a card that redraws
            // whenever the list around it does. The point is what it depends on.
            val schedule = remember(point) {
                val days = when (point.type) {
                    PickupPointType.SHOP -> point.openingDays
                        .mapNotNull { frenchDayName(it) }
                        .joinToString(", ")

                    PickupPointType.MARKET -> point.date.orEmpty()
                }
                listOf(days, point.timeRange)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
            }

            if (schedule.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Closures are the only way a recurring shop can be shut, so they are worth
            // showing on the card rather than hiding one tap away in the editor.
            if (point.closedDates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${point.closedDates.size} fermeture(s) programmée(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

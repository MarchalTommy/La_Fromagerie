package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.presentation.composable.PrimaryButton

/**
 * Composable for the cart's footer, displaying the total amount and a button to proceed to checkout.
 * It also handles the display of a message when the device is offline.
 * 
 * @param modifier Modifier for the layout.
 * @param totalAmount The formatted total price of the cart.
 * @param hasItems Whether the cart has items (controls visibility).
 * @param canShowDelivery Whether the device is connected to the internet, allowing the user to proceed.
 * @param onPayClick Callback when the checkout button is clicked.
 */
@Composable
fun CartFooter(
    modifier: Modifier,
    totalAmount: String,
    hasItems: Boolean,
    canShowDelivery: Boolean,
    onPayClick: () -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = hasItems,
        exit = fadeOut(animationSpec = tween(150)),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total : ",
                    modifier = Modifier
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = totalAmount,
                    modifier = Modifier
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Show checkout button only if online, otherwise show a friendly offline message.
            if (canShowDelivery) {
                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = "Choisir une date de livraison",
                    onClick = { onPayClick.invoke() }
                )
            } else {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    text = "Vous ne semblez pas être connecté à internet",
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    text = "mais nous en avons besoins pour passer la commande !",
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    text = "On vous laisse trouver une connexion, on patiente sagement...",
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}
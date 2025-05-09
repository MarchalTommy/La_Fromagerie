package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
            if (canShowDelivery) {
                Button(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.secondary
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { onPayClick.invoke() }) {
                    Text(text = "Choisir une date de livraison")
                }
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
package com.mtdevelopment.home.presentation.composable.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CartEmptyMessage(
    alphaAnimation: Animatable<Float, AnimationVector1D>
) {
    Text(
        "C'est bien vide d'ailleurs...\nOn commande un p'tit fromage ?",
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alphaAnimation.value
            },
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleLarge
    )
}
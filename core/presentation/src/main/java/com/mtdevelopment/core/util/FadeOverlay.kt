package com.mtdevelopment.core.util

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Heavily inspired by
// https://gist.github.com/dovahkiin98/85acb72ab0c4ddfc6b53413c955bcd10
// But adapted to my needs
@Composable
fun FadeOverlay(
    modifier: Modifier = Modifier,
    overlayWidth: Dp = 32.dp,
    content: @Composable () -> Unit
) {
    val color = MaterialTheme.colorScheme.background

    Box(modifier = modifier.drawWithContent {
        drawContent()
        val width = overlayWidth.toPx()

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    color,
                    Color.Transparent,
                ),
                startX = 0f,
                endX = width,
            ),
            size = Size(
                width,
                this.size.height,
            )
        )

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    color,
                ),
                startX = size.width - width,
                endX = size.width,
            ),
            topLeft = Offset(x = size.width - width, y = 0f),
        )
    }) {
        content()
    }
}
package com.mtdevelopment.core.presentation.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The application's primary call-to-action button.
 *
 * Mirrors the "Continuer" button from the "Artisan Order Prep" design system:
 * a full-width, pill-shaped ([CircleShape]) filled button using the primary color,
 * white label, an elevated shadow, an optional trailing icon (an arrow by default),
 * and a subtle scale-down when pressed (the `active:scale-95` interaction).
 *
 * Use this in place of rectangular filled action buttons across the customer flow so
 * that every "next step / validate / pay" affordance reads as one consistent control.
 * Square/icon buttons are intentionally out of scope and keep their own shape.
 *
 * @param text The button label.
 * @param onClick Invoked on tap when [enabled].
 * @param modifier Layout modifier (defaults to full width).
 * @param enabled Whether the button is interactive.
 * @param trailingIcon Optional icon shown after the label. Pass `null` to hide it
 *   (e.g. for a payment action that is not a "continue" step). Defaults to a
 *   forward arrow to signal progression.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowForward
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "primaryButtonScale"
    )

    Button(
        modifier = modifier.scale(scale),
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.surface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null
            )
        }
    }
}

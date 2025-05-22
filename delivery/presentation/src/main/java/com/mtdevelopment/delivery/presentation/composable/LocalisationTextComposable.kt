package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.delivery.presentation.R
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.DayOfWeek

@Composable
fun LocalisationTextComposable(
    selectedPath: UiDeliveryPath?,
    geolocIsOnPath: Boolean,
    canAskForDelivery: Boolean,
    userCity: String
) {
    val isCyclingTextMode = selectedPath == null && !canAskForDelivery

    val cyclingTextResourceIds = remember {
        listOf(
            R.string.auto_geoloc_not_on_path_at_all_part_1,
            R.string.auto_geoloc_not_on_path_at_all_part_2,
            R.string.auto_geoloc_not_on_path_at_all_part_3
        )
    }

    // État pour l'ID de la ressource de texte actuellement affichée en mode cycle.
    // Réinitialisé au premier texte si isCyclingTextMode change.
    var currentCyclingTextResId by remember(isCyclingTextMode) {
        mutableStateOf(cyclingTextResourceIds[0])
    }

    // État pour l'index du texte actuel dans le cycle.
    // Réinitialisé à 0 si isCyclingTextMode change.
    var cyclingTextIndex by remember(isCyclingTextMode) {
        mutableStateOf(0)
    }

    val textAlpha = remember { Animatable(1f) }

    LaunchedEffect(isCyclingTextMode) {
        if (isCyclingTextMode) {
            currentCyclingTextResId =
                cyclingTextResourceIds[cyclingTextIndex] // S'assure que le texte est le bon
            textAlpha.snapTo(1f)

            while (true) {
                delay(3800L)
                if (!this.isActive) break

                // Fondu sortant (fade-out)
                textAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                )
                if (!this.isActive) break

                // Passer au texte suivant dans le cycle
                cyclingTextIndex = (cyclingTextIndex + 1) % cyclingTextResourceIds.size
                currentCyclingTextResId = cyclingTextResourceIds[cyclingTextIndex]

                // Fondu entrant (fade-in) pour le nouveau texte
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
                )
            }
        } else {
            // Si on n'est pas en mode cycle, s'assurer que le texte est pleinement visible
            textAlpha.snapTo(1f)
            // cyclingTextIndex et currentCyclingTextResId sont déjà réinitialisés
            // grâce à la clé `isCyclingTextMode` dans leur `remember`.
        }
    }

    val textToDisplay: String = when {
        isCyclingTextMode -> {
            stringResource(id = currentCyclingTextResId)
        }

        geolocIsOnPath && selectedPath != null -> {
            stringResource(
                R.string.auto_geoloc_success,
                userCity,
                DayOfWeek.valueOf(selectedPath.deliveryDay).getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.FRENCH
                )
            )
        }

        selectedPath != null -> {
            stringResource(
                R.string.manual_path_chosen,
                DayOfWeek.valueOf(selectedPath.deliveryDay).getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.FRENCH
                )
            )
        }

        else -> {
            stringResource(R.string.auto_geoloc_not_on_path_but_close)
        }
    }

    Text(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .graphicsLayer(alpha = textAlpha.value),
        text = textToDisplay,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}
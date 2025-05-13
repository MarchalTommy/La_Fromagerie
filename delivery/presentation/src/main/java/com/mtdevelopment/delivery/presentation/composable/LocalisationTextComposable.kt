package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.delivery.presentation.R
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath

@Composable
fun LocalisationTextComposable(
    selectedPath: UiDeliveryPath?,
    geolocIsOnPath: Boolean,
    canAskForDelivery: Boolean,
    userCity: String
) {
    Text(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        text = when {
            selectedPath != null -> stringResource(
                R.string.manual_path_chosen,
                selectedPath.deliveryDay
            )

            geolocIsOnPath -> stringResource(
                R.string.auto_geoloc_success,
                userCity
            )

            canAskForDelivery -> {
                // TODO: Add a way to send a quick message to EARL. Answer the user via notification ?
                stringResource(
                    R.string.auto_geoloc_not_on_path_but_close
                )
            }

            !canAskForDelivery -> {
                // TODO: Add a way to warn that some people asks for this city
                stringResource(
                    R.string.auto_geoloc_not_on_path_at_all
                )
            }

            else -> "ERROR"
        },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}
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

@Composable
fun LocalisationTextComposable(
    selectedPath: com.mtdevelopment.delivery.presentation.model.UiDeliveryPath?,
    geolocIsOnPath: Boolean,
    userCity: String
) {
    Text(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        text = when {
            selectedPath != null -> stringResource(
                R.string.manual_path_chosen,
                selectedPath.name
            )

            geolocIsOnPath -> stringResource(
                R.string.auto_geoloc_success,
                userCity
            )

            !geolocIsOnPath -> stringResource(
                R.string.auto_geoloc_not_on_path
            )

            else -> "ERROR"
        },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}
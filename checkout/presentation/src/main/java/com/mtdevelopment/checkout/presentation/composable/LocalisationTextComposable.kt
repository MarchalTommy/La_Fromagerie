package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.checkout.presentation.R
import com.mtdevelopment.checkout.presentation.model.DeliveryPath

@Composable
fun LocalisationTextComposable(
    selectedPath: State<DeliveryPath?>?,
    geolocIsOnPath: State<Boolean>,
    userCity: State<String?>
) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        text = when {
            selectedPath?.value != null -> stringResource(
                R.string.manual_path_chosen,
                selectedPath.value?.pathName ?: "Unknown"
            )

            geolocIsOnPath.value -> stringResource(
                R.string.auto_geoloc_success,
                userCity.value?: "ERREUR"
            )

            !geolocIsOnPath.value && selectedPath?.value == null -> stringResource(
                R.string.auto_geoloc_not_on_path
            )

            else -> "ERROR"
        },
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}
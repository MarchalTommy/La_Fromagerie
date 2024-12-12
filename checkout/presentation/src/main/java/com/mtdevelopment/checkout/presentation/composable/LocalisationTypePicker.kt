package com.mtdevelopment.checkout.presentation.composable

import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mtdevelopment.checkout.presentation.R
import com.mtdevelopment.checkout.presentation.model.UiDeliveryPath

@Composable
fun LocalisationTypePicker(
    selectedPath: UiDeliveryPath?,
    localisationSuccess: Boolean,
    showDeliverySelection: () -> Unit,
    shouldAskLocalisationPermission: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedPath == null && !localisationSuccess && Geocoder.isPresent()) {
            TextButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                onClick = {
                    shouldAskLocalisationPermission.invoke()
                },
                content = {
                    Text(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.CenterVertically),
                        text = stringResource(R.string.auto_locate),
                        textAlign = TextAlign.Center
                    )
                })

            VerticalDivider(
                modifier = Modifier
                    .padding(8.dp)
                    .height(64.dp),
                thickness = Dp.Hairline
            )
        }

        TextButton(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),
            onClick = {
                showDeliverySelection()
            },
            content = {
                Text(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically),
                    text = stringResource(R.string.manually_select_delivery_path),
                    textAlign = TextAlign.Center
                )
            })
    }
}
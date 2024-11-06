package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mtdevelopment.checkout.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTextField(
    shouldBeClickable: Boolean,
    datePickerState: DatePickerState,
    dateFieldText: String,
    shouldShowDatePicker: () -> Unit = {},
    newDateFieldText: (String) -> Unit = {}
) {
    OutlinedTextField(
        modifier = if (shouldBeClickable) {
            Modifier.clickable {
                shouldShowDatePicker.invoke()
            }.padding(horizontal = 8.dp).fillMaxWidth()
        } else {
            Modifier.padding(horizontal = 8.dp).fillMaxWidth()
        },
        value = if (datePickerState.selectableDates.isSelectableDate(
                datePickerState.selectedDateMillis ?: 0L
            )
        ) {
            dateFieldText
        } else {
            ""
        },
        onValueChange = { newValue ->
            newDateFieldText.invoke(newValue)
        },
        enabled = false,
        label = {
            Text(
                if (shouldBeClickable) {
                    stringResource(R.string.delivery_date_label)
                } else {
                    "Sélectionnez un parcours pour choisir une date"
                },
                Modifier.background(Color.Transparent)
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = ShapeDefaults.Medium,
        colors = OutlinedTextFieldDefaults.colors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        leadingIcon = {
            Icon(Icons.Rounded.DateRange, "")
        }
    )
}
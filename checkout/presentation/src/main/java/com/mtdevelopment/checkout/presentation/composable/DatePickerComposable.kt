package com.mtdevelopment.checkout.presentation.composable

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerComposable(
    datePickerVisibility: MutableState<Boolean>,
    dateFieldText: MutableState<String>,
    datePickerState: DatePickerState
) {
    androidx.compose.material3.DatePickerDialog(
        modifier = Modifier,
        onDismissRequest = {
            datePickerVisibility.value = false
        },
        confirmButton = {
            Text("OK", Modifier.padding(16.dp).clickable {
                dateFieldText.value = datePickerState.selectedDateMillis?.let {
                    SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(it)
                } ?: ""
                datePickerVisibility.value = false
            })
        },
        dismissButton = {
            Text("Annuler", Modifier.padding(16.dp).clickable {
                datePickerVisibility.value = false
            })
        },
        shape = ShapeDefaults.Medium,
        tonalElevation = 24.dp,
        colors = DatePickerDefaults.colors(),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true
        ),
        content = {
            DatePicker(
                modifier = Modifier,
                state = datePickerState,
                showModeToggle = false,
                title = {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, start = 16.dp),
                        text = "Quand souhaitez-vous être livré ?"
                    )
                }
            )
        }
    )
}
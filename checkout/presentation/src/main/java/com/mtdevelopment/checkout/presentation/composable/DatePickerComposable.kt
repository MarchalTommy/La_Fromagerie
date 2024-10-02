package com.mtdevelopment.checkout.presentation.composable

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mtdevelopment.checkout.presentation.model.ShippingDefaultSelectableDates
import java.util.Calendar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getDatePickerState(
    shippingSelectableDates: SelectableDates
): DatePickerState {

    val nextSelectableDatesList = remember { mutableStateListOf<Long>() }

    val calendar = Calendar.getInstance()
    val nextJanuary = Calendar.getInstance()
    val thisDecember = Calendar.getInstance()
    val nextSelectableDate = Calendar.getInstance()
    val tomorrow = Calendar.getInstance()

    nextJanuary.set(calendar.get(Calendar.YEAR) + 1, 1, 14)

    thisDecember.set(calendar.get(Calendar.YEAR), 11, 1)

    return if (shippingSelectableDates != ShippingDefaultSelectableDates()) {
        nextSelectableDatesList.clear()

        for (days in calendar.get(Calendar.DAY_OF_YEAR) + 2..365) {
            nextSelectableDate.set(Calendar.DAY_OF_YEAR, days)

            if (shippingSelectableDates.isSelectableDate(nextSelectableDate.timeInMillis)) {
                nextSelectableDatesList.add(nextSelectableDate.timeInMillis)
            }
        }

        rememberDatePickerState(
            initialSelectedDateMillis = nextSelectableDatesList.firstOrNull(),
            yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
            } else {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
            },
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = shippingSelectableDates
        )
    } else {
        tomorrow.set(Calendar.DAY_OF_YEAR, (calendar.get(Calendar.DAY_OF_YEAR) + 1))
        rememberDatePickerState(
            initialSelectedDateMillis = tomorrow.timeInMillis,
            yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
            } else {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
            },
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = ShippingDefaultSelectableDates()
        )
    }
}
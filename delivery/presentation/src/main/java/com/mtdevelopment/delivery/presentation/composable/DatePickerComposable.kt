package com.mtdevelopment.delivery.presentation.composable

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerComposable(
    datePickerState: DatePickerState,
    shouldRemoveDatePicker: () -> Unit,
    newDateFieldText: (String) -> Unit,
    onDateSelected: (Long) -> Unit = {}
) {
    androidx.compose.material3.DatePickerDialog(
        modifier = Modifier,
        onDismissRequest = {
            shouldRemoveDatePicker.invoke()
        },
        confirmButton = {
            Text("OK",
                Modifier
                    .padding(16.dp)
                    .clickable {
                        newDateFieldText.invoke(datePickerState.selectedDateMillis?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(it)
                        } ?: "")
                        onDateSelected.invoke(datePickerState.selectedDateMillis ?: 0)
                        shouldRemoveDatePicker.invoke()
                    })
        },
        dismissButton = {
            Text("Annuler",
                Modifier
                    .padding(16.dp)
                    .clickable {
                        shouldRemoveDatePicker.invoke()
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
fun getDatePickerState(
    selectedPath: com.mtdevelopment.delivery.presentation.model.UiDeliveryPath?
): DatePickerState {

    val shippingSelectableDates = if (selectedPath != null) {
        com.mtdevelopment.delivery.presentation.model.ShippingSelectableDatesTest(selectedPath.deliveryDay)
    } else {
        com.mtdevelopment.delivery.presentation.model.ShippingDefaultSelectableDates()
    }

    val nextSelectableDatesList = mutableStateListOf<Long>()

    val calendar = Calendar.getInstance()
    val nextJanuary = Calendar.getInstance()
    val thisDecember = Calendar.getInstance()
    val nextSelectableDate = Calendar.getInstance()
    val tomorrow = Calendar.getInstance()

    nextJanuary.set(calendar.get(Calendar.YEAR) + 1, 1, 14)

    thisDecember.set(calendar.get(Calendar.YEAR), 11, 1)

    return if (shippingSelectableDates != com.mtdevelopment.delivery.presentation.model.ShippingDefaultSelectableDates()) {
        nextSelectableDatesList.clear()

        for (days in calendar.get(Calendar.DAY_OF_YEAR) + 2..365) {
            nextSelectableDate.set(Calendar.DAY_OF_YEAR, days)

            if (shippingSelectableDates.isSelectableDate(nextSelectableDate.timeInMillis)) {
                nextSelectableDatesList.add(nextSelectableDate.timeInMillis)
            }
        }

        DatePickerState(
            initialSelectedDateMillis = nextSelectableDatesList.firstOrNull(),
            yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
            } else {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
            },
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = shippingSelectableDates,
            locale = Locale.FRANCE

        )
    } else {
        tomorrow.set(Calendar.DAY_OF_YEAR, (calendar.get(Calendar.DAY_OF_YEAR) + 1))
        DatePickerState(
            initialSelectedDateMillis = tomorrow.timeInMillis,
            yearRange = if (calendar.before(nextJanuary) && calendar.after(thisDecember)) {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR) + 1)
            } else {
                IntRange(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
            },
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = com.mtdevelopment.delivery.presentation.model.ShippingDefaultSelectableDates(),
            locale = Locale.FRANCE
        )

    }
}
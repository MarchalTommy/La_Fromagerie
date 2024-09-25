package com.mtdevelopment.checkout.presentation.model

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class ShippingSelectableDates(deliveryZone: Int = 0) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        // TODO: Implements DeliveryZones, so that each delivery place gets it's own days to be selected only.
        return true
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}

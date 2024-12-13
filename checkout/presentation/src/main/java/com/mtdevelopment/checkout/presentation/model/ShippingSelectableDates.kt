package com.mtdevelopment.checkout.presentation.model

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

private val limitDate = LocalDate.now().plusDays(2)

@OptIn(ExperimentalMaterial3Api::class)
class ShippingSelectableDatesTest(val selectedDay: String) :
    SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val instant = Instant.ofEpochMilli(utcTimeMillis)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val targetDate = zonedDateTime.toLocalDate()

        val isInTimeLimit = targetDate.isAfter(limitDate) || targetDate.isEqual(limitDate)

        val deliveryDay = DayOfWeek.valueOf(selectedDay)

        return targetDate.dayOfWeek == deliveryDay && isInTimeLimit
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class ShippingDefaultSelectableDates :
    SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return false
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}

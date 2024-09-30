package com.mtdevelopment.checkout.presentation.model

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class ShippingSelectableMetaDates :
    SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayCalendar = Calendar.getInstance()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = utcTimeMillis
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY && todayCalendar.time.before(
            calendar.time
        )
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class ShippingSelectableSalinDates :
    SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayCalendar = Calendar.getInstance()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = utcTimeMillis
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY && todayCalendar.time.before(
            calendar.time
        )
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR) + 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class ShippingSelectablePontarlierDates :
    SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayCalendar = Calendar.getInstance()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = utcTimeMillis
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && todayCalendar.time.before(
            calendar.time
        )
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

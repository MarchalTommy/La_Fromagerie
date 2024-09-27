package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent

class CheckoutViewModel : ViewModel(), KoinComponent {

    private val _cartObjects =
        MutableStateFlow(UiBasketObject("1", flowOf(emptyList()), flowOf("0,00€")))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    private val _selectedPath = MutableStateFlow<DeliveryPath?>(null)
    val selectedPath: StateFlow<DeliveryPath?> = _selectedPath.asStateFlow()

    fun setSelectedPath(path: DeliveryPath) {
        _selectedPath.value = path
    }

    // TODO: All logic about keepink which delivery path is selected, user name, address, etc...
}
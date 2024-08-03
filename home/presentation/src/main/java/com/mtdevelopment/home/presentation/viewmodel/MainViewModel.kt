package com.mtdevelopment.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.home.presentation.model.UiBasketObject
import com.mtdevelopment.home.presentation.model.UiProductObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class MainViewModel : ViewModel(), KoinComponent {

    private val _cartObjects = MutableStateFlow(UiBasketObject("1", emptyList(), 0.0))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    fun addCartObject(value: UiProductObject) {
        viewModelScope.launch {
            _cartObjects.emit(_cartObjects.value.copy(content = _cartObjects.value.content + value))
        }
    }

    fun removeCartObject(value: UiProductObject) {
        viewModelScope.launch {
            _cartObjects.emit(_cartObjects.value.copy(content = _cartObjects.value.content - value))
        }
    }
}
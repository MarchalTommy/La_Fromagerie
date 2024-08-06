package com.mtdevelopment.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.home.presentation.model.UiBasketObject
import com.mtdevelopment.home.presentation.model.UiProductObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class MainViewModel : ViewModel(), KoinComponent {

    private val _cartObjects =
        MutableStateFlow(UiBasketObject("1", flowOf(emptyList()), flowOf(0.0)))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    fun addCartObject(value: UiProductObject) {
        viewModelScope.launch {

            var mutableContent = _cartObjects.value.content.last() as? MutableList
            val selectedItem = mutableContent?.find { it.id == value.id }
            if (selectedItem != null) {
                selectedItem.quantity++
            } else if (mutableContent?.isNotEmpty() == true) {
                mutableContent.add(value.apply { quantity = 1 })
            } else {
                mutableContent = mutableListOf(value.apply { quantity = 1 })
            }

            _cartObjects.emit(
                _cartObjects.value.copy(content = flowOf(mutableContent as List<UiProductObject>))
            )

        }
    }

    fun removeCartObject(value: UiProductObject) {
        viewModelScope.launch {
            val mutableContent = _cartObjects.value.content.single() as MutableList
            val selectedItem = mutableContent.find { it.id == value.id }
            if (selectedItem != null && selectedItem.quantity > 1) {
                selectedItem.quantity--
            }

            _cartObjects.emit(
                _cartObjects.value.copy(content = flowOf(mutableContent))
            )
        }
    }

    fun totallyRemoveObject(value: UiProductObject) {
        viewModelScope.launch {
            val cleanedList = (_cartObjects.value.content.single() as MutableList)
            cleanedList.remove(value)
            _cartObjects.emit(
                _cartObjects.value.copy(content = (flowOf(cleanedList)))
            )
        }
    }
}
package com.mtdevelopment.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.core.util.toCentsLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.util.Formatter
import kotlin.random.Random

class CartViewModel(
    getIsNetworkConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase
) : ViewModel(), KoinComponent {

    // TODO: Add a button to reset / empty the cart with confirmation popup
    // TODO: Add a way to save a "preferred cart" with stuff you usually order

    val isConnected = getIsNetworkConnectedUseCase()

    private val _cartObjects =
        MutableStateFlow(
            UiBasketObject(
                Random.nextLong().toString(),
                flowOf(emptyList()),
                flowOf("0,00€")
            )
        )
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    fun addCartObject(value: UiProductObject) {
        viewModelScope.launch {

            val sb: StringBuilder =
                StringBuilder()
            val formatter = Formatter(sb)

            var mutableContent = _cartObjects.value.content.last() as? MutableList
            val selectedItem = mutableContent?.find { it.id == value.id }
            if (selectedItem != null) {
                selectedItem.quantity++
            } else if (mutableContent?.isNotEmpty() == true) {
                mutableContent.add(value.apply { quantity = 1 })
            } else {
                mutableContent = mutableListOf(value.apply { quantity = 1 })
            }

            val newCartObject = _cartObjects.value.copy(
                content = flowOf(mutableContent as List<UiProductObject>),
                totalPrice = flowOf(
                    formatter.format(
                        "%,.2f€",
                        mutableContent.sumOf { (it.priceInCents * it.quantity) }).toString()
                )
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.last().map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice.last().replace(",", ".").toDouble().toCentsLong()
                )
            )

            _cartObjects.emit(newCartObject)
        }
    }

    fun removeCartObject(value: UiProductObject) {
        viewModelScope.launch {

            val sb: StringBuilder =
                StringBuilder()
            val formatter = Formatter(sb)

            val mutableContent = _cartObjects.value.content.single() as MutableList
            val selectedItem = mutableContent.find { it.id == value.id }
            if (selectedItem != null && selectedItem.quantity > 1) {
                selectedItem.quantity--
            }

            val newCartObject = _cartObjects.value.copy(
                content = flowOf(mutableContent),
                totalPrice = flowOf(
                    formatter.format(
                        "%,.2f€",
                        mutableContent.sumOf { (it.priceInCents * it.quantity) }).toString()
                )
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.last().map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice.last().replace(",", ".").toDouble().toCentsLong()
                )
            )

            _cartObjects.emit(
                newCartObject
            )
        }
    }

    fun totallyRemoveObject(value: UiProductObject) {
        viewModelScope.launch {

            val sb: StringBuilder =
                StringBuilder()
            val formatter = Formatter(sb)

            val cleanedList = (_cartObjects.value.content.single() as MutableList)
            cleanedList.remove(value)

            val newCartObject = _cartObjects.value.copy(
                content = (flowOf(cleanedList)),
                totalPrice = flowOf(
                    formatter.format(
                        "%,.2f€",
                        cleanedList.sumOf { (it.priceInCents * it.quantity) }).toString()
                )
            )

            saveToDatastoreUseCase.invoke(
                CartItems(
                    newCartObject.content.last().map {
                        CartItem(
                            name = it.name,
                            price = it.priceInCents,
                            quantity = it.quantity
                        )
                    },
                    newCartObject.totalPrice.last().replace(",", ".").toDouble().toCentsLong()
                )
            )

            _cartObjects.emit(
                newCartObject
            )
        }
    }
}
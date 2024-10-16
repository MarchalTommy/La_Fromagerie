package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,

    ) : ViewModel(), KoinComponent {


    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _cartObjects =
        MutableStateFlow(UiBasketObject("1", flowOf(emptyList()), flowOf("0,00€")))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    private val _selectedPath = MutableStateFlow<DeliveryPath?>(null)
    val selectedPath: StateFlow<DeliveryPath?> = _selectedPath.asStateFlow()

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    fun setSelectedPath(path: DeliveryPath) {
        _selectedPath.value = path
    }

    fun setUserInfo(userInfo: UserInfo) {
        _userInfo.value = userInfo
    }
}
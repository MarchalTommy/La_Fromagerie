package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wallet.PaymentData
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.FetchCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetLoadPaymentDataTaskUseCase
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class CheckoutViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val createPaymentClientUseCase: CreatePaymentClientUseCase,
    private val fetchCanUseGooglePayUseCase: FetchCanUseGooglePayUseCase,
    private val getLoadPaymentDataTaskUseCase: GetLoadPaymentDataTaskUseCase,
    private val fetchAllowedPaymentMethods: FetchAllowedPaymentMethods
) : ViewModel(), KoinComponent {

    val allowedPaymentMethods = fetchAllowedPaymentMethods.invoke().toString()

    private val _cartObjects =
        MutableStateFlow(UiBasketObject("1", flowOf(emptyList()), flowOf("0,00€")))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()

    private val _selectedPath = MutableStateFlow<DeliveryPath?>(null)
    val selectedPath: StateFlow<DeliveryPath?> = _selectedPath.asStateFlow()

    private val _googlePayData: MutableStateFlow<PaymentData> =
        MutableStateFlow(PaymentData.fromJson("{}"))
    val googlePayData: StateFlow<PaymentData> = _googlePayData.asStateFlow()

    fun setSelectedPath(path: DeliveryPath) {
        _selectedPath.value = path
    }

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    fun setUserInfo(userInfo: UserInfo) {
        _userInfo.value = userInfo
    }

    init {
        createPaymentClientUseCase.invoke()
    }

    fun payWithGooglePay(amount: Double) {
        viewModelScope.launch {
            if (fetchCanUseGooglePayUseCase.invoke() == true) {
                getLoadPaymentDataTaskUseCase.invoke(
                    price = amount
                )
            } else {
                // TODO: MANAGE ERROR
            }
        }
    }

    fun setGooglePayResult(googlePayData: PaymentData) {
        _googlePayData.value = googlePayData
        // TODO: NOW SEND PAYMENT TOKEN TO GATEWAY ?! SUMUP I THINK
//        googlePayData.toJson().get("")
    }

}
package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.mapbox.maps.logE
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
    private val savedStateHandle: SavedStateHandle,
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    private val createPaymentClientUseCase: CreatePaymentClientUseCase,
    private val fetchCanUseGooglePayUseCase: FetchCanUseGooglePayUseCase,
    private val getLoadPaymentDataTaskUseCase: GetLoadPaymentDataTaskUseCase,
    private val fetchAllowedPaymentMethods: FetchAllowedPaymentMethods
) : ViewModel(), KoinComponent {

    val allowedPaymentMethods = fetchAllowedPaymentMethods.invoke().toString()

    val paymentUiState = savedStateHandle.getStateFlow("paymentUiState", PaymentUiState.NotStarted)
    private val _googlePayData: MutableStateFlow<PaymentData> =
        MutableStateFlow(PaymentData.fromJson("{}"))

    val googlePayData: StateFlow<PaymentData> = _googlePayData.asStateFlow()

    init {

        viewModelScope.launch {
            createPaymentClientUseCase.invoke()
        }
    }

    fun test() {
        savedStateHandle["deliveryUiState"] = DeliveryUiState.Ready
    }

    fun payWithGooglePay(amount: Double, onComplete: (Task<PaymentData>?) -> Unit) {
        viewModelScope.launch {
            if (fetchCanUseGooglePayUseCase.invoke() == true) {
                logE(tag = "PAYMENT", "CAN USE GOOGLE PAY")
                onComplete.invoke(
                    getLoadPaymentDataTaskUseCase.invoke(
                        price = amount
                    )
                )
            } else {
                logE(tag = "PAYMENT", "CANNOT USE GOOGLE PAY")
                onComplete.invoke(null)
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

abstract class PaymentUiState internal constructor() {
    object NotStarted : PaymentUiState()
    object Available : PaymentUiState()
    class PaymentCompleted(val payerName: String) : PaymentUiState()
    class Error(val code: Int, val message: String? = null) : PaymentUiState()
}
package com.mtdevelopment.checkout.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.mtdevelopment.checkout.domain.model.LocalCheckoutInformation
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetIsReadyToPayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.component.KoinComponent

class CheckoutViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    fetchAllowedPaymentMethods: FetchAllowedPaymentMethods,
    createPaymentsClientUseCase: CreatePaymentsClientUseCase,
    private val getCheckoutDataUseCase: GetCheckoutDataUseCase,
    private val getIsReadyToPayUseCase: GetIsReadyToPayUseCase,
    private val getCanUseGooglePayUseCase: GetCanUseGooglePayUseCase,
    private val getPaymentDataRequestUseCase: GetPaymentDataRequestUseCase,
    private val createNewCheckoutUseCase: CreateNewCheckoutUseCase,
    private val processSumUpCheckoutUseCase: ProcessSumUpCheckoutUseCase
) : ViewModel(), KoinComponent {

    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val checkoutScreenObject: StateFlow<LocalCheckoutInformation?> =
        getCheckoutDataUseCase.invoke().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )

    private val _paymentUiState: MutableStateFlow<PaymentUiState> =
        MutableStateFlow(PaymentUiState.NotStarted)
    val paymentUiState: StateFlow<PaymentUiState> = _paymentUiState.asStateFlow()

    val allowedPaymentMethods = fetchAllowedPaymentMethods.invoke().toString()

    private val _googlePayData: MutableStateFlow<PaymentData> =
        MutableStateFlow(PaymentData.fromJson("{}"))
    val googlePayData: StateFlow<PaymentData> = _googlePayData.asStateFlow()

    private val _isGooglePayAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isGooglePayAvailable: StateFlow<Boolean> = _isGooglePayAvailable.asStateFlow()

    // A client for interacting with the Google Pay API.
    private val paymentsClient: PaymentsClient = createPaymentsClientUseCase.invoke()

    init {
        viewModelScope.launch {
            verifyGooglePayReadiness()
        }
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
    ) */
    private suspend fun verifyGooglePayReadiness() {
        val newUiState: PaymentUiState = try {
            if (getCanUseGooglePayUseCase.invoke() == true) {
                PaymentUiState.Available
            } else {
                PaymentUiState.Error(CommonStatusCodes.ERROR)
            }
        } catch (exception: ApiException) {
            PaymentUiState.Error(exception.statusCode, exception.message)
        }

        _paymentUiState.update { newUiState }
    }

    /**
     * Creates a [Task] that starts the payment process with the transaction details included.
     *
     * @return a [Task] with the payment information.
     * @see [PaymentDataRequest](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#loadPaymentData(com.google.android.gms.wallet.PaymentDataRequest)
    ) */
    fun getLoadPaymentDataTask(priceCents: Long): Task<PaymentData> {
        val paymentDataRequestJson = getPaymentDataRequestUseCase.invoke(priceCents)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return paymentsClient.loadPaymentData(request)
    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int, message: String?) {
        Log.e("Google Pay API error", "Error code: $statusCode, Message: $message")
    }

    fun setPaymentData(paymentData: PaymentData) {
        val payState = extractPaymentBillingName(paymentData)?.let {
            PaymentUiState.PaymentCompleted(payerName = it)
        } ?: PaymentUiState.Error(CommonStatusCodes.INTERNAL_ERROR)



        _paymentUiState.update { payState }
    }

    private fun extractPaymentBillingName(paymentData: PaymentData): String? {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            // TODO : As shipping is mandatory here, we need to verify and cancel the order if the
            //  shipping selected here is NOT available for delivery or selected delivery date

            // TODO : CHECK LEGALLY if I can NOT ask for billing Address
            val shippingName = JSONObject(paymentInformation)
                .getJSONObject("shippingAddress").getString("name")
            Log.d("Shipping Name", shippingName)

            // Logging token string.
            Log.d(
                "Google Pay token", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            return shippingName
        } catch (error: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $error")
        }

        return null
    }

}

abstract class PaymentUiState internal constructor() {
    object NotStarted : PaymentUiState()
    object Available : PaymentUiState()
    class PaymentCompleted(val payerName: String) : PaymentUiState()
    class Error(val code: Int, val message: String? = null) : PaymentUiState()
}
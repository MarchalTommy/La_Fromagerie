package com.mtdevelopment.checkout.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPreviouslyCreatedCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCheckoutReferenceUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCreatedCheckoutUseCase
import com.mtdevelopment.checkout.presentation.model.PaymentScreenState
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.util.toPriceDouble
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import java.util.Calendar

class CheckoutViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    fetchAllowedPaymentMethods: FetchAllowedPaymentMethods,
    createPaymentsClientUseCase: CreatePaymentsClientUseCase,
    private val json: Json,
    private val getCheckoutDataUseCase: GetCheckoutDataUseCase,
    private val getCanUseGooglePayUseCase: GetCanUseGooglePayUseCase,
    private val getPaymentDataRequestUseCase: GetPaymentDataRequestUseCase,
    private val createNewCheckoutUseCase: CreateNewCheckoutUseCase,
    private val processSumUpCheckoutUseCase: ProcessSumUpCheckoutUseCase,
    private val saveCheckoutReferenceUseCase: SaveCheckoutReferenceUseCase,
    private val getPreviouslyCreatedCheckoutUseCase: GetPreviouslyCreatedCheckoutUseCase,
    private val saveCreatedCheckoutUseCase: SaveCreatedCheckoutUseCase
) : ViewModel(), KoinComponent {

    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _paymentScreenState: MutableStateFlow<PaymentScreenState> =
        MutableStateFlow(PaymentScreenState())
    val paymentScreenState: StateFlow<PaymentScreenState> = _paymentScreenState.asStateFlow()

    val allowedPaymentMethods = fetchAllowedPaymentMethods.invoke().toString()

    private val _googlePayData: MutableStateFlow<PaymentData> =
        MutableStateFlow(PaymentData.fromJson("{}"))
    val googlePayData: StateFlow<PaymentData> = _googlePayData.asStateFlow()

    // A client for interacting with the Google Pay API.
    private val paymentsClient: PaymentsClient = createPaymentsClientUseCase.invoke()

    init {
        viewModelScope.launch {
            updateUiState()
        }
        viewModelScope.launch {
            verifyGooglePayReadiness()
        }
    }

    private suspend fun updateUiState() {
        _paymentScreenState.update {
            it.copy(isLoading = true)
        }
        getCheckoutDataUseCase.invoke().collect { data ->
            if (data != null) {
                _paymentScreenState.update {
                    it.copy(
                        isLoading = false,
                        buyerName = data.buyerName,
                        buyerAddress = data.buyerAddress,
                        totalPrice = data.totalPrice,
                        deliveryDate = data.deliveryDate,
                        cartItems = data.cartItems
                    )
                }
            }
        }
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
    ) */
    private suspend fun verifyGooglePayReadiness() {
        _paymentScreenState.update {
            it.copy(
                isLoading = true
            )
        }

        try {
            if (getCanUseGooglePayUseCase.invoke() == true) {
                _paymentScreenState.update {
                    it.copy(
                        isGooglePayAvailable = true
                    )
                }
            } else {
                FirebaseCrashlytics.getInstance()
                    .recordException(Throwable("Google Pay not available, exception raised by hand"))
                _paymentScreenState.update {
                    it.copy(
                        error = "Google Pay ne semble pas disponible sur votre téléphone.\nMerci de contacter l'équipe de l'EARL."
                    )
                }
            }
        } catch (exception: ApiException) {
            FirebaseCrashlytics.getInstance()
                .recordException(Throwable("Error on checking Google Pay : ${exception.statusCode} - ${exception.message}"))
            _paymentScreenState.update {
                it.copy(
                    error = "Une erreur est survenue avec le prestataire de paiement.\nMerci de contacter l'équipe de l'EARL."
                )
            }
        }


        _paymentScreenState.update {
            it.copy(
                isLoading = false
            )
        }
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

    fun createCheckout(isSuccess: (Boolean) -> Unit) {
        _paymentScreenState.update {
            it.copy(
                isLoading = true
            )
        }

        val checkoutRef =
            paymentScreenState.value.buyerName.toString()
                .replace(" ", "-") + "_" + Calendar.getInstance().time.toInstant()
                .toEpochMilli().toString()

        viewModelScope.launch {
            createNewCheckoutUseCase.invoke(
                amount = paymentScreenState.value.totalPrice?.toPriceDouble() ?: 0.0,
                reference = checkoutRef
            ).collect { checkout ->
                saveCheckoutReferenceUseCase.invoke(checkoutRef)
                saveCreatedCheckoutUseCase.invoke(checkout)
                _paymentScreenState.update {
                    it.copy(
                        isLoading = false
                    )
                }
                isSuccess.invoke(checkout.status != null)
            }
        }
    }

    private suspend fun processCheckout(paymentDataItem: GooglePayData, checkoutId: String) {
        _paymentScreenState.update {
            it.copy(
                isLoading = true
            )
        }

        processSumUpCheckoutUseCase.invoke(
            checkoutId = checkoutId,
            googlePayData = paymentDataItem
        ).collect { temporaryPair ->


            delay(temporaryPair.second)

            // TODO: Now, loader until fetch again checkouts from SumUp to see if completed ! Still needs to bypass SumUp for now as I'm still in preprod Google Pay
            _paymentScreenState.update { state ->
                state.copy(
                    isLoading = true,
                    isPaymentSuccess = temporaryPair.first
                )
            }
        }
    }

    fun setPaymentData(paymentData: PaymentData) {
        viewModelScope.launch {
            _paymentScreenState.update {
                it.copy(
                    isLoading = true
                )
            }

            getPreviouslyCreatedCheckoutUseCase.invoke().collect {

                if (it.status.equals("pending", true)) {
                    val paymentDataItem =
                        json.decodeFromString<GooglePayData>(paymentData.toJson())
                    processCheckout(paymentDataItem, it.id ?: "")
                }
            }
        }
    }

    private fun extractPaymentBillingName(paymentData: PaymentData): String? {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
//            // TODO : As shipping is mandatory here, we need to verify and cancel the order if the
//            //  shipping selected here is NOT available for delivery or selected delivery date
//
//            // TODO : CHECK LEGALLY if I can NOT ask for billing Address
//            val shippingName = JSONObject(paymentInformation)
//                .getJSONObject("shippingAddress").getString("name")
//            Log.d("Shipping Name", shippingName)

            // Logging token string.
            Log.d(
                "Google Pay token", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

            return "ROBERTS TESTEUR"
        } catch (error: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $error")
        }

        return null
    }

    fun setGooglePayEnabled(enabled: Boolean) {
        _paymentScreenState.update {
            it.copy(
                isGooglePayAvailable = enabled
            )
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // FOR DEBUGGING PURPOSE ONLY
    ///////////////////////////////////////////////////////////////////////////
    fun setPaymentSuccess(isSuccess: Boolean) {
        _paymentScreenState.update {
            it.copy(
                isPaymentSuccess = isSuccess
            )
        }
    }

}
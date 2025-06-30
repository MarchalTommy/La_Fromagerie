package com.mtdevelopment.checkout.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreateNewOrderUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetIsPaymentSuccessUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPreviouslyCreatedCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.GetSavedOrderUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.ResetCheckoutStatusUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCheckoutReferenceUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCreatedCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.SavePaymentStateUseCase
import com.mtdevelopment.checkout.domain.usecase.UpdateOrderStatus
import com.mtdevelopment.checkout.presentation.model.PaymentScreenState
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.usecase.ClearCartUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import java.util.Locale

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
    private val saveCreatedCheckoutUseCase: SaveCreatedCheckoutUseCase,
    private val savePaymentStateUseCase: SavePaymentStateUseCase,
    private val getIsPaymentSuccessUseCase: GetIsPaymentSuccessUseCase,
    private val clearCartUseCase: ClearCartUseCase,
    private val resetCheckoutStatusUseCase: ResetCheckoutStatusUseCase,
    private val createNewOrderUseCase: CreateNewOrderUseCase,
    private val updateOrderStatus: UpdateOrderStatus,
    private val getSavedOrderUseCase: GetSavedOrderUseCase
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
            verifyGooglePayReadiness()
        }
    }

    suspend fun updateUiState() {
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
                        cartItems = data.cartItems,
                        isPaymentSuccess = false
                    )
                }
            }
        }
    }

    fun updateCheckoutNote(note: String) {
        _paymentScreenState.update {
            it.copy(
                checkoutNote = note
            )
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
                        isGooglePayAvailable = false,
                        error = "Google Pay ne semble pas disponible sur votre téléphone.\nMerci de contacter l'équipe de l'EARL."
                    )
                }
            }
        } catch (exception: ApiException) {
            FirebaseCrashlytics.getInstance()
                .recordException(Throwable("Error on checking Google Pay : ${exception.statusCode} - ${exception.message}"))
            _paymentScreenState.update {
                it.copy(
                    isGooglePayAvailable = false,
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

    // TODO: This has been commented to avoid overloading SumUp API during development of Google Pay
//    fun createCheckout(isSuccess: (Boolean) -> Unit) {
//        _paymentScreenState.update {
//            it.copy(
//                isLoading = true
//            )
//        }
//
//        val checkoutRef =
//            paymentScreenState.value.buyerName.toString()
//                .replace(" ", "-") + "_" + Calendar.getInstance().time.toInstant()
//                .toEpochMilli().toString()
//
//        viewModelScope.launch {
//            createNewCheckoutUseCase.invoke(
//                amount = paymentScreenState.value.totalPrice?.toPriceDouble() ?: 0.0,
//                reference = checkoutRef
//            ).collect { checkout ->
//                saveCheckoutReferenceUseCase.invoke(checkoutRef)
//                saveCreatedCheckoutUseCase.invoke(checkout)
//                _paymentScreenState.update {
//                    it.copy(
//                        isLoading = false
//                    )
//                }
//                isSuccess.invoke(checkout.status != null)
//            }
//        }
//    }

//    private suspend fun processCheckout(paymentDataItem: GooglePayData, checkoutId: String) {
//        // TODO: Now, loader until fetch again checkouts from SumUp to see if completed !
//        //  Still needs to bypass SumUp for now as I'm still in preprod Google Pay
//
//        getIsPaymentSuccessUseCase.invoke().collect {
//            _paymentScreenState.update { state ->
//                state.copy(
//                    isLoading = false,
//                    isPaymentSuccess = it
//                )
//            }
//        }
//        processSumUpCheckoutUseCase.invoke(
//            checkoutId = checkoutId,
//            googlePayData = paymentDataItem
//        ).collect { temporaryPair ->
//
//
//        }
//    }

    fun setGooglePaySuccess(isSuccess: Boolean) {
        if (!viewModelScope.isActive) {
            return
        }
        viewModelScope.launch {
            try {
                _paymentScreenState.update { state ->
                    state.copy(isLoading = true)
                }
                savePaymentStateUseCase.invoke(isSuccess)
                delay(3000)
                _paymentScreenState.update { state ->
                    state.copy(
                        isLoading = false,
                        isPaymentSuccess = isSuccess
                    )
                }
            } catch (e: Exception) {
                // Mettre à jour l'état pour refléter l'erreur si nécessaire
                _paymentScreenState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

//    fun setPaymentData(paymentData: PaymentData) {
//        viewModelScope.launch {
//            _paymentScreenState.update {
//                it.copy(
//                    isLoading = true
//                )
//            }
//            // TODO: FIRST OF ALL, CHECK THE ORDER TO MAKE IT SAVE THE STATUS RIGHT AFTER GOOGLE PAY
//            // TODO: THEN, COLLECT THE FLOW TO UPDATE THE UI
//            savePaymentStateUseCase.invoke(true)
//
//            val paymentDataItem =
//                json.decodeFromString<GooglePayData>(paymentData.toJson())
//            processCheckout(paymentDataItem, "")
//
//            // TODO: Uncomment when Google Pay is ready
////            getPreviouslyCreatedCheckoutUseCase.invoke().collect {
////                if (it.status.equals("pending", true)) {
////                    val paymentDataItem =
////                        json.decodeFromString<GooglePayData>(paymentData.toJson())
////                    processCheckout(paymentDataItem, it.id ?: "")
////                }
////            }
//        }
//    }

    private fun extractPaymentBillingName(paymentData: PaymentData): String? {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
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

    private fun resetPaymentState() {
        viewModelScope.launch {
            resetCheckoutStatusUseCase.invoke()
            _paymentScreenState.update {
                it.copy(
                    isPaymentSuccess = false
                )
            }
        }
    }

    fun resetAppStateAfterSuccess() {
        viewModelScope.launch {
            updateOrderStatus.invoke(
                orderId = getSavedOrderUseCase.invoke().first().id,
                newStatus = OrderStatus.PAID
            )
            clearCartUseCase.invoke()
            resetPaymentState()
        }
    }

    fun createOrder(isSuccess: (Boolean) -> Unit) {
        val cleanName =
            _paymentScreenState.value.buyerName?.trim()?.replace(" ", "_")
                ?.lowercase(Locale.getDefault())
        val orderId = "${cleanName}#${
            Timestamp.now().toInstant().toEpochMilli()
        }"
        _paymentScreenState.update {
            it.copy(
                orderId = orderId
            )
        }

        val orderProduct = mutableMapOf<String, Int>()
        _paymentScreenState.value.cartItems?.cartItems?.forEach {
            orderProduct[it?.name ?: ""] = it?.quantity ?: 0
        }

        viewModelScope.launch {
            isSuccess.invoke(
                createNewOrderUseCase.invoke(
                    Order(
                        id = orderId,
                        customerName = _paymentScreenState.value.buyerName.toString(),
                        customerAddress = _paymentScreenState.value.buyerAddress.toString(),
                        deliveryDate = _paymentScreenState.value.deliveryDate?.toStringDate() ?: "",
                        orderDate = Timestamp.now().toDate().time.toStringDate(),
                        products = orderProduct,
                        status = OrderStatus.PENDING,
                        note = _paymentScreenState.value.checkoutNote.toString()
                    )
                )
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

    fun setPaymentError(message: String? = null) {
        _paymentScreenState.update {
            it.copy(
                error = message
            )
        }
    }

}
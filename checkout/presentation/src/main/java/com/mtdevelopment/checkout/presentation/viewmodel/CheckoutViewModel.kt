package com.mtdevelopment.checkout.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.firebase.Timestamp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.checkout.domain.usecase.ClearPendingPaymentFinalizationUseCase
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
import com.mtdevelopment.checkout.domain.usecase.SchedulePaymentFinalizationUseCase
import com.mtdevelopment.checkout.domain.usecase.UpdateOrderStatus
import com.mtdevelopment.checkout.presentation.ThreeDSecureActivity
import com.mtdevelopment.checkout.presentation.model.PaymentScreenState
import com.mtdevelopment.core.domain.toPriceDouble
import com.mtdevelopment.core.domain.toStringDate
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.usecase.ClearCartUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import java.util.Calendar
import java.util.Locale

/**
 * ViewModel for the Checkout process, orchestrating the interaction between the UI, 
 * Google Pay API, and SumUp payment gateway.
 *
 * The payment flow follows a specific sequence:
 * 1. [verifyGooglePayReadiness]: Checks if the device and user are capable of using Google Pay.
 * 2. [createOrder]: Saves the order details to Firestore with a 'PENDING' status.
 * 3. [createCheckout]: Creates a "Checkout" session on SumUp's servers.
 * 4. [getLoadPaymentDataTask]: Initiates the Google Pay payment sheet.
 * 5. [setPaymentData]: Receives the Google Pay token and triggers [processCheckout].
 * 6. [processCheckout]: Sends the Google Pay token to SumUp to authorize the transaction.
 *    This step might trigger a 3D Secure challenge.
 * 7. After success, [resetAppStateAfterSuccess] updates the order status to 'PAID' and clears the cart.
 *
 * Durability: right before step 6 submits the payment, a persisted WorkManager job
 * (FinalizePaymentWorker) is scheduled. If the app is killed before the terminal status
 * is handled in-app, the worker resumes polling and reconciles the order status itself.
 */
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
    private val getSavedOrderUseCase: GetSavedOrderUseCase,
    private val schedulePaymentFinalizationUseCase: SchedulePaymentFinalizationUseCase,
    private val clearPendingPaymentFinalizationUseCase: ClearPendingPaymentFinalizationUseCase
) : ViewModel(), KoinComponent {

    /**
     * Observes network connectivity. Required for payment processing.
     */
    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Main UI state for the checkout screen.
     */
    private val _paymentScreenState: MutableStateFlow<PaymentScreenState> =
        MutableStateFlow(PaymentScreenState())
    val paymentScreenState: StateFlow<PaymentScreenState> = _paymentScreenState.asStateFlow()

    /**
     * JSON configuration for allowed payment methods, required by Google Pay API.
     */
    val allowedPaymentMethods = fetchAllowedPaymentMethods.invoke().toString()

    /**
     * Data returned by Google Pay after user authorization.
     */
    private val _googlePayData: MutableStateFlow<PaymentData> =
        MutableStateFlow(PaymentData.fromJson("{}"))
    val googlePayData: StateFlow<PaymentData> = _googlePayData.asStateFlow()

    /**
     * The Google Pay client instance.
     */
    private val paymentsClient: PaymentsClient = createPaymentsClientUseCase.invoke()

    init {
        viewModelScope.launch {
            verifyGooglePayReadiness()
        }
    }

    /**
     * Initializes the UI state with data from the cart and user profile.
     */
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
                        buyerBillingAddress = data.billingAddress,
                        totalPrice = data.totalPrice,
                        deliveryDate = data.deliveryDate,
                        cartItems = data.cartItems,
                        isPaymentSuccess = false
                    )
                }
            }
        }
    }

    /**
     * Updates the buyer's email in the UI state.
     */
    fun updateBuyerEmail(email: String) {
        _paymentScreenState.update {
            it.copy(buyerEmail = email)
        }
    }

    /**
     * Updates any special delivery notes or instructions.
     */
    fun updateCheckoutNote(note: String) {
        _paymentScreenState.update {
            it.copy(
                checkoutNote = note
            )
        }
    }

    /**
     * Verifies if Google Pay is available on the device and for the current user.
     * This checks for hardware support and that the user has a valid payment method.
     */
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
     * Prepares the Google Pay request task. 
     * This task is used to launch the Google Pay bottom sheet.
     * 
     * @param priceCents Transaction amount in cents.
     *
     * Creates a [Task] that starts the payment process with the transaction details included.
     *
     * @return a [Task] with the payment information.
     * @see [PaymentDataRequest](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#loadPaymentData(com.google.android.gms.wallet.PaymentDataRequest))
     */
    fun getLoadPaymentDataTask(priceCents: Long): Task<PaymentData> {
        val paymentDataRequestJson = getPaymentDataRequestUseCase.invoke(priceCents)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return paymentsClient.loadPaymentData(request)
    }

    /**
     * Step 3: Creates a checkout session on SumUp.
     * A checkout ID is required before we can process the Google Pay token.
     */
    fun createCheckout(isSuccess: (Boolean) -> Unit) {
        _paymentScreenState.update {
            it.copy(
                isLoading = true
            )
        }

        // Unique reference for this checkout session
        val checkoutRef =
            paymentScreenState.value.buyerName.toString()
                .replace(" ", "-") + "_" + Calendar.getInstance().time.toInstant()
                .toEpochMilli().toString()

        viewModelScope.launch {
            createNewCheckoutUseCase.invoke(
                amount = paymentScreenState.value.totalPrice?.toPriceDouble() ?: 0.0,
                description = paymentScreenState.value.cartItems?.cartItems?.joinToString(", ") { "${it?.quantity} x ${it?.name}" }
                    ?: "",
                buyerName = paymentScreenState.value.buyerName.toString(),
                buyerAddress = paymentScreenState.value.buyerAddress.toString(),
                buyerEmail = paymentScreenState.value.buyerEmail.toString(),
                reference = checkoutRef
            ).collect { checkout ->
                // Store the reference and checkout info for step 5 & 6
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

    /**
     * Step 6: Finalize the payment with SumUp.
     * Sends the Google Pay token to SumUp's [processCheckout] endpoint.
     * 
     * @param context Android context for launching 3DS activity.
     * @param paymentDataItem The Google Pay data (including token).
     * @param checkoutId The ID created in [createCheckout].
     */
    private suspend fun processCheckout(
        context: Context,
        paymentDataItem: GooglePayData,
        checkoutId: String
    ) {
        // Polling for the final PAID/FAILED status is handled at the DataSource level
        // (see SumUpDataSource for its known limitations if the app is killed mid-polling).

        // Verify if Google Pay reported success before proceeding with SumUp authorization
        getIsPaymentSuccessUseCase.invoke().collect {
            if (it) {
                processSumUpCheckoutUseCase.invoke(
                    checkoutId = checkoutId,
                    googlePayData = paymentDataItem,
                    on3DSecureRequired = { nextStep ->
                        // If the card requires 3DS verification, redirect to the custom activity
                        launch3DSActivity(context, nextStep)
                    }
                ).collect { finalResponse ->
                    // The flow emits only when a terminal status (PAID or FAILED) is reached due to polling.
                    // On PAID, resetAppStateAfterSuccess finalizes the order and clears the
                    // pending-finalization marker. On FAILED, the marker is left in place on
                    // purpose: FinalizePaymentWorker marks the Firestore order as CANCELED.
                    _paymentScreenState.update { state ->
                        state.copy(
                            isLoading = false,
                            isPaymentSuccess = finalResponse.status?.value.equals("PAID", true)
                        )
                    }
                }
            }
        }
    }

    /**
     * Step 5: Receives the Google Pay result and initiates SumUp processing.
     */
    fun setPaymentData(context: Context, paymentData: PaymentData) {
        viewModelScope.launch {
            _paymentScreenState.update {
                it.copy(
                    isLoading = true
                )
            }
            // Mark payment as 'in progress'
            savePaymentStateUseCase.invoke(true)

            // Retrieve the pending checkout ID created in Step 3
            getPreviouslyCreatedCheckoutUseCase.invoke().collect { checkout ->
                if (checkout == null) {
                    // No (or corrupt) saved checkout session: abort instead of crashing
                    _paymentScreenState.update {
                        it.copy(
                            isLoading = false,
                            error = "Une erreur est survenue lors de la récupération de la session de paiement.\nMerci de réessayer."
                        )
                    }
                } else if (checkout.status.equals("pending", true)) {
                    // Schedule the durable finalization work BEFORE submitting the payment:
                    // if the app is killed while waiting for SumUp's terminal status, the
                    // worker resumes the polling and reconciles the Firestore order.
                    val checkoutId = checkout.id
                    val orderId = _paymentScreenState.value.orderId
                    if (checkoutId != null && orderId != null) {
                        schedulePaymentFinalizationUseCase.invoke(checkoutId, orderId)
                    }

                    val paymentDataItem =
                        json.decodeFromString<GooglePayData>(paymentData.toJson())
                    processCheckout(context, paymentDataItem, checkout.id ?: "")
                }
            }
        }
    }

    fun setGooglePayEnabled(enabled: Boolean) {
        _paymentScreenState.update {
            it.copy(
                isGooglePayAvailable = enabled
            )
        }
    }

    /**
     * Clears payment-related temporary state.
     */
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

    /**
     * Step 7: Final cleanup and order confirmation.
     * Executed after a successful PAID status from SumUp.
     */
    fun resetAppStateAfterSuccess() {
        viewModelScope.launch {
            // Update order status to PAID in Firestore
            updateOrderStatus.invoke(
                orderId = getSavedOrderUseCase.invoke().first().id,
                newStatus = OrderStatus.PAID
            )
            // Empty the cart
            clearCartUseCase.invoke()
            // The order reached its terminal state in-app: the background
            // finalization work is no longer needed and will no-op.
            clearPendingPaymentFinalizationUseCase.invoke()
            resetPaymentState()
        }
    }

    /**
     * Step 2: Creates the order record in Firestore.
     * This is done BEFORE payment to ensure the order intent is captured.
     * Status is 'PENDING' until payment succeeds.
     */
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
                        customerBillingAddress = _paymentScreenState.value.buyerBillingAddress.toString(),
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

    /**
     * Redirects the user to a specialized activity to handle 3D Secure verification.
     */
    private fun launch3DSActivity(context: Context, nextStep: ProcessCheckoutResult.NextStep) {
        val intent = Intent(context, ThreeDSecureActivity::class.java).apply {
            putExtra(ThreeDSecureActivity.EXTRA_URL, nextStep.url)
            putExtra(ThreeDSecureActivity.EXTRA_METHOD, nextStep.method)
            putExtra(ThreeDSecureActivity.EXTRA_REDIRECT_URL, nextStep.redirectUrl)

            // Conversion of the Payload to HashMap for Intent extras
            val params = HashMap<String, String>()
            nextStep.payload?.let { p ->
                p.paReq?.let { params["PaReq"] = it }
                p.md?.let { params["MD"] = it }
                p.termUrl?.let { params["TermUrl"] = it }
                // Ajoutez les autres champs si nécessaire
            }
            putExtra(ThreeDSecureActivity.EXTRA_PAYLOAD_PARAMS, params)
        }
        context.startActivity(intent)
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
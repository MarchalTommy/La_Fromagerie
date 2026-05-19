package com.mtdevelopment.checkout.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.remote.model.request.Address
import com.mtdevelopment.checkout.data.remote.model.request.CHECKOUT_CREATION_BODY_PURPOSE
import com.mtdevelopment.checkout.data.remote.model.request.CheckoutCreationBody
import com.mtdevelopment.checkout.data.remote.model.request.PersonalDetails
import com.mtdevelopment.checkout.data.remote.model.request.ProcessCheckoutRequest
import com.mtdevelopment.checkout.data.remote.model.request.toPaymentData
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.toDomainCheckout
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.toNewCheckoutResult
import com.mtdevelopment.checkout.data.remote.source.FirestoreOrderDataSource
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.core.data.Constants
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.toOrderData
import com.mtdevelopment.core.util.NetWorkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implementation of [PaymentRepository] that manages both Google Pay integration and SumUp payment gateway.
 */
class PaymentRepositoryImpl(
    private val context: Context,
    private val sumUpDataSource: SumUpDataSource,
    private val firestoreOrderDataSource: FirestoreOrderDataSource,
) : PaymentRepository {

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY CONFIGURATION
    ///////////////////////////////////////////////////////////////////////////

    private lateinit var client: PaymentsClient

    /**
     * Base request JSON template for Google Pay API.
     */
    private val baseRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)

    /**
     * Tokenization parameters that tell Google Pay how to encrypt the payment data.
     * These parameters are typically provided by the payment gateway (SumUp).
     */
    private val gatewayTokenizationSpecification: JSONObject =
        JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject(Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))

    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)

    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    /**
     * Defines the supported card payment methods.
     */
    private fun baseCardPaymentMethod(): JSONObject =
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", allowedCardAuthMethods)
                    .put("allowedCardNetworks", allowedCardNetworks)
            )

    /**
     * Full configuration for card payments, including tokenization info.
     */
    private val cardPaymentMethod: JSONObject = baseCardPaymentMethod()
        .put("tokenizationSpecification", gatewayTokenizationSpecification)

    /**
     * The list of payment methods accepted by this app for Google Pay.
     */
    override val allowedPaymentMethods: JSONArray = JSONArray().put(cardPaymentMethod)

    /**
     * Builds the request to check if the user is ready to pay with Google Pay.
     */
    override fun isReadyToPayRequest(): JSONObject? =
        try {
            baseRequest
                .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
        } catch (e: JSONException) {
            null
        }

    /**
     * Queries the Google Pay API to see if the payment button should be displayed.
     */
    override suspend fun canUseGooglePay(): Boolean {
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest().toString())
        return client.isReadyToPay(request).await()
    }

    private val merchantInfo: JSONObject =
        JSONObject().put("merchantName", "EARL Des Laizinnes")

    /**
     * Initializes the Google Pay client with the appropriate environment (TEST or PRODUCTION).
     */
    override fun createPaymentsClient(): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
            .build()
        client = Wallet.getPaymentsClient(context, walletOptions)
        return client
    }

    /**
     * Builds the transaction info object required for the payment sheet.
     */
    private fun getTransactionInfo(price: String): JSONObject =
        JSONObject()
            .put("totalPrice", price)
            .put("totalPriceStatus", "FINAL")
            .put("countryCode", Constants.COUNTRY_CODE)
            .put("currencyCode", Constants.CURRENCY_CODE)

    /**
     * Prepares the full data request to launch the Google Pay sheet.
     * @param priceCents The amount to charge, in cents.
     */
    override fun getPaymentDataRequest(priceCents: Long): JSONObject =
        baseRequest
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .put("transactionInfo", getTransactionInfo(priceCents.centsToString()))
            .put("merchantInfo", merchantInfo)
            .put("shippingAddressRequired", false)


    private val CENTS = BigDecimal(100)

    /**
     * Converts cents (Long) to a string format with 2 decimal places (e.g., 1050 -> "10.50").
     * This format is required by the Google Pay JSON API.
     */
    private fun Long.centsToString() = BigDecimal(this)
        .divide(CENTS)
        .setScale(2, RoundingMode.HALF_EVEN)
        .toString()

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP INTEGRATION
    ///////////////////////////////////////////////////////////////////////////

    override suspend fun getCheckoutFromRef(reference: String?) {
        // // TODO: Implement if needed to recover lost checkout sessions by their reference string.
        TODO("Not yet implemented")
    }

    override suspend fun getCheckoutFromId(id: String) {
        // // TODO: Implement if direct ID lookup from repository is needed.
        TODO("Not yet implemented")
    }

    /**
     * Step 3: Creates a checkout session on SumUp servers.
     * This provides a unique ID that we then "process" with Google Pay data.
     */
    override fun createNewCheckout(
        amount: Double,
        description: String,
        buyerName: String,
        buyerAddress: String,
        buyerEmail: String,
        reference: String
    ): Flow<NewCheckoutResult> {
        return sumUpDataSource.createNewCheckout(
            CheckoutCreationBody(
                checkoutReference = reference,
                amount = amount,
                currency = "EUR",
                id = "${reference.hashCode()}_${System.currentTimeMillis()}",
                personalDetails = PersonalDetails(
                    firstName = buyerName,
                    email = buyerEmail,
                    address = Address(
                        firstLine = buyerAddress
                    )
                ),
                description = description,
                redirectUrl = BuildConfig.SUMUP_REDIRECT_URL,
                purpose = CHECKOUT_CREATION_BODY_PURPOSE.CHECKOUT,
                merchantCode = BuildConfig.SUMUP_MERCHANT_ID
            )
        ).transform { value ->
            if (
                (value as? NetWorkResult.Success)?.data != null) {
                emit(value.data!!.toNewCheckoutResult())
            }
        }
    }

    /**
     * Step 6: Finalizes the transaction by sending the Google Pay token to SumUp.
     * This method chains the initial authorize call with the polling mechanism in the DataSource.
     */
    override fun processCheckout(
        checkoutId: String,
        googlePayData: GooglePayData,
        on3DSecureRequired: (ProcessCheckoutResult.NextStep) -> Unit
    ): Flow<Checkout> {

        val processCheckoutRequest = ProcessCheckoutRequest(
            id = checkoutId,
            currency = "EUR",
            paymentType = "google_pay",
            googlePay = ProcessCheckoutRequest.GooglePay(
                apiVersion = googlePayData.apiVersion,
                apiVersionMinor = googlePayData.apiVersionMinor,
                paymentMethodData = googlePayData.paymentMethodData?.toPaymentData()
            )
        )

        return sumUpDataSource.processCheckout(
            requestBody = processCheckoutRequest,
            on3DSecureRequired = on3DSecureRequired
        ).mapNotNull { networkResult -> 
            when (networkResult) {
                is NetWorkResult.Success -> {
                    val checkoutResponse = networkResult.data
                    // We only emit terminal states to the Domain layer.
                    if (checkoutResponse.status == CHECKOUT_STATUS.PAID || checkoutResponse.status == CHECKOUT_STATUS.FAILED) {
                        checkoutResponse.toDomainCheckout()
                    } else {
                        Log.w(
                            "ProcessCheckout",
                            "DataSource returned non-final status: ${checkoutResponse.status} for checkout ${checkoutResponse.id}"
                        )
                        null
                    }
                }

                is NetWorkResult.Error -> {
                    Log.e(
                        "ProcessCheckout",
                        "Error: ${networkResult.message} (${networkResult.code})"
                    )
                    throw Throwable(networkResult.message)
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ORDER MANAGEMENT
    ///////////////////////////////////////////////////////////////////////////
    override suspend fun createFirestoreOrder(order: Order): Result<Unit> {
        return firestoreOrderDataSource.createOrder(order.toOrderData())
    }

    override suspend fun updateFirestoreOrderStatus(
        orderId: String,
        newStatus: OrderStatus
    ): Result<Unit> {
        return firestoreOrderDataSource.updateOrder(orderId = orderId, newStatus = newStatus)
    }

}
package com.mtdevelopment.checkout.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.mtdevelopment.checkout.data.BuildConfig
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

class PaymentRepositoryImpl(
    private val context: Context,
    private val sumUpDataSource: SumUpDataSource,
    private val firestoreOrderDataSource: FirestoreOrderDataSource,
//    private val datastore: CheckoutDatastorePreferenceImpl
) : PaymentRepository {

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY PART
    ///////////////////////////////////////////////////////////////////////////

    private lateinit var client: PaymentsClient

    private val baseRequest = JSONObject()
        .put("apiVersion", 2)
        .put("apiVersionMinor", 0)

    private val gatewayTokenizationSpecification: JSONObject =
        JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject(Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))

    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)

    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    private fun baseCardPaymentMethod(): JSONObject =
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", allowedCardAuthMethods)
                    .put("allowedCardNetworks", allowedCardNetworks)
            )

    private val cardPaymentMethod: JSONObject = baseCardPaymentMethod()
        .put("tokenizationSpecification", gatewayTokenizationSpecification)

    override val allowedPaymentMethods: JSONArray = JSONArray().put(cardPaymentMethod)

    override fun isReadyToPayRequest(): JSONObject? =
        try {
            baseRequest
                .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
        } catch (e: JSONException) {
            null
        }

    override suspend fun canUseGooglePay(): Boolean {
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest().toString())
        return client.isReadyToPay(request).await()
    }

    private val merchantInfo: JSONObject =
        JSONObject().put("merchantName", "EARL Des Laizinnes")

    override fun createPaymentsClient(): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
            .build()
        client = Wallet.getPaymentsClient(context, walletOptions)
        return client
    }

    private fun getTransactionInfo(price: String): JSONObject =
        JSONObject()
            .put("totalPrice", price)
            .put("totalPriceStatus", "FINAL")
            .put("countryCode", Constants.COUNTRY_CODE)
            .put("currencyCode", Constants.CURRENCY_CODE)

    override fun getPaymentDataRequest(priceCents: Long): JSONObject =
        baseRequest
            .put("allowedPaymentMethods", allowedPaymentMethods)
            .put("transactionInfo", getTransactionInfo(priceCents.centsToString()))
            .put("merchantInfo", merchantInfo)
            .put("shippingAddressRequired", false)


    private val CENTS = BigDecimal(100)

    /**
     * Converts cents to a string format accepted by [getPaymentDataRequest].
     */
    private fun Long.centsToString() = BigDecimal(this)
        .divide(CENTS)
        .setScale(2, RoundingMode.HALF_EVEN)
        .toString()

    ///////////////////////////////////////////////////////////////////////////
    // SUMUP PART
    ///////////////////////////////////////////////////////////////////////////

    override suspend fun getCheckoutFromRef(reference: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun getCheckoutFromId(id: String) {
        TODO("Not yet implemented")
    }

    // TODO: Create checkout when clicking on google pay button
    // TODO: Save checkout reference securely, locally and remotely
    override fun createNewCheckout(
        amount: Double,
        reference: String
    ): Flow<NewCheckoutResult> {
        return sumUpDataSource.createNewCheckout(
            CheckoutCreationBody(
                checkoutReference = reference,
                amount = amount,
                currency = "EUR",
                id = "${reference.hashCode()}_${System.currentTimeMillis()}",
                personalDetails = PersonalDetails(

                ),
                purpose = CHECKOUT_CREATION_BODY_PURPOSE.CHECKOUT,
//                merchantCode = "BuildConfig.SUMUP_MERCHANT_ID",
                merchantCode = BuildConfig.SUMUP_MERCHANT_ID
            )
        ).transform { value ->
            if (
                (value as? NetWorkResult.Success)?.data != null) {
                emit(value.data!!.toNewCheckoutResult())
            }
        }
    }

    override fun processCheckout(
        checkoutId: String,
        googlePayData: GooglePayData,
        handle3dsRedirect: (redirectUrl: String?) -> Unit
    ): Flow<Checkout> {

        val processCheckoutRequest = ProcessCheckoutRequest(
            id = checkoutId,
            currency = "EUR",
            googlePay = ProcessCheckoutRequest.GooglePay(
                apiVersion = googlePayData.apiVersion,
                apiVersionMinor = googlePayData.apiVersionMinor,
                paymentMethodData = googlePayData.paymentMethodData?.toPaymentData()
            )
        )

        return sumUpDataSource.processCheckout(
            requestBody = processCheckoutRequest,
            is3DSecure = handle3dsRedirect
        ).mapNotNull { networkResult -> // networkResult est NetWorkResult<CheckoutResponse>
            when (networkResult) {
                is NetWorkResult.Success -> {
                    val checkoutResponse = networkResult.data // Ceci est CheckoutResponse
                    // Le DataSource est censé avoir attendu un statut final.
                    if (checkoutResponse.status == CHECKOUT_STATUS.PAID || checkoutResponse.status == CHECKOUT_STATUS.FAILED) {
                        checkoutResponse.toDomainCheckout()
                    } else {
                        // Ce cas ne devrait pas arriver si le DataSource respecte le contrat de ne retourner que l'état final.
                        Log.w(
                            "ProcessCheckout",
                            "DataSource returned non-final status: ${checkoutResponse.status} for checkout ${checkoutResponse.id}"
                        )
                        // Pour l'instant, on le filtre en retournant null (mapNotNull).
                        // Alternativement, si DomainCheckout peut représenter PENDING : checkoutResponse.toDomainCheckout()
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
    // ORDERS
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
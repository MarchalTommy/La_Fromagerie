package com.mtdevelopment.checkout.data.repository

import android.content.Context
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.remote.model.Constants
import com.mtdevelopment.checkout.data.remote.model.request.CHECKOUT_CREATION_BODY_PURPOSE
import com.mtdevelopment.checkout.data.remote.model.request.CheckoutCreationBody
import com.mtdevelopment.checkout.data.remote.model.request.PersonalDetails
import com.mtdevelopment.checkout.data.remote.model.request.ProcessCheckoutRequest
import com.mtdevelopment.checkout.data.remote.model.request.toPaymentData
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.toNewCheckoutResult
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.toProcessCheckoutResult
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
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
//    private val datastore: CheckoutDatastorePreferenceImpl
) : PaymentRepository {

//    init {
//        initClientToken()
//    }

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
//    private fun initClientToken() {
//        // TODO: GET TOKENs FROM LOCAL STORAGE (JETPACK DATASTORE I THINK ?) Try with api key and if it does not work, go with OAUTH 2.0
////        val token = datastore.sumUpTokenFlow.first()
////        val refresh = datastore.sumUpRefreshTokenFlow.first()
//        val bearerTokens = BearerTokens(BuildConfig.SUMUP_PRIVATE_KEY, null)
//        sumUpDataSource.initClientToken(bearerTokens)
//    }

//    private fun generateTokenRequestBody(
//        clientId: String,
//        clientSecret: String,
//        authCode: String,
//        grantType: GrantType,
//        refreshToken: String? = null
//    ): TokenRequest {
//        return TokenRequest(
//            clientId = clientId,
//            clientSecret = clientSecret,
//            authCode = authCode,
//            grantType = grantType,
//            refreshToken = refreshToken
//        )
//    }

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
                id = "${reference.hashCode()}",
                personalDetails = PersonalDetails(

                ),
                purpose = CHECKOUT_CREATION_BODY_PURPOSE.CHECKOUT,
//                merchantCode = "BuildConfig.SUMUP_MERCHANT_ID",
                merchantCode = BuildConfig.SUMUP_MERCHANT_ID_TEST
            )
        ).transform { value ->
            if (value.data != null) {
                emit(value.data!!.toNewCheckoutResult())
            }
        }
    }

    override fun processCheckout(
        checkoutId: String,
        googlePayData: GooglePayData
    ): Flow<ProcessCheckoutResult> {
        return sumUpDataSource.processCheckout(
            ProcessCheckoutRequest(
                id = checkoutId,
                currency = "EUR",
                googlePay = ProcessCheckoutRequest.GooglePay(
                    apiVersion = googlePayData.apiVersion,
                    apiVersionMinor = googlePayData.apiVersionMinor,
                    paymentMethodData = googlePayData.paymentMethodData?.toPaymentData()
                )
            )
        ).transform { value -> value.data?.toProcessCheckoutResult() }
    }
}
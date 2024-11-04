package com.mtdevelopment.checkout.data.repository

import android.content.Context
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.remote.model.Constants
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import io.ktor.client.plugins.auth.providers.BearerTokens
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
    private fun initClientToken() {
        // TODO: GET TOKENs FROM LOCAL STORAGE (JETPACK DATASTORE I THINK ?) Try with api key and if it does not work, go with OAUTH 2.0
//        val token = datastore.sumUpTokenFlow.first()
//        val refresh = datastore.sumUpRefreshTokenFlow.first()
        val bearerTokens = BearerTokens(BuildConfig.SUMUP_PRIVATE_KEY, null)
        sumUpDataSource.initClientToken(bearerTokens)
    }

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

    override fun createNewCheckout() {
        TODO("Not yet implemented")
    }

    override fun processCheckout(id: String) {
        TODO("Not yet implemented")
    }
}
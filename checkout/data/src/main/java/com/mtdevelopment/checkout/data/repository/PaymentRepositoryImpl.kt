package com.mtdevelopment.checkout.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.local.CheckoutDatastorePreferenceImpl
import com.mtdevelopment.checkout.data.remote.model.allowedAuthMethods
import com.mtdevelopment.checkout.data.remote.model.allowedCardNetworks
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.tasks.await
import org.json.JSONException
import org.json.JSONObject

class PaymentRepositoryImpl(
    private val context: Context,
    private val sumUpDataSource: SumUpDataSource,
//    private val datastore: CheckoutDatastorePreferenceImpl
) : PaymentRepository {

    ///////////////////////////////////////////////////////////////////////////
    // GOOGLE PAY PART
    ///////////////////////////////////////////////////////////////////////////
    override val baseRequest: JSONObject =
        JSONObject().put("apiVersion", 2).put("apiVersionMinor", "0")

    private var client: PaymentsClient? = null

    override val cardPaymentMethod: JSONObject =
        baseCardPaymentMethod().put("tokenizationSpecification", gatewayTokenizationSpecification())

    private fun transactionInfo(price: String): JSONObject =
        JSONObject()
            .put("totalPrice", price)
            .put("totalPriceStatus", "FINAL")
            .put("countryCode", "FR")
            .put("currencyCode", "EUR")

    private val merchantInfo: JSONObject =
        JSONObject().put("merchantName", "EARL Des Laizinnes")
//            .put("merchantId", "123809134581094")
    // TODO: Here replace merchant ID shit by the real one given by google after doing what needs to be done
    // TODO: MAY NOT BE NECESSARY ON ANDROID ? Not specified in google tutorial, but is specified on SumUp Doc... Try and see !


    override fun createPaymentsClient(): PaymentsClient? {
        client = Wallet.getPaymentsClient(
            context,
            Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )
        return client
    }

    override suspend fun fetchCanUseGooglePay(): Boolean? {
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequest().toString())
        return client?.isReadyToPay(request)?.await()
    }

    override fun getLoadPaymentDataTask(
        price: String
    ): Task<PaymentData>? {
        val paymentDataRequestJson =
            generatePaymentDataRequest(transactionInfo(price), merchantInfo)
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
        return client?.loadPaymentData(request)
    }

    private fun isReadyToPayRequest(): JSONObject? =
        try {
            baseRequest
                .put("allowedPaymentMethods", cardPaymentMethod)
        } catch (e: JSONException) {
            null
        }

    private fun baseCardPaymentMethod(): JSONObject =
        JSONObject()
            .put("type", "CARD")
            .put(
                "parameters", JSONObject()
                    .put("allowedAuthMethods", allowedAuthMethods)
                    .put("allowedCardNetworks", allowedCardNetworks)
                    .put("billingAddressRequired", true)
                    .put(
                        "billingAddressParameters", JSONObject()
                            .put("format", "FULL")
                    )
            )

    private fun gatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put(
                "parameters", JSONObject(
                    mapOf(
                        "gateway" to "sumup",
                        "gatewayMerchantId" to BuildConfig.SUMUP_MERCHANT_ID
                    )
                )
            )
        }
    }

    private fun generatePaymentDataRequest(
        transactionInfo: JSONObject,
        merchantInfo: JSONObject
    ): JSONObject {
        return baseRequest
            .put("allowedPaymentMethods", cardPaymentMethod)
            .put("transactionInfo", transactionInfo)
            .put("merchantInfo", merchantInfo)
    }

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
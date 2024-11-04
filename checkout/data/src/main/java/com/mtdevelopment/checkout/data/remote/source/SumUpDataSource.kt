package com.mtdevelopment.checkout.data.remote.source

import com.mtdevelopment.checkout.data.remote.model.request.CheckoutCreationBody
import com.mtdevelopment.checkout.data.remote.model.request.ProcessCheckoutRequest
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutFromIdResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.NewCheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.ProcessCheckoutResponse
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.core.util.toResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow

class SumUpDataSource(private val httpClient: HttpClient) {

    fun initClientToken(bearerTokens: BearerTokens?) {
        httpClient.config {
            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokens
                    }
//                    refreshTokens {
//                        // TODO: DO WHAT'S NEEDED TO REFRESH THE TOKEN, AND RETURN THE NEW PAIR HERE :
//                        BearerTokens(TODO("BEARER"), TODO("REFRESH"))
//                    }
                }
            }
        }
    }

//    fun getToken(
//        request: TokenRequest
//    ): Flow<NetWorkResult<TokenResponse>> {
//        return toResultFlow {
//            val response = httpClient.get {
//                url {
//                    path("token")
//                }
//                setBody(request)
//            }.body<TokenResponse>()
//            NetWorkResult.Success(response)
//        }
//    }

    fun getCheckoutsList(reference: String? = null): Flow<NetWorkResult<List<CheckoutResponse?>>> {
        return toResultFlow {
            val response = httpClient.get {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                    if (reference != null) {
                        parameters.append("checkout_reference", "$reference")
                    }
                }
            }.body<List<CheckoutResponse?>>()
            NetWorkResult.Success(response)
        }
    }

    fun getCheckoutFromId(id: String): Flow<NetWorkResult<CheckoutFromIdResponse>> {
        return toResultFlow {
            val response = httpClient.get {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                    parameters.append("id", id)

                }
            }.body<CheckoutFromIdResponse>()
            NetWorkResult.Success(response)
        }
    }

    fun createNewCheckout(body: CheckoutCreationBody): Flow<NetWorkResult<NewCheckoutResponse?>> {
        return toResultFlow {
            val response = httpClient.post {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                    setBody(body)
                }
            }.body<NewCheckoutResponse?>()
            NetWorkResult.Success(response)
        }
    }

    fun processCheckout(body: ProcessCheckoutRequest): Flow<NetWorkResult<ProcessCheckoutResponse?>> {
        return toResultFlow {
            val response = httpClient.put {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                    body.id?.let {
                        parameters.append("id", it)
                    }
                    setBody(body)
                }
            }.body<ProcessCheckoutResponse?>()
            NetWorkResult.Success(response)
        }
    }
}
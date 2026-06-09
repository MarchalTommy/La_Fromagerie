package com.mtdevelopment.checkout.data.remote.source

import android.util.Log
import com.mtdevelopment.checkout.data.remote.model.request.CheckoutCreationBody
import com.mtdevelopment.checkout.data.remote.model.request.ProcessCheckoutRequest
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.NewCheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.ProcessCheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.toNextStep
import com.mtdevelopment.checkout.domain.model.ProcessCheckoutResult
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.core.util.toResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

// TODO:   Error processing checkout ac86fa64-d0d0-4072-ae23-d48463525e12: 500 Internal Server Error.
//  Body: {"error_code":"INTERNAL_SERVER_ERROR","message":"Internal Server Error"}

/**
 * Internal marker to indicate that the initial PUT request to process a checkout 
 * was accepted (202) but is still processing (e.g., waiting for 3DS or backend authorization).
 */
private data class InitialPutAccepted(val checkoutId: String)

/**
 * Data source for interacting with the SumUp API.
 * This class handles the complexity of the payment lifecycle, including:
 * - Checkout session creation.
 * - Processing payments with Google Pay tokens.
 * - Handling 3D Secure verification flows.
 * - Polling for final transaction status (PAID/FAILED).
 */
private const val MAX_POLLING_ATTEMPTS =
    40 // e.g., 40 attempts * 3 seconds = 2 minutes max (covers most 3DS scenarios)
private const val POLLING_INTERVAL_MS = 3000L // 3 seconds

class SumUpDataSource(private val httpClient: HttpClient) {

    /**
     * Retrieves a list of checkout sessions, optionally filtered by reference.
     */
    fun getCheckoutsList(reference: String? = null): Flow<NetWorkResult<List<CheckoutResponse?>>> {
        return toResultFlow {
            val response = httpClient.get {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                    if (reference != null) {
                        parameters.append("checkout_reference", reference)
                    }
                }
            }.body<List<CheckoutResponse?>>()
            NetWorkResult.Success(response)
        }
    }

    /**
     * Fetches detailed information about a specific checkout session by its ID.
     */
    fun getCheckoutFromId(id: String): Flow<NetWorkResult<CheckoutResponse>> {
        return toResultFlow {
            try {
                val response = httpClient.get {
                    url {
                        path("v0.1/checkouts/$id")
                    }
                }
                if (response.status == HttpStatusCode.OK) {
                    NetWorkResult.Success(response.body<CheckoutResponse>())
                } else {
                    Log.e("GetCheckoutError", "Error fetching checkout $id: ${response.status}")
                    NetWorkResult.Error(
                        response.status.description,
                        response.status.value.toString()
                    )
                }
            } catch (e: Exception) {
                Log.e("GetCheckoutException", "Exception fetching checkout $id: ${e.message}", e)
                NetWorkResult.Error(e.message ?: "Unknown error", "EXCEPTION")
            }
        }
    }

    /**
     * Step 3 of the payment flow: Creates a new checkout session on SumUp.
     * This must be done before collecting payment information.
     */
    fun createNewCheckout(body: CheckoutCreationBody): Flow<NetWorkResult<NewCheckoutResponse?>> {
        return toResultFlow {
            val response = httpClient.post {
                url {
                    path(
                        "v0.1/checkouts"
                    )
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<NewCheckoutResponse?>()
            NetWorkResult.Success(response)
        }
    }

    /**
     * Step 6 of the payment flow: Processes the checkout using Google Pay data.
     * 
     * This method is complex because it handles the asynchronous nature of card payments:
     * 1. Sends the initial PUT request with payment details.
     * 2. If status is 200 (OK), the payment might be already terminal (PAID/FAILED) or PENDING.
     * 3. If status is 202 (Accepted), it means the payment is being processed (often waiting for 3DS).
     * 4. In case of 202 or PENDING, it initiates a polling mechanism to wait for the final status.
     * 
     * @param requestBody Contains the checkout ID and Google Pay tokenized data.
     * @param on3DSecureRequired Callback triggered if the transaction requires 3DS verification.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun processCheckout(
        requestBody: ProcessCheckoutRequest,
        on3DSecureRequired: (ProcessCheckoutResult.NextStep) -> Unit
    ): Flow<NetWorkResult<CheckoutResponse>> {

        val checkoutIdToProcess = requestBody.id ?: return flowOf(
            NetWorkResult.Error(
                "ID de checkout manquant dans la requête.",
                "MISSING_ID"
            )
        )

        return toResultFlow<Any?> {
            // The response can be CheckoutResponse (200 OK) or ProcessCheckoutResponse (202 Accepted)
            val result = httpClient.put {
                url { path("v0.1/checkouts/$checkoutIdToProcess") }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            when (result.status) {
                HttpStatusCode.Accepted -> {
                    // 202 Accepted: The payment requires further action or is being processed.
                    val response3DS = result.body<ProcessCheckoutResponse>()

                    // Notify UI if 3DS is required. The UI will then launch the 3DS Activity.
                    response3DS.nextStep?.let { on3DSecureRequired(it.toNextStep()) }

                    Log.i("ProcessCheckoutInfo", "3DS Required or Processing. Polling started.")
                    NetWorkResult.Success(InitialPutAccepted(checkoutIdToProcess))
                }

                HttpStatusCode.OK -> {
                    // 200 OK: Initial request succeeded.
                    val checkoutResponse = result.body<CheckoutResponse>()
                    Log.i(
                        "ProcessCheckoutInfo",
                        "Checkout $checkoutIdToProcess returned 200 OK with status: ${checkoutResponse.status}"
                    )
                    NetWorkResult.Success(checkoutResponse)
                }

                else -> {
                    // Error case
                    val errorBody = try {
                        result.body<String>()
                    } catch (e: Exception) {
                        "No error body"
                    }
                    Log.e(
                        "ProcessCheckoutError",
                        "Error processing checkout $checkoutIdToProcess: ${result.status}. Body: $errorBody"
                    )
                    NetWorkResult.Error(
                        "Erreur ${result.status.value}: ${result.status.description}. Détail: $errorBody",
                        result.status.value.toString()
                    )
                }
            }
        }.flatMapLatest { initialPutResult ->
            // Chain the initial request with the polling logic if needed.
            when (initialPutResult) {
                is NetWorkResult.Success -> {
                    when (val data = initialPutResult.data) {
                        is InitialPutAccepted -> {
                            // Case 202: Start polling immediately
                            pollCheckoutStatus(data.checkoutId)
                        }

                        is CheckoutResponse -> {
                            // Case 200: Check if it's already terminal or if we need to poll
                            if (data.status == CHECKOUT_STATUS.PENDING) {
                                data.id?.let { pollCheckoutStatus(it) }
                                    ?: flowOf(
                                        NetWorkResult.Error(
                                            "ID manquant dans CheckoutResponse pour polling.",
                                            "MISSING_CHECKOUT_ID_IN_RESPONSE"
                                        )
                                    )
                            } else {
                                // Final status (PAID, FAILED) already reached
                                flowOf(NetWorkResult.Success(data))
                            }
                        }

                        else -> {
                            flowOf(
                                NetWorkResult.Error(
                                    "Type de réponse interne inattendu après PUT.",
                                    "INTERNAL_TYPE_ERROR"
                                )
                            )
                        }
                    }
                }

                is NetWorkResult.Error -> {
                    flowOf(initialPutResult)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Polls the SumUp API until the checkout session reaches a terminal state (PAID or FAILED).
     * This is essential for payments that require background processing or user verification (3DS).
     * 
     * // TODO: If the app is killed, this polling loop stops. 
     * // TODO: Suggest implementing a WorkManager task or a more robust state recovery mechanism on app launch to resume polling for pending checkouts.
     */
    private fun pollCheckoutStatus(checkoutId: String): Flow<NetWorkResult<CheckoutResponse>> {
        return flow {
            for (attempt in 0 until MAX_POLLING_ATTEMPTS) {
                if (!currentCoroutineContext().isActive) {
                    Log.i("PollCheckoutStatus", "Polling cancelled for $checkoutId")
                    break
                }

                Log.d(
                    "PollCheckoutStatus",
                    "Attempt ${attempt + 1}/$MAX_POLLING_ATTEMPTS to get status for checkout $checkoutId"
                )
                var shouldContinuePolling = false

                val result: NetWorkResult<CheckoutResponse> = try {
                    getCheckoutFromId(checkoutId).first()
                } catch (e: Exception) {
                    Log.e(
                        "PollCheckoutStatus",
                        "Exception when calling getCheckoutFromId($checkoutId).first(): ${e.message}",
                        e
                    )
                    emit(
                        NetWorkResult.Error(
                            "Erreur interne lors de la récupération du statut: ${e.message}",
                            "GET_STATUS_EXCEPTION"
                        )
                    )
                    return@flow 
                }

                when (result) {
                    is NetWorkResult.Success -> {
                        val checkoutResponse = result.data
                        Log.i(
                            "PollCheckoutStatus",
                            "Status for $checkoutId is ${checkoutResponse.status}"
                        )
                        when (checkoutResponse.status) {
                            CHECKOUT_STATUS.PAID, CHECKOUT_STATUS.FAILED -> {
                                // Terminal status reached!
                                emit(NetWorkResult.Success(checkoutResponse))
                                return@flow 
                            }

                            CHECKOUT_STATUS.PENDING -> {
                                // Still processing, continue the loop
                                shouldContinuePolling = true
                            }

                            null -> { 
                                Log.w(
                                    "PollCheckoutStatus",
                                    "Checkout status is null for $checkoutId. Treating as PENDING for polling."
                                )
                                shouldContinuePolling = true
                            }
                        }
                    }

                    is NetWorkResult.Error -> {
                        Log.e(
                            "PollCheckoutStatus",
                            "Error polling $checkoutId: ${result.message}"
                        )
                        emit(result) 
                        return@flow
                    }
                }

                if (!shouldContinuePolling) {
                    // Logic safety net: stop if we are in an unexpected state
                    if (currentCoroutineContext().isActive) {
                        Log.w(
                            "PollCheckoutStatus",
                            "Polling logic decided not to continue for $checkoutId without reaching a final state."
                        )
                        emit(
                            NetWorkResult.Error(
                                "Erreur logique de polling ou statut inattendu",
                                "POLLING_LOGIC_ERROR_OR_UNEXPECTED_STATUS"
                            )
                        )
                    }
                    return@flow
                }

                // Wait before next attempt
                if (attempt < MAX_POLLING_ATTEMPTS - 1 && currentCoroutineContext().isActive) {
                    delay(POLLING_INTERVAL_MS)
                }
            }

            // Timeout reached without terminal status
            if (currentCoroutineContext().isActive) {
                Log.w(
                    "PollCheckoutStatus",
                    "Polling timeout for checkout $checkoutId after $MAX_POLLING_ATTEMPTS attempts."
                )
                emit(
                    NetWorkResult.Error(
                        "Timeout du polling du statut du checkout.",
                        "POLLING_TIMEOUT"
                    )
                )
            }
        }.flowOn(Dispatchers.IO)
    }
}
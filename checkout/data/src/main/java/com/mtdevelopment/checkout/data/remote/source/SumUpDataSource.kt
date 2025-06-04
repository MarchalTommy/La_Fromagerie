package com.mtdevelopment.checkout.data.remote.source

import android.util.Log
import com.mtdevelopment.checkout.data.remote.model.request.CheckoutCreationBody
import com.mtdevelopment.checkout.data.remote.model.request.ProcessCheckoutRequest
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CHECKOUT_STATUS
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.CheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.NewCheckoutResponse
import com.mtdevelopment.checkout.data.remote.model.response.sumUp.ProcessCheckoutResponse
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

// Classe interne pour marquer le succès initial du PUT avec un statut 202
private data class InitialPutAccepted(val checkoutId: String)

// Constantes pour le polling
private const val MAX_POLLING_ATTEMPTS =
    40 // Ex: 40 tentatives * 3 secondes = 2 minute max (pour les cas 3DS par exemple)
private const val POLLING_INTERVAL_MS = 3000L // 3 secondes

class SumUpDataSource(private val httpClient: HttpClient) {

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

    // TODO: FINISH THIS SHIT ONCE THEY ANSWER ME, AS I'M CURRENTLY FACING AN ISSUE ->
    //  Erreur 409: Conflict. Détail: {"error_code":"NON_EXTDEV_PAYMENT_METHOD","message":"Payment method not allowed for ExtDev accounts"}
    @OptIn(ExperimentalCoroutinesApi::class)
    fun processCheckout(
        requestBody: ProcessCheckoutRequest,
        is3DSecure: (String?) -> Unit
    ): Flow<NetWorkResult<CheckoutResponse>> {

        val checkoutIdToProcess = requestBody.id ?: return flowOf(
            NetWorkResult.Error(
                "ID de checkout manquant dans la requête.",
                "MISSING_ID"
            )
        )

        return toResultFlow<Any?> { // Utilise Any? car la réponse peut être CheckoutResponse (200 OK)
            // ou ProcessCheckoutResponse (202 Accepted) pour un 3DS
            val result = httpClient.put {
                url { path("v0.1/checkouts/$checkoutIdToProcess") }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            when (result.status) {
                HttpStatusCode.Accepted -> {
                    // Le traitement a été accepté, il faut lancer la requête 3DS, nous devons poller.
                    is3DSecure.invoke(result.body<ProcessCheckoutResponse>().nextStep?.url)
                    Log.i(
                        "ProcessCheckoutInfo",
                        "Checkout $checkoutIdToProcess processing accepted (202). Starting polling."
                    )
                    NetWorkResult.Success(InitialPutAccepted(checkoutIdToProcess))
                }

                HttpStatusCode.OK -> {
                    // Le traitement a peut-être déjà un statut final.
                    val checkoutResponse = result.body<CheckoutResponse>()
                    Log.i(
                        "ProcessCheckoutInfo",
                        "Checkout $checkoutIdToProcess returned 200 OK with status: ${checkoutResponse.status}"
                    )
                    NetWorkResult.Success(checkoutResponse)
                }

                else -> {
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
            when (initialPutResult) {
                is NetWorkResult.Success -> {
                    when (val data = initialPutResult.data) {
                        is InitialPutAccepted -> {
                            // Cas 202: Commencer le polling
                            pollCheckoutStatus(data.checkoutId)
                        }

                        is CheckoutResponse -> {
                            // Cas 200 initial: Vérifier le statut
                            if (data.status == CHECKOUT_STATUS.PENDING) {
                                data.id?.let { pollCheckoutStatus(it) }
                                    ?: flowOf(
                                        NetWorkResult.Error(
                                            "ID manquant dans CheckoutResponse pour polling.",
                                            "MISSING_CHECKOUT_ID_IN_RESPONSE"
                                        )
                                    )
                            } else {
                                // Statut final (PAID, FAILED), retourner directement
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

    private fun pollCheckoutStatus(checkoutId: String): Flow<NetWorkResult<CheckoutResponse>> {
        return flow {
            for (attempt in 0 until MAX_POLLING_ATTEMPTS) {
                if (!currentCoroutineContext().isActive) {
                    Log.i("PollCheckoutStatus", "Polling cancelled for $checkoutId")
                    break // Sort de la boucle for
                }

                Log.d(
                    "PollCheckoutStatus",
                    "Attempt ${attempt + 1}/$MAX_POLLING_ATTEMPTS to get status for checkout $checkoutId"
                )
                var shouldContinuePolling = false

                // Utiliser .first() ici car getCheckoutFromId émet une seule valeur
                val result: NetWorkResult<CheckoutResponse> = try {
                    getCheckoutFromId(checkoutId).first()
                } catch (e: Exception) {
                    // .first() peut lever NoSuchElementException si le Flow est vide,
                    // mais toResultFlow garantit une émission.
                    // Capturer d'autres exceptions potentielles de la récupération de l'élément.
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
                    return@flow // Terminer le flux de polling en cas d'exception imprévue ici
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
                                emit(NetWorkResult.Success(checkoutResponse)) // Statut final atteint
                                return@flow // Terminer le flux de polling
                            }

                            CHECKOUT_STATUS.PENDING -> {
                                shouldContinuePolling = true // Continuer le polling
                            }

                            null -> { // Statut est nullable dans CheckoutResponse
                                Log.w(
                                    "PollCheckoutStatus",
                                    "Checkout status is null for $checkoutId. Treating as PENDING for polling."
                                )
                                shouldContinuePolling = true // Continuer le polling
                            }
                        }
                    }

                    is NetWorkResult.Error -> {
                        Log.e(
                            "PollCheckoutStatus",
                            "Error polling $checkoutId: ${result.message}"
                        )
                        emit(result) // Propager l'erreur de getCheckoutFromId
                        return@flow
                    }
                }

                if (!shouldContinuePolling) {
                    // Ce bloc est atteint si NetWorkResult.Success a été reçu,
                    // mais le statut n'était ni PAID, FAILED, PENDING, ni null (ou un autre cas qui n'a pas mis shouldContinuePolling à true).
                    // Ou si un 'else' dans le when(checkoutResponse.status) a été atteint sans return@flow.
                    if (currentCoroutineContext().isActive) {
                        Log.w(
                            "PollCheckoutStatus",
                            "Polling logic decided not to continue for $checkoutId without reaching a final state. Last known status: ${(result as? NetWorkResult.Success)?.data?.status}"
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

                // Attendre avant la prochaine tentative, sauf si c'est la dernière
                if (attempt < MAX_POLLING_ATTEMPTS - 1 && currentCoroutineContext().isActive) {
                    delay(POLLING_INTERVAL_MS)
                }
            }

            // Si la boucle se termine après toutes les tentatives sans statut final
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
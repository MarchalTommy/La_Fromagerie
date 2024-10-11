package com.mtdevelopment.core.util

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> toResultFlow(call: suspend () -> NetWorkResult<T?>): Flow<NetWorkResult<T>> {
    return flow {
        emit(NetWorkResult.Loading(true))
        val c = call.invoke()
        c.let { response ->
            try {
                println("response${response.data}")
                emit(NetWorkResult.Success(response.data))
            } catch (e: Exception) {
                emit(NetWorkResult.Error(response.data, e.message ?: ""))
            }
        }
    }
}

sealed class NetWorkResult<out T>(val status: ApiStatus, val data: T?, val message: String?) {
    data class Success<out T>(val _data: T?) :
        NetWorkResult<T>(status = ApiStatus.SUCCESS, data = _data, message = null)

    data class Error<out T>(val _data: T?, val exception: String) :
        NetWorkResult<T>(status = ApiStatus.ERROR, data = _data, message = exception)

    data class Loading<out T>(val isLoading: Boolean) :
        NetWorkResult<T>(status = ApiStatus.LOADING, data = null, message = null)
}

enum class ApiStatus {
    SUCCESS,
    ERROR,
    LOADING,
}
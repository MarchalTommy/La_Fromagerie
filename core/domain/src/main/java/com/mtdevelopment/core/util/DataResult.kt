package com.mtdevelopment.core.util

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Error(val exception: Throwable? = null, val message: String? = null) :
        DataResult<Nothing>()

    object Loading : DataResult<Nothing>()

    fun <R> map(transform: (T) -> R): DataResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(exception, message)
            is Loading -> Loading
        }
    }
}

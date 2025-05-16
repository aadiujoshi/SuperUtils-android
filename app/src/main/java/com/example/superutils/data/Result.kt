package com.example.superutils.data

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: String) : Result<Nothing>()
}

val <T> Result<T>.isSuccess: Boolean
    get() = this is Result.Success

val <T> Result<T>.isFailure: Boolean
    get() = this is Result.Failure

val <T> Result<T>.print: Unit
    get() {
        when (this) {
            is Result.Success -> println("Success: $data")
            is Result.Failure -> println("Failure: $error")
        }
    }
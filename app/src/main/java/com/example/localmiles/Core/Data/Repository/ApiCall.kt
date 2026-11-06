package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Utils.Result
import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs a suspending API call and maps the usual failures to Result.Error with a
 * message that is safe to show in the UI.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(friendlyErrorMessage(e), e)
    }

/**
 * Turns any API failure into one short sentence a dispatcher can act on.
 */
fun friendlyErrorMessage(e: Throwable): String = when (e) {
    is HttpException -> {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        httpErrorMessage(e.code(), body)
    }
    is IOException -> "Network error — check your connection and try again"
    else -> e.message ?: "Unexpected error"
}

/** Pure mapping from an HTTP status + error body to UI text. */
fun httpErrorMessage(code: Int, body: String?): String {
    val serverMessage = extractServerError(body)

    duplicateFieldName(serverMessage)?.let { field ->
        return "That ${humanise(field)} is already in use. Please enter a different one."
    }

    return when {
        code == 401 -> "Your session has expired. Please log in again."
        code == 403 -> serverMessage ?: "You don't have permission to do that."
        code == 404 -> serverMessage ?: "That record could not be found."
        code == 409 -> serverMessage?.removePrefix("Conflict: ")
            ?: "That change conflicts with the record's current state."
        code >= 500 -> "The server had a problem (${code}). Please try again shortly."
        serverMessage != null -> serverMessage
        else -> "Request failed ($code)"
    }
}

private fun extractServerError(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        JsonParser.parseString(body).asJsonObject.get("error")?.asString
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: body.takeIf { !it.trimStart().startsWith("{") && !it.trimStart().startsWith("<") }
}

private val DUPLICATE_KEY = Regex("""Key \((\w+)\)=\(.*\) already exists""")

private fun duplicateFieldName(message: String?): String? =
    message?.let { DUPLICATE_KEY.find(it)?.groupValues?.get(1) }

// "EmployeeNumber" -> "employee number"
private fun humanise(field: String): String =
    field.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()

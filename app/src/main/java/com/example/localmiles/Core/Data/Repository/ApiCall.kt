package com.example.localmiles.Core.Data.Repository

import com.example.localmiles.Core.Utils.Result
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs a suspending API call and maps the usual failures to Result.Error with a
 * message that is safe to show in the UI.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (e: HttpException) {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        Result.Error("Server error ${e.code()}${if (!body.isNullOrBlank()) ": $body" else ""}", e)
    } catch (e: IOException) {
        Result.Error("Network error — check your connection and try again", e)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unexpected error", e)
    }

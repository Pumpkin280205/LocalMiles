package com.example.localmiles.Core.Network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

//Wraps a plain value as a text/plain multipart form field.
fun String.toPlainRequestBody(): RequestBody =
    this.toRequestBody("text/plain".toMediaTypeOrNull())

fun Any.toPlainRequestBody(): RequestBody = this.toString().toPlainRequestBody()

//Wraps an image file as a multipart file part under the given field name.
fun File.toImagePart(fieldName: String): MultipartBody.Part {
    val mediaType = when (extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        else -> "image/jpeg"
    }.toMediaTypeOrNull()
    return MultipartBody.Part.createFormData(fieldName, name, this.asRequestBody(mediaType))
}

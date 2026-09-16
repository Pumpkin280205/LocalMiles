package com.example.localmiles.Core.Network

import com.example.localmiles.BuildConfig
import com.example.localmiles.Core.Utils.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile
    private var apiService: ApiService? = null

    fun getApiService(sessionManager: SessionManager): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(sessionManager).also { apiService = it }
        }
    }

    private fun buildApiService(sessionManager: SessionManager): ApiService {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // Never attach a token to the login endpoint itself
            val requestBuilder = originalRequest.newBuilder()
            val token = sessionManager.getToken()
            if (!token.isNullOrBlank() && !originalRequest.url.encodedPath.contains("auth/login")) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            requestBuilder.addHeader("Content-Type", "application/json")

            chain.proceed(requestBuilder.build())
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
package com.drive.roadhazard.repositories

import android.util.Log
import com.drive.roadhazard.data.LoginRequest
import com.drive.roadhazard.data.NewHazardRequest
import com.drive.roadhazard.data.RegisterRequest
import com.drive.roadhazard.network.RoadHazardAPI
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class EventRepository {
    companion object {
        private const val TAG = "EventRepository"
        private const val BASE_URL = "https://roadmap-x7c3.onrender.com"
    }

    private val httpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("API_LOGS", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(RoadHazardAPI::class.java) }

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        phoneNumber: String
    ): Boolean {
        val request = RegisterRequest(email, password, name, phoneNumber)
        try {
            val response = api.signUp(request)
            if (response.isSuccessful) {
                return true
            }
        } catch (e: Exception) {
        }
        return false;
    }

    suspend fun signIn(email: String, password: String): String {
        val request = LoginRequest(email, password)
        try {
            val response = api.signIn(request)
            if (response.code() == 200) {
                return response.body()?.token ?: ""
            }
        } catch (e: Exception) {
        }
        return "";
    }

    suspend fun reportHazard(
        token: String,
        latitude: Double,
        longitude: Double,
        type: String,
        description: String? = null
    ) {
        val request = NewHazardRequest(latitude, longitude, type, description)
        try {
            val response = api.reportHazard("Bearer $token", request)
        } catch (e: Exception) {
        }
    }

}
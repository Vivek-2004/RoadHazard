package com.drive.roadhazard.network

import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventUpload
import com.drive.roadhazard.data.LoginRequest
import com.drive.roadhazard.data.LoginResponse
import com.drive.roadhazard.data.NewHazardRequest
import com.drive.roadhazard.data.NewHazardResponse
import com.drive.roadhazard.data.RegisterRequest
import com.drive.roadhazard.data.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface RoadHazardAPI {
    @POST("auth/signup")
    suspend fun signUp(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/signin")
    suspend fun signIn(@Body request: LoginRequest): Response<LoginResponse>

    @POST("hazards/new-report")
    suspend fun reportHazard(
        @Header("Authorization") token: String,
        @Body request: NewHazardRequest
    ): NewHazardResponse

    //

    @POST("events/upload")
    suspend fun uploadEvents(@Body events: EventUpload): List<EventResponse>

    @GET("events/nearby")
    suspend fun getNearbyEvents(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 5.0
    ): List<EventResponse>
}
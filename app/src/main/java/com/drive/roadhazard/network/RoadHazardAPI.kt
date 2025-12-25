package com.drive.roadhazard.network

import com.drive.roadhazard.data.LoginRequest
import com.drive.roadhazard.data.LoginResponse
import com.drive.roadhazard.data.NewHazardRequest
import com.drive.roadhazard.data.NewHazardResponse
import com.drive.roadhazard.data.RegisterRequest
import com.drive.roadhazard.data.RegisterResponse
import com.drive.roadhazard.data.SingleHazardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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

    @GET("hazards/all")
    suspend fun getAllHazards(
        @Header("Authorization") token: String
    ): Response<List<SingleHazardResponse>>
}
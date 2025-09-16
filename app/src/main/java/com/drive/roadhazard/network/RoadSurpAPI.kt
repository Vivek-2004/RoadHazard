package com.drive.roadhazard.network

import com.drive.roadhazard.data.EventResponse
import com.drive.roadhazard.data.EventUpload
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RoadSurpAPI {
    @POST("events/upload")
    suspend fun uploadEvents(@Body events: EventUpload): List<EventResponse>

    @GET("events/nearby")
    suspend fun getNearbyEvents(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 5.0
    ): List<EventResponse>
}
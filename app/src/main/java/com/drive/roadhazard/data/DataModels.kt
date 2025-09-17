package com.drive.roadhazard.data

import java.util.UUID

data class User(
    val username: String,
    val password: String
)

data class RoadEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: EventType,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val confidence: Float,
    val speed: Float,
    val confirmed: Boolean = false
)

// API Models
data class EventUpload(
    val events: List<RoadEvent>
)

data class EventResponse(
    val latitude: Double,
    val longitude: Double,
    val type: String
)

enum class EventType { SPEED_BREAKER, POTHOLE, BROKEN_PATCH }
enum class VehicleType { TWO_WHEELER, THREE_WHEELER, FOUR_WHEELER, BUS }
enum class PhoneOrientation { POCKET, MOUNTER, DASHBOARD }
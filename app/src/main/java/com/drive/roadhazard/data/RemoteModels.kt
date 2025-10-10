package com.drive.roadhazard.data

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phoneNumber: String
)

data class RegisterResponse(
    val message: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String? = "",
    val user: RemoteUser? = null
)

data class RemoteUser(
    val id: Int
)

data class NewHazardRequest(
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val description: String? = null
)

data class NewHazardResponse(
    val message: String,
    val hazardId: String
)
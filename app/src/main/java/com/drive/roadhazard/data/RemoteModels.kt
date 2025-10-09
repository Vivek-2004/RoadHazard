package com.drive.roadhazard.data

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phoneNumber: String
)

data class RegisterResponse(
    val message: String,
    val user: RemoteUser
)


data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: RemoteUser
)

data class RemoteUser(
    val id: Int
)
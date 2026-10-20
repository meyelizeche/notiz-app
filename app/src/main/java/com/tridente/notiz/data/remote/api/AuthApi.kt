package com.tridente.notiz.data.remote.api

import com.tridente.notiz.data.remote.dto.AuthResponse
import com.tridente.notiz.data.remote.dto.LoginRequest
import com.tridente.notiz.data.remote.dto.RefreshTokenRequest
import com.tridente.notiz.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>
}

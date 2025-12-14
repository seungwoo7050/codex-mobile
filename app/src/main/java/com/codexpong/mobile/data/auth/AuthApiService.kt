package com.codexpong.mobile.data.auth

import com.codexpong.mobile.data.auth.model.AuthResponse
import com.codexpong.mobile.data.auth.model.LoginRequest
import com.codexpong.mobile.data.auth.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 인증 관련 REST API 정의.
 */
interface AuthApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/logout")
    suspend fun logout(): Map<String, String>
}

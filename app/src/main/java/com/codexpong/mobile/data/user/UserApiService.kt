package com.codexpong.mobile.data.user

import com.codexpong.mobile.data.user.model.ProfileUpdateRequest
import com.codexpong.mobile.data.user.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * 사용자 관련 API 정의.
 */
interface UserApiService {
    @GET("/api/users/me")
    suspend fun getProfile(): UserResponse

    @PUT("/api/users/me")
    suspend fun updateProfile(
        @Body payload: ProfileUpdateRequest
    ): UserResponse
}

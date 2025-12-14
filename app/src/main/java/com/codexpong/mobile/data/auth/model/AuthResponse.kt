package com.codexpong.mobile.data.auth.model

import com.codexpong.mobile.data.user.model.UserResponse
import com.squareup.moshi.JsonClass

/**
 * 인증 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String?,
    val expiresAt: String?,
    val user: UserResponse?
)

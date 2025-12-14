package com.codexpong.mobile.data.user.model

import com.squareup.moshi.JsonClass

/**
 * 사용자 프로필 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Long?,
    val username: String?,
    val nickname: String?,
    val avatarUrl: String?,
    val rating: Int?,
    val authProvider: String?,
    val locale: String?,
    val createdAt: String?,
    val updatedAt: String?
)

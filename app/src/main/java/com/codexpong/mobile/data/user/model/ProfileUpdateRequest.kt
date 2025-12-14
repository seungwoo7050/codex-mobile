package com.codexpong.mobile.data.user.model

import com.squareup.moshi.JsonClass

/**
 * 내 프로필 수정 요청 모델.
 */
@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    val nickname: String,
    val avatarUrl: String?
)

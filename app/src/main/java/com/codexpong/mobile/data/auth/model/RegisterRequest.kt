package com.codexpong.mobile.data.auth.model

import com.squareup.moshi.JsonClass

/**
 * 회원가입 요청 페이로드.
 */
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String,
    val avatarUrl: String?
)

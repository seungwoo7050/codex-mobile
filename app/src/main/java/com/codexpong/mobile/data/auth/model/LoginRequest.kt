package com.codexpong.mobile.data.auth.model

import com.squareup.moshi.JsonClass

/**
 * 로그인 요청 페이로드.
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

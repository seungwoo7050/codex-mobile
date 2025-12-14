package com.codexpong.mobile.core.network

import com.codexpong.mobile.core.auth.AuthTokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authorization 헤더를 자동으로 부착하고 401 응답 시 세션을 정리하는 인터셉터.
 */
class AuthorizationInterceptor(
    private val tokenRepository: AuthTokenRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentToken = runBlocking { tokenRepository.currentToken() }
        val request = if (currentToken.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $currentToken")
                .build()
        }

        val response = chain.proceed(request)
        if (response.code == 401) {
            runBlocking { tokenRepository.clearToken() }
        }
        return response
    }
}

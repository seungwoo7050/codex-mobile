package com.codexpong.mobile.data.auth

import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.auth.model.AuthResponse
import com.codexpong.mobile.data.auth.model.LoginRequest
import com.codexpong.mobile.data.auth.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 로그인, 회원가입, 로그아웃 흐름을 담당하는 저장소.
 */
open class AuthRepository(
    private val baseUrlRepository: BaseUrlRepository,
    private val retrofitProvider: RetrofitProvider,
    private val tokenRepository: AuthTokenRepository
) {
    private suspend fun service(): AuthApiService {
        val baseUrl = baseUrlRepository.currentBaseUrl()
        return retrofitProvider.create(baseUrl).create(AuthApiService::class.java)
    }

    /**
     * 로그인 후 토큰을 저장한다.
     */
    open suspend fun login(username: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = service().login(LoginRequest(username, password))
            response.token?.let { tokenRepository.saveToken(it) }
            response
        }
    }

    /**
     * 회원가입 후 토큰을 저장한다.
     */
    open suspend fun register(
        username: String,
        password: String,
        nickname: String,
        avatarUrl: String?
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = service().register(RegisterRequest(username, password, nickname, avatarUrl))
            response.token?.let { tokenRepository.saveToken(it) }
            response
        }
    }

    /**
     * 서버 로그아웃을 호출하고 토큰을 제거한다.
     */
    open suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            service().logout()
        }.onFailure {
            // 호출 실패와 상관없이 세션은 종료한다.
            tokenRepository.clearToken()
        }.map {
            tokenRepository.clearToken()
        }
    }
}

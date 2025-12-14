package com.codexpong.mobile.data.user

import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.user.model.ProfileUpdateRequest
import com.codexpong.mobile.data.user.model.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자 프로필 조회/수정을 담당하는 저장소.
 */
open class UserRepository(
    private val baseUrlRepository: BaseUrlRepository,
    private val retrofitProvider: RetrofitProvider
) {
    private suspend fun service(): UserApiService {
        val baseUrl = baseUrlRepository.currentBaseUrl()
        return retrofitProvider.create(baseUrl).create(UserApiService::class.java)
    }

    /**
     * /api/users/me 결과를 반환한다.
     */
    open suspend fun fetchProfile(): Result<UserResponse> = withContext(Dispatchers.IO) {
        runCatching { service().getProfile() }
    }

    /**
     * /api/users/me 업데이트를 수행한다.
     */
    open suspend fun updateProfile(request: ProfileUpdateRequest): Result<UserResponse> = withContext(Dispatchers.IO) {
        runCatching { service().updateProfile(request) }
    }
}

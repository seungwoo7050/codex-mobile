package com.codexpong.mobile.data.health

import retrofit2.http.GET

/**
 * 헬스 체크 엔드포인트 정의.
 */
interface HealthApiService {
    @GET("/api/health")
    suspend fun getHealth(): Map<String, String>
}

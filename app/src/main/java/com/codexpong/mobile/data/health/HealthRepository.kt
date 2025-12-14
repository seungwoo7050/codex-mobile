package com.codexpong.mobile.data.health

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * 헬스 체크 요청을 담당하는 저장소.
 */
open class HealthRepository(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi = Moshi.Builder().build()
) {
    /**
     * 지정한 baseUrl로 Retrofit 인스턴스를 매 요청마다 생성한다.
     */
    private fun createService(baseUrl: String): HealthApiService {
        val normalizedBaseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(HealthApiService::class.java)
    }

    /**
     * /api/health 호출 결과를 Map 형태로 반환한다.
     */
    open suspend fun fetchHealth(baseUrl: String): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val service = createService(baseUrl)
                service.getHealth()
            }
        }
    }
}

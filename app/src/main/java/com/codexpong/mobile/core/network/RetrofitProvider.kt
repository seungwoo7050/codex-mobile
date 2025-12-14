package com.codexpong.mobile.core.network

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * 공통 Retrofit 인스턴스를 생성하는 헬퍼.
 */
class RetrofitProvider(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi = Moshi.Builder().build()
) {
    fun create(baseUrl: String): Retrofit {
        val normalizedBaseUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * 공용 OkHttpClient를 그대로 노출해 WebSocket 및 스트리밍 다운로드에 재사용한다.
     */
    fun client(): OkHttpClient = okHttpClient

    /**
     * Moshi 인스턴스를 외부에서 재사용할 수 있도록 제공한다.
     */
    fun moshi(): Moshi = moshi
}

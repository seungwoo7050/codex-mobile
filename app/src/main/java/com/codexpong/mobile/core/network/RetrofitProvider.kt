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
}

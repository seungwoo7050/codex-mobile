package com.codexpong.mobile.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.data.health.HealthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * 애플리케이션 단위에서 단순하게 의존성을 보관하는 컨테이너.
 */
class AppContainer(context: Context) {
    private val dataStore: DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("codex_mobile_settings") }
    )

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // 기본 로거는 디버그용으로 최소한의 요청/응답을 남긴다.
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val baseUrlRepository: BaseUrlRepository = BaseUrlRepository(
        dataStore = dataStore,
        defaultBaseUrl = DEFAULT_BASE_URL
    )

    val healthRepository: HealthRepository = HealthRepository(
        okHttpClient = okHttpClient
    )

    /**
     * health 요청에 사용할 현재 기본 URL을 가져오는 헬퍼.
     */
    fun currentBaseUrl(): String = runBlocking { baseUrlRepository.baseUrl.first() }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }
}

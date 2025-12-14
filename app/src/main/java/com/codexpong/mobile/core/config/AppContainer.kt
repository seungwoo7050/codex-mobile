package com.codexpong.mobile.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.AuthorizationInterceptor
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.auth.AuthRepository
import com.codexpong.mobile.data.health.HealthRepository
import com.codexpong.mobile.data.replay.ReplayRepository
import com.codexpong.mobile.data.user.UserRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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

    val authTokenRepository = AuthTokenRepository(dataStore)

    val baseUrlRepository: BaseUrlRepository = BaseUrlRepository(
        dataStore = dataStore,
        defaultBaseUrl = DEFAULT_BASE_URL
    )

    private val loggingInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // 기본 로거는 디버그용으로 최소한의 요청/응답을 남긴다.
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(AuthorizationInterceptor(authTokenRepository))
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val retrofitProvider: RetrofitProvider = RetrofitProvider(okHttpClient, moshi)

    val authRepository: AuthRepository = AuthRepository(
        baseUrlRepository = baseUrlRepository,
        retrofitProvider = retrofitProvider,
        tokenRepository = authTokenRepository
    )

    val userRepository: UserRepository = UserRepository(
        baseUrlRepository = baseUrlRepository,
        retrofitProvider = retrofitProvider
    )

    val replayRepository: ReplayRepository = ReplayRepository(
        baseUrlRepository = baseUrlRepository,
        retrofitProvider = retrofitProvider
    )

    val healthRepository: HealthRepository = HealthRepository(
        okHttpClient = okHttpClient,
        moshi = moshi
    )

    /**
     * health 요청에 사용할 현재 기본 URL을 가져오는 헬퍼.
     */
    fun currentBaseUrl(): String = runBlocking { baseUrlRepository.baseUrl.first() }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }
}

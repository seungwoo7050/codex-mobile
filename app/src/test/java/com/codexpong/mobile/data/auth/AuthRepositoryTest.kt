package com.codexpong.mobile.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.AuthorizationInterceptor
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * AuthRepository의 기본 동작을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenRepository: AuthTokenRepository
    private lateinit var baseUrlRepository: BaseUrlRepository
    private lateinit var retrofitProvider: RetrofitProvider
    private lateinit var server: MockWebServer

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { Files.createTempFile("auth-test", ".preferences_pb").toOkioPath() },
            corruptionHandler = null,
            migrations = listOf()
        )
        tokenRepository = AuthTokenRepository(dataStore)
        server = MockWebServer()
        server.start()
        baseUrlRepository = object : BaseUrlRepository(dataStore, server.url("/").toString()) {
            override val baseUrl: Flow<String> = flowOf(server.url("/").toString())
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenRepository))
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        retrofitProvider = RetrofitProvider(okHttpClient, moshi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `로그인 성공 시 토큰을 저장한다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"jwt-token","expiresAt":"2024-01-01T00:00:00Z","user":{"id":1,"username":"user","nickname":"닉","avatarUrl":null,"rating":1200,"authProvider":"local","locale":"ko","createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}}"""
            )
        )
        val repository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)

        val result = repository.login("user", "password")

        assertTrue(result.isSuccess)
        assertEquals("jwt-token", tokenRepository.currentToken())
    }

    @Test
    fun `로그인 401이면 실패로 처리하고 토큰을 비운다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        val repository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)

        val result = repository.login("user", "wrong")

        assertTrue(result.isFailure)
        assertEquals(null, tokenRepository.currentToken())
    }

    @Test
    fun `회원가입 성공 시 토큰을 저장한다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"signup-token","expiresAt":"2024-02-01T00:00:00Z","user":{"id":2,"username":"signup","nickname":"회원","avatarUrl":null,"rating":1100,"authProvider":"local","locale":"ko","createdAt":"2024-02-01T00:00:00Z","updatedAt":"2024-02-01T00:00:00Z"}}"""
            )
        )
        val repository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)

        val result = repository.register(username = "signup", password = "pw", nickname = "회원", avatarUrl = null)

        assertTrue(result.isSuccess)
        assertEquals("signup-token", tokenRepository.currentToken())
        val request = server.takeRequest()
        assertEquals("/api/auth/register", request.path)
    }

    @Test
    fun `로그아웃 요청 후 토큰을 제거한다`() = runTest {
        tokenRepository.saveToken("existing-token")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val repository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertNull(tokenRepository.currentToken())
        val request = server.takeRequest()
        assertEquals("/api/auth/logout", request.path)
    }

    @Test
    fun `로그아웃 실패여도 토큰을 제거한다`() = runTest {
        tokenRepository.saveToken("existing-token")
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        val repository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)

        val result = repository.logout()

        assertTrue(result.isFailure)
        assertNull(tokenRepository.currentToken())
    }
}

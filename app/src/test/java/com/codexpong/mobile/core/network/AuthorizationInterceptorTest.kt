package com.codexpong.mobile.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.auth.AuthTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * AuthorizationInterceptor의 헤더 추가 및 401 처리 동작을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizationInterceptorTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenRepository: AuthTokenRepository
    private lateinit var client: OkHttpClient
    private lateinit var server: MockWebServer

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { Files.createTempFile("auth-interceptor", ".preferences_pb").toOkioPath() }
        )
        tokenRepository = AuthTokenRepository(dataStore)
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenRepository))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `저장된 토큰을 Authorization 헤더에 붙인다`() = runTest {
        tokenRepository.saveToken("sample-token")
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder().url(server.url("/protected"))
            .get().build()
        client.newCall(request).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer sample-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `401 응답이면 토큰을 제거한다`() = runTest {
        tokenRepository.saveToken("sample-token")
        server.enqueue(MockResponse().setResponseCode(401))

        val request = Request.Builder().url(server.url("/protected"))
            .get().build()
        client.newCall(request).execute()

        val cleared = tokenRepository.currentToken()
        assertNull(cleared)
    }
}

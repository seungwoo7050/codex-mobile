package com.codexpong.mobile.data.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.AuthorizationInterceptor
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.user.model.ProfileUpdateRequest
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * UserRepository의 GET/PUT 동작을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var retrofitProvider: RetrofitProvider
    private lateinit var baseUrlRepository: BaseUrlRepository
    private lateinit var server: MockWebServer

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { Files.createTempFile("user-repo", ".preferences_pb").toOkioPath() },
            corruptionHandler = null,
            migrations = listOf()
        )
        server = MockWebServer()
        server.start()
        val tokenRepository = AuthTokenRepository(dataStore)
        baseUrlRepository = object : BaseUrlRepository(dataStore, "http://localhost") {
            override val baseUrl: Flow<String> = flowOf(server.url("/").toString())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenRepository))
            .build()
        retrofitProvider = RetrofitProvider(client, Moshi.Builder().add(KotlinJsonAdapterFactory()).build())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `프로필을 GET으로 가져온다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":1,"username":"codex","nickname":"코덱스","avatarUrl":"http://example.com","rating":1300,"authProvider":"local","locale":"ko","createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}"""
            )
        )
        val repository = UserRepository(baseUrlRepository, retrofitProvider)

        val result = repository.fetchProfile()

        assertTrue(result.isSuccess)
        assertEquals("codex", result.getOrThrow().username)
    }

    @Test
    fun `프로필을 PUT으로 업데이트한다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":1,"username":"codex","nickname":"업데이트","avatarUrl":"http://updated","rating":1300,"authProvider":"local","locale":"ko","createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-02T00:00:00Z"}"""
            )
        )
        val repository = UserRepository(baseUrlRepository, retrofitProvider)

        val result = repository.updateProfile(ProfileUpdateRequest(nickname = "업데이트", avatarUrl = "http://updated"))

        assertTrue(result.isSuccess)
        assertEquals("업데이트", result.getOrThrow().nickname)
    }
}

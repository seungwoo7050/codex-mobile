package com.codexpong.mobile.data.replay

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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * ReplayRepository의 목록/상세 파싱을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReplayRepositoryTest {
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
            produceFile = { Files.createTempFile("replay-repo", ".preferences_pb").toOkioPath() },
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
    fun `리플레이 목록을 페이징으로 불러온다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"replayId":1,"matchId":10,"ownerUserId":5,"opponentUserId":6,"opponentNickname":"상대1","matchType":"RANK","myScore":11,"opponentScore":9,"durationMs":120000,"createdAt":"2024-02-01T00:00:00Z","eventFormat":"STANDARD"},{"replayId":2,"matchId":11,"ownerUserId":5,"opponentUserId":7,"opponentNickname":"상대2","matchType":"FRIENDLY","myScore":5,"opponentScore":7,"durationMs":90000,"createdAt":"2024-03-01T00:00:00Z","eventFormat":"STANDARD"}],"page":0,"size":20,"totalElements":2,"totalPages":1}"""
            )
        )
        val repository = ReplayRepository(baseUrlRepository, retrofitProvider)

        val result = repository.fetchReplays(page = 0, size = 20)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(2, response.items?.size)
        assertEquals(1, response.totalPages)
        val request = server.takeRequest()
        assertEquals("/api/replays?page=0&size=20", request.path)
    }

    @Test
    fun `리플레이 상세를 불러온다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"summary":{"replayId":1,"matchId":10,"ownerUserId":5,"opponentUserId":6,"opponentNickname":"상세상대","matchType":"RANK","myScore":11,"opponentScore":9,"durationMs":120000,"createdAt":"2024-02-01T00:00:00Z","eventFormat":"STANDARD"},"checksum":"abc123","downloadPath":"/downloads/replay1"}"""
            )
        )
        val repository = ReplayRepository(baseUrlRepository, retrofitProvider)

        val result = repository.fetchReplayDetail(replayId = 1)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("abc123", response.checksum)
        assertEquals(1L, response.summary?.replayId)
        val request = server.takeRequest()
        assertEquals("/api/replays/1", request.path)
    }
}

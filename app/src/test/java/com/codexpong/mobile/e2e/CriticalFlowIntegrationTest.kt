package com.codexpong.mobile.e2e

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.AuthorizationInterceptor
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.auth.AuthRepository
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.job.ws.JobWebSocketClient
import com.codexpong.mobile.data.job.ws.JobWebSocketEvent
import com.codexpong.mobile.data.replay.ReplayRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * login -> replay list -> export -> ws progress -> download 흐름을 통합 테스트로 고정한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CriticalFlowIntegrationTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var baseUrlRepository: BaseUrlRepository
    private lateinit var retrofitProvider: RetrofitProvider
    private lateinit var tokenRepository: AuthTokenRepository
    private lateinit var server: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { Files.createTempFile("critical-flow", ".preferences_pb").toOkioPath() }
        )
        tokenRepository = AuthTokenRepository(dataStore)
        server = MockWebServer()
        server.start()
        baseUrlRepository = object : BaseUrlRepository(dataStore, "http://localhost") {
            override val baseUrl: Flow<String> = flowOf(server.url("/").toString())
            override suspend fun currentBaseUrl(): String = server.url("/").toString()
        }
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenRepository))
            .build()
        retrofitProvider = RetrofitProvider(okHttpClient, moshi)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `핵심 흐름이 끝까지 성공적으로 이어진다`() = runTest {
        val authRepository = AuthRepository(baseUrlRepository, retrofitProvider, tokenRepository)
        val replayRepository = ReplayRepository(baseUrlRepository, retrofitProvider)
        val jobRepository = JobRepository(baseUrlRepository, retrofitProvider)

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"jwt-token","expiresAt":"2024-12-31T00:00:00Z","user":{"id":1,"username":"tester","nickname":"테스터","avatarUrl":null,"rating":1200,"authProvider":"LOCAL","locale":"ko","createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-02T00:00:00Z"}}"""
            )
        )

        val loginResponse = authRepository.login("tester", "password").getOrThrow()

        assertEquals("jwt-token", loginResponse.token)
        assertEquals("jwt-token", tokenRepository.currentToken())

        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"replayId":5,"matchId":99,"ownerUserId":1,"opponentUserId":2,"opponentNickname":"상대","matchType":"RANK","myScore":3,"opponentScore":1,"durationMs":180000,"createdAt":"2024-06-01T00:00:00Z","eventFormat":"SINGLE"}],"page":0,"size":10,"totalElements":1,"totalPages":1}"""
            )
        )

        val replayPage = replayRepository.fetchReplays(page = 0, size = 10).getOrThrow()

        assertEquals(1, replayPage.items?.size)
        assertEquals(5L, replayPage.items?.firstOrNull()?.replayId)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"jobId":77}"""))

        val jobResponse = jobRepository.requestMp4Export(replayId = 5).getOrThrow()
        val jobId = requireNotNull(jobResponse.jobId) { "jobId should be returned for download" }

        assertEquals(77L, jobId)

        val wsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val wsClient = JobWebSocketClient(
            okHttpClient = okHttpClient,
            baseUrlRepository = baseUrlRepository,
            authTokenRepository = tokenRepository,
            moshi = moshi,
            coroutineScope = wsScope,
            backoffDelaysMs = listOf(0L)
        )
        val events = mutableListOf<JobWebSocketEvent>()
        val collectJob = wsScope.launch {
            wsClient.events.collect { events.add(it) }
        }
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    Thread.sleep(30)
                    webSocket.send("""{"type":"job.connected"}""")
                    webSocket.send("""{"type":"job.progress","payload":{"jobId":77,"progress":50,"phase":"ENCODE","message":"인코딩"}}""")
                    webSocket.send("""{"type":"job.completed","payload":{"jobId":77,"downloadUrl":"/api/jobs/77/result","checksum":"deadbeef"}}""")
                }
            })
        )

        wsClient.start()

        val completed = withTimeout(5_000) {
            while (events.none { it is JobWebSocketEvent.Completed }) {
                delay(25)
            }
            events.first { it is JobWebSocketEvent.Completed }
        }

        wsClient.stop()
        collectJob.cancel()
        wsScope.cancel()

        val progressEvents = events.filterIsInstance<JobWebSocketEvent.Progress>()
        assertTrue(progressEvents.any { it.progress == 50 })
        assertEquals("/api/jobs/77/result", (completed as JobWebSocketEvent.Completed).downloadUrl)

        server.enqueue(MockResponse().setResponseCode(200).setBody("export-binary"))
        val destination = Files.createTempFile("job-download", ".bin").toFile()

        val downloadResult = jobRepository.downloadResult(jobId = jobId, destination = destination)

        assertTrue(downloadResult.isSuccess)
        assertEquals("export-binary", destination.readText())

        val requestPaths = listOfNotNull(
            server.takeRequest(1, TimeUnit.SECONDS)?.path,
            server.takeRequest(1, TimeUnit.SECONDS)?.path,
            server.takeRequest(1, TimeUnit.SECONDS)?.path,
            server.takeRequest(2, TimeUnit.SECONDS)?.path,
            server.takeRequest(1, TimeUnit.SECONDS)?.path
        )

        assertEquals(
            listOf(
                "/api/auth/login",
                "/api/replays?page=0&size=10",
                "/api/replays/5/exports/mp4",
                "/ws/jobs?token=jwt-token",
                "/api/jobs/77/result"
            ),
            requestPaths
        )
    }
}

package com.codexpong.mobile.data.job

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.data.job.ws.JobWebSocketClient
import com.codexpong.mobile.data.job.ws.JobWebSocketEvent
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * JobWebSocketClient의 이벤트 파싱과 재연결 동작을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobWebSocketClientTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenRepository: AuthTokenRepository
    private lateinit var baseUrlRepository: BaseUrlRepository
    private lateinit var server: MockWebServer
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val okHttpClient = OkHttpClient()

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { Files.createTempFile("job-ws", ".preferences_pb").toOkioPath() }
        )
        tokenRepository = AuthTokenRepository(dataStore)
        server = MockWebServer()
        server.start()
        baseUrlRepository = object : BaseUrlRepository(dataStore, "http://localhost") {
            override val baseUrl: Flow<String> = flowOf(server.url("/").toString())
            override suspend fun currentBaseUrl(): String = server.url("/").toString()
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `업그레이드된 웹소켓에서 중복 진행률 포함 이벤트를 파싱한다`() = runBlocking {
        tokenRepository.saveToken("jwt-token")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val client = JobWebSocketClient(
            okHttpClient = okHttpClient,
            baseUrlRepository = baseUrlRepository,
            authTokenRepository = tokenRepository,
            moshi = moshi,
            coroutineScope = scope,
            backoffDelaysMs = listOf(0L)
        )

        val events = mutableListOf<JobWebSocketEvent>()
        val collectJob = launch {
            client.events.collect { events.add(it) }
        }
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    Thread.sleep(50)
                    webSocket.send("""{"type":"job.connected"}""")
                    webSocket.send(
                        """{"type":"job.progress","payload":{"jobId":10,"progress":25,"phase":"PREPARE","message":"준비"}}"""
                    )
                    webSocket.send(
                        """{"type":"job.progress","payload":{"jobId":10,"progress":25,"phase":"PREPARE","message":"중복"}}"""
                    )
                    webSocket.send(
                        """{"type":"job.completed","payload":{"jobId":10,"downloadUrl":"/api/jobs/10/result","checksum":"abc"}}"""
                    )
                }
            })
        )

        client.start()
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("/ws/jobs?token=jwt-token", request?.path)
        withTimeout(2_000) {
            while (events.none { it is JobWebSocketEvent.Completed }) {
                // OkHttp WebSocket 콜백이 백그라운드에서 들어올 때까지 대기한다.
                delay(20)
            }
        }
        collectJob.cancel()
        val progresses = events.filterIsInstance<JobWebSocketEvent.Progress>()

        assertTrue(events.any { it is JobWebSocketEvent.Connected })
        assertEquals(2, progresses.size)
        val completed = events.filterIsInstance<JobWebSocketEvent.Completed>().first()
        assertEquals("/api/jobs/10/result", completed.downloadUrl)
        assertEquals("abc", completed.checksum)
        scope.cancel()
    }

    @Test
    fun `연결 종료 후 재연결하고 실패 이벤트를 전달한다`() = runBlocking {
        tokenRepository.saveToken("jwt-token")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val client = JobWebSocketClient(
            okHttpClient = okHttpClient,
            baseUrlRepository = baseUrlRepository,
            authTokenRepository = tokenRepository,
            moshi = moshi,
            coroutineScope = scope,
            backoffDelaysMs = listOf(0L, 0L)
        )

        val events = mutableListOf<JobWebSocketEvent>()
        val collectJob = launch {
            client.events.collect { events.add(it) }
        }
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    Thread.sleep(50)
                    webSocket.send("""{"type":"job.connected"}""")
                    webSocket.send("""{"type":"job.progress","payload":{"jobId":11,"progress":20}}""")
                    webSocket.close(1001, "closing")
                }
            })
        )
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                    Thread.sleep(50)
                    webSocket.send("""{"type":"job.connected"}""")
                    webSocket.send(
                        """{"type":"job.failed","payload":{"jobId":11,"errorCode":"ENCODE_TIMEOUT","errorMessage":"타임아웃"}}"""
                    )
                }
            })
        )

        client.start()
        val firstRequest = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull(firstRequest)
        withTimeout(5_000) {
            while (server.requestCount < 2) {
                delay(50)
            }
        }
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(secondRequest)
        val failedEvent = withTimeoutOrNull(5_000) {
            while (events.none { it is JobWebSocketEvent.Failed }) {
                delay(20)
            }
            events.first { it is JobWebSocketEvent.Failed }
        }
        collectJob.cancel()
        val types = events.map { it::class.simpleName }

        assertNotNull("events: $types", failedEvent)
        assertTrue(events.any { it is JobWebSocketEvent.Disconnected })
        scope.cancel()
    }
}

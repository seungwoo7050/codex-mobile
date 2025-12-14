package com.codexpong.mobile.data.job

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
 * JobRepository의 내보내기 및 잡 조회 파싱을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobRepositoryTest {
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
            produceFile = { Files.createTempFile("job-repo", ".preferences_pb").toOkioPath() },
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
    fun `MP4 내보내기 요청 시 jobId를 반환한다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"jobId":101}""")
        )
        val repository = JobRepository(baseUrlRepository, retrofitProvider)

        val result = repository.requestMp4Export(replayId = 5)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(101L, response.jobId)
        val request = server.takeRequest()
        assertEquals("/api/replays/5/exports/mp4", request.path)
    }

    @Test
    fun `잡 목록을 페이징으로 불러온다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[{"jobId":1,"jobType":"REPLAY_EXPORT_MP4","status":"RUNNING","progress":40,"targetReplayId":10,"createdAt":"2024-05-01T00:00:00Z","startedAt":"2024-05-01T00:01:00Z","endedAt":null,"errorCode":null,"errorMessage":null,"resultUri":null,"downloadUrl":null}],"page":0,"size":20,"totalItems":1,"totalPages":1}"""
            )
        )
        val repository = JobRepository(baseUrlRepository, retrofitProvider)

        val result = repository.fetchJobs(page = 0, size = 20, status = null, type = null)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(1, response.items?.size)
        assertEquals(1, response.totalPages)
        val request = server.takeRequest()
        assertEquals("/api/jobs?page=0&size=20", request.path)
    }

    @Test
    fun `잡 상세를 불러온다`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"jobId":2,"jobType":"REPLAY_THUMBNAIL","status":"SUCCEEDED","progress":100,"targetReplayId":20,"createdAt":"2024-06-01T00:00:00Z","startedAt":"2024-06-01T00:02:00Z","endedAt":"2024-06-01T00:03:00Z","errorCode":null,"errorMessage":null,"resultUri":"/files/thumb","downloadUrl":"http://example.com/thumb.png"}"""
            )
        )
        val repository = JobRepository(baseUrlRepository, retrofitProvider)

        val result = repository.fetchJobDetail(jobId = 2)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals("REPLAY_THUMBNAIL", response.jobType)
        assertEquals(100, response.progress)
        assertEquals("/files/thumb", response.resultUri)
        val request = server.takeRequest()
        assertEquals("/api/jobs/2", request.path)
    }

    @Test
    fun `401과 5xx 응답은 실패로 처리된다`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
        val repository = JobRepository(baseUrlRepository, retrofitProvider)

        val unauthorized = repository.fetchJobDetail(jobId = 3)
        assertTrue(unauthorized.isFailure)
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        val serverError = repository.requestThumbnailExport(replayId = 9)
        assertTrue(serverError.isFailure)
    }

    @Test
    fun `완료된 잡 결과를 스트리밍으로 저장한다`() = runTest {
        val binaryBody = "result-bytes"
        server.enqueue(MockResponse().setResponseCode(200).setBody(binaryBody))
        val repository = JobRepository(baseUrlRepository, retrofitProvider)
        val destination = Files.createTempFile("job-result", ".bin").toFile()

        val result = repository.downloadResult(jobId = 7, destination = destination)

        assertTrue(result.isSuccess)
        assertEquals(binaryBody, destination.readText())
        val request = server.takeRequest()
        assertEquals("/api/jobs/7/result", request.path)
    }
}

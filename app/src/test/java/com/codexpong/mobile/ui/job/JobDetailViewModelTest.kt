package com.codexpong.mobile.ui.job

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.job.model.JobResponse
import com.codexpong.mobile.data.job.ws.JobWebSocketEvent
import com.codexpong.mobile.data.job.ws.JobWebSocketSource
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * JobDetailViewModel의 상태 전이를 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobDetailViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: JobDetailViewModel
    private lateinit var webSocketSource: FakeJobWebSocketSource
    private lateinit var repository: FakeJobRepository

    @Before
    fun setup() {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
            produceFile = { Files.createTempFile("job-detail", ".preferences_pb").toOkioPath() }
        )
        val baseUrlRepository = object : BaseUrlRepository(dataStore, "http://localhost") {
            override val baseUrl: Flow<String> = flowOf("http://localhost")
        }
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofitProvider = RetrofitProvider(OkHttpClient(), moshi)
        repository = FakeJobRepository(baseUrlRepository, retrofitProvider)
        webSocketSource = FakeJobWebSocketSource()
        viewModel = JobDetailViewModel(repository, webSocketSource)
    }

    @Test
    fun `연결 후 REST 보강과 진행률 이벤트를 반영한다`() = runTest {
        viewModel.start(5)
        webSocketSource.emit(JobWebSocketEvent.Connected)
        advanceUntilIdle()
        assertEquals(2, repository.detailCallCount)

        webSocketSource.emit(JobWebSocketEvent.Progress(5, 40, "ENCODE", "진행"))
        advanceUntilIdle()
        assertEquals(40, viewModel.uiState.value.job?.progress)
        assertEquals("진행", viewModel.uiState.value.progressMessage)

        webSocketSource.emit(JobWebSocketEvent.Completed(5, "/api/jobs/5/result", "abc"))
        advanceUntilIdle()
        val job = viewModel.uiState.value.job
        assertEquals("COMPLETED", job?.status)
        assertEquals("/api/jobs/5/result", job?.downloadUrl)
        assertEquals("abc", job?.checksum)
    }

    @Test
    fun `재연결 시 REST를 다시 조회해 누락된 상태를 보강한다`() = runTest {
        viewModel.start(12)
        webSocketSource.emit(JobWebSocketEvent.Connected)
        advanceUntilIdle()
        assertEquals(2, repository.detailCallCount)

        webSocketSource.emit(JobWebSocketEvent.Disconnected("closed"))
        webSocketSource.emit(JobWebSocketEvent.Connected)
        advanceUntilIdle()

        assertEquals(3, repository.detailCallCount)
    }

    @Test
    fun `중복 진행률과 실패 이벤트를 멱등하게 처리한다`() = runTest {
        viewModel.start(8)
        webSocketSource.emit(JobWebSocketEvent.Progress(8, 10, null, null))
        webSocketSource.emit(JobWebSocketEvent.Progress(8, 10, null, null))
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.job?.progress)

        webSocketSource.emit(JobWebSocketEvent.Failed(8, "ERR", "실패"))
        advanceUntilIdle()
        val job = viewModel.uiState.value.job
        assertEquals("FAILED", job?.status)
        assertEquals("실패", job?.errorMessage)
    }

    @Test
    fun `완료 이후 진행률 이벤트는 무시한다`() = runTest {
        viewModel.start(15)
        webSocketSource.emit(JobWebSocketEvent.Completed(15, "/api/jobs/15/result", "xyz"))
        advanceUntilIdle()

        val completed = viewModel.uiState.value.job
        assertEquals("COMPLETED", completed?.status)
        assertEquals(100, completed?.progress)

        webSocketSource.emit(JobWebSocketEvent.Progress(15, 20, "ENCODE", "뒤늦은 진행"))
        advanceUntilIdle()

        val afterProgress = viewModel.uiState.value.job
        assertEquals("COMPLETED", afterProgress?.status)
        assertEquals(100, afterProgress?.progress)
        assertEquals("xyz", afterProgress?.checksum)
    }

    private class FakeJobRepository(
        baseUrlRepository: BaseUrlRepository,
        retrofitProvider: RetrofitProvider
    ) : JobRepository(baseUrlRepository, retrofitProvider) {
        var detailCallCount: Int = 0
        override suspend fun fetchJobDetail(jobId: Long): Result<JobResponse> {
            detailCallCount++
            return Result.success(
                JobResponse(
                    jobId = jobId,
                    status = "RUNNING",
                    progress = 5,
                    downloadUrl = "/api/jobs/$jobId/result"
                )
            )
        }

        override suspend fun downloadResult(jobId: Long, destination: File): Result<File> {
            destination.writeText("content")
            return Result.success(destination)
        }
    }

    private class FakeJobWebSocketSource : JobWebSocketSource {
        private val _events = MutableSharedFlow<JobWebSocketEvent>(extraBufferCapacity = 10)
        override val events: SharedFlow<JobWebSocketEvent> = _events
        override fun start() {}
        override fun stop() {}
        suspend fun emit(event: JobWebSocketEvent) {
            _events.emit(event)
        }
    }
}

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.codexpong.mobile.ui.replay

import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.job.model.JobCreateResponse
import com.codexpong.mobile.data.replay.ReplayRepository
import com.codexpong.mobile.data.replay.model.ReplayDetailResponse
import com.codexpong.mobile.data.replay.model.ReplaySummaryResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * 리플레이 상세 ViewModel이 버전별 요구사항을 충족하는지 검증한다.
 */
class ReplayDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `상세 로드 성공 시 상태를 갱신한다`() = runTest {
        val detail = ReplayDetailResponse(
            summary = ReplaySummaryResponse(
                replayId = 3,
                matchId = 30,
                ownerUserId = 1,
                opponentUserId = 2,
                opponentNickname = "상대",
                matchType = "RANK",
                myScore = 11,
                opponentScore = 9,
                durationMs = 1000L,
                createdAt = "2024-01-01T00:00:00Z",
                eventFormat = "STANDARD"
            ),
            checksum = "abc123",
            downloadPath = "/downloads/3"
        )
        val viewModel = ReplayDetailViewModel(
            replayRepository = ReplayRepositoryFake(Result.success(detail)),
            jobRepository = JobRepositoryFake(
                mp4Result = Result.success(JobCreateResponse(jobId = 11)),
                thumbnailResult = Result.success(JobCreateResponse(jobId = 12))
            )
        )

        viewModel.load(3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3L, state.replayId)
        assertEquals(detail, state.detail)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `상세 로드 실패 시 오류 메시지를 노출한다`() = runTest {
        val viewModel = ReplayDetailViewModel(
            replayRepository = ReplayRepositoryFake(Result.failure(IllegalStateException("detail 실패"))),
            jobRepository = JobRepositoryFake(
                mp4Result = Result.success(JobCreateResponse(jobId = 21)),
                thumbnailResult = Result.success(JobCreateResponse(jobId = 22))
            )
        )

        viewModel.load(4)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.contains("detail 실패") == true)
    }

    @Test
    fun `MP4 내보내기 성공 시 잡 정보를 반영한다`() = runTest {
        val detail = ReplayDetailResponse(summary = null, checksum = "", downloadPath = null)
        val viewModel = ReplayDetailViewModel(
            replayRepository = ReplayRepositoryFake(Result.success(detail)),
            jobRepository = JobRepositoryFake(
                mp4Result = Result.success(JobCreateResponse(jobId = 33)),
                thumbnailResult = Result.failure(IllegalStateException("unused"))
            )
        )

        viewModel.load(9)
        advanceUntilIdle()
        viewModel.requestMp4Export()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(33L, state.lastCreatedJobId)
        assertTrue(state.exportMessage?.contains("33") == true)
        assertFalse(state.isExporting)
    }

    @Test
    fun `썸네일 내보내기 실패 시 오류 메시지를 노출한다`() = runTest {
        val detail = ReplayDetailResponse(summary = null, checksum = "", downloadPath = null)
        val viewModel = ReplayDetailViewModel(
            replayRepository = ReplayRepositoryFake(Result.success(detail)),
            jobRepository = JobRepositoryFake(
                mp4Result = Result.failure(IllegalStateException("unused")),
                thumbnailResult = Result.failure(IllegalStateException("썸네일 실패"))
            )
        )

        viewModel.load(12)
        advanceUntilIdle()
        viewModel.requestThumbnailExport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExporting)
        assertTrue(state.exportMessage?.contains("썸네일 실패") == true)
    }
}

private fun baseUrlRepositoryStub(): BaseUrlRepository = object : BaseUrlRepository(
    dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = listOf(),
        scope = CoroutineScope(UnconfinedTestDispatcher()),
        produceFile = { Files.createTempFile("replay-detail", ".preferences_pb").toOkioPath() }
    ),
    defaultBaseUrl = "http://placeholder"
) {
    override val baseUrl: Flow<String> = flowOf("http://placeholder")
}

private class ReplayRepositoryFake(
    private val detailResult: Result<ReplayDetailResponse>
) : ReplayRepository(
    baseUrlRepositoryStub(),
    RetrofitProvider(OkHttpClient(), Moshi.Builder().build())
) {
    override suspend fun fetchReplayDetail(replayId: Long): Result<ReplayDetailResponse> {
        return detailResult
    }

    override suspend fun fetchReplays(page: Int, size: Int) = Result.failure<Nothing>(IllegalStateException("unused"))
}

private class JobRepositoryFake(
    private val mp4Result: Result<JobCreateResponse>,
    private val thumbnailResult: Result<JobCreateResponse>
) : JobRepository(
    baseUrlRepositoryStub(),
    RetrofitProvider(OkHttpClient(), Moshi.Builder().build())
) {
    override suspend fun requestMp4Export(replayId: Long): Result<JobCreateResponse> {
        return mp4Result
    }

    override suspend fun requestThumbnailExport(replayId: Long): Result<JobCreateResponse> {
        return thumbnailResult
    }
}

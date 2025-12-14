@file:OptIn(ExperimentalCoroutinesApi::class)

package com.codexpong.mobile.ui.replay

import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.replay.ReplayRepository
import com.codexpong.mobile.data.replay.model.ReplayDetailResponse
import com.codexpong.mobile.data.replay.model.ReplayPageResponse
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * ReplayListViewModel의 페이징, 빈 상태, 오류 상태를 검증한다.
 */
class ReplayListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `다음 페이지를 불러오면 페이지 정보와 목록이 갱신된다`() = runTest {
        val repository = ReplayRepositoryStub(
            replayPageProvider = { page, _ ->
                Result.success(
                    ReplayPageResponse(
                        items = listOf(
                            ReplaySummaryResponse(
                                replayId = page.toLong(),
                                matchId = 100L + page,
                                ownerUserId = 1L,
                                opponentUserId = 2L,
                                opponentNickname = "상대$page",
                                matchType = "RANK",
                                myScore = 11,
                                opponentScore = 9,
                                durationMs = 1000L,
                                createdAt = "2024-01-01T00:00:00Z",
                                eventFormat = "STANDARD"
                            )
                        ),
                        page = page,
                        size = 20,
                        totalElements = 3,
                        totalPages = 3
                    )
                )
            },
            replayDetailProvider = { Result.failure(IllegalStateException("unused")) }
        )
        val viewModel = ReplayListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.page)
        assertEquals(3, state.totalPages)
        assertEquals(1L, state.items.first().replayId)
    }

    @Test
    fun `빈 목록이면 isEmpty가 true가 된다`() = runTest {
        val repository = ReplayRepositoryStub(
            replayPageProvider = { _, _ ->
                Result.success(
                    ReplayPageResponse(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0,
                        totalPages = 0
                    )
                )
            },
            replayDetailProvider = { Result.failure(IllegalStateException("unused")) }
        )
        val viewModel = ReplayListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `에러가 발생하면 오류 메시지를 노출하고 로딩을 종료한다`() = runTest {
        val repository = ReplayRepositoryStub(
            replayPageProvider = { _, _ -> Result.failure(IllegalStateException("네트워크 오류")) },
            replayDetailProvider = { Result.failure(IllegalStateException("unused")) }
        )
        val viewModel = ReplayListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.contains("네트워크 오류") == true)
    }
}

private open class ReplayRepositoryStub(
    private val replayPageProvider: (Int, Int) -> Result<ReplayPageResponse>,
    private val replayDetailProvider: (Long) -> Result<ReplayDetailResponse>
) : ReplayRepository(
    baseUrlRepository = object : BaseUrlRepository(
        dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { Files.createTempFile("replay-stub", ".preferences_pb").toOkioPath() }
        ),
        defaultBaseUrl = "http://placeholder"
    ) {
        override val baseUrl: Flow<String> = flowOf("http://placeholder")
    },
    retrofitProvider = RetrofitProvider(OkHttpClient(), Moshi.Builder().build())
) {
    override suspend fun fetchReplays(page: Int, size: Int): Result<ReplayPageResponse> {
        return replayPageProvider(page, size)
    }

    override suspend fun fetchReplayDetail(replayId: Long): Result<ReplayDetailResponse> {
        return replayDetailProvider(replayId)
    }
}

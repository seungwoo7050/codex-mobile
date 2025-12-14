@file:OptIn(ExperimentalCoroutinesApi::class)

package com.codexpong.mobile.ui.health

import com.codexpong.mobile.MainDispatcherRule
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.data.health.HealthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * HealthViewModel의 기본 흐름을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `기본 URL을 반영해 헬스 결과를 업데이트한다`() = runTest {
        val baseUrlFlow = MutableStateFlow("http://localhost:8080")
        val fakeBaseUrlRepo = FakeBaseUrlRepository(baseUrlFlow)
        val fakeHealthRepository = FakeHealthRepository(result = Result.success(mapOf("status" to "ok")))

        val viewModel = HealthViewModel(fakeBaseUrlRepo, fakeHealthRepository)
        viewModel.refreshHealth()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("http://localhost:8080", state.baseUrl)
        assertEquals("status: ok", state.healthText)
    }

    @Test
    fun `헬스 호출 실패 시 오류 메시지를 노출한다`() = runTest {
        val baseUrlFlow = MutableStateFlow("http://localhost:8080")
        val fakeBaseUrlRepo = FakeBaseUrlRepository(baseUrlFlow)
        val fakeHealthRepository = FakeHealthRepository(result = Result.failure(IllegalStateException("boom")))

        val viewModel = HealthViewModel(fakeBaseUrlRepo, fakeHealthRepository)
        viewModel.refreshHealth()
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals("boom", state.errorMessage)
    }

    private class FakeBaseUrlRepository(private val flow: Flow<String>) : BaseUrlRepositoryStub(flow)

    private class FakeHealthRepository(private val result: Result<Map<String, String>>) : HealthRepositoryStub(result)
}

private open class BaseUrlRepositoryStub(private val flow: Flow<String>) : BaseUrlRepository(
    dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = listOf(),
        scope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher()),
        produceFile = { java.nio.file.Files.createTempFile("stub", ".preferences_pb").toFile().toPath().toOkioPath() }
    ),
    defaultBaseUrl = "http://placeholder"
) {
    override val baseUrl: Flow<String> = flow
    override suspend fun updateBaseUrl(value: String) {
        // 테스트 더블에서는 별도 동작 없음
    }
}

private open class HealthRepositoryStub(private val response: Result<Map<String, String>>) : HealthRepository(
    okHttpClient = okhttp3.OkHttpClient()
) {
    override suspend fun fetchHealth(baseUrl: String): Result<Map<String, String>> {
        return response
    }
}

package com.codexpong.mobile.data.health

import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * HealthRepository의 기본 동작을 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: HealthRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.enqueue(MockResponse().setBody("{\"status\":\"ok\"}"))
        server.start()
        repository = HealthRepository(OkHttpClient(), Moshi.Builder().build())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `health 응답을 성공적으로 파싱한다`() = runTest {
        val result = repository.fetchHealth(server.url("/").toString())
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrThrow()["status"])
    }
}

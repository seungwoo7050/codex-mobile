package com.codexpong.mobile.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.codexpong.mobile.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

/**
 * BaseUrlRepository의 저장 및 조회를 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseUrlRepositoryTest {
    private lateinit var dataStore: DataStore<Preferences>

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        val tempFile = Files.createTempFile("datastore-test", ".preferences_pb").toFile()
        dataStore = PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = { tempFile.toPath().toOkioPath() }
        )
    }

    @Test
    fun `기본 URL을 저장하고 다시 불러온다`() = runTest {
        val repository = BaseUrlRepository(dataStore, "http://initial")
        val firstValue = repository.baseUrl.first()
        assertEquals("http://initial", firstValue)

        repository.updateBaseUrl("http://updated")
        val updated = repository.baseUrl.first()
        assertEquals("http://updated", updated)
    }
}

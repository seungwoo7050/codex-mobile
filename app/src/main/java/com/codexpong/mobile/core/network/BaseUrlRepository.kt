package com.codexpong.mobile.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 런타임에 REST 기본 URL을 보관하고 갱신하는 저장소.
 */
open class BaseUrlRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultBaseUrl: String
) {
    private val keyBaseUrl = stringPreferencesKey("base_url")

    open val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[keyBaseUrl] ?: defaultBaseUrl
    }

    /**
     * 최신 기본 URL을 즉시 반환한다.
     */
    open suspend fun currentBaseUrl(): String = baseUrl.first()

    /**
     * 사용자가 입력한 기본 URL을 저장한다.
     */
    open suspend fun updateBaseUrl(value: String) {
        dataStore.edit { preferences ->
            preferences[keyBaseUrl] = value
        }
    }
}

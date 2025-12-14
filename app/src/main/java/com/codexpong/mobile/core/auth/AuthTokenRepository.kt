package com.codexpong.mobile.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * 인증 토큰을 DataStore에 보관하고 제공하는 저장소.
 */
class AuthTokenRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val keyAuthToken = stringPreferencesKey("auth_token")

    val token: Flow<String?> = dataStore.data.map { preferences ->
        preferences[keyAuthToken]
    }

    /**
     * 새 토큰을 저장한다.
     */
    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[keyAuthToken] = token
        }
    }

    /**
     * 토큰을 삭제한다.
     */
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(keyAuthToken)
        }
    }

    /**
     * 현재 저장된 토큰을 즉시 반환한다.
     */
    suspend fun currentToken(): String? = token.firstOrNull()
}

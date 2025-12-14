package com.codexpong.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.config.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 세션 상태를 추적해 인증 여부를 노출하는 ViewModel.
 */
class SessionViewModel(
    private val tokenRepository: AuthTokenRepository
) : ViewModel() {

    val uiState = tokenRepository.token
        .map { token -> AuthSessionState(isAuthenticated = !token.isNullOrBlank()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthSessionState(isAuthenticated = false)
        )

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                SessionViewModel(appContainer.authTokenRepository)
            }
        }
    }
}

/**
 * 인증 흐름을 제어하기 위한 상태 모델.
 */
data class AuthSessionState(
    val isAuthenticated: Boolean
)

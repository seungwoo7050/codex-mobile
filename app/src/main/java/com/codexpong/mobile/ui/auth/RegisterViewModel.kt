package com.codexpong.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 회원가입 화면 상태를 관리하는 ViewModel.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun updateNickname(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value)
    }

    fun updateAvatarUrl(value: String) {
        _uiState.value = _uiState.value.copy(avatarUrl = value)
    }

    /**
     * 회원가입 요청을 실행한다.
     */
    fun register() {
        val snapshot = _uiState.value
        _uiState.value = snapshot.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = authRepository.register(
                username = snapshot.username,
                password = snapshot.password,
                nickname = snapshot.nickname,
                avatarUrl = snapshot.avatarUrl.ifBlank { null }
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "회원가입에 실패했습니다"
                    )
                }
            )
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                RegisterViewModel(appContainer.authRepository)
            }
        }
    }
}

/**
 * 회원가입 UI 상태 데이터 클래스.
 */
data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

package com.codexpong.mobile.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.auth.AuthRepository
import com.codexpong.mobile.data.user.UserRepository
import com.codexpong.mobile.data.user.model.ProfileUpdateRequest
import com.codexpong.mobile.data.user.model.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 내 프로필 화면 상태를 담당하는 ViewModel.
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        refreshProfile()
    }

    /**
     * 프로필 정보를 다시 불러온다.
     */
    fun refreshProfile() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = userRepository.fetchProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        user = profile,
                        nicknameInput = profile.nickname.orEmpty(),
                        avatarInput = profile.avatarUrl.orEmpty()
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "프로필을 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    fun updateNicknameInput(value: String) {
        _uiState.value = _uiState.value.copy(nicknameInput = value)
    }

    fun updateAvatarInput(value: String) {
        _uiState.value = _uiState.value.copy(avatarInput = value)
    }

    /**
     * 입력값으로 프로필을 업데이트한다.
     */
    fun saveProfile() {
        val snapshot = _uiState.value
        _uiState.value = snapshot.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val result = userRepository.updateProfile(
                ProfileUpdateRequest(
                    nickname = snapshot.nicknameInput,
                    avatarUrl = snapshot.avatarInput.ifBlank { null }
                )
            )
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        user = profile,
                        errorMessage = null,
                        nicknameInput = profile.nickname.orEmpty(),
                        avatarInput = profile.avatarUrl.orEmpty()
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = throwable.localizedMessage ?: "프로필 저장에 실패했습니다"
                    )
                }
            )
        }
    }

    /**
     * 로그아웃을 실행해 토큰을 정리한다.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                ProfileViewModel(
                    userRepository = appContainer.userRepository,
                    authRepository = appContainer.authRepository
                )
            }
        }
    }
}

/**
 * 프로필 화면 상태 데이터 클래스.
 */
data class ProfileUiState(
    val user: UserResponse? = null,
    val nicknameInput: String = "",
    val avatarInput: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

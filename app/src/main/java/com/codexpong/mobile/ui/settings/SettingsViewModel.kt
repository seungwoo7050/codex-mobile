package com.codexpong.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.core.network.BaseUrlRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 환경 설정 화면 상태를 관리하는 ViewModel.
 */
class SettingsViewModel(
    private val baseUrlRepository: BaseUrlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(baseUrl = ""))
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val current = baseUrlRepository.baseUrl.first()
            _uiState.value = _uiState.value.copy(baseUrl = current)
        }
    }

    /**
     * 새 URL을 저장하고 상태를 갱신한다.
     */
    fun saveBaseUrl(value: String) {
        viewModelScope.launch {
            baseUrlRepository.updateBaseUrl(value)
            _uiState.value = _uiState.value.copy(baseUrl = value)
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                SettingsViewModel(appContainer.baseUrlRepository)
            }
        }
    }
}

data class SettingsUiState(val baseUrl: String)

package com.codexpong.mobile.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.data.health.HealthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 헬스 체크 UI 상태를 관리하는 ViewModel.
 */
class HealthViewModel(
    private val baseUrlRepository: BaseUrlRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HealthUiState(baseUrl = "", healthText = "", isLoading = false, errorMessage = null)
    )
    val uiState: StateFlow<HealthUiState> = _uiState

    private var baseUrlJob: Job? = null

    init {
        observeBaseUrl()
    }

    /**
     * DataStore에 저장된 기본 URL을 수집해 UI 상태에 반영한다.
     */
    private fun observeBaseUrl() {
        baseUrlJob?.cancel()
        baseUrlJob = viewModelScope.launch {
            baseUrlRepository.baseUrl.collectLatest { url ->
                _uiState.value = _uiState.value.copy(baseUrl = url)
            }
        }
    }

    /**
     * 서버 헬스 체크를 호출한다.
     */
    fun refreshHealth() {
        val targetUrl = _uiState.value.baseUrl
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = healthRepository.fetchHealth(targetUrl)
            result.fold(
                onSuccess = { payload ->
                    val text = payload.entries.joinToString(separator = "\n") { (key, value) -> "$key: $value" }
                    _uiState.value = _uiState.value.copy(healthText = text, isLoading = false, errorMessage = null)
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "알 수 없는 오류"
                    )
                }
            )
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                HealthViewModel(
                    baseUrlRepository = appContainer.baseUrlRepository,
                    healthRepository = appContainer.healthRepository
                )
            }
        }
    }
}

data class HealthUiState(
    val baseUrl: String,
    val healthText: String,
    val isLoading: Boolean,
    val errorMessage: String?
)

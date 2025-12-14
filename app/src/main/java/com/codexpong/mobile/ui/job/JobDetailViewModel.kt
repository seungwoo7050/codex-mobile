package com.codexpong.mobile.ui.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.job.model.JobResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 잡 상세 조회를 담당하는 ViewModel.
 */
class JobDetailViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState

    /**
     * 잡 상세를 불러온다.
     */
    fun load(jobId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, jobId = jobId)
        viewModelScope.launch {
            val result = jobRepository.fetchJobDetail(jobId)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        job = response,
                        errorMessage = null
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "잡 상세를 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                JobDetailViewModel(appContainer.jobRepository)
            }
        }
    }
}

/**
 * 잡 상세 화면 상태를 표현한다.
 */
data class JobDetailUiState(
    val jobId: Long? = null,
    val job: JobResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

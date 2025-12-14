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
 * 잡 목록 조회를 담당하는 ViewModel.
 */
class JobListViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState

    /**
     * 현재 페이지 기준으로 잡 목록을 불러온다.
     */
    fun load(page: Int = _uiState.value.page) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, page = page)
        viewModelScope.launch {
            val result = jobRepository.fetchJobs(
                page = page,
                size = _uiState.value.size,
                status = _uiState.value.statusFilter.takeIf { it.isNotBlank() },
                type = _uiState.value.typeFilter.takeIf { it.isNotBlank() }
            )
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = response.items ?: emptyList(),
                        totalPages = response.totalPages ?: 0,
                        totalItems = response.totalItems ?: 0L,
                        errorMessage = null
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "잡 목록을 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    /**
     * 다음 페이지를 요청한다.
     */
    fun loadNextPage() {
        val nextPage = _uiState.value.page + 1
        if (_uiState.value.totalPages != 0 && nextPage >= _uiState.value.totalPages) return
        load(nextPage)
    }

    /**
     * 이전 페이지를 요청한다.
     */
    fun loadPreviousPage() {
        val prevPage = (_uiState.value.page - 1).coerceAtLeast(0)
        load(prevPage)
    }

    /**
     * 상태 필터 입력을 갱신한다.
     */
    fun updateStatusFilter(value: String) {
        _uiState.value = _uiState.value.copy(statusFilter = value)
    }

    /**
     * 타입 필터 입력을 갱신한다.
     */
    fun updateTypeFilter(value: String) {
        _uiState.value = _uiState.value.copy(typeFilter = value)
    }

    /**
     * 필터를 적용하여 첫 페이지를 다시 불러온다.
     */
    fun applyFilters() {
        load(page = 0)
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                JobListViewModel(appContainer.jobRepository)
            }
        }
    }
}

/**
 * 잡 목록 화면 상태를 표현하는 모델.
 */
data class JobListUiState(
    val items: List<JobResponse> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalPages: Int = 0,
    val totalItems: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusFilter: String = "",
    val typeFilter: String = ""
)

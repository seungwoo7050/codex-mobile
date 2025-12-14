package com.codexpong.mobile.ui.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.replay.ReplayRepository
import com.codexpong.mobile.data.replay.model.ReplaySummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 리플레이 목록 상태와 페이징을 담당하는 ViewModel.
 */
class ReplayListViewModel(
    private val replayRepository: ReplayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplayListUiState())
    val uiState: StateFlow<ReplayListUiState> = _uiState

    init {
        loadPage(0)
    }

    /**
     * 지정한 페이지를 불러온다.
     */
    fun loadPage(page: Int) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = replayRepository.fetchReplays(page, currentState.size)
            result.fold(
                onSuccess = { response ->
                    val items = response.items ?: emptyList()
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        items = items,
                        page = response.page ?: page,
                        totalPages = response.totalPages ?: currentState.totalPages,
                        totalElements = response.totalElements ?: currentState.totalElements,
                        isEmpty = items.isEmpty(),
                        errorMessage = null
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "리플레이 목록을 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    /**
     * 다음 페이지를 요청한다.
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading) return
        val nextPage = state.page + 1
        if (state.totalPages == 0 || nextPage < state.totalPages) {
            loadPage(nextPage)
        }
    }

    /**
     * 이전 페이지를 요청한다.
     */
    fun loadPreviousPage() {
        val state = _uiState.value
        if (state.isLoading) return
        val previousPage = state.page - 1
        if (previousPage >= 0) {
            loadPage(previousPage)
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                ReplayListViewModel(appContainer.replayRepository)
            }
        }
    }
}

/**
 * 리플레이 목록 화면 UI 상태.
 */
data class ReplayListUiState(
    val items: List<ReplaySummaryResponse> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
)

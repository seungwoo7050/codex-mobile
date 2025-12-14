package com.codexpong.mobile.ui.replay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.replay.ReplayRepository
import com.codexpong.mobile.data.replay.model.ReplayDetailResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 리플레이 상세 조회를 담당하는 ViewModel.
 */
class ReplayDetailViewModel(
    private val replayRepository: ReplayRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplayDetailUiState())
    val uiState: StateFlow<ReplayDetailUiState> = _uiState

    /**
     * 리플레이 상세를 불러온다.
     */
    fun load(replayId: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, replayId = replayId)
        viewModelScope.launch {
            val result = replayRepository.fetchReplayDetail(replayId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = detail,
                        errorMessage = null
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "리플레이 상세를 불러오지 못했습니다"
                    )
                }
            )
        }
    }

    /**
     * MP4 내보내기를 요청한다.
     */
    fun requestMp4Export() {
        val replayId = _uiState.value.replayId ?: return
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        viewModelScope.launch {
            val result = jobRepository.requestMp4Export(replayId)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = response.jobId?.let { "잡 #$it 생성됨" } ?: "잡 생성 ID를 알 수 없습니다",
                        lastCreatedJobId = response.jobId
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = throwable.localizedMessage ?: "내보내기 요청에 실패했습니다"
                    )
                }
            )
        }
    }

    /**
     * 썸네일 내보내기를 요청한다.
     */
    fun requestThumbnailExport() {
        val replayId = _uiState.value.replayId ?: return
        _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
        viewModelScope.launch {
            val result = jobRepository.requestThumbnailExport(replayId)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = response.jobId?.let { "잡 #$it 생성됨" } ?: "잡 생성 ID를 알 수 없습니다",
                        lastCreatedJobId = response.jobId
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportMessage = throwable.localizedMessage ?: "내보내기 요청에 실패했습니다"
                    )
                }
            )
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                ReplayDetailViewModel(appContainer.replayRepository, appContainer.jobRepository)
            }
        }
    }
}

/**
 * 리플레이 상세 화면 상태.
 */
data class ReplayDetailUiState(
    val replayId: Long? = null,
    val detail: ReplayDetailResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val lastCreatedJobId: Long? = null
)

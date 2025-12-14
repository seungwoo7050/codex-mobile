package com.codexpong.mobile.ui.job

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.data.job.JobRepository
import com.codexpong.mobile.data.job.model.JobResponse
import com.codexpong.mobile.data.job.ws.JobWebSocketEvent
import com.codexpong.mobile.data.job.ws.JobWebSocketSource
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 잡 상세 조회와 실시간 상태 변화를 처리하는 ViewModel.
 */
class JobDetailViewModel(
    private val jobRepository: JobRepository,
    private val jobWebSocketSource: JobWebSocketSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState

    private var started = false

    init {
        observeWebSocket()
    }

    /**
     * 화면 진입 시 초기 로딩 및 WebSocket 연결을 시작한다.
     */
    fun start(jobId: Long) {
        if (started && _uiState.value.jobId == jobId) {
            return
        }
        started = true
        _uiState.value = JobDetailUiState(jobId = jobId)
        load(jobId)
        jobWebSocketSource.start()
    }

    /**
     * 잡 상세를 REST로 불러온다.
     */
    fun load(jobId: Long) {
        _uiState.update { state ->
            state.copy(isLoading = true, errorMessage = null, jobId = jobId)
        }
        viewModelScope.launch {
            val result = jobRepository.fetchJobDetail(jobId)
            result.fold(
                onSuccess = { response ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            job = response,
                            errorMessage = null,
                            progressMessage = state.progressMessage
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage
                                ?: "잡 상세를 불러오지 못했습니다"
                        )
                    }
                }
            )
        }
    }

    /**
     * 완료된 잡 결과를 파일로 내려받는다.
     */
    fun downloadResult(context: Context) {
        val jobId = _uiState.value.jobId ?: return
        val targetFile = File(context.cacheDir, "job-${jobId}-result.bin")
        _uiState.update { it.copy(isDownloading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = jobRepository.downloadResult(jobId, targetFile)
            result.fold(
                onSuccess = { file ->
                    _uiState.update { state ->
                        state.copy(
                            isDownloading = false,
                            downloadedFilePath = file.absolutePath,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isDownloading = false,
                            errorMessage = throwable.localizedMessage ?: "다운로드에 실패했습니다"
                        )
                    }
                }
            )
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            jobWebSocketSource.events.collect { event ->
                when (event) {
                    is JobWebSocketEvent.Connected -> {
                        val needRefresh = !_uiState.value.isConnected
                        _uiState.update { state ->
                            state.copy(
                                isConnected = true,
                                connectionMessage = "실시간 연결됨",
                                connectionError = null
                            )
                        }
                        if (needRefresh) {
                            _uiState.value.jobId?.let { load(it) }
                        }
                    }

                    is JobWebSocketEvent.Disconnected -> {
                        _uiState.update { state ->
                            state.copy(
                                isConnected = false,
                                connectionError = event.reason
                            )
                        }
                    }

                    is JobWebSocketEvent.Progress -> applyProgress(event)

                    is JobWebSocketEvent.Completed -> applyCompleted(event)

                    is JobWebSocketEvent.Failed -> applyFailed(event)
                }
            }
        }
    }

    private fun applyProgress(event: JobWebSocketEvent.Progress) {
        val targetJobId = _uiState.value.jobId
        if (targetJobId != null && targetJobId != event.jobId) return
        _uiState.update { state ->
            val current = state.job ?: JobResponse(jobId = event.jobId)
            if (current.status == "COMPLETED" || current.status == "FAILED") {
                return@update state
            }
            val updated = current.copy(
                jobId = event.jobId,
                status = current.status ?: "RUNNING",
                progress = event.progress,
                errorMessage = null
            )
            state.copy(
                job = updated,
                progressMessage = event.message,
                connectionError = null
            )
        }
    }

    private fun applyCompleted(event: JobWebSocketEvent.Completed) {
        val targetJobId = _uiState.value.jobId
        if (targetJobId != null && targetJobId != event.jobId) return
        _uiState.update { state ->
            val current = state.job ?: JobResponse(jobId = event.jobId)
            val updated = current.copy(
                jobId = event.jobId,
                status = "COMPLETED",
                progress = 100,
                downloadUrl = event.downloadUrl ?: current.downloadUrl,
                checksum = event.checksum ?: current.checksum
            )
            state.copy(
                job = updated,
                progressMessage = null,
                connectionError = null
            )
        }
    }

    private fun applyFailed(event: JobWebSocketEvent.Failed) {
        val targetJobId = _uiState.value.jobId
        if (targetJobId != null && targetJobId != event.jobId) return
        _uiState.update { state ->
            val current = state.job ?: JobResponse(jobId = event.jobId)
            val updated = current.copy(
                jobId = event.jobId,
                status = "FAILED",
                errorCode = event.errorCode,
                errorMessage = event.errorMessage
            )
            state.copy(
                job = updated,
                progressMessage = null,
                connectionError = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobWebSocketSource.stop()
    }

    companion object {
        fun provideFactory(appContainer: AppContainer) = viewModelFactory {
            initializer {
                JobDetailViewModel(
                    jobRepository = appContainer.jobRepository,
                    jobWebSocketSource = appContainer.createJobWebSocketClient()
                )
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
    val errorMessage: String? = null,
    val connectionMessage: String? = null,
    val connectionError: String? = null,
    val isConnected: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedFilePath: String? = null,
    val progressMessage: String? = null
)

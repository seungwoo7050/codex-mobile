package com.codexpong.mobile.data.job.ws

import com.codexpong.mobile.core.auth.AuthTokenRepository
import com.codexpong.mobile.core.network.BaseUrlRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.math.min
import kotlin.jvm.Volatile

/**
 * 잡 진행률 WebSocket 연결을 관리하고 이벤트를 파싱한다.
 */
class JobWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrlRepository: BaseUrlRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val moshi: Moshi,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val backoffDelaysMs: List<Long> = listOf(1000L, 2000L, 4000L, 8000L)
) : JobWebSocketSource {

    private val _events = MutableSharedFlow<JobWebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<JobWebSocketEvent> = _events.asSharedFlow()

    private val envelopeAdapter = moshi.adapter(JobWebSocketEnvelope::class.java)

    @Volatile
    private var stopped = false
    private var webSocket: WebSocket? = null
    private var reconnectAttempt: Int = 0
    private var connectJob: Job? = null

    override fun start() {
        stopped = false
        reconnectAttempt = 0
        scheduleConnect(0)
    }

    override fun stop() {
        stopped = true
        reconnectAttempt = 0
        connectJob?.cancel()
        webSocket?.cancel()
    }

    private fun scheduleConnect(delayMs: Long) {
        connectJob?.cancel()
        connectJob = coroutineScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            val url = buildWebSocketUrl() ?: run {
                _events.tryEmit(JobWebSocketEvent.Disconnected("토큰 또는 기본 주소가 없습니다"))
                return@launch
            }
            val request = Request.Builder()
                .url(url)
                .build()
            webSocket = okHttpClient.newWebSocket(request, listener)
        }
    }

    private suspend fun buildWebSocketUrl(): String? {
        val baseUrl = baseUrlRepository.currentBaseUrl().trimEnd('/')
        val token = authTokenRepository.currentToken() ?: return null
        val wsBase = when {
            baseUrl.startsWith("http://") -> "ws://" + baseUrl.removePrefix("http://")
            baseUrl.startsWith("https://") -> "wss://" + baseUrl.removePrefix("https://")
            baseUrl.startsWith("ws://") || baseUrl.startsWith("wss://") -> baseUrl
            else -> return null
        }
        return "$wsBase/ws/jobs?token=$token"
    }

    private fun handleDisconnect(reason: String?) {
        if (stopped) return
        _events.tryEmit(JobWebSocketEvent.Disconnected(reason))
        val nextDelay = backoffDelaysMs.getOrElse(reconnectAttempt) { backoffDelaysMs.last() }
        reconnectAttempt = min(reconnectAttempt + 1, backoffDelaysMs.size - 1)
        scheduleConnect(nextDelay)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            reconnectAttempt = 0
            _events.tryEmit(JobWebSocketEvent.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            parseMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            parseMessage(bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect(reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect(reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            handleDisconnect(t.localizedMessage)
        }
    }

    private fun parseMessage(text: String) {
        val envelope = runCatching { envelopeAdapter.fromJson(text) }.getOrNull() ?: return
        val event = when (envelope.type) {
            "job.connected" -> JobWebSocketEvent.Connected
            "job.progress" -> envelope.payload?.jobId?.let { jobId ->
                JobWebSocketEvent.Progress(
                    jobId = jobId,
                    progress = envelope.payload.progress ?: 0,
                    phase = envelope.payload.phase,
                    message = envelope.payload.message
                )
            }
            "job.completed" -> envelope.payload?.jobId?.let { jobId ->
                JobWebSocketEvent.Completed(
                    jobId = jobId,
                    downloadUrl = envelope.payload.downloadUrl,
                    checksum = envelope.payload.checksum
                )
            }
            "job.failed" -> envelope.payload?.jobId?.let { jobId ->
                JobWebSocketEvent.Failed(
                    jobId = jobId,
                    errorCode = envelope.payload.errorCode,
                    errorMessage = envelope.payload.errorMessage
                )
            }
            else -> null
        }
        event?.let { _events.tryEmit(it) }
    }
}

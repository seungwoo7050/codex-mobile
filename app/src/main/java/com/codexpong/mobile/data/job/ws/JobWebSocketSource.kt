package com.codexpong.mobile.data.job.ws

import kotlinx.coroutines.flow.SharedFlow

/**
 * 잡 진행률 WebSocket 이벤트를 제공하는 인터페이스.
 */
interface JobWebSocketSource {
    val events: SharedFlow<JobWebSocketEvent>

    /**
     * 연결을 시작한다.
     */
    fun start()

    /**
     * 모든 재시도를 중단하고 연결을 닫는다.
     */
    fun stop()
}

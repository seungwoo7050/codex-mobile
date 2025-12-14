package com.codexpong.mobile.data.job.ws

/**
 * 잡 WebSocket에서 발생하는 주요 이벤트 유형.
 */
sealed class JobWebSocketEvent {
    /**
     * 서버가 연결을 수락했음을 알리는 이벤트.
     */
    data object Connected : JobWebSocketEvent()

    /**
     * 서버가 연결을 끊었거나 오류가 발생한 경우.
     */
    data class Disconnected(val reason: String?) : JobWebSocketEvent()

    /**
     * 진행률 업데이트.
     */
    data class Progress(
        val jobId: Long,
        val progress: Int,
        val phase: String?,
        val message: String?
    ) : JobWebSocketEvent()

    /**
     * 완료 이벤트.
     */
    data class Completed(
        val jobId: Long,
        val downloadUrl: String?,
        val checksum: String?
    ) : JobWebSocketEvent()

    /**
     * 실패 이벤트.
     */
    data class Failed(
        val jobId: Long,
        val errorCode: String?,
        val errorMessage: String?
    ) : JobWebSocketEvent()
}

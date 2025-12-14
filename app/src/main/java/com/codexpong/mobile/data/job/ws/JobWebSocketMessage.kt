package com.codexpong.mobile.data.job.ws

import com.squareup.moshi.JsonClass

/**
 * WebSocket으로부터 전달되는 공통 envelope.
 */
@JsonClass(generateAdapter = true)
data class JobWebSocketEnvelope(
    val type: String,
    val payload: JobWebSocketPayload?
)

/**
 * 이벤트별 페이로드를 통합한 모델.
 */
@JsonClass(generateAdapter = true)
data class JobWebSocketPayload(
    val jobId: Long? = null,
    val progress: Int? = null,
    val phase: String? = null,
    val message: String? = null,
    val downloadUrl: String? = null,
    val checksum: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

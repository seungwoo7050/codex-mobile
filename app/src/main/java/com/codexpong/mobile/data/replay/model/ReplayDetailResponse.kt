package com.codexpong.mobile.data.replay.model

import com.squareup.moshi.JsonClass

/**
 * 리플레이 상세 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class ReplayDetailResponse(
    val summary: ReplaySummaryResponse?,
    val checksum: String?,
    val downloadPath: String?
)

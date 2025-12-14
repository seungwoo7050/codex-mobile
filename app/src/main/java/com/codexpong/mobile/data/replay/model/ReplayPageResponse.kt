package com.codexpong.mobile.data.replay.model

import com.squareup.moshi.JsonClass

/**
 * 리플레이 페이지 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class ReplayPageResponse(
    val items: List<ReplaySummaryResponse>?,
    val page: Int?,
    val size: Int?,
    val totalElements: Long?,
    val totalPages: Int?
)

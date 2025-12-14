package com.codexpong.mobile.data.job.model

import com.squareup.moshi.JsonClass

/**
 * 잡 목록 페이지 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class JobPageResponse(
    val items: List<JobResponse>?,
    val page: Int?,
    val size: Int?,
    val totalItems: Long?,
    val totalPages: Int?
)

package com.codexpong.mobile.data.job.model

import com.squareup.moshi.JsonClass

/**
 * 잡 생성 시 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class JobCreateResponse(
    val jobId: Long?
)

package com.codexpong.mobile.data.job.model

import com.squareup.moshi.JsonClass

/**
 * 잡 상세 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class JobResponse(
    val jobId: Long?,
    val jobType: String?,
    val status: String?,
    val progress: Int?,
    val targetReplayId: Long?,
    val createdAt: String?,
    val startedAt: String?,
    val endedAt: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val resultUri: String?,
    val downloadUrl: String?
)

package com.codexpong.mobile.data.job.model

import com.squareup.moshi.JsonClass

/**
 * 잡 상세 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class JobResponse(
    val jobId: Long? = null,
    val jobType: String? = null,
    val status: String? = null,
    val progress: Int? = null,
    val targetReplayId: Long? = null,
    val createdAt: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val resultUri: String? = null,
    val downloadUrl: String? = null,
    val checksum: String? = null
)

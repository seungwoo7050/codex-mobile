package com.codexpong.mobile.data.replay.model

import com.squareup.moshi.JsonClass

/**
 * 리플레이 요약 응답 모델.
 */
@JsonClass(generateAdapter = true)
data class ReplaySummaryResponse(
    val replayId: Long?,
    val matchId: Long?,
    val ownerUserId: Long?,
    val opponentUserId: Long?,
    val opponentNickname: String?,
    val matchType: String?,
    val myScore: Int?,
    val opponentScore: Int?,
    val durationMs: Long?,
    val createdAt: String?,
    val eventFormat: String?
)

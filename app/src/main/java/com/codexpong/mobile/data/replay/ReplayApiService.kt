package com.codexpong.mobile.data.replay

import com.codexpong.mobile.data.replay.model.ReplayDetailResponse
import com.codexpong.mobile.data.replay.model.ReplayPageResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 리플레이 관련 API 명세.
 */
interface ReplayApiService {
    @GET("/api/replays")
    suspend fun getReplays(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ReplayPageResponse

    @GET("/api/replays/{replayId}")
    suspend fun getReplayDetail(
        @Path("replayId") replayId: Long
    ): ReplayDetailResponse
}

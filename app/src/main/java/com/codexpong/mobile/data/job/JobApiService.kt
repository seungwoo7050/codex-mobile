package com.codexpong.mobile.data.job

import com.codexpong.mobile.data.job.model.JobCreateResponse
import com.codexpong.mobile.data.job.model.JobPageResponse
import com.codexpong.mobile.data.job.model.JobResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 잡 관련 REST API 정의.
 */
interface JobApiService {
    @POST("/api/replays/{replayId}/exports/mp4")
    suspend fun requestReplayMp4(
        @Path("replayId") replayId: Long
    ): JobCreateResponse

    @POST("/api/replays/{replayId}/exports/thumbnail")
    suspend fun requestReplayThumbnail(
        @Path("replayId") replayId: Long
    ): JobCreateResponse

    @GET("/api/jobs")
    suspend fun getJobs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null
    ): JobPageResponse

    @GET("/api/jobs/{jobId}")
    suspend fun getJobDetail(
        @Path("jobId") jobId: Long
    ): JobResponse
}

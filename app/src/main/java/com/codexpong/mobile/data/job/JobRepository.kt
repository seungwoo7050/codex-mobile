package com.codexpong.mobile.data.job

import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.job.model.JobCreateResponse
import com.codexpong.mobile.data.job.model.JobPageResponse
import com.codexpong.mobile.data.job.model.JobResponse
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * 잡 생성 및 조회를 담당하는 저장소.
 */
open class JobRepository(
    private val baseUrlRepository: BaseUrlRepository,
    private val retrofitProvider: RetrofitProvider
) {
    private suspend fun service(): JobApiService {
        val baseUrl = baseUrlRepository.currentBaseUrl()
        return retrofitProvider.create(baseUrl).create(JobApiService::class.java)
    }

    /**
     * MP4 내보내기 잡을 생성한다.
     */
    open suspend fun requestMp4Export(replayId: Long): Result<JobCreateResponse> = withContext(Dispatchers.IO) {
        runCatching { service().requestReplayMp4(replayId) }
    }

    /**
     * 썸네일 내보내기 잡을 생성한다.
     */
    open suspend fun requestThumbnailExport(replayId: Long): Result<JobCreateResponse> = withContext(Dispatchers.IO) {
        runCatching { service().requestReplayThumbnail(replayId) }
    }

    /**
     * 잡 목록을 페이지 단위로 조회한다.
     */
    open suspend fun fetchJobs(
        page: Int,
        size: Int,
        status: String?,
        type: String?
    ): Result<JobPageResponse> = withContext(Dispatchers.IO) {
        runCatching { service().getJobs(page, size, status, type) }
    }

    /**
     * 단일 잡 상세를 조회한다.
     */
    open suspend fun fetchJobDetail(jobId: Long): Result<JobResponse> = withContext(Dispatchers.IO) {
        runCatching { service().getJobDetail(jobId) }
    }

    /**
     * 완료된 잡 결과를 스트리밍으로 다운로드하여 지정된 파일에 저장한다.
     */
    open suspend fun downloadResult(jobId: Long, destination: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = baseUrlRepository.currentBaseUrl().trimEnd('/')
            val request = Request.Builder()
                .url("$baseUrl/api/jobs/$jobId/result")
                .get()
                .build()

            retrofitProvider.client().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("다운로드 실패: ${response.code}")
                }
                val body = response.body ?: throw IOException("다운로드 응답이 비었습니다")
                body.byteStream().use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            destination
        }
    }
}

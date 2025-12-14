package com.codexpong.mobile.data.replay

import com.codexpong.mobile.core.network.BaseUrlRepository
import com.codexpong.mobile.core.network.RetrofitProvider
import com.codexpong.mobile.data.replay.model.ReplayDetailResponse
import com.codexpong.mobile.data.replay.model.ReplayPageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 리플레이 목록 및 상세를 제공하는 저장소.
 */
open class ReplayRepository(
    private val baseUrlRepository: BaseUrlRepository,
    private val retrofitProvider: RetrofitProvider
) {
    private suspend fun service(): ReplayApiService {
        val baseUrl = baseUrlRepository.currentBaseUrl()
        return retrofitProvider.create(baseUrl).create(ReplayApiService::class.java)
    }

    /**
     * /api/replays 결과를 페이지 단위로 가져온다.
     */
    open suspend fun fetchReplays(page: Int, size: Int): Result<ReplayPageResponse> = withContext(Dispatchers.IO) {
        runCatching { service().getReplays(page, size) }
    }

    /**
     * /api/replays/{replayId} 상세를 반환한다.
     */
    open suspend fun fetchReplayDetail(replayId: Long): Result<ReplayDetailResponse> = withContext(Dispatchers.IO) {
        runCatching { service().getReplayDetail(replayId) }
    }
}

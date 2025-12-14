package com.codexpong.mobile.contract

import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 계약 파일 존재 여부와 필수 경로를 확인하는 게이트 테스트.
 */
class ContractGateTest {

    @Test
    fun `contracts 파일이 모두 존재해야 한다`() {
        val rootDir = File("../contracts")
        val openapi = File(rootDir, "openapi.json")
        val wsContract = File(rootDir, "ws-contract.md")
        val serverVersion = File(rootDir, "SERVER_VERSION.txt")

        assertTrue("openapi.json 이 존재해야 한다", openapi.exists())
        assertTrue("ws-contract.md 이 존재해야 한다", wsContract.exists())
        assertTrue("SERVER_VERSION.txt 이 존재해야 한다", serverVersion.exists())
    }

    @Test
    fun `openapi에 health 엔드포인트가 존재해야 한다`() {
        val openapi = File("../contracts/openapi.json")
        assertTrue("openapi.json 파일이 존재해야 한다", openapi.exists())
        val json = JSONObject(openapi.readText())
        val paths = json.optJSONObject("paths")
        val healthPath = paths?.optJSONObject("/api/health")
        assertNotNull("/api/health 경로가 정의되어야 한다", healthPath)
    }

    @Test
    fun `openapi에 인증 및 프로필 경로가 존재해야 한다`() {
        val openapi = File("../contracts/openapi.json")
        assertTrue("openapi.json 파일이 존재해야 한다", openapi.exists())
        val json = JSONObject(openapi.readText())
        val paths = json.optJSONObject("paths")

        assertNotNull("/api/auth/login 경로가 정의되어야 한다", paths?.optJSONObject("/api/auth/login"))
        assertNotNull("/api/auth/register 경로가 정의되어야 한다", paths?.optJSONObject("/api/auth/register"))
        assertNotNull("/api/auth/logout 경로가 정의되어야 한다", paths?.optJSONObject("/api/auth/logout"))
        assertNotNull("/api/users/me 경로가 정의되어야 한다", paths?.optJSONObject("/api/users/me"))
    }

    @Test
    fun `openapi에 리플레이 목록 및 상세 경로가 존재해야 한다`() {
        val openapi = File("../contracts/openapi.json")
        assertTrue("openapi.json 파일이 존재해야 한다", openapi.exists())
        val json = JSONObject(openapi.readText())
        val paths = json.optJSONObject("paths")

        assertNotNull("/api/replays 경로가 정의되어야 한다", paths?.optJSONObject("/api/replays"))
        assertNotNull("/api/replays/{replayId} 경로가 정의되어야 한다", paths?.optJSONObject("/api/replays/{replayId}"))
    }
}

# VERSION_PROMPTS.md

이 문서는 모바일 레포에서 **버전별로 에이전트에게 그대로 던질 수 있는 프롬프트 모음**이다.

전제:
- 레포 루트에 아래 문서가 존재한다:
  - AGENTS.md
  - STACK_DESIGN.md
  - PRODUCT_SPEC.md
  - CODING_GUIDE.md
  - VERSIONING.md
  - VERSION_PROMPTS.md
- 그리고 아래 계약 파일이 반드시 존재한다:
  - contracts/openapi.json
  - contracts/ws-contract.md
  - contracts/SERVER_VERSION.txt

공통 강제:
- 코드 주석/사용자 문구/인간용 문서는 한국어.
- 바이너리 파일 커밋 금지(png/jar/keystore/apk/aab 등).
- gradle-wrapper.jar 커밋 금지.
- 계약(openapi/ws) 밖의 엔드포인트/필드/이벤트를 추측해서 만들지 말 것.

---

## v0.1.0 – Skeleton + contract gates + health smoke

```text
1) AGENTS.md → STACK_DESIGN.md → PRODUCT_SPEC.md → CODING_GUIDE.md → VERSIONING.md → VERSION_PROMPTS.md를 순서대로 읽어라.
2) contracts/ 3개 파일이 존재하는지 확인해라.
3) 안드로이드 프로젝트 스켈레톤을 만든다(Compose + Navigation).
   - 주의: Android Studio 기본 템플릿이 png 아이콘을 만들면 즉시 삭제하고, XML vector만 사용한다.
4) v0.1.0에서 반드시 추가할 것:
   - Contract gate test:
     - contracts 파일 3개 존재 확인
     - openapi.json 안에 "/api/health" path가 존재하는지 확인
   - Health smoke:
     - /api/health 호출해서 UI에 표시
   - Settings:
     - base URL을 런타임에서 바꿀 수 있게(에뮬레이터/실기기 대응)
5) 테스트를 통과시켜라.
6) v0.1.0 산출물로 CLONE_GUIDE.md(한국어)를 생성해라(실행/테스트 방법 포함).
7) VERSIONING.md에서 v0.1.0을 완료 처리해라.
8) .github/workflows/ci.yml에 CI 설정을 추가해라.
````

---

## v0.2.0 – Auth + token + profile

```text
1) 위 문서들 다시 읽고 v0.2.0 범위만 작업해라.
2) openapi.json을 기준으로 다음을 구현:
   - 로그인/회원가입/로그아웃
   - 토큰 저장(DataStore)
   - Authorization: Bearer <token> 자동 첨부(Interceptor)
   - 내 프로필 조회/수정 (/api/users/me)
3) 실패 처리:
   - 401이면 토큰 삭제 + 로그인 화면으로 전환
4) 테스트:
   - 로그인 성공/실패(401)
   - 토큰 인터셉터 동작
   - 프로필 GET/PUT
5) CLONE_GUIDE.md를 v0.2.0 기준으로 갱신하고,
6) VERSIONING.md에서 v0.2.0 완료 처리.
```

---

## v0.3.0 – Replays list + detail

```text
1) v0.3.0 범위: 리플레이 목록/상세만.
2) 구현:
   - GET /api/replays (paging)
   - GET /api/replays/{replayId}
   - 목록 화면 + 상세 화면
3) 테스트:
   - MockWebServer로 응답 스키마 파싱 검증
   - 페이징/빈 상태/에러 상태
4) 문서/버전 갱신:
   - CLONE_GUIDE.md 업데이트
   - VERSIONING.md v0.3.0 완료 처리
```

---

## v0.4.0 – Export requests + jobs list/detail (REST)

```text
1) v0.4.0 범위: 내보내기 요청 + 잡 REST만.
2) 구현:
   - POST /api/replays/{replayId}/exports/mp4
   - POST /api/replays/{replayId}/exports/thumbnail
   - GET /api/jobs (paging/filter)
   - GET /api/jobs/{jobId}
3) UI:
   - 리플레이 상세에서 export 버튼
   - jobs 목록 + jobs 상세
4) 테스트:
   - jobId 수신
   - jobs 목록/상세 파싱
   - 401/5xx 처리
5) VERSIONING.md v0.4.0 완료 처리.
```

---

## v0.5.0 – Job progress via WebSocket + download

```text
1) ws-contract.md를 기준으로 job progress WS를 구현한다.
2) 구현:
   - ws://<host>:8080/ws/jobs?token=<JWT> 연결
   - 이벤트 처리:
     - job.connected
     - job.progress
     - job.completed
     - job.failed
   - 재연결(지수 백오프)
   - 재연결 후 REST로 상태 보강: GET /api/jobs/{jobId}
3) 다운로드:
   - GET /api/jobs/{jobId}/result 스트리밍 다운로드
   - 앱 저장소에 저장(바이너리 파일을 레포에 추가하지 말 것)
4) 테스트:
   - MockWebServer websocket upgrade로 이벤트 파싱/상태전이 테스트
   - disconnect/duplicate 이벤트/failed 이벤트 케이스
5) 문서:
   - CLONE_GUIDE.md에 WS/다운로드/재연결 정책을 한국어로 명시
6) VERSIONING.md v0.5.0 완료 처리.
```

---

## v1.0.0 – Portfolio-ready hardening

```text
1) 새 기능 추가 금지. 안정성/재현성/테스트/문서만 강화.
2) 최소 보장 시나리오를 자동화 테스트로 고정:
   - login -> replay list -> export -> ws progress -> download
3) 문서 정리:
   - CLONE_GUIDE.md를 “리뷰어가 그대로 따라하면 재현되는 수준”으로 완성
4) VERSIONING.md에서 v1.0.0 완료 처리.
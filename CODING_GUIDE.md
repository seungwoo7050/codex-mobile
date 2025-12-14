# CODING_GUIDE.md

This document defines coding conventions for the Android client.

## 1. Global rules

### 1.1 Language policy (MANDATORY)

- All code comments MUST be written in Korean.
- All user-facing strings MUST be Korean.
- Identifiers MUST be English.

### 1.2 Binary policy (MANDATORY)

Do not commit binaries. If Android tooling generates PNG icons by default, delete them.

### 1.3 Package naming

- Base package example: `com.codexpong.mobile`
- Layers:
  - `ui.*`
  - `domain.*`
  - `data.*`
  - `core.*`

## 2. Kotlin / Compose rules

- Prefer immutable state: data classes + copy()
- UI state must be a single `StateFlow<UiState>`
- Avoid mutable shared singletons (except a minimal AppContainer)

### 2.1 Compose

- One screen = one route
- No network call inside composables
- Side-effects:
  - use LaunchedEffect only for UI lifecycle triggers
  - do not hide network logic in composables

## 3. Networking rules

### 3.1 REST client

- Retrofit + OkHttp
- Authentication:
  - Inject `Authorization: Bearer <token>` via OkHttp Interceptor
- Error handling:
  - Map HTTP errors into a single `ApiError` model
  - 401 must trigger “로그인 필요/토큰 만료” UX

### 3.2 WebSocket client

- Use OkHttp WebSocket
- Parse incoming message as:
  - envelope: `{ type: String, payload: JsonObject? }`
- WS is best-effort:
  - client must tolerate disconnects and duplicate events
  - on reconnect, reconcile with REST `GET /api/jobs/{jobId}` where needed

### 3.3 Timeouts / retries

- Use sane defaults:
  - connect/read timeouts (OkHttp)
- Retry policy:
  - exponential backoff
  - cap max delay
  - never infinite hot loop

## 4. Testing

### 4.1 Unit tests

- Reducers, mappers, parsers, use-cases
- Pure Kotlin JVM tests whenever possible

### 4.2 API tests

- Use OkHttp MockWebServer for REST
- For WS, use MockWebServer websocket upgrade:
  - test parsing of:
    - job.connected
    - job.progress
    - job.completed
    - job.failed

### 4.3 Contract gate tests (MANDATORY)

Add tests that fail fast if:
- `contracts/openapi.json` missing
- `contracts/ws-contract.md` missing
- `contracts/SERVER_VERSION.txt` missing
- required endpoint paths are missing for the target version scope

Do NOT implement features without these gates.

## 5. Repo hygiene

- `.gradle/`, `build/`, `.idea/`, `local.properties` must not be committed.
- Never commit keystore or signing configs.
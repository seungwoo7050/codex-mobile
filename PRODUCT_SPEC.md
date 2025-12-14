# PRODUCT_SPEC.md

This document defines what the Android client MUST build (scope + user flows).
The source of truth for network behavior is `contracts/`.

## 1. Goal

Build a portfolio-grade Android client that demonstrates:
- contract-first development (OpenAPI + WS contract)
- auth/token handling
- async job lifecycle UI (create job -> progress -> completion/failure)
- WebSocket real-time progress + REST reconciliation
- reliable error handling (401, timeouts, retries)

## 2. Non-goals (explicitly out of scope)

Unless a later version explicitly adds them:
- implementing real-time gameplay UI
- chat/tournament/admin full feature coverage
- any server-side implementation
- push notifications

## 3. Core user flows

### 3.1 Authentication

- Register (username/password) if needed
- Login to obtain JWT
- Logout
- Persist token securely (DataStore)

### 3.2 Profile

- View my profile
- Update basic profile fields (as allowed by API)

### 3.3 Replays (browse + detail)

- List my replays (paging)
- Open replay detail
- From replay detail:
  - request export (MP4)
  - request thumbnail export

### 3.4 Jobs (async export pipeline)

- After requesting export, the client receives a `jobId`
- Client shows:
  - job list (paging/filter)
  - job detail state
  - progress updates in near real-time using WebSocket

Progress source:
- Primary: WebSocket events
- Secondary: REST reconciliation (poll or on reconnect)

Completion:
- When job is completed:
  - expose a “download” action to fetch result
- When failed:
  - show errorCode/errorMessage

### 3.5 Download

- Download job result from server when ready.
- Store in app-private storage (cache or files).
- Do not bundle any sample media files in the repository.

## 4. UX rules

- All user-facing texts must be Korean.
- Error messages must be actionable:
  - “인증 만료” / “네트워크 오류” / “서버 응답 없음” 등

## 5. Quality bar

- Every network feature requires:
  - success flow
  - at least one failure test (401 / 5xx / timeout)
- No binary commits policy must be preserved.
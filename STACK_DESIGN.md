# STACK_DESIGN.md

This document defines the authoritative tech stack and architecture for the Android client.

## 1. Overview

- Platform: Android
- Language: Kotlin
- UI: Jetpack Compose
- Networking: REST + WebSocket (raw)
- API contracts: provided under `contracts/` (contract-first)

This client targets the server snapshot defined in:
- `contracts/SERVER_VERSION.txt`

## 2. Tech stack

### 2.1 Android baseline

- minSdk: 26 (to keep resource/icon strategy binary-free and simplify)
- targetSdk: latest stable
- Build system: Gradle (NO committed gradle-wrapper.jar)

### 2.2 UI

- Jetpack Compose (Material 3)
- Navigation: androidx.navigation-compose
- State:
  - ViewModel + Kotlin Flow (StateFlow)
  - UI state is immutable data classes

### 2.3 Networking

REST:
- Retrofit + OkHttp
- JSON: Moshi (or Kotlinx Serialization; pick one and keep it consistent)

WebSocket:
- OkHttp WebSocket (raw WebSocket, not STOMP)

### 2.4 Storage

- Auth token: DataStore (Preferences)
- Cache (later versions only if needed):
  - Optional Room (but do not introduce early if not required by VERSIONING)

### 2.5 Concurrency

- Kotlin Coroutines
- No blocking on main thread
- IO work: Dispatchers.IO

## 3. Architecture

Recommended layering (keep it pragmatic):

- ui/
  - Compose screens + viewmodels
- domain/
  - use-cases + pure models (no Android dependencies where possible)
- data/
  - api clients, websocket client, repositories, DTO mappers
- core/
  - error model, auth token provider, time/log helpers

Rules:
- UI does not call Retrofit/OkHttp directly.
- WebSocket parsing/dispatch is centralized in one module (data layer).
- Domain layer must not depend on Retrofit/OkHttp.

## 4. Contract mapping (MUST follow `contracts/`)

### 4.1 Base URLs

- REST base URL is environment-dependent (emulator/device), but the contract server URL is provided by:
  - `contracts/SERVER_VERSION.txt`
  - `contracts/openapi.json` servers section

Client MUST support configuring base URL at runtime for dev:
- Emulator typical: `http://10.0.2.2:8080`
- Physical device: `http://<LAN-IP>:8080`

### 4.2 REST authentication

- Store JWT token after login.
- Send token via `Authorization: Bearer <token>` on protected endpoints.

### 4.3 WebSocket authentication

- Connect to job progress WS:
  - `ws://<host>:8080/ws/jobs?token=<JWT>`
- Parse incoming text messages as JSON.
- Handle initial connection ack message if present.

## 5. Testing approach

- Unit tests:
  - parsing, mapping, reducers, use-cases
- Integration-ish tests (JVM):
  - REST: OkHttp MockWebServer
  - WebSocket: MockWebServer websocket upgrade to test event parsing + reconnection policy
- No UI screenshot assets committed.

## 6. CI / reproducibility

- Do not commit gradle-wrapper.jar.
- CI should install/setup Gradle and run:
  - unit tests
  - static checks (optional: ktlint/detekt)
- Never produce APK/AAB as committed artifacts.
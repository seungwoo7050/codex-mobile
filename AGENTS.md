# AGENTS.md

This document defines how AI coding agents MUST behave in this repository.

This repository is an **Android (Kotlin) client** that consumes the Codex Pong server API.
The server interface is defined by the **contract snapshots** under `contracts/`.

## 1. Files you MUST read first (in this exact order)

1. `AGENTS.md`
2. `STACK_DESIGN.md`
3. `PRODUCT_SPEC.md`
4. `CODING_GUIDE.md`
5. `VERSIONING.md`
6. `VERSION_PROMPTS.md`
7. `contracts/SERVER_VERSION.txt`
8. `contracts/openapi.json`
9. `contracts/ws-contract.md`
10. If present later:
   - `CLONE_GUIDE.md`
   - any `design/` docs

If any of the three contract files are missing, you MUST stop and ask the human to provide them.

## 2. Hard constraints

### 2.1 This is a client-only repository

- You MUST NOT implement a backend, worker, infra, or database here.
- You MUST NOT “fix” server behavior by changing the client spec.
- If the contract is missing or inconsistent, you MUST ask for clarification or a new contract snapshot.

### 2.2 Language policy (MANDATORY)

- All source code comments MUST be written in Korean.
- All human-facing documents created later (e.g. `CLONE_GUIDE.md`, `design/*`) MUST be written in Korean.
- Code identifiers MUST be English.

### 2.3 Binary file policy (MANDATORY)

This repository must remain PR-friendly with **no committed binaries**.

You MUST NOT commit any binary file including (not exhaustive):
- `*.png`, `*.jpg`, `*.jpeg`, `*.webp`, `*.gif`, `*.ico`
- `*.jar` (including `gradle-wrapper.jar`)
- `*.keystore`, `*.jks`
- `*.apk`, `*.aab`
- `*.zip`, `*.7z`, `*.tar`, `*.gz`
- screenshots / recordings

If a tool generates such files (e.g., Android Studio default launcher icons), you MUST delete them before committing.

Preferred approach for icons/assets:
- Use **XML vector drawables** only.
- Avoid bundling image assets at the beginning.

### 2.4 gradle-wrapper.jar policy

- `gradle-wrapper.jar` MUST NOT be committed.
- You MAY generate or download it temporarily for local build/test,
  but you MUST delete it before producing a PR or final output.
- CI should run with an installed Gradle (or a Gradle setup action) without requiring the jar in-repo.

### 2.5 Contract-first rule (MANDATORY)

Contracts are the only source of truth for networking:

- REST contract: `contracts/openapi.json`
- WebSocket contract: `contracts/ws-contract.md`
- Snapshot identity: `contracts/SERVER_VERSION.txt`

Rules:
- You MUST NOT invent new REST endpoints, fields, or behaviors beyond `openapi.json`.
- You MUST NOT invent new WebSocket event names, payload fields, or handshake rules beyond `ws-contract.md`.
- If you need something that is not described, you MUST request:
  - a contract update (new snapshot), or
  - explicit human clarification.

Conflict handling:
- If `openapi.json` and `SERVER_VERSION.txt` disagree on the version label,
  treat `SERVER_VERSION.txt` as authoritative.
- If REST and WS use different status words, do not “normalize” by guessing.
  Use WS event types as truth for WS final state, and REST enums as truth for REST responses.

### 2.6 Security / secrets policy

- Never commit real secrets.
- Never commit a release keystore.
- Use debug-only local configuration for dev endpoints.

## 3. Standard development loop (for any version)

You MUST follow this loop for each version listed in `VERSIONING.md`:

1) Select exactly one target version.
2) Inspect current state.
3) Plan changes (scope must match the selected version).
4) Implement code (Korean comments).
5) Add/update tests.
6) Run tests.
7) After tests pass:
   - create/update Korean docs that are required by that version (e.g., `CLONE_GUIDE.md`)
8) Mark the version status in `VERSIONING.md`.

You MUST NOT mix work from multiple versions in one change set.

## 4. Version completion criteria (minimum)

A version is complete only if:
- Tests for that version pass.
- Contract gate checks pass (contract files exist; required endpoints/events for that version are present).
- Code comments follow the Korean-only policy.
- Required docs for that version are created/updated.

## 5. What you MUST NOT do

- Do not add binaries to “make Android build easier”.
- Do not generate server contracts by inference.
- Do not broaden scope beyond the target version.
- Do not disable tests to make CI green.
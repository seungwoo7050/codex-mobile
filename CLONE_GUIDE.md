# CLONE_GUIDE (v0.5.0)

이 문서는 레포를 복제한 뒤 빠르게 실행/테스트하기 위한 안내서다.

## 1. 필수 도구
- JDK 21
- Android SDK (platforms;android-35, build-tools;35.0.0, platform-tools)
- Gradle 8.14.3 이상 (레포에 wrapper가 없으므로 로컬에 설치된 gradle 명령을 사용한다)

### 1.1 Android SDK 설치 예시 (리눅스)
```bash
SDK_ROOT="$HOME/android-sdk"
mkdir -p "$SDK_ROOT"
# commandline-tools 설치 후 sdkmanager 사용
# sdkmanager가 PATH에 없다면 "$SDK_ROOT/cmdline-tools/latest/bin"을 PATH에 추가
yes | sdkmanager --sdk_root="$SDK_ROOT" "platform-tools" "build-tools;35.0.0" "platforms;android-35"
```
- 환경 변수 설정: `export ANDROID_SDK_ROOT=$SDK_ROOT` `export ANDROID_HOME=$SDK_ROOT`

## 2. 프로젝트 구조 및 주요 기능
- Compose + Navigation 기반의 단일 앱 모듈(`app`)
- v0.5.0 기능:
  - 로그인/회원가입/로그아웃, 프로필 조회/수정 유지
  - 리플레이 목록(페이징) 및 상세 조회(`/api/replays`, `/api/replays/{replayId}`)
  - 내보내기 요청 후 잡 목록/상세 REST 조회(`/api/jobs`, `/api/jobs/{jobId}`)
  - 잡 상세 화면에서 WebSocket을 통해 실시간 진행률(`job.connected`, `job.progress`, `job.completed`, `job.failed`) 반영
  - 완료된 잡의 결과를 `/api/jobs/{jobId}/result`로 스트리밍 다운로드하여 앱 캐시에 저장
  - WebSocket 연결 끊김 시 지수 백오프로 재연결하고, 연결 재개 시 REST로 상태를 보강

## 3. 실행 방법
1) Android Studio에서 열거나, CLI로 빌드:
```bash
export ANDROID_SDK_ROOT=$SDK_ROOT
export ANDROID_HOME=$SDK_ROOT
gradle assembleDebug
```
2) 앱 실행 후 `환경 설정`에서 기본 URL을 서버 주소로 맞춘다.
3) 로그인/회원가입 후 프로필 화면에서 내 정보 확인 및 수정이 가능하다.
4) 리플레이 상세에서 내보내기 요청을 누르면 잡 상세 화면으로 이동하여 실시간 진행률을 확인할 수 있다.

## 4. 테스트 방법
- 단위 테스트 전체 실행:
```bash
export ANDROID_SDK_ROOT=$SDK_ROOT
export ANDROID_HOME=$SDK_ROOT
gradle test --console=plain --no-daemon --no-parallel
```
- 주요 테스트 커버리지:
  - 계약 게이트(`/api/health`, 인증/프로필, 리플레이 목록/상세 경로 확인)
  - 로그인 성공/401 실패
  - Authorization 헤더 인터셉터 및 401 시 토큰 정리
  - 프로필 GET/PUT 파싱
  - 리플레이 목록/상세 파싱(MockWebServer)
  - 리플레이 목록 ViewModel의 페이징/빈/에러 상태
  - 잡 WebSocket 연결/재연결, 진행률/완료/실패 이벤트 파싱(MockWebServer 업그레이드)
  - 잡 상세 ViewModel의 진행률/실패 멱등 처리
  - 잡 결과 스트리밍 다운로드 요청 경로 검증

## 5. 기타 주의사항
- PNG/JAR/APK 등의 바이너리 파일을 커밋하지 않는다.
- `gradle-wrapper.jar`를 레포에 추가하지 않는다.
- 모든 한국어 UI/주석 정책을 유지한다.

## 6. WebSocket/다운로드/재연결 정책
- WebSocket 주소: `ws://<호스트>:8080/ws/jobs?token=<JWT>` (기본 URL의 http/https를 ws/wss로 변환하여 사용)
- 이벤트 타입: `job.connected`, `job.progress`, `job.completed`, `job.failed`만 처리하며, 동일 이벤트가 중복 수신되어도 멱등하게 반영한다.
- 연결이 끊기면 지수 백오프(1s→2s→4s→8s 캡)로 재연결한다.
- 재연결 후에는 `GET /api/jobs/{jobId}`로 한 번 더 상태를 조회하여 누락된 진행률을 보강한다.
- `job.completed`/`job.failed` 이후 뒤늦게 들어오는 `job.progress`는 무시해 최종 상태를 덮어쓰지 않는다.
- 다운로드는 `GET /api/jobs/{jobId}/result`를 스트리밍으로 받아 앱 캐시(`cacheDir`)에 저장하며, 저장된 바이너리 파일은 레포에 커밋하지 않는다.

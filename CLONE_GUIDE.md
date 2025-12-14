# CLONE_GUIDE (v0.3.0)

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
- v0.3.0 기능:
  - 로그인/회원가입/로그아웃, 프로필 조회/수정 유지
  - 리플레이 목록(페이징) 및 상세 조회(`/api/replays`, `/api/replays/{replayId}`)
  - 목록 화면에서 빈 상태/에러 상태/페이징 버튼 제공
  - 상세 화면에서 체크섬, 다운로드 경로, 요약 정보 표시

## 3. 실행 방법
1) Android Studio에서 열거나, CLI로 빌드:
```bash
export ANDROID_SDK_ROOT=$SDK_ROOT
export ANDROID_HOME=$SDK_ROOT
gradle assembleDebug
```
2) 앱 실행 후 `환경 설정`에서 기본 URL을 서버 주소로 맞춘다.
3) 로그인/회원가입 후 프로필 화면에서 내 정보 확인 및 수정이 가능하다.

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

## 5. 기타 주의사항
- PNG/JAR/APK 등의 바이너리 파일을 커밋하지 않는다.
- `gradle-wrapper.jar`를 레포에 추가하지 않는다.
- 모든 한국어 UI/주석 정책을 유지한다.

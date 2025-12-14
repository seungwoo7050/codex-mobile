# CLONE_GUIDE (v0.1.0)

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

## 2. 프로젝트 구조 요약
- Compose + Navigation 기반의 단일 앱 모듈(`app`)
- 설정 화면: REST 기본 URL을 런타임에서 변경 가능
- 헬스 화면: `/api/health` 응답을 호출하여 UI에 표시
- 계약 게이트 테스트: `contracts/`의 3개 파일 존재 여부 및 `/api/health` 경로를 확인

## 3. 실행 방법
1) Android Studio에서 열거나, CLI로 빌드:
```bash
export ANDROID_SDK_ROOT=$SDK_ROOT
export ANDROID_HOME=$SDK_ROOT
gradle assembleDebug
```
2) 에뮬레이터/실기기에서 실행 후 설정 화면에서 기본 URL을 환경에 맞게 수정한다.

## 4. 테스트 방법
- 단위 테스트 전체 실행:
```bash
export ANDROID_SDK_ROOT=$SDK_ROOT
export ANDROID_HOME=$SDK_ROOT
gradle test --console=plain --no-daemon --no-parallel
```
- CI에서도 동일 명령을 사용하며, 계약 게이트 테스트와 헬스 관련 스모크 테스트가 포함된다.

## 5. 기타 주의사항
- PNG/JAR/APK 등의 바이너리 파일을 커밋하지 않는다.
- `gradle-wrapper.jar`를 레포에 추가하지 않는다.
- 모든 한국어 UI/주석 정책을 유지한다.

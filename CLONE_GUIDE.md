# CLONE_GUIDE (v1.0.0)

리뷰어가 그대로 따라 해도 동일한 실행·테스트 결과를 얻도록 모든 단계를 적었다.

## 1. 필수 도구 확인
- JDK 21
- Android SDK: `platforms;android-35`, `build-tools;35.0.0`, `platform-tools`
- Gradle 8.14.3 이상 (레포에 wrapper가 없으므로 로컬 `gradle` 명령을 사용)

```bash
java -version
gradle -v
```
위 두 명령이 정상 출력되면 준비 완료다.

## 2. Android SDK 설치 예시 (리눅스)
```bash
SDK_ROOT="$HOME/android-sdk"
mkdir -p "$SDK_ROOT"
# commandline-tools 설치 후 sdkmanager 사용 (이미 설치돼 있으면 생략)
# sdkmanager가 PATH에 없다면 "$SDK_ROOT/cmdline-tools/latest/bin"을 PATH에 추가
yes | sdkmanager --sdk_root="$SDK_ROOT" "platform-tools" "build-tools;35.0.0" "platforms;android-35"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
```

## 3. 저장소 클론 후 체크리스트
1) 레포 클론: `git clone <repo-url> && cd codex-mobile`
2) 계약 스냅샷 존재 확인: `ls contracts/SERVER_VERSION.txt contracts/openapi.json contracts/ws-contract.md`
3) 바이너리 생성물 제거 정책 유지: PNG/JAR/APK/zip 등은 절대 커밋하지 않는다.

## 4. 테스트 실행(재현 필수)
아래 순서로 전체 단위 테스트를 실행하면 v1.0.0 최소 보장 시나리오까지 검증된다.
```bash
export ANDROID_SDK_ROOT="$SDK_ROOT"  # 2단계에서 설정한 경로
export ANDROID_HOME="$SDK_ROOT"
gradle test --console=plain --no-daemon --no-parallel
```
주요 포함 항목:
- 계약 게이트 검사(필수 contract 파일 확인)
- 인증/프로필/리플레이/잡 REST 파싱
- WebSocket 진행률/재연결 파싱
- **login → replay list → export → ws progress → download** 통합 흐름 고정 테스트

## 5. 앱 실행 및 핵심 시나리오 수동 검증
1) Android Studio로 열거나 CLI 빌드: `gradle assembleDebug`
2) 앱 실행 후 `환경 설정` 화면에서 서버 기본 URL을 실제 서버 주소로 입력(예: `http://10.0.2.2:8080`).
3) 로그인/회원가입 → 토큰이 저장됐는지 확인(로그인 화면으로 되돌아가지 않음).
4) 리플레이 목록에서 항목을 열고 `MP4 내보내기`를 요청하면 잡 상세 화면으로 이동한다.
5) 잡 상세 화면에서 실시간 진행률(WebSocket)과 완료 후 `다운로드` 버튼 활성화를 확인한다.
6) 다운로드가 끝나면 결과 파일이 앱 캐시에 저장되고, 실패 시 한국어 오류 메시지가 노출되어야 한다.

## 6. 자주 발생하는 문제 해결 팁
- `No connected devices`: 에뮬레이터/디바이스 연결 후 다시 `assembleDebug` 또는 Android Studio 실행.
- `ANDROID_SDK_ROOT not set`: 2단계 환경 변수 export 여부 확인.
- 포트 접근 불가: 서버가 로컬이면 에뮬레이터에서는 `10.0.2.2` 주소를 사용한다.
- 캐시/빌드 잔여물: `git clean -xfd`로 정리하되 중요 파일이 삭제되지 않도록 주의한다.

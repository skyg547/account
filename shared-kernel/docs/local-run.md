# shared-kernel local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`shared-kernel`은 공통 타입, 공통 예외, 마스킹, 서비스 메타정보 같은 기술 공통 부품을 담는 `java-library` 모듈입니다.
서버로 실행하지 않고 컴파일과 테스트로 검증합니다.

## IntelliJ에서 확인하기

1. 오른쪽 Gradle 창에서 `account > shared-kernel > Tasks > build > compileJava`를 실행합니다.
2. 또는 상단 Run Configuration에서 `Foundation Library Compile`을 선택합니다.
3. 관련 Boot 앱까지 함께 확인할 때는 `Foundation Infra Tests`를 실행합니다.

## PowerShell 명령

```powershell
.\gradlew :shared-kernel:compileJava --console=plain --max-workers=1 --no-daemon
```

`contracts`와 함께 확인하려면:

```powershell
.\gradlew :contracts:compileJava :shared-kernel:compileJava --console=plain --max-workers=1 --no-daemon
```

## 변경할 때 주의할 점

- 모든 모듈이 함께 물고 들어가는 모듈이므로 비즈니스 규칙을 넣지 않습니다.
- 공통 enum을 바꾸면 직렬화, DB 값, 문서 예시가 같이 영향을 받습니다.
- 공통 예외 응답 형식을 바꾸면 API 테스트와 프론트 처리가 영향을 받습니다.

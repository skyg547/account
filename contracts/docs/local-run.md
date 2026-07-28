# Contracts 로컬 검증

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM 17
- 저장소 루트를 Gradle 프로젝트로 열기

`contracts`는 서버가 아니므로 `bootRun`, 내장 WAS, H2, PostgreSQL, Docker가 필요하지 않습니다.

## IntelliJ

1. Run Configuration에서 `Foundation Library Compile`을 선택합니다.
2. 또는 Gradle 창에서 `contracts > verification > test`를 실행합니다.
3. 계약을 변경했다면 구현/호출 모듈 테스트도 함께 실행합니다.

## Gradle

```powershell
.gradlew :contracts:test :contracts:compileJava --console=plain --max-workers=1 --no-daemon
```

Master Data 기준일 계약까지 변경했을 때:

```powershell
.gradlew :contracts:test :master-data:test :closing:core:test --console=plain --max-workers=1 --no-daemon
```

## 실패를 읽는 순서

1. `contracts:compileJava`: 계약 자체 문법/타입 오류
2. 소비 모듈 `compileJava`: 공개 메서드·생성자 호환 오류
3. 제공 모듈 테스트: Adapter 구현/업무 의미 오류
4. 통합 테스트: 실제 Spring Bean 연결 오류

## 메모리 정리

검증 후 애플리케이션을 띄울 필요가 없습니다. Gradle JVM이 남으면 먼저 확인합니다.

```powershell
jps -l
.gradlew --stop
```

현재 Windows 페이지 파일 여유가 부족한 환경에서는 Gradle이 시작 전에 멈출 수 있습니다.
이 경우 테스트 성공으로 기록하지 말고 시스템 자원을 회복한 뒤 같은 명령을 다시 실행합니다.
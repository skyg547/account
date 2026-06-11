# reporting local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`reporting` 상위 프로젝트는 실행 코드가 없고, 실제 코드는 `reporting:core`, `reporting:api`, `reporting:batch`에 있다. 현재 세 하위 모듈은 standalone Boot 앱이 아니므로 모듈 테스트와 컴파일로 검증한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 아래 태스크를 실행한다.
3. 또는 상단 Run Configuration에서 `Reporting Module Tests`를 선택한다.

추가된 실행 구성:

- `.run/Reporting Module Tests.run.xml`

## PowerShell에서 실행하기

전체 reporting 테스트:

```powershell
.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인:

```powershell
.\gradlew :reporting:core:compileJava :reporting:api:compileJava :reporting:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

## HTTP API를 로컬에서 호출하려면

현재 `reporting:api`에는 `SpringBootApplication`이 없다. 따라서 아래 중 하나가 필요하다.

1. 기존 호스트 애플리케이션이 reporting api/core 패키지를 컴포넌트 스캔하도록 구성한다.
2. 테스트 전용 또는 통합 실행용 Boot 앱 모듈을 별도로 만든다.
3. `ReportingControllerTest`처럼 컨트롤러 단위 테스트로 API 계약을 검증한다.

## 데이터 준비 순서

실제 보고서 생성에는 다음 데이터나 테스트 더블이 필요하다.

1. `LoadLedgerPort`가 반환할 계정별 잔액.
2. `RPT_LINE_MAPPING` 또는 인메모리 매핑 어댑터.
3. 전기 비교를 위한 FINAL 스냅샷.
4. 주석 마트 생성을 위한 `noteNumber`가 있는 보고 라인.
5. 감독보고 제출을 위한 `RPT_REGULATORY_REPORT_MAPPING` seed.

로컬 데모처럼 DB 없이 인메모리 어댑터를 쓰려면 `account.reporting.persistence.mode=memory` 설정을 사용하는 구성이 필요하다.

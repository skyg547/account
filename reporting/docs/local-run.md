# reporting local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

`reporting` 상위 프로젝트는 실행 코드가 없고, 실제 코드는 `reporting:core`, `reporting:api`, `reporting:batch`에 있다. `reporting:core`는 library 모듈이며, `reporting:api`와 `reporting:batch`는 standalone Boot 앱으로 실행한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 아래 태스크를 실행한다.
3. 상단 Run Configuration에서 API 실행, Batch 컨텍스트 실행, 모듈 테스트 중 하나를 선택한다.

추가된 실행 구성:

- `.run/Reporting Module Tests.run.xml`
- `.run/Reporting API bootRun.run.xml`
- `.run/Reporting Batch Context.run.xml`

## PowerShell에서 실행하기

API 앱 실행:

```powershell
.\gradlew :reporting:api:bootRun --args="--spring.profiles.active=local --server.port=8090 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :reporting:batch:bootRun --args="--spring.profiles.active=local --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --account.reporting.persistence.mode=memory --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

전체 reporting 테스트:

```powershell
.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일만 확인:

```powershell
.\gradlew :reporting:core:compileJava :reporting:api:compileJava :reporting:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

bootJar 확인:

```powershell
.\gradlew :reporting:api:bootJar :reporting:batch:bootJar --console=plain --max-workers=1 --no-daemon
```

## HTTP API를 로컬에서 호출하려면

로컬 학습용 실행은 `spring.profiles.active=local`과 `account.reporting.persistence.mode=memory`를 함께 사용한다. `local` profile은 logstash 전송을 끄고 콘솔 로그만 남기며, memory 모드에서는 `InMemoryLedgerBalanceAdapter`가 샘플 GL 잔액을 제공하고 reporting의 인메모리 매핑/스냅샷 어댑터가 사용된다.

예시:

```http
POST http://localhost:8090/api/v1/reporting/generate?type=BALANCE_SHEET&baseDate=2026-03-31T00:00:00
X-User-ID: tester
```

운영형 JPA 모드는 `account.reporting.persistence.mode=jpa` 또는 기본값을 사용한다. 이때는 `LedgerQueryPort` 실제 구현, DB, Flyway migration, 보고 라인 매핑 seed가 준비되어야 한다.

## 데이터 준비 순서

실제 보고서 생성에는 다음 데이터나 테스트 더블이 필요하다.

1. `LoadLedgerPort`가 반환할 계정별 잔액.
2. `RPT_LINE_MAPPING` 또는 인메모리 매핑 어댑터.
3. 전기 비교를 위한 FINAL 스냅샷.
4. 주석 마트 생성을 위한 `noteNumber`가 있는 보고 라인.
5. 감독보고 제출을 위한 `RPT_REGULATORY_REPORT_MAPPING` seed.

로컬 데모처럼 DB 없이 인메모리 어댑터를 쓰려면 `account.reporting.persistence.mode=memory` 설정을 사용한다.

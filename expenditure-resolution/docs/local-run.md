# expenditure-resolution local run

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

## 모듈 구조

`expenditure-resolution`은 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `expenditure-resolution:core`: 지출결의, 예산 차감, AP 지급, 세금계산서/자산/전표 연계 업무 흐름.
- `expenditure-resolution:api`: Spring Boot HTTP 실행 진입점. 상태 전이와 검증은 core를 참조한다.
- `expenditure-resolution:batch`: Spring Boot Batch 실행 진입점. 승인/정산 대상 조회와 Job/Step 제어를 맡고 업무 판단은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > expenditure-resolution > core > Tasks > verification > test`를 실행한다.
3. API 서버를 띄울 때는 Gradle task `:expenditure-resolution:api:bootRun`을 실행한다.
4. Batch 컨텍스트만 확인할 때는 Gradle task `:expenditure-resolution:batch:bootRun`을 실행한다.
5. 실행 후 `BUILD SUCCESSFUL` 또는 Spring Boot `Started ...Application` 로그를 확인한다.

## PowerShell에서 실행하기

core 테스트:

```powershell
.\gradlew :expenditure-resolution:core:test --console=plain --max-workers=1 --no-daemon
```

빠른 컴파일:

```powershell
.\gradlew :expenditure-resolution:core:compileJava :expenditure-resolution:api:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

API 서버 실행:

```powershell
.\gradlew :expenditure-resolution:api:bootRun --console=plain --max-workers=1
```

Batch 컨텍스트 실행:

```powershell
.\gradlew :expenditure-resolution:batch:bootRun --console=plain --max-workers=1
```

실제 Batch Job 실행:

```powershell
.\gradlew :expenditure-resolution:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=expenditureResolutionApprovalJob startDate=2026-06-19 endDate=2026-06-19 paymentDueDate=2026-06-19" --console=plain --max-workers=1
```

`expenditureResolutionApprovalJob`은 조회 기간 내 REQUESTED 결의서 중 지급 예정일이 `paymentDueDate` 이내인 건을 core 승인 유즈케이스로 위임한다.

API 스모크만 확인하고 서버를 계속 띄우지 않을 때:

```powershell
.\gradlew :expenditure-resolution:api:bootRun --args="--spring.main.web-application-type=none" --console=plain --max-workers=1
```

## local profile 동작

local profile은 H2 메모리 DB를 사용하고, Config Server, Eureka, Vault, tracing을 끈다. 지출결의 core가 요구하는 master-data, tax, asset, journal 경계는 local adapter가 대체한다.

초보자 관점에서는 아래처럼 보면 된다.

- 지출결의 서비스는 실제 운영처럼 부서, 계정, 거래처, 세금계산서, 전표 포트를 호출한다.
- 로컬에서는 외부 서버를 띄우지 않기 위해 local adapter가 "존재하는 테스트용 참조 데이터"를 반환한다.
- 업무 규칙은 local adapter에 넣지 않는다. 예산 사용, 상태 전이, 전표 생성 요청 시점은 계속 core 서비스와 도메인이 판단한다.
- Batch 모듈은 `spring.batch.job.enabled=false`로 시작하므로, 별도 Job 이름과 파라미터를 주지 않으면 컨텍스트만 확인하고 종료한다.

## 자주 보는 실패

| 증상 | 확인할 지점 |
| --- | --- |
| `거래처를 찾을 수 없습니다` | local profile이 켜졌는지, 또는 운영 프로파일에서 master-data가 기동 중인지 확인 |
| `예산이 부족합니다` | 테스트 데이터의 예산 배정액과 지출 상세 금액을 확인 |
| H2 DDL 경고 | master-data SCD2 코드 컬럼은 유니크가 아니므로 DB FK 대신 포트 검증을 사용해야 함 |
| Batch가 바로 종료됨 | local 기본 설정은 Job 자동 실행이 꺼져 있다. 실제 Job 검증은 Job 이름과 파라미터를 별도로 준다 |

# expenditure-resolution local run

## 실행 프로파일 계약

- 기본 프로파일은 `local`이다. API와 Batch는 외부 서비스 없이 H2 PostgreSQL mode에서 시작하며, 전용 Flyway V1을 적용한 뒤 Hibernate `validate`로 엔티티 정합성을 확인한다.
- `dev`/`prod`는 PostgreSQL 전용이다. API/Batch 프로세스에서는 Flyway, Hibernate DDL, SQL init, Batch metadata init을 모두 끄고 release-time `migration-runner`가 먼저 스키마를 반영한다.
- `prod` JDBC URL은 `sslmode=verify-full`을 강제한다. 비밀번호는 설정 파일이나 명령행에 기록하지 않고 환경 변수로 주입한다.
- 실제 PostgreSQL clean migrate/validate는 승인된 환경에서 별도로 수행해야 한다. Master Data, Tax, Asset, Journal 원격 어댑터는 Issue #264 범위이므로 그 전까지 완전한 dev/prod 업무 플로우는 준비되지 않았다.

개발 환경은 `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`, 운영은 대응하는 `PROD_DB_*` 변수를 secret injection으로 제공하고 `--spring.profiles.active=dev|prod`로 시작한다. 사설 호스트나 자격증명을 저장소에 기록하지 않는다.

## 전제 조건

- JDK 17
- IntelliJ IDEA Gradle JVM도 JDK 17로 설정
- 저장소 루트를 Gradle 프로젝트로 Import
- Gradle wrapper 사용: `.\gradlew`

## 모듈 구조

`expenditure-resolution`은 `core/api/batch` 하위 Gradle 모듈로 분리되어 있다.

- `expenditure-resolution:core`: 지출결의, 예산 차감, AP 지급, 세금계산서/자산/전표 연계 업무 흐름. HTTP DTO/Controller 없이 command와 port 중심으로 동작한다.
- `expenditure-resolution:api`: Spring Boot HTTP 실행 진입점. 요청 DTO를 core command로 변환한다.
- `expenditure-resolution:batch`: Spring Boot Batch 실행 진입점. 승인/정산 대상 조회와 Job/Step 제어를 맡고 업무 판단은 core를 참조한다.

## IntelliJ에서 실행하기

1. IntelliJ에서 루트 프로젝트를 연다.
2. 오른쪽 Gradle 창에서 `account > expenditure-resolution > core > Tasks > verification > test`를 실행한다.
3. API 서버를 띄울 때는 Gradle task `:expenditure-resolution:api:bootRun`을 실행한다.
4. Batch 컨텍스트만 확인할 때는 Gradle task `:expenditure-resolution:batch:bootRun`을 실행한다.
5. 실행 후 `BUILD SUCCESSFUL` 또는 Spring Boot `Started ...Application` 로그를 확인한다.

## PowerShell에서 실행하기

core/api 테스트:

```powershell
.\gradlew :expenditure-resolution:core:test :expenditure-resolution:api:test --console=plain --max-workers=1
```

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

실행 JAR 생성 및 직접 실행:

```powershell
.\gradlew :expenditure-resolution:api:bootJar :expenditure-resolution:batch:bootJar --console=plain --max-workers=1
java -jar expenditure-resolution\api\build\libs\account-expenditure-resolution-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -jar expenditure-resolution\batch\build\libs\account-expenditure-resolution-batch-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

실제 Batch Job 실행:

```powershell
.\gradlew :expenditure-resolution:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=expenditureResolutionApprovalJob startDate=2026-06-19 endDate=2026-06-19 paymentDueDate=2026-06-19" --console=plain --max-workers=1
```

`expenditureResolutionApprovalJob`은 조회 기간 내 REQUESTED 결의서 중 지급 예정일이 `paymentDueDate` 이내인 건을 core 승인 유즈케이스로 위임한다.

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


ExpenditureResolutionBatchJobRegistryConfiguration은 Spring Batch Job 등록 시점을 늦춰 JobRegistry 조기 초기화 경고를 줄인다. 이 설정은 인프라 초기화 순서만 다루고 업무 로직은 포함하지 않는다.

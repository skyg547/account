# master-data local run

## 전제 조건

- JDK 17
- IntelliJ IDEA의 Gradle JVM도 JDK 17
- 루트 프로젝트 `C:\Users\skyg547\IdeaProjects\account`를 Gradle 프로젝트로 열기

`master-data`는 `core`, `api`, `batch` Gradle 하위 프로젝트로 분리되어 있습니다.
`core`는 실행 진입점이 없는 업무 library로 도메인·application·영속성 구현을 소유합니다.
`api`와 `batch`는 core를 의존하는 독립 Spring Boot 실행 모듈이고 서로 의존하지 않으므로
HTTP 서버와 스케줄 작업을 독립적으로 배포·확장할 수 있습니다. 일일 유효성 보고 계산은 core
pipeline에 두어 batch가 업무 규칙을 중복 구현하지 않습니다.

## IntelliJ에서 H2 단독 실행

1. Gradle 창에서 프로젝트를 새로고침합니다.
2. 상단 Run Configuration에서 `Master Data bootRun`을 선택합니다.
3. 실행하면 기본 `local` profile, 내장 H2 PostgreSQL mode, Flyway V1-V8 (target=8), JPA `validate`로 API 서버가 `8082` 포트에 올라옵니다.
4. 로그에서 `Started MasterDataApplication`을 확인합니다.
5. 테스트가 끝나면 IntelliJ의 Stop 버튼으로 프로세스를 종료합니다.

이 실행 설정은 Config Server, Discovery, Vault, 외부 tracing을 끕니다. 따라서 초보자가 인프라를 먼저 띄우지 않아도 기준정보 API와 JPA 매핑을 확인할 수 있습니다. 인메모리 H2이므로 프로세스를 끄면 데이터도 사라집니다.

### local 기준 데이터

`local` profile로 API를 시작하면 Flyway가 스키마 마이그레이션을 마친 뒤
`data-local.sql`을 실행하여 다음 기준 데이터를 인메모리 H2에 적재합니다.

- 통화 3건: KRW, USD, EUR
- 부서 3건: 재무팀, 회계팀, 경영관리팀
- 계정과목 8건
- 거래처 3건: 신한은행, 삼성전자, 쿠팡
- 2026년 `OPEN` 회계기간 12건: 1월부터 12월까지

H2는 인메모리 데이터베이스이므로 API 프로세스를 종료하면 적재된 데이터도 초기화되고,
다음 `local` 실행 시 다시 적재됩니다. 이 초기화 설정은 `local` profile에만 적용되므로
`dev`와 `prod`의 데이터 초기화 정책에는 영향을 주지 않습니다.

로컬 기준 데이터와 인증 profile 회귀를 함께 확인하려면 저장소 루트에서 다음 명령을 실행합니다.

```powershell
./gradlew :master-data:core:test :master-data:api:test :auth:core:test :auth:api:test --console=plain
```

## PowerShell H2 단독 실행

```powershell
.\gradlew :master-data:api:bootRun --console=plain --max-workers=1
```

API 서버를 열지 않고 Spring/JPA 빈 연결만 확인하려면 다음 smoke 명령을 사용합니다.

```powershell
.\gradlew :master-data:api:bootRun --args="--spring.main.web-application-type=none --server.port=0" --console=plain --max-workers=1
```

## 승인된 PostgreSQL 검증

`master-data`에는 H2와 PostgreSQL JDBC 드라이버가 모두 포함됩니다. PostgreSQL DDL 변경은
애플리케이션이 아니라 `migration-runner`의 승인 절차로 수행한 뒤 `dev` profile로 validate합니다.

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://<approved-host>:5432/master_data_dev'
$env:SPRING_DATASOURCE_USERNAME = '<runtime-role>'
# 비밀번호는 승인된 secret 주입 기능으로 설정합니다.
.\gradlew :master-data:api:bootRun --args="--spring.profiles.active=dev" --console=plain --max-workers=1 --no-daemon
```

V6이 전체 기준정보 스키마를 forward-only로 완성합니다. V1-V5 체크섬은 보존하며 오류를
숨기기 위한 `flyway repair`, 애플리케이션 `ddl-auto=update`, 승인 없는 실DB 접속은 금지합니다.

## 공통 인프라와 함께 실행

Config Server 설정을 확인하는 통합 모드에서는 다음 순서로 실행합니다.

1. `Config Server bootRun`
2. Eureka 등록이 필요하면 `Discovery bootRun`
3. `Master Data bootRun`에서 standalone 인자를 제거한 통합용 실행 설정

통합 모드는 `config-repo/master-data.yml`의 H2/Flyway/Eureka 설정을 사용합니다. 운영 PostgreSQL 접속정보는 저장소나 문서에 직접 기록하지 말고 승인된 환경변수/비밀 저장소로 주입해야 합니다.

## 검증 명령

거래처 도메인/JPA 분리를 집중 검증하려면 다음 명령을 사용합니다.

```powershell
.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test :master-data:api:bootJar :master-data:batch:bootJar --rerun-tasks --console=plain --max-workers=1 --no-daemon
.\gradlew :expenditure-resolution:api:test --tests "com.ho.account.expenditure.integration.ExpenditureTaxApiIntegrationTest" --rerun-tasks --console=plain --max-workers=1 --no-daemon
.\gradlew :loan:core:test --tests "com.ho.account.loan.infrastructure.adapter.LoanReferenceDataAdapterTest" --rerun-tasks --console=plain --max-workers=1 --no-daemon
```

첫 번째 명령은 순수 거래처 불변식, 도메인↔JPA 왕복과 자식 FK, 현재/과거 SCD2 조회,
API 마스킹, API/Batch composition root와 실행 JAR을 포함합니다. 두 번째와 세 번째 명령은
소비 모듈이 JPA Repository 대신
`BusinessPartnerPersistencePort` 또는 contracts 경계를 계속 사용하는지 확인합니다.
H2는 V8까지만 적용하며 PostgreSQL exclusion과 실제 경쟁은 아래 합성 PostgreSQL 회귀로 검증합니다. 운영 데이터 크기의 인덱스/잠금 성능은 별도 배포 검증 대상입니다.

```powershell
.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --console=plain --max-workers=1 --no-daemon
```

`./gradlew :master-data:test`도 같은 세 하위 모듈 테스트를 실행합니다. 순수 도메인,
서비스 버전/잠금 순서, H2 JPA 왕복·API/Batch 구성과 일일 집계 회귀가 포함됩니다.
PostgreSQL 환경변수가 없으면 PostgreSQL 전용 테스트는 skip되며 동시성 검증 통과로
판단할 수 없습니다. 최신 실행 수와 결과는 모듈 [worklog](ai-harness/worklog.md)에 기록합니다.

### PostgreSQL SCD2 회귀 검증

전제: JDK17, 캐시된 Gradle 의존성, **합성 테스트 전용** PostgreSQL16 데이터베이스.
테스트는 `postgres`/빈 비밀번호로 접근하는 로컬 trust 인증 fixture용이며 운영 접속정보를
사용하지 않습니다. 매 실행마다 임의의 새 schema를 만들고 기존 schema/행은 삭제하지 않습니다.
`btree_gist` 생성 권한이 필요합니다.

```bash
MASTER_DATA_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:17533/master_data_753 \
./gradlew :master-data:test --offline --no-daemon --console=plain --max-workers=2 --rerun-tasks
```

이 명령은 네 유형의 서로 다른 승인 ID 경쟁, 직접 쓰기와 승인 반영 경쟁, 키 부재 CREATE,
UPDATE/DEACTIVATE, 롤백과 재시도, 다른 키의 독립 진행, 실제 DB 기간 제약/HTTP409를
검증합니다. PostgreSQL 테스트는 실제 서비스·JPA·Flyway·JDBC를 사용하며 HTTP 응답은
MockMvc로 검증합니다. 배포나 실제 사용자 데이터 보정은 수행하지 않습니다.

H2의 `local` 프로파일과 H2 전용 schema 테스트는 `spring.flyway.target=8`입니다. V9는
PostgreSQL 전용 SQL이며 release migration-runner의 기존 SQL 복사 경로에 포함됩니다.
새 H2 구성도 target8을 명시해야 합니다. PostgreSQL 검증/배포에는 이 target을 적용하지
마세요. 이후 공통 migration을 추가할 때는 이 H2 경계를 함께 검토해야 합니다.

## 일일 유효성 Batch 실행

`Master Data Batch Validity` Run Configuration 또는 아래 명령을 사용합니다. `asOfDate`는 Spring Batch의 식별 파라미터이므로 재실행할 회계 기준일을 반드시 명시합니다.

```powershell
.\gradlew :master-data:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=masterDataValidityJob asOfDate=2026-07-29" --console=plain --max-workers=1 --no-daemon
```

Job/Step은 스케줄과 파라미터 매핑만 담당합니다. 활성 건수 계산과 유효기간 규칙은 `master-data:core`의 `MasterDataValidityReportPipeline`에 남아 있어 API와 Batch가 서로 다른 업무 규칙을 만들지 않습니다.

기본 Batch 실행은 local H2/Flyway/JPA validate 계약을 사용합니다. dev/prod에서는 injected
PostgreSQL과 `initialize-schema=never`를 사용하므로 Batch metadata도 release-time runner가
먼저 provision해야 합니다.

## 주요 엔드포인트

- `GET /api/basic/account-subjects`
- `GET /api/basic/departments`
- `GET /api/basic/businesspartners`
- `GET /api/basic/products`
- `GET /api/basic/references/account-subjects/{code}?effectiveDate=yyyy-MM-dd`
- `GET /api/basic/references/business-partners/{code}?effectiveDate=yyyy-MM-dd`
- `GET /api/basic/references/departments/{code}?effectiveDate=yyyy-MM-dd`
- `GET /api/basic/fiscal-periods/{fiscalYear}/{fiscalPeriod}`
- `POST /api/master-data/change-requests`
- `GET /api/master-data/change-requests/pending`
- `POST /api/master-data/change-requests/{requestId}/approve`
- `POST /api/master-data/change-requests/{requestId}/reject`
- `POST /api/master-data/change-requests/{requestId}/apply`
- `POST /api/master-data/change-requests/apply-due`

`requestedVersion` 예시는 다음과 같습니다.

- 아직 이력이 없는 `D-001` 생성: `CREATE / requestedVersion=1`
- 이력이 1행인 `D-001` 수정: `UPDATE / requestedVersion=2`
- 이력이 2행인 `D-001` 종료: `DEACTIVATE / requestedVersion=2`

지원하지 않는 targetType이나 DTO 형식 오류는 HTTP 400, 저장 이력과 `requestedVersion`이 달라진 낙관적 업무 충돌은 HTTP 409로 구분합니다. 예상하지 못한 내부 예외 메시지는 공통 예외 처리기가 외부에 그대로 노출하지 않습니다.

## 로컬 실행 주의사항

- 로컬 API 기본 포트는 `8082`입니다.
- V1-V5는 그대로 보존하고 V6이 전체 JPA 소유 테이블을 완성합니다. local도 Flyway와 JPA `validate`를 사용합니다.
- dev/prod에서는 `ddl-auto=validate`, runtime Flyway/SQL/Batch 초기화 금지 계약을 유지합니다.
- 일일 유효성 집계는 전체 행을 Java 메모리에 올리지 않고 JPA `COUNT` 쿼리 네 번으로 처리합니다.
- `CURRENCY`, `EXCHANGE_RATE`, `FISCAL_PERIOD` 변경 요청은 typed applier와 버전 조회 어댑터가 함께 추가되기 전까지 접수 단계에서 fail-closed 됩니다.
- 직접 CRUD 쓰기 엔드포인트는 `MasterDataDirectWritePolicy`에 의해 관리자 보정 권한(`ROLE_ADMIN`, `ROLE_SYSTEM_ADMIN` 등)으로 제한되어 있으며, `X-Auth-Roles` 헤더가 없거나 비관리자인 경우 HTTP 403으로 거부됩니다.
- `apply-due`는 현재 최대 500건을 한 트랜잭션으로 처리합니다. 운영 대량 실행 전 요청별 재시작 경계와 `SKIP LOCKED` 파티셔닝이 필요합니다.

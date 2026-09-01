## 2026-07-30 (Issue #45 executable Budget Control foundation)
- 요청 목표: 예산 계획·전용·집행을 실제 core/API/Batch 헥사고날 경계와 영속성으로 구현한다.
- 통합: PR `#246`이 source commit `5f06cad1`, merge commit `db7feeb4`로 `main`에 병합됐고 `Fixes #45`로 Issue가 닫혔다. 원격 feature branch도 삭제됐다.
- 변경:
  - `budget:core`, `budget:api`, `budget:batch` 모듈과 실행 진입점·문서를 추가했다.
  - 순수 계획·전용·집행·회계연도 제어 Aggregate, command/use case, output Port, 트랜잭션 application service를 구현했다.
  - `idempotency shard → fiscal-year control → plan id` 잠금, 동일 월 규칙, 정밀도·잔액·상태 전이, 취소와 durable 회계연도 마감을 fail-closed로 강제했다.
  - Flyway V50의 10,000 연도 제어, 256 lock shard, 세 업무 테이블과 명시적 JPA entity/mapper/adapter를 추가했다.
  - JWT 서명·issuer/역할 검증, JWT `sub` actor, typed HTTP 오류, 7개 endpoint와 Gateway route/fallback을 추가했다.
  - Batch는 core 유즈케이스에만 위임하고 API/Batch JAR에 PostgreSQL driver를 포함한다.
- 검증:
  - Budget Core 31, API 9, Batch 11, Gateway 33, Expenditure Core 10/API 1로 총 95 affected tests와 API/Batch bootJar가 통과했다.
  - 실제 API/Batch composition root가 H2 PostgreSQL mode에서 Flyway V50 seed, JPA schema validate, 실제 Adapter를 함께 기동했다.
  - 두 스레드 테스트가 첫 동시 요청 exactly-once와 close-vs-approval 직렬화를 검증했고, 두 bootJar의 PostgreSQL driver 포함도 확인했다.
  - 최초 리뷰의 P1 4건/P2 2건과 재검토 중 발견한 typed 404, 인프라 예외 오분류, 문자열 길이, 기술중립 `yearMonth` 검증을 보정한 뒤 최종 P0-P3 finding 없이 통과했다.
- 리스크:
  - PostgreSQL 실DB migration/locking, shard hotspot/lock timeout과 연말 마감의 대량 `saveAndFlush` 비용은 미검증이다.
  - 기존 Expenditure 예산 데이터와 reservation/commit/release 호출 흐름의 이전은 #17에서 별도 설계해야 한다.
## 2026-07-30 (Issue #243 PostgreSQL JDBC/Actuator runtime)

- 11개 bounded context의 API/Batch 22개에 PostgreSQL runtime driver를 추가하고, 누락된 API 9개에 Actuator를 추가했다.
- Local H2 dependency와 production/domain 코드는 변경하지 않았다.
- `verifyProductionRuntimeDependencies`가 22개 runtimeClasspath와 bootJar의 PostgreSQL JAR, API 11개의 Actuator JAR을 검증한다.
- 22 bootJar와 영향 테스트 56개(신규 readiness endpoint 9개 포함)가 실패/오류/skip 없이 통과했다. PostgreSQL/migration 실DB 검증은 #244다.
- 독립 리뷰어가 최신 source 상태에서 변경 API 9개 전체 29 tests를 재실행했고, 최종 P0-P3 지적은 없다.
- commit `c8b10947`을 최신 `origin/main@5c810422`에 rebase했고 post-rebase gate와 재리뷰가 통과했다. `d0586cbe`와 함께 push해 Draft PR #248을 열었으며 외부 DB/registry/container/server는 변경하지 않았다.

## 2026-07-30 (Issue #44 GL/Sub-ledger domain authority)
- 요청 목표: 차변/대변 VO, JournalEntry/GeneralLedger Aggregate, BigDecimal 정밀도 정책을 실제 전기 흐름에 연결한다.
- 통합: PR `#238`이 source commit `d8c0504c`, merge commit `299746a7`로 `main`에 병합됐고 `Fixes #44`로 Issue가 닫혔다. 원격 feature branch도 삭제됐다.
- 변경:
  - `Debit`/`Credit`, `AccountingPrecision`, `GeneralLedger`를 추가하고 거래/기준통화 차대일치와 persisted-lineage 규칙을 구현했다.
  - `JournalEntry`가 상세 컬렉션을 소유하고 임의 상태 setter 없이 의도 기반 전이를 사용하도록 보강했다.
  - `LedgerEntryPersistencePort`를 Aggregate 입력으로 바꾸고 JPA/JDBC adapter가 같은 snapshot을 GL/SL 저장 형태로 변환하게 했다.
  - 활성 `GlBalance`/`SlBalance`에 정밀도 정책을 적용하고 미사용 병렬 `Money`/`GlAccountBalance` 계열을 제거했다.
  - 공용 Journal 상태 계약을 사용하던 Loan/Expenditure production/test 호출부를 함께 정렬했다.
- 검증:
  - Journal Ledger Core 35, API 2, Batch 3, Loan Core 30, Expenditure Core 10/API 1, Closing Batch 12로 총 93 tests가 실패/오류/skip 없이 통과했다.
  - Journal Ledger API/Batch와 Closing Batch bootJar, JPA/JDBC mapping parity, diff/conflict 정적 게이트를 확인했다.
  - 독립 리뷰의 Closing Batch 상태 fixture P1과 대형 분할 전표 합계 정밀도 P2를 수정했고, 재검토는 P0-P3 finding 없이 PASS했다.
- 리스크:
  - scale 2는 현재 DB 계약이며 통화별 minor unit 확장은 별도 migration/도메인 정책이 필요하다.
  - PostgreSQL bulk 성능과 실제 lock 경합은 미검증이다.

## 2026-07-30 (Issue #43 EOD/BOD 날짜 상태 머신)
- 요청 목표: 일마감 상태를 실제 도메인·유즈케이스·영속성·API 흐름으로 연결하고 전일 마감 이력을 보존한다.
- 통합: PR `#236`이 source commit `86ebedf9`, merge commit `10d80939`로 `main`에 병합됐고 `Fixes #43`으로 Issue가 닫혔다. 원격 feature branch도 삭제됐다.
- 변경:
  - `DailyClosingStatus`를 날짜별 상태 aggregate로 전환하고 `EodState`, 단계별 actor/시각, `@Version`을 추가했다.
  - EOD 준비/취소/시작/완료와 전일 `CLOSED`를 보존하는 별도 다음 날짜 BOD 생성/완료 유즈케이스를 구현했다.
  - output port, pessimistic lock JPA adapter, trusted-header API, Gateway route/CircuitBreaker/fallback을 연결했다.
  - Closing 전용 Flyway 위치/이력 테이블과 V50 legacy `is_closed` backfill을 추가했다.
  - 미사용 `ClosingPeriod` 병렬 모델을 제거하고 Closing API/Batch의 #41 이후 Master Data JPA composition 누락을 보완했다.
- 검증:
  - Closing Core 59, API 8, Batch 12, Gateway 31, 총 110 tests와 Closing API/Batch bootJar가 통과했다.
  - Flyway baseline/migration, legacy Boolean backfill, JPA lock/version, API role gate, Gateway route 순서를 회귀 테스트로 확인했다.
  - 독립 리뷰에서 확인된 Closing 전용 Flyway history와 BOD 직전 선행일 검증을 보완했고, 재검토는 P0-P3 finding 없이 PASS했다.
- 리스크:
  - Journal의 일마감 거래 차단 연동, PostgreSQL 실DB migration, live Gateway/Auth/Discovery 검증은 후속 범위다.
## 2026-07-30 (Issue #229 development PostgreSQL contract)

- branch/worktree: `agent/229-dev-postgres` / `C:\tmp\account-229-dev-postgres`; base `origin/main@5fb9cb67`.
- self-contained PostgreSQL/pgAdmin을 명시 profile로 격리하고 16개 database별 owner role, post-bootstrap manifest healthcheck를 추가했다.
- external-dev 전용 Compose는 로컬 DB 없이 `DEV_DB_*` secret/env로 인증된 read-only probe를 제공한다.
- 중앙 dev datasource는 PostgreSQL/Flyway/JPA validate/SQL init off를 강제하고 H2/기본 자격증명 fallback을 제거한다.
- dummy env examples, ignore, runbook, Config Server policy tests를 추가했다.
- `:config-server:test --offline`, init `bash -n`, diff/conflict/link/security scans와 독립 리뷰가 통과했다.
- Docker/Compose provider와 cached image가 없어 live bootstrap/restart/idempotency는 미실행이며 #66의 통합 gate로 남긴다.
- `agent/229-dev-postgres` push와 Draft PR `#241` (`Refs #229`) 생성 완료; merge/Issue close 미실행.

## 2026-07-30 (Issue #42 Master Data 거래처 승인 UI/API)
- 요청 목표: 거래처 등록과 `REQUESTED` 변경요청의 승인·반려를 실제 Master Data API에 연결한다.
- 통합: PR `#234`가 source commit `c8f2b81a`, merge commit `57aa1729`로 `main`에 병합됐고 `Fixes #42`로 Issue가 닫혔다. 원격 source branch도 삭제됐다.
- 변경:
  - `/master-data/partner` 화면과 실제 Backend DTO 기반 `masterDataService`를 구현했다.
  - 기존 거래처 화면 DTO와 메뉴 경로를 정정하고, Gateway가 `/api/master-data/**`를 Master Data로 라우팅하게 했다.
  - `NEXT_PUBLIC_API_URL`을 실제 호출 기준으로 사용하고 로컬 기본값을 Gateway 8000으로 정렬했다.
  - trusted `X-Auth-User`/`X-Auth-Roles`, 승인 역할 게이트, 접수·승인 전 BUSINESS_PARTNER payload 검증을 추가했다.
  - 거래처 및 변경요청 Controller 계약과 Gateway route 정책 테스트를 추가·확장했다.
- 검증:
  - Master Data Core 13, API 9, Gateway 30, 총 52 tests와 API/Gateway bootJar가 통과했다.
  - 변경 프런트 ESLint, diff/conflict/legacy-path 정적 검사가 통과했다.
  - 전체 Next type check/build는 이번 변경과 무관한 기존 `PageHeader.breadcrumbs` 누락 2건에서 중단됐다.
  - 독립 재검토는 P0-P3 finding 없이 PASS했다. Docker CLI 부재로 Compose 실행 검증은 미실행했다.
- 리스크:
  - pending API의 서버측 필터·pagination·payload projection과 Gateway-only 서비스 노출 보장이 후속 보안/확장성 범위다.
  - 로컬 dev server 응답 지연으로 브라우저 시각 검증은 미완료다.
## 2026-07-30 (Issue #227 runtime parity audit)

- 목표: 모든 Gradle 실행 모듈과 frontend가 local에서는 직접 Gradle/JAR/NPM으로, dev/prod에서는 container/Compose와 PostgreSQL로 실행될 수 있는지 최신 main에서 재현 가능하게 감사한다.
- branch/worktree: `agent/227-runtime-parity-audit` / `C:\tmp\account-227-runtime-parity-audit`; audit snapshot `origin/main@c0fb871b`, push-gate rebase `origin/main@36a1be4f`.
- 변경:
  - `tools/runtime-smoke.ps1`에 Inventory, dry-run TaskContract, 실제 Packaging, library/aggregator `test+jar`, executable LocalJar, Frontend 모드를 추가했다.
  - LocalJar는 외부 Config/Eureka/Vault를 끄고 명시적 `jdbc:h2:mem:`을 강제하며 Spring `Started` marker가 있어야 성공으로 판정한다.
  - README와 local development guide를 실제 70-subproject/phantom `:app` 구조에 맞추고 `docs/runtime-execution-matrix.md`에 환경/모듈/컨테이너 gap을 기록했다.
- 검증:
  - executable 35: packaging 33 PASS, 1 NON_EXECUTABLE, 1 NON_EXECUTABLE_COMPILE_FAILED.
  - library/aggregator 34: 프로젝트별 `test+jar` 전부 PASS.
  - local JAR 35: 25 PASS, 8 FAIL, 2 BLOCKED_BY_PACKAGING. Batch는 job-disabled context만 검증했고 대표 업무 Job은 실행하지 않았다.
  - 최신 main에서 기존 실패 8개를 재패키징/재기동한 결과 Closing Batch가 PASS로 복구되어 현재는 26 PASS, 7 FAIL, 2 BLOCKED_BY_PACKAGING다.
  - frontend scripts/lockfile/npm은 확인했으나 dependency installation 미승인과 `node_modules` 부재로 build/start는 미실행했다. Docker CLI도 없어 image/Compose runtime은 미실행했다.
  - PowerShell parse, `git diff --check`, conflict marker 및 민감 host literal 검사가 통과했다.
- 후속:
  - 실패는 #73-#80, #89-#90; image는 #228, dev DB는 #229, prod overlay는 #230, Internal Audit/Auth 경계는 #231, root Compose는 #66에서 처리한다.
  - 공유 개발 PostgreSQL은 `DEV_DB_HOST`/JDBC 환경변수 override로만 사용하고 self-contained dev DB와 동시에 실행하지 않는다.
- 상태: `agent/227-runtime-parity-audit` push와 Draft PR `#242` (`Refs #227`) 생성 완료. merge/Issue close는 미실행.

## 2026-07-29 (Issue #40 Master Data API/Core/Batch split)
- 요청 목표: `master-data`를 실제 배포 가능한 `api`, `batch`와 재사용 library인 `core`로 물리 분리한다.
- 통합: PR `#158`이 merge commit `178a7eb1`로 `main`에 병합됐고 `Fixes #40`으로 Issue가 닫혔다.
- 변경:
  - Controller/DTO/API 실행점은 `master-data:api`, Batch orchestration/report/Job/Step은 `master-data:batch`로 이동했다.
  - core에는 application/domain/infrastructure와 Flyway migration을 표준 resources 경로로 유지했다.
  - API/Batch `bootJar`, Docker, IntelliJ Run Configuration, README와 실행 문서를 새 Gradle 경로에 맞췄다.
  - 일일 유효성 Job은 필수 `asOfDate`를 검증하고 실제 집계 규칙을 core pipeline에 위임한다.
  - Journal Ledger core의 빈 `:master-data` aggregator 의존을 제거했다.
- 검증:
  - JDK 17 컨테이너에서 Master Data core/API/Batch 6 tests와 Journal Ledger core 23 tests, 두 bootJar가 통과했다.
  - populated H2 Job 통합 테스트에서 활성/만료 행을 구분해 Step context의 네 활성 건수가 각각 1임을 검증했다.
  - Config Server 부재 시 기본 Batch는 fail-fast했고, 명시적인 로컬 H2 예외 플래그에서는 `masterDataValidityJob asOfDate=2026-07-29`가 `COMPLETED`로 종료됐다.
  - 경계/충돌 표식 검색과 `git diff --check`가 통과했다.
- 리스크:
  - PostgreSQL/Flyway 통합 및 대량 성능, Batch 결과 장기 보관/관제 출력 포트는 후속 검증·구현이 필요하다.
  - typed applier/version/lock production 흐름의 회귀 테스트는 현재 test source에 없어 별도 복원이 필요하다.

## 2026-07-08 (payable API/core command boundary)
- 요청 목표: 모듈 순차 점검 중 payable의 헥사고날/DDD 경계, API DTO/core command 분리, Batch 실행 경고 정리, 초보자 문서 최신화.
- 변경:
  - `payable:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했다.
  - `PurchaseInvoiceCommand`, `PaymentRunCommand`, `ExecutePaymentCommand`, `AdvancePaymentCommand`, `OffsetPayableCommand`를 core 유즈케이스 입력으로 추가했다.
  - `PurchaseController`, `PaymentController`, 요청 DTO를 `payable:api`로 이동하고 응답 DTO를 추가했다.
  - Batch 지급런 Tasklet이 `PaymentRunCommand`로 core 유즈케이스를 호출하도록 변경했다.
  - `PayableBatchJobRegistryConfiguration`을 추가해 Batch JobRegistry 조기 초기화 경고를 제거했다.
  - payable README/docs/process-flow/schema/local-run과 handoff/Gemini prompt를 갱신했다.
- 검증:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1` 성공.
  - `:payable:api:bootRun` local/H2 context smoke 성공.
  - `:payable:batch:bootRun` local/H2 context smoke 성공, JobRegistry 경고 미재현.
- 리스크:
  - PostgreSQL/Flyway와 실제 은행 지급 어댑터, 대량 지급런 성능은 별도 검증 필요.
## 2026-07-08 (expenditure-resolution API/core command boundary)
- 요청 목표: 모듈 순차 점검 중 expenditure-resolution의 헥사고날/DDD 경계, master-data 내부 참조, API DTO/core command 분리, 문서 최신화.
- 변경:
  - ExpenditureResolutionCommand, APPaymentCommand를 추가하고 core use case/service 입력을 command로 전환했다.
  - Controller/DTO/assembler를 expenditure-resolution:api로 이동하고 core Web/Validation 의존을 제거했다.
  - ExpenditureResolutionService는 MasterDataQueryPort와 TaxInvoiceRef 계약 메서드로 외부 참조를 검증한다.
  - Budget/Invoice와 예산 포트를 코드 기반으로 전환해 master-data 엔티티 JPA 연관을 제거했다.
  - API 통합 테스트를 api 테스트 소스로 이동하고 Batch JobRegistry 지연 등록 설정을 추가했다.
- 검증:
  - $compile 성공.
  - $verify 성공.
  - $apiRun 성공.
  - $batchRun 성공.
- 리스크:
  - PostgreSQL migration 및 실제 대량 approval Job은 별도 검증 필요.
## 2026-07-08 (tax API/core command boundary)
- 요청 목표: 모듈 순차 점검 중 tax의 헥사고날/DDD 경계, 취소 세금계산서 외부 참조 정책, 초보자 문서 최신화.
- 변경:
  - `tax:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했다.
  - `TaxInvoiceCommand`를 추가하고 `TaxInvoiceService`가 command 기반으로 생성/수정 유즈케이스를 처리하게 했다.
  - `APInvoiceController`, `TaxInvoiceRequestDto`, `TaxInvoiceDto`를 `tax:api`로 이동했다.
  - `TaxInvoiceRef`에 `purchase()`, `active()`, `usableForPurchaseSettlement()`를 추가하고 expenditure 검증에서 계약 메서드를 사용하게 했다.
  - tax 서비스/외부 조회 adapter 테스트와 docs/worklog/handoff/Gemini prompt를 갱신했다.
- 검증:
  - `.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1`
- `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
- `.\gradlew :tax:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1` 성공.
- 리스크:
  - PostgreSQL/Flyway 및 실제 대량 Job 실행은 이번 pass에서 미검증.
## 2026-07-07 (수정: asset-lease 감가상각 Batch 계산/반영 경계 분리)
- 수정 내용:
  - `FixedAssetDepreciationResult` 값 객체를 추가했다.
  - `FixedAsset.calculateDepreciation()`은 mutation 없는 batch preview 계산으로 추가하고, `depreciate()`는 단건 API 상태 전이 경로로 유지했다.
  - `DepreciationPipeline`이 엔티티를 변경하지 않고 결과 값 목록을 반환하도록 변경했다.
  - `AssetPersistencePort`/`AssetJdbcAdapter`가 결과 값 기준으로 상각누계액, 장부가액, 상태, 최종상각일을 JDBC bulk update로 한 번만 반영하도록 변경했다.
  - `AssetDepreciationBatchConfig`와 asset-lease 문서를 최신화했다.
  - 리스 월별 회계처리 API/유즈케이스도 `X-User-ID`를 받아 IFRS 16 월별 처리 이벤트의 actor로 기록하도록 보강했다.
- 검증:
  - `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1` 성공.
- 남은 리스크:
  - PostgreSQL/H2 실제 대량 Job 실행과 `updated_at = NOW()` 운영 DDL 정합성은 추가 확인이 필요하다.

## 2026-07-07 (수정: account-mart 담보 상세 DQ/LGD 연결)
- 수정 내용:
  - `OdsApartCollDetail`의 `@todo`를 실제 구현으로 전환하고 LGD 필수 입력값 검증 메서드를 추가했다.
  - `OdsApartCollDetailRepository` port, JPA repository, persistence adapter를 추가했다.
  - `CollateralDataQualityInspectionService`가 application service에서 상세 port 조회와 domain processor 호출을 조정하도록 추가했다.
  - `CollateralDataQualityProcessor`는 담보 마스터 평가액과 아파트 상세 필수값을 판단하는 순수 DQ 규칙으로 보강했다.
  - `CollateralDataQualityItemProcessor`는 batch adapter로 축소하고, `DataPopulator`는 부동산 담보 상세 seed를 함께 생성하도록 보강했다.
  - `V5__add_ods_apart_coll_detail.sql`과 account-mart 문서/Gemini prompt를 갱신했다.
- 검증:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1` 성공.
  - account-mart Java TODO 검색 결과 없음.
- 남은 리스크:
  - PostgreSQL Flyway 적용과 운영 대량 담보 상세 조회 성능은 아직 검증하지 않았다.

# CODEX WORKLOG

> Codex 에이전트의 전용 작업 이력 관리 문서입니다.
> 이전 작업 내역은 사용자 요청에 의해 초기화되었습니다.


## 2026-07-03 (수정: Loan 전표 포트 경계 및 로컬 실행 정리)
- 수정 내용:
  - `InterestAccrualService`의 journal-ledger 직접 의존을 제거하고 `LoanJournalPort` 전표 명령으로 전환했다.
  - `EIRAmortizationSchedule`, `LoanEvent`, `LoanAccrualLog`가 전표 엔티티 대신 전표 ID/전표번호 값 참조를 보관하도록 정리했다.
  - `V32__loan_accrual_journal_reference.sql`과 Flyway 테스트를 추가했다.
  - `LoanBatchJobRegistryConfiguration`을 추가해 Batch JobRegistry 조기 초기화 경고를 제거했다.
  - Loan README/docs/local-run/process-flow/schema 및 IntelliJ `.run` 설정을 local/H2 실행 기준으로 갱신했다.
- 검증:
  - `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks` 성공.
  - `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1` 성공.
  - `:loan:api:bootRun` local/H2 context smoke 성공, 로그 app name `loan-api` 확인.
  - `:loan:batch:bootRun` local/H2 context smoke 성공, JobRegistry 경고 미재현.
- 남은 리스크:
  - PostgreSQL migration 적용과 대량 대출 이자 발생 Job의 실제 데이터 성능은 아직 검증하지 않았다.
## 2026-06-18 (구조화: library형 업무 모듈 core/api/batch 분리)
- 사용자 요청:
  - `contracts`, `shared-kernel` 같은 공통 라이브러리는 제외하고, 기존 compile/test 중심 업무 모듈을 Spring Boot Gradle 실행 구조로 정리.
  - `api`/`batch`는 반드시 `core` 업무 로직을 참조하도록 구성.
- 수정 내용:
  - `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`을 각각 `core/api/batch` 하위 프로젝트로 분리했다.
  - 기존 루트 프로젝트 경로는 호환 alias로 유지해 기존 `project(':tax')` 같은 의존 경로가 바로 깨지지 않게 했다.
  - API/BATCH main class와 H2 local 설정을 추가했다.
  - local profile 외부 포트 어댑터를 추가해 외부 서버 없이 core 컨텍스트가 뜨도록 했다.
  - Gradle capability 충돌 방지를 위해 새 하위 프로젝트 group/archive 식별자를 고유화했다.
  - `tax:core`의 미사용 journal-ledger 직접 의존을 제거했다.
  - `expenditure-resolution`의 코드 기반 JPA 참조 매핑을 H2 기동 가능하게 보정했다.
  - 각 대상 모듈 README/local-run과 `docs/local-development.md`, `docs/WORKLOG.md`를 최신화했다.
- 검증:
  - `.\gradlew projects --console=plain` 성공.
  - 대상 15개 `core/api/batch:compileJava` 성공.
  - `:payable:core:test :receivable:core:test :reconciliation:core:test :tax:core:test :expenditure-resolution:core:test` 성공.
  - `:payable:api`, `:receivable:api`, `:reconciliation:api`, `:tax:api`, `:expenditure-resolution:api` bootRun 스모크 성공(`web-application-type=none`).
  - `:payable:batch`, `:receivable:batch`, `:reconciliation:batch`, `:tax:batch`, `:expenditure-resolution:batch` bootRun 성공.
  - `.\gradlew --stop` 후 프로젝트 Gradle/BootRun Java 프로세스가 남지 않음.
- 남은 확인사항:
  - PostgreSQL profile과 실제 master-data/journal/tax/asset 연동은 별도 검증 필요.
  - 새 Batch 모듈의 `jobRegistryBeanPostProcessor` 경고는 후속으로 deposit/reporting 패턴을 이식해 정리 가능.
  - `expenditure-resolution`의 master-data 엔티티 직접 JPA 연관은 장기적으로 port/code 기반 참조로 더 분리하는 것이 좋다.

## 2026-06-18 (수정: Batch JobRegistry 조기 초기화 경고 제거)
- Git 동기화:
  - `git fetch origin` 실행 후 `main...origin/main` 차이 `0 0` 확인.
- 수정 내용:
  - `deposit:batch`, `reporting:batch`에 JobRegistry 등록 설정을 추가했다.
  - Spring Batch 기본 `jobRegistryBeanPostProcessor`를 제거하고 `JobRegistrySmartInitializingSingleton`으로 Job 등록 시점을 모든 singleton 생성 이후로 옮겼다.
  - Deposit/Reporting local-run 문서와 전사 local-development 문서에 설정 의미를 설명했다.
- 검증:
  - Deposit Batch 로컬 profile bootRun 성공.
  - Reporting Batch 로컬 profile bootRun 성공.
  - 두 Batch 기동 로그에서 `jobRegistryBeanPostProcessor` 관련 BeanPostProcessorChecker 경고가 재현되지 않았다.
- 남은 리스크:
  - 운영/일반 profile에서는 기존 logstash 수집기 의존성이 유지된다.
  - H2/Flyway 버전 권고 경고는 로컬 H2 조합의 별도 경고로 남아 있다.

## 2026-06-17 (수정: local standalone LoadBalancer 자동 구성 비활성화)
- Git 동기화:
  - `git fetch origin` 실행 후 `main...origin/main` 차이 `0 0` 확인.
- 수정 내용:
  - 로컬 H2/메모리/로컬 어댑터 실행에서 discovery/Eureka를 끄는 명령에 `--spring.cloud.loadbalancer.enabled=false`를 추가했다.
  - Deposit, Loan, Closing, Asset-Lease, Reporting의 `.run` 설정과 README/local-run 문서를 같은 기준으로 정리했다.
  - `docs/local-development.md`에 단독 실행 시 LoadBalancer 자동 구성을 끄는 이유를 보강했다.
- 검증:
  - Deposit API/BATCH, Reporting API/BATCH 로컬 profile bootRun 스모크 성공.
  - Deposit API에서 Spring Cloud LoadBalancer BeanPostProcessor 경고가 재현되지 않음을 확인했다.
  - 수정한 `.run` XML 파싱 성공.
  - 활성 로컬 실행 문서와 `.run` 설정 기준 LoadBalancer 비활성화 인자 누락 없음.
- 남은 리스크:
  - Batch 컨텍스트의 Spring Batch `jobRegistry` BeanPostProcessor 경고는 별도 경고로 남아 있다.
  - 운영/일반 profile에서는 기존 logstash 수집기 의존성이 유지된다.

## 2026-06-17 (수정: Spring Cloud LoadBalancer Caffeine 캐시 반영)
- Git 동기화:
  - `git fetch origin` 실행 후 `main...origin/main` 차이 `0 0` 확인.
- 수정 내용:
  - Eureka/Gateway/OpenFeign을 직접 쓰는 실행 모듈에 `com.github.ben-manes.caffeine:caffeine` 의존성을 추가했다.
  - 대상 모듈: `account-mart:mart-api`, `account-mart:mart-batch`, `asset-lease`, `auth`, `deposit:api`, `ecl:ecl-api`, `ecl:ecl-batch`, `gateway`, `governance`, `journal-ledger:api`, `journal-ledger:core`, `loan:api`, `master-data`.
  - `docs/local-development.md`에 LoadBalancer/Eureka/Caffeine 역할과 로컬 실행 시 의미를 보강했다.
- 검증:
  - Spring Cloud 클라이언트를 직접 쓰는 build 파일의 Caffeine 누락 여부를 확인했다.
  - Caffeine 의존성을 추가한 전체 대상 모듈의 `compileJava`가 성공했다.
  - `deposit:api:compileJava` 성공.
  - `deposit:api:bootRun` 로컬 profile 스모크 성공.
  - 대표 기동 로그에서 Spring Cloud LoadBalancer 기본 캐시/Caffeine 권고 경고가 재현되지 않음을 확인했다.
- 남은 리스크:
  - Spring Cloud 내부 BeanPostProcessor 경고는 별도 성격으로 남아 있다.
  - 운영/일반 profile에서는 기존 logstash 수집기 의존성이 유지된다.

## 2026-06-15 (수정: local profile logstash 비활성화)
- Git 동기화:
  - `git fetch origin` 실행 후 `main...origin/main` 차이 `0 0` 확인.
- 수정 내용:
  - logstash appender가 있는 모든 `logback-spring.xml`에 `local`/`!local` profile 분기를 추가했다.
  - `local` profile에서는 console appender만 root logger에 연결하고 `LOGSTASH` appender를 생성하지 않도록 정리했다.
  - Deposit/Reporting IntelliJ `.run` 설정과 README/docs/local-run 명령에 `--spring.profiles.active=local`을 추가했다.
- 검증:
  - `deposit:api`, `deposit:batch`, `reporting:api`, `reporting:batch` bootRun 컨텍스트 스모크 성공.
  - 네 가지 bootRun 모두 logstash `localhost:5000` 연결 실패 경고 없이 종료.
  - 모든 `logback-spring.xml` XML 파싱 성공.
  - Deposit/Reporting `.run` XML 파싱 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기 기동이 필요하다.
  - Spring Cloud LoadBalancer 기본 캐시 경고는 별도 운영 튜닝 대상이다.

## 2026-06-12 (수정: standalone API/Batch 검수 결과 반영)
- Git 동기화:
  - 작업 시작 시 `main...origin/main` 차이 없음 확인.
- 수정 대상:
  - Gemini가 로컬 미추적 파일로 추가한 standalone Application 후보와, 이전 Codex 검수에서 실패로 기록한 `asset-lease`, `deposit:batch`, `reporting:api`, `reporting:batch`, `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`.
- 수정 내용:
  - `asset-lease` 중복 API/BATCH Application 후보 제거.
  - 단일 `java-library` 모듈인 `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`의 잘못된 Application 후보 제거.
  - `deposit:batch`를 Spring Boot Batch 컨텍스트 앱으로 정리하고 `bootJar` mainClass, Boot BOM, H2 runtime, JPA repository/entity scan을 추가.
  - `reporting:api`, `reporting:batch`를 Spring Boot 앱으로 정리하고 mainClass, H2 runtime, 좁은 component scan, Batch JPA scan을 추가.
  - `deposit` 로컬 단독 실행용 master-data/journal 포트 어댑터를 명시 속성(`account.deposit.local-adapters.enabled=true`) 기반으로 추가.
  - `reporting` memory 모드용 GL 잔액/전표 상세 조회 어댑터를 추가.
  - Deposit/Reporting IntelliJ `.run` 설정과 README/docs/local-run 문서를 최신화.
- 검증:
  - 대상 전체 `bootJar`/`compileJava`/`deposit:core:test`/`reporting:core:test` 성공.
  - `deposit:api`, `deposit:batch`, `reporting:api`, `reporting:batch` 개별 `bootRun` 컨텍스트 스모크 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 로컬 logstash appender가 `localhost:5000`에 연결을 시도해 수집기가 없으면 경고와 종료 지연이 발생한다.
  - deposit/reporting 로컬 memory 어댑터는 학습/기동 확인용이며 운영 데이터 검증을 대체하지 않는다.

## 2026-06-11 (검수: Gemini standalone API/Batch Application)
- Git 동기화:
  - `git fetch origin` 실행 후 `main...origin/main` 차이 `0 0` 확인.
- 검수 대상:
  - Gemini가 로컬 미추적 파일로 추가한 `asset-lease`, `deposit`, `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `reporting`, `tax`의 API/Batch Spring Boot Application 후보.
- 주요 결과:
  - `asset-lease:bootJar` 실패. 기존 `AssetLeaseApplication`에 더해 `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`이 추가되어 main class 후보가 3개가 됨.
  - `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`는 `spring-boot-starter-batch` 없이 `@EnableBatchProcessing`을 사용해 `compileJava` 실패.
  - `deposit:batch`는 dependency management에 Spring Boot BOM이 없어 batch/shared-kernel 의존성 버전을 해석하지 못해 `compileJava` 실패.
  - `reporting:api`, `reporting:batch`는 컴파일은 가능하지만 `java-library`라 `bootRun`/`bootJar` 태스크가 없음.
  - `deposit:api:bootJar`는 기존 추적 파일 `DepositApplication` 기준으로 성공.
- 검증:
  - 영향 모듈 `compileJava --continue` 실행.
  - 대표 boot/task 확인: `:asset-lease:bootJar`, `:deposit:api:bootJar`, `:reporting:api:tasks`, `:reporting:batch:tasks`, `:payable:tasks`, `:reconciliation:tasks`.
- 권고:
  - 단독 API/Batch 실행은 기존 `loan`, `closing`, `journal-ledger`, `account-mart`, `ecl`처럼 `core/api/batch` 하위 프로젝트와 Spring Boot 플러그인 적용이 필요하다.
  - 단일 `java-library` 모듈에는 Application 클래스만 추가하지 말고, 별도 실행 모듈이나 통합 호스트 앱 전략을 먼저 정해야 한다.
  - `scanBasePackages = "com.ho.account"`는 과도하게 넓으므로 각 실행 모듈의 소유 패키지와 필요한 adapter/core 패키지로 제한해야 한다.

## 2026-06-11 (문서 통합 9차: foundation/infra)
- Foundation library 문서:
  - `contracts/docs/local-run.md`, `shared-kernel/docs/local-run.md`를 추가하고 `README.md`/docs 인덱스에서 library 모듈 컴파일 검증 흐름을 연결했다.
- Master-Data/Governance 문서:
  - `master-data/docs/beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 SCD2 기준정보, 변경 요청 승인/반영, 포트/어댑터 흐름을 통합했다.
  - `governance/docs/beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 감사 로그, 승인, SOD, Auth 역할 반영 흐름을 통합했다.
- Auth/Infra 문서:
  - `auth/docs`, `config-server/docs`, `gateway/docs/README.md`, `gateway/docs/local-run.md`, `discovery/docs/README.md`, `discovery/docs/local-run.md`를 추가했다.
  - 깨진 `discovery/docs/concept.md` 원문은 `discovery/docs/archive/concept_legacy_corrupt_2026-06-11.md`로 이동해 보존하고, 새 `concept.md`를 현재 Eureka 흐름 기준으로 작성했다.
  - `config-repo/README.md`, `docs/README.md`, `docs/local-development.md`에 foundation/infra 실행 순서와 IntelliJ `.run` 설정을 반영했다.
- 실행 설정:
  - `.run/Config Server bootRun.run.xml`, `.run/Discovery bootRun.run.xml`, `.run/Auth bootRun.run.xml`, `.run/Master Data bootRun.run.xml`, `.run/Governance bootRun.run.xml`, `.run/Gateway bootRun.run.xml`, `.run/Foundation Library Compile.run.xml`, `.run/Foundation Infra Tests.run.xml`을 추가했다.
- 주석 최신화:
  - Auth 메모리 로그인 잠금 공유, Gateway token-version 검증, Master-Data 변경 반영/예약 반영, Governance 미지원 승인 대상 fail-closed 지점에 `@todo` 5건을 남겼다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 foundation/infra를 Done으로 전환하고 0~9차 완료 상태를 기록했다.
- 검증:
  - `.\gradlew :contracts:compileJava :shared-kernel:compileJava :master-data:test :governance:test :auth:test :gateway:test :discovery:test :config-server:assemble --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - Auth 로그인 실패/잠금은 운영 다중 인스턴스에서 Redis/DB 기반 공유 어댑터가 필요하다.
  - Gateway는 JWT 서명 검증 후 roleVersion을 전달하지만, 역할 변경 직후 기존 JWT 차단은 Auth token-version 검증 또는 캐시 정책과 연동해야 한다.
  - Master-Data 변경 요청 반영은 targetType별 실제 도메인 applier와 대량 예약 반영 chunk 처리가 필요하다.
  - Governance 승인 흐름은 미지원 masterType을 조용히 승인하지 않는 fail-closed 정책이 필요하다.

## 2026-06-11 (문서 통합 8차: reconciliation/reporting)
- Reconciliation 문서:
  - 기존 `reconciliation/docs/README.md`를 `reconciliation/docs/archive/README_legacy_index_2026-06-11.md`로 이동해 원문 보존.
  - `reconciliation/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대사 단위, 대사 규칙, 실행 이력, 차이 배정/해소, 외부 단계 집계, 조정 전표 흐름을 통합했다.
  - `reconciliation/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 모듈 기준의 Gradle 테스트/컴파일 안내로 교체했다.
- Reporting 문서:
  - `reporting/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `local-run.md`를 현재 `reporting:core`, `reporting:api`, `reporting:batch` 하위 모듈 구조와 감독보고/주석 마트 흐름 기준으로 보강했다.
  - `reporting/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 하위 모듈 기준의 Gradle 테스트/컴파일 안내로 교체했다.
- 실행 설정:
  - `.run/Reconciliation Module Tests.run.xml`, `.run/Reporting Module Tests.run.xml`을 추가했다.
- 주석 최신화:
  - `ReconciliationService`에 대사 규칙 물리 삭제와 조정 전표 멱등 키 개선 필요 지점 2건을 `@todo`로 남겼다.
  - `ReportingBatchAdapter`에 운영 Spring Batch Job/Step 전환 필요 지점 1건을 `@todo`로 남겼다.
  - `LocalRegulatoryFilingGatewayAdapter`에 랜덤 반려 시뮬레이션 설정화 필요 지점 1건을 `@todo`로 남겼다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 reconciliation/reporting을 Done으로 전환하고 다음 순서를 foundation/infra로 지정했다.
- 검증:
  - `.\gradlew :reconciliation:test :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - Reconciliation 규칙 삭제는 현재 물리 삭제라 과거 대사 이력 감사 관점에서 논리 비활성화 전환이 필요하다.
  - Reconciliation 자동 조정 전표 source document id는 시간 기반이라 장애 재시도 시 멱등 키 보강이 필요하다.
  - Reporting Batch Adapter는 운영 대량 배치 전환 시 Job/Step/JobParameter 구조가 필요하다.
  - Reporting 로컬 감독보고 게이트웨이의 랜덤 반려는 테스트 재현성을 위해 설정화가 필요하다.

## 2026-06-11 (문서 통합 7차: asset-lease/tax)
- Asset-Lease 문서:
  - 기존 `asset-lease/docs/README.md`를 `asset-lease/docs/archive/README_legacy_index_2026-06-11.md`로 이동해 원문 보존.
  - `asset-lease/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 고정자산/감가상각 Batch/IFRS 16 리스/이벤트/지급결의 포트 흐름을 통합했다.
  - `asset-lease/docs/api-spec.md`, `requirements.md`를 현재 API 필드와 구현 리스크 기준으로 보강했다.
  - `asset-lease/README.md`에서 standalone Boot 실행 방법과 IntelliJ 실행 순서를 갱신했다.
- Tax 문서:
  - 기존 `tax/docs/README.md`를 `tax/docs/archive/README_legacy_index_2026-06-11.md`로 이동해 원문 보존.
  - `tax/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 AP 세금계산서/금액 검증/논리 취소/외부 조회 포트 흐름을 통합했다.
  - `tax/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 모듈 기준의 Gradle 테스트/컴파일 안내로 교체했다.
- 실행 설정:
  - `.run/Asset Lease API bootRun.run.xml`, `.run/Asset Lease Tests.run.xml`, `.run/Tax Module Tests.run.xml`을 추가했다.
- 주석 최신화:
  - Asset-Lease 운영 개선 지점 4건을 `@todo`로 남겼다: Batch targetDate/파이프라인 분리 2건, 리스 실행자 감사 1건, 리스 계정 매핑 포트 분리 1건.
  - Tax 외부 조회 포트의 취소 증빙 정책 확정 필요 지점 1건을 `@todo`로 남겼다.
  - `LeaseContractRequest`의 사용하지 않는 master-data 엔티티 import를 제거했다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 asset-lease/tax를 Done으로 전환하고 다음 순서를 `reconciliation`, `reporting`으로 지정했다.
- 검증:
  - `.\gradlew :asset-lease:test :tax:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - asset-lease Batch는 `targetDate` JobParameter와 `DepreciationPipeline` 중심으로 추가 정리해야 한다.
  - 리스 계정 매핑과 tax 취소 증빙 외부 조회 정책은 후속 설계 결정이 필요하다.

## 2026-06-10 (문서 통합 6차: payable/receivable)
- Payable 문서:
  - 기존 `payable/docs/README.md`를 `payable/docs/archive/README_legacy_index_2026-06-10.md`로 이동해 원문 보존.
  - `payable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매입 인보이스/채무/지급 런/지급 실행/선급금/상계 흐름을 통합했다.
  - `payable/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 모듈 기준의 Gradle 테스트/컴파일 안내로 교체했다.
- Receivable 문서:
  - 기존 `receivable/docs/README.md`를 `receivable/docs/archive/README_legacy_index_2026-06-10.md`로 이동해 원문 보존.
  - `receivable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매출채권/수납/자동·수동 매칭/부분 매칭 흐름을 통합했다.
  - `receivable/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 모듈 기준의 Gradle 테스트/컴파일 안내로 교체했다.
- 실행 설정:
  - `.run/Payable Module Tests.run.xml`, `.run/Receivable Module Tests.run.xml`을 추가했다.
- 주석 최신화:
  - `PurchaseInvoiceId`의 깨진 한글 주석을 복구했다.
  - Payable 인바운드 DTO/Bean Validation 분리 필요 지점 4건은 `@todo`로 남겼다.
  - `CollectionController`에 수납/매칭 인바운드 어댑터 역할 설명을 추가했다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 payable/receivable을 Done으로 전환하고 다음 순서를 `asset-lease`, `tax`로 지정했다.
- 검증:
  - `.\gradlew :payable:test :receivable:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 실제 HTTP API smoke는 payable/receivable을 스캔하는 호스트 Boot 앱에서 별도 검증해야 한다.
  - Payable 컨트롤러 DTO 분리 4건은 후속 리팩토링 대상으로 남겼다.

## 2026-06-10 (문서 통합 5차: loan)
- Loan 문서:
  - 깨진 `loan/docs/README.md`를 `loan/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 원문 보존.
  - `loan/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대출 생성/실행/이연/EIR/재계산/일일 이자 Batch 흐름을 통합했다.
  - `loan/README.md`에 문서 읽기 순서와 Windows PowerShell 기준 Gradle 실행 예시를 추가했다.
  - `.run/Loan API bootRun.run.xml`, `.run/Loan Batch Context.run.xml`을 추가했다.
- 주석 최신화:
  - `DeferredItemType`, `DeferredItem`, `EIRAmortizationSchedule`, `RecalculationRun`, `LoanEvent`, `LoanDisbursal`, `LoanJdbcAdapter`의 깨진 한글 주석을 초보자용 설명으로 복구했다.
  - `EIRCalculator`에 이연 수수료/비용 부호 정책 보강 필요 지점을 `@todo`로 남겼다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 Loan을 Done으로 전환하고 다음 순서를 `payable`, `receivable`로 지정했다.
- 검증:
  - `.\gradlew :loan:core:test :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 실제 일일 이자 발생 Job은 ACTIVE 대출, 상각 스케줄 엔트리, master-data 계정 seed가 있는 환경에서 별도 실행 검증이 필요하다.

## 2026-06-10 (문서 통합 4차: closing)
- Closing 문서:
  - 깨진 `closing/docs/README.md`를 `closing/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 원문 보존.
  - `closing/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 결산 캘린더/태스크/게이트/잠금/재오픈/FX/ECL 배치 흐름을 통합했다.
  - `closing/README.md`에 문서 읽기 순서와 Windows PowerShell 기준 Gradle 실행 예시를 추가했다.
  - `.run/Closing API bootRun.run.xml`, `.run/Closing Batch Context.run.xml`을 추가했다.
- 주석 최신화:
  - `ClosingCalendar`, `ClosingTask`, `ClosingGate`, `ClosingPeriod`, `DailyClosingStatus`의 깨진 한글 주석을 초보자용 설명으로 복구했다.
  - `FxValuationBatchConfig`, `FxValuationService`에 운영 대량 처리와 부채 계정 차대변 판정 개선 필요 지점을 `@todo`로 남겼다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 Closing을 Done으로 전환하고 다음 순서를 `loan`으로 지정했다.
- 검증:
  - `.\gradlew :closing:core:test :closing:api:compileJava :closing:batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 실제 FX/ECL Job 실행은 환율, GL 잔액, `allowance_summary` seed가 준비된 환경에서 별도 확인해야 한다.

## 2026-06-10 (문서 통합 1차: 공통 + account-mart)
- 사용자 요청: 전체 docs를 모듈별로 순차 점검/통합/고도화하고, IntelliJ 로컬 실행과 Gradle 설정을 문서화.
- 공통 문서:
  - 깨진 `docs/beginner_guide.md`를 `docs/archive/beginner_guide_legacy_corrupt_2026-06-10.md`로 이동해 원문 보존.
  - 새 `docs/beginner_guide.md`를 현재 코드 구조 기준으로 재작성.
  - `docs/local-development.md`를 추가해 IntelliJ IDEA, JDK 17, Gradle JVM, Spring Boot 실행 클래스, 인프라 실행, 검증 명령을 정리.
  - `docs/module-documentation-sequence.md`를 추가해 모듈별 순차 진행표를 기록.
- account-mart:
  - `account-mart/docs/README.md`를 문서 인덱스로 추가.
  - root/account-mart/mart-api/mart-batch/mart-core README와 기존 docs를 보강해 실행 인자, demo profile, Job 목록, 재실행 체크, batch 성능 설정을 최신화.
  - IntelliJ 공유 Gradle 실행 설정 `.run/Account Mart API bootRun.run.xml`, `.run/Account Mart Batch Demo.run.xml` 추가.
- 검증:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 다음 순서:
  - `journal-ledger` 문서 통합 및 원장/JDBC bulk 실행 흐름 정리.

## 2026-06-10 (문서 통합 2차: ecl)
- ECL 문서:
  - `ecl/README.md` 실행 예시를 Windows PowerShell 기준으로 보정하고 IntelliJ 실행 기준을 추가했다.
  - `ecl/docs/README.md`, `ALLOWANCE_BEGINNER_GUIDE.md`, `BATCH_EXECUTION_GUIDE.md`에 account-mart snapshot 선행 조건, demo profile의 한계, batch 컨텍스트 기동 방법을 기록했다.
  - `ecl/ecl-api/README.md`, `ecl/ecl-batch/README.md`, `ecl/ecl-core/README.md`에 로컬 실행/검증 명령을 추가했다.
  - `.run/ECL API bootRun.run.xml`, `.run/ECL Batch Context.run.xml`을 추가했다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 ECL을 Done으로 전환하고 다음 순서를 `journal-ledger`로 지정했다.
- 검증:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - 실제 ECL 산출 Job은 snapshot/모델 마스터/계정 매핑 seed가 있는 환경에서 별도 실행 검증이 필요하다.

## 2026-06-10 (문서 통합 3차: journal-ledger)
- Journal Ledger 문서:
  - 소스 트리 하위 legacy README 3개를 `journal-ledger/docs/archive`로 이동해 보존했다.
  - `journal-ledger/docs/layer-guide.md`를 추가해 application/domain/adapter/infrastructure 책임과 출력 포트 경계를 현재 코드 기준으로 통합했다.
  - `journal-ledger/README.md`, `docs/README.md`, `docs/beginner-guide.md`, `docs/process-flow.md`에 로컬 실행, JDBC bulk 모드, 계층 가이드 링크를 보강했다.
  - `.run/Journal Ledger API bootRun.run.xml`, `.run/Journal Ledger API JDBC Bulk.run.xml`을 추가했다.
- 진행표:
  - `docs/module-documentation-sequence.md`에서 Journal Ledger를 Done으로 전환하고 다음 순서를 `closing`으로 지정했다.
- 검증:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon` 성공.
  - IntelliJ `.run` XML 파싱 성공.
  - 변경 문서 상대 링크 검사 성공.
  - `git diff --check` 성공(CRLF 안내만 출력).
- 남은 리스크:
  - JDBC bulk 운영 모드는 실제 PostgreSQL/MySQL 환경에서 배치 크기, 인덱스, 락 대기 부하 검증이 필요하다.

## 2026-06-10 (ECL·Journal 잔여 경계 및 문서 통합)
- ECL:
  - `EadCalculator`의 배열 결과와 레거시 하드코딩 LGD 오버로드를 `EadCalculationResult` 값 객체로 교체했다.
  - 필수 LGD/CCF 비율을 fail-closed 검증하고, 상품 CCF 누락 시 `AllowanceModelParams.defaultCcfRate` 정책을 사용하도록 변경했다.
  - `AllowanceModelParameterRepository`에서 Spring Data 상속을 제거하고 JPA 저장소/영속성 어댑터를 infrastructure로 분리했다.
  - 등급·상품·LGD·담보배분·거시시나리오·전이행렬 포트에서도 Spring Data/캐시 기술을 제거하고 전용 JPA 어댑터로 이동했다.
- Journal Ledger:
  - `UnsettledItemPersistencePort`와 JPA 어댑터를 추가해 `UnsettledService`의 Repository 직접 의존과 거래처별 인메모리 필터를 제거했다.
  - `JournalRuleQueryPort`와 어댑터를 추가하고 자동분개 규칙 차대변을 `JournalSide` 타입으로 제한했다.
  - 전기·잔액 서비스를 `JournalPersistencePort`, `LedgerEntryPersistencePort`, `LedgerBalancePersistencePort` 경계로 분리하고 GL/SL 조건 필터를 DB 조회로 이동했다.
  - `journal-ledger.ledger.persistence-mode=jdbc-bulk` 설정 기반 JDBC bulk 어댑터를 추가했다.
    - 엔트리는 `JdbcLedgerEntryBulkPersistenceAdapter`에서 batch insert로 저장한다.
    - GL 잔액은 DB 방언별 upsert로 저장한다.
    - SL 잔액은 거래처·부서가 `null`인 키도 처리하도록 bulk update 후 insert로 저장한다.
  - `LedgerService` 재집계 저장 경로를 일별 집계 후 bulk 포트 저장으로 변경했다.
  - JPA/JDBC가 같은 잔액 기간 키를 사용하도록 `YearMonthAttributeConverter`를 추가했다.
  - 구현 완료 후 남아 있던 `stub`, 향후 포트 분리, 인메모리 필터 안내 주석을 실제 구현 기준으로 최신화했다.
- 문서:
  - `ecl/docs/README.md`를 문서 진입점으로 추가하고 입문·업무 흐름·데이터 모델 문서를 상세화했다.
  - 누락되어 있던 `journal-ledger/docs/beginner-guide.md`, `process-flow.md`, `schema.md`를 추가하고 README 중복 설명을 상세 문서로 연결했다.
  - 완료 T47-T53을 `docs/todo_remediation_plan.md`에 기록했다.
- 검증:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :loan:core:test --console=plain --max-workers=1 --no-daemon` 성공. 첫 실행에서 누락된 `Collectors` import를 발견해 보정 후 재실행했다.
  - T53 구현 후 `.\gradlew :journal-ledger:core:test --console=plain --max-workers=1 --no-daemon` 성공. H2 기반 JDBC batch insert/upsert 집중 테스트를 포함한다.
  - 대상 코드의 오래된 TODO/stub/인메모리 필터/배열 결과 검색 0건, 문서 링크 누락 0건, 변경 범위 `git diff --check` 성공.
- 남은 리스크:
  - Journal JDBC bulk 경로는 H2 SQL 동작까지 검증했으며, 운영 DB 기준 배치 크기·인덱스·락 대기·동시 재집계 부하 검증이 필요하다.

## 2026-06-09 (잔여 TODO 최종 경계 통합)
- Java 소스의 잔여 `@todo`를 0건으로 정리하고, 완료 표기와 실제 코드가 어긋난 인증/지급/수금/전표 경계를 복구했다.
- Auth: `LoginAttemptPort`와 설정 기반 임시 잠금/감사 어댑터를 추가했다.
- Payable: `PaymentExecutionPort`, 멱등 실행 결과, 정확한 `payableId`, 실패/재시도 상태를 추가하고 master-data 내부 의존을 제거했다.
- Receivable: 참조번호 우선 자동 매칭 정책과 `CollectionAllocation` 잔액 이력을 추가했다.
- Journal/Unsettled: API DTO, 필수 actor, 미결 인바운드 포트, 반제 참조번호 멱등성과 감사 필드를 추가했다.
- 기존 변경인 Loan 출력 포트, Reconciliation Aggregate 통합, allowance JPA 소유권, Closing 초안 통제, Tax 논리 취소, Asset actor 전달을 함께 검증했다.
- 문서: 관련 모듈 README, `docs/todo_remediation_plan.md`, 통합 워크로그, Gemini 리뷰 프롬프트를 갱신했다.
- 검증:
  - `.\gradlew :auth:test :payable:test :receivable:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :asset-lease:test :tax:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :closing:batch:test :journal-ledger:core:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :reconciliation:test :ecl:ecl-core:test --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :journal-ledger:api:compileJava :loan:core:test :loan:api:compileJava --console=plain --max-workers=1 --no-daemon` 성공.
  - `.\gradlew :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon` 성공. 최초 실행에서 Flyway V2 중복과 통합 테스트 구매 actor 누락을 발견해 전용 V10 마이그레이션/테스트 계약으로 보정했다.
  - `.\gradlew :shared-kernel:compileJava :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test :ecl:ecl-api:compileJava --console=plain --max-workers=1 --no-daemon` 성공.
  - 최종 변경 모듈 통합 명령(68 tasks) 성공: Auth, Payable, Receivable, Asset, Tax, Closing, Journal API/Core, Reconciliation, Loan, Account-Mart, ECL.
- 남은 리스크:
  - Auth 기본 로그인 시도 어댑터는 인메모리이므로 다중 인스턴스 운영에서는 공유 저장 어댑터가 필요하다.
  - Payable의 로컬 지급 실행 어댑터는 운영 은행 API 어댑터로 교체해야 한다.
  - 신규 payable/receivable 컬럼과 allocation 테이블은 배포 환경의 통합 스키마 관리 정책에 맞춘 마이그레이션 확인이 필요하다.

### 📅 초기화
- 작업 이력 정리 및 초기화 완료.

## 2026-05-21 (Governance -> Auth 내부 API 토큰 보호)
- 사용자 요청: "커밋 진행하고 다음 작업 진행해".
- 선확인:
  - `git status --short --branch`: `main...origin/main [ahead 1]`, 사용자/Gemini로 보이는 기존 미커밋 변경은 범위에서 제외.
  - `docs/WORKLOG.md`, `auth/README.md`, `governance/README.md`, `governance/docs/README.md` 확인.
  - `auth/docs`는 존재하지 않음.
- 수정 내용:
  - Auth 내부 역할 할당 API에 `X-Internal-Auth-Token` 헤더 검증 추가.
  - `auth.internal-api.token` / `AUTH_INTERNAL_API_TOKEN` 설정 추가.
  - Governance Auth RestClient 역할 반영 호출에 내부 토큰 헤더 추가.
  - `governance.integrations.auth.internal-token` / `GOVERNANCE_AUTH_INTERNAL_TOKEN` 설정 추가.
  - Auth 컨트롤러 테스트와 Governance RestClient 테스트 보강.
  - 관련 README/docs/worklog/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Auth/Governance 테스트 성공.
- 남은 리스크:
  - 실제 환경에서 Auth/Governance 양쪽 내부 토큰 환경변수 값이 불일치하면 승인 반영 호출이 403으로 실패한다.
  - 장기적으로는 토큰 회전, mTLS, 내부망 ACL 같은 서비스 간 인증 강화 설계가 필요하다.

## 2026-05-21 (Reporting 감독보고 제출본 버전/정정/검증)
- 사용자 요청: "다음 작업 진행해줘".
- 선확인:
  - `docs/todo.md`: 14번 재무보고/공시/감독보고 영역 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md` 확인.
  - `rg -n "@todo|TODO|todo"` 결과 코드 내 미처리 TODO 문자열은 없음.
- 수정 내용:
  - `SubmitRegulatoryReportUseCase`, `RegulatoryReportSubmissionService` 추가.
  - `RegulatoryReportSubmission` 도메인 모델 추가. 제출 전 FINAL 스냅샷, 라인 중복/금액/계층, BS 총계, IS 순액 검증.
  - `StoreRegulatoryReportSubmissionPort`와 JPA/인메모리 어댑터 추가.
  - `RPT_REGULATORY_SUBMISSION` Flyway 마이그레이션 추가.
  - `POST /api/v1/reporting/submissions/regulatory` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Reporting core/api 테스트 성공.
- 남은 리스크:
  - 실제 감독기관 제출 전문/파일 전송 및 제출 결과 수신 상태 모델은 후속 구현 필요.
  - 주석 마트와 CF 라인 매핑은 후속 과제로 남음.

## 2026-05-21 (Reporting 주석 마트 생성/조회)
- 사용자 요청: "진행해 깃 동기화도".
- Git 동기화:
  - `git fetch origin` 실행.
  - `git rev-list --left-right --count origin/main...HEAD`: `0 3`.
  - `git push origin main` 성공. `d502fae`, `42ddcc8`, `cfe79a5`가 `origin/main`에 반영됨.
- 선확인:
  - `docs/todo.md`: 14.2 주석 마트 미완료 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/schema.md`, `reporting/docs/process-flow.md`, `reporting/docs/beginner-guide.md` 확인.
- 수정 내용:
  - `DisclosureNoteMartUseCase`, `DisclosureNoteMartService` 추가.
  - `DisclosureNoteMart`, `DisclosureNoteMartEntry` 도메인 모델 추가.
  - note 번호/라인 코드/라벨 기반으로 만기, 금리, 통화, 리스크 범주 분류.
  - `LoadDisclosureNoteMartPort`, `StoreDisclosureNoteMartPort`와 JPA/인메모리 어댑터 추가.
  - `RPT_DISCLOSURE_NOTE_MART` Flyway 마이그레이션 추가.
  - `POST /api/v1/reporting/disclosure-notes/generate`, `GET /api/v1/reporting/disclosure-notes` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - 첫 실행은 Gradle daemon stop으로 중단.
  - 동일 명령 재실행 성공.
- 남은 리스크:
  - 운영 수준의 공시 분류 정책은 SCD2 매핑 테이블로 분리 필요.
  - 전표/원천 이벤트 단위 drill-through는 후속 구현 필요.

## 2026-05-21 (Reporting 감독보고 매핑/제출)
- 사용자 요청: "진행해".
- 선확인:
  - `git status --short --branch`: `main...origin/main`, clean.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/todo.md` 확인.
  - `reporting/README.md`, `reporting/docs/README.md`, `reporting/docs/process-flow.md`, `reporting/docs/schema.md`, `reporting/docs/beginner-guide.md` 확인.
- 수정 내용:
  - `SubmitRegulatoryFilingUseCase`, `RegulatoryFilingService` 추가.
  - `RegulatoryReportMapping`, `RegulatoryFiling`, `RegulatoryFilingLine`, `RegulatoryFilingPackage`, `RegulatoryFilingReceipt` 도메인 모델 추가.
  - READY 제출본, 주석 마트, SCD2 감독보고 매핑을 조합해 제출 패키지를 생성.
  - `LocalRegulatoryFilingGatewayAdapter`로 로컬 접수 영수증 생성.
  - `RPT_REGULATORY_REPORT_MAPPING`, `RPT_REGULATORY_FILING` Flyway 마이그레이션 및 기본 FSS BS/IS 매핑 seed 추가.
  - JPA/인메모리 매핑 로더와 제출 이력 어댑터 추가.
  - `POST /api/v1/reporting/regulatory-filings/submit`, `GET /api/v1/reporting/regulatory-filings/latest` API 추가.
  - 서비스/API/JPA 테스트와 reporting 문서, `docs/todo.md`, 워크로그/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
- 결과:
  - Reporting core/api/batch 테스트 성공.
- 남은 리스크:
  - 실제 감독기관 전송 프로토콜, 인증, 반려 응답 수신은 후속 구현 필요.
  - 운영 서식 전체 필드 매핑과 버전별 검증 규칙은 추가 보강 필요.

## 2026-05-20 (Governance 승인 기반 Auth 역할 반영)
- 사용자 요청: "다음 작업 진행해줘".
- 선확인:
  - `git status --short --branch`: `main...origin/main` clean.
  - `docs/WORKLOG.md`, `governance/README.md`, `governance/docs/README.md`, `auth/README.md` 확인.
- 수정 내용:
  - Auth 내부 역할 할당 API 추가: `POST /api/auth/internal/users/{username}/role-assignments`.
  - `AuthUserRoleAssignmentUseCase`, `AuthUserRoleAssignmentService`, `AuthUserRoleAssignmentPersistencePort` 추가.
  - JPA/인메모리 역할 교체 어댑터 추가. JPA 모드는 기존 role assignments를 교체하고 `roleVersion`을 증가.
  - Governance `AUTH_USER_ROLE` 승인 apply 어댑터와 Auth RestClient 연동 추가.
  - Frontend `/admin/users` 승인 요청 payload에 `username`을 포함하고 `masterKey`를 이메일 기반으로 변경.
  - 관련 README/docs/worklog/Gemini 리뷰 프롬프트 갱신.
- 실행 명령:
  - `.\gradlew :auth:test :governance:test --console=plain --max-workers=1 --no-daemon -D"org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=384m"`
  - `npm run build` (workdir: `frontend`)
  - `git diff --check -- auth governance frontend docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md`
- 결과:
  - Auth/Governance 테스트 성공.
  - Frontend build 성공. 기존 `closing` unused variable warning은 남음.
  - 변경 범위 diff check 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 실제 MSA 환경에서 Governance -> Auth 내부 API 인증/네트워크 경로 smoke 필요.
  - 전체 `git diff --check`는 이번 범위 밖 미커밋 파일들의 trailing whitespace 때문에 실패.

## 2026-05-22 (DDD/헥사고날 업무 흐름 @todo 검수)
- 사용자 요청: "모듈들을 순차적으로 점검하면서 @todo를 남겨줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/todo.md` 확인.
  - `journal-ledger`, `payable`, `receivable`, `reconciliation`, `closing`, `reporting` 주요 README/docs 및 업무 흐름 코드 확인.
  - 이번 범위의 프론트엔드 변경은 없음.
- 수정 내용:
  - GL/SL/Journal Ledger: 전표 상태 기준 혼재, 대사 집계 필터 누락, 자동분개 감사자/통화/Rule DSL null 처리 위험 `@todo`를 확인.
  - P2P/AP: 하드코딩 계정, Mock 지급 실행, 지급-open item 매칭 정합성, 감사자 기본값 위험 `@todo`를 확인.
  - O2C/AR: 하드코딩 계정, 수납 자동매칭/부분매칭 잔액 처리 위험 `@todo`를 확인.
  - Reconciliation: 모델 이원화, JSON 정책, skeleton source snapshot, 대량 매칭 성능, 중복 매칭, 조정 정책 위험에 `@todo`를 추가/확인.
  - Closing: FX/ECL 하드코딩, 가상 장부환율, idempotency, 자동 승인/전기 통제 위험에 `@todo`를 추가.
  - Reporting: 로컬 감독보고 영수증 게이트웨이의 실제 연동 대체 필요성을 `@todo`로 추가.
  - `docs/WORKLOG.md`에 검수 결과와 남은 리스크 기록.
- 검증:
  - `rg -n "@todo" journal-ledger payable receivable reconciliation closing reporting`로 주석 위치 확인.
  - `git diff --check` 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 이번 작업은 검수 주석 추가이며 실제 업무 로직 보완은 후속 구현 필요.
  - 테스트는 동작 변경이 없는 주석/문서 변경 중심이라 아직 실행하지 않음.

## 2026-05-22 (결산 배치 재실행 정합성 및 대사 매칭 정합성 보강)
- 사용자 요청: "계속 진행해 줘".
- 선확인:
  - `git status --short --branch`: `main...origin/main`, 이전 검수 주석 변경 미커밋 상태 확인.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `closing/README.md`, `closing/docs/README.md`, `reconciliation/README.md`, `reconciliation/docs/README.md` 확인.
- 수정 내용:
  - `ClosingSlipNoFactory` 추가.
  - FX/ECL 배치 전표번호를 `System.currentTimeMillis()` 대신 기준일, 배치 ID, 계정 식별자 해시로 결정적으로 생성.
  - 배치 ID 파라미터가 없을 때 현재시각 대신 기준일(`yyyyMMdd`)을 기본값으로 사용.
  - `closing:batch` 테스트 의존성을 `org.springframework.batch:spring-batch-test`로 수정.
  - 전표번호 결정성/20자 길이 테스트 추가.
  - `AutomatedMatchingEngine`에서 한 번 매칭된 전표 라인을 재사용하지 않도록 변경.
  - 은행 출금과 전표 CREDIT 라인을 음수로 비교해 반대방향 금액 매칭을 방지.
  - `closing/README.md`, `reconciliation/docs/README.md`, `docs/WORKLOG.md` 갱신.
  - `GEMINI_REVIEW_PROMPT.md`를 이번 변경 범위로 갱신.
- 실행 명령:
  - `.\gradlew :closing:batch:test --console=plain`
  - `.\gradlew :reconciliation:test --console=plain`
- 결과:
  - Closing batch 테스트 성공.
  - Reconciliation 테스트 성공.
- 남은 리스크:
  - 대사 매칭 성능은 아직 O(n*m) 구조라 대량 처리를 위한 인덱싱/DB 후보 추출이 필요하다.
  - FX/ECL 통화/정책 하드코딩과 자동 승인/전기 통제는 후속 과제로 남아 있다.

## 2026-05-22 (Reconciliation 자동매칭 후보 인덱싱)
- 사용자 요청: "계속 진행해 줘".
- 선확인:
  - `reconciliation/README.md`, `reconciliation/docs/README.md`, `AutomatedMatchingEngine`, `AutomatedMatchingEngineTest` 확인.
- 수정 내용:
  - 전표 라인을 부호 반영 금액 기준 `TreeMap` 인덱스로 구성.
  - 은행 거래 금액의 허용오차 범위에 들어오는 전표 후보만 평가하도록 변경.
  - 후보 내부는 기존 전표 라인 입력 순서를 유지하도록 정렬.
  - 허용오차 내 입력 순서 보존 테스트 추가.
  - `reconciliation/docs/README.md`, `docs/WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- 실행 명령:
  - `.\gradlew :reconciliation:test --console=plain`
  - 변경 범위 `git diff --check`
- 결과:
  - Reconciliation 테스트 성공.
  - 변경 범위 diff check 성공(CRLF 경고만 출력).
- 남은 리스크:
  - 전체 `git diff --check`는 이번 범위 밖 프론트엔드 파일의 trailing whitespace로 실패한다.
  - 1억 건 이상 대사에는 인메모리 인덱스 외에 DB/배치 파티셔닝 기반 후보 조회와 청크 상태 저장이 필요하다.

## 2026-05-26 (account-mart/ecl 대손충당금 전용화 설계)
- 사용자 요청: "account-mart랑 ecl 새로 들여 왔는데 대손충당금 산출에 활용할지 분석하고, 대손충당금 산출만을 위한 변경 계획/설계와 업무기록을 남겨줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `docs/todo.md` 확인.
  - `account-mart/README.md`, `account-mart/docs/DATA_MART_SPEC.md`, `account-mart/docs/ETL_INTERFACE_SPEC.md` 확인.
  - `ecl/README.md`, `ecl/docs/process-flow.md`, `ecl/docs/CREDIT_DATA_MODEL_SPEC.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md` 확인.
  - `closing/README.md`, `closing/docs/README.md`, `closing/batch/.../EclProvisionService.java`, `EclProvisionBatchConfig.java` 확인.
- 분석 내용:
  - `account-mart`는 ODS 계좌/고객/상품/담보/잔액을 CDM으로 만드는 구조라 ECL 입력 스냅샷 마트로 재사용 가능.
  - `ecl`은 `CreditRiskCalculator.calculateEcl`, `ForwardLookingEclService`, `EclProcessor`, `StagingService`, `EadCrmProcessor`가 대손충당금 산출 핵심으로 재사용 가능.
  - RWA/SA/IRB/감독보고/집중도/스트레스 기능은 대손충당금 전용 흐름에서는 기본 실행 경로에서 제외해야 함.
  - 신규 모듈은 현재 루트 `settings.gradle`에 포함되지 않았고, `project(':common')`, `com.ho.account.shared.finance`, `credit-risk-service`, `risk-data-mart-service` 좌표가 현 저장소 구조와 맞지 않아 이관 작업이 필요.
- 수정 내용:
  - `docs/allowance-ecl-refocus-plan.md` 신규 작성.
  - `docs/WORKLOG.md`에 설계/분석 기록 추가.
- 실행 명령:
  - 문서 설계 작업이라 빌드/테스트는 실행하지 않음.
- 남은 리스크:
  - `closing`의 기존 ECL 배치는 하드코딩 대출채권 계정, KRW, 1% 산식, 환입 계정 임시 처리, 자동 승인/전기 통제가 남아 있다.
  - 다음 구현 단계에서는 `closing`이 ECL 산출 결과 summary를 포트로 조회하도록 바꾸고, `account-mart/ecl`은 ECL 전용 빌드 가능한 최소 경로부터 정리해야 한다.

## 2026-05-27 (Closing ECL summary 포트 연동)
- 사용자 요청: "진행해".
- 선확인:
  - `git status --short`: 직전 설계 문서 변경만 미커밋 상태 확인.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `closing/README.md`, `closing/docs/README.md`, `docs/allowance-ecl-refocus-plan.md` 확인.
  - `closing/batch/.../EclProvisionService.java`, `EclProvisionBatchConfig.java`, `ClosingSlipNoFactoryTest.java`, `JournalUseCase`, `GlAccountBalanceRepository` 확인.
- 수정 내용:
  - `closing:core`에 `EclAllowanceSummary`와 `EclAllowanceResultPort` 추가.
  - `closing:batch`에 `JdbcEclAllowanceResultAdapter` 추가. 기준일별 `allowance_summary` row를 읽어 ECL 충당 summary로 변환.
  - `EclProvisionService`에서 대출채권 계정/KRW/1% 고정 산식을 제거.
  - `EclProvisionService`가 ECL summary의 목표 충당금과 GL 기존 대손충당금 잔액 차이만 보충/환입 전표로 처리하도록 변경.
  - 환입 시 summary의 `reversalIncomeAccountCode`를 사용하도록 변경해 비용 계정 재사용을 제거.
  - `EclProvisionServiceTest` 추가: 보충, 환입, summary 없음 케이스 검증.
  - `docs/allowance-ecl-refocus-plan.md`, `closing/README.md`, `closing/docs/README.md`, `docs/WORKLOG.md` 갱신.
- 실행 명령:
  - `.\gradlew :closing:batch:test --console=plain`
  - 변경 범위 `git diff --check -- closing docs\allowance-ecl-refocus-plan.md docs\WORKLOG.md CODEX_WORKLOG.md GEMINI_REVIEW_PROMPT.md`
- 결과:
  - Closing batch 테스트 성공.
  - 변경 범위 diff check 성공(CRLF 경고만 출력).
- 남은 리스크:
  - `allowance_summary` 테이블 생성과 적재는 아직 `account-mart/ecl`에서 구현되지 않았다.
  - 결산 자동 승인/전기 통제는 기존 흐름을 유지한다. 후속으로 closing approval/reversal policy 포트 분리가 필요하다.

## 2026-05-27 (ECL allowance_summary 생성 경로 추가)
- 사용자 요청: "다음 작업 진행해줘".
- 선확인:
  - `settings.gradle`: `ecl`, `account-mart`가 아직 루트 Gradle 프로젝트에 포함되지 않음 확인.
  - `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `ecl/docs/CREDIT_DATA_MODEL_SPEC.md` 확인.
  - `CrRiskResult`, `CrAccount`, `MainReportingBatchConfig`, `EclProcessor`, `RwaProcessor`, 기존 Flyway migration 확인.
  - 현재 작업트리에 `FxValuationService`, `contracts`, `journal-ledger`, `reconciliation` 등 이번 범위 밖 미커밋 변경이 있음을 확인하고 미수정.
- 수정 내용:
  - `ecl:ecl-core`에 `AllowanceSummaryBuildPort`, `AllowanceSummaryService`, `AllowanceSummaryBuildResult` 추가.
  - `JdbcAllowanceSummaryPersistenceAdapter`를 추가해 완료된 `cr_risk_results`와 `allowance_account_mappings`를 조인하고 `allowance_summary`를 SQL bulk 집계로 재생성.
  - mapping 누락 시 기존 summary를 보존하고 실패하도록 서비스 검증 추가.
  - 같은 기준일 이전 run summary가 `closing`에 중복 조회되지 않도록 mapping 검증 통과 후 기준일 summary를 교체하도록 처리.
  - `AllowanceSummaryTasklet`, `allowanceSummaryStep`, `standaloneAllowanceSummaryJob` 추가.
  - `V3__add_allowance_summary.sql` 추가.
  - `h2-combined-cr.sql`에 summary 테이블, 샘플 계정 매핑, `cr_accounts.biz_unit_cd` 등 런타임 보강 컬럼 추가.
  - `AllowanceSummaryServiceTest` 추가.
  - `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `ecl/docs/CREDIT_DATA_MODEL_SPEC.md`, `docs/allowance-ecl-refocus-plan.md`, `docs/WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- 실행 명령:
  - `.\gradlew :closing:batch:test --console=plain`
  - `.\gradlew projects --console=plain`
  - 변경 대상 파일 `git diff --check`
  - 신규 ECL 파일 trailing whitespace `rg` 점검
- 결과:
  - Closing batch 테스트 성공.
  - Gradle 프로젝트 목록 확인 성공. `ecl`, `account-mart` 미편입 상태 확인.
  - 변경 대상 파일 diff check 성공(CRLF 경고만 출력).
  - 신규 ECL 파일 trailing whitespace 없음.
- 남은 리스크:
  - `ecl`/`account-mart`는 루트 Gradle 미편입 및 stale `com.ho.account.shared.finance`/`project(':common')` 좌표 때문에 신규 ECL 단위 테스트를 Gradle로 실행하지 못했다.
  - 전체 `git diff --check`는 이번 범위 밖 `FxValuationService.java`의 기존 trailing whitespace로 실패한다.
  - account-mart의 allowance exposure snapshot 생성과 ECL 전용 `allowanceEclJob` 분리는 다음 단계로 남아 있다.

## 2026-05-27 (account-mart/ecl 루트 Gradle 편입 및 wiring 복구)
- 사용자 요청: "다음단계 진행해줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `build.gradle`, `settings.gradle` 확인.
  - `ecl`, `account-mart`, 기존 `com.ho.account.shared.finance` 참조와 stale package import 확인.
  - 기존 미해결 변경(`FxValuationService`, `contracts`, `journal-ledger`, `reconciliation` 등)은 범위 밖으로 보고 미수정.
- 수정 내용:
  - 루트 `settings.gradle`에 `risk-common`, `account-mart:mart-core/api/batch`, `ecl:ecl-core/api/batch` 포함.
  - `risk-common` 모듈 추가: 기존 `com.ho.account.shared.finance` 엔티티/enum/event/DTO/예외/락 호환 타입 제공.
  - `ecl`/`account-mart` Gradle 의존성을 현재 프로젝트 좌표로 정리하고 Kafka 의존성 누락 보강.
  - `account-mart` batch/API/test의 예전 `domain.*.repository/entity` import를 현재 `application.port.out`/순수 도메인 위치로 수정.
  - KAP 외부등급, 조기경보, 계좌금리, 수익률곡선, 대사이력, 등급마스터 포트 adapter 추가.
  - JPA reader가 entity를 core processor에 직접 넘기지 않도록 도메인 projection 쿼리로 변경.
  - `IntegratedPositionProcessor`에 환율 기반 KRW `marketValue` 보강.
  - `RegulatoryDataTasklet` demo SQL을 현재 JPA entity 컬럼명에 맞게 수정.
  - ODS/GL 대사 집계를 계좌번호가 아니라 상품 GL 계정코드 기준으로 수행하고 MATCH/MISMATCH 이력을 남기도록 수정.
  - 테스트에서 Kafka 없이 batch를 검증할 수 있도록 `mart.batch.cdm-event.enabled=false` 지원 추가.
  - 관련 README, 설계 문서, worklog, Gemini handoff 갱신.
- 실행 명령:
  - `.\gradlew :ecl:ecl-api:compileJava --console=plain`
  - `.\gradlew :account-mart:mart-api:compileJava --console=plain`
  - `.\gradlew :risk-common:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava --console=plain`
  - `.\gradlew :account-mart:mart-batch:test --console=plain`
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceSummaryServiceTest :account-mart:mart-core:test --tests com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessorTest --console=plain`
  - `.\gradlew :account-mart:mart-core:compileTestJava :account-mart:mart-batch:compileTestJava :ecl:ecl-core:compileTestJava :ecl:ecl-batch:compileTestJava --console=plain`
  - `.\gradlew projects --console=plain`
- 결과:
  - 루트 Gradle 프로젝트 목록에 `risk-common`, `account-mart`, `ecl` 포함 확인.
  - 신규 편입 모듈 compile 성공.
  - `account-mart:mart-batch:test`, `AllowanceSummaryServiceTest`, `IntegratedPositionProcessorTest` 성공.
- 남은 리스크:
  - `risk-common`은 수입 모듈 호환을 위한 임시 성격이므로 장기적으로 `shared-kernel` 또는 allowance 전용 공통 모델로 이관 필요.
  - `account-mart`의 allowance exposure snapshot 생성과 `ecl`의 RWA 제외 `allowanceEclJob`은 아직 다음 단계.
  - `mart-batch` 테스트 종료 시 step-scope reader close 경고가 남아 있으나 테스트 결과는 성공.

## 2026-05-27 (allowance exposure snapshot 및 allowanceEclJob 구현)
- 사용자 요청: "다음 진행 해줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `account-mart/README.md`, `ecl/README.md`, `account-mart/docs/*.md`, `ecl/docs/*.md` 확인.
  - `IntegratedPositionEtlJobConfig`, `IntegratedRiskPosition`, `AllowanceSummaryService`, `MainReportingBatchConfig`, `IndividualStepJobConfig` 확인.
  - `contracts/...JournalQueryPort.java`, `journal-ledger/...JournalDetailRepository.java` 등 범위 밖 변경은 미수정.
- 수정 내용:
  - `account-mart:mart-core`에 `AllowanceExposureSnapshotBuildPort`, `AllowanceExposureSnapshotService`, `AllowanceExposureSnapshotBuildResult`, `JdbcAllowanceExposureSnapshotPersistenceAdapter` 추가.
  - `allowance_exposure_snapshots` JPA entity와 `mart-api` Flyway `V2__add_allowance_exposure_snapshot.sql`, `account-mart/db/schema-mart.sql` 정의 추가.
  - `IntegratedPositionEtlJobConfig`에 `allowanceExposureSnapshotStep`과 개별 `allowanceExposureSnapshotJob` 추가. 통합 Job은 `cdmLoadStep` 직후 스냅샷을 재생성.
  - `IntegratedPositionEtlJobTest`에서 스냅샷 건수, 미사용한도, 회계 노출 계정코드 검증 추가.
  - `ecl:ecl-core`에 `AllowanceExposureSyncService`/port/adapter를 추가해 `allowance_exposure_snapshots`에서 `cr_customers`, `cr_accounts`를 SQL bulk upsert.
  - `ecl:ecl-core`에 `AllowanceEclCompletionService`/port/adapter를 추가해 RWA를 타지 않는 ECL 전용 경로에서도 weighted ECL 결과를 `COMPLETED`로 확정.
  - `ecl:ecl-batch`에 `AllowanceExposureSyncTasklet`, `AllowanceEclCompletionTasklet`, `AllowanceEclBatchConfig`와 `allowanceEclJob` 추가.
  - `IndividualStepJobConfig`에 `standaloneAllowanceExposureSyncJob`, `standaloneAllowanceEclCompletionJob` 추가.
  - `JobRunner`가 `spring.batch.job.enabled=false`를 존중하고 `job.name`/`spring.batch.job.name`으로 실행 Job을 선택하도록 보강.
  - `StressSimulatorService`의 깨진 baseline 식별자 복구.
  - `account-mart/README.md`, `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `docs/allowance-ecl-refocus-plan.md`, `docs/WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- 실행 명령:
  - `.\gradlew :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest --console=plain`
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceEclCompletionServiceTest :ecl:ecl-batch:compileJava --console=plain`
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest --console=plain`
  - `.\gradlew :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest :ecl:ecl-core:test --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceEclCompletionServiceTest :ecl:ecl-batch:compileJava --console=plain`
- 결과:
  - account-mart 스냅샷 서비스 테스트 및 통합 ETL 테스트 성공.
  - ECL exposure sync/completion 서비스 테스트 성공.
  - `ecl:ecl-batch:compileJava` 성공.
  - `JobRunner` 수정 후 `CreditRiskBatchIntegrationTest`는 컨텍스트 로딩은 통과했으나, 기존 Batch metadata table 미초기화(`BATCH_JOB_INSTANCE` 없음)로 job launch 단계에서 실패.
- 남은 리스크:
  - `JdbcAllowanceExposureSyncAdapter`의 `INSERT ... ON CONFLICT`는 PostgreSQL 기준 bulk upsert다. H2에서 `allowanceEclJob` end-to-end 테스트를 돌리려면 H2 호환 upsert 또는 테스트용 스키마 보강이 필요하다.
  - 기존 ECL 통합 테스트는 Batch metadata 초기화 정리가 필요하다.
  - RWA/감독보고 코드는 legacy 경로에 보관되어 있으므로 기본 운영 Job 선택 정책을 배포 설정에서 명확히 해야 한다.

## 2026-05-28 (ECL 실행 기본값 및 IFRS 9 문서/설정 정리)
- 사용자 요청: "다음 작업 진행 해줘 그리고 제미나이가 risk 관련된 부분을 삭제 중이고 또 이 시스템은 재무모듈이야 ifrs 위주로만 하구".
- 선확인:
  - `Agents.md`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `ecl/ecl-batch/src/main/resources/application.yml` 확인.
  - 현재 워킹트리에 Gemini/사용자 작업으로 보이는 `com/risk` 삭제 및 `com/ho/account` 추가/이관 변경이 대량 존재함을 확인하고 되돌리지 않음.
- 수정 내용:
  - `JobRunner` 기본 Job을 `allowanceEclJob`으로 변경하고 실행 로그/unknown job 오류 문구를 IFRS 9 대손충당금 기준으로 정리.
  - `JdbcAllowanceExposureSyncAdapter`에 DB 제품명 감지와 H2 `MERGE INTO ... KEY` upsert SQL을 추가해 PostgreSQL/H2를 모두 지원.
  - `JdbcAllowanceExposureSyncAdapterTest` 추가: H2에서 snapshot count, customer/account merge, notional amount, 재실행 업데이트 멱등성 검증.
  - `ecl/ecl-batch/src/main/resources/application.yml`의 애플리케이션명, H2 DB명, 로그 패키지, PostgreSQL 예시 DB/계정을 allowance/IFRS 9 기준으로 변경.
  - `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`를 `allowance_exposure_snapshots -> allowanceEclJob -> allowance_summary -> closing` 중심으로 재작성하고 규제자본/감독보고/집중도 분석을 표준 경로에서 제외.
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md` 갱신.
- 실행 명령:
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.infrastructure.adapter.persistence.JdbcAllowanceExposureSyncAdapterTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest :ecl:ecl-batch:compileJava --console=plain`
  - `.\gradlew :ecl:ecl-core:test --tests com.ho.account.ecl.core.infrastructure.adapter.persistence.JdbcAllowanceExposureSyncAdapterTest --tests com.ho.account.ecl.core.application.service.allowance.AllowanceExposureSyncServiceTest :ecl:ecl-batch:compileJava --rerun-tasks --console=plain`
  - `git diff --check -- ecl\README.md ecl\docs\BATCH_EXECUTION_FLOW.md ecl\ecl-batch\src\main\resources\application.yml ecl\ecl-batch\src\main\java\com\ho\account\ecl\batch\job\JobRunner.java ecl\ecl-core\src\main\java\com\ho\account\ecl\core\infrastructure\adapter\persistence\JdbcAllowanceExposureSyncAdapter.java`
  - `rg -n "[ \t]+$" ...`로 변경/신규 파일 trailing whitespace 점검
- 결과:
  - ECL core 대상 테스트와 ecl-batch compile 성공.
  - 강제 재실행(`--rerun-tasks`) 성공.
  - 변경 대상 diff check 성공(CRLF 변환 경고만 출력).
  - 변경/신규 파일 trailing whitespace 없음.
- 남은 리스크:
  - 기존 ECL 통합 테스트의 Batch metadata table 초기화 문제는 후속 작업으로 남아 있다.
  - 코드 내부 일부 레거시 클래스명은 Gemini의 risk 제거 작업 완료 후 호출부/문서 재점검이 필요하다.
  - PostgreSQL 운영 DB명/계정 변경은 실제 배포 secret과 맞춰 확인해야 한다.

## 2026-05-28 (allowanceEclJob 통합 테스트 복구 및 문서/배포 참조 정리)
- 사용자 요청: "작업하던거 진행해줘".
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `ecl/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, `account-mart/README.md` 확인.
  - 워킹트리에 Gemini/사용자 작업으로 보이는 risk -> allowance 명칭 전환 변경이 다수 있음을 확인하고 되돌리지 않음.
- 수정 내용:
  - `CreditRiskBatchIntegrationTest`를 `allowanceEclJob` end-to-end 검증으로 재구성.
  - H2 테스트에서 Spring Batch metadata, `allowance_exposure_snapshots`, `allowance_account_mappings`, `allowance_summary` fixture를 생성하고 snapshot -> ECL -> summary 생성을 검증.
  - `application-test.yml`에서 Flyway/Vault/Discovery를 비활성화해 외부 인프라 없이 테스트되도록 조정.
  - `BatchInfrastructureConfig`의 transaction manager를 `JpaTransactionManager`로 바꿔 JPA writer가 step transaction에서 실제 flush/commit되도록 수정.
  - `JpaCrRiskResultRepository` ID 타입을 `CrRiskResultId`로 수정하고 `CrRiskResultPersistenceAdapter.saveAll`에서 chunk 저장 후 flush.
  - `JdbcAllowanceSummaryPersistenceAdapter` summary insert SQL을 H2/PostgreSQL 호환 derived-table `INSERT INTO ... SELECT` 형태로 변경.
  - `JobRunner`가 CLI `runId`, `modelVersion`을 JobParameters로 전달하도록 보강.
  - `ecl/Dockerfile`을 현재 `:ecl:ecl-api:bootJar` 경로와 `shared-kernel`/`ecl` 모듈 구조에 맞게 갱신.
  - `ecl/ecl-batch/README.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`, 루트 `README.md`, `account-mart/README.md`의 stale credit/risk 실행 참조를 allowance 기준으로 정리.
- 실행 명령:
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest --console=plain`
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.CreditRiskBatchIntegrationTest :ecl:ecl-api:bootJar --console=plain`
- 결과:
  - `allowanceEclJob` 통합 테스트가 통과했고, 로그에서 `cr_risk_results` 1건 완료 및 `allowance_summary` 1건 생성 확인.
  - `:ecl:ecl-api:bootJar` 성공.
- 남은 리스크:
  - Docker 이미지 빌드는 실행하지 않았고, Dockerfile 대상 Gradle bootJar까지만 검증했다.
  - PostgreSQL 운영 DB/secret 값은 실제 배포 환경과 별도 대조가 필요하다.
  - legacy RWA/감독보고 코드는 표준 `allowanceEclJob` 외부에 남아 있어 운영 실행 Job 선택을 계속 명시해야 한다.

## 2026-05-28 (IFRS 9 대손충당금 전용 account-mart/ecl 정리)
- 사용자 요청: IFRS 9 대손충당금 전용으로 정리.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `GEMINI_REVIEW_PROMPT.md`, 루트 README, `docs/todo.md` 확인.
  - `account-mart`, `ecl`, `shared-kernel`의 allowance 전환 상태와 검색 결과를 확인.
- 수정 내용:
  - `shared-kernel`, `account-mart`, `ecl`의 CDM 입력 모델을 `AllowanceInputPosition` / `allowance_input_positions` 기준으로 정리.
  - ECL 산출 결과 모델을 `AllowanceEclResult` / `allowance_ecl_results` 기준으로 정리.
  - 모델 파라미터 저장을 `AllowanceModelParameter` / `allowance_model_parameters` 기준으로 정리.
  - allowance 범위 밖 컨트롤러, 서비스, 배치 설정, processor, 테스트, 샘플 DB 파일 제거.
  - README, docs, HTTP 샘플, Docker/run 스크립트를 IFRS 9 대손충당금 경로 기준으로 갱신.
  - `IntegratedPositionEtlJobTest`에 deterministic DEMO fixture를 추가해 ODS -> CDM -> allowance snapshot 경로 검증.
  - `GEMINI_REVIEW_PROMPT.md`, 루트 `README.md`, `docs/todo.md`, `docs/WORKLOG.md`, `CODEX_WORKLOG.md` 최신 항목 갱신.
- 실행 명령:
  - `.\gradlew :shared-kernel:compileJava :ecl:ecl-core:compileJava :ecl:ecl-api:compileJava :ecl:ecl-batch:compileJava :account-mart:mart-core:compileJava :account-mart:mart-api:compileJava :account-mart:mart-batch:compileJava --console=plain`
  - `.\gradlew :ecl:ecl-core:testClasses :ecl:ecl-batch:testClasses :account-mart:mart-core:testClasses :account-mart:mart-batch:testClasses --console=plain`
  - `.\gradlew :ecl:ecl-batch:test --tests com.ho.account.ecl.batch.AllowanceEclBatchIntegrationTest :account-mart:mart-batch:test --tests com.ho.account.mart.batch.job.ods.IntegratedPositionEtlJobTest :account-mart:mart-core:test --tests com.ho.account.mart.core.application.service.allowance.AllowanceExposureSnapshotServiceTest --console=plain`
  - 대상 모듈과 활성 문서의 비-allowance 실행 경로 표현 검색
- 결과:
  - compile/testClasses/targeted integration test 모두 성공.
  - 대상 모듈과 활성 핸드오프 문서에서 비-allowance 실행 경로 표현 검색 결과 없음.
- 남은 확인사항:
  - Docker 이미지 빌드는 실행하지 않았다.
  - `mart-batch` 테스트 종료 시 일부 step-scope reader close 경고가 출력되지만 테스트 결과는 성공이다.

## 2026-05-29 (IFRS 9 대손충당금 단독 서비스 런북 추가)
- 사용자 요청: 대손충당금 모듈만 먼저 서비스하는 방법을 모듈 docs에 남기기.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `ecl/README.md`, `ecl/ecl-api/README.md`, `ecl/ecl-batch/README.md`, `ecl/docs/*.md` 확인.
  - `ecl-api`/`ecl-batch` 설정, `JobRunner`, `AllowanceEclBatchConfig`, snapshot sync adapter, account-mart snapshot DDL을 확인.
- 수정 내용:
  - `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md` 추가.
  - 최소 구성: PostgreSQL, `ecl-api`, `ecl-batch`, `allowance_exposure_snapshots` 공급 계약으로 정리.
  - ECL migration SQL, snapshot DDL, 기준월 partition, 모델 마스터, 회계 계정 매핑, snapshot 최소 시드 예시 추가.
  - API 실행, 배치 CLI 실행, 개별 재실행, 검증 SQL, 자주 막히는 지점을 문서화.
  - `ecl/README.md`, `ecl/ecl-batch/README.md`, `ecl/docs/SERVICE_ONBOARDING.md`, `ecl/docs/BATCH_EXECUTION_GUIDE.md`, `ecl/docs/ALLOWANCE_DATA_MODEL_SPEC.md` 갱신.
- 실행 명령:
  - 대손충당금 단독 서비스 문서의 비-allowance 실행 경로 표현 검색
  - `git diff --check -- ecl\docs\ALLOWANCE_SERVICE_RUNBOOK.md ecl\README.md ecl\ecl-batch\README.md ecl\docs\SERVICE_ONBOARDING.md ecl\docs\BATCH_EXECUTION_GUIDE.md ecl\docs\ALLOWANCE_DATA_MODEL_SPEC.md`
- 결과:
  - 대손충당금 단독 서비스 문서에서 비-allowance 실행 경로 표현 검색 결과 없음.
  - 문서 diff check 성공(CRLF 변환 경고만 출력).
- 남은 확인사항:
  - 문서 변경만 수행했으므로 애플리케이션 기동이나 Docker 이미지 빌드는 실행하지 않았다.

## 2026-05-29 (@todo 정리 계획 및 1단계 T01 수정)
- 사용자 요청: `@todo`를 모두 검색하고, 수정 계획을 파일로 만든 뒤 단계별로 진행.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md` 확인.
  - `rg -n "@todo|TODO:" . -g "!**/build/**" -g "!**/.gradle/**" -g "!**/node_modules/**" -g "!**/.git/**"`로 코드 TODO와 문서 기록을 분리 확인.
  - 관련 모듈 README/docs 및 TODO 위치 주변 코드를 확인.
- 수정 내용:
  - `docs/todo_remediation_plan.md` 추가: 코드 TODO 40건을 6개 단계로 분류하고 파일/라인/수정 방향/검증 명령을 기록.
  - T01 처리: `IntegratedPositionProcessor`의 연체일수 기반 staging 분기를 제거하고 `OdsAccountLedger.determineStaging()` 도메인 메서드로 이동.
  - `OdsAccountLedgerTest` 추가: null, 29, 30, 89, 90일 경계값 검증.
  - T02 처리: `EclProcessor`의 잔존만기 계산을 `CrAccount.resolveMaturityYears()`로 이동하고, `AllowanceCalculationService`도 같은 도메인 메서드를 사용하도록 정리.
  - `CrAccountTest` 추가: 만기일 없음, 최소 1년, 일수 기반 연환산 경계 검증.
  - 기존 `AllowanceCalculationServiceTest`는 도메인 메서드가 산출한 잔존만기 값을 기준으로 stub 하도록 보정.
  - T03-T06 처리: `JournalRuleEngine`이 이벤트 데이터에서 actor/currency를 해석하고, 라인은 entry audit actor를 상속하도록 변경.
  - DSL 값 조회를 내부 typed result로 바꿔 required 누락 값은 명확한 예외를 반환하고 optional 누락 값은 의도적으로 skip 되도록 정리.
  - `JournalRuleEngineTest`에 actor/currency 전파와 DSL missing-value 예외 검증 추가.
  - 계획 문서에 Phase 1(T01-T06) 완료 상태와 현재 코드 TODO 34건을 반영.
- 실행 명령:
  - `.\gradlew :account-mart:mart-core:test --console=plain`
  - `rg -n "@todo|TODO:" account-mart/mart-core/src/main/java account-mart/mart-core/src/test/java -g "*.java"`
  - `.\gradlew :ecl:ecl-core:test --console=plain`
  - `.\gradlew :ecl:ecl-batch:test --console=plain`
  - `rg -n "@todo|TODO:" ecl/ecl-batch/src/main/java ecl/ecl-core/src/main/java -g "*.java"`
  - `.\gradlew :journal-ledger:core:test --console=plain`
  - `rg -n "@todo|TODO:" journal-ledger/core/src/main/java journal-ledger/core/src/test/java -g "*.java"`
- 결과:
  - `account-mart:mart-core:test` 성공.
  - `account-mart/mart-core` 코드 TODO 검색 결과 없음.
  - `ecl:ecl-core:test`, `ecl:ecl-batch:test` 성공.
  - ECL 코드 TODO 검색 결과 `JdbcAllowanceSummaryPersistenceAdapter`의 T39만 남음.
  - `journal-ledger:core:test` 성공.
  - `journal-ledger/core` 코드 TODO 검색 결과 `JournalDetailRepository`의 T40만 남음.
- 남은 확인사항:
  - 전체 TODO 재검색은 출력이 많아 중간에 timeout 되었지만, account-mart 범위의 T01 제거는 확인했다.
  - 다음 단계는 Phase 2의 policy/account-mapping 항목부터 진행하면 된다.

## 2026-05-29 (@todo Phase 2 payable T11-T15 수정)
- 사용자 요청: 다음 단계 진행.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `payable/README.md`, `payable/docs/README.md` 확인.
  - `payable/docs/README.md`가 가리키는 상세 docs 3개는 현재 파일이 없어 README 범위까지만 확인.
  - `PurchaseService`, `PaymentService`, payable 테스트 구조, 관련 port/contract 확인.
- 수정 내용:
  - `PayableAccountMappingPort` 추가: 매입 인식, 지급 실행, 선급금, 선급금 상계 계정 매핑을 application port로 분리.
  - `ConfiguredPayableAccountMappingAdapter` 추가: 기본 계정 코드를 Spring property로 설정 가능하게 하고 기존 기본값은 adapter에 격리.
  - `PurchaseService`의 `createdBy` 기본 `SYSTEM` 대입을 제거하고 요청 actor가 없으면 저장 전에 실패하도록 변경.
  - `PurchaseService`/`PaymentService`의 AP, 비용, VAT, 현금, 선급금 계정 코드를 account mapping port로 대체.
  - 지급 실행 전표는 지급런의 `createdBy`를 actor로 전파하도록 보강.
  - `PurchaseServiceTest`, `PaymentServiceTest` 추가.
  - 계획 문서에 T11-T15 완료 상태와 현재 코드 TODO 29건을 반영.
- 실행 명령:
  - `.\gradlew :payable:test --console=plain`
  - `rg -n "@todo|TODO:" payable/src/main/java payable/src/test/java -g "*.java"`
  - `(rg -n "@todo|TODO:" closing reporting deposit ecl reconciliation receivable journal-ledger payable account-mart -g "*.java" -g "!**/build/**" | Measure-Object).Count`
- 결과:
  - `payable:test` 성공.
  - payable 범위에는 지급 gateway/mock(T19)와 open-item 매칭(T20) TODO만 남음.
  - 전체 코드 TODO count는 29건.
- 남은 확인사항:
  - 다음 단계는 같은 패턴으로 `receivable` T16-T18 계정 매핑 분리부터 진행하는 것이 가장 작다.

## 2026-05-29 (@todo Phase 2 receivable T16-T18 수정)
- 사용자 요청: 다음 단계 진행 및 git 동기화.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `receivable/README.md`, `receivable/docs/README.md` 확인.
  - `receivable/docs/README.md`가 가리키는 상세 docs 3개는 현재 파일이 없어 README 범위까지만 확인.
  - `git fetch origin` 성공, 작업 시작 시 `main...origin/main` ahead/behind 없음 확인.
  - `SalesService`, `CollectionService`, receivable 도메인/port/test 구조 확인.
- 수정 내용:
  - `ReceivableAccountMappingPort` 추가: 매출 인식, 수납 인식, 수납 매칭 계정 매핑을 application port로 분리.
  - `ConfiguredReceivableAccountMappingAdapter` 추가: AR, 매출, output VAT, 현금, AR clearing 기본 계정 코드를 property 기반으로 격리.
  - `SalesService`의 AR/매출/VAT 계정 코드 하드코딩을 account mapping port로 대체.
  - `CollectionService`의 수납 인식 cash/clearing 및 매칭 clearing/AR 계정 코드 하드코딩을 account mapping port로 대체.
  - `SalesServiceTest`, `CollectionServiceTest` 추가.
  - 계획 문서에 T16-T18 완료 상태와 현재 코드 TODO 26건을 반영.
- 실행 명령:
  - `git fetch origin`
  - `.\gradlew :receivable:test --console=plain`
  - `.\gradlew :payable:test :receivable:test --console=plain`
  - `rg -n "@todo|TODO:" receivable/src/main/java receivable/src/test/java -g "*.java"`
  - `(rg -n "@todo|TODO:" closing reporting deposit ecl reconciliation receivable journal-ledger payable account-mart -g "*.java" -g "!**/build/**" | Measure-Object).Count`
- 결과:
  - `receivable:test` 성공.
  - `payable:test :receivable:test` 성공.
  - receivable 범위에는 matching policy(T21)와 partial residual(T22) TODO만 남음.
  - 전체 코드 TODO count는 26건.
- 남은 확인사항:
  - `docs/msa_roadmap.md` 변경이 별도로 감지되어 이번 커밋 대상에서는 제외한다.
  - 다음 단계는 Phase 2 잔여 T07-T10(deposit/closing 정책 경계) 또는 Phase 3 matching 항목으로 이어갈 수 있다.

## 2026-05-29 (@todo Phase 2 deposit T07 수정)
- 사용자 요청: 작업하던 TODO 계속 진행 및 완료 여부 확인.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md` 확인.
  - `deposit` 루트 README 및 `deposit/docs/*.md`는 현재 없음.
  - `DepositService`, `OpenAccountUseCase`, `DepositAccount`, deposit Gradle 설정 확인.
- 수정 내용:
  - `DepositAccountMappingPort` 추가: 초기입금 전표의 현금/예금부채 계정 매핑을 application port로 분리.
  - `ConfiguredDepositAccountMappingAdapter` 추가: 현금 계정 및 예금부채 계정을 Spring property로 설정 가능하게 구성.
  - `DepositService`가 양수 초기입금 계좌 개설 시 계좌 저장 후 `JournalPostingPort`로 초기입금 전표를 생성하도록 변경.
  - 기존 흐름에서 초기입금 전에 `DepositStatus.ACTIVE`가 설정되지 않아 도메인 `deposit()`이 실패할 수 있던 순서를 수정.
  - `DepositServiceTest` 추가: 초기입금 전표 생성 및 초기입금 없음 시 전표 미생성 검증.
  - `deposit/core/build.gradle`에 Spring Boot BOM을 추가해 `spring-boot-starter-*` 의존성 버전 해석을 정상화.
  - 계획 문서에 T07 완료 상태와 현재 코드 TODO 25건을 반영.
- 실행 명령:
  - `.\gradlew :deposit:core:test --console=plain`
  - `rg -n "@todo|TODO:" deposit/core/src/main/java deposit/core/src/test/java -g "*.java"`
  - `(rg -n "@todo|TODO:" closing reporting deposit ecl reconciliation receivable journal-ledger payable account-mart -g "*.java" -g "!**/build/**" | Measure-Object).Count`
- 결과:
  - 첫 `deposit:core:test`는 `deposit/core/build.gradle`의 Spring Boot BOM 누락으로 compile classpath 해석 실패.
  - BOM 추가 후 `deposit:core:test` 성공.
  - deposit/core 범위 TODO 검색 결과 없음.
  - 전체 코드 TODO count는 25건.
- 남은 확인사항:
  - 전체 TODO는 아직 완료되지 않았고, 다음 단계는 Phase 2 closing T08-T10이다.

## 2026-05-29 (@todo Phase 2 closing T08-T10 수정)
- 사용자 요청: 작업하던 TODO 계속 진행.
- 선확인:
  - `closing/README.md`, `closing/docs/README.md`, `FxValuationService`, `ClosingAccountingProperties`, `GlAccountBalance`, closing batch 테스트 구조 확인.
  - `closing/docs/README.md`가 가리키는 상세 docs 3개는 현재 파일이 없어 README 범위까지만 확인.
- 수정 내용:
  - `ClosingAccountingProperties`에 `fxValuationReportingCurrencyCode`와 필수 설정 검증 메서드 추가.
  - `FxValuationService`가 환율 조회와 전표 통화를 closing reporting currency 정책으로 처리하도록 변경.
  - `GlAccountBalance`에 `baseEndingBalance`를 추가하고, FX 평가 시 실제 장부 기준통화 잔액과 재평가 금액의 차이만 전표화하도록 변경.
  - 기준통화 장부 잔액이 없으면 가상 book rate를 만들지 않고 평가를 건너뛰도록 변경.
  - `FxValuationServiceTest` 추가: 정책 reporting currency, base ending balance 기반 평가, base amount 누락 시 skip 검증.
  - 계획 문서에 T08-T10 완료 상태와 현재 코드 TODO 22건을 반영.
- 실행 명령:
  - `.\gradlew :closing:batch:test --console=plain`
  - `rg -n "@todo|TODO:" closing/batch/src/main/java closing/core/src/main/java journal-ledger/core/src/main/java/com/ho/account/journalledger/domain/ledger/domain/GlAccountBalance.java -g "*.java"`
  - `(rg -n "@todo|TODO:" closing reporting deposit ecl reconciliation receivable journal-ledger payable account-mart -g "*.java" -g "!**/build/**" | Measure-Object).Count`
- 결과:
  - 첫 `closing:batch:test`는 `ClosingAccountingProperties` 외부 클래스의 `hasText` 헬퍼 누락으로 컴파일 실패.
  - 헬퍼 추가 후 `closing:batch:test` 성공.
  - closing FX valuation 정책/정확도 TODO는 제거됐고 closing 범위에는 자동 승인/전기 통제 TODO 2건만 남음.
  - 전체 코드 TODO count는 22건.
- 남은 확인사항:
  - Phase 2는 완료됐고, 다음 단계는 Phase 3의 지급/수납/open-item/matching 항목이다.

## 2026-05-29 (IFRS 9 대손충당금 아키텍처/설계 흐름도 추가)
- 사용자 요청: 아키텍처와 설계 흐름도 추가.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md` 확인.
- 수정 내용:
  - `ecl/docs/ALLOWANCE_ARCHITECTURE.md` 추가.
  - 단독 서비스 구성도, 헥사고날 레이어, 런타임 산출 흐름, 실행 시퀀스, 데이터 설계 흐름, 배포 확대 단계를 Mermaid 다이어그램으로 정리.
  - `ecl/README.md`, `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`, `ecl/docs/SERVICE_ONBOARDING.md`, `ecl/docs/BATCH_EXECUTION_FLOW.md`에 새 문서 링크 추가.
- 실행 명령:
  - 대손충당금 아키텍처/런북/온보딩 문서의 비-allowance 실행 경로 표현 검색
  - `git diff --check -- ecl\docs\ALLOWANCE_ARCHITECTURE.md ecl\docs\ALLOWANCE_SERVICE_RUNBOOK.md ecl\README.md ecl\docs\BATCH_EXECUTION_FLOW.md ecl\docs\SERVICE_ONBOARDING.md docs\WORKLOG.md CODEX_WORKLOG.md`
- 결과:
  - 검색 결과 없음.
  - 문서 diff check 성공(CRLF 변환 경고만 출력).
- 남은 확인사항:
  - 문서 변경만 수행했으므로 애플리케이션 기동이나 Docker 이미지 빌드는 실행하지 않았다.

## 2026-06-18 (잔여 Java @todo 리팩터링 완료)
- 사용자 요청: API Spring Boot run/Gradle build 가능 여부 확인, Batch 실행 형태 확인, 남은 `@todo` 주석 처리 진행.
- 선확인:
  - 루트 `WORKLOG.md`는 없고 `docs/WORKLOG.md`가 실제 기록 파일임을 확인했다.
  - 잔여 Java `@todo` 5건(Auth, Gateway, Reporting Batch, Closing FX 2건)을 확인했다.
- 수정 내용:
  - Auth 로그인 잠금 저장소를 `memory`/`jpa` 조건부 어댑터로 분리하고 JPA 공유 잠금 테이블/테스트/문서를 추가했다.
  - Gateway JWT 필터가 Auth token-version 검증 API를 WebClient로 호출하고, Caffeine TTL 캐시와 fail-closed 정책을 적용하도록 변경했다.
  - Reporting batch에 Spring Batch `reportingStatementGenerationJob`/`reportingStatementGenerationStep`과 JobParameter 검증 Tasklet을 추가했다.
  - Closing FX 평가가 `AccountSubjectRef.normalBalanceSide`를 사용해 부채/수익 등 대변 정상잔액 계정의 차대 반전을 처리하도록 변경했다.
  - Closing FX batch Reader를 계정코드 Partition + Repository paging reader 구조로 전환했다.
  - 관련 docs와 설정 예시를 최신화했다.
- 실행 명령:
  - `.\gradlew :reporting:batch:test :reporting:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :auth:test --console=plain --max-workers=1`
  - `.\gradlew :gateway:test --console=plain --max-workers=1`
  - `.\gradlew :contracts:compileJava :master-data:compileJava :journal-ledger:core:compileJava :closing:batch:test --console=plain --max-workers=1`
  - `.\gradlew compileJava --console=plain --max-workers=1`
  - `rg -n "@todo|TODO:" auth gateway closing reporting master-data governance payable receivable reconciliation tax asset-lease loan expenditure-resolution contracts journal-ledger --glob "*.java" --glob "!**/build/**"`
  - `git diff --check -- auth gateway closing contracts master-data journal-ledger reporting`
- 결과:
  - 위 Gradle 검증은 모두 성공.
  - Java 소스 기준 `@todo`/`TODO:` 검색 결과 없음.
  - diff check는 CRLF 변환 경고만 출력.
- 남은 확인사항:
  - 전체 Spring Boot 앱을 동시에 기동하는 검증은 메모리 관리 요청 때문에 수행하지 않았다.
  - Batch Job은 JobParameter와 데이터 준비가 필요하며, 기본 bootRun 컨텍스트 검증은 `spring.batch.job.enabled=false` 기준으로 수행하는 편이 안전하다.

## 2026-06-19 (API/BATCH bootRun 및 전체 build 검증 완료)
- 사용자 요청: 전체 모듈 API Spring Boot run 가능 여부, Gradle build 가능 여부, Batch가 Spring Batch 형태로 실행되는지, 잔여 `@todo` 처리 여부 확인.
- 선확인:
  - 루트 `WORKLOG.md`는 없고 `docs/WORKLOG.md`가 실제 기록 파일임을 재확인했다.
  - 변경 대상인 `account-mart`, `ecl`, `auth`, `closing`, 공통 로컬 실행 문서를 확인했다.
- 수정 내용:
  - `account-mart:mart-api`가 Boot 3.2/Spring 6.1에서 뜨도록 springdoc을 `2.5.0`으로 맞췄다.
  - `account-mart` mart API에서 빠져 있던 `AllowanceAuditLogRepository`, `OdsProductMstRepository`의 실제 JPA/JDBC 경계 어댑터와 Flyway schema를 추가했다.
  - `auth` 로그인 시도 어댑터의 다중 생성자 주입을 명시해 bootRun 빈 생성 실패를 제거했다.
  - `closing:batch`가 batch 오케스트레이터로 필요한 journal-ledger/master-data 포트와 어댑터만 스캔하도록 정리했다.
  - `ecl:batch` demo profile을 컨텍스트 확인용 기본값으로 맞추고, 커스텀 `JobRunner`가 `job.name` 명시 시에만 실행하며 실패 Job을 프로세스 실패로 전파하게 수정했다.
  - 수동 Batch infrastructure 환경에서도 Spring Batch 메타 테이블을 초기화하도록 보강하고, 기존 통합 테스트와 중복 생성되지 않게 했다.
  - ECL/Account-Mart/Closing/Deposit 공유 `.run` 설정과 ECL 실행 문서를 실제 검증 인자에 맞췄다.
- 실행 명령:
  - `.\gradlew projects --console=plain`
  - `.\gradlew compileJava --console=plain --max-workers=1`
  - `.\gradlew build --console=plain --max-workers=1`
  - API bootRun smoke: `account-mart:mart-api`, `ecl:ecl-api`, `asset-lease`, `auth`, `closing:api`, `deposit:api`, `loan:api`, `journal-ledger:api`, `payable:api`, `receivable:api`, `reconciliation:api`, `tax:api`, `expenditure-resolution:api`, `reporting:api`, `master-data`, `governance`, `config-server`
  - 서버형 smoke wrapper: `gateway:bootRun`, `discovery:bootRun` 시작 로그 확인 후 프로세스 종료
  - Batch bootRun context smoke: `account-mart:mart-batch`, `ecl:ecl-batch`, `closing:batch`, `deposit:batch`, `journal-ledger:batch`, `loan:batch`, `payable:batch`, `receivable:batch`, `reconciliation:batch`, `tax:batch`, `expenditure-resolution:batch`, `reporting:batch`
  - 실제 Spring Batch Job smoke: `account-mart:mart-batch integratedPositionEtlJob`, `ecl:ecl-batch standaloneDqJob`, `reporting:batch reportingStatementGenerationJob`
  - `rg -n "@todo|TODO:" --glob "*.java" --glob "!**/build/**" .`
- 결과:
  - 전체 `build` 성공.
  - Java 소스 기준 `@todo`/`TODO:` 검색 결과 없음.
  - API/BATCH bootRun smoke는 검증한 로컬 H2/메모리/인프라 비활성화 조건에서 모두 시작 성공.
  - ECL standalone Job과 reporting/account-mart 대표 Job은 Spring Batch `JobLauncher` 경로로 실행 성공.
- 남은 리스크:
  - 모든 API를 동시에 장시간 띄우는 통합 환경 검증은 수행하지 않았다. 이번 검증은 각 모듈 단독 bootRun/context smoke 기준이다.
  - 실제 업무 Job은 운영 DB, 선행 mart/ECL/master-data 데이터, Kafka/외부 API 등 환경 준비 후 별도 end-to-end 검증이 필요하다.
  - Logstash 수집기가 없는 로컬에서는 일부 기본 profile에서 연결 경고가 날 수 있으며, local/H2 smoke에서는 인프라 의존을 비활성화했다.

## 2026-06-19 (업무 모듈 Batch Job 누락 보강)
- 사용자 요청: 모든 업무 모듈의 API는 Spring Boot run, Batch는 Spring Batch run 가능 여부를 조사하고 누락 시 수정.
- 선확인:
  - `docs/WORKLOG.md`, `CODEX_WORKLOG.md` 최신 항목을 확인했다.
  - `settings.gradle`, 변경 대상 모듈 README 및 `docs/README.md`/`docs/local-run.md`를 확인했다.
  - split API 프로젝트의 `@SpringBootApplication`과 업무 batch 프로젝트의 `Job` 정의를 검색했다.
- 수정 내용:
  - `deposit:batch`: `depositAccountIntegrityJob`을 추가하고, core `DepositBatchUseCase`가 활성 예금 계좌 필수값/잔액/이자율/SCD2 유효기간을 검증하게 했다. Batch auto-run을 위해 수동 `@EnableBatchProcessing`을 제거하고 batch `application.yml` local 기본값을 추가했다.
  - `payable:batch`: `payablePaymentRunJob`을 추가해 `runDate`, `createdBy`, `description` 파라미터로 core 지급런 생성 유즈케이스를 호출하게 했다.
  - `receivable:core/batch`: 자동 매칭 후보 조회 포트를 보강하고 `ReceivableBatchUseCase`/`receivableAutoMatchingJob`을 추가했다.
  - `reconciliation:core/batch`: 활성 대사 단위를 core 대사 서비스로 위임하는 `ReconciliationBatchUseCase`/`reconciliationDailyJob`을 추가했다.
  - `tax:core/batch`: 기간 내 PURCHASE 세금계산서 금액/거래처 참조를 검증하는 `TaxInvoiceBatchUseCase`/`taxInvoiceValidationJob`을 추가했다.
  - `expenditure-resolution:core/batch`: REQUESTED 결의서를 지급예정일 기준으로 core 승인 유즈케이스에 위임하는 `ExpenditureResolutionBatchUseCase`/`expenditureResolutionApprovalJob`을 추가했다.
  - 각 변경 모듈 README와 `docs/local-run.md`에 실제 Job 이름과 실행 파라미터를 반영했다.
- 검증:
  - `.\gradlew :deposit:batch:compileJava :payable:batch:compileJava :receivable:batch:compileJava :reconciliation:batch:compileJava :tax:batch:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew :deposit:core:test :payable:core:test :receivable:core:test :reconciliation:core:test :tax:core:test :expenditure-resolution:core:test :deposit:batch:compileJava :payable:batch:compileJava :receivable:batch:compileJava :reconciliation:batch:compileJava :tax:batch:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew build --console=plain --max-workers=1` 성공.
  - 실제 Spring Batch Job smoke 성공: `depositAccountIntegrityJob`, `payablePaymentRunJob`, `receivableAutoMatchingJob`, `reconciliationDailyJob`, `taxInvoiceValidationJob`, `expenditureResolutionApprovalJob`.
  - 모든 업무 batch 프로젝트에서 `Job` 정의 검색 결과 확인.
  - split API 및 standalone 업무 앱의 `@SpringBootApplication` 검색 결과 확인.
- 남은 리스크:
  - 이번 Job smoke는 local H2와 빈 업무 데이터 기준이다. 운영 데이터 기준의 대량 처리 성능, 재실행성, 멱등성, 회계 금액 결과는 별도 데이터셋으로 검증해야 한다.
  - 일부 batch 앱은 기존 Spring Batch `jobRegistryBeanPostProcessor` 조기 초기화 경고가 남아 있으나, Job 실행은 `COMPLETED`로 확인했다.

## 2026-06-25 (AI 하네스 업그레이드)
- 사용자 요청: 기존 AI 하네스/에이전트 지침/문서 체계를 보존하면서 Codex, Gemini CLI, Antigravity를 포함한 다중 에이전트 운영 하네스를 업그레이드.
- 선확인:
  - 기존 `Agents.md`, `CLAUDE.md`, `GEMINI.md`, `.clinerules`, `SKILL.md`, `.agent/`, `.claude/`, `.github/workflows`, `docs/` 구조를 확인했다.
  - `docs/ai-harness/`는 신규 디렉터리임을 확인했다.
  - 루트 `WORKLOG.md`는 없고 `docs/WORKLOG.md`가 실제 공용 기록 파일임을 재확인했다.
- 수정 내용:
  - 기존 루트 지침 파일을 보존하고 `Agents.md`, `CLAUDE.md`, `GEMINI.md`에 공통 하네스 참조와 작업 원칙을 보강했다.
  - `docs/ai-harness/`에 overview, rules, workflow, agent roles, test checklist, worktree guide, rebase/merge policy, model assignment, file ownership, worklog/status/decision/conflict/integration/handoff 문서를 추가했다.
  - 기존 하네스 관련 파일 백업을 `docs/ai-harness/_backup/2026-06-25/`에 생성했다.
  - `.gitignore`에 `.worktrees/`, `.claude/worktrees/`를 추가했다.
- 검증:
  - 필수 `docs/ai-harness` 파일 존재 확인 성공.
  - conflict marker 검색 결과 없음.
  - trailing whitespace 검색 결과 없음.
  - `git diff --check -- .gitignore Agents.md CLAUDE.md GEMINI.md`는 CRLF 변환 경고만 출력하고 오류 없음.
- 남은 리스크:
  - 현재 브랜치는 로컬 Git ref 제한으로 slash prefix가 아닌 `ai-harness-upgrade-20260625`를 사용한다.
  - 문서-only 변경이라 Gradle 빌드는 실행하지 않았다.

## 2026-06-26 (AI 에이전트 Git 초보자 가이드 추가)
- 사용자 요청: AI 에이전트 코딩 방법을 초보자 기준으로 설명하고, Git과 여러 AI 에이전트가 함께 작업하는 최신 흐름을 문서화한 뒤 push/main 병합/동기화.
- 수정 내용:
  - `docs/ai-harness/90-beginner-ai-agent-git-guide.md`를 추가했다.
  - `Agents.md`와 `docs/ai-harness/00-overview.md`에 신규 가이드 참조를 추가했다.
  - `docs/ai-harness/worklog.md`, `agent-status.md`, `handoff.md`, `integration-log.md`에 작업 상태를 반영했다.
- 검증:
  - 필수 파일 존재 확인 성공.
  - conflict marker 검색 결과 없음.
  - trailing whitespace 검색 결과 없음.
  - `git diff --check`는 CRLF 변환 경고만 있고 오류 없음.
- 통합 계획:
  - `ai-harness-upgrade-20260625` 브랜치 push.
  - 사용자 명시 요청에 따라 `main` 병합 및 push.
- 통합 결과:
  - `ai-harness-upgrade-20260625` 브랜치 push 완료.
  - `main` 최신화 후 `git merge --no-ff ai-harness-upgrade-20260625`로 병합 완료.
  - merge conflict 없음.
  - `main` push 완료.
  - 최종 동기화 확인은 이 로그 커밋 push 직후 수행한다.

## 2026-06-30 (asset-lease API/BATCH split)
- 사용자 요청: `asset-lease`도 다른 업무 모듈처럼 `core`, `api`, `batch`로 나누고 API/BATCH가 core를 참조하게 변경.
- 선확인:
  - `docs/WORKLOG.md`, `docs/ai-harness/10-rules.md`, `asset-lease/README.md`, `asset-lease/docs/README.md`, `asset-lease/docs/local-run.md` 확인.
  - 기존 `asset-lease`는 단일 Gradle 프로젝트가 `core/api/batch` 소스셋을 모두 포함하는 구조였고, `expenditure-resolution:core`가 `:asset-lease`를 참조 중임을 확인.
- 수정 내용:
  - `settings.gradle`에 `asset-lease:core`, `asset-lease:api`, `asset-lease:batch` 하위 프로젝트를 추가.
  - 기존 `:asset-lease`는 `:asset-lease:core`를 노출하는 호환 wrapper로 전환.
  - `asset-lease:core`는 library 모듈로 분리하고, 기존 core 실행 클래스는 `AssetLeaseCoreModule` marker로 정리.
  - `asset-lease:api`에 `AssetLeaseApiApplication`, `asset-lease:batch`에 `AssetLeaseBatchApplication`을 추가.
  - `expenditure-resolution:core` 의존성을 `:asset-lease:core`로 변경.
  - `.run`, Dockerfile, asset-lease 문서, 로컬 개발 문서, Gemini 리뷰 프롬프트를 split 경로로 갱신.
- 검증:
  - `.\gradlew projects --console=plain` 성공.
  - `.\gradlew :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :expenditure-resolution:core:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew :asset-lease:test :asset-lease:compileJava --console=plain --max-workers=1` 성공.
- 남은 리스크:
  - API/BATCH bootRun 장시간 smoke는 아직 실행하지 않았다.
  - Batch 실제 Job 실행은 `targetDate`와 업무 데이터 준비 후 별도 확인이 필요하다.
## 2026-07-02 (전체 Gradle/API/BATCH 실행 재검증)
- 사용자 요청: 모든 모듈의 Gradle build, API Spring Boot 내장 WAS 실행, Spring Batch context/Job 실행, H2/PostgreSQL 로컬 실행 경로를 점검하고 문서화.
- 수정 내용:
  - `AssetLeaseBatchApplication`, `AllowanceMartBatchApplication`, `AllowanceEclBatchApplication`을 Batch 성격에 맞게 non-web 실행으로 정리했다.
  - `FixedAssetRepository`에 Pageable 조회를 추가해 `assetDepreciationJob`의 `RepositoryItemReader` 호출 실패를 수정했다.
  - `docs/local-development.md`에 전체 모듈 순차 실행 매트릭스, H2 공통 옵션, PostgreSQL datasource 옵션, 대표 Spring Batch Job 표를 추가했다.
  - `asset-lease`, `account-mart`, `ecl` 실행 문서의 Vault/Config/Eureka 비활성화, Batch non-web, Job 파라미터 예시를 최신화했다.
- 검증:
  - `.\gradlew projects --console=plain` 성공.
  - `.\gradlew compileJava --console=plain --max-workers=1` 성공.
  - API bootRun smoke 19개 실행 대상 기동 성공.
  - Batch context smoke 13개 실행 대상 기동 성공.
  - 실제 Spring Batch Job 13개 smoke 성공: `assetDepreciationJob`, `fxValuationJob`, `eclProvisionJob`, `loanInterestAccrualJob`, `depositAccountIntegrityJob`, `payablePaymentRunJob`, `receivableAutoMatchingJob`, `reconciliationDailyJob`, `taxInvoiceValidationJob`, `expenditureResolutionApprovalJob`, `reportingStatementGenerationJob`, `integratedPositionEtlJob`, `standaloneDqJob`.
  - Java `@todo`/`TODO:` 검색 결과 없음.
  - `git diff --check` 통과.
  - `.\gradlew build --console=plain --max-workers=1` 성공.
- 남은 리스크:
  - PostgreSQL은 명령/설정 경로를 문서화했지만, 이번 실행 검증은 H2/demo/memory 기준이다.
  - 운영 데이터 기준 대량 처리, 재실행 멱등성, 외부 인프라 연동은 별도 seed/통합 환경 검증이 필요하다.

## 2026-07-02 (account-mart core/batch boundary refactor)
- 사용자 요청: 모듈별 순차 검수 결과를 바탕으로 `@todo` 처리/리팩토링을 진행하고, 커밋/푸시까지 수행.
- 수정 내용:
  - `IntegratedPositionProcessor`, `LedgerDataQualityProcessor`, `CollateralDataQualityProcessor`를 core 업무 컴포넌트로 정리했다.
  - `mart-batch`에 Spring Batch 전용 processor adapter와 `BatchStepParameterUtils`를 추가했다.
  - tasklet과 Job config가 batch adapter를 통해 core 업무 로직을 호출하게 변경했다.
  - core의 `spring-batch-core` API 의존을 제거하고, 미사용 `OdsApartCollDetailRepository`를 삭제했다.
  - ODS-GL 대사 조회를 계정/통화 합계 기준 JPA query로 보정했다.
  - account-mart 문서에 core/batch 헥사고날 경계 설명을 추가했다.
- 검증:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :account-mart:mart-batch:test --console=plain --max-workers=1` 성공.
  - core Spring Batch 타입 직접 참조 검색 결과 없음(설명 주석 제외).
  - 미사용 skeleton 포트/테스트 mock 잔존 검색 결과 없음.
- 리스크:
  - shutdown 시 기존 step-scope reader close WARN은 남아 있으나 테스트는 성공.
  - ODS-GL 합계 대사는 H2/test 기준 검증이며 PostgreSQL 대량 데이터 플랜 검증은 남아 있음.
- 롤백:
  - `git revert <이번 커밋>` 또는 `account-mart` 하위 변경 파일을 이전 커밋으로 되돌린다.

## 2026-07-03 (ecl core pipeline boundary refactor)
- 사용자 목표: 모듈을 순차 검수하면서 헥사고날/DDD/업무 프로세스 기준으로 리팩토링하고, 기존 주석과 초보자 문서를 보존·개선.
- 수정 내용:
  - `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`을 `ecl-core/application/pipeline`에 추가했다.
  - `StagingProcessor`, `EadCrmProcessor`, `EclProcessor`를 Spring Batch adapter로 축소했다.
  - `AllowanceCalculationService`가 batch와 같은 core pipeline을 재사용하도록 변경했다.
  - pipeline 단위 테스트 3개와 유즈케이스 서비스 테스트를 갱신했다.
  - ecl 문서와 batch config 주석에 core pipeline / batch adapter 경계를 반영했다.
- 검증:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1` 성공.
  - ecl-batch processor 내 계산 서비스/BigDecimal 직접 참조 검색 결과 없음.
  - ecl-core Spring Batch 타입 직접 참조 검색 결과 없음(설명 주석 제외).
  - ecl Java TODO 검색 결과 없음.
- 리스크:
  - PostgreSQL 대량 seed 기준 성능/실데이터 금액 검증은 남아 있다.
- 롤백:
  - 변경 커밋 생성 전이면 `git restore -- ecl docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness/* docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` 범위로 되돌릴 수 있다.
## 2026-07-03 (journal-ledger balance reaggregation batch refactor)
- 사용자 요청: 커밋/푸시 전까지 남은 모듈 순차 검수 결과를 반영하고, Spring Boot Batch 실행 경로를 실제 검증.
- 수정 내용:
  - `journal-ledger:batch`에 `dailyBalanceReaggregationJob` 실행 Tasklet과 JobParameter 기간 변환 유틸을 추가했다.
  - `BalanceReaggregationBatchConfig`는 Job/Step 구성만 담당하도록 축소하고 core `LedgerService` 호출은 Tasklet adapter로 이동했다.
  - `application.yml`의 H2 datasource/JPA/Batch 설정 계층을 수정하고, Batch test 의존성을 `spring-batch-test`로 보정했다.
  - IntelliJ 실행 설정과 `journal-ledger`/전사 local-development 문서를 H2 local Batch Job 실행 기준으로 최신화했다.
  - `Money` 값 객체의 깨진 초보자용 주석을 BigDecimal 금액 규칙 설명으로 복구했다.
- 검증:
  - `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1` 성공, `dailyBalanceReaggregationJob` COMPLETED.
  - ecl/journal-ledger Java TODO/깨진문자 검색 결과 없음.
  - 문서 literal 검색 결과 없음.
  - `git diff --check` 오류 없음(CRLF 변환 경고만 출력).
- 리스크:
  - PostgreSQL 대량 데이터 기준 잔액 재집계 성능/락/재실행 검증은 남아 있다.
  - Batch bootRun의 Spring Cloud/Batch 조기 BeanPostProcessor WARN은 기존 의존성 조합 영향으로 남아 있다.
- 롤백:
  - 커밋 후에는 이번 커밋을 revert한다.
## 2026-07-03 (closing core/batch boundary refactor)
- 사용자 목표: 모듈을 순차 검수하면서 헥사고날/DDD/업무 프로세스 기준으로 리팩토링하고, 기존 주석과 초보자 문서를 보존·개선.
- 수정 내용:
  - `closing:batch`의 FX 평가/ECL 충당 업무 서비스와 전표번호 팩토리를 `closing:core`로 이동했다.
  - core outbound port(`FxExchangeRateLookupPort`, `AllowanceBalanceLookupPort`, `ClosingJournalEntryPort`)와 전표 command/result 타입을 추가했다.
  - batch adapter에서 master-data 환율, journal-ledger GL 잔액, journal-ledger 전표 생성 기술 구현을 담당하게 했다.
  - Batch config는 Job/Step/Reader/Tasklet과 core 위임만 담당하도록 정리했다.
  - 기존 batch service 테스트를 core service 테스트로 이동했다.
  - closing 문서와 주석에 core/batch 책임 경계를 최신화했다.
- 검증:
  - `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :closing:api:compileJava --console=plain --max-workers=1` 성공.
  - closing core Spring Batch 타입 직접 참조 검색 결과 없음.
  - closing Java/문서 TODO 및 깨진문자 검색 결과 없음.
- 리스크:
  - PostgreSQL 대량 데이터 기준 FX/ECL 결산 전표 성능과 skip/retry 운영 정책 검증은 남아 있다.
- 롤백:
  - 커밋 전이면 `git restore -- closing docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` 범위로 되돌릴 수 있다.
- 추가 검증:
  - `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1` 성공.
  - closing API/BATCH local logback 설정을 추가해 로컬 실행에서 Logstash 연결 경고를 제거했다.
## 2026-07-08 (receivable API/core command boundary refactor)
- 사용자 요청: 커밋/푸시 전까지 남은 API/core command 경계 정리와 검증을 완료.
- 수정 내용:
  - `receivable:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했다.
  - `SalesInvoiceCommand`, `CollectionCommand`, `ManualMatchingCommand`를 core application input boundary로 추가했다.
  - `receivable:api`가 Controller, 요청/응답 DTO, Bean Validation, controller tests를 소유하도록 이동했다.
  - `SalesService`와 `CollectionService`는 command를 받아 domain 객체를 생성하고 기존 업무 규칙과 전표 포트 호출을 유지한다.
  - `ReceivableBatchJobRegistryConfiguration`으로 Batch Job 등록 순서를 늦춰 local/H2 context 경고를 제거했다.
- 검증:
  - `.\gradlew :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1` 성공.
  - `receivable:api:bootRun` local/H2 context smoke 성공.
  - `receivable:batch:bootRun` local/H2 context smoke 성공, JobRegistry BeanPostProcessor warning 미재현.
- 리스크:
  - PostgreSQL/Flyway 및 실제 대량 `receivableAutoMatchingJob` 실행은 별도 검증 필요.
- 롤백:
  - 커밋 후에는 이번 receivable/payable boundary refactor 커밋을 revert한다.
## 2026-07-08 (reconciliation API/core command boundary refactor)
- 사용자 요청: 커밋/푸시 전까지 남은 API/core command 경계 정리와 검증을 완료.
- 수정 내용:
  - `reconciliation:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했다.
  - `AssignDifferenceCommand`, `DifferenceReasonCodeCommand`, `ReconciliationRuleCommand`, `ReconciliationUnitCommand`, `ResolveDifferenceCommand`, `RunReconciliationCommand`를 core application input boundary로 추가했다.
  - `reconciliation:api`가 Controller, 요청/응답 DTO, Bean Validation을 소유하도록 이동했다.
  - `ReconciliationService`와 `ReconciliationBatchService`는 command를 받아 domain 객체 생성, 대사 실행, 차이 배정/해결을 수행한다.
  - `ReconciliationBatchJobRegistryConfiguration`으로 Batch Job 등록 순서를 늦춰 local/H2 context 경고를 제거했다.
  - reconciliation 문서와 Gemini 리뷰 프롬프트를 API DTO -> core command -> domain/service 흐름 기준으로 최신화했다.
- 검증:
  - `.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1` 성공.
  - `reconciliation:api:bootRun` local/H2 context smoke 성공.
  - `reconciliation:batch:bootRun` local/H2 context smoke 성공, JobRegistry BeanPostProcessor warning 미재현.
- 리스크:
  - PostgreSQL/Flyway 및 실제 대량 `reconciliationDailyJob` 실행은 별도 검증 필요.
- 롤백:
  - 커밋 후에는 이번 reconciliation boundary refactor 커밋을 revert한다.
## 2026-07-08 (reporting API response DTO boundary refactor)
- 사용자 요청: 모듈 순차 점검을 이어가며 헥사고날/DDD/API 경계, 업무 흐름, 문서 최신화를 진행.
- 수정 내용:
  - `ReportingController`가 core 도메인 객체를 HTTP 응답으로 직접 반환하지 않도록 response DTO를 추가했다.
  - `FinancialStatementResponseDto`, `DisclosureNoteMartResponseDto`, `RegulatoryReportSubmissionResponseDto`, `RegulatoryFilingResponseDto`, drill-down DTO 등을 `reporting:api`에 추가했다.
  - Controller는 core command/use case 호출 후 DTO로 변환하고, 보고서 생성/제출/주석 분류 업무 판단은 core에 유지했다.
  - reporting README/docs와 Gemini 리뷰 프롬프트를 API DTO 경계 기준으로 최신화했다.
- 검증:
  - `.\gradlew :reporting:api:test --console=plain --max-workers=1` 성공.
  - `.\gradlew :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1` 성공.
- 리스크:
  - PostgreSQL/Flyway 및 실제 대량 Batch Job 실행은 별도 검증 필요.
- 롤백:
  - 커밋 전이면 `git restore -- reporting docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` 범위로 되돌릴 수 있다.
## 2026-07-09 (deposit command and Batch asOfDate boundary refactor)
- 사용자 요청: 모듈 순차 점검을 이어가며 헥사고날/DDD/업무 프로세스 기준으로 경계와 문서를 보강.
- 수정 내용:
  - `OpenAccountCommand`에 필수 코드, 통화 코드 정규화, 초기입금/금리 음수 방어를 추가했다.
  - `DepositAccountIntegrityBatchConfig`가 `asOfDate` 누락 시 현재 날짜로 대체하지 않고 실패하도록 변경했다.
  - core command 검증 테스트와 batch JobParameter 테스트를 추가했다.
  - deposit README/docs와 Gemini 리뷰 프롬프트를 API DTO -> core command, Batch 기준일 필수 정책 기준으로 최신화했다.
- 검증:
  - `.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1` 성공.
- 리스크:
  - PostgreSQL/Flyway 및 실제 외부 포트 연결은 별도 통합 검증 필요.
- 롤백:
  - 커밋 전이면 `git restore -- deposit docs/WORKLOG.md CODEX_WORKLOG.md docs/ai-harness docs/module-documentation-sequence.md GEMINI_REVIEW_PROMPT.md` 범위로 되돌릴 수 있다.

## 2026-07-14 (master-data typed applier and validity statistics boundary)

- 사용자 목표: 모듈을 순차 검수하면서 헥사고날/DDD/업무 정합성/대량 처리 기준으로 리팩토링하고 초보자 문서와 `@todo`를 최신화.
- 수정 내용:
  - core pipeline의 batch DTO 역참조를 제거하고 `MasterDataValidityReport`를 core 모델로 추가했다.
  - `MasterDataValidityStatisticsPort`와 JPA 통계 어댑터/COUNT 쿼리로 전체 행 메모리 집계를 제거했다.
  - 일일 보고 기준일을 필수화해 재실행 시 현재 날짜에 따라 결과가 달라지지 않게 했다.
  - 계정과목/거래처/부서/상품 typed change applier를 구현했다.
  - JSON payload 파싱을 `MasterDataChangePayloadDecoder` 포트와 Jackson 어댑터로 분리했다.
  - targetKey 일치, 승인 effectiveDate 전달, 비활성화 payload 불필요, SCD2 종료 기간 검증을 보강했다.
  - IntelliJ/H2 standalone 실행 설정과 master-data 문서를 실제 코드 경계 기준으로 갱신했다.
- 검증:
  - `.\gradlew :master-data:test --console=plain --max-workers=1` 성공.
  - H2 JPA COUNT 통합 테스트 성공.
  - local/H2 non-web bootRun 성공.
  - core→batch 역참조 및 batch 업무 연산 검색 결과 없음.
- 남은 리스크:
  - 통화/환율/회계기간 typed applier와 target 버전 충돌 검사는 코드 `@todo`로 남아 있다.
  - master-data batch는 독립 Spring Batch Job/Step 모듈이 아니다.
  - PostgreSQL Flyway DDL과 대량 실행 계획은 별도 검증이 필요하다.
- 롤백:
  - 커밋 전에는 `master-data`, `.run/Master Data bootRun.run.xml`, 관련 문서/하네스/Gemini prompt 파일을 기존 사용자 변경과 구분해 되돌린다.

## 2026-07-14 (governance approval boundary and standalone runtime)
- 요청 목표: 누적 변경을 커밋/푸시하기 전에 governance 모듈의 DDD/헥사고날 승인 경계와 H2/PostgreSQL 실행 문서를 완성한다.
- 변경:
  - 실제 audit/master-data 승인 Bean을 명시적으로 스캔해 빈 Spring Boot 서버 기동 문제를 제거했다.
  - 권한 회수를 즉시 삭제에서 승인 요청으로 전환하고 API가 `202 Accepted` 접수 정보를 반환하게 했다.
  - 역할/권한 Apply adapter의 지원 조합을 명시하고 모든 미지원 조합을 fail-closed 처리했다.
  - 서비스/어댑터/컨텍스트 테스트, H2/PostgreSQL 런타임 의존성, IntelliJ 단독 실행 설정과 초보자 문서를 보강했다.
- 검증:
  - `.\gradlew :governance:compileJava --console=plain --max-workers=1` 성공.
  - `.\gradlew :governance:test :governance:bootJar --console=plain --max-workers=1` 성공.
  - H2/local non-web `bootRun` 성공.
- 리스크:
  - PostgreSQL/Flyway 실제 실행은 미검증이다.
  - preview API 응답과 외부 Auth outbox/inbox `@todo`가 남아 있다.
- 롤백:
  - governance와 관련 문서/Run Configuration 변경이 포함된 이번 커밋을 revert한다.

## 2026-07-14 (auth authentication snapshot and approval idempotency)
- 요청 목표: 모듈 순차 점검 중 auth의 DDD/헥사고날 경계, 인증 시점 정합성, Governance 역할 승인 재시도, 로컬 실행 문서를 고도화한다.
- 변경:
  - core의 API DTO 역참조를 제거하고 LoginCommand/AuthenticationResult로 분리했다.
  - 단일 Clock 시점의 역할 스냅샷을 응답/JWT에 공통 사용하고 token-version 상태 검사를 강화했다.
  - memory 역할 메타데이터 손실을 수정하고 memory/JPA 양쪽에 approvalTraceId/fingerprint 멱등성을 구현했다.
  - JPA 사용자 lock, apply log, Flyway V72, 도메인 기간/길이 검증과 회귀 테스트를 추가했다.
  - H2/Flyway/JPA validate 단독 실행 및 PostgreSQL 로컬 명령을 문서화했다.
- 검증:
  - `.\gradlew :auth:test :auth:bootJar --console=plain --max-workers=1` 성공, 32개 테스트 통과.
  - H2 local non-web bootRun 성공, Flyway V70~V72 및 Hibernate validate 성공.
- 리스크:
  - PostgreSQL 동시성/Flyway 실환경은 미검증이다.
  - 평문 비밀번호 승격, 최초 실패 원자화, 멱등 이력 보존 `@todo` 3건이 남아 있다.
- 롤백:
  - auth, Run Configuration, 관련 문서/하네스 변경을 되돌린다.

## 2026-07-20 (gateway global authentication and trusted-header boundary)
- 요청 목표: auth 다음 순차 모듈인 gateway를 헥사고날/업무 정합성/함수형 WebFlux/스켈레톤/실행 문서 기준으로 검수하고 고도화한다.
- 변경:
  - 선택적 route filter를 모든 `/api/**`의 전역 인증 정책으로 바꿔 legacy catch-all 인증 우회를 제거했다.
  - 로그인 POST만 공개하고 Auth validate/internal 경로를 외부에서 차단했다.
  - 클라이언트 `X-Auth-*`를 제거하고 검증된 `AuthenticatedPrincipal` 값으로만 재생성한다.
  - `AccessTokenVerifier`/`TokenVersionValidator` 포트와 JJWT/WebClient 어댑터를 분리했다.
  - 필수 JWT claim과 양의 정수 roleVersion을 fail-closed 검증하고 권한 거절 401과 Auth 장애 503을 구분했다.
  - 빈 Auth response publisher도 503으로 고정하고 정상 결과만 case-sensitive canonical username 기준으로 캐시한다.
  - request ID 검증, 설정 Bean Validation, test console logging, route/Compose YAML 회귀 테스트를 추가했다.
  - Gateway/Config/Docker 포트 8000, Docker Auth 서비스 주소, JDK 17 Dockerfile과 standalone IntelliJ 설정을 정합화했다.
  - gateway README/docs와 공통 실행/순차 문서를 실제 코드/데이터 흐름으로 최신화했다.
- 검증:
  - `.\gradlew :gateway:test :gateway:bootJar --console=plain --max-workers=1 --no-daemon` 성공, 30개 테스트 통과.
  - local standalone bootRun의 Netty 8000 및 actuator health `UP` 확인 후 프로세스 종료.
  - route/Compose YAML 파싱 테스트 성공.
- 리스크:
  - Docker CLI가 없어 실제 compose config/image 실행은 미검증이다.
  - live Config/Discovery/Auth 라우팅 통합은 미검증이다.
  - JWKS 키 회전, 이벤트 기반 cache 무효화, Auth 내부 API 서비스 인증, legacy catch-all 제거 `@todo`가 남아 있다.
- 롤백:
  - gateway/config-repo/Compose/run configuration 및 관련 문서·하네스 변경을 한 묶음으로 revert한다.
## 2026-07-20 (discovery registry lifecycle and readiness boundary)
- 요청 목표: gateway 다음 discovery 모듈을 인프라 책임, 정합성, 스켈레톤 여부, 실행/문서 기준으로 순차 검수하고 고도화한다.
- 변경:
  - Config가 없어도 8761, register/fetch=false로 기동하는 Eureka Server 기본 계약을 추가했다.
  - Config Client, actuator, Prometheus, Brave/Zipkin 설정을 실제 의존성과 연결했다.
  - readiness와 register -> lookup -> cancel registry 생명주기 통합 테스트를 추가했다.
  - local/config/루트·모듈 Compose/Dockerfile 정책 테스트를 추가했다.
  - Dockerfile을 JDK 17 단일 bootJar/readiness healthcheck로 정리하고 module Compose build context를 루트로 수정했다.
  - root Compose의 Discovery 의존 14개 서비스를 `service_healthy`로 전환하고 container Logstash/Zipkin 주소를 주입했다.
  - standalone IntelliJ 설정과 test console logging을 추가했다.
  - 기존 전화번호부 설명과 archive를 보존하면서 lease/self-preservation/재시작/보안·HA 데이터 흐름 문서를 상세화했다.
- 검증:
  - `.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon` 성공, 6개 테스트 통과.
  - local standalone 8761 readiness UP, Dashboard/registry/Prometheus 200, JVM metric 확인.
  - 종료 후 Discovery/Gradle 프로세스 없음.
- 리스크:
  - Docker CLI가 없어 실제 Compose/image 실행은 미검증이다.
  - live Config/다중 서비스 heartbeat/LoadBalancer와 운영 self-preservation 부하는 미검증이다.
  - private network+mTLS/인증, multi-AZ peer sync/장애 전환 `@todo`가 남아 있다.
- 롤백:
  - discovery/config-repo/root Compose/run configuration 및 관련 문서·하네스 변경을 한 묶음으로 revert한다.
## 2026-07-22 (config-server repository availability boundary)
- Branch: `agent/asset-lease-split`
- Scope: `config-server`, `config-repo/README.md`, Config Server root Compose dependencies, shared docs/harness.
- Findings:
  - Baseline had `test NO-SOURCE`, a runtime-missing native repository in Docker, JDK 21 drift, and `service_started` ordering.
  - Spring default Config health accepts an empty Environment, which was too weak for downstream startup.
- Changes:
  - Added a strict repository health indicator behind `EnvironmentRepository` and configurable representative application/profile.
  - Added HTTP/config/health/policy tests, JDK 17 Docker packaging, read-only repository mounts, and Config health dependency ordering.
  - Preserved and expanded beginner recipe-headquarters comments and documented application/profile/property-source/client-binding flow.
- Verification:
  - Initial `:config-server:test :config-server:bootJar` passed with 9 tests and no failures/errors/skips.
  - The later health-detail sanitization sources produced newer main/test class outputs, but the targeted test rerun stalled before producing a new report because the Windows paging file was exhausted; the Gradle JVMs were stopped.
  - The initial standalone `java -jar` on 8888 returned readiness UP, one property source for `master-data/default`, and Prometheus JVM metrics.
- Risk:
  - Docker image/Compose could not be run because Docker CLI is unavailable.
  - Rerun `ConfigRepositoryHealthIndicatorTest` after restoring paging-file headroom.
  - Production service authentication/mTLS and Git-backed reviewed version/refresh/rollback remain explicit TODOs.
- Rollback:
  - Revert only Config Server/config-repo/root Compose condition changes and associated docs/log entries after checking Discovery dependency overlap.

## 2026-07-22 (contracts/shared-kernel second-pass boundary review)

- Branch: `agent/asset-lease-split`
- Findings:
  - Dated Master Data contract silently fell back to current data.
  - `@Masked` had no Jackson serializer binding.
  - Local capability lookup used ApplicationContext as a service locator.
  - `@DistributedLock` had no interceptor, while the ECL consumer claimed lock acquisition.
  - CDM timestamp parameters defeated Spring Batch idempotency and failures were swallowed.
  - Library Docker/Compose files were explicit no-op skeletons.
  - shared-kernel exposes broad infrastructure dependencies and ECL/account-mart-specific types.
- Changes:
  - Added immutable/validated journal contracts and strict normal-balance values.
  - Implemented dated account/partner/department SCD2 lookup in the master-data adapter.
  - Connected and hardened masking; refactored the local registry to immutable constructor injection and duplicate-name failure.
  - Removed the inert lock usage; used eventId for Batch identity, ignored only completed duplicates, and propagated other failures.
  - Added 15 focused tests and archived four no-op runtime skeletons.
  - Rewrote module docs while preserving beginner analogies and documenting business/data flow.
- Verification:
  - `git diff --check` passed; no `DistributedLock` Java usage remains.
  - Gradle and direct javac verification could not start/finish under the exhausted Windows paging file, even with 64-128 MB heaps.
  - Spawned JVMs and the temporary javac directory were cleaned up.
- Risk:
  - All new tests and affected module compilation remain pending; do not represent this pass as green before rerun.
  - Dependency extraction, dated-port default removal, versioned source DTOs, allowance contract ownership, and real distributed locking remain TODO.
- Rollback:
  - Revert contracts/shared-kernel, dated master-data adapter/repository, ECL event consumer/build/tests, archived skeleton moves, and this pass's docs/harness entries.

## 2026-07-27 (Master Data change governance and runtime boundary)

- Branch: `agent/asset-lease-split`.
- Scope: Master Data change-request domain/application/persistence/API boundaries, migrations, runtime packaging, tests, and beginner documentation.
- Changes:
  - Added SCD2 requested-version policy and repository-backed version query port.
  - Added immutable fail-closed applier registry with duplicate ownership detection.
  - Added pessimistic decision lookup, optimistic request lock version, and version rechecks at request/approve/apply boundaries.
  - Added Governance approval sourceReference idempotency, payload conflict detection, applied timestamp lineage, and V5 migration; UPDATE/DELETE now require an explicit requested version.
  - Added Bean Validation, HTTP 409 conflict mapping, runtime dependency/port/readiness alignment, JDK 17 bootJar Docker packaging, and focused tests.
  - Preserved V2 checksum; added V3/V4/V5 forward migrations and documented the clean PostgreSQL bootstrap gap caused by historical V2 `CLOB`.
  - Preserved beginner comments and updated business/data flow, schema, local run, archive, and handoff documents.
- Verification:
  - `.\gradlew :master-data:clean :governance:clean :master-data:test :master-data:bootJar :governance:test :governance:bootJar --console=plain --max-workers=1 --no-daemon "-Dorg.gradle.jvmargs=-Xmx320m -XX:MaxMetaspaceSize=224m -Dfile.encoding=UTF-8"`: passed.
  - Master Data: 17 suites / 57 tests. Governance: 10 suites / 25 tests. Total: 82 tests with 0 failures, errors, or skips; both bootJars passed.
  - `git diff --check`, conflict/placeholder scan, and changed Markdown relative-link check passed.
- Risk:
  - Live PostgreSQL migration and Docker/Compose execution were not run.
  - Same-business-key distributed serialization, atomic concurrent source-reference recovery, per-request chunk transactions/SKIP LOCKED, trusted actor extraction, payload masking, direct-write governance, and three typed appliers remain explicit TODOs.
- Rollback:
  - Revert the Master Data governance/runtime commit as one unit; do not partially remove V3/V4/V5 while retaining the entity fields they support.

## 2026-07-27 (Master Data SCD2 validity and historical-partner follow-up)

- 요청 목표: Master Data 검수 후속으로 SCD2 상태 변경 순서, 과거 거래처 조회, 중복 기간 fail-closed와 초보자 문서를 실제 코드와 일치시킨다.
- 변경:
  - 네 UPDATE 서비스가 신규 `validFrom/validTo`를 먼저 검증하고 현재 버전을 종료하도록 정리했다.
  - 계정과목/부서의 상위 참조 확인과 신규 버전 조립을 기존 행 종료보다 먼저 수행하도록 순서를 고정했다.
  - 거래처 현재 활성 조회와 과거 유효기간 조회를 분리하고 단건 결과를 `Optional`로 고정해 중복 행을 숨기지 않게 했다.
  - H2 JPA 통합 테스트로 종료된 과거 거래처 조회와 겹치는 기간의 예외를 검증했다.
  - Governance 승인 ID 멱등키/미래 시행일 예약 설명과 Master Data 과거 조회/호출 순서 문서를 보강했다.
  - PostgreSQL 기간 exclusion constraint와 거래처 `useYn` 상태 분리의 완료 조건을 코드 `@todo`로 남겼다.
- 검증:
  - `.\gradlew :master-data:test :master-data:bootJar :governance:test :governance:bootJar --console=plain --max-workers=1 --no-daemon "-Dorg.gradle.jvmargs=-Xmx320m -XX:MaxMetaspaceSize=224m -Dfile.encoding=UTF-8"` 성공.
  - Master Data 63 + Governance 25 = 88 tests, 실패/오류/skip 0건, 두 bootJar 성공.
  - `git diff --check`, conflict marker 검색, 변경 Markdown 상대 링크 검증 성공.
- 리스크:
  - PostgreSQL exclusion constraint/migration과 Docker/Compose는 실환경에서 검증하지 않았다.
  - 과거 `BusinessPartnerRef.active`는 legacy `useYn` 때문에 기준일 당시 의미가 완전하지 않다.
- 상태:
  - 기준 커밋 `5d55704`는 현재 원격 feature branch에 존재하며, 이 후속 변경은 로컬 working tree에 커밋되지 않았다.

## 2026-07-27 (Master Data lookup and fiscal-period boundary second pass)

- Branch: `agent/asset-lease-split`; base HEAD `5d55704` is already on the remote feature branch.
- Scope: database-side active lookup/search, as-of exchange-rate selection, fiscal-period domain/port locking, dead-skeleton cleanup, tests, and beginner documentation.
- Changes:
  - Moved Account Subject/Product active lists and Business Partner active-name search into validity-aware repository queries; blank searches fail fast.
  - Selected the latest exchange rate not after the requested date, validated ISO codes and positive rates, and made overlapping active Currency rows fail closed.
  - Added a locked Fiscal Period persistence port and domain transition method with actor validation, no OPEN-to-permanent jump, and terminal permanent close.
  - Removed unused change-request `getAll`, no-op active setters, synthetic detached Currency compatibility methods, and unused validity helpers after usage scans.
  - Added explicit TODOs for a complete PostgreSQL baseline, pagination, TaxProfile ownership, and current Loan/Closing direct Master Data dependencies.
- Verification:
  - `.\gradlew :master-data:test :master-data:bootJar :governance:test :governance:bootJar :closing:core:test :closing:batch:compileJava :journal-ledger:core:test --console=plain --max-workers=1 --no-daemon "-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8"`: passed.
  - Master Data 73 + Governance 25 + Closing Core 19 + Journal Ledger Core 19 = 136 tests, with 0 failures/errors/skips; Closing Batch compiled and both bootJars passed.
- Risk:
  - Live PostgreSQL/Flyway and Docker/Compose remain unverified; pagination and temporal exclusion constraints are not implemented yet.
  - TaxProfile has no complete application/persistence flow. Loan and Closing still contain direct provider-internal dependencies documented for the next sequential pass.
- State:
  - The cumulative follow-up remains uncommitted. No commit, push, or merge is authorized.

## 2026-07-27 (Loan boundary, lifecycle, EIR and accrual workflow pass)

- Branch/base: `agent/asset-lease-split` on remote base `5d55704`; cumulative Master Data and Loan changes remain uncommitted.
- Findings: provider entities leaked into the aggregate, HTTP DTOs lived in core, API/runtime schedules diverged, EIR mixed double/percent units, lifecycle events shared an invalid enum conversion, and disbursal/accrual concurrency and retry contracts were incomplete.
- Implementation:
  - Introduced Loan-owned inbound/outbound ports and scalar reference snapshots; confined Master Data and Journal provider types to infrastructure adapters.
  - Moved DTO/validation to API and added stable event response plus 404/400/409 mapping.
  - Added pending/full-disbursal/active/default/recovery domain transitions, current-balance recalculation, locks, version and idempotency constraints.
  - Replaced EIR with fail-closed BigDecimal decimal-rate calculation and one monthly runtime schedule used by both API and Batch.
  - Added locked accrual persistence, success skip, failed retry, required date, core chunk pipeline and Step failure aggregation.
  - Added V33, JDK 17 exact bootJar Docker/8088 Compose alignment, tests and current workflow documentation.
- Verification: 58 affected suites/169 tests passed with 0 failures/errors/skips; Loan API and Batch bootJars passed. Static architecture, whitespace, marker, legacy consumer and current Markdown link checks passed.
- Gaps: Docker/YAML runtime validation unavailable locally; PostgreSQL migration, V33 duplicate preflight, outbox/inbox, trusted actor, daily day-count/calendar and remote MSA adapters remain.
- Rollback: revert the Loan tree, six dated Master Data account/currency port-adapter-repository files, root Loan Compose environment, and matching docs/log entries. Never roll back an applied V33 by deleting migration history.
- State: review-ready and uncommitted; no commit, push or merge without explicit user authorization.

## 2026-07-28 (Closing consistency, batch and runtime pass)

- Branch/base: `agent/closing-consistency-pass` at `a06ebd6`; no linked Issue/PR and no commit/push/merge.
- Reviewed Closing core, batch, API runtime, Journal and Master Data bridge calls rather than treating the prior Closing documentation pass as complete.
- Implemented fail-closed fiscal-period lookup, controlled calendar/task/gate/reopen transitions, maker-checker and non-swallowed audit writes.
- Separated API execution history transactions, kept DRAFT as PENDING_APPROVAL, and added explicit FAILED persistence tests.
- Moved FX to posted-journal signed balances with bounded range partition/Cursor/chunk rollback; hid exchange-rate repository behind a contracts port.
- Moved ECL aggregation into SQL, corrected GL credit sign and per-group subtraction, and enforced finalized single snapshot/legal entity/date inputs.
- Added deterministic explicit slips, same-content retry reuse, state-aware auto-post and annual closing idempotency/base-currency/category behavior.
- Narrowed API/Batch runtime scans, imported only required provider adapters, isolated batch-only services and added executable-owned application names.
- Verification: Contracts 4, Master Data 74, Journal Ledger Core 23, Closing Core 44, API 1, Batch 12 = 158 tests in 52 suites; failures/errors/skips 0. API/Batch bootJars passed.
- Remaining risks: PostgreSQL/Docker not run; FX read model/load test, annual aggregate port, execution-key/outbox recovery, legal-entity GL key, unlock history migration, typed evidence and remote adapters remain.
- Rollback: revert `closing/**`, this pass's contracts/Master Data/Journal Ledger bridge files, and synchronized docs/harness records.
- State: review-ready and uncommitted; explicit user authorization is required before commit/push/merge.

## 2026-07-28 (Foundation pending verification and phantom app cleanup)

- Branch/base: `agent/closing-consistency-pass` at `a06ebd6`; cumulative Closing changes remain uncommitted.
- Re-ran the previously resource-blocked Config Server health test and full Config test/bootJar successfully.
- Found and fixed a real Jackson runtime mismatch: explicitly pinned databind 2.17.1 was paired with Boot-managed core 2.15.4. Databind now follows the Spring Boot 3.2.5 BOM.
- Corrected the ECL consumer failure-propagation test to throw a checked exception actually declared by `JobLauncher.run`.
- Removed the source-less `:app` Gradle include and aligned root/beginner/local-development guidance with service-owned applications; the ignored local `app/build` directory was not deleted.
- Verification: Config target rerun passed; shared-kernel 6 and ECL target 3 tests were forced and passed; final affected run passed 40 suites/140 tests plus Config Server bootJar, and `projects` no longer lists `:app`.
- Risks: shared-kernel infrastructure dependency extraction and allowance type ownership remain staged work; Config Git governance/security and live Docker/PostgreSQL integration remain unverified.
- Rollback: revert `shared-kernel/build.gradle`, the ECL consumer test, `settings.gradle`, the three execution-guide edits, and synchronized harness entries.
- State: review-ready and uncommitted; no commit, push, or merge without explicit user authorization.

## 2026-07-29 (Git synchronization, Issue #20 transfer and latest-main conflict resolution)

- Workspace/branch: Issue #20 전용 `agent/20-closing-consistency`, repository root `C:\Users\skyg547\IdeaProjects\account`.
- Preserved all tracked and untracked local work in the named stash `codex-sync-backup-closing-foundation-2026-07-29`, fast-forwarded to `origin/main@b7c8aaa`, and applied the backup without dropping it.
- Verified all 77 backup paths remain in the working tree; no path was lost. Resolved the sole `docs/WORKLOG.md` conflict by retaining both upstream and local records and documented it in the conflict log.
- Created a second transfer stash with 82 paths. After the external C:\tmp worktree disappeared during verification, recovered the unchanged transfer stash into the clean repository-root issue branch.
- Latest main advanced to `f3d33ea` with Issue #29. Resolved standalone internal-audit modify/delete and shared-kernel conflicts by preserving the new core/api/batch structure and upstream AuditAspect API; the interim b7 AuditAspect assembly fix is not in the final PR.
- Verification on `f3d33ea`: 176 affected tests passed with 0 failures/errors/skips; Closing API, Closing Batch, and Config Server bootJars passed. Internal-audit submodule tasks pass but have no tests, and API/Batch have no production source.
- Local environment: upstream no longer selects the installed JDK 17 path while `JAVA_HOME` is JDK 21, so verification used `-Porg.gradle.java.installations.paths=C:\Java\jdk17` without committing a machine-specific path.
- State: base equals latest origin/main at `f3d33ea`; user authorized commit/push/Draft PR. Merge and Issue close remain unapproved; two named backup stashes are retained.
- Rollback: revert the Issue #20 commit after commit, or compare/apply the retained transfer stash in a clean safe checkout before commit. No DB migration was added in this pass.

## 2026-07-29 (Issue #20 Closing main parity reconciliation)

- Fetched `origin/main@ce35ce5` and compared the full Closing/Foundation transfer snapshot before applying any new code commit.
- Found that commit `31be6f1` already contains all Closing production, test, module documentation, and required bridge changes; the feature branch therefore fast-forwarded to main without duplicating code.
- Retained three recovery stashes, including `codex-post-main-comparison-gh-20-2026-07-29`.
- Verified 98 affected tests plus Closing API/Batch and Config Server bootJars successfully.
- The broader suite found two unrelated main regressions in Master Data and Config Server Compose policy tests after Issue #66 removed/commented their root Compose services. These were documented, not fixed in the Closing scope.
- Issue #20 was already CLOSED. The user authorized main integration of this harness reconciliation through the PR path.
- Rollback for this handoff is the harness-only reconciliation commit. The already-main Closing implementation is not rolled back by applying an old stash.
## 2026-07-30 (Issue #41 BusinessPartner domain/JPA separation)

- Issue/branch/worktree: `#41`, `agent/41-business-partner-ddd`, `C:\dev\account\.worktrees\account-41-business-partner-ddd`; rebased onto `main@39dabd4e` after Issue #40 physically split Master Data into API/Core/Batch.
- PR #225 initially closed Issue #41 with documentation-only commit `cdb892e4`, but a closure audit reopened the Issue because production code was still missing. This branch is the verified implementation and its PR uses `Fixes #41`.
- Replaced JPA-coupled `BusinessPartner`/`BusinessPartnerAccount` with pure domain models and separate `BusinessPartnerJpaEntity`/`BusinessPartnerAccountJpaEntity`; preserved the existing tables, columns, indexes, child FK and REST paths, so no migration was added.
- Moved all domain↔JPA conversion into `JpaBusinessPartnerPersistenceAdapter`, kept application services behind `BusinessPartnerPersistencePort`, and changed cross-module query composition to consume that port rather than Spring Data.
- Added explicit SCD2 `closeVersion` versus business `terminate` semantics, defensive child-account invariants, fail-closed current/as-of lookup, API DTO mapping and registration-number masking.
- Updated the API and Batch composition roots to register the persistence-owned entity package, removed a duplicate/BOM-prefixed API entry point already superseded by `MasterDataApplication`, and changed the #40 batch integration fixture to persist Business Partners through the output port.
- Verification: Master Data Core 12, API 2, Batch 4, Expenditure API 1, Loan Core 30, and Journal Ledger integration 1 tests passed; API and Batch bootJars passed. Total observed: 50 tests, 0 failures/errors/skips.
- Independent review found and drove one correctness fix: next SCD2 versions now copy account values into new child rows with new IDs, preserving both current payment accounts and historical FK ownership. Domain, service-order, child merge, and overlap fail-closed tests cover the corrected contract.
- Independent post-rebase review and static checks found no remaining P0-P3 findings. PostgreSQL was not executed; overlap exclusion constraint and list pagination remain follow-ups.
- PR `#232` merged as `c720ae58` after Ready/mergeability checks, `Fixes #41` closed the reopened Issue automatically, and GitHub deleted the remote source branch. The source worktree is cleanup-eligible.
- Rollback: revert the Issue #41 follow-up implementation commit. No schema rollback is required.
## 2026-07-30 (Issue #228 canonical container image packaging)

- Worktree/branch: `C:\tmp\account-228-container-images`, `agent/228-container-images`, base `origin/main@5fb9cb67`.
- Unified root Java images on Gradle 8.7/Java 17 builder and Java 17 JRE runtime with validated exact project/path arguments, deterministic single executable JAR selection, non-root users and exec-form startup.
- Added `deploy/image-targets.json` for 35 Java runtime applications and one Next frontend. Internal Audit API/Batch remain disabled and linked to #73/#74/#231 rather than being represented as successful images.
- Converted all 19 active module Compose build blocks from divergent module Dockerfiles to the canonical root Containerfile and exact API/Batch/infra target arguments.
- Preserved the Next standalone runtime and same-origin `/api` default, removed bind mounts that hid its packaged `server.js`, and prevented recursive tracked `.env*`, ignored Spring local config and private-key/keystore files from entering image build contexts.
- Removed tracked DB defaults from active Auth/Account Mart/ECL module Compose files, made datasource inputs required and changed Auth DDL policy to `validate`.
- Added container package/build tooling, docs and policy tests. The verifier drains both output streams concurrently, emits a complete JSON report and returns non-zero when any enabled target fails.
- Verification passed Config Server/Gateway tests and all 33 enabled Java `bootJar`/single-artifact checks offline; 2 targets reported expected `BLOCKED`, and the frontend static contract passed.
- Actual Docker/Podman image build was not run: Docker is absent, Podman lacks required base images/Compose support, and no image pull or package install was authorized.
- Independent review and re-review drove root/frontend secret-context, Compose credential, stream-capture and frontend URL corrections; no unresolved finding remains.
- State: pushed to `origin/agent/228-container-images`; Draft PR `#240` opened with `Refs #228`. No merge or Issue close.
## 2026-08-11 (Issue #254 Asset Lease PostgreSQL baseline reintegration)

- `agent/254-asset-lease-postgres-reintegration` was created from `origin/main@ea6d8063` because PR #259 had merged only into an obsolete stacked base and its Asset Lease changes were absent from main.
- Restored independently runnable API/Batch local H2 profiles, dev/prod PostgreSQL-only profiles, executable packaging, complete six-table V20 parity, financial precision/nullability fixes, and domain validation matching the PostgreSQL lease-date check.
- Removed the standalone conflict marker inherited by main and locked the published H2 V20 bytes with SHA-256.
- Verification passed Core 19, API 2, Batch 1 and migration-runner 78 tests; both bootJars; the 183-task READY driver gate; and direct `java -jar --spring.profiles.active=local` startup for API and Batch with H2. Final independent review found no P0-P3 finding.
- Commit `39edbd26` is pushed and Draft PR #288 targets `main` with `Refs #254`. Real PostgreSQL clean migrate/JPA validate, merge and Issue close were not performed.
## 2026-08-11 (Issue #249 Budget runtime matrix)

- Added Budget API/Batch to local H2, dev/prod PostgreSQL, canonical image inventory, module Compose and buildless production Compose contracts.
- Local Gradle/JAR execution keeps Config/Discovery opt-in; Batch stays non-web, opt-in and job-disabled. Production requires shared Auth/Gateway/Budget JWT trust, a container-reachable token-version endpoint, Auth bootstrap secrets, immutable images and TLS-valid PostgreSQL URLs.
- Self-contained dev now provisions separate owner/runtime roles with different passwords. Runtime table/sequence grants run only after release migration and revoke every `flyway_schema_history*` privilege; no default DML grant or sequence `UPDATE` remains.
- Rebased onto main including PR #288. The combined Budget/Config/migration/runtime gate executed 195 tasks successfully; API `PASS_STARTED` and Batch `PASS_EXITED` local JAR smokes used H2, Flyway V50 and JPA validate. Validator self/template, shell syntax, diff/marker checks passed.
- Independent review findings were corrected and final review reported no P0-P3. Commit `d904c2ce` is pushed and Draft PR #289 targets main with `Refs #249`; no external DB/container/deployment was touched.

## 2026-08-11 (Issue #231 Internal Audit runtime boundary)

- Made `internal-audit:api` the executable Spring Boot composition root and kept `internal-audit:core` as a library; removed placeholder Auth/Internal Audit Batch applications because they had no real Job/Step.
- Added local H2 plus dev/prod PostgreSQL profiles, H2/PostgreSQL V60 migration parity, production TLS fail-fast validation, canonical image/Compose wiring and Gateway routing.
- Enforced RCM/Evaluation parent integrity and API error mapping before writes; moved web adapters out of Core and removed duplicated Audit/Security sources.
- Verification passed 244 affected tests and a 179-task combined Gradle/bootJar gate. The local executable JAR started with servlet, H2, Flyway V60 and JPA validate. Production template/image policy validation passed with 36 images and 17 PostgreSQL URLs.
- Independent review findings were corrected and final review reported no remaining P0-P3. Commit `5052163c` is pushed and Draft PR #338 targets main with `Refs #231`, `#73`, and `#74`.
- Docker was unavailable and no external development host was accessed, so real PostgreSQL migration, Compose startup and live Gateway/Eureka routing remain external verification gates.

## 2026-08-11 (Issue #341 remove source-less Gradle app)

- Removed only the `:app` include, which had no directory, build file, source, entrypoint or tests, and corrected current-state root/container/local-development text.
- `gradlew projects --offline` changed from 73 entries to 72 with set difference exactly `app`; runtime manifest and Compose targets were unchanged.
- Twenty-one Config/image/development/production policy tests, diff/marker checks and independent review passed with no P0-P3. No app directory or ignored build output was deleted.
- Commit `7b393665` is pushed and Draft PR `#346` targets `main` with `Refs #341` and `Refs #227`.

## 2026-08-11 (Issue #66 root development Compose topology)

- Created the full root development runtime for 36 enabled image targets: Config Server, Discovery, Gateway, Frontend, 17 APIs and 15 opt-in Batch applications.
- Added self-contained PostgreSQL/Redis/Kafka and external-dev no-infrastructure overlays, 17-context secret-safe probes, post-migration runtime privilege checks and a fail-closed PowerShell environment validator.
- Enforced dev PostgreSQL/JPA-validate/no-runtime-migration policy, internal-only business ports, health/dependency ordering, non-web/job-disabled Batch defaults and canonical Java/Frontend development images.
- Verification passed six Compose policy tests, Config/Gateway/migration-runner tests, 159 Gradle packaging tasks, shell/PowerShell/static checks and a Frontend production build with 122 routes and no development rewrite. Independent final review found no P0-P3.
- Docker was unavailable and Podman had no Compose provider; no external server, DB, secret or existing container was accessed. Live Compose and PostgreSQL privilege checks remain Issue #66 close gates. Rollback is a normal revert and Compose `down` without volume deletion.
- Commit `375105ab` is pushed and Draft PR `#339` targets `main` with `Refs #66`; the Issue remains open for approved live environment verification.

## 2026-08-11 (Issue #340 Expenditure/Tax test classpath)

- Fast-forwarded the isolated Issue worktree onto `origin/main@81f4206e` after confirming no overlap between upstream files and the three-file Gradle diff.
- Enabled an explicitly classified Tax API plain JAR for the Expenditure cross-API test while retaining the unclassified executable boot JAR; removed duplicate/unused Expenditure dependencies.
- Focused latest-main verification passed 45 tasks and 55 observed tests with no failure/error/skip. Tax packaging produced exactly one executable artifact and one small `-plain.jar`; container selection policy passed.
- The full root forced build passed the original #340 failure and executed 237 tasks before the known #344 Internal Audit explicit local datasource policy failure. Fresh evidence was posted to #344.
- Independent latest-main review found no P0-P3. PR `#350` merged as `aaad0c0d`; Issue #340 was closed and its active workflow/owner labels were removed.

## 2026-08-11 (Issue #348 multi-tool GitHub ownership)

- Audited GitHub workflow metadata and found only default labels, no configured Project Status, and no assignees or workflow labels on the active runtime Issues.
- Added eight approved status/ownership labels, assigned active Codex Issues to the authenticated repository owner, and synchronized Issues into in-progress, ready, blocked, or needs-review states with comments.
- Defined a shared Codex/Gemini/Claude Code protocol: only `status:ready` may be claimed, one Issue has one active writer, implementation ownership is one `agent:*` label plus a start comment, reviews remain read-only, and transfers require a parent-Integrator handoff.
- Created Issue #348 and a latest-remote external worktree at `origin/main@81f4206e`. The dirty primary checkout remains untouched and 15 commits behind; its uncommitted harness changes were not overwritten or copied.
- No GitHub Project or bot account was created. No package, secret, database, container, production/test code, or primary-checkout file was accessed or changed by this documentation implementation.
- Allowlist/link/label/Issue-state/diff/conflict-marker checks passed. Commit `34d14d34` is pushed and Draft PR `#349` is open with `Refs #348`.
- Issue #348 is now `status:needs-review`. Gemini or Claude Code can review the frozen diff read-only; merge and Issue close remain pending.
- The initial independent review found three P2 process defects. The remediation makes the parent Integrator the only GitHub mutator, completes every advertised ready-Issue contract, and supersedes the malformed #348 claim comment with an exact contract.
- Rebased onto `origin/main@aaad0c0d`; four append-only harness conflicts retained both #340 and #348 records. No production/test/build/runtime file conflicted.
- Remediation re-review found one remaining P2 in review/transfer wording and stale ready-Issue comments. The parent-only rule now covers Issue comments, handoff, review transitions and CLI mutation examples; #80/#343/#345/#347 each received a superseding parent-only coordination comment.
- Final independent re-review at `9a783fc1` passed with no P0-P3. PR #349 is CLEAN/MERGEABLE on latest main; the user authorized Ready/merge and the parent Integrator owns the final transition.
- PR #349 merged as `c4a50f17`; Issue #348 closed and its active status/owner labels were removed.

## 2026-08-11 (Issue #79 Loan API local H2 composition)

- Reused `agent/79-loan-api-local-h2` / `C:\tmp\account-79-loan-api-local-h2` and fast-forwarded its preserved two-file implementation without overlap to `origin/main@c4a50f17`; the dirty primary checkout remained untouched.
- Explicitly registered Master Data persistence entities plus shared audit/security entity and repository packages required by the existing local adapters, without broad root scanning or changes to Loan financial/domain behavior.
- Added a `local` H2 context regression test for the real Loan API composition root and required Master Data managed types.
- Latest-main 24-task gate passed: 42 Loan Core/API tests in 14 suites, zero failure/error/skip, API bootJar, and direct executable-JAR H2 startup with `Started LoanApplication`. Diff/marker/allowlist checks passed.
- Independent review found no P0-P3. PostgreSQL schema parity, Loan Batch startup and CI/container smoke remain separate gates; no external environment was accessed.
- Commit `09b72f21` is pushed and Draft PR #352 targets main with `Refs #79/#227`. Issue #79 is `status:needs-review`; merge and close remain gated.
- Final PR-head review passed with no P0-P3. PR #352 merged as `4fa50cc8`; Issue #79 closed and active status/owner labels were removed.

## 2026-08-11 (Issue #342 Reconciliation local health)

- Reused `agent/342-reconciliation-local-health` / `C:\tmp\account-342-reconciliation-local-health` and fast-forwarded its preserved three-file change without overlap to `origin/main@4fa50cc8`; the dirty primary checkout remained untouched.
- Added local-only health probes and disabled the unused Redis health contributor, leaving dev/prod resources and Reconciliation business/runtime code unchanged.
- Added a Spring configuration policy test for exact local versus dev/prod properties and documented Redis-free health/readiness behavior.
- Latest-main 34 Core/API tests in 9 suites, API bootJar, and direct executable-JAR health/readiness HTTP 200+UP passed. Diff/marker/allowlist checks passed.
- Independent review found no P0-P3. Real Redis-backed dev/prod and automated container/CI smoke remain outside scope; no external environment was accessed.
- Commit `d0698585` is pushed and Draft PR #353 targets main with `Refs #342/#227`. Issue #342 is `status:needs-review`; merge and close remain gated.
- Final PR review found one P3: whole-commit rollback would remove accurate #79 integration history co-located in `d0698585`. Corrected the contract to revert only the three #342 paths through a reviewed follow-up while preserving append-only harness records; runtime/test review remains clear.
- Independent re-review found no P0-P3. PR #353 merged as `cf50e4fc`; Issue #342 closed and active labels were removed.

## 2026-08-11 (Issue #344 Internal Audit explicit local policy)

- Reused `agent/344-internal-audit-local-h2` / `C:\tmp\account-344-internal-audit-local-h2` and fast-forwarded its preserved two-file change without overlap from `81f4206e` to `origin/main@cf50e4fc`; the dirty primary checkout remained untouched.
- Confirmed the production `application-local.yml` was already integrated by #231. Strengthened the focused policy test for local H2 PostgreSQL mode, Flyway V60, JPA validate, disabled SQL init/control-plane clients, and clarified that local in-memory data is ephemeral.
- Focused and full Internal Audit Core/API gates passed 17 tests in 6 suites, zero failure/error/skip, API bootJar, and direct executable-JAR startup with active local profile, H2, Flyway V60, JPA and `Started InternalAuditApiApplication`.
- The first smoke checker searched for an obsolete class name and falsely returned failure despite a successful start; corrected evaluation of the same log passed all checks. Diff/marker/allowlist checks passed.
- No production resource, migration, dev/prod behavior, external PostgreSQL, credential, private URL, Compose stack or existing container changed. Independent read-only review remains before commit/PR.
- Independent review found one P3 because the test helper masked three local control-plane values. The fix limits those overrides to dev/prod test contexts, so local assertions now exercise the tracked profile; focused and full module gates reran successfully and independent re-review remains.
- A root `build --offline --rerun-tasks --max-workers=1` attempt exceeded the five-minute command timeout without an observed failure. It is recorded as timed out, not passed; no external system was involved.
- Repeated the root forced build with a ten-minute limit; it passed in 8m31s with all 349 actionable tasks executed. This clears the earlier #340/#344 full-root stopping point on the tested base.
- Independent remediation re-review found no P0-P3. A newer `origin/main` overlaps three append-only harness logs, so latest-main synchronization and focused revalidation remain before commit/Draft PR.
- Synchronized through a named stash and fast-forward to `origin/main@b2d5c6ef`. Resolved the sole `agent-status.md` conflict by preserving upstream #312/#314 and local #344/#342 records; auto-merged logs preserved both histories. The reviewed Internal Audit paths were byte-identical and the focused policy test/static gates passed again.
- Commit `e4a86d67` is pushed and Draft PR #357 targets main with `Refs #344/#227`. Issue #344 is `status:needs-review`; Ready/merge/close remain pending a final PR-head gate.
- Final PR-head review found no P0-P3. PR #357 merged to main as `21395eb3`; Issue #344 closed and active owner/status labels were removed.

## 2026-08-12 (Issue #90 Asset Lease Batch explicit local profile)

- Reused `agent/90-asset-lease-batch-local` / `C:\tmp\account-90-asset-lease-batch-local`, preserved the prior restart-validator experiment in a named stash and fast-forwarded without implementation overlap to `origin/main@43f5b36c`. The dirty primary checkout remained untouched.
- Current-main baseline Batch tests/bootJar and a profile-only packaged-JAR smoke passed, proving PR #288 had already fixed the historical startup defect. The remaining gap was an incomplete Batch-owned local policy and missing real-profile regression coverage.
- Made `application-local.yml` explicitly own H2 PostgreSQL mode, create-drop JPA, Batch metadata initialization, jobs-off and disabled control-plane/Kafka-listener/tracing behavior. Added an unmasked real local context test and simplified IntelliJ/docs to profile-only execution.
- Latest-main Core/Batch 21 tests in 8 suites, zero failure/error/skip, Batch bootJar, and bounded packaged-JAR startup passed. The smoke observed H2/JPA/Batch metadata/start, no Job launch, no external attempt and no startup failure. Diff/marker/allowlist gates passed.
- The first focused test failed only from unsupported JUnit method-parameter injection; field injection fixed the test harness. Dev/prod PostgreSQL resources, depreciation logic and Job parameter semantics remain unchanged. Independent read-only review remains.
- Independent review found no P0-P3. Commit `65396222` is pushed and Draft PR #359 targets main with `Refs #90/#227`; Issue #90 is `status:needs-review` pending final PR-head verification.
- The first final PR-head review passed and PR #359 was promoted Ready, but concurrent Discovery PR #360 advanced main and GitHub rejected the now-conflicting merge. Merged `origin/main@191c5c28`, preserved #90/#304 append-only records and reran the focused local-context test successfully; final PR-head review must repeat after the sync commit.

## 2026-08-14 (Issue #227 latest-main runtime audit)

- Fast-forwarded the isolated Issue worktree to `origin/main@1ae9e108` through a named safety stash; no conflict occurred and the dirty primary checkout was untouched.
- Classified 72 Gradle subprojects as 17 API, 15 Batch, 3 infra, 1 CLI, 19 libraries and 17 aggregators. Inventory/task contract, all 36 executable packages and all 36 library/aggregator `test+jar` boundaries passed.
- Strict ProfileJar across 36 artifacts is 16 `PASS_STARTED`, 15 `PASS_EXITED`, and five fail-closed results. Auth/Budget/Gateway require secure local JWT input; Closing API/Batch expose `FiscalPeriodMapper`/`JournalPostingPort` composition gaps.
- Opened unclaimed `status:ready` #420-#424 for Auth, Gateway, Closing API, Closing Batch and the stale Production Compose runbook test path. Existing #249 owns Budget; #66/#228/#230 own live Compose/image/PostgreSQL. Closed #362 was not duplicated because PR #398 already integrated the Frontend Node 20/lifecycle contract.
- Rebuilt the runtime matrix from current JSON evidence and corrected the local guide. Java Compose mapping is development 36/36 and production server targets 35/35; Podman still has no Compose provider, so no live Compose or PostgreSQL operation occurred.
- Hardened the audit tool with bounded Gradle/process-tree handling, concurrent output capture, isolated environment/user home, precise evidence redaction, single executable-JAR enforcement, CLI coverage, stable arrays and failure exit codes.
- No external host, credential, DB, container or volume was accessed. Parser/diff/marker checks pass; focused Compose/image policy execution is 20/21 with the single stale-path failure tracked by #424. The unaffected policy subset passes, and dynamic redaction/timeout/process-tree checks pass. Independent review precedes commit/push/Draft PR.
- Final review identified termination-error fail-open, raw diagnostic JWT command evidence, inaccurate timeout wording and unrelated #90 harness scope. All four were remediated; dynamic process-tree, JDK/source preservation, endpoint/credential redaction and Budget LocalJar command checks pass. Independent re-review remains.
- Re-review's final P2 found user-home, IPv6 and `.java`-suffixed endpoint gaps. Redaction is now context-aware and adversarial samples plus real Budget LocalJar JSON confirm no user path, endpoint or diagnostic JWT leakage. Final re-review remains.
- A further bypass review constrained stack source tokens and covered compressed/zone-id IPv6. Parenthesized `.java` endpoints and both IPv6 forms redact; source/JDK/loopback utility remains. Adversarial and real Budget JSON checks pass; final re-review remains.
- Final independent re-review found no P0-P3. Commit/push/Draft PR and final PR-head merge gate are next.
- Commit `adade958` is pushed and Draft PR #433 targets `main`; Issue #227 is `status:needs-review` pending the final PR-head merge gate.

## 2026-08-14 (Issue #420 Auth secure local H2 runtime)

- Claimed #420, created `agent/420-auth-local-runtime` in the external worktree and reproduced the two baseline Auth local-runtime failures. The dirty primary checkout was not modified.
- Added the explicit local H2/Flyway/JPA/control-plane policy without tracked credentials. Base/local/dev/prod retain fail-closed JWT/internal-token requirements and tests use only generated ephemeral values.
- Updated runtime policy/integration tests and Auth local/schema guidance. Review findings corrected the 8081 commands, V73/security documentation and actual profile-binding coverage for both missing credential guards.
- Fast-forwarded to non-overlapping `origin/main@db5c865c`. Auth Core/API passed 45 tests in 14 suites, API bootJar passed, and the executable JAR passed missing-input fail-closed plus ephemeral-input local-start smokes.
- Static gates passed and final independent review found no P0-P3. No external endpoint, DB, credential, container or volume was accessed.
- Force-added the ignored local resource by exact path, verified its non-empty staged blob, committed `62cc8717`, pushed the branch and opened Draft PR #441. Issue #420 is `status:needs-review`.
- The first PR-head review found no implementation/security finding and one P3 stale-harness state. This harness-only follow-up records the published PR state; independent re-review and green checks remain the Ready/merge gate, and #427 is reevaluated only after #420 integration.

## 2026-08-14 (Issue #421 Gateway secure standalone local runtime)

- Claimed #421 and created `agent/421-gateway-local-jwt` in an external worktree from `origin/main@eb7ce92b`; the dirty primary checkout was not modified.
- Baseline packaging passed and missing input failed closed, while generated-input profile-only startup still attempted Eureka. Added a tracked-intent local profile that disables external control-plane and token-version calls without storing any key or endpoint.
- Updated the standalone IntelliJ entry, Gateway/root local guidance and actual-profile tests. Fixed test secret literals were replaced with generated values, and the tests reject any JWT/secret/public-key/JWKS/base-url section in the local resource.
- Gateway passed 43 tests in 9 suites, bootJar and both packaged-JAR paths. Ephemeral-input startup observed no Config/Eureka attempt; static credential/resource/diff/marker gates passed.
- Review findings for exact allowlist evidence, literal-secret coverage and local troubleshooting were remediated; final independent re-review found no P0-P3. No external endpoint, credential, DB, container or volume was accessed.
- Force-added the ignored local resource by exact path, verified its non-empty staged blob, committed `f066eb31`, pushed the branch and opened Draft PR #445. Issue #421 is `status:needs-review`.
- The first PR-head review found no implementation/security finding and one P3 stale-harness state. This harness-only follow-up records the published PR state; independent re-review and green checks remain the Ready/merge gate.

## 2026-08-14 (Issue #422 Closing API Master Data composition)

- Claimed #422 and created `agent/422-closing-api-local-mapper` from `origin/main@305fa259` in an external worktree; the dirty primary checkout was not modified.
- Reproduced the missing `FiscalPeriodMapper` failure. Replacing the broad Master Data adapter scan with explicit Closing-used contract adapters and their minimal persistence/mapper dependency closure also removed the next hidden `AccountSubjectPersistencePort` startup failure.
- Closing Core/API passed 74 tests in 19 suites, API bootJar passed and the packaged local H2 JAR started without mapper failure or external attempt. Exact implementation allowlist, diff and marker gates passed.
- Independent implementation review found no P0-P3 and confirmed production adapters are neither duplicated nor shadowed. No business/domain or Master Data production source changed; no external state was accessed.
- Commit `325b6e98` is pushed, Draft PR #447 is open and Issue #422 is `status:needs-review`. Independent PR-head review and green GitHub checks remain the Ready/merge gate.

## 2026-08-14 (Issue #423 Closing Batch local Journal composition)

- Claimed #423 and created `agent/423-closing-batch-local-journal` in an external worktree, leaving the dirty primary checkout untouched. The branch is stacked on PR #447 so the API and Batch fixes remain separately reviewable while clearing their shared Closing CI gate.
- Added explicit Batch composition for the existing local Journal ports, restricted the fallback configuration to `local`, and verified that approved port beans make the fallback back off.
- Replaced broad Master Data adapter scanning with the exact exchange-rate, fiscal-period and master-data adapters plus their minimal persistence/mapper closure.
- Closing API/Batch/Core passed 90 tests in 27 suites, Batch `bootJar` passed, and the packaged local H2 JAR started without missing ports or external attempts. No actual Job, DB, endpoint, credential, container or volume was used.
- Independent review's P3 test-coverage finding was remediated and final re-review found no P0-P3. Commit `8a582592` is pushed and stacked Draft PR #448 is open; Issue #423 is `status:needs-review` pending PR-head checks and merge into the #422 branch.
- PR #448 subsequently passed all GitHub checks and final PR-head review, then merged into the #422 branch as `dbedb96f`. Refreshed main-target PR #447 now owns the combined Closing API/Batch integration gate; neither Issue closes before that PR reaches main.

## 2026-08-21 (Issue #66 PR #407 container topology conflict resolution)

- Identified the user's “PR 66” as existing PR #407 for Issue #66; it is a separate follow-up from the earlier Codex PR #339. Reused its isolated worktree and left the dirty primary checkout untouched.
- Merged latest `origin/main@07499d93` and reconciled the semantic image-policy conflict: 35 manifest Java targets use their module Dockerfiles, while migration-runner remains the explicit central `Containerfile` exception.
- Cancelled stale PR-only compose/script/docs and unrelated stale-branch application changes. The final main-relative scope is container image definitions, Compose selection, manifest/tooling, policy tests and runbooks.
- Config Server policy tests passed 37/37 and package verification passed 35/35 with a single non-plain executable JAR per Java target. Independent review findings were remediated and final re-review found no P0-P3; refreshed GitHub checks remain.
- No external DB, credential, private endpoint, Docker/Compose service, registry, container or volume was accessed. Issue #66 remains blocked on approved live Compose/PostgreSQL verification.
- Safely removed only the clean detached completed worktree `C:\tmp\account-487-merge`; all dirty, divergent, active and other-agent worktrees were retained.
- The first refreshed CI found three stale Compose policy assertions in Budget, Gateway and Internal Audit. Updated the exact module-Dockerfile expectations and Budget runbook; 7 focused tests and the six-project CI-equivalent set passed 123 tests in 29 suites, and independent remediation review found no P0-P3. A new GitHub run remains the merge gate.

## 2026-08-21 (Issue #435 Config Server ENCRYPT_KEY fail-closed)

- Claimed #435 for Codex, restored its rejected PR #511 branch in `C:\tmp\account-435-config-key-fail-closed`, and merged latest `origin/main@0b2280fd` without changing the dirty primary checkout or another agent's worktree.
- Replaced the empty key fallback with a composition-root startup guard that rejects missing/empty/whitespace/surrounding-whitespace input before HTTP context creation and never includes the input in its diagnostic.
- Added generated-input context coverage, required root/module Compose interpolation, a blank env template contract and safe local cleanup guidance.
- Config Server passed 45 tests in 7 suites and bootJar. Packaged-JAR missing/empty/whitespace failures and generated-input startup passed, as did module Compose render and fallback/diff/marker scans.
- No actual secret or external state was accessed. Independent review, exact-path commit/push, PR #511 reopen, fresh CI and final merge review remain.
- The first independent review found the production Compose and canonical env templates omitted the new key contract. Added required production forwarding, blank dev/external-dev/prod template entries and validator enforcement; revalidation and re-review remain.
- Development and production validator checks plus root development Compose rendering passed. The production Compose renderer is blocked later by a pre-existing pids-limit model conflict unrelated to the encryption-key contract, which remains a separate follow-up.
- Re-review identified PowerShell truthiness accepting whitespace-only production key input. The validator now shares the startup guard's blank/whitespace semantics and self-tests the actual/template distinction.
- Opened follow-up Issue #530 (`status:ready`) for the unrelated production Compose pids-limit model conflict.
- Final independent re-review found no P0-P3; publication and remote-head CI/review gates remain.
- Pushed `1ef36b71` and reopened PR #511. Main advanced to `1025417b`; resolved only four append-only harness conflicts by preserving both #435 and upstream #462/Auth records, then force-reran Config Server 45/45 and bootJar successfully.
- Pushed latest-main merge `70ca599c`; PR #511 is open/non-draft/MERGEABLE on exact main `1025417b`, pending fresh CI and final remote-head review.
## 2026-08-21 (Issue #462 Auth LDAP OTP fail-closed)

- Claimed #462 on `agent/462-auth-otp-fail-closed` in an external worktree from `origin/main@0b2280fd`; unrelated checkouts and running containers were untouched.
- Replaced the fixed `123456` LDAP OTP success condition with a provider-unavailable fail-closed path that records an internal reason, returns the existing generic credentials failure and never reaches token issuance.
- Added focused legacy/alternate OTP and no-JWT assertions while preserving the existing normal-login test, then aligned Auth README and process-flow documentation.
- Static gates passed and independent review found no P0-P3. Host Java 17 is absent; the offline cached JDK 17 container lacks `jjwt-api:0.11.5`, so no local test result is claimed and no package was downloaded.
- Commit `1b4407f7` is pushed, Draft PR #525 is open with `Refs #462/#515`, and Issue #462 is `status:needs-review`. GitHub's actual Auth Core/API test tasks remain the Ready/merge gate; API packaging/build configuration is unchanged and local bootJar is explicitly unexecuted.
- Final PR-head review found a P1 in the first handoff: it incorrectly claimed Module Validation runs bootJar with one worker. The records now match the workflow's Core/API test tasks and disclose the omitted packaging evidence; independent remediation re-review remains.
- Final JDK 17 Auth Core/API CI and remediation re-review passed; PR #525 squash-merged as `1025417b`, Issue #462 closed, and active labels were removed.

## 2026-08-21 (Issue #518 Auth client-selected login type fail-closed)

- Claimed #518 in `agent/518-auth-sso-fail-closed` from merged #462 head `1025417b`, using an external worktree and leaving unrelated checkouts untouched.
- Restricted success to explicit case-insensitive NORMAL. SSO and unsupported login types terminate before user, credential, login-attempt and token adapters; LDAP preserves password verification and provider-unavailable failure.
- Added adapter non-invocation, repeated unsupported request, lowercase normal and retained LDAP tests, and aligned Auth documents.
- Independent review found and remediation fixed a medium lockout DoS caused by counting provider/contract failures as credential failures. Re-review found no P0-P3; static gates pass.
- Local JDK 17 execution is unavailable and the offline constrained container lacks one cached dependency. Commit `20e1806e` is pushed, Draft PR #531 is open, and final-head Auth Core/API CI remains the merge gate; bootJar is not required for unchanged packaging/build configuration.

## 2026-08-22 (Issues #518 integration and #535 Frontend ESLint compatibility)

- Final Auth Core/API CI and final-head review passed for #518; PR #531 squash-merged as `b6b43031`, Issue #518 closed, and its active labels were removed.
- Isolated #519 after TypeScript passed but Node 20 lint failed before source analysis on the repository's extensionless Next ESLint ESM imports. Its network-disabled build reached the existing Google Fonts fetch and stopped there without a code failure; #535 now owns only the lint blocker.
- Updated only `frontend/eslint.config.mjs` on `agent/535-frontend-eslint-esm`: `FlatCompat` loads the locked legacy Next config, a conditional compatibility rule covers the installed hooks plugin gap, and two legacy rule classes remain visible as warnings. #536 tracks cleanup and is non-blocking.
- Network-disabled Node 20.20.2 verification with CPU 1 / memory 2 GiB passed full lint at 0 errors / 245 warnings and TypeScript at exit 0. Failed intermediate import and initial 31-error results are disclosed; package, lock, runtime source and container state remain unchanged.
- Independent review found no P1/P2 or merge-blocking finding. Commit `c2364782` is pushed, Draft PR #537 is open, and Issue #535 is `status:needs-review`; final PR-head checks/review gate Ready/merge and then #519 resumes.

## 2026-08-22 (Issues #535 integration and #538 offline Frontend font build)

- Corrected the final #535 P3 PR-body omission, passed remote-head review, all checks and the Ready-event Guard, then squash-merged PR #537 as `7364a926`; Issue #535 closed and active labels were removed.
- After #519 passed lint and TypeScript on the merged config, its required network-none build still stopped only on the root Google Inter fetch. Split that prerequisite to #538 and preserved the six login files in the isolated blocked worktree.
- #538 removes only the `next/font/google` Inter dependency and generated body class while retaining the existing system font stack and root layout contracts. A concurrent CI-only main advancement was fast-forwarded without conflict before publication.
- CPU-1/memory-2-GiB/network-none Node 20 validation passed lint at 0 errors / 245 warnings, TypeScript and a complete production build with 122/122 static pages. Two read-only-mount EROFS attempts are disclosed; the final isolated writable-worktree/separate-output run exited 0 without OOM.
- Independent review found no P0-P3; exact static gates passed. Temporary verification containers and about 983 MiB of output were removed. Commit `d7397aef` is pushed, Draft PR #539 is open, and Issue #538 is `status:needs-review` pending final PR-head gates before #519 resumes.

## 2026-08-22 (Issues #538 integration and #519 Frontend password login contract)

- Final remote-head review, GitHub checks and Ready-event Guard passed for #538; PR #539 squash-merged as `9c62b8e6`, Issue #538 closed, and active labels were removed.
- Synced the preserved six-file #519 change to that main without conflict. Login now offers only provisioned username/password, blocks blank fetches, calls exact same-origin Auth through Gateway with explicit NORMAL, and removes demo/unsupported modes plus raw JWT display and `user_info` token duplication.
- Independent review found P3 partial-session/error semantics on browser storage failure and missing async accessibility status. The remediation separates failures, best-effort clears both keys, blocks success/redirect and adds alert/status/aria-busy; latest-main re-review found no P0-P3.
- CPU-1/memory-2-GiB/network-none Node 20 full lint passed at 0 errors / 244 warnings, TypeScript passed, and production build exited 0/OOM false with a 118-second compile and 122/122 static pages. Empty JSON POSTs at Frontend, Gateway and Auth all returned the same HTTP 400 validation response.
- Exact static gates passed; the test container and 541 MiB output were removed. #540 tracks the remaining localStorage JWT/legacy fallback migration. Commit `1cf73e3d` is pushed, Draft PR #541 is open, and Issue #519 is `status:needs-review` pending final remote-head gates.

## 2026-08-22 (Issues #519 integration and #497 Gateway discovery-route bypass)

- Final #519 remote-head review, GitHub checks and Ready-event Guard passed; PR #541 squash-merged as `a0e0f8a6`, Issue #519 closed, and active labels were removed. #540 remains the explicit localStorage-to-HttpOnly follow-up.
- Reproduced #497 on the unchanged dev Gateway without credentials: normal GET `/api/auth/login` was 401 with the JWT error, but `/auth-service/api/auth/login` reached discovery/rate-limit processing and returned 500 without that error.
- Disabled discovery locator in both packaged/external Gateway configs, removed locator-only lower-case options and retained Eureka registration/fetch plus ten explicit `lb://` route targets. Tests parse both configs and assert the full explicit route contract; docs distinguish registry participation from external route generation.
- Independent review's two P3s for route URI coverage and Eureka wording were remediated; re-review found no P0-P3. Exact YAML/diff/marker/secret/alternate-enable gates pass.
- Local CPU-1/memory-1536-MiB/network-none JDK 17 Gradle stopped before compilation on uncached existing Spring/JJWT artifacts, with no download or success claim. Commit `e1456136` is pushed, Draft PR #542 is open, and Issue #497 is `status:needs-review` pending GitHub `:gateway:test` and final remote-head gates. #520 owns post-restart 404 smoke; #466 owns missing explicit business routes.

## 2026-08-22 (Issue #497 integration, #520 preflight and #543 Gateway Compose JWT input)

- Final Gateway CI, remote-head review and Ready-event Guard passed for #497; PR #542 squash-merged as `54003994`, Issue #497 closed, and active labels were removed.
- #520's read-only Linux preflight confirmed Podman/Compose and the unchanged minimal containers, but the approved external-dev env path is absent. No secret-bearing environment, DB, image, network or service state was changed. PowerShell is unavailable, so #544 owns a package-free Python minimal validator.
- Found that the module Gateway Compose used a repository-fixed JWT verification key. Split P0 #543, removed the tracked key, required exact external fail-closed interpolation, strengthened the Compose policy test and documented coordinated Auth/Gateway rotation without exposing a value.
- Missing-input quiet Compose render failed and process-only generated-input render passed without value/config output. Exact static gates and independent review found no P0-P3; no build/up/restart occurred.
- Commit `b1d50065` is pushed and Draft PR #545 is open. GitHub JDK 17 `:gateway:test` plus final remote-head/Ready-event gates remain before merge; actual secret-backed rotation and runtime smoke remain blocked in #520.

## 2026-08-27 (Issue #543 integration and #544 Linux minimal Auth env validator)

- #543 passed final Gateway CI, independent remote-head review and the Ready-event Guard; PR #545 squash-merged as `a89ac260`, Issue #543 closed and active labels were removed. No running Auth/Gateway service was recreated, so actual key rotation remains #520 work.
- Added one Python 3 standard-library validator for the bounded 11-key Auth/Master Data external-input preflight. It redacts values/paths, rejects unsafe dotenv syntax and placeholders, enforces exact DB targets/runtime roles and external host/JDBC contracts, and opens only secure regular non-symlink files with identity checks.
- Independent test/review found frozen-exception classification, local Compose alias, JDBC delimiter, DEV IPv6, non-ASCII port and control-character gaps. All were remediated. In-memory compile, self-test 165, independent adversarial 91 and final P0-P3-clean review pass.
- Commit `2a0b6ebd` is pushed and Draft PR #551 is open. No actual env/container/DB/image state was accessed. #520 remains blocked on an approved env and on reconciling the root Compose requirement for `AUTH_DEFAULT_PASSWORD` plus all 17 DB contexts; this validator does not claim full-root render readiness.

## 2026-08-28 (Issue #520 low-resource external-dev Auth stack)

- Created an isolated seven-container Compose path for the two-context external PostgreSQL/Redis gate, Config Server, Discovery, Auth, Master Data, Gateway and Frontend. It excludes every Batch, unrelated API, Kafka and observability service and publishes only loopback ports 13000/18000.
- Added sequential one-worker image tooling, explicit external PostgreSQL runtime hardening, 3.95-CPU/about-6-GiB limits, tracing-off console logging, env identity/symlink checks, legacy Compose-label conflict detection, engine-specific stats, bounded Eureka retry and fixed-project failure rollback.
- Runner tests pass 7/7, validator self-test passes 165, fixture Compose renders and static gates pass. A temporary resource-bounded probe received existing Redis `PONG` and removed itself. Review-found retry, rollback, symlink, legacy-name, Docker stats, Redis and logging gaps were fixed; final independent review found no P0-P3.
- Local JDK 17 policy execution is blocked by the unavailable toolchain/offline missing OW2 POM, with no download. Draft PR #576 nevertheless passed all four GitHub checks, including JDK 17 Config Server validation. The ignored mode-600 `.env.external-dev` now has newly generated local-only encryption/JWT/internal-token values without disclosure, but the redacting validator stops at blank `AUTH_DB_URL`; no image build, external DB access, new-stack start, login smoke or legacy-target stop is claimed, and Ready/merge remains gated on the eight approved external DB inputs.

## 2026-08-29 (Issue #561 Logstash development pipeline recovery)

- Claimed #561 on `agent/561-logstash-pipeline` in an isolated worktree from `main@38ad309c`. Added a bounded one-service development Compose project with exact read-only config/pipeline mounts, JSON TCP 5000 to a development-only Elasticsearch index, internal-only ports, a 256 MiB JVM heap and 0.5 CPU/640 MiB/256-pid caps.
- Live validation passes: Compose quiet render, Logstash syntax, start/stop/recovery, mounts, TCP 5000, closed 5044, no host port, healthy/restart0/OOM-false state, and redacted synthetic count plus pipeline in/out 1/1. The old stateless no-mount container became stuck after SIGTERM and only that exact PID-less container was force-removed; no volume or Elasticsearch data was removed.
- Independent review found a P2 because HTTP 200 alone could accept red/timed-out Elasticsearch. Health now requires curl success, `timed_out=false` and yellow/green status; green is accepted while red/timeout fixtures are rejected. The policy test enforces those predicates and final independent re-review found no P0-P3.
- Local Java success is not claimed: host Java 17 is absent and the cached network-disabled JDK 17 Gradle run lacks one existing OW2 POM. No dependency/image download occurred; GitHub Module Validation remains required before integration. Commit `c48bca76` is pushed, Draft PR #585 is open, Issue #561 is `status:needs-review`, and rollback remains service-scoped without prune or volume deletion.
- GitHub JDK 17 Config Server validation and all four checks passed on final head `3e23cf65`. Independent exact-head review found no P0-P3, the Ready-event Guard passed, and PR #585 squash-merged as `67c53f2f`; Issue #561 closed with active labels removed. The bounded live Logstash service remains healthy.

## 2026-08-30 (Issues #592/#593 external-dev PostgreSQL migration verification)

- Reused the existing PostgreSQL 16.13 and Redis containers while preserving volumes and prior data. Canonical Auth/Master Data databases and least-privilege owner/runtime roles are ready, and ignored mode-600 external-dev files hold local-only inputs without value disclosure.
- Reproduced migration-runner SQLSTATE `22023` at the nullable `character_maximum_length` read and isolated the repair as #593. The verifier now uses `getLong`/`wasNull`; tests cover positive, zero and SQL NULL values, enforce call order and prohibit the failing typed `getObject` path.
- CPU-1/memory-1-GiB JDK-17 verification passed all 85 migration-runner tests. The rebuilt runner passed migrate/validate for both canonical databases against PostgreSQL 16.13.
- The existing runtime-grant script missed one identity sequence. Current DB privileges were completed safely and the final two-database/Redis gate passed; #594 separately tracks the reusable enumeration fix. No volume, database or existing record was deleted.
- Static gates pass. Independent review and #593 PR/CI/merge remain before the now-unblocked #520 six-image sequential build and external-dev cutover.

## 2026-08-30 (Issues #593 integration and #596 external-dev Frontend runtime)

- #593 passed independent review and all GitHub checks; PR #595 squash-merged as `89a770cc` and the Issue closed. #594 retains the reusable identity-sequence grant enumeration follow-up.
- Completed #520's approved 11-key preflight, Auth/Master Data PostgreSQL 16.13 migrate/validate, two-DB/Redis gate and six-image sequential build without disclosing inputs. Existing database and cache containers, volumes and data were preserved.
- Split #596 after the first real rootless cutover exposed EACCES from the host `/app` bind. Removed that bind, retained only named write volumes and moved readiness from `/` to bounded public `/next.svg`; the separate value-redacting smoke still proves Frontend → Gateway → Auth.
- A second attempt proved permissions but exposed the former 768 MiB cap during the 4,726-module first compile; both failed attempts used project-scoped rollback and restored the retained legacy six. The final 1 GiB run is healthy with restart 0/OOM false, `/login` HTTP 200 and repeat authentication smoke PASS. Seven cgroup max events document a slow-first-compile residual risk without OOM.
- Python tests pass 7/7, actual preflight/live smoke pass, and the focused JDK-17 policy test passes with CPU 1, memory 1.5 GiB and one Gradle worker. Independent review's P3 beginner cutover/restore command omission was remediated and final re-review found no P0-P3. Rootless Podman is the live-evidence engine; PR checks and merge remain before #520/#592 closure.

## 2026-08-30 (Issue #596 integration and #594 identity-sequence grants)

- #596 passed all checks, exact-head review and the Ready-event Guard; PR #597 squash-merged as `866c4de0`. Issues #596/#520/#592 closed, and the healthy seven-container external-dev runtime plus retained legacy rollback targets remain.
- #594 replaces the incomplete `information_schema.sequences` enumeration with public-schema `pg_class`/`pg_namespace` rows whose relkind is `S`, aligning grant and health-gate catalogs without changing tables, migrations or default privileges.
- A CPU-0.5/memory-384-MiB temporary PostgreSQL 16 test passed ordinary/SERIAL/identity coverage, two-run idempotency, zero sequence/table gaps and zero Flyway exposure, then auto-removed without a volume. Actual Auth/Master Data grant rerun and the live two-DB/Redis gate also passed without service or data removal.
- Shell syntax/diff gates and the final exact-assertion CPU-1/memory-1.5-GiB JDK-17 policy test pass. Independent review's two documentation P3s were remediated and re-review found no P0-P3. New sequences require grant/gate rerun and non-public schemas remain out of scope; #594 PR/CI/merge remain before the next observability issue.

## 2026-08-30 (Issue #594 integration and #601 Prometheus live targets)

- #594 passed all GitHub/exact-head/Ready-event gates; PR #600 squash-merged as `5f84c30b` and the Issue closed while actual DB/application gates remained healthy.
- Audited closed #562 and found only static merge evidence: the tracked targets used stopped legacy/host duplicates and the existing unbounded `/tmp`-configured `prometheus` container remained Created. All five actual `minimal-*` metrics endpoints passed, so #601 isolates the live repair without deleting that legacy container.
- Added a unique pinned Prometheus 3.14.0 project with loopback 19090, read-only config, separate data volume, 0.5 CPU/512 MiB/128 pids and exact self plus five minimal targets. Compose render, promtool and final JDK-17 policy pass; the only intermediate failure was a remediated map-order-only test assertion.
- Live and post-restart gates repeatedly report 6 targets up/0 down, healthy/restart0/OOM-false and about 31 MiB observed memory. Exact stop preserved the volume, legacy container and seven application health states. Review-found lifecycle/recreate P2s and duplicate-job/rootfs-evidence P3s were remediated; lifecycle-disabled live 6/6 and strengthened JDK-17 policy pass, and final re-review found no P0-P3. Docker live, digest and TSDB-size limits remain disclosed; #601 PR/CI/merge precedes the newly prioritized #540 work.

## 2026-08-31 (Issue #540 HttpOnly BFF and #607 stacked security gate)

- Implemented a Next.js Route Handler BFF on isolated `agent/540-http-only-bff`: browser code no longer reads or writes JWTs, the server stores the token in an HttpOnly/SameSite Strict cookie, reconstructs Gateway Authorization and clears the cookie on logout or upstream 401. Runtime `GATEWAY_INTERNAL_URL` replaces the former build-time rewrite/build-argument coupling.
- Same-origin mutation checks, request/response header allowlists, redirect blocking, login response validation and 16-KiB/1-MiB streamed body limits are covered. PAT/governance override and mock behavior was restored as out of scope.
- CPU/memory/network-bounded TypeScript passes, the final development live flow passes 12/12 and ESLint remains 0 errors/244 inherited warnings. Earlier final-page production build and live-cookie checks passed before the last review remediation and must be repeated for the publication head.
- Independent review identified that the Gateway IP rate limiter collapses browser users behind one BFF address and that arbitrary `X-Forwarded-For` is unsafe. Merge-blocking Issue #607 was created for an opaque, short-lived HMAC-signed login key plus verified-principal buckets in a dedicated stacked branch/worktree. #540 remains in progress until #607 integration, final production validation, independent re-review, CI and Ready-event gates pass.

## 2026-09-01 (Issue #607 BFF rate-limit trust boundary)

- Resumed `agent/607-bff-rate-limit` in `/tmp/account-607-bff-rate-limit` at `f913d8d3` on current `origin/main@4d01f240`. PR #608/#540 had already integrated the HttpOnly BFF, so Draft PR #623 independently targets main with the required rate-limit trust correction.
- Gateway protected requests now use a verified-principal attribute; login accepts a short-lived method/path-bound HMAC over an opaque username hash and otherwise ignores forwarding/BFF spoofing in favor of the direct peer. A route-scoped 10,000-entry LRU plus BFF-peer aggregate quota bounds memory and username rotation.
- Frontend/Gateway share a distinct runtime-only 32-512-byte secret through required Compose inputs and value-redacting validators. No secret value, public browser variable, build argument, runtime container or external service was changed.
- Node 20 development/production BFF live tests pass 12/12 each, TypeScript passes, ESLint remains 0 errors/244 inherited #536 warnings and production build passes 122/122 pages. Forced offline JDK-17 Gateway and affected Config policy tests pass; Python validator self-test passes 168. PowerShell is unavailable and this skipped runner is covered only by static Java policy assertions.
- Independent review found no P0-P3. Diff/conflict/clean-tree gates pass. GitHub checks, final exact-head confirmation and the Ready-event Guard remain before the user-approved squash merge; rollback is a reviewed PR #623 revert.

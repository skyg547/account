# 2026-07-08 payable + receivable API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `payable:core`, `receivable:core`에서 HTTP Controller/DTO/Web/Validation 의존을 제거하고, 각 API 모듈이 요청 DTO/응답 DTO를 core command로 연결하도록 정리한 변경.
- 우선 확인:
  - `payable:core`, `receivable:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, Controller/DTO를 더 이상 소유하지 않는지.
  - `PurchaseUseCase`, `PaymentUseCase`, `SalesUseCase`, `CollectionUseCase`가 command 기반 입력만 받는지.
  - `SalesInvoiceCommand`, `CollectionCommand`, `ManualMatchingCommand`가 API/Batch 공통 업무 입력값으로 충분한지.
  - `receivable:api` 요청 DTO가 Bean Validation 후 command로 변환하고, 응답 DTO가 domain/JPA 엔티티 직접 직렬화를 막는지.
  - `SalesService`가 매출 전표 actor를 `createdBy` 기준으로 전달하고, `CollectionService`가 수납/매칭 금액 상태 전이를 기존 도메인 규칙으로 유지하는지.
  - `PayableBatchJobRegistryConfiguration`, `ReceivableBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :receivable:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :receivable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|@RequestMapping|jakarta\.validation|com\.ho\.account\.receivable\.api" receivable\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`

# 2026-07-08 payable API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `payable:core`에서 HTTP Controller/DTO/Web/Validation 의존을 제거하고, `payable:api`가 요청 DTO/응답 DTO를 core command로 연결하도록 정리한 변경.
- 우선 확인:
  - `payable:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, Controller/DTO를 더 이상 소유하지 않는지.
  - `PurchaseUseCase`, `PaymentUseCase`가 command 기반 입력만 받는지.
  - `PurchaseInvoiceCommand`, `PaymentRunCommand`, `ExecutePaymentCommand`, `AdvancePaymentCommand`, `OffsetPayableCommand`가 API/Batch 공통 업무 입력값으로 충분한지.
  - `payable:api` 요청 DTO가 Bean Validation 후 command로 변환하고, 응답 DTO가 JPA/domain 엔티티 직접 직렬화를 막는지.
  - `PayablePaymentRunBatchConfig`가 Job/Step orchestration만 담당하고 업무 판단은 core `PaymentUseCase`에 위임하는지.
  - `PayableBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :payable:core:test :payable:api:compileJava :payable:batch:compileJava --console=plain --max-workers=1`
  - `.\gradlew :payable:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :payable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|@RequestMapping|jakarta\.validation|com\.ho\.account\.expenditure\.payable\.api" payable\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`

# 2026-07-08 expenditure-resolution API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: gent/asset-lease-split
- 주요 범위: expenditure-resolution:core에서 HTTP Controller/DTO와 master-data 내부 참조를 제거하고, API DTO -> core command -> domain/service 흐름으로 정리한 변경.
- 우선 확인:
  - expenditure-resolution:core가 spring-boot-starter-web, spring-boot-starter-validation, Controller/DTO를 더 이상 소유하지 않는지.
  - ExpenditureResolutionUseCase, APPaymentUseCase가 command 기반 입력만 받는지.
  - ExpenditureResolutionService가 MasterDataQueryPort와 TaxInvoiceRef.purchase()/active()로 외부 참조를 검증하는지.
  - Budget과 Invoice가 master-data Entity 연관 대신 코드 값을 저장하는지.
  - API 통합 테스트가 expenditure-resolution:api 테스트 소스로 이동했고 기존 지출결의 -> tax -> AP 지급 시나리오가 유지되는지.
  - ExpenditureResolutionBatchJobRegistryConfiguration이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - $compile
  - $verify
  - $apiRun
  - $batchRun
  -
g -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|jakarta\.validation|com\.ho\.account\.expenditure\.resolution\.api|masterdata\.core|AccountSubjectPersistencePort|DepartmentPersistencePort|BusinessPartnerPersistencePort" expenditure-resolution\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"
# 2026-07-08 tax API/core command 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `tax:core`에서 HTTP Controller/DTO를 제거하고 `tax:api`가 API DTO를 core `TaxInvoiceCommand`로 변환하도록 정리한 변경.
- 우선 확인:
  - `tax:core`가 `spring-boot-starter-web`, `spring-boot-starter-validation`, API DTO/Controller를 더 이상 소유하지 않는지.
  - `TaxInvoiceUseCase`와 `TaxInvoiceService`가 `TaxInvoiceCommand`를 기준으로 업무 흐름을 처리하는지.
  - `APInvoiceController`와 `TaxInvoiceRequestDto`가 `tax:api`에 있고, HTTP 요청 검증 후 command로 변환하는지.
  - `TaxInvoiceRef.purchase()`, `active()`, `usableForPurchaseSettlement()`가 외부 모듈이 취소/매입 정책을 명시적으로 판단하기에 충분한지.
  - `expenditure-resolution`의 지출결의/AP 지급 검증이 `TaxInvoiceRef` 계약 메서드를 사용하고 기존 SALES/CANCELLED 차단 테스트가 유지되는지.
  - `TaxBatchJobRegistryConfiguration`이 Batch Job 등록 순서만 조정하고 업무 로직을 포함하지 않는지.
- 재실행 권장:
  - `.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1`
  - `.\gradlew :tax:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `.\gradlew :tax:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1`
  - `rg -n "spring-boot-starter-web|spring-boot-starter-validation|@RestController|jakarta\.validation|com\.ho\.account\.tax\.api" tax\core --glob "*.java" --glob "*.gradle" --glob "!**/build/**"`
# 2026-07-07 asset-lease 감가상각 Batch 경계 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `asset-lease` 대량 감가상각 batch에서 JPA 엔티티 mutation과 JDBC bulk update가 동시에 발생할 수 있는 경계 수정.
- 우선 확인:
  - `FixedAsset.calculateDepreciation()`이 상태를 변경하지 않고 `FixedAssetDepreciationResult`만 반환하는지.
  - 단건 API 경로의 `FixedAsset.depreciate()`는 기존처럼 도메인 상태 전이를 수행하는지.
  - `DepreciationPipeline`이 batch chunk를 결과 값 객체로 변환하고 엔티티를 변경하지 않는지.
  - `AssetJdbcAdapter.updateDepreciationBulk()`가 상각누계액, 장부가액, 상태, 최종상각일을 결과 값 기준으로 한 번만 반영하는지.
  - `AssetDepreciationBatchConfig`가 Job/Step/Reader/Writer orchestration만 담당하고 업무 계산은 core pipeline에 위임하는지.
- 재실행 권장:
  - `.\gradlew :asset-lease:core:test :asset-lease:api:compileJava :asset-lease:batch:compileJava --console=plain --max-workers=1`
  - `rg -n "org\.springframework\.batch|StepExecution|ItemProcessor|Tasklet|JobParameters|StepScope|JobScope" asset-lease\core\src\main\java asset-lease\core\src\test\java asset-lease\core\build.gradle --glob "*.java" --glob "*.gradle"`

# 2026-07-07 account-mart 담보 DQ/LGD 상세 연결 리뷰

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `account-mart`의 아파트/부동산 담보 상세(`OdsApartCollDetail`)를 실제 DQ/LGD 선행 흐름에 연결.
- 우선 확인:
  - `CollateralDataQualityInspectionService`가 application service로서 `OdsApartCollDetailRepository` port 조회와 domain processor 호출 순서만 조정하는지.
  - `CollateralDataQualityProcessor`가 DB/JPA를 알지 않고 담보 마스터 평가액, 부동산/아파트 상세 존재 여부, KB 시세/지역/전용면적 필수값만 판단하는지.
  - `OdsApartCollDetailPersistenceAdapter`와 `JpaOdsApartCollDetailRepository`가 infrastructure adapter 경계에 머무르는지.
  - `CollateralDataQualityItemProcessor`가 Spring Batch adapter로서 기준일과 row 전달만 담당하는지.
  - `DataPopulator`가 부동산 담보 seed 생성 시 `ods_apart_coll_detail` 상세를 함께 만들어 demo DQ 흐름을 깨지 않는지.
  - `V5__add_ods_apart_coll_detail.sql`이 PostgreSQL/H2 호환 DDL로 충분한지.
- 재실행 권장:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1`
  - `rg -n "@todo|TODO|FIXME" account-mart --glob "*.java" --glob "!**/build/**"`

# Latest Review Target - 2026-07-03 Loan

Gemini는 이번 Codex 변경에서 `loan` 모듈을 우선 검토하세요.

1. Architecture / DDD
- `InterestAccrualService`가 journal-ledger `JournalUseCase`/`JournalEntry`를 직접 알지 않고 `LoanJournalPort` 명령만 생성하는지 확인하세요.
- `LoanAccrualLog`, `LoanEvent`, `EIRAmortizationSchedule`이 전표 엔티티 연관 대신 전표 ID/전표번호 값 참조만 보관하는지 확인하세요.
- `LoanEventDto`가 전표번호를 null로 버리지 않고 이벤트 이력의 값 참조를 그대로 반환하는지 확인하세요.

2. Batch / Local Execution
- `LoanInterestAccrualBatchConfig`가 Reader/Processor/Writer/Chunk 실행 책임만 갖고 이자 금액, 중복 처리, 전표 명령 생성은 core `InterestAccrualService`에 위임하는지 확인하세요.
- `LoanBatchJobRegistryConfiguration`이 Spring Batch 인프라 초기화 순서만 조정하고 업무 로직을 포함하지 않는지 확인하세요.
- Loan README/local-run 문서와 `.run` 설정의 `spring.application.name`, Redis repository 비활성화, H2/local 옵션이 실제 bootRun 명령과 일치하는지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :loan:core:test --console=plain --max-workers=1 --rerun-tasks` 성공.
- `.\gradlew :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1` 성공.
- `:loan:api:bootRun` local/H2 context smoke 성공, 로그 app name `loan-api` 확인, Redis repository 스캔 로그 미재현.
- `:loan:batch:bootRun` local/H2 context smoke 성공, 로그 app name `loan-batch` 확인, Redis repository 스캔 로그 및 Batch JobRegistry 경고 미재현.

4. 알려진 리스크
- PostgreSQL migration 적용과 대량 ACTIVE 대출 이자 발생 Job은 아직 seeded 데이터로 검증하지 않았습니다.
- `spring.data.redis.repositories.enabled=false`는 local smoke 옵션입니다. 향후 loan에 Redis repository가 실제로 추가되면 문서 옵션을 재검토해야 합니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# Latest Review Target - 2026-07-03 Closing

Gemini는 이번 Codex 변경에서 `closing` 모듈을 우선 검토하세요.

1. Architecture / DDD
- `closing:core/application/service/FxValuationService`가 FX 평가 금액, 정상잔액 방향, 차대변 판단을 core 업무 로직으로 적절히 소유하는지 확인하세요.
- `closing:core/application/service/EclProvisionService`가 확정 `allowance_summary`와 기존 GL 충당금 잔액 차이만 처리하고 Stage/PD/LGD/EAD를 재계산하지 않는지 확인하세요.
- `closing:core/application/port/out`의 `FxExchangeRateLookupPort`, `AllowanceBalanceLookupPort`, `ClosingJournalEntryPort`가 기술 독립 포트로 충분한지 확인하세요.

2. Batch Boundary
- `closing:batch`가 Spring Batch Job/Step/Reader/Tasklet, 외부 Repository/JournalUseCase adapter 책임만 갖고 업무 산식과 차대변 판단을 직접 구현하지 않는지 확인하세요.
- `FxValuationBatchConfig`의 partition/reader/writer 흐름이 core DTO 변환과 위임만 수행하는지 확인하세요.
- `EclProvisionBatchConfig` Tasklet이 기준일/batch ID를 core 서비스에 전달하는 실행 어댑터로 충분한지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :closing:api:compileJava --console=plain --max-workers=1` 성공.
- `.\gradlew :closing:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false" --console=plain --max-workers=1` 성공.
- closing API/BATCH local logback XML 파싱 성공.
- closing core Spring Batch 타입 직접 참조 검색 결과 없음.
- closing Java/문서 TODO 및 깨진문자 검색 결과 없음.

4. 알려진 리스크
- PostgreSQL 대량 GL 잔액/allowance_summary 기준 성능, skip/retry, 중복 전표 감지 운영 검증은 아직 수행하지 않았습니다.
- FX valuation writer는 현재 계정별 실패를 로깅하고 계속 진행합니다. 운영 정책상 fail-fast 또는 skip-limit/reporting이 더 적절한지 검토가 필요합니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# Latest Review Target - 2026-07-03 ECL + Journal Ledger

Gemini는 이번 Codex 변경에서 아래 범위를 우선 검토하세요.

1. ECL
- `ecl-core/application/pipeline`의 `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`이 IFRS 9 Stage/PD, EAD/LGD, 미래전망 ECL 산출 순서를 올바르게 core에 두는지 확인하세요.
- `ecl-batch` processor가 Spring Batch adapter 책임만 갖고 업무 산식/순서를 직접 구현하지 않는지 확인하세요.
- `AllowanceCalculationService`와 Batch가 같은 core pipeline을 공유하면서 API/단건 산출과 Batch 산출의 업무 순서가 어긋나지 않는지 확인하세요.

2. Journal Ledger
- `dailyBalanceReaggregationJob`이 Job/Step wiring, Tasklet adapter, core `LedgerService` 호출로 책임이 나뉘어 있는지 확인하세요.
- `BatchDateRangeParameterUtils`의 `startDate/endDate`, `fromDate/toDate`, `baseDate`, `targetDate` 해석과 기간 역전 검증이 재실행/운영 파라미터 관점에서 충분한지 확인하세요.
- `journal-ledger/batch/application.yml`의 H2 local datasource/JPA/Batch 계층과 `spring.batch.job.enabled=false` 기본값이 로컬/운영 실행에 문제를 만들지 않는지 확인하세요.
- `journal-ledger` docs와 `.run/Journal Ledger Batch Reaggregation.run.xml`의 명령이 실제 Gradle 실행 경로와 일치하는지 확인하세요.

3. Codex 검증 결과
- `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1` 성공.
- `.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1` 성공, Job status `COMPLETED`.
- ECL/Journal Ledger Java TODO 및 깨진문자 검색 결과 없음.
- `git diff --check` 오류 없음(CRLF 변환 경고만 출력).

4. 알려진 리스크
- PostgreSQL 대량 seed 기준 성능/락/재실행 검증은 아직 수행하지 않았습니다.
- Journal Ledger Batch bootRun에서 Spring Cloud/Batch BeanPostProcessor WARN은 남아 있으나 Job 실패는 유발하지 않았습니다.

아래 기존 리뷰 지침도 함께 따르세요.

---
# 2026-07-03 ecl core pipeline boundary review

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `ecl`의 batch processor 업무 산출 순서를 `ecl-core/application/pipeline`으로 이동하고, `ecl-batch` processor를 Spring Batch adapter로 축소한 변경.
- 우선 확인:
  - `ecl-batch`의 `StagingProcessor`, `EadCrmProcessor`, `EclProcessor`가 산식/상태 판단/BigDecimal 계산을 직접 수행하지 않는지.
  - `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`이 DDD/헥사고날 기준으로 core 업무 순서를 적절히 소유하는지.
  - `AllowanceCalculationService`와 batch가 같은 core pipeline을 재사용해 API/Batch 산출 순서가 불일치하지 않는지.
  - ecl README/docs와 batch config 주석이 실제 코드 흐름과 맞는지.
- 재실행 권장:
  - `.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1`
  - `rg -n "AllowanceParameterService|PdCalculationService|LgdCalculationService|CcfCalculationService|EadCrmCalculationService|LifetimePdService|ForwardLookingEclService|BigDecimal|resolveMaturityYears|AllowanceEclResult\.builder" ecl\ecl-batch\src\main\java\com\ho\account\ecl\batch\processor`

기존 장기 리뷰 프롬프트는 아래 내용을 참고하세요.
# 2026-07-02 account-mart core/batch boundary review

Gemini는 아래 최신 변경을 우선 리뷰하세요.

- 대상 브랜치: `agent/asset-lease-split`
- 주요 범위: `account-mart`의 `mart-core` Spring Batch 의존 제거, `mart-batch` processor adapter 추가, 미사용 JPA-leaking application port 삭제, ODS-GL 대사 합계 조회 보정, account-mart 문서 보강.
- 우선 확인:
  - `mart-core`가 Spring Batch 타입을 import/구현하지 않는지.
  - `mart-batch`가 Job/Step/Reader/Writer/Chunk/adapter 책임만 갖고 업무 판단을 core에 위임하는지.
  - `OdsGeneralLedgerPersistenceAdapter.getBalanceSummaryByBaseDate()`가 기준일+계정+통화 합계 기준으로 대사에 충분한지.
  - `OdsApartCollDetail`에 남긴 `@todo`가 실제 후속 LGD/DQ 연결 리스크를 정확히 설명하는지.
  - account-mart README/docs 설명이 실제 Gradle 모듈과 실행 흐름에 맞는지.
- 재실행 권장:
  - `.\gradlew :account-mart:mart-core:test :account-mart:mart-batch:test --console=plain --max-workers=1`
  - `rg -n "org\.springframework\.batch|StepExecution|ItemProcessor|StepExecutionListener|ExitStatus|StepScope" account-mart\mart-core\src\main\java account-mart\mart-core\src\test\java account-mart\mart-core\build.gradle`

기존 장기 리뷰 프롬프트는 아래 내용을 참고하세요.
# Gemini Review Prompt

이 파일은 Codex가 구현을 맡고 Gemini가 독립 리뷰를 맡는 표준 핸드오프 프롬프트다.
Gemini에게 리뷰를 요청할 때 아래 프롬프트를 그대로 전달한다.

## Role Split

- Codex: 구현, 수정, 테스트 보강, 문서/워크로그 갱신, 최종 커밋 담당.
- Gemini: 독립 코드 리뷰 담당.
- Gemini는 사용자가 명시적으로 구현을 요청하지 않는 한 코드를 수정하지 않는다.
- Gemini 리뷰 결과는 Codex가 다시 검토한 뒤 실제 수정 여부를 판단한다.

## Gemini에게 전달할 프롬프트

```text
당신은 account 저장소의 독립 코드 리뷰어입니다.
이번 리뷰에서는 코드를 직접 수정하지 말고, Codex가 작업한 변경분을 Findings 중심으로 검수하세요.
이번 추가 리뷰 범위에는 2026-06-19 Codex의 업무 모듈 API/BATCH 실행 구조 재점검과 누락 Batch Job 보강이 포함됩니다. 특히 `deposit`, `payable`, `receivable`, `reconciliation`, `tax`, `expenditure-resolution`에 추가한 Spring Batch `Job/Step`, core batch use case, local-run 문서, `deposit:batch` Boot Batch auto-run 설정을 우선 검수하세요.

1. 먼저 아래 문서를 읽어 현재 프로젝트 규칙과 최근 작업 이력을 확인하세요.
- GEMINI.md
- docs/GEMINI.md
- docs/GEMINI_SKILL.md
- Agents.md
- docs/WORKLOG.md 최신 항목
- CODEX_WORKLOG.md 최신 항목
- MODULE_REVIEW_2026-05-06.md 최신 항목
- docs/todo.md
- 변경 대상 모듈의 README.md 및 docs/*.md

2. 현재 리뷰 대상 범위를 확인하세요.
- git status --short --branch
- git diff --stat
- git diff
- 새 파일이 있으면 해당 파일도 확인

3. 리뷰 대상 변경 범위는 Codex의 2026-06-19 "업무 모듈 API/BATCH 실행 구조 재점검 및 누락 Spring Batch Job 보강"입니다. 이전 누적 변경도 워킹트리에 섞여 있을 수 있으나, 우선순위는 아래 신규 파일/수정 파일입니다.
- `deposit:core/batch`: `DepositBatchUseCase`, `DepositBatchService`, `depositAccountIntegrityJob`, batch `application.yml`, `DepositBatchApplication`
- `payable:batch`: `payablePaymentRunJob`
- `receivable:core/batch`: `ReceivableBatchUseCase`, `ReceivableBatchService`, `CollectionPersistencePort` 후보 조회, `receivableAutoMatchingJob`
- `reconciliation:core/batch`: `ReconciliationBatchUseCase`, `ReconciliationBatchService`, `reconciliationDailyJob`
- `tax:core/batch`: `TaxInvoiceBatchUseCase`, `TaxInvoiceBatchService`, `taxInvoiceValidationJob`
- `expenditure-resolution:core/batch`: `ExpenditureResolutionBatchUseCase`, `ExpenditureResolutionBatchService`, `expenditureResolutionApprovalJob`
- 각 모듈 README 및 `docs/local-run.md`의 Job 실행 명령

이전 누적 검토 참고 범위는 Codex의 2026-06-09 "잔여 TODO 최종 경계 통합"부터 2026-06-19 "전체 API/BATCH bootRun smoke 및 build 검증 반영"까지입니다.
- Auth 로그인 성공/실패 감사, 설정 기반 임시 잠금, `LoginAttemptPort`/기본 어댑터
- Payable `PaymentExecutionPort`, 지급 멱등 키, 실패/재시도 상태, 정확한 `payableId`, master-data 내부 의존 제거
- Receivable 참조번호 우선/만기일 허용/중복 실패 폐쇄 자동 매칭과 `CollectionAllocation` 잔액 이력
- Journal/Unsettled HTTP DTO, 필수 `X-User-ID`, 미결 인바운드 포트, 반제 참조번호 멱등/감사 필드
- Loan 소유 출력 포트와 외부 전표 값 참조
- Reconciliation 표준 `Unit -> Run -> Difference` Aggregate 및 단계 결과 연결
- Allowance input JPA 쓰기 소유권의 account-mart 이동과 ECL 자체 읽기 모델
- Closing 조정 전표 기본 DRAFT 통제, Tax 논리 취소/actor/계약 포트, Asset actor 전달
- 2026-06-09 구현 종료 시점의 Java 코드 `@todo` 0건 기록과, 이후 문서 통합에서 의도적으로 추가한 운영 개선용 `@todo`의 실제 리스크 일치 여부
- ECL `EadCalculationResult`, 모델 비율 fail-closed 검증, 이름 있는 기본 CCF 정책
- ECL 모델 파라미터 기술 독립 포트와 JPA 어댑터 빈 구성
- ECL 등급·상품·LGD·담보배분·거시시나리오·전이행렬 기술 독립 포트와 JPA/캐시 어댑터
- Journal `UnsettledItemPersistencePort`, 거래처별 DB 필터, `JournalRuleQueryPort`, `JournalSide` 규칙 타입
- Journal `JournalPersistencePort`/`LedgerEntryPersistencePort`/`LedgerBalancePersistencePort` 전기·잔액 경계와 DB 조건 필터
- Journal `journal-ledger.ledger.persistence-mode=jdbc-bulk` 설정 기반 JDBC batch insert/upsert 어댑터, `YearMonthAttributeConverter`, 잔액 재집계 bulk 저장 경로
- ECL/Journal docs 인덱스, 입문·프로세스·스키마 문서의 실제 코드 일치 여부
- 공통 docs `beginner_guide.md`, `local-development.md`, `module-documentation-sequence.md`가 실제 settings.gradle/실행 클래스/Gradle 명령과 맞는지
- account-mart 문서 인덱스, batch/API/core README, Batch Job 목록, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- ECL README/docs/API/batch/core 문서, account-mart 선행 데이터 조건, demo profile 설명, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Journal Ledger README/docs/layer-guide, legacy README archive 이동, JDBC bulk 실행 설정, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Closing README/docs, legacy README archive 이동, FX/ECL Batch 실행 조건, IntelliJ `.run` Gradle 설정, 깨진 한글 주석 복구가 실제 코드와 맞는지
- Closing에 새로 남긴 `@todo` 2건이 실제 운영 리스크(대량 FX Reader, 부채 계정 차대변 판정)를 정확히 가리키는지
- Loan README/docs, legacy README archive 이동, EIR/일일 이자 Batch 실행 조건, IntelliJ `.run` Gradle 설정, 깨진 한글 주석 복구가 실제 코드와 맞는지
- Loan에 새로 남긴 `@todo` 1건이 실제 운영 리스크(이연 수수료/비용 부호 정책)를 정확히 가리키는지
- Payable README/docs, legacy README archive 이동, 매입채무/지급 런/지급 실행/선급금/상계 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Receivable README/docs, legacy README archive 이동, 매출채권/수납/자동·수동 매칭/부분 매칭 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Payable/Receivable이 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Payable에 새로 남긴 `@todo` 4건이 실제 고도화 리스크(인바운드 DTO/Bean Validation 분리)를 정확히 가리키는지
- Asset-Lease README/docs, legacy README archive 이동, 고정자산/감가상각 Batch/IFRS 16 리스/이벤트/지급결의 포트 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Asset-Lease가 `core`/`api`/`batch`로 분리되어 있고 API/BATCH 실행 문서와 Config/Eureka/Batch 비활성화 실행 인자가 실제 `build.gradle`, `AssetLeaseApiApplication`, `AssetLeaseBatchApplication`, `application.yml`과 맞는지
- Asset-Lease에 새로 남긴 `@todo` 4건이 실제 운영 리스크(Batch targetDate/파이프라인 분리, 리스 actor 감사, 리스 계정 매핑 포트 분리)를 정확히 가리키는지
- Tax README/docs, legacy README archive 이동, AP 세금계산서/금액 검증/논리 취소/외부 조회 포트 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Tax가 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Tax에 새로 남긴 `@todo` 1건이 실제 운영 리스크(취소 증빙 외부 조회 정책)를 정확히 가리키는지
- Reconciliation README/docs, legacy README archive 이동, 대사 단위/규칙/실행/차이/사유 코드/조정 전표 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Reconciliation이 현재 standalone Boot 앱이 아니라 `java-library` 모듈이라는 문서 설명이 `build.gradle` 및 소스 구조와 맞는지
- Reconciliation에 새로 남긴 `@todo` 2건이 실제 운영 리스크(규칙 물리 삭제, 조정 전표 멱등 키)를 정확히 가리키는지
- Reporting README/docs, 재무제표 생성, 제출본 버전, 주석 마트, 감독보고 제출, batch adapter 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Reporting의 `core`는 `java-library`, `api`/`batch`는 standalone Boot 앱이라는 문서 설명이 `build.gradle`, Application 클래스, `.run` 설정과 맞는지
- Deposit의 `core`는 library, `api`/`batch`는 standalone Boot 앱이라는 문서 설명과 로컬 어댑터 설정이 실제 코드와 맞는지
- `asset-lease`, `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`에 잘못된 미추적 API/BATCH Application 후보가 남아 있지 않은지
- local profile에서 logback `LOGSTASH` appender가 생성/참조되지 않고, 일반 profile에서는 기존 logstash 전송 구조가 유지되는지
- Eureka/Gateway/OpenFeign 실행 모듈에 `com.github.ben-manes.caffeine:caffeine` 의존성이 누락 없이 추가되어 Spring Cloud LoadBalancer 기본 캐시 경고를 제거하는지
- 로컬 H2/메모리/로컬 어댑터 실행 명령에서 `spring.cloud.discovery.enabled=false`와 `spring.cloud.loadbalancer.enabled=false`가 함께 적용되어 불필요한 LoadBalancer 자동 구성을 피하는지
- Deposit/Reporting Batch가 `JobRegistrySmartInitializingSingleton`으로 Batch Job 등록 시점을 늦추면서 `jobRegistryBeanPostProcessor` 조기 초기화 경고를 제거하고, Batch 앱에 업무 if/for/math 로직을 추가하지 않았는지
- Reporting에 새로 남긴 `@todo` 2건이 실제 운영 리스크(Spring Batch Job/Step 전환, 랜덤 반려 설정화)를 정확히 가리키는지
- Contracts/Shared-Kernel README/docs/local-run이 실제 `java-library` build.gradle 및 컴파일 검증 흐름과 맞는지
- Master-Data README/docs, SCD2 기준정보, 변경 요청 승인/반영, 포트/어댑터 흐름, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Governance README/docs, 감사 로그, 승인, SOD, Auth 역할 반영, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Auth README/docs, 로그인, JWT, roleVersion, 내부 역할 반영 API, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Config-Server/Config-Repo README/docs, native `config-repo` 설정 조회, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Discovery README/docs, legacy corrupt concept archive 이동, 새 Eureka concept/local-run이 실제 코드와 맞는지
- Gateway README/docs, `config-repo/gateway-service.yml` 라우트, JWT filter, fallback, IntelliJ `.run` Gradle 설정이 실제 코드와 맞는지
- Foundation/Infra에 새로 남긴 `@todo` 5건이 실제 운영 리스크(Auth 잠금 공유, Gateway roleVersion 검증, Master-Data 실제 반영/chunk, Governance fail-closed)를 정확히 가리키는지

4. 리뷰 기준은 아래 순서로 우선순위를 둡니다.
- 컴파일/테스트/bootJar/smoke 기동 실패를 유발하는 결함
- IFRS 9 Stage, PD, LGD, EAD, ECL, summary 금액 오류
- 회계 금액/차대/상태 전이/마감/승인/라인리지 오류
- 헥사고날 아키텍처 위반: adapter, application, domain, infrastructure 경계 침범
- DDD 위반: 단순 setter 중심 변경, 도메인 규칙의 서비스 산재, 의미 없는 단순 위임
- 배치 규칙 위반: batch 모듈 내 비즈니스 if/for/math 구현
- API 계약 회귀: DTO, Controller, Service, Test 불일치
- Flyway migration 버전 충돌 또는 다중 모듈 런타임 classpath 확인사항
- 테스트 누락 또는 기존 테스트 계약 미갱신
- 문서/WORKLOG/todo 완료 표기와 실제 코드 상태 불일치

5. 가능하면 아래 검증을 재실행하세요.
- .\gradlew :deposit:core:test :payable:core:test :receivable:core:test :reconciliation:core:test :tax:core:test :expenditure-resolution:core:test :deposit:batch:compileJava :payable:batch:compileJava :receivable:batch:compileJava :reconciliation:batch:compileJava :tax:batch:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1
- .\gradlew build --console=plain --max-workers=1
- .\gradlew :deposit:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=depositAccountIntegrityJob asOfDate=2026-06-19" --console=plain --max-workers=1
- .\gradlew :payable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=payablePaymentRunJob runDate=2026-06-19 createdBy=SMOKE description=Smoke" --console=plain --max-workers=1
- .\gradlew :receivable:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=receivableAutoMatchingJob" --console=plain --max-workers=1
- .\gradlew :reconciliation:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=reconciliationDailyJob reconciliationDate=2026-06-19 runBy=SMOKE deepMode=false" --console=plain --max-workers=1
- .\gradlew :tax:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=taxInvoiceValidationJob startDate=2026-06-19 endDate=2026-06-19" --console=plain --max-workers=1
- .\gradlew :expenditure-resolution:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=expenditureResolutionApprovalJob startDate=2026-06-19 endDate=2026-06-19 paymentDueDate=2026-06-19" --console=plain --max-workers=1
- rg -n "\bJob\s+\w+\s*\(" --glob "*.java" --glob "!**/build/**" account-mart\mart-batch asset-lease\batch closing\batch deposit\batch ecl\ecl-batch expenditure-resolution\batch journal-ledger\batch loan\batch payable\batch receivable\batch reconciliation\batch reporting\batch tax\batch
- .\gradlew :auth:test :payable:test :receivable:test :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :tax:test --console=plain
- .\gradlew :closing:batch:test :journal-ledger:core:test :journal-ledger:api:compileJava :reconciliation:test --console=plain
- .\gradlew :reconciliation:test :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain
- .\gradlew :contracts:compileJava :shared-kernel:compileJava :master-data:test :governance:test :auth:test :gateway:test :discovery:test :config-server:assemble --console=plain
- .\gradlew :loan:core:test :loan:api:compileJava :shared-kernel:compileJava :account-mart:mart-core:test :account-mart:mart-batch:test :ecl:ecl-core:test :ecl:ecl-api:compileJava --console=plain
- .\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test :journal-ledger:core:test :journal-ledger:api:test --console=plain
- .\gradlew :journal-ledger:core:test :journal-ledger:api:test :loan:core:test --console=plain
- rg -ni "@todo" --glob "*.java" .

6. 출력 형식은 반드시 아래 순서를 따르세요.

Findings:
- Severity: Critical/High/Medium/Low 중 하나
- 파일/라인: 실제 경로와 라인 번호
- 문제: 무엇이 잘못됐는지
- 영향: 운영/회계/빌드/테스트에 어떤 문제가 생기는지
- 제안: Codex가 수행할 수정 방향

Verification:
- 실행한 명령
- 성공/실패 결과
- 실패 시 핵심 에러 요약

Open Questions:
- 정책 결정이 필요한 항목만 작성

Notes:
- 이미 WORKLOG 또는 MODULE_REVIEW에 기록된 알려진 이슈는 새 증거가 있을 때만 중복 보고하세요.
- 리뷰는 한국어로 작성하세요.
- 코드 수정은 하지 마세요.
```

## Current Handoff Context

- 현재 로컬 워킹트리에는 Codex 변경 외에 사용자/Gemini가 남긴 문서 이동/삭제 및 기타 미커밋 변경이 섞여 있을 수 있다.
- 이번 핸드오프의 기준은 2026-06-09 잔여 TODO 통합부터 2026-06-15 local profile logstash 비활성화까지의 누적 변경이다.
- 루트 `WORKLOG.md`는 현재 작업트리에 없고, 추적된 최신 작업 이력은 `docs/WORKLOG.md`에 있다.
- Codex 작업 로그는 루트 `CODEX_WORKLOG.md`에 최신 항목을 추가했다.
- 최종 검증 결과:
  - Auth/Payable/Receivable/Asset/Tax 집중 테스트 성공.
  - Closing/Journal/Reconciliation/ECL 집중 테스트 및 API 컴파일 성공.
  - Loan/Account-Mart/Allowance 소유권 경계 테스트 및 API 컴파일 성공.
  - 2026-06-09 구현 종료 시점의 Java 소스 `@todo` 검색 결과는 0건이었다.
  - ECL core/API/batch와 Journal core/API 통합 검증 성공.
  - Journal T53 구현 후 core 테스트와 H2 기반 JDBC batch insert/upsert 집중 테스트 성공.
  - 문서 통합 1차 후 account-mart core/test, mart-api compileJava, mart-batch test 성공.
  - 문서 통합 2차 후 ecl core test, ecl-api compileJava, ecl-batch test 성공.
  - 문서 통합 3차 후 journal-ledger core/api test 성공.
  - 문서 통합 4차 후 closing core test, closing-api compileJava, closing-batch test 성공.
  - 문서 통합 4차에서 Closing 주석 복구와 함께 운영 개선용 Java `@todo` 2건을 의도적으로 추가했다.
  - 문서 통합 5차 후 loan core test, loan-api compileJava, loan-batch compileJava 성공.
  - 문서 통합 5차에서 Loan 주석 복구와 함께 운영 개선용 Java `@todo` 1건을 의도적으로 추가했다.
  - 문서 통합 6차 후 payable/receivable test 성공.
  - 문서 통합 6차에서 Payable 주석 복구와 함께 인바운드 DTO/Bean Validation 분리용 Java `@todo` 4건을 의도적으로 추가했다.
  - 문서 통합 7차 후 asset-lease/tax test 성공.
  - 문서 통합 7차에서 Asset-Lease 운영 개선용 Java `@todo` 4건과 Tax 외부 조회 정책 `@todo` 1건을 의도적으로 추가했다.
  - 문서 통합 8차 후 reconciliation, reporting core/api/batch test 성공.
  - 문서 통합 8차에서 Reconciliation 운영 개선용 Java `@todo` 2건과 Reporting 운영 개선용 Java `@todo` 2건을 의도적으로 추가했다.
  - 문서 통합 9차 후 contracts/shared-kernel compileJava, master-data/governance/auth/gateway/discovery test, config-server assemble 성공.
  - 문서 통합 9차에서 Auth/Gateway/Master-Data/Governance 운영 개선용 Java `@todo` 5건을 의도적으로 추가했다.
  - 2026-06-12 검수 반영 후 `asset-lease:bootJar`, `deposit:api:bootJar`, `deposit:batch:bootJar`, `reporting:api:bootJar`, `reporting:batch:bootJar`, 영향 library 모듈 compileJava 성공.
  - 2026-06-12 검수 반영 후 `deposit:api`, `deposit:batch`, `reporting:api`, `reporting:batch` 개별 bootRun 컨텍스트 스모크 성공.
  - 2026-06-12 검수 반영에서 Deposit/Reporting IntelliJ `.run` 설정과 로컬 실행 문서를 추가했다.
  - 2026-06-15 logstash 비활성화 반영 후 Deposit/Reporting API/BATCH 네 가지 bootRun 컨텍스트 스모크가 `spring.profiles.active=local`로 성공했고, `localhost:5000` logstash 연결 실패 경고가 사라졌다.
  - 2026-06-15에 모든 `logback-spring.xml` XML 파싱과 Deposit/Reporting `.run` XML 파싱, `git diff --check`를 통과했다.
- Docker 이미지 빌드는 실행하지 않았다.
- 운영/일반 profile에서는 기존 logstash 전송이 유지되므로 수집기(`localhost:5000`) 기동이 필요하다.
- Auth 기본 잠금 어댑터와 Payable 로컬 지급 어댑터는 운영용 공유/외부 어댑터 교체가 필요하다.
- Journal 전기·잔액 Repository 직접 의존과 ECL 마스터 포트의 JPA 기술 누수는 제거했다.
- Journal JDBC bulk 구현은 설정 기반으로 추가했으며, 운영 DB 기준 배치 크기·인덱스·락 대기·동시 재집계 부하 검증은 남아 있다.

## Review Handoff Checklist

- 리뷰 전 `git status --short --branch`로 범위를 확인한다.
- 변경 대상 모듈 문서를 읽는다.
- `docs/WORKLOG.md`, `CODEX_WORKLOG.md`, `MODULE_REVIEW_2026-05-06.md` 최신 항목을 확인한다.
- Findings는 요약보다 먼저 작성한다.
- 각 Finding은 파일/라인 근거를 포함한다.
- Gemini는 코드를 수정하지 않는다.

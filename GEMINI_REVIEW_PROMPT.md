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
- Asset-Lease가 standalone Boot 앱이라는 문서 설명과 Config/Eureka/Batch 비활성화 실행 인자가 실제 `build.gradle`, `AssetLeaseApplication`, `application.yml`과 맞는지
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
- .\gradlew :auth:test :payable:test :receivable:test :asset-lease:test :tax:test --console=plain
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

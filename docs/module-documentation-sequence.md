# 모듈별 문서 통합 진행표

이 문서는 모듈별 문서 점검/통합/고도화 작업의 순서를 기록합니다.

## 작업 원칙

- 기존 문서는 삭제하지 않고, 유효한 내용은 새 인덱스나 상세 문서로 흡수합니다.
- 깨졌거나 중복되어 직접 읽기 어려운 문서는 `docs/archive` 또는 해당 모듈의 `docs/archive`로 이동합니다.
- 각 모듈은 `README.md`에서 빠른 실행과 문서 인덱스로 이어지게 합니다.
- IntelliJ 로컬 실행, Gradle 테스트/컴파일, 주요 profile/argument를 함께 적습니다.
- 문서 변경 후 가능한 범위의 Gradle 검증과 `git diff --check`를 실행합니다.

## 진행 순서

| 순서 | 범위 | 상태 | 비고 |
| ---: | --- | --- | --- |
| 0 | 공통 docs | Done | `docs/beginner_guide.md`, `docs/local-development.md`, `docs/README.md`, 루트 `README.md` 정리 |
| 1 | `account-mart` | Done | 문서 인덱스 신설, batch/API/core 실행 가이드 보강, IntelliJ `.run` 설정 추가 |
| 2 | `ecl` | Done | ECL API/batch/core 실행 가이드 보강, account-mart 선행 데이터와 IntelliJ `.run` 설정 정리 |
| 3 | `journal-ledger` | Done | 원장/전기/JDBC bulk 문서 보강, 소스 트리 README archive 이동, 계층 가이드 추가 |
| 4 | `closing` | Done | ECL summary 연동, FX/ECL 결산 실행 가이드, 깨진 주석 복구, IntelliJ `.run` 설정 추가 |
| 5 | `loan` | Done | EIR/전표 포트/배치 실행 흐름, 깨진 주석 복구, IntelliJ `.run` 설정 추가 |
| 6 | `payable`, `receivable` | Done | AP/AR 수금·지급/반제 흐름, library 모듈 실행 가이드, IntelliJ 테스트 설정 정리 |
| 7 | `asset-lease`, `tax` | Done | 자산/리스/세금계산서 실행·검증 흐름, archive 보존, IntelliJ 실행 설정 정리 |
| 8 | `reconciliation`, `reporting` | Done | 대사/보고 문서와 감독보고 흐름 정리 |
| 9 | foundation/infra | Done | `contracts`, `shared-kernel`, `master-data`, `governance`, `auth`, gateway/discovery/config 등 |
| 10 | standalone Application 검토 반영 | Done | Gemini 실행 클래스 검수 결과 반영, `deposit:batch`/`reporting` Boot 앱 정리 |
| 12 | `ecl` core pipeline 경계 리팩토링 | Done | batch processor를 adapter로 축소하고 Stage/PD, EAD/LGD, ECL 순서를 core pipeline으로 이동 |

## 1차 완료 상세

### 공통 docs

- 깨진 `docs/beginner_guide.md` 원문을 `docs/archive/beginner_guide_legacy_corrupt_2026-06-10.md`로 보존했습니다.
- 새 `docs/beginner_guide.md`는 현재 모듈 구조, 문서 읽는 순서, IntelliJ 시작 방법을 기준으로 다시 작성했습니다.
- `docs/local-development.md`를 추가해 JDK 17, Gradle JVM, Spring Boot 실행 클래스, 인프라 실행, 검증 명령을 정리했습니다.
- 루트 `README.md`와 `docs/README.md`에서 새 로컬 실행 문서를 연결했습니다.

### account-mart

- `account-mart/docs/README.md`를 추가해 기존 분산 문서의 읽기 순서를 통합했습니다.
- `account-mart/README.md`, `mart-api/README.md`, `mart-batch/README.md`, `mart-core/README.md`에 IntelliJ/Gradle 실행법을 보강했습니다.
- `DATA_MART_BEGINNER_GUIDE.md`, `DATA_MART_SPEC.md`, `ETL_INTERFACE_SPEC.md`, `BATCH_LEARNING_GUIDE.md`, `MART_BATCH_ARCHITECTURE_GUIDE.md`, `MART_CORE_GUIDE_FOR_BEGINNERS.md`에 데이터 흐름, Job 목록, 재실행 체크, 성능 설정을 보강했습니다.
- `.run/Account Mart API bootRun.run.xml`, `.run/Account Mart Batch Demo.run.xml`을 추가했습니다.

## 1차 검증

```powershell
.\gradlew :account-mart:mart-core:test :account-mart:mart-api:compileJava :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- account-mart Gradle 검증 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력

## 2차 완료 상세

### ecl

- `ecl/README.md`의 실행 예시를 Windows PowerShell/IntelliJ 기준으로 보강했습니다.
- `ecl/docs/README.md`, `ALLOWANCE_BEGINNER_GUIDE.md`, `BATCH_EXECUTION_GUIDE.md`에 account-mart snapshot 선행 조건, demo profile의 한계, batch 컨텍스트 기동 방법을 추가했습니다.
- `ecl/ecl-api/README.md`, `ecl/ecl-batch/README.md`, `ecl/ecl-core/README.md`에 실행/검증 명령을 보강했습니다.
- `.run/ECL API bootRun.run.xml`, `.run/ECL Batch Context.run.xml`을 추가했습니다.

## 2차 검증

```powershell
.\gradlew :ecl:ecl-core:test :ecl:ecl-api:compileJava :ecl:ecl-batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- ECL Gradle 검증 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력

## 3차 완료 상세

### journal-ledger

- 소스 트리 하위 README 3개를 `journal-ledger/docs/archive`로 이동해 보존했습니다.
- `journal-ledger/docs/layer-guide.md`를 추가해 application/domain/adapter/infrastructure 책임과 출력 포트 경계를 최신 코드 기준으로 통합했습니다.
- `journal-ledger/README.md`, `docs/README.md`, `docs/beginner-guide.md`, `docs/process-flow.md`에 로컬 실행, JDBC bulk 모드, 계층 가이드 링크를 보강했습니다.
- `.run/Journal Ledger API bootRun.run.xml`, `.run/Journal Ledger API JDBC Bulk.run.xml`을 추가했습니다.

## 3차 검증

```powershell
.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- journal-ledger core/API 테스트 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력

## 4차 완료 상세

### closing

- 깨진 `closing/docs/README.md` 원문을 `closing/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 보존했습니다.
- `closing/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 결산 캘린더/태스크/게이트/잠금/재오픈/FX/ECL 배치 흐름을 통합했습니다.
- `closing/README.md`에 문서 읽기 순서와 Windows PowerShell 기준 Gradle 실행 예시를 보강했습니다.
- `.run/Closing API bootRun.run.xml`, `.run/Closing Batch Context.run.xml`을 추가했습니다.
- `ClosingCalendar`, `ClosingTask`, `ClosingGate`, `ClosingPeriod`, `DailyClosingStatus`의 깨진 한글 주석을 초보자용 설명으로 복구했습니다.
- `FxValuationBatchConfig`, `FxValuationService`에는 운영 대량 처리와 부채 계정 차대변 판정 개선 필요 지점을 `@todo`로 명시했습니다.

## 4차 검증

```powershell
.\gradlew :closing:core:test :closing:api:compileJava :closing:batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- closing core 테스트, API 컴파일, batch 테스트 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 다음 순서는 `loan` 문서 통합입니다.

## 5차 완료 상세

### loan

- 깨진 `loan/docs/README.md` 원문을 `loan/docs/archive/README_legacy_corrupt_2026-06-10.md`로 이동해 보존했습니다.
- `loan/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대출 생성, 실행, 이연 항목, EIR 스케줄, 재계산, 일일 이자 Batch 흐름을 통합했습니다.
- `loan/README.md`에 문서 읽기 순서와 Windows PowerShell 기준 Gradle 실행 예시를 보강했습니다.
- `.run/Loan API bootRun.run.xml`, `.run/Loan Batch Context.run.xml`을 추가했습니다.
- `DeferredItemType`, `DeferredItem`, `EIRAmortizationSchedule`, `RecalculationRun`, `LoanEvent`, `LoanDisbursal`, `LoanJdbcAdapter`의 깨진 한글 주석을 초보자용 업무 설명으로 복구했습니다.
- `EIRCalculator`에는 이연 수수료/비용 부호 정책 보강 필요 지점을 `@todo`로 명시했습니다.

## 5차 검증

```powershell
.\gradlew :loan:core:test :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- loan core 테스트, API 컴파일, batch 컴파일 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 다음 순서는 `payable`, `receivable` 문서 통합입니다.

## 6차 완료 상세

### payable, receivable

- `payable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매입채무, 지급 런, 지급 실행, 선급금, 상계 흐름을 통합했습니다.
- `receivable/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 매출채권, 수납, 자동/수동 매칭, 부분 매칭 흐름을 통합했습니다.
- 기존 모듈 docs 인덱스는 각각 `docs/archive/README_legacy_index_2026-06-10.md`로 이동해 보존했습니다.
- `payable/README.md`, `receivable/README.md`, 전사 `docs/README.md`에서 새 문서와 IntelliJ/Gradle 검증 방식을 연결했습니다.
- `payable`과 `receivable`은 현재 `java-library` 모듈이라 standalone `bootRun` 대신 `:payable:test`, `:receivable:test` 실행 흐름을 명시했습니다.
- `.run/Payable Module Tests.run.xml`, `.run/Receivable Module Tests.run.xml`을 추가했습니다.
- `PurchaseInvoiceId`의 깨진 한글 주석을 복구하고, payable 인바운드 DTO 분리 필요 지점은 `@todo`로 표시했습니다.
- `CollectionController`에 수납/매칭 인바운드 어댑터 역할 주석을 추가했습니다.

## 6차 검증

```powershell
.\gradlew :payable:test :receivable:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- payable/receivable 테스트 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 다음 순서는 `asset-lease`, `tax` 문서 통합입니다.

## 7차 완료 상세

### asset-lease, tax

- `asset-lease/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 고정자산 등록/상각/처분, 감가상각 Batch, IFRS 16 리스 최초 인식/월별 처리/재측정, Kafka 이벤트, 리스 지급결의 포트 흐름을 통합했습니다.
- `tax/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 AP 세금계산서 생성/조회/수정/논리 취소, 금액 정합성, 외부 조회 포트 흐름을 통합했습니다.
- 기존 `asset-lease/docs/README.md`, `tax/docs/README.md`는 각각 `docs/archive/README_legacy_index_2026-06-11.md`로 이동해 보존했습니다.
- `asset-lease/docs/api-spec.md`, `requirements.md`에 현재 API 필드, `X-User-ID`, 로컬 실행 전제, Batch/리스 계정 고도화 필요 지점을 보강했습니다.
- `asset-lease/README.md`, `tax/README.md`, 전사 `docs/README.md`에서 새 문서와 IntelliJ/Gradle 실행 방식을 연결했습니다.
- `.run/Asset Lease API bootRun.run.xml`, `.run/Asset Lease Tests.run.xml`, `.run/Tax Module Tests.run.xml`을 추가했습니다.
- `AssetDepreciationBatchConfig`, `LeaseEntryService`, `LeaseAccountingController`, `TaxInvoiceQueryAdapter`에 운영 개선 필요 지점을 `@todo`로 명시했습니다.

## 7차 검증

```powershell
.\gradlew :asset-lease:core:test :tax:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- asset-lease/tax 테스트 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 다음 순서는 `reconciliation`, `reporting` 문서 통합입니다.

## 8차 완료 상세

### reconciliation, reporting

- 기존 `reconciliation/docs/README.md`를 `reconciliation/docs/archive/README_legacy_index_2026-06-11.md`로 이동해 원문을 보존했습니다.
- `reconciliation/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 대사 단위/규칙/실행/차이/사유 코드/조정 전표 흐름을 현재 코드 기준으로 통합했습니다.
- `reporting/docs/README.md`, `beginner-guide.md`, `process-flow.md`, `local-run.md`를 보강해 재무제표 생성, 제출본 버전, 주석 마트, 감독보고 제출, 하위 모듈 구조, 로컬 Gradle 검증 흐름을 연결했습니다.
- `reconciliation/README.md`, `reporting/README.md`, 전사 `docs/README.md`에서 잘못된 standalone Docker 실행 안내를 현재 `java-library` 모듈 기준의 IntelliJ/Gradle 검증 방식으로 교체했습니다.
- `.run/Reconciliation Module Tests.run.xml`, `.run/Reporting Module Tests.run.xml`을 추가했습니다.
- `ReconciliationService`, `ReportingBatchAdapter`, `LocalRegulatoryFilingGatewayAdapter`에 운영 개선 필요 지점을 `@todo`로 명시했습니다.

## 8차 검증

```powershell
.\gradlew :reconciliation:test :reporting:core:test :reporting:api:test :reporting:batch:test --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- reconciliation/reporting 테스트 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 다음 순서는 foundation/infra 문서 통합입니다.

## 9차 완료 상세

### foundation/infra

- `contracts`, `shared-kernel`에 `local-run.md`를 추가하고 library 모듈 컴파일 검증 흐름을 README/docs에 연결했습니다.
- `master-data/docs`에 `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 SCD2 기준정보, 변경 요청 승인/반영, 포트/어댑터 흐름을 통합했습니다.
- `governance/docs`에 `beginner-guide.md`, `process-flow.md`, `schema.md`, `local-run.md`를 추가해 감사 로그, 승인, SOD, Auth 역할 반영 흐름을 통합했습니다.
- `auth/docs`를 신설해 로그인, JWT, roleVersion, 내부 역할 반영 API, 로컬 실행 설정을 정리했습니다.
- `config-server/docs`를 신설해 `config-repo` 기반 중앙 설정 조회 흐름과 로컬 실행 방법을 정리했습니다.
- `gateway/docs/README.md`, `local-run.md`를 추가하고 `concept.md`를 현재 `config-repo/gateway-service.yml` 라우트 기준으로 보강했습니다.
- 깨진 `discovery/docs/concept.md` 원문은 `discovery/docs/archive/concept_legacy_corrupt_2026-06-11.md`로 이동해 보존하고, 새 `concept.md`, `README.md`, `local-run.md`를 작성했습니다.
- `auth`, `gateway`, `discovery`, `config-server`, `master-data`, `governance` README에 IntelliJ 실행 순서와 PowerShell Gradle 명령을 보강했습니다.
- `.run/Config Server bootRun.run.xml`, `.run/Discovery bootRun.run.xml`, `.run/Auth bootRun.run.xml`, `.run/Master Data bootRun.run.xml`, `.run/Governance bootRun.run.xml`, `.run/Gateway bootRun.run.xml`, `.run/Foundation Library Compile.run.xml`, `.run/Foundation Infra Tests.run.xml`을 추가했습니다.
- `InMemoryLoginAttemptAdapter`, `JwtAuthenticationFilter`, `MasterDataChangeRequestService`, `MasterApprovalService`에 운영 개선 필요 지점을 `@todo`로 명시했습니다.

## 9차 검증

```powershell
.\gradlew :contracts:compileJava :shared-kernel:compileJava :master-data:test :governance:test :auth:test :gateway:test :discovery:test :config-server:assemble --console=plain --max-workers=1 --no-daemon
git diff --check
```

결과:
- foundation/infra Gradle 검증 성공
- IntelliJ `.run` XML 파싱 성공
- 변경 문서 상대 링크 검사 성공
- `git diff --check` 오류 없음, CRLF 안내만 출력
- 모듈별 문서 통합 진행표 기준 0~9차 완료

## 10차 후속 상세

### standalone Application 검토 반영

- `asset-lease`는 기존 추적 `AssetLeaseApplication`이 정식 실행 앱이므로, Gemini가 추가한 API/BATCH Application 후보로 생긴 main class 중복을 제거했습니다.
- `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`는 현재 단일 `java-library` 모듈이므로 Application 클래스만 추가하지 않고 기존 library 검증 흐름을 유지했습니다.
- `deposit:batch`는 실제 하위 프로젝트이므로 Spring Boot 플러그인, Boot BOM, `bootJar` mainClass, H2 runtime, Batch test 의존성을 추가했습니다.
- `reporting:api`, `reporting:batch`는 실제 하위 프로젝트이므로 Spring Boot 플러그인과 mainClass를 추가하고 `scanBasePackages`를 `com.ho.account.reporting`으로 제한했습니다.
- `reporting` memory 모드용 `InMemoryLedgerBalanceAdapter`를 추가해 journal-ledger 없이 로컬 API/BATCH 컨텍스트를 기동할 수 있게 했습니다.
- `deposit` 로컬 학습용 `LocalDepositMasterDataAdapter`, `LocalDepositJournalPostingAdapter`를 추가해 외부 master-data/journal-ledger 없이 단독 실행을 확인할 수 있게 했습니다.
- `.run/Deposit API bootRun.run.xml`, `.run/Deposit Batch Context.run.xml`, `.run/Reporting API bootRun.run.xml`, `.run/Reporting Batch Context.run.xml`을 추가했습니다.

## 10차 검증

```powershell
.\gradlew :asset-lease:api:bootJar :asset-lease:batch:bootJar :deposit:api:bootJar :deposit:batch:bootJar :reporting:api:bootJar :reporting:batch:bootJar --console=plain --max-workers=1 --no-daemon
.\gradlew :expenditure-resolution:compileJava :payable:compileJava :receivable:compileJava :reconciliation:compileJava :tax:compileJava --console=plain --max-workers=1 --no-daemon
```

결과:
- 검증 결과는 2026-06-12 작업 워크로그에 최신 기록합니다.

## 11차 후속 상세

### asset-lease split 실행 구조 반영

- `asset-lease`를 `core`, `api`, `batch` Gradle 하위 프로젝트로 분리했습니다.
- 기존 `:asset-lease`는 `:asset-lease:core`를 노출하는 호환 wrapper로 유지했습니다.
- API 실행 클래스는 `com.ho.account.asset.api.AssetLeaseApiApplication`, Batch 실행 클래스는 `com.ho.account.asset.batch.AssetLeaseBatchApplication`입니다.
- `asset-lease:core`는 업무 규칙, 포트, JPA/Kafka 어댑터를 담는 library 모듈입니다.
- `asset-lease:api`와 `asset-lease:batch`는 각각 core를 참조하는 standalone Boot 실행 모듈입니다.
- `.run/Asset Lease API bootRun.run.xml`은 `:asset-lease:api:bootRun`, `.run/Asset Lease Batch Context.run.xml`은 `:asset-lease:batch:bootRun`, `.run/Asset Lease Tests.run.xml`은 `:asset-lease:core:test`를 실행합니다.

## 11차 검증

```powershell
.\gradlew projects --console=plain
.\gradlew :asset-lease:core:test :asset-lease:api:bootJar :asset-lease:batch:bootJar :expenditure-resolution:core:compileJava --console=plain --max-workers=1
.\gradlew :asset-lease:test :asset-lease:compileJava --console=plain --max-workers=1
```

결과:
- asset-lease core 테스트 성공
- asset-lease API/BATCH bootJar 성공
- expenditure-resolution core compileJava 성공
- 기존 호환 경로 `:asset-lease:test`, `:asset-lease:compileJava` 성공
## 12차 후속 상세

### ecl core pipeline 경계 리팩토링

- `StagingCalculationPipeline`, `EadCrmCalculationPipeline`, `ForwardLookingEclCalculationPipeline`을 `ecl-core/application/pipeline`에 추가했습니다.
- `ecl-batch`의 `StagingProcessor`, `EadCrmProcessor`, `EclProcessor`는 Spring Batch adapter로 축소하고 실제 업무 산출 순서는 core pipeline이 담당하게 했습니다.
- `AllowanceCalculationService`도 같은 core pipeline을 재사용해 API/단건 산출과 batch 산출의 업무 순서를 맞췄습니다.
- `ecl` README/docs와 batch config 주석에 core pipeline / batch adapter 경계를 보강했습니다.

## 12차 검증

```powershell
.\gradlew :ecl:ecl-core:test :ecl:ecl-batch:test --console=plain --max-workers=1
```

결과:
- ecl core 테스트 성공
- ecl batch 테스트 성공
- batch processor의 계산 서비스/금액 산식 직접 참조 검색 결과 없음
- ecl Java TODO 검색 결과 없음
## 13차 후속 상세

### journal-ledger balance reaggregation Batch 전환

- `BalanceReaggregationBatchConfig`는 Spring Batch Job/Step wiring만 담당하도록 정리했습니다.
- `BalanceReaggregationTasklet`을 추가해 Step 실행 시 core `LedgerService.reaggregateLedgerBalancesForPeriod`를 호출합니다.
- `BatchDateRangeParameterUtils`를 추가해 `startDate/endDate`, `fromDate/toDate`, `baseDate`, `targetDate` JobParameter를 업무 기간으로 변환합니다.
- `journal-ledger:batch` H2 local datasource/JPA/Batch YAML 계층과 Batch test 의존성을 보정했습니다.
- `journal-ledger` 문서, `docs/local-development.md`, IntelliJ `.run` 설정에 `dailyBalanceReaggregationJob` 실행 명령을 추가했습니다.

## 13차 검증

```powershell
.\gradlew :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --console=plain --max-workers=1
.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob baseDate=2026-04-30" --console=plain --max-workers=1
```

결과:
- journal-ledger core/api/batch 테스트 성공
- `dailyBalanceReaggregationJob` H2 local 실행 성공, Job status `COMPLETED`
- ecl/journal-ledger Java TODO 및 깨진문자 검색 결과 없음
- `git diff --check` 오류 없음(CRLF 안내만 출력)

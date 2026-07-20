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
| 13 | `loan` journal port 경계 리팩토링 | Done | 일일 이자 전표 포트화, 전표 값 참조 정합성, Batch JobRegistry/local-run 정리 |
| 14 | `account-mart` 담보 상세 DQ/LGD 연결 | Done | `OdsApartCollDetail`을 port/adapter/application service로 실제 담보 DQ 흐름에 연결 |
| 15 | `asset-lease` 감가상각 Batch 경계 리팩토링 | Done | Batch preview 계산과 JDBC bulk 반영을 분리해 이중 상각/완료 상태 누락 리스크 제거 |
| 16 | `tax` API/core command 경계 리팩토링 | Review ready | HTTP DTO/Controller를 api로 이동하고 core는 `TaxInvoiceCommand`/도메인 규칙만 소유 |
| 17 | `expenditure-resolution` API/core command 경계 리팩토링 | Review ready | HTTP DTO/Controller를 api로 이동하고 core는 command/port/master-data code 참조만 소유 |
| 18 | `payable` API/core command 경계 리팩토링 | Review ready | HTTP DTO/Controller를 api로 이동하고 core는 command/use case/domain 규칙만 소유, Batch JobRegistry 정리 |
| 19 | `receivable` API/core command 경계 리팩토링 | Review ready | HTTP DTO/Controller를 api로 이동하고 core는 command/use case/domain 규칙만 소유, Batch JobRegistry 정리 |
| 20 | `reconciliation` API/core command 경계 리팩토링 | Review ready | HTTP DTO/Controller를 api로 이동하고 core는 command/use case/domain 규칙만 소유, Batch JobRegistry 정리 |
| 21 | `reporting` API response DTO 경계 리팩토링 | Review ready | API가 core 도메인 객체를 직접 반환하지 않고 response DTO로 외부 JSON 계약을 고정 |
| 22 | `deposit` command/Batch 기준일 경계 리팩토링 | Review ready | 계좌 개설 command 입력 검증과 Batch `asOfDate` fail-fast로 업무 재실행성 강화 |
| 23 | `master-data` typed applier/대량 통계 경계 리팩토링 | Review ready | 4개 기준정보 typed applier, effectiveDate 정합성, core→batch 역참조 제거, DB COUNT 집계 |
| 24 | `governance` 승인 경계/단독 실행 리팩토링 | Review ready | 실제 기능 Bean 스캔, 권한 회수 승인, fail-closed apply, H2 단독 실행 |
| 25 | `auth` 인증/역할 승인 멱등 경계 리팩토링 | Review ready | API/core DTO 분리, 역할 시점 고정, memory/JPA 정합성, approvalTraceId 멱등 반영 |

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
- 당시 `payable`과 `receivable`은 `java-library` 검증 흐름을 기준으로 문서화했습니다. 이후 `payable`은 `core/api/batch` 실행 구조와 API/core command 경계로 갱신했습니다.
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
- 당시 `expenditure-resolution`, `payable`, `receivable`, `reconciliation`, `tax`는 단일 `java-library` 흐름을 기준으로 검토했습니다. 이후 `tax`, `expenditure-resolution`, `payable`은 `core/api/batch` 실행 구조와 API/core command 경계로 갱신했습니다.
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
## 14차 후속 상세

### closing core/batch 경계 리팩토링

- `closing:batch`의 `FxValuationService`, `EclProvisionService`, `ClosingSlipNoFactory`를 `closing:core/application/service`로 이동했습니다.
- `FxExchangeRateLookupPort`, `AllowanceBalanceLookupPort`, `ClosingJournalEntryPort`를 추가해 core가 master-data/journal-ledger 기술 구현을 직접 알지 않게 했습니다.
- `closing:batch/adapter/out`에 환율 조회, 기존 충당금 잔액 조회, 전표 생성 어댑터를 추가했습니다.
- `FxValuationBatchConfig`, `EclProvisionBatchConfig`는 Job/Step/Reader/Tasklet과 core 위임만 담당하도록 정리했습니다.
- closing README/docs/local-run/process-flow와 초보자용 주석을 core/batch 책임 경계 기준으로 최신화했습니다.

## 14차 검증

```powershell
.\gradlew :closing:core:test :closing:batch:test --console=plain --max-workers=1
.\gradlew :closing:api:compileJava --console=plain --max-workers=1
```

결과:
- closing core/batch 테스트 성공
- closing api compileJava 성공
- closing core Spring Batch 타입 직접 참조 검색 결과 없음
- closing Java/문서 TODO 및 깨진문자 검색 결과 없음

추가 결과:
- `closing:batch:bootRun` local profile 컨텍스트 기동 성공
- closing API/BATCH local logback XML 파싱 성공

### tax API/core command 경계 리팩토링

- `tax:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거하고, `TaxInvoiceCommand` 기반 유즈케이스 경계로 정리했습니다.
- `tax:api`로 `APInvoiceController`, `TaxInvoiceRequestDto`, `TaxInvoiceDto`를 이동해 HTTP 요청 검증과 응답 매핑을 API adapter 책임으로 분리했습니다.
- `TaxInvoiceRef`에 `purchase()`, `active()`, `usableForPurchaseSettlement()`를 추가해 외부 모듈이 취소/매입 정책을 명시적으로 판단하도록 했습니다.
- `expenditure-resolution`의 지출결의/AP 지급 검증은 문자열 직접 비교 대신 계약 객체 메서드를 사용하도록 보강했습니다.
- tax README/docs/local-run/process-flow/schema와 운영 로그를 새 경계 기준으로 최신화했습니다.

검증:

```powershell
.\gradlew :tax:core:test :tax:api:compileJava :tax:batch:compileJava :expenditure-resolution:core:test --console=plain --max-workers=1
```

결과:
- tax core 테스트, api 컴파일, batch 컴파일 성공
- tax API/BATCH local context smoke 성공
- tax batch JobRegistry 조기 초기화 경고 제거 확인
- expenditure-resolution core 테스트 성공
### expenditure-resolution API/core command 경계 리팩토링

- expenditure-resolution:core에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했습니다.
- ExpenditureResolutionCommand, APPaymentCommand를 추가해 API DTO와 core 업무 입력을 분리했습니다.
- ExpenditureResolutionService는 master-data 내부 Repository/Entity 대신 MasterDataQueryPort를 사용합니다.
- Budget과 Invoice는 master-data 엔티티 JPA 연관 대신 코드 값을 저장합니다.
- API 통합 테스트를 expenditure-resolution:api 테스트로 이동했고 Batch JobRegistry 지연 등록 설정을 추가했습니다.

검증:

`powershell
.\gradlew :expenditure-resolution:core:compileJava :expenditure-resolution:api:compileJava :expenditure-resolution:batch:compileJava --console=plain --max-workers=1
.\gradlew :expenditure-resolution:core:test :expenditure-resolution:api:test --console=plain --max-workers=1
.\gradlew :expenditure-resolution:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
.\gradlew :expenditure-resolution:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
`

결과:
- expenditure-resolution core/api/batch 컴파일 성공
- expenditure-resolution core/api 테스트 성공
- API/BATCH local H2 context smoke 성공
- Batch JobRegistry 조기 초기화 경고 제거 확인

### receivable API/core command 경계 리팩토링

- `receivable:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했습니다.
- `SalesInvoiceCommand`, `CollectionCommand`, `ManualMatchingCommand`를 추가해 API/Batch/내부 호출이 같은 core 업무 입력을 사용하게 했습니다.
- `receivable:api`가 Controller, 요청 DTO, 응답 DTO, Bean Validation을 소유하고 request DTO는 command로 변환합니다.
- `receivable:batch`에는 JobRegistry 지연 등록 설정을 추가해 local/H2 Batch 컨텍스트의 조기 초기화 경고를 제거했습니다.

검증:

```powershell
.\gradlew :receivable:core:test :receivable:api:test :receivable:batch:compileJava --console=plain --max-workers=1
.\gradlew :receivable:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
.\gradlew :receivable:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```
### reconciliation API/core command 경계 리팩토링

- `reconciliation:core`에서 HTTP Controller/DTO와 Web/Validation 의존을 제거했습니다.
- `AssignDifferenceCommand`, `DifferenceReasonCodeCommand`, `ReconciliationRuleCommand`, `ReconciliationUnitCommand`, `ResolveDifferenceCommand`, `RunReconciliationCommand`를 추가해 API/BATCH 공통 업무 입력을 core command로 통일했습니다.
- `reconciliation:api`가 `ReconciliationController`, 요청/응답 DTO, Bean Validation을 소유하고 request DTO는 command로 변환합니다.
- `reconciliation:batch`에는 `ReconciliationBatchJobRegistryConfiguration`을 추가해 local/H2 Batch 컨텍스트의 JobRegistry 조기 초기화 경고를 제거했습니다.
- reconciliation README/docs/local-run/process-flow/beginner-guide를 API DTO -> core command -> domain/service 흐름 기준으로 최신화했습니다.

검증:

```powershell
.\gradlew :reconciliation:core:test :reconciliation:api:compileJava :reconciliation:batch:compileJava --console=plain --max-workers=1
.\gradlew :reconciliation:api:bootRun --args="--spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
.\gradlew :reconciliation:batch:bootRun --args="--spring.main.web-application-type=none --spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

결과:
- reconciliation core 테스트, api 컴파일, batch 컴파일 성공
- API/BATCH local H2 context smoke 성공
- Batch JobRegistry 조기 초기화 경고 제거 확인
### reporting API response DTO 경계 리팩토링

- `ReportingController`가 `FinancialStatement`, `DisclosureNoteMart`, `RegulatoryReportSubmission`, `RegulatoryFiling` 도메인 객체를 직접 반환하지 않도록 정리했습니다.
- `reporting:api/.../dto`에 `FinancialStatementResponseDto`, `ReportLineResponseDto`, `DisclosureNoteMartResponseDto`, `DisclosureNoteMartEntryResponseDto`, `RegulatoryReportSubmissionResponseDto`, `RegulatoryFilingResponseDto`, `RegulatoryFilingLineResponseDto`, `JournalDetailSummaryResponseDto`를 추가했습니다.
- Drill-down 응답도 `JournalDetailSummaryResponseDto`로 감싸 journal-ledger contract 객체가 HTTP 응답 계약에 직접 노출되지 않게 했습니다.
- reporting README/docs/process-flow/beginner-guide를 core domain -> API response DTO 흐름 기준으로 최신화했습니다.

검증:

```powershell
.\gradlew :reporting:api:test --console=plain --max-workers=1
```

결과:
- reporting API 테스트 성공
- 기존 JSON 필드명 계약 유지 확인
### deposit command/Batch 기준일 경계 리팩토링

- `OpenAccountCommand`에 고객/상품/통화 코드 필수값, 통화 코드 `Locale.ROOT` 대문자 정규화, 초기입금/금리 음수 방어를 추가했습니다.
- `DepositAccountIntegrityBatchConfig`가 `asOfDate` 누락 시 현재 날짜로 대체하지 않고 fail-fast 하도록 변경했습니다.
- `DepositAccountIntegrityBatchConfigTest`를 추가해 `asOfDate=yyyy-MM-dd` 파라미터 정책을 검증했습니다.
- `DepositServiceTest`에 계좌 개설 command 입력 검증 테스트를 추가했습니다.
- deposit README/docs/local-run에 API DTO -> core command, Batch 기준일 필수 정책을 초보자용 업무 흐름으로 보강했습니다.

검증:

```powershell
.\gradlew :deposit:core:test :deposit:batch:test :deposit:api:bootJar :deposit:batch:bootJar --console=plain --max-workers=1
```

결과:
- deposit core/batch 테스트 성공
- deposit api/batch bootJar 성공

## 23차 후속 상세

### master-data typed applier와 일일 유효성 통계 경계 리팩토링

- `MasterDataValidityReportPipeline`이 batch DTO를 반환하던 역방향 참조를 제거하고 core `MasterDataValidityReport`를 반환하도록 변경했습니다.
- `MasterDataValidityStatisticsPort`와 JPA 통계 어댑터를 추가해 계정과목/부서/상품/거래처 전체 행 조회와 Java stream 집계를 DB `COUNT` 쿼리로 전환했습니다.
- 기준일 `asOfDate`가 없으면 현재 날짜로 대체하지 않고 fail-fast 하도록 재실행 정책을 고정했습니다.
- `ACCOUNT_SUBJECT`, `BUSINESS_PARTNER`, `DEPARTMENT`, `PRODUCT` typed applier를 구현했습니다.
- 변경 payload JSON 파싱은 `MasterDataChangePayloadDecoder` 출력 포트와 Jackson 어댑터로 분리했습니다.
- CREATE/UPDATE payload key와 승인 `targetKey`가 다르면 거부하며, DEACTIVATE는 payload 없이 승인 `effectiveDate`를 종료일로 사용합니다.
- IntelliJ `Master Data bootRun`과 local-run 문서를 Config/Discovery/Vault 없이 H2 단독 실행 가능한 설정으로 최신화했습니다.
- 남은 `@todo`는 미지원 통화/환율/회계기간 typed applier와 `requestedVersion` 충돌 검사입니다.

검증:

```powershell
.\gradlew :master-data:test --console=plain --max-workers=1
.\gradlew :master-data:test --tests "*JpaMasterDataValidityStatisticsAdapterTest" --console=plain --max-workers=1
.\gradlew :master-data:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

결과:
- master-data 전체 테스트 성공
- H2에서 네 기준정보 DB `COUNT` 집계 테스트 성공
- local/H2 Spring Boot 컨텍스트 기동 성공
- core의 batch 패키지/DTO 역참조 검색 결과 없음
- batch 패키지의 if/for/math/stream 업무 연산 검색 결과 없음

## 24차 후속 상세

### governance 승인 경계와 단독 실행 보강

- `GovernanceApplication`이 실제 `com.ho.account.audit` 기능과 승인 반영에 필요한 master-data service/adapter/repository/entity를 명시적으로 스캔하도록 수정했습니다.
- `GovernanceApplicationContextTest`가 `AuditController`, 감사/승인 유스케이스, master-data 변경 요청 유스케이스 Bean을 확인해 빈 서버 회귀를 차단합니다.
- 권한 회수 API는 즉시 삭제 대신 `AUTHORIZATION / DELETE` 승인 요청을 만들고 `202 Accepted` 승인 접수 정보를 반환합니다.
- `SystemRoleApprovalApplyAdapter`는 역할 CREATE, 권한 CREATE/DELETE만 지원하고 나머지 조합과 알 수 없는 masterType을 fail-closed로 거부합니다.
- H2와 PostgreSQL JDBC 런타임 의존성을 추가하고 IntelliJ `Governance bootRun`, README/docs를 H2 단독 API 실행과 폐기 가능한 PostgreSQL smoke 절차 기준으로 최신화했습니다.
- 남은 `@todo`는 역할/권한 생성 API의 preview 응답을 승인 접수 DTO로 통일하는 작업과 외부 Auth 반영 outbox/inbox 원자성 경계입니다.

검증:

```powershell
.\gradlew :governance:compileJava --console=plain --max-workers=1
.\gradlew :governance:test :governance:bootJar --console=plain --max-workers=1
.\gradlew :governance:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --spring.flyway.enabled=false" --console=plain --max-workers=1
```

결과:
- governance 컴파일, 전체 테스트, bootJar 성공
- local/H2 Spring Boot 컨텍스트 기동 성공, JPA Repository 12개와 실제 audit/master-data Bean 등록 확인
- PostgreSQL 드라이버 패키징은 완료했으나 실제 PostgreSQL/Flyway 실행은 미검증

## 25차 후속 상세

### auth 인증 결과 경계와 역할 승인 멱등성 리팩토링

- `AuthService`가 API `LoginResponse`를 직접 반환하던 역참조를 제거하고 core `LoginCommand`/`AuthenticationResult` 경계로 전환했습니다.
- 공용 `Clock`의 한 시점에서 유효 역할을 확정하고 같은 역할 스냅샷을 로그인 응답과 JWT claim에 사용합니다.
- token-version 검증은 버전뿐 아니라 계정 활성, 관리 잠금, 유효 역할 존재 여부도 확인합니다.
- 로그인 오케스트레이터의 광범위한 readOnly 트랜잭션을 제거하고 각 JPA 출력 어댑터가 짧은 읽기/쓰기 경계를 소유하도록 정리했습니다.
- memory 역할 교체 어댑터가 버리던 `dataScope`, `validFrom`, `validTo`를 보존하도록 수정했습니다.
- Governance `approvalTraceId`와 SHA-256 request fingerprint를 실제 멱등 경계로 연결했습니다.
- JPA 어댑터는 사용자별 비관적 lock과 `AUTH_ROLE_ASSIGNMENT_APPLY_LOG` 이력으로 같은 승인 재시도의 역할 재교체/roleVersion 중복 증가를 막습니다.
- 같은 trace가 다른 사용자/역할 내용으로 재사용되면 memory/JPA 모두 fail-closed 처리합니다.
- `RoleAssignment`은 DB 길이와 `[validFrom, validTo)` 기간 규칙을 도메인 생성 시 검증합니다.
- IntelliJ `Auth bootRun`과 문서를 Config/Eureka 없이 H2 + Flyway + JPA validate로 단독 실행하도록 최신화했습니다.

검증:

```powershell
.\gradlew :auth:test :auth:bootJar --console=plain --max-workers=1
.\gradlew :auth:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.data.redis.repositories.enabled=false --spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password= --spring.jpa.hibernate.ddl-auto=validate --spring.flyway.enabled=true --auth.persistence.mode=jpa --auth.login-security.store=jpa" --console=plain --max-workers=1
```

결과:
- auth 전체 32개 테스트와 bootJar 성공
- H2에서 Flyway V70~V72 적용, JPA Repository 3개 등록, Hibernate schema validate, Spring Boot 컨텍스트 기동 성공
- core의 `auth.api` 참조 검색 결과 없음
- PostgreSQL/Flyway와 동시 승인/로그인 실패 부하는 실제 PostgreSQL 환경에서 미검증
- 남은 코드 `@todo`는 평문 비밀번호 해시 승격, 최초 로그인 실패 원자적 upsert, 멱등 이력 archive/retention 정책 3건

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
| 26 | `gateway` 전역 인증/신뢰 헤더 경계 리팩토링 | Review ready | 모든 API 기본 인증, 내부 Auth 차단, JWT 포트 분리, roleVersion 401/503 구분, 8000/Docker 정합화 |
| 27 | `discovery` registry/readiness/컨테이너 경계 리팩토링 | Review ready | 8761 standalone, 실제 Config Client/actuator, register 생명주기, Docker health/service_healthy, 보안·HA TODO |
| 28 | `config-server` native 저장소/readiness/컨테이너 경계 리팩토링 | Review ready | strict property-source probe 대상 강제 재실행 및 전체 9 tests/bootJar 재검증, 8888 standalone, 보안·Git TODO |
| 29 | `contracts`/`shared-kernel` 2차 계약·공유 경계 리팩토링 | Review ready | Jackson BOM 정렬, 전표 불변 계약, 실제 마스킹, capability registry와 ECL eventId 경로를 영향 범위 140 tests로 재검증 |
| 30 | `master-data` 변경 승인·버전·런타임 경계 리팩토링 | Review ready | SCD2 requestedVersion 검증, Governance 멱등 계보, applier registry fail-closed, 요청 잠금, 8082/JDK17/Docker 정합화, 82 tests/2 bootJars |
| 31 | `master-data` SCD2 유효기간·과거 거래처 조회 후속 보강 | Review ready / uncommitted | 신규 기간/상위 참조 선검증, 현재/과거 거래처 쿼리 분리, 중복 기간 fail-closed, 88 tests/2 bootJars |
| 32 | `master-data` 조회·회계기간 경계 2차 점검 | Review ready / uncommitted | DB 활성 조회/검색, 최신 기준일 환율, 회계기간 포트·잠금·상태 규칙, 미사용 스켈레톤 정리, 영향 범위 136 tests/2 bootJars |
| 33 | `loan` 값 참조·상태 전이·EIR·발생 Batch 정합성 점검 | Review ready / uncommitted | provider 값 경계, PENDING→ACTIVE, BigDecimal EIR, 단일 스케줄, 발생 재실행, 169 affected tests/2 Loan bootJars |
| 34 | `closing` 상태 전이·FX/ECL·재실행·런타임 경계 점검 | Review ready / uncommitted | fail-closed 마감, posted-journal FX, ECL DB 선집계, 멱등 slip, 최소 runtime scan, 158 affected tests/2 bootJars |
| 35 | foundation 잔여 검증·phantom `app` 정리 | Review ready / uncommitted | Config 보류 해소, Jackson 버전 충돌과 ECL 예외 테스트 수정, 소스 없는 `:app` include 제거, 140 affected tests/Config bootJar |
| 36 | `origin/main` 동기화·Issue #29 구조 충돌 해결 | Draft PR ready | `f3d33ea` 기준 internal-audit split 보존, 82개 transfer 경로 복구, 176 affected tests/3 bootJars; internal-audit source/test NO-SOURCE 위험 기록 |

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

## 26차 후속 상세

### gateway 전역 인증과 런타임 경계 리팩토링

- 라우트마다 선택 적용하던 `JwtAuthenticationFilter`를 모든 `/api/**`에 기본 적용하는 `GlobalFilter`로 전환했습니다.
- 외부 `X-Auth-*`를 항상 제거하고 JWT/roleVersion 검증이 모두 성공한 뒤 `AuthenticatedPrincipal`에서 신뢰 헤더를 다시 생성합니다.
- `/api/auth/login`의 POST만 공개하고 token-version/internal Auth 경로는 라우트 설정과 무관하게 외부에서 차단합니다.
- `AccessTokenVerifier` 포트와 `JjwtAccessTokenVerifier` 어댑터를 추가해 JJWT 기술을 HTTP 필터에서 분리했습니다.
- 누락된 roleVersion을 1로 가정하지 않고, 필수 iat/exp/roles와 양의 정수 roleVersion, 헤더 안전 코드를 fail-closed 검증합니다.
- token-version 결과를 `VALID/REJECTED/UNAVAILABLE`로 나눠 권한 변경은 401, Auth timeout/빈 응답/장애는 503으로 반환합니다.
- 정상 결과만 canonical username 기준으로 캐시하고 request ID 길이/문자 검증을 추가했습니다.
- Gateway/Config/Docker 포트를 8000으로 통일하고 Docker 내부 Auth 주소를 `http://auth:8084`로 설정했습니다.
- Dockerfile을 프로젝트 기준 JDK 17과 단일 bootJar 복사 구조로 수정하고 standalone IntelliJ 실행 설정을 추가했습니다.
- README/docs/process-flow/local-run을 전역 인증과 실제 데이터 흐름, 종료/메모리 관리 기준으로 최신화했습니다.
- 남은 `@todo`는 JWKS 키 회전, 역할 변경 이벤트 기반 다중 노드 cache 무효화, Auth 내부 API 서비스 인증, 레거시 account catch-all 제거입니다.

검증:

```powershell
.\gradlew :gateway:test :gateway:bootJar --console=plain --max-workers=1 --no-daemon
.\gradlew :gateway:bootRun --args="--spring.profiles.active=local --server.port=8000 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --auth.token-version-validation.enabled=false" --console=plain --no-daemon
Invoke-RestMethod http://localhost:8000/actuator/health
```

결과:
- gateway 전체 30개 테스트 성공, 실패/오류/skip 0건
- `bootJar` 성공
- local standalone Netty 서버 포트 8000 기동 및 actuator health `UP`
- Config/Compose YAML 파싱 테스트로 로그인 단일 공개 경로, 8000 포트, Auth 컨테이너 주소와 의존 순서 확인
- Docker CLI가 설치되어 있지 않아 실제 `docker compose config`와 이미지 빌드/실행은 미검증
- live Config/Discovery/Auth 라우팅 통합과 다중 노드 cache 무효화는 별도 환경 검증 필요
## 27차 후속 상세

### discovery registry 생명주기와 readiness 경계 리팩토링

- Config Server가 없으면 8080으로 뜨고 자신을 Eureka client로 등록/fetch하려던 기본값을 8761 단일 노드 server 정책으로 고정했습니다.
- 문서에만 있던 Config/health/prometheus/tracing 설정을 실제 Config Client, actuator, Prometheus, Brave/Zipkin 의존성과 연결했습니다.
- `DiscoveryApplicationTests`는 actuator readiness와 임시 instance register -> lookup -> cancel 생명주기를 검증합니다.
- `DiscoveryConfigurationPolicyTest`는 local/config YAML, 루트/모듈 Compose, JDK 17 Dockerfile/readiness와 모든 Discovery 의존 서비스의 `service_healthy` 조건을 검증합니다.
- Dockerfile의 JDK 21/다중 wildcard COPY를 JDK 17 단일 bootJar로 바꾸고 image readiness healthcheck를 추가했습니다.
- 루트 Compose는 Discovery container 주소/Logstash/Zipkin 값을 주입하고 14개 의존 서비스가 readiness healthy를 기다리게 했습니다.
- 테스트 전용 console Logback과 `Discovery standalone bootRun` IntelliJ 설정을 추가했습니다.
- 기존 전화번호부 비유와 archive 이력을 유지하면서 README/docs에 register/heartbeat/fetch/cancel/eviction/self-preservation 흐름을 상세화했습니다.
- 운영 인증 방식과 peer 주소가 확정되지 않아 private network+mTLS/인증, multi-AZ peer sync/장애 전환을 Config 코드 `@todo`로 남겼습니다.

검증:

```powershell
.\gradlew :discovery:test :discovery:bootJar --console=plain --max-workers=1 --no-daemon
.\gradlew :discovery:bootRun --args="--spring.profiles.active=local --server.port=8761 --spring.cloud.config.enabled=false --eureka.client.register-with-eureka=false --eureka.client.fetch-registry=false --management.tracing.enabled=false" --console=plain --no-daemon
```

결과:
- discovery 전체 6개 테스트 성공, 실패/오류/skip 0건
- `bootJar` 성공
- standalone 8761 readiness `UP`, Dashboard/registry/Prometheus HTTP 200, JVM metric 노출 확인
- runtime 종료 후 Discovery/Gradle 프로세스가 남지 않음을 확인
- Docker CLI가 설치되어 있지 않아 실제 image/Compose 실행은 미검증
- live Config Server와 실제 여러 서비스 heartbeat/load-balancing, 운영 self-preservation 임계값은 별도 통합/부하 환경 검증 필요
## 28차 후속 상세

### config-server native 저장소와 strict readiness 경계 리팩토링

- 테스트가 없던 Config Server에 실제 `/{application}/{profile}` HTTP 조회와 설정 정책 테스트를 추가했습니다.
- Spring 기본 health가 빈 Environment를 UP으로 볼 수 있는 한계를 보완해, 대표 `master-data/default`의 property source가 하나 이상일 때만 UP인 `ConfigRepositoryHealthIndicator`를 추가했습니다.
- 정상 source, 빈 source, 조회 예외를 각각 UP/DOWN으로 검증하고 health 응답에는 설정 값/URL을 노출하지 않습니다.
- native 저장소 위치를 `CONFIG_REPO_LOCATION`으로 외부화하고 로컬은 `file:./config-repo`, Docker는 `file:/config-repo`를 사용합니다.
- Dockerfile을 JDK 17 단일 `bootJar`와 readiness healthcheck로 통일하고, 설정 원본을 이미지에 포함하지 않고 Compose read-only volume으로만 연결했습니다.
- root Compose의 Config Server 자체에서 불필요한 Eureka/Config/Kafka/Redis 환경을 제거하고 15개 의존 서비스가 `service_healthy`를 기다리게 했습니다.
- 기존 레시피 본사 비유를 유지하면서 profile/property source 우선순위, `optional` fallback, 클라이언트 재시작/refresh, 종료 절차를 문서화했습니다.
- 남은 `@todo`는 Config 조회 API private network+mTLS/서비스 인증과 승인된 Git backend/고정 label/refresh/rollback 정책입니다.

검증:

```powershell
.\gradlew :config-server:test :config-server:bootJar --rerun-tasks --console=plain --max-workers=1 --no-daemon
java -jar config-server/build/libs/config-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=native --server.port=8888 --spring.cloud.config.server.native.search-locations=file:./config-repo
```

결과:
- 1차 config-server 전체 검증에서 9개 테스트 성공, 실패/오류/skip 0건
- 이후 health 오류 상세 비노출 보강 소스와 테스트 class 생성은 확인했으나, 대상 테스트 재실행은 Windows 페이지 파일 부족으로 새 결과를 만들지 못해 메모리 회복 후 재검증 필요
- 1차 `bootJar` 성공
- 1차 산출 JAR로 실제 8888 readiness UP, `master-data/default` HTTP 200/property source 1개, Prometheus HTTP 200/JVM 지표 확인
- runtime 종료 후 Config Server/Gradle 프로세스가 남지 않음을 확인
- Docker CLI가 없어 실제 image/Compose 실행은 미검증

## 29차 후속 상세

### contracts/shared-kernel 계약과 공용 경계 리팩토링

- `JournalEntryCommand`가 일자와 비어 있지 않은 라인을 검증하고 List를 불변 복사하도록 보강했습니다.
- `JournalLineCommand`가 `DEBIT/CREDIT`, 계정 코드, 금액 필수 형식을 생성 시점에 검증합니다.
- `AccountSubjectRef`는 명시적인 정상잔액 방향에서 `DEBIT/CREDIT` 외 값을 fail-closed 처리합니다.
- master-data `MonolithMasterDataQueryAdapter`가 계정과목/거래처/부서를 실제 기준일 SCD2 쿼리로 조회해 Closing 평가일 정합성을 보강했습니다.
- `@Masked`를 Jackson serializer에 실제 연결하고 사업자번호/계좌번호/이메일 마스킹과 미지원 패턴 fail-closed를 구현했습니다.
- Spring ApplicationContext 서비스 로케이터를 생성자 주입 불변 목록과 중복 이름 fail-fast 방식으로 변경했습니다.
- 구현 없는 `@DistributedLock` 사용을 ECL 소비자에서 제거했습니다.
- CDM 이벤트는 생산자의 `eventId`를 Spring Batch JobInstance 키로 사용하고, 완료 이벤트 재전달은 정상 중복으로 종료하며 다른 실패는 Kafka 정책으로 전파합니다.
- library 루트의 빈 Dockerfile/Compose 4개는 삭제하지 않고 각 `docs/archive/legacy-runtime-skeleton`로 이동했습니다.
- 기존 초보자 비유를 유지하면서 JVM 내부 Port 호출, 원격 Adapter, SCD2, 마스킹, 멱등 데이터 흐름을 실제 코드 기준으로 최신화했습니다.

남은 코드 TODO:

- 모든 Master Data 제공자의 기준일 조회 구현 후 호환 default 제거
- Source Document Map을 versioned DTO로 변경
- contracts/shared-kernel public API와 로컬 capability SPI 분리
- shared-kernel의 JPA/Kafka/Redis/Vault/관측/Swagger 전이 의존성을 convention/platform과 각 Adapter로 이동
- ECL 전용 BaseEntity/enum/event 계약 이동
- owner token/lease 갱신을 갖춘 실제 분산락 포트 구현

검증 상태:

- `git diff --check` 성공.
- 구현 없는 `DistributedLock` 사용 검색 0건.
- Config Server 대상 테스트, contracts 직접 `javac` 모두 64~128MB JVM도 시작하지 못할 정도의 Windows 페이지 파일 부족으로 중단했습니다.
- 생성된 Gradle/javac JVM과 임시 디렉터리는 정리했으며 IntelliJ/SonarLint 외 Java 프로세스는 남지 않았습니다.
- 자원 회복 후 `:shared-kernel:test :contracts:test :master-data:test :closing:core:test :ecl:ecl-api:test` 재실행이 필요합니다.

## 30차 후속 상세

### master-data 변경 승인·버전·런타임 경계 리팩토링

- CREATE/UPDATE/DEACTIVATE 요청 버전을 실제 SCD2 이력 수와 요청·승인·반영 직전에 대조합니다.
- target type별 applier를 불변 registry로 고정하고 중복 담당과 미지원 유형을 fail-closed 처리합니다.
- 승인/반영 조회에 비관적 잠금, 요청 aggregate에 낙관적 `lockVersion`을 적용했습니다.
- API Bean Validation과 버전 충돌 409 응답을 추가하고 신뢰 actor/payload 마스킹/직접 쓰기 정책은 구체적인 TODO로 남겼습니다.
- 8082, JDK 17, 단일 bootJar, DB readiness, Config/Compose/IntelliJ 설정과 초보자 문서를 실제 실행 구조에 맞췄습니다.
- 기존 V2 체크섬을 보존하면서 V3/V4/V5 forward migration을 추가하고 Governance 승인 `sourceReference`와 `appliedAt` 계보를 연결했습니다. 실제 신규 PostgreSQL은 V2 `CLOB` 선행 문제를 해결할 vendor별 baseline이 필요합니다.
- clean Gradle 검증에서 Master Data 57개와 Governance 25개, 총 82개 테스트 및 두 `bootJar`가 성공했고 정적/문서 링크 검사도 통과했습니다.

## 31차 후속 상세

### master-data SCD2 유효기간·과거 거래처 조회 후속 보강

- 신규 SCD2 기간과 계정과목/부서 상위 참조를 먼저 검증·조립한 뒤 현재 행을 종료하도록 호출 순서를 고정했습니다.
- 거래처 현재 조회와 과거 기준일 조회를 분리하고 기간 중복을 임의 선택하지 않도록 단건 쿼리를 fail-closed 처리했습니다.
- Master Data 63개와 Governance 25개, 총 88개 테스트 및 두 `bootJar`가 성공했습니다.

## 32차 후속 상세

### master-data 조회·환율·회계기간 경계 2차 점검

- 계정과목/상품 활성 목록과 거래처명 검색을 기준일·활성 조건이 있는 DB 쿼리로 옮겼습니다.
- 기준일 이전 최신 환율 한 건을 명시적으로 선택하고 ISO 통화 코드/양수 환율 및 활성 통화 중복을 fail-closed 검증합니다.
- 회계기간 변경을 비관적 잠금 애플리케이션 포트와 감사 actor/상태 전이 규칙을 가진 도메인 행위로 변경했습니다.
- 사용되지 않는 변경요청 전체 조회와 no-op/synthetic 호환 메서드를 제거하고 PostgreSQL baseline, pagination, TaxProfile, Loan/Closing 직접 의존을 후속 항목으로 기록했습니다.
- Master Data 73개, Governance 25개, Closing Core 19개, Journal Ledger Core 19개로 총 136개 테스트가 성공했고 Closing Batch 컴파일과 두 `bootJar`가 통과했습니다.

## 33차 후속 상세

### loan 값 참조·상태 전이·EIR·발생 Batch 정합성 점검

- Loan Aggregate의 Master Data 엔티티 관계를 거래처 ID, 통화/계정 코드 값으로 바꾸고 provider 타입은 로컬 인프라 어댑터에 격리했습니다.
- HTTP DTO/검증을 API로 이동하고 `LoanUseCase`, 예외 상태, 이벤트+선택적 재계산 응답을 추가했습니다.
- PENDING 계약, 1회 원금 전액 실행, ACTIVE/DEFAULTED/RECOVERY 상태 전이와 잠금·버전·멱등 제약을 구현했습니다.
- EIR을 BigDecimal 소수 단위로 통일하고 현재 잔액 기준 재계산과 단일 월별 EIR 스케줄 생성/Batch 소비 흐름을 연결했습니다.
- Batch는 필수 `accrualDate`, 안정 정렬/chunk만 담당하고 core pipeline이 성공 skip·실패 retry·실패 ID 집계를 수행합니다.
- V33, Java 17 API bootJar Docker, 8088 Compose, 실제 스키마/호출 순서/운영 TODO 기준 문서를 추가했습니다.
- Master Data 73 + Governance 25 + Closing 19 + Journal Ledger 19 + Loan Core 30 + Loan API 3 = 총 169개 테스트와 Loan API/Batch bootJar가 성공했습니다.
- 다음 순차 후보는 Closing Batch의 Master Data Repository 직접 의존 경계이며, 현재 Loan pass의 독립 리뷰와 사용자 승인 전에는 commit/push/merge하지 않습니다.

## 34차 후속 상세

### Closing 상태 전이·FX/ECL·재실행·런타임 정합성 점검

- 회계기간 누락과 미정의 태스크/게이트를 fail-closed 처리하고 캘린더·태스크·게이트·재오픈 상태 전이를 도메인에 고정했습니다.
- API 평가/충당 이력의 독립 트랜잭션과 PENDING_APPROVAL/FAILED 상태를 구현했습니다.
- FX는 실제 POSTED 전표 signed balance, bounded range/Cursor/chunk pipeline과 환율 계약 포트를 사용합니다.
- ECL은 DB 선집계, 단일 run/model/법인/기준일, GL 대변 부호와 그룹당 기존 잔액 1회 차감을 사용합니다.
- 결정적 explicit slip의 동일 내용 재사용, 상태별 자동전기, POSTED 기준통화 Annual Closing을 보강했습니다.
- API/Batch의 provider Application 광역 스캔을 제거하고 필요한 로컬 어댑터만 조립했으며 자체 application name과 컨텍스트 테스트를 추가했습니다.
- 52 suites/158 tests와 Closing API/Batch bootJar가 성공했습니다. PostgreSQL/Docker 및 대량 read model/집계 포트는 후속입니다.
- 현재 브랜치는 `agent/closing-consistency-pass`, 변경은 review-ready/uncommitted이며 명시 승인 전 commit/push/merge하지 않습니다.

## 36차 후속 상세

### origin/main 동기화와 Issue #29 구조 충돌 해결

- 이름 있는 stash로 기존 77개 변경 경로를 백업한 뒤 `agent/closing-consistency-pass`를 `origin/main@b7c8aaa`로 fast-forward하고 변경을 누락 없이 복원했습니다.
- `docs/WORKLOG.md` 충돌은 최신 upstream 기록과 로컬 Closing/Foundation 기록을 모두 보존해 해결하고 conflict log에 남겼습니다.
- commit/PR transfer stash의 82개 경로를 Issue #20 브랜치에 보존했습니다. 외부 `C:\tmp` worktree 소실 후 깨끗한 repository-root checkout에서 복구했습니다.
- 최신 main `f3d33ea`의 Issue #29 internal-audit split과 감사 패키지 이동을 보존하고 삭제된 standalone Application/test를 되살리지 않았습니다. 중간 AuditAspect 수정은 최종 PR에서 제외했습니다.
- 최신 기준 176 tests와 Closing API/Batch, Config Server의 3 bootJars가 성공했습니다. internal-audit 세 모듈의 test와 API/Batch production source가 `NO-SOURCE`인 상태는 별도 위험입니다.
- Issue #20 전용 `agent/20-closing-consistency` 브랜치를 repository root에서 사용합니다. 복구용 stash는 유지하며 사용자 승인 범위는 commit/push/Draft PR까지이고 merge/Issue close는 포함하지 않습니다.

## 37차 후속 상세

### Closing 고도화 main 반영 확인과 브랜치 정합화

- 최신 `origin/main@ce35ce5`와 82-path transfer snapshot을 비교해 Closing production/test/module docs 및 필수 bridge 변경이 모두 동일함을 확인했습니다.
- 고도화는 `31be6f1`에 이미 포함되어 main에 병합됐으므로 중복 코드를 적용하지 않고 Issue #20 feature 브랜치를 fast-forward했습니다.
- Shared Kernel/Contracts, Closing 연계 Master Data 어댑터, Journal Ledger, Closing core/api/batch, ECL의 98 tests와 3 bootJars가 성공했습니다.
- Issue #66이 root Compose의 Master Data/Config Server 서비스를 제거·주석 처리한 뒤 기존 정책 테스트 2건이 실패하는 main 회귀는 Closing 범위 밖 후속으로 분리했습니다.
- 세 recovery stash는 유지하고, 이번 feature branch에는 비교·검증·롤백 사실을 담은 하네스 기록만 커밋·PR merge합니다. Issue #20은 이미 CLOSED입니다.

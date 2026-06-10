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
| 6 | `payable`, `receivable` | Next | AP/AR 수금·지급/반제 흐름과 API 실행 가이드 정리 |
| 7 | `asset-lease`, `tax` | Pending | 자산/리스/세금계산서 실행·검증 흐름 정리 |
| 8 | `reconciliation`, `reporting` | Pending | 대사/보고 문서와 감독보고 흐름 정리 |
| 9 | foundation/infra | Pending | `contracts`, `shared-kernel`, `master-data`, `governance`, `auth`, gateway/discovery/config 등 |

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

# Loan 로컬 실행 가이드

이 문서는 IntelliJ와 Gradle로 `loan` 모듈을 로컬에서 확인하는 순서를 정리합니다.

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM: 17
- 루트 프로젝트 `account`를 Gradle 프로젝트로 import
- 전표 생성까지 확인하려면 `master-data` 계정/통화/거래처와 `journal-ledger` 구성이 필요합니다.

먼저 테스트와 컴파일로 모듈 상태를 확인합니다.

```powershell
.\gradlew :loan:core:test :loan:api:compileJava :loan:batch:compileJava --console=plain --max-workers=1 --no-daemon
```

## IntelliJ Run Configuration

공유 실행 설정은 `.run`에 있습니다.

| 이름 | 역할 |
| --- | --- |
| `Loan API bootRun` | REST API 컨텍스트 기동 |
| `Loan Batch Context` | Job을 실행하지 않고 Batch Bean 구성만 확인 |

## API 실행

```powershell
.\gradlew :loan:api:bootRun --args="--server.port=8087 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain
```

API가 정상 기동되면 아래 순서로 업무 흐름을 확인합니다.

1. `POST /api/loan/loans`로 대출 생성
2. `POST /api/loan/disbursals`로 대출 실행 전표 생성
3. `POST /api/loan/deferred-item-types`로 이연 항목 유형 생성
4. `POST /api/loan/deferred-items`로 이연 항목 생성
5. `POST /api/loan/amortization-schedules/generate`로 EIR 상각 스케줄 생성
6. `POST /api/loan/events` 또는 `/api/loan/dod-scenario`로 재계산 흐름 확인

## Batch 컨텍스트만 실행

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain
```

이 명령은 Job을 실행하지 않고 Batch 설정과 Bean 로딩만 확인합니다.

## 일일 이자 발생 Job 실행

선행 조건:

- ACTIVE 상태의 대출이 있어야 합니다.
- `loan_amortization_schedule_entries`에 `accrualDate`와 같은 `payment_date`가 있어야 합니다.
- 미수이자/이자수익 계정이 master-data에 있어야 합니다.
- 같은 대출/기준일의 `loan_accrual_log`가 이미 있으면 중복 전표를 만들지 않습니다.

```powershell
.\gradlew :loan:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=loanInterestAccrualJob accrualDate=2026-04-30 --spring.batch.jdbc.initialize-schema=always --account.loan.accounting.cash-account-code=101000 --account.loan.accounting.loan-receivable-account-code=131000 --account.loan.accounting.deferred-asset-account-code=118000 --account.loan.accounting.recognized-income-account-code=410000 --account.loan.accounting.accrued-interest-receivable-account-code=115010 --account.loan.accounting.interest-income-account-code=410100" --console=plain
```

## 주의할 점

- 로컬 H2 `create-drop`은 빠른 컨텍스트 확인용입니다. 운영 스키마 검증은 Flyway migration과 실제 DB로 확인해야 합니다.
- `LoanInterestAccrualBatchConfig`는 ACTIVE 대출을 paging으로 읽지만, 실제 전표 금액은 `LoanAmortizationScheduleEntry`의 `interestAmount`를 사용합니다.
- EIR 계산은 `DeferredItemType.eirCashFlowTreatment`에 따라 수수료/비용 부호를 결정합니다. 운영 전에는 상품별 이연 항목 유형이 `CUSTOMER_FEE_INFLOW`, `ORIGINATION_COST_OUTFLOW`, `EXCLUDED_FROM_EIR` 중 맞는 정책으로 등록됐는지 확인합니다.

# Closing 로컬 실행 가이드

이 문서는 IntelliJ와 Gradle로 `closing` 모듈을 로컬에서 확인하는 순서를 정리합니다.

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM: 17
- 루트 프로젝트 `account`를 Gradle 프로젝트로 import
- 필요한 경우 `master-data`, `journal-ledger`, `ecl`의 seed 또는 로컬 DB 준비

먼저 컴파일과 테스트로 모듈 상태를 확인합니다.

```powershell
.\gradlew :closing:core:test :closing:api:compileJava :closing:batch:test --console=plain --max-workers=1 --no-daemon
```

## IntelliJ Run Configuration

공유 실행 설정은 `.run`에 있습니다.

| 이름 | 역할 |
| --- | --- |
| `Closing API bootRun` | REST API 컨텍스트 기동 |
| `Closing Batch Context` | Job을 실행하지 않고 Batch Bean 구성만 확인 |

Batch Job은 선행 데이터가 필요하므로 기본 Run Configuration은 컨텍스트 확인용으로 둡니다. 실제 Job 실행은 아래 Gradle 명령을 사용해 기준일과 batch ID를 명확히 넘기는 편이 좋습니다.

## API 실행

```powershell
.\gradlew :closing:api:bootRun --args="--server.port=8086 --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --eureka.client.enabled=false --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

API가 정상 기동되면 아래 엔드포인트를 기준으로 흐름을 확인합니다.

| 기능 | 엔드포인트 |
| --- | --- |
| 결산 캘린더 생성 | `POST /api/closing/calendars` |
| 태스크 생성 | `POST /api/closing/tasks` |
| 게이트 생성 | `POST /api/closing/gates` |
| 기간 잠금 | `POST /api/closing/period-locks` |
| 재오픈 요청 | `POST /api/closing/reopen-approvals` |
| 결산 조정 등록 | `POST /api/closing/adjustments` |
| 마감 완료 판정 | `POST /api/closing/calendars/determine-status` |
| 연차 손익 대체 | `POST /api/closing/annual/perform-income-statement-closing` |

로컬 API에서 실제 업무 데이터를 확인하려면 `master-data`의 회계기간과 `journal-ledger`의 전표/잔액 데이터가 함께 준비되어야 합니다.

## Batch 컨텍스트만 실행

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.batch.job.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.jpa.hibernate.ddl-auto=create-drop" --console=plain
```

이 명령은 Job을 실행하지 않고 Batch 설정과 Bean 로딩만 확인합니다. 신규 개발자는 이 명령으로 의존성 누락이나 설정 오류를 먼저 잡는 것이 안전합니다.

## FX 평가 Job 실행

선행 조건:

- `journal-ledger`에 기준일 외화 GL 잔액이 있어야 합니다.
- `master-data`에 외화 -> 보고통화 환율이 있어야 합니다.
- 외화환산손익 계정 코드 설정이 운영 계정 체계와 맞아야 합니다.

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=fxValuationJob valuationDate=2026-04-30 valuationBatchId=20260430 --spring.batch.jdbc.initialize-schema=always" --console=plain
```

## ECL 충당 Job 실행

선행 조건:

- `account-mart`가 `allowance_exposure_snapshots`를 생성해야 합니다.
- `ecl`의 `allowanceEclJob`이 완료되어 `allowance_summary`가 생성되어야 합니다.
- `journal-ledger`에 기존 대손충당금 GL 잔액이 있어야 합니다.
- `allowance_summary`의 계정 매핑 컬럼이 비어 있지 않아야 합니다.

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=eclProvisionJob closingDate=2026-04-30 provisionBatchId=20260430 --spring.batch.jdbc.initialize-schema=always" --console=plain
```

## API 평가/충당 배치 설정 예시

`ClosingService.runValuationBatch`와 `runProvisionBatch`는 `account.closing.accounting.*` 설정 룰을 요구합니다. 설정이 없으면 전표 생성 전에 실패합니다.

```yaml
account:
  closing:
    accounting:
      auto-post-adjustments: false
      fx-valuation-reporting-currency-code: KRW
      fx-translation-gain-account-code: "72000"
      fx-translation-loss-account-code: "92000"
      valuation-rules:
        FX_RATE:
          debit-account-code: "11000"
          credit-account-code: "72000"
          amount: 1000.00
      provision-rules:
        ECL:
          debit-account-code: "93000"
          credit-account-code: "12900"
          amount: 1000.00
```

운영에서는 금액을 고정값으로 둘지, 별도 산출 결과를 연결할지 정책이 필요합니다. Batch 경로의 ECL 충당은 `allowance_summary`를 사용하므로 실제 결산 충당 전표는 Batch 경로를 우선 확인합니다.

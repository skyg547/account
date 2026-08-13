# Closing 로컬 실행 가이드

이 문서는 IntelliJ와 Gradle로 `closing` 모듈을 로컬에서 확인하는 순서를 정리합니다.

## 전제 조건

- JDK 17
- IntelliJ Gradle JVM: 17
- 루트 프로젝트 `account`를 Gradle 프로젝트로 import
- 필요한 경우 `master-data`, `journal-ledger`, `ecl`의 seed 또는 로컬 DB 준비

먼저 컴파일과 테스트로 모듈 상태를 확인합니다.

```powershell
.\gradlew :closing:core:test :closing:api:test :closing:batch:test --console=plain --max-workers=1 --no-daemon
```

## IntelliJ Run Configuration

공유 실행 설정은 `.run`에 있습니다.

| 이름 | 역할 |
| --- | --- |
| `Closing API bootRun` | REST API 컨텍스트 기동 |
| `Closing Batch Context` | Job을 실행하지 않고 Batch Bean 구성만 확인 |

Batch Job은 선행 데이터가 필요하므로 기본 Run Configuration은 컨텍스트 확인용으로 둡니다. 실제 Job 실행은 아래 Gradle 명령을 사용해 기준일과 batch ID를 명확히 넘기는 편이 좋습니다. FX/ECL의 업무 판단은 core 서비스가 맡고, Batch는 Reader/Tasklet과 외부 어댑터를 조립합니다.

## API 실행

실행 모듈은 의존 JAR의 설정 이름을 상속하지 않도록 `spring.application.name=closing-service`, 기본 포트 `8086`을 자체 설정합니다.

```powershell
.\gradlew :closing:api:bootRun --console=plain
```

직접 실행 JAR도 profile을 생략하면 `local`을 사용합니다.

```powershell
.\gradlew :closing:api:bootJar --console=plain
java -jar closing\api\build\libs\closing-api-0.0.1-SNAPSHOT.jar
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
| 첫 영업일 초기화 | `POST /api/closing/eod/{businessDate}/bootstrap` |
| EOD 준비/시작/완료 | `POST /api/closing/eod/{businessDate}/eod/{prepare\|start\|complete}` |
| EOD 준비 취소 | `POST /api/closing/eod/{businessDate}/eod/cancel-preparation` |
| 다음 영업일 BOD 시작 | `POST /api/closing/eod/{closedDate}/bod/{nextBusinessDate}/start` |
| 다음 영업일 BOD 완료 | `POST /api/closing/eod/{businessDate}/bod/complete` |
| 일마감 상태 조회 | `GET /api/closing/eod/{businessDate}` |

로컬 API에서 실제 업무 데이터를 확인하려면 `master-data`의 회계기간과 `journal-ledger`의 전표/잔액 데이터가 함께 준비되어야 합니다.

EOD 변경 명령에는 Gateway가 JWT에서 만든 `X-Auth-User`와 `X-Auth-Roles`가 필요합니다. 허용 역할은 `ROLE_ADMIN`, `ROLE_ACCOUNTING_ADMIN`, `ROLE_CLOSING_MANAGER`입니다. 로컬에서 API 포트를 직접 호출할 때 이 헤더를 임의로 넣을 수 있으므로 해당 방식은 기능 확인용일 뿐 보안 검증이 아닙니다.

Closing Flyway는 의존 모듈의 동일 버전 migration과 충돌하지 않도록 `classpath:db/closing-migration`만 실행하고 `flyway_schema_history_closing`에 독립적으로 이력을 기록합니다. local clean H2는 V49 clean baseline, V50 EOD/BOD 전환, V51 운영 인덱스를 순서대로 적용합니다. 개발·운영 PostgreSQL은 애플리케이션 시작 Flyway를 끄고 release-time `migration-runner`가 먼저 migrate/validate하며, API와 Batch는 `ddl-auto=validate`로만 부팅합니다.

Closing API 조합 루트는 실제로 사용하는 Master Data의 `FiscalPeriodControlPort`와 `MasterDataQueryPort` 어댑터, 그리고 이들이 요구하는 최소 persistence adapter/mapper를 함께 명시 import합니다. 이는 Closing의 회계기간 제어와 평가 조회 포트를 완성하는 조합 책임이며, Master Data 전체 infrastructure 패키지를 scan하거나 local 전용 fallback으로 production 어댑터를 가리지 않습니다.

## Batch 컨텍스트만 실행

Batch 실행 모듈의 애플리케이션 이름은 `closing-batch`입니다.

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.main.web-application-type=none" --console=plain
```

이 명령은 Job을 실행하지 않고 Batch 설정과 Bean 로딩만 확인합니다. 신규 개발자는 이 명령으로 의존성 누락이나 설정 오류를 먼저 잡는 것이 안전합니다.

## 개발·운영 PostgreSQL profile

- `dev`: `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`
- `prod`: `PROD_DB_URL`, `PROD_DB_USER`, `PROD_DB_PASSWORD`

두 profile 모두 runtime Flyway와 SQL init, Batch metadata 자동 생성을 끄고 JPA `validate`를
사용합니다. 비밀번호와 완성된 운영 URL은 Compose나 저장소에 기록하지 않고 secret으로
주입합니다. 실제 DB 변경은 `migration-runner`와 승인된 변경 티켓으로만 수행합니다.

## FX 평가 Job 실행

선행 조건:

- `journal-ledger`에 기준일까지 전기된 외화 `journal_entries`/`journal_details`와 거래통화·기준통화 금액이 있어야 합니다.
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

API의 `runValuationBatch`/`runProvisionBatch`는 설정 기반 자동분개 흐름을 확인하는 경량 경로입니다. 실제 FX/ECL 결산 전표는 Batch가 외부 잔액/환율/allowance_summary를 읽고 core `FxValuationService`/`EclProvisionService`가 금액과 차대변을 판단하는 경로를 우선 확인합니다.

두 Job의 날짜와 batch ID는 생략할 수 없습니다. 누락·잘못된 ISO 날짜·0 이하 ID는 Job 시작 전에 실패하며, 시스템 날짜나 날짜 기반 임시 ID로 대체하지 않습니다. ECL summary가 비어 있는 경우도 성공으로 처리하지 않습니다.

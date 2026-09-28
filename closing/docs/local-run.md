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
| 최종 마감 증빙 추가 | `POST /api/closing/calendars/{calendarId}/final-close-evidence` |
| 연차 손익 대체 | `POST /api/closing/annual/perform-income-statement-closing` |
| 첫 영업일 초기화 | `POST /api/closing/eod/{businessDate}/bootstrap` |
| EOD 준비/시작/완료 | `POST /api/closing/eod/{businessDate}/eod/{prepare\|start\|complete}` |
| EOD 준비 취소 | `POST /api/closing/eod/{businessDate}/eod/cancel-preparation` |
| 다음 영업일 BOD 시작 | `POST /api/closing/eod/{closedDate}/bod/{nextBusinessDate}/start` |
| 다음 영업일 BOD 완료 | `POST /api/closing/eod/{businessDate}/bod/complete` |
| 일마감 상태 조회 | `GET /api/closing/eod/{businessDate}` |

로컬 API에서 실제 업무 데이터를 확인하려면 `master-data`의 회계기간과 `journal-ledger`의 전표/잔액 데이터가 함께 준비되어야 합니다.

EOD 변경 명령에는 Gateway가 JWT에서 만든 `X-Auth-User`와 `X-Auth-Roles`가 필요합니다. 허용 역할은 `ROLE_ADMIN`, `ROLE_ACCOUNTING_ADMIN`, `ROLE_CLOSING_MANAGER`입니다. 로컬에서 API 포트를 직접 호출할 때 이 헤더를 임의로 넣을 수 있으므로 해당 방식은 기능 확인용일 뿐 보안 검증이 아닙니다.

Closing Flyway는 의존 모듈의 동일 버전 migration과 충돌하지 않도록 `classpath:db/closing-migration`만 실행하고 `flyway_schema_history_closing`에 독립적으로 이력을 기록합니다. local clean H2는 V49 clean baseline, V50 EOD/BOD 전환, V51 운영 인덱스, V52 월말 전이 기록, V53 최종 마감 증빙을 순서대로 적용합니다. 개발·운영 PostgreSQL은 애플리케이션 시작 Flyway를 끄고 release-time `migration-runner`가 먼저 migrate/validate하며, API와 Batch는 `ddl-auto=validate`로만 부팅합니다.

Closing API 조합 루트는 실제로 사용하는 Master Data의 `FiscalPeriodControlPort`와 `MasterDataQueryPort` 어댑터, 그리고 이들이 요구하는 최소 persistence adapter/mapper를 함께 명시 import합니다. 이는 Closing의 회계기간 제어와 평가 조회 포트를 완성하는 조합 책임이며, Master Data 전체 infrastructure 패키지를 scan하거나 local 전용 fallback으로 production 어댑터를 가리지 않습니다.

## 일반 전표 허용 여부 조회

API가 실행되고 대상 Master 월 회계기간과 Closing 캘린더가 준비된 후 다음 경로를 호출합니다.

```http
GET /api/closing/admission?accountingDate=2026-01-15
```

두 상태 모두 `OPEN`이고 잠금이 없으면 `{"accountingDate":"2026-01-15","ordinaryPostingAllowed":true}`,
잠금·결산 진행·누락된 캘린더 등 차단 조건이면 같은 형태의 `false`를 반환합니다.
응답은 `Cache-Control: no-store`입니다. 회계일자 누락·잘못된 형식은 400,
Master 기간 누락·잘못된 기간·조회 장애는 상세 원인을 노출하지 않는 503입니다.
HTTP 오류, 응답 부재, 역직렬화 실패를 전표 허용으로 해석하면 안 됩니다.

이는 조회 시점의 참고 결과입니다. 현재 독립 Journal에는 이 경로를 호출하는 소비자가 없으며,
결과 `true`만으로 이후 전기 커밋이 보장되지 않습니다. 통합 한계는
[업무 흐름](process-flow.md#일반-전표-허용-판정-gh-772)을 참조하세요.

```bash
./gradlew :closing:test
```

이 명령은 부모 프로젝트의 빈 테스트 태스크뿐 아니라 Core/API/Batch 테스트를 모두 실행합니다.
Journal 의존성은 Core 테스트 전용이므로 API/Batch 런타임에 Journal이 추가되지 않습니다.

## 최종 마감 증빙 제출과 검증 (GH-778)

증빙 API는 공개 사용자용이 아니라 내부 공급자용입니다. Gateway가 클라이언트가 보낸
`X-Auth-User`/`X-Auth-Roles`를 제거하고 검증한 서비스 identity로 다시 만들어야 합니다.
`ROLE_CLOSING_EVIDENCE_PROVIDER` 역할과 trusted submitter allowlist가 모두 필요합니다. actor 누락은
401, 역할 부족 또는 allowlist 불일치는 403입니다. 기본 allowlist는 비어 있어 모든 제출을 거부합니다.
로컬에서 헤더를 직접 넣는 예시는 payload/상태 흐름 확인용일 뿐 Gateway 보안 검증이 아닙니다.

```yaml
account:
  closing:
    final-close-evidence:
      max-age: PT24H
      trusted-submitters:
        - payable-close-provider
```

`trusted-submitters`는 최대 64개이며, 각 actor는 trim 후 100자 이하이고 대소문자까지 정확히
`X-Auth-User`와 일치해야 합니다. 운영 환경의 승인된 서비스 identity만 설정하고 빈 값은 넣지 않습니다.
`max-age`는 양수 Spring `Duration`이며 기본값은 `PT24H`입니다. `observedAt`은 현재 캘린더의
마감 시작 시각보다 뒤이고 미래가 아니며 이 나이 안에 있어야 합니다. 아래 본문에는 의도적으로
`submittedBy`가 없습니다. path/body `calendarId`, `fiscalPeriodId`, 회계연도·기간,
`ledgerCutoff`은 최종 마감 때 Master identity/종료일과 정확히 일치해야 합니다.
공급자는 `observedAt`의 나노초를 반올림하지 않고 마이크로초로 절삭한 ISO Instant를 body에 넣고,
바로 그 문자열로 digest를 계산해야 합니다. 서버 응답도 PostgreSQL 호환 마이크로초 값입니다.

```http
POST /api/closing/calendars/10/final-close-evidence
X-Auth-User: payable-close-provider
X-Auth-Roles: ROLE_CLOSING_EVIDENCE_PROVIDER
Content-Type: application/json
```

```json
{
  "evidenceSetId": "2026-04-close-0001",
  "calendarId": 10,
  "fiscalPeriodId": 104,
  "fiscalYear": "2026",
  "fiscalPeriod": "04",
  "ledgerCutoff": "2026-04-30",
  "observedAt": "2026-04-30T15:10:00.123456Z",
  "contentDigest": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "controls": [
    {
      "type": "AP_SUBLEDGER",
      "sourceSystem": "payable",
      "sourceRunId": "ap-close-2026-04-30-7",
      "outcome": "PASS",
      "blockingItemCount": 0,
      "totals": [
        {"accountCode": "21000", "currencyCode": "KRW", "sourceTotal": 0, "postedTotal": 0}
      ]
    }
  ]
}
```

digest 값은 형식만 맞춘 placeholder이고, 예시는 명시적 `0/0`과 필드 모양만 보여 주므로 그대로
제출하면 digest 불일치로 거부되며 최종 마감 조건도 충족하지 않습니다.
일반 기간은 AP/AR/리스/대출/Journal 조정/ECL 여섯 통제를 각각 한 번, `YEAR`는 여기에
`ANNUAL_TRANSFER`를 더해야 합니다. 각 통제의 expected `sourceSystem`, digest 정렬·길이-prefix·
금액 정규화 규칙은 [업무 흐름](process-flow.md#최종-마감-증빙-gh-778)을 따릅니다.

성공은 201과 저장된 `submittedBy`/digest/control count를 반환합니다. 같은 set ID와 digest 재시도는
기존 set을 반환하고, 같은 ID의 다른 내용은 409입니다. 형식 위반은 400, 캘린더 없음은 404,
digest/상태/identity 위반 또는 최종 마감 증빙 실패는 409입니다. 실패한 증빙을 무시하는 override는
없습니다. 최신 set이 FAIL이면 새롭고 올바른 set을 append해야 합니다.

검증 명령(실행 결과는 별도로 기록):

```bash
./gradlew :closing:core:test --tests '*FinalCloseEvidence*' --console=plain --max-workers=1 --no-daemon
./gradlew :closing:api:test --tests '*FinalCloseEvidence*' --tests '*ClosingTransitionRecovery*' --console=plain --max-workers=1 --no-daemon
./gradlew :closing:test --console=plain --max-workers=1 --no-daemon
./gradlew :closing:api:bootJar :closing:batch:bootJar --offline --console=plain --max-workers=1 --no-daemon
```

생산 공급자들이 실제 확정 run과 완전한 계정·통화 차원을 보내는지, Gateway가 헤더를 제거/재구성하는지,
실제 PostgreSQL의 migration·잠금과 원천 관측 후 Master 변경 사이 분산 TOCTOU는 사람이 감독하는
end-to-end/운영 검증이 필요합니다. 헤더와 allowlist는 암호학적 identity 증명이 아니므로 production은
사설 네트워크와 Gateway의 외부 헤더 제거/재구성을 검증해야 하며, 향후 mTLS나 서명된 서비스 토큰을
사용할 수 있습니다. 이 검증이 없다는 이유로 fail-open하지 않습니다.

## 월말 전이 조회와 복구 (GH-774)

| 기능 | 엔드포인트 |
| --- | --- |
| 진행 중인 전이 조회 | `GET /api/closing/calendars/{id}/transition` |
| 작업 ID에 묶인 복구 | `POST /api/closing/calendars/{id}/transition/recover` |

복구는 Gateway가 검증해 넣은 `X-Auth-User`, `X-Auth-Roles`를 사용합니다.
허용 역할은 `ROLE_ADMIN`, `ROLE_ACCOUNTING_ADMIN`, `ROLE_CLOSING_MANAGER`입니다.
조회와 복구 모두 이 권한을 요구하며, 사용자 헤더 누락은 401, 역할 부족은 403입니다.
요청 본문에 처리자 이름을 넣어 권한을 대신하지 않습니다. Closing API를 외부에 직접 노출하지 않는
기존 EOD와 같은 신뢰 경계를 전제로 합니다.

먼저 조회한 작업 ID와 단계, 목표 상태를 확인합니다. `PREPARED`는 최초 전송을 재개할 수 있습니다.
`DISPATCHED`이면 원 요청이 더 이상 Master에서 실행될 수 없음을 운영 절차로 확인한 후에만
`remoteRequestTerminated=true`로 복구를 요청합니다. 이 값은 원격 종료를 자동 증명하지 않습니다.

```json
{
  "operationId": "조회에서 확인한 작업 UUID",
  "remoteRequestTerminated": true
}
```

복구는 Master 목표 상태를 확인한 뒤 로컬 상태를 완료합니다. 이미 전송한 PUT을 다시 보내지 않으며,
Master가 원 상태에 남았으면 계속 차단합니다. 해당 경우는 별도 권한의 Master 운영자가 원 요청 종료와
기간 상태를 대사·조정해야 합니다. 오래된 작업 ID로 재호출하거나 경쟁 결정이 들어오면 충돌로 처리합니다.
원 승인자의 결정은 복구자 이름으로 덮어쓰지 않습니다.

최초 마감·승인 호출 중 원격 반영이나 로컬 완료가 불명확하면 HTTP 503과
`PERIOD_TRANSITION_RECOVERY_REQUIRED`를 반환합니다. 이 응답은 승인 결정이 롤백됐다는 뜻이 아닙니다.
조회 결과의 `operationId`, `target`, `stage`, `decisionActor`를 확인하고 복구 흐름을 사용합니다.
완료되면 `calendarStatus`가 목표 상태이며 진행 중인 전이 필드는 null입니다.
경쟁 결정·오래된 작업 ID·확인 누락·Master 상태 불일치는 HTTP 409입니다.

검증 명령:

```bash
./gradlew :closing:api:test --tests '*ClosingAggregateConcurrencyIntegrationTest' --tests '*ClosingTransitionRecoveryIntegrationTest'
./gradlew :closing:test
./gradlew :closing:api:bootJar :closing:batch:bootJar
```

동시성 테스트는 별도 트랜잭션과 latch로 순서를 제어하고, 원격 역할의 상태는 Closing 트랜잭션과 독립적으로
저장합니다. 복구 테스트는 응답 유실과 로컬 완료 실패를 주입합니다. H2 검증은 실제 PostgreSQL 잠금·운영 부하나
배포된 Master의 네트워크 장애 증거를 대신하지 않습니다. 실행 결과는 [모듈 worklog](ai-harness/worklog.md)에 기록합니다.

## Batch 컨텍스트만 실행

Batch 실행 모듈의 애플리케이션 이름은 `closing-batch`입니다.

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.main.web-application-type=none" --console=plain
```

이 명령은 Job을 실행하지 않고 Batch 설정과 Bean 로딩만 확인합니다. 신규 개발자는 이 명령으로 의존성 누락이나 설정 오류를 먼저 잡는 것이 안전합니다.

`local` profile에서는 Closing Core의 in-memory `JournalPostingPort`/`JournalQueryPort`가
Batch 조합 루트에만 명시적으로 연결됩니다. 이 포트는 로컬 H2 컨텍스트와 파이프라인 조립을
검증하기 위한 것으로 실제 전표를 저장하지 않으며, `dev`/`prod` profile에서는 등록되지 않아
실제 Journal 연동을 가리지 않습니다. Batch 조합 루트도 FX 평가에 필요한 환율 조회와 Closing이
사용하는 Master Data 포트 및 최소 persistence adapter/mapper만 명시 import합니다.

따라서 실제 전표 생성·승인·전기 결과를 검증할 때는 local 스텁을 사용하지 말고 승인된
개발 환경의 실제 Journal 어댑터 구성을 사용해야 합니다.

`dev`의 `HttpClosingJournalAdapter`는 생성·승인·전기·조회에서 HTTP 3xx를 정상 처리하지 않고
`IllegalStateException`으로 실패시킵니다. `Location`에 대한 추가 요청도 하지 않습니다.
초안 생성 실패 시 호출 흐름은 승인·전기로 진행하지 않고, 승인 실패 시 전기를 호출하지 않습니다.
조회에서 404만 전표 없음으로 처리하며 302를 전표 없음이나 성공으로 바꾸지 않습니다.
기존 actor·lineage 전달은 유지됩니다. 원격 쓰기는 각각 별도 트랜잭션이므로 실패 후 자동 재시도하지
않고, 이미 생성·승인된 전표가 있는지 대사해야 합니다.

```bash
bash gradlew :closing:core:test --tests '*HttpClosingJournalAdapterTest' --console=plain --max-workers=1 --no-daemon
```

이 테스트는 실제 loopback HTTP 서버로 리다이렉트와 후속 호출 차단을 검증하며 외부 Journal 서버·DB는 사용하지 않습니다.

## 개발·운영 PostgreSQL profile

- `dev`: `DEV_DB_HOST`, `DEV_DB_PORT`, `DEV_DB_NAME`, `DEV_DB_USER`, `DEV_DB_PASSWORD`
- `prod`: `PROD_DB_URL`, `PROD_DB_USER`, `PROD_DB_PASSWORD`

두 profile 모두 runtime Flyway와 SQL init, Batch metadata 자동 생성을 끄고 JPA `validate`를
사용합니다. 비밀번호와 완성된 운영 URL은 Compose나 저장소에 기록하지 않고 secret으로
주입합니다. 실제 DB 변경은 `migration-runner`와 승인된 변경 티켓으로만 수행합니다.

## FX 평가 Job 실행

연속 평가 회귀는 외부 서버 없이 합성 H2 전표에 실제 Reader SQL과 core 평가 서비스를 연결합니다.
테스트 포트가 생성 명령을 저장·전기 상태로 바꾸므로 실제 Journal의 승인·전기 API 검증을 대체하지는 않습니다.

```bash
./gradlew :closing:core:test --tests '*FxValuation*'
./gradlew :closing:batch:test --tests '*FxValuation*' --tests '*JournalFxValuationBalanceSourceTest'
./gradlew :closing:test
```

기대 결과는 전체 테스트 통과입니다. USD100 / KRW1,000에 평가 조정 KRW200이 이미 전기되었으면
다음 환율12에서 추가 전표가 생기지 않아야 합니다. 상승·하락, 부채/비정상 잔액, 같은 계정의
복수 외화, 전기 상태·기준일, 평가와 역분개의 연결도 확인합니다. 실제 PostgreSQL 실행계획,
운영 부하, 배포 서비스 간 장애 검증은 별도 환경에서 수행해야 합니다.
적격성 회귀는 현금/수익·비용/부채·비화폐성 자산/부채의 양쪽 전표, 기준일 정책과
설정 바인딩, 분류 누락·모순, 제외 행을 포함한 Cursor 재시작도 확인합니다.

선행 조건:

- `journal-ledger`에 기준일까지 전기된 외화 `journal_entries`/`journal_details`와 거래통화·기준통화 금액이 있어야 합니다.
- `master-data`에 외화 -> 보고통화 환율이 있어야 합니다.
- 기준일 유효 Master 계정 정보와 아래의 계정별 평가 정책이 있어야 합니다. 외화 수익·비용처럼 제외할 계정도 `HISTORICAL_COST`로 명시합니다.
- 외화환산손익 계정 코드 설정이 운영 계정 체계와 맞아야 합니다.

```powershell
.\gradlew :closing:batch:bootRun --args="--spring.batch.job.enabled=true --spring.batch.job.name=fxValuationJob valuationDate=2026-04-30 valuationBatchId=20260430 --spring.batch.jdbc.initialize-schema=always" --console=plain
```

### FX 평가 적격성 설정 (GH-780)

아래는 합성 현금/수익 두 계정의 설정 형태입니다. 실제 계정 코드와 적용 기간은 승인된
계정 정책에 맞춰 제공해야 합니다. 숫자 계정 코드는 문자열로 유지하며 기간 양 끝을 포함합니다.
기본 정책 목록은 비어 있어 계정을 자동으로 화폐성으로 취급하지 않습니다.

```yaml
account:
  closing:
    accounting:
      fx-valuation-policies:
        - account-code: "11000"
          effective-from: 2026-01-01
          effective-to: 2026-12-31
          treatment: MONETARY
        - account-code: "41000"
          effective-from: 2026-01-01
          effective-to: 2026-12-31
          treatment: HISTORICAL_COST
```

현금 `11000`은 Master에서 `ASSETS`(호환값 `ASSET`), `fixedAsset=false`, 수익 `41000`은 `REVENUE`로
조회되어야 합니다. 현금/수익 USD100, 장부금액110, 평가환율1.2이면 현금 조정10과
환산이익10의 전표 하나가 기대 결과입니다. 수익 조정 전표는 생기지 않습니다.
기간이 만료되거나 겹침·계정 분류 모순이 있으면 실행이 실패하므로 입력 정책을 확인한 뒤
원장과 같은 기준일·실행 식별자로 재검증합니다. 과거 유효 정책을 덮어쓰지 않습니다.

비화폐성 예외 평가와 기존 잘못된 전표의 자동 정정은 제공하지 않습니다. 위 설정은 실행
형태를 설명하는 예시이며 운영 Job을 실행했다는 증거는 아닙니다.

## ECL 충당 Job 실행

선행 조건:

- `account-mart`가 `allowance_exposure_snapshots`를 생성해야 합니다.
- `ecl`의 `allowanceEclJob`이 완료되어 `allowance_summary`가 생성되어야 합니다.
- `journal-ledger`의 기준일까지 전기된 충당금 전표에서 거래통화·기능통화 금액을 함께 조회할 수 있어야 합니다. 신규 충당금의 두 잔액은 0입니다.
- 외화는 기준일 이하 최신 양수 환율이 필요합니다. `fx-valuation-reporting-currency-code`는 원장의 기능통화와 일치해야 합니다.
- 환율이 바뀌었다면 해당 계정의 유효 FX 정책을 확인하고 FX 평가를 승인·전기한 뒤 ECL을 실행합니다. DRAFT 평가만으로는 선행 조건을 충족하지 않습니다.
- `allowance_summary`의 계정 매핑 컬럼이 비어 있지 않아야 합니다.

외부 서버 없이 합성 H2 원장과 실제 Closing 조회·환율·전표 어댑터 계약을 검증합니다.
환율 변경 전 차단/평가 후 성공, 외화 증액·환입·동일 목표, 환율 누락과 양 통화 대사는
[ECL 업무 흐름](process-flow.md#ecl-거래통화와-기능통화-대사-gh-781)에 설명되어 있습니다.

```bash
./gradlew :closing:core:test --tests '*EclProvision*'
./gradlew :closing:batch:test --tests '*EclProvision*' --tests '*GlAllowanceBalanceLookupAdapterTest'
./gradlew :closing:test
```

기대 결과는 모든 테스트 통과입니다. 합성 전기 포트는 생성 명령을 H2 원장에 반영하며,
배포된 Journal의 승인·전기 API, 실제 PostgreSQL 실행계획·동시성·운영 부하를 대체하지 않습니다.

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

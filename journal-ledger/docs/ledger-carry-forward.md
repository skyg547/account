# 📖 원장 잔액 이월 (Ledger Carry-forward) 가이드

이 문서는 재무 시스템에서 장부의 연속성을 보장하는 **'자동 잔액 이월(Automatic Carry-forward)'**과 일별 잔액을 기간 잔액으로 조회하는 규칙을 설명합니다.

---

## 1. 초보자를 위한 개념 설명
"어제 내 통장에 10,000원이 있었는데, 오늘 아침에 0원으로 시작하면 안 되겠죠?"

회계 장부도 마찬가지입니다. 
- **기초 잔액(Beginning Balance):** 오늘 일을 시작하기 전, 어제로부터 넘어온 금액
- **기말 잔액(Ending Balance):** 오늘 발생한 모든 거래를 더하고 뺀 최종 금액

시스템은 매일 전표가 처음 기록될 때, **직전 최종 잔액을 자동으로 찾아 오늘 장부의 기초 잔액으로 설정**합니다. 이를 통해 수동 작업 없이도 장부가 끊기지 않고 이어지게 됩니다.

---

## 2. 핵심 비즈니스 로직 Flow

1. **전표 전기(Posting) 발생:** 사용자가 전표를 확정하거나 시스템이 자동 생성된 전표를 승인함.
2. **잠금과 잔액 확인:** 이번 호출에 포함된 모든 계정·통화의 공통 잠금 행을 먼저 확보한 뒤, 해당 날짜(`balanceDate`)의 최신 잔액을 확인. 잔액 행이 아직 없어도 같은 키의 첫 생성은 직렬화됩니다.
3. **기초 잔액 설정 (Carry-forward):**
   - 레코드가 **없을 경우:** DB에서 해당 날짜 이전(`balanceDate < today`) 중 **가장 최근의 `endingBalance`**를 조회.
   - 조회된 금액을 새로운 레코드의 `beginningBalance`로 설정.
   - 만약 한 번도 거래가 없었다면 0원부터 시작.
4. **증분 업데이트:** 오늘 발생한 전표의 차변(Debit) 또는 대변(Credit) 금액을 합산.
5. **최종 잔액 재계산:** `Ending Balance = Beginning + Debit - Credit` (자산 기준).

---

## 3. 관련 핵심 클래스 및 메서드

- **`LedgerService.updateGlBalance()`**: 총계정원장 잔액 이월 로직의 본거지.
- **`GlBalanceRepository.findFirstBy...BeforeOrderByBalanceDateDesc()`**: 직전 잔액을 찾는 핵심 쿼리.
- **`GlBalance.recalculate()`**: 기초 잔액과 증분을 합산하여 기말 잔액을 산출하는 도메인 로직.

---

## 4. 주의사항 (Developer Tips)
- **SCD2 연동:** 계정과목이나 부서가 변경되어도 원장의 이력은 보존되어야 하므로, 잔액 데이터 생성 시점의 마스터 데이터 정보를 정확히 참조합니다.
- **역날짜 전표(Back-dated):** 과거 날짜의 전표를 입력할 경우, 그 이후 모든 날짜의 잔액을 다시 계산(Re-aggregation)해야 합니다. 현재는 `reaggregateLedgerBalancesForPeriod`와 `journal-ledger:batch`의 `dailyBalanceReaggregationJob`으로 이를 지원합니다. JobParameter는 `startDate/endDate` 또는 단일 `baseDate`를 사용합니다.
- **순서와 트랜잭션:** bulk 입력은 날짜 오름차순으로 처리해 다음 날 기초가 앞선 날 기말을 읽도록 합니다. 단일 트랜잭션 재집계는 삭제 전에 모든 계정 잠금을 확보합니다. Batch Job은 cleanup/chunk마다 커밋하므로 전체 Job 동안 전기를 중지하고 단일 Job만 실행해야 합니다. [동시성·재시도 제약](posting-concurrency.md)을 확인합니다.

## 5. 기간 잔액 조회: 기초는 한 번, 거래 흐름은 모두

초보자 설명: 이틀 동안의 통장 잔액을 구할 때 두 날의 아침 잔액을 더하면 같은 돈을 두 번 셉니다.
첫날 아침 잔액에 이틀간의 입출금만 반영해야 합니다. GL/SL도 같은 방식으로 계산합니다.

입력은 `LedgerBalancePersistencePort`가 조회 기간과 필터에 맞춰 반환한 일별 잔액 행입니다.
`LedgerService.getGlBalances()` / `getSlBalances()`는 다음 순서로 기간 응답을 만듭니다.

1. GL은 계정·통화, SL은 계정·거래처·부서·통화별로 기존 키를 유지해 행을 묶습니다.
2. 각 그룹에서 `balanceDate`가 가장 이른 행의 `beginningBalance`를 한 번만 선택합니다.
   이 행의 기초에는 조회 첫 거래일 이전의 이월액도 이미 들어 있으므로 과거 잔액을 다시 더하지 않습니다.
3. 그룹 안의 모든 `debitAmount`와 `creditAmount`를 각각 합산합니다.
4. 기초 선택과 흐름 합산이 모두 끝나면 `recalculate()`로 `기초 + 차변 합계 - 대변 합계`를 계산합니다.
   중간 합계는 아직 일부 날짜의 차변 또는 대변이 빠져 있으므로 기말 검증은 완성된 그룹에 한 번만 수행합니다.

예를 들어 다음 두 행을 **역순으로 받아도** 같은 결과가 나옵니다.

| 날짜 | 일별 기초 | 차변 | 대변 | 일별 기말 |
| --- | ---: | ---: | ---: | ---: |
| 2026-09-01 | 1,000.00 | 100.00 | 20.00 | 1,080.00 |
| 2026-09-02 | 1,080.00 | 50.00 | 10.00 | 1,120.00 |

기간 응답은 기초 `1,000.00`, 차변 `150.00`, 대변 `30.00`, 기말 `1,120.00`입니다.
둘째 날의 기초 `1,080.00`은 기간 기초에 추가하지 않습니다.

### 응답과 경계 조건

- 저장소 쿼리는 날짜 정렬을 보장하지 않습니다. 서비스는 원본 목록을 정렬하지 않고 그룹별 최소 날짜를 추적합니다.
  처리 시간은 조회 행 수에 비례하며, 추가 상태는 그룹 수에 비례합니다. 결과 그룹 순서는 기존처럼 입력에서 처음 만난 순서입니다.
- 응답 객체는 원본 JPA 행과 별개입니다. 원본 목록 순서, 금액, 날짜, 식별자와 감사 시각을 변경하거나 저장하지 않습니다.
  같은 입력으로 조회를 반복해도 원본 잔액에 금액이 누적되지 않습니다.
- 응답 `balanceDate`는 기존처럼 **요청한 `startDate`**, `period`는 그 시작일의 월입니다.
  기초를 가져온 행의 날짜로 응답 날짜를 바꾸지 않습니다. 예를 들어 8월 30일부터 조회했는데 첫 행이 9월 1일이면,
  9월 1일 행의 기초를 사용하되 응답 날짜는 8월 30일, `period`는 `2026-08`입니다.
- 거래가 없는 중간 날짜나 월경계를 만나도 가상 일별 행이나 추가 이월액을 만들지 않습니다.
  단일 날짜는 기존 금액과 같고, 조회 행이 없으면 빈 목록입니다.
- 금액은 `BigDecimal`과 기존 `DECIMAL(19,2)` 정책을 따릅니다. 소수 둘째 자리와 음수 기초·기말을 유지하며,
  임의 반올림을 추가하거나 금액 범위·차대변 비음수 검증을 완화하지 않습니다.
- 기간 안에 행이 전혀 없는 계정의 과거 잔액을 찾아 주는 **as-of 조회는 지원 범위가 아닙니다**.
  기존 일별 projection 오류 복구, 전기·Batch 처리, `calculateGlBalanceAggregate()`의 COUNT/SUM 계약도 이 조회 수정의 대상이 아닙니다.

### 호출 예와 로컬 검증

아래 GET은 API가 이미 실행 중일 때의 요청 경로 예입니다. 테스트를 위해 업무 API 서버나 Batch를 직접 시작할 필요는 없습니다.

```http
GET /api/v1/ledger/gl/balances?startDate=2026-09-01&endDate=2026-09-02&accountCode=10100&currencyCode=KRW
GET /api/v1/ledger/sl/balances?startDate=2026-09-01&endDate=2026-09-02&accountCode=10100&businessPartnerCode=BP-001&deptCode=D-10&currencyCode=KRW
```

위 예제 행이 조회되면 각 응답 목록에 위의 기간 금액을 가진 한 행이 반환됩니다.
같은 서비스를 사용하는 `/api/v1/ledger/balances/gl`, `/api/v1/ledger/balances/sl` 및
`MonolithLedgerQueryAdapter`의 잔액 목록 조회에도 같은 계산이 적용됩니다. 각 경로의 기존 필터 계약은 그대로입니다.

저장소 루트에서 JDK 17, Gradle 8.7 및 의존성이 이미 로컬에 준비되어 있다면 다음으로 확인합니다.
오프라인 캐시가 없으면 먼저 승인된 환경을 준비해야 하며, 이 명령을 위해 자동 설치하지 않습니다.

```powershell
.\gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test --offline --no-daemon --console=plain --max-workers=1
```

JDK 자동 탐색이 안 되면 실제 설치 경로를 담은 `'-Porg.gradle.java.installations.paths=<JDK17 경로>'`를 추가합니다.
기대 결과는 실패·오류 0입니다. `LedgerServiceTest`는 정순·역순, 여러 키와 통화·거래처·부서,
빈 목록·단일 날짜·음수·월경계·날짜 간격 및 정밀도 상한 근처의 세 날짜 모든 순열을 검사합니다.
`LedgerPeriodBalanceQueryTest`는 합성 출력 포트와 **실제 서비스**를 standalone MockMvc에 연결해
두 컨트롤러의 GL/SL HTTP 금액, 필터 전달, 원본 불변을 확인합니다. 이 검증은 운영 DB나 실제 업무 Batch 실행 증거가 아닙니다.

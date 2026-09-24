# Journal Ledger 업무·데이터 흐름

## 전체 흐름

```mermaid
flowchart LR
    SOURCE[업무 모듈 / 사용자 / Kafka] --> IN[HTTP·Kafka Inbound Adapter]
    IN --> USECASE[Journal Use Case]
    USECASE --> RULE[JournalRuleEngine 또는 수동 전표 변환]
    RULE --> VALIDATE[JournalValidationEngine]
    VALIDATE --> DRAFT[DRAFT 전표 저장]
    DRAFT --> REQUEST[REQUESTED 승인 요청]
    REQUEST --> APPROVE[APPROVED]
    APPROVE --> POST[PostingService]
    POST --> SNAPSHOT[GeneralLedger immutable snapshot]
    SNAPSHOT --> PERIOD{회계기간 OPEN 확인}
    PERIOD -->|성공| POSTED[POSTED 전환 / 전표 저장]
    POSTED --> GL[GL Entry / Balance]
    POSTED --> SL[SL Entry / Balance]
    PERIOD -->|마감 / 조회 불가| REJECT[전기 거부 / 쓰기 없음]
    POST --> UNSETTLED[미결 항목 등록]
    UNSETTLED --> SETTLE[수금·지급 반제]
```

## 전표 라이프사이클

| 단계 | 상태 | 핵심 검증 | 데이터 결과 |
| --- | --- | --- | --- |
| 작성 | `DRAFT` | 라인 금액, 차대변, 처리자, 원천 추적 | `journal_entries`, `journal_details` |
| 승인 요청 | `REQUESTED` | 요청자가 canonical 작성자와 동일하고 차대일치 | maker와 요청 상태 |
| 승인 | `APPROVED` | 승인 역할, `REQUESTED`, maker와 다른 canonical approver | 별도 `approved_by` 증거 |
| 전기 | `POSTED` | posting 역할, 승인 증거·차대일치·상세 ID 검증 후 회계기간 재확인 | 승인 증거 유지, 최종 actor, GL/SL 엔트리와 잔액 |

재무 잔액과 기간 집계에는 `POSTED` 전표만 포함합니다. `APPROVED`는 승인됐지만 아직 원장에 반영되지 않은 상태입니다.

### POSTED 최종 이력 보호

초보자 설명: 전기는 장부에 금액을 확정하는 단계입니다. 확정된 원본을 나중에 고치면 당시
승인 내용과 현재 장부가 달라지므로, `POSTED` 원본은 그대로 두고 정정 사실도 별도 전표로
남깁니다.

1. 최초 `APPROVED -> POSTED` 전환은 정상 처리됩니다. JPA 콜백은 로드·저장 직후 캡처한
   persisted 상태와 현재 상태를 함께 보므로, 이 최초 전환을 이미 확정된 이력의 수정으로
   오인하지 않습니다.
2. 한번 commit된 `POSTED` 전표에서는 헤더·감사 setter, 상세의 금액·차대 구분·계정·차원·적요·
   감사 setter, 상세 소유권 변경과 `addDetail`/`removeDetail`/`clearDetails`/`setDetails`가
   변경 전에 `IllegalStateException`을 냅니다. 감사 identity도 최종 이력이며 “비재무 필드”라는
   일반 예외는 없습니다.
3. setter를 거치지 않은 JPA 경로도 persisted-state 콜백이 막습니다. 관리 엔티티 dirty update,
   detached merge, 새 상세 삽입과 orphan removal이 flush에서 실패합니다. 일반 repository의
   `delete`/`deleteById`/`deleteAll`은 엔티티 생명주기 콜백을 거치므로 `POSTED` 상세·헤더 삭제가
   거부됩니다. 콜백을 우회하는 `deleteAllInBatch`/`deleteAllByIdInBatch`와 deprecated
   `deleteInBatch`는 두 journal repository 경계에서 명시적으로 fail-closed합니다. 실패한
   트랜잭션은 rollback하며, 확인은 같은 영속성 컨텍스트를 재사용하지 않고 새 트랜잭션에서
   clear/reload하여 원래 값과 상세 멤버십이 유지됐는지 봅니다.
4. 정정은 원본을 변경하지 않고 원본 lineage를 연결한 새 역분개 또는 조정 전표를 생성해 기존
   승인·전기 통제를 다시 거칩니다. `DRAFT`/`REQUESTED`/`APPROVED`는 기존 상태·maker-checker·
   정밀도 규칙에 따라 계속 편집할 수 있습니다.

이 보호는 도메인과 JPA 영속성 경계의 방어이며 DB trigger가 아닙니다. repository 밖에서 별도
`EntityManager`로 실행하는 bulk JPQL, native SQL과 권한 있는 직접 JDBC/DB 쓰기는 엔티티 콜백과
repository guard를 우회합니다. 조사된 production 코드에서는 이런 전표 변경 경로를 찾지 못했지만,
배포 권한과 향후 우회 writer는 별도 통제로 제한해야 합니다.

저장소 루트에서 전체 회귀를 실행합니다.

```bash
./gradlew :journal-ledger:test
```

forced full 검증 결과는 core 29 suites/215 tests, API 15/51, batch 4/11로 합계
48 suites/277 tests이며 실패·오류·skip은 0입니다. 집중 persistence 19/19와 batch 5/5도
통과했습니다. 감사 대상 회귀 3개는 수정 전 기준 commit `a97d10ab`에서 3/3 실패하고 현재
3/3 통과하여 수정 전후 차이를 확인했습니다. Batch fixture가 먼저
`APPROVED`를 저장한 뒤 전기하도록 바뀐 것은 합법적인 상태 전이를 준비하는 테스트 설정일 뿐,
새 production 업무 흐름이나 우회 API를 추가한 것이 아닙니다. Batch 테스트의 직접 SQL cleanup도
고정된 폐기 가능 H2 테스트 DB만 비우며 production 우회 경로가 아닙니다.

### Maker-checker 승인과 HTTP 권한

HTTP 쓰기 입력은 외부 요청값이 아니라 Gateway가 JWT 검증 뒤 다시 만든 `X-Auth-User`와
쉼표 구분 `X-Auth-Roles`를 사용합니다. 사용자는 trim 후 `Locale.ROOT` lowercase로 canonicalize하며
현재 전표 감사 컬럼 계약에 맞춰 50자를 넘으면 거부합니다. 따라서 `Maker.One`, ` maker.one `,
`MAKER.ONE`은 같은 identity입니다. API가 역할을 검사하고, core가 호출 경로와 무관하게 상태와
maker-checker 불변식을 다시 검사합니다.

| 명령 | 허용 역할 | 상태/identity 결과 |
| --- | --- | --- |
| 수동·HTTP 이벤트·계약 전표 작성 | `ROLE_JOURNAL_MAKER` | trusted actor가 `createdBy`인 `DRAFT`; body actor는 무시 |
| `POST /api/journals/{id}/request-approval` | `ROLE_JOURNAL_MAKER` | 작성자 본인만 `DRAFT -> REQUESTED` |
| `POST /api/journals/{id}/approve` | `ROLE_JOURNAL_APPROVER` | 별도 actor만 `REQUESTED -> APPROVED`, `approvedBy` 저장 |
| `POST /api/journals/{id}/post` | `ROLE_JOURNAL_POSTER` | `APPROVED -> POSTED`, `approvedBy` 유지, `auditUser`는 poster |
| 조회 | Gateway 인증 정책 | 이 모듈의 command role 검사는 적용하지 않음 |

`ROLE_ACCOUNTING_ADMIN`과 `ROLE_ADMIN`은 세 command 역할을 모두 만족하지만 자기 승인을 허용하지
않습니다. `ROLE_` 접두사가 없는 동일 역할 문자열도 정규화합니다. actor가 없으면 HTTP 401,
역할이 없거나 부족하면 403, 잘못된 상태·자기 승인·도메인 검증 실패는 400이며 use case는 권한
실패 전에 호출되지 않습니다. posting 권한은 approval 권한과 별도입니다.

Kafka `transaction-events`와 Spring contract event는 Gateway header가 없으므로 payload의 actor를
신뢰하지 않습니다. 각 inbound adapter가 maker를 `service:journal-kafka-maker`와
`service:journal-spring-event-maker`로 덮어씁니다. `approveAndPost`는 DRAFT를 저장된 maker로 먼저
`REQUESTED`에 제출한 뒤 전달된 checker가 별도 identity일 때만 승인·전기합니다. 따라서 Spring
event 전표를 후속 처리하는 checker도 `service:journal-spring-event-maker`와 다른 canonical service
principal이어야 합니다. 예를 들어 maker `service:closing-maker`, checker
`service:closing-checker`는 가능하지만 maker/checker가 모두 `SYSTEM`이면 실패합니다. 기계
발행자는 공유 `SYSTEM` 대신 서로 구분되는 service principal과 checker 자격을 공급해야 합니다.

초보자 설명: 역할은 “이 버튼을 누를 자격”, identity는 “실제로 누른 사람”입니다. 관리자에게
두 역할이 있어도 자신이 만든 전표를 자신이 승인할 수는 없습니다. 기존 직접 HTTP 연동은
승인 전에 새 request-approval 호출과 Gateway 신뢰 헤더/역할을 연결해야 합니다.

배포 호환성 조사에서 직접 Journal HTTP API를 호출하는 어댑터는 Closing, Deposit,
Expenditure Resolution, Loan, Payable, Receivable, Reconciliation에서 확인됐습니다. 일곱 어댑터
모두 draft 생성의 trusted maker header/role 연결이 필요합니다. Deposit은 현재 원격 승인/전기를
지원하지 않으며, 나머지 승인/전기 호출자는 legacy `X-User-ID`와 DRAFT 직접 승인 순서를
`X-Auth-User`/`X-Auth-Roles` 및 request-approval 단계로 바꿔야 합니다. 이 consumer 변경은
journal-ledger 경계 밖의 별도 배포 게이트입니다.

전표 저장은 `JournalPersistencePort`, GL/SL 엔트리 저장은 `LedgerEntryPersistencePort`,
잔액 조회·저장·재집계는 `LedgerBalancePersistencePort`를 사용합니다.

`PostingService`는 JPA 엔티티를 직접 만들지 않습니다. 승인된 `JournalEntry`를
`GeneralLedger` Aggregate로 바꾸고 `LedgerEntryPersistencePort.save()`에 전달합니다.
JPA와 JDBC bulk adapter는 이 같은 불변 snapshot을 각 저장 형태로 변환하므로 GL과 SL의
차변/대변, 기준통화 금액, lineage가 서로 어긋나지 않습니다.

원장 금액은 `Debit`/`Credit` VO와 `AccountingPrecision`을 거칩니다. 저장 계약은
`DECIMAL(19,2)`이며 소수 센트를 무음 반올림하지 않습니다. 거래통화 합계뿐 아니라
기준통화 합계도 차대일치해야 전기할 수 있습니다.

### 작성 때와 전기 직전의 회계기간 확인

초보자 설명: 작성할 때 열려 있던 장부도 승인 후 전기하기 전에는 닫힐 수 있습니다.
따라서 저장된 `APPROVED` 상태만 믿지 않고, 실제 장부에 반영하기 직전에 다시 확인합니다.

1. 입력은 전표 ID와 처리자입니다. `PostingService`가 같은 트랜잭션에서 전표 헤더를 배타적으로 잠그고
   최신 헤더와 상세를 다시 읽은 뒤
   `GeneralLedger.fromApproved()`로 승인 상태, 거래/기준통화 차대일치, 저장된 상세 ID를
   먼저 검증하여 불변 스냅샷을 만듭니다. 비승인·이미 `POSTED`·잘못된 전표는 기간 조회 전에 거부됩니다.
2. 기존 `ClosingLockValidationFilter`가 `AccountingPeriodStatusPort`로 **회계 반영일
   (`accountingDate`)**의 기간을 다시 확인합니다. 전표 작성일(`slipDate`)이 다른 달이어도
   작성일의 기간을 조회하지 않습니다. 생성 시 `JournalValidationEngine`이 수행하는 기간 검증과
   별개이며, 전기 때 전체 엔진을 재실행하거나 계정 정보를 다시 조회하지 않습니다.
3. 기간 확인을 통과한 뒤에만 `post(poster)`로 상태와 감사 사용자를 변경하고,
   전표 저장 → 같은 스냅샷으로 GL/SL 엔트리 저장 → bulk 잔액 갱신을 각각 한 번 호출합니다.
   처리자를 생략하는 내부 `postJournalEntry(id)`도 같은 검증을 거치며 성공 처리자는 canonical
   `system`입니다. 이 overload는 HTTP posting 권한을 대신하지 않습니다.

기존 `FiscalPeriodAccountingPeriodStatusAdapter`는 `OPEN`을 허용하고 `CLOSED`와
`PERMANENTLY_CLOSED`를 닫힌 기간으로 판단합니다. 기간 부재와 조회 예외도 전기 실패로
전파합니다. 이 거부 경로에서는 원래 `APPROVED` 상태, 승인 증거, 감사 사용자와 상세가 유지되고,
전표·GL/SL 엔트리 저장 및 잔액 갱신 호출은 모두 0회입니다. HTTP command는 위 역할 계약을 따릅니다.

유효 전표의 전기 요청마다 기간 조회가 한 번 추가됩니다. 이는 조회 당시 이미 닫힌 기간의
전기를 차단하는 통제입니다. 같은 전표의 동시 전기는 아래 DB 잠금과 고유 제약으로 차단하지만,
기간 조회 후 동시에 마감되는 분산 경쟁은 별도 통제 대상입니다. 기존 오전기 데이터를 복구하지 않습니다.

### 로컬 회귀 검증

전제: 저장소 루트의 PowerShell, 설치된 JDK 17과 Gradle 8.7/의존성 캐시입니다.
JDK 경로는 실제 설치 위치에 맞춥니다. 아래 명령은 오프라인 테스트만 실행하며 업무 Batch나
외부 DB/서버를 실행하지 않습니다.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot'
.\gradlew.bat :journal-ledger:core:test :journal-ledger:api:test :journal-ledger:batch:test :loan:core:test :loan:api:test :loan:batch:test :loan:api:bootJar :loan:batch:bootJar --offline --no-daemon --console=plain --max-workers=1 --rerun-tasks '-Porg.gradle.java.installations.paths=C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot' '-Porg.gradle.java.installations.auto-download=false'
```

기대 결과는 종료 코드 0과 테스트 실패/오류/skip 0입니다. `PostingServiceTest`는 실제 필터와
기간 port 대역, 실제 Fiscal adapter와 조회 port 대역을 사용하여 기간별 허용·거부, 원본 불변,
쓰기 횟수, 서로 다른 작성일/회계일, 처리자/SYSTEM과 기존 전표 검증 우선순위를 확인합니다.
직접 생성자 소비자인 `LoanJournalPostingFlowTest`도 실제 필터와 명시적 OPEN 기간 대역을
연결하여 Loan → Journal → Ledger 흐름의 회계일자 조회를 확인합니다. 생성자 변경과 이
fixture를 분리하면 소비자 컴파일이 깨지므로 같은 변경에서 검증하며 Loan 업무 로직은 바꾸지 않습니다.
Loan core/API/Batch 테스트와 API/Batch 패키징까지 확인하고, `NO-SOURCE`인 task는
테스트 통과 수에 포함하지 않습니다. `bootJar`는 패키징 검증이며 서버나 업무 Job 실행이 아닙니다.
이 로컬 검증은 실제 PostgreSQL 커밋이나 분산 마감 경합의 증거가 아닙니다.

### 전기 저장 어댑터 선택

기본 어댑터는 JPA입니다. 운영에서 대량 전기/재집계가 필요하면 아래 설정으로 JDBC bulk 구현체를 사용합니다.

```yaml
journal-ledger:
  ledger:
    persistence-mode: jdbc-bulk
```

이 설정을 켜면 전기 엔트리는 batch insert, GL 잔액은 upsert, SL 잔액은 null 거래처·부서 키를 고려한 bulk update/insert로 저장됩니다.
서비스 코드는 같은 포트를 호출하므로 업무 흐름은 바뀌지 않고, 저장 기술만 바뀝니다.

JDBC bulk 모드는 H2 기준 SQL 동작을 `JdbcLedgerBulkPersistenceAdapterTest`에서 검증합니다. 운영 DB에서는 같은 포트 계약을 유지하되 배치 크기, unique index, lock wait, 재집계 동시 실행을 별도 부하 테스트로 확인합니다.

## 자동분개 규칙 흐름

1. 이벤트의 거래유형, 금액, 계정, 통화, 처리자 정보를 받습니다.
2. `JournalRuleCondition`이 문자열·숫자 조건을 평가합니다.
3. 일치한 규칙의 `JournalRuleDetail`이 생성할 전표 라인을 정의합니다.
4. 차대변은 `JournalSide` 타입으로 제한되어 잘못된 문자열을 저장 전에 차단합니다.
5. 필수 표현식 값이 없거나 금액이 0 이하이면 전표 생성을 중단합니다.

`JournalRuleEngine`은 `JournalRuleQueryPort`를 통해 규칙을 읽습니다. JPA 저장소 세부 구조와 조회 메서드는 `JournalRuleQueryAdapter` 뒤에 숨깁니다.

### 이벤트 JSON 금액 정밀도

`POST /api/journals/from-event`의 `eventData`와 Kafka `transaction-events`는 이벤트마다
필드 구성이 달라 `Map<String, Object>`로 전달됩니다. API의 Jackson 설정은 JSON 소수 토큰을
binary floating-point인 `Double`이 아니라 `BigDecimal`로 만들며, JSON 정수 토큰은 기존처럼
정수 타입을 유지합니다. Kafka도 Boot가 관리하는 같은 `ObjectMapper`를
`StringJsonMessageConverter`에 연결하므로 HTTP와 메시지 경계의 소수 처리 규칙이 같습니다.

입력된 `BigDecimal`은 룰 표현식의 `${amount}` 같은 자리표시자에 정확한 문자열로 보간된 뒤
`JournalDetail.setAmount`에 전달됩니다. 여기서 기존 `AccountingPrecision`이 원장
`DECIMAL(19,2)` 계약을 `RoundingMode.UNNECESSARY`로 검사합니다. 따라서
`900719925474099.11`은 센트까지 그대로 유지되고, `100.000000000000001`처럼 허용 범위를
넘는 소수는 `100.00`으로 반올림되기 전에 HTTP 400 또는 Kafka 리스너 예외로 거부됩니다.
Kafka 리스너는 이 예외를 소비 실패로 다시 던지며, 실제 재시도 횟수와 DLQ 라우팅은 배포별
Kafka 오류 처리 설정의 책임입니다. 이 경계 설정은 정밀도를 보존할 뿐 금액을 확정하거나
반올림하지 않으며, 금융 유효성 판단은 계속 core 도메인이 담당합니다.

## 미결 등록·반제 흐름

```mermaid
sequenceDiagram
    participant API as UnsettledController
    participant PortIn as UnsettledItemUseCase
    participant Service as UnsettledService
    participant Domain as UnsettledItem
    participant PortOut as UnsettledItemPersistencePort
    participant Adapter as UnsettledItemPersistenceAdapter
    participant DB as unsettled_items / reference rows

    API->>PortIn: settle(id, amount, actor, reference)
    PortIn->>Service: 반제 유즈케이스 / 쓰기 트랜잭션 시작
    Service->>PortOut: findByIdForSettlement(id)
    PortOut->>Adapter: 반제용 독점 조회 의도
    Adapter->>DB: 부모 ID만 SELECT ... FOR UPDATE
    DB-->>Adapter: 선행 트랜잭션 뒤 잠금 획득
    Adapter->>DB: 현재 상태와 참조 이력 refresh
    Service->>Domain: settle(amount, actor, reference)
    Domain->>Domain: 정밀도·중복·OPEN/PARTIAL/CLEARED 검증
    Service->>PortOut: save(item)
    Adapter->>DB: 금액·상태·참조·감사 정보 저장
    Service-->>PortIn: commit 후 잠금 해제
```

상태는 `OPEN -> PARTIAL -> CLEARED` 순서로 진행합니다. `CLEARED`가 되면 `resolved=true`가 되어 활성 미결 조회에서 제외됩니다.

### 같은 미결 항목의 동시 반제

초보자 설명: 잔액 100인 같은 청구서에 두 창구가 동시에 40과 50을 기록하면, 둘 다 옛 잔액
100을 보고 마지막 저장이 앞선 저장을 덮어써서는 안 됩니다. 이 경로는 청구서에 해당하는
`unsettled_items` 부모 한 행을 잠가 한 요청씩 처리하고, 뒤 요청은 앞 요청의 커밋 결과를
다시 읽은 뒤 계산합니다.

1. `UnsettledService.settleItem`의 호출자 트랜잭션이 시작되고, 서비스는 일반 조회가 아니라
   반제 의도를 표현한 `findByIdForSettlement` 출력 포트를 호출합니다.
2. JPA adapter는 같은 트랜잭션의 미반영 변경을 먼저 flush하고, 참조 이력을 JOIN하지 않은
   ID 전용 `SELECT id FROM unsettled_items WHERE id = ? FOR UPDATE`로 부모 행만 잠급니다.
   `EAGER` 컬렉션까지 한 SQL로 잠그지 않는 이유는 PostgreSQL의 outer join 잠금 제약을
   피하면서도 모든 반제를 하나의 안정된 부모 잠금으로 직렬화하기 위해서입니다.
3. 잠금을 기다리기 전에 같은 영속성 컨텍스트가 항목을 읽었을 수 있으므로, 잠금 획득 뒤
   부모의 현재 금액·상태·감사 정보와 참조 컬렉션을 명시적으로 refresh합니다. 잠금만 얻고
   오래된 1차 캐시 값을 다시 쓰면 여전히 갱신 유실이 생길 수 있습니다.
4. 기존 `UnsettledItem.settle`이 `BigDecimal`/`NUMERIC(19,2)` 정밀도, 참조번호 멱등성,
   잔액 검증과 `OPEN -> PARTIAL -> CLEARED` 전이를 그대로 담당합니다. 서비스는 결과를
   저장하고 트랜잭션이 commit 또는 rollback될 때까지 부모 잠금을 유지합니다.

따라서 잔액 100에 서로 다른 참조로 40과 50이 겹치면 뒤 요청은 최신 잔액을 기준으로
계산하여 누적 반제액 90, 잔액 10이 되고 두 참조가 모두 남습니다. 반대로 같은 **이미 처리된**
참조는 대기 후 최신 이력에서 발견되어 no-op이며, 완전 반제 뒤 그 참조를 다시 보내도 금액·상태·
감사 정보가 바뀌지 않습니다. 잔액 경계에서는 먼저 커밋한 반제 뒤의 실제 잔액을 사용하므로,
기다리던 요청 금액이 그 잔액보다 크면 정상적인 업무 검증 실패로 거부될 수 있습니다.

도메인 검증, 저장/flush 또는 이후 커밋 과정이 실패하면 금액, 상태, 참조 이력, 처리자·시각을
같은 트랜잭션에서 함께 rollback합니다. deadlock, serialization failure 또는 lock timeout은
실패한 트랜잭션 안에서 잠금/저장만 다시 실행하지 않습니다. 호출자가 전체 반제 입력을 새
트랜잭션으로 다시 호출해야 최신 잔액과 참조 이력을 다시 검증할 수 있습니다.

이 변경은 기존 부모 행을 잠금 대상으로 사용하므로 migration, schema version, `@Version`
열을 추가하지 않습니다. 같은 ID의 인기 항목(hot item)은 의도적으로 한 요청씩 처리되며,
긴 트랜잭션은 lock wait와 처리 지연을 늘릴 수 있으므로 실제 부하의 대기 시간을 별도로
관찰해야 합니다. 다른 ID는 이 부모 잠금 때문에 직렬화되지 않습니다. 포트를 거치지 않는
직접 SQL/우회 writer는 보호하지 않으며, 과거 부정합 탐지·대사·복구도 이 변경의 범위가 아닙니다.

### 동시 반제 회귀 실행

저장소 루트에서 JDK 17, Gradle wrapper와 기존 의존성 캐시가 준비되어 있으면 기본 합성 H2
PostgreSQL mode 회귀를 다음처럼 실행합니다. 두 executor thread가 서로 다른 DB 연결과
트랜잭션을 사용하며, `UnsettledSettlementConcurrencyIntegrationTest`가 40+50 누적,
같은 참조와 완전 반제 뒤 재전송의 no-op, 60 커밋 뒤 기다리던 50의 잔액 초과 거부,
flush 뒤 강제 실패의 전체 rollback과 새 트랜잭션 재시도를 확인합니다.

```bash
./gradlew :journal-ledger:core:test \
  --tests '*UnsettledSettlementConcurrencyIntegrationTest' \
  --rerun-tasks --offline --max-workers=1
```

PostgreSQL 고유 잠금 동작은 실행자가 만든 **폐기 가능한 독립 테스트 DB** URL을 명시적으로
공급할 때만 같은 회귀를 실행합니다. fixture는 로컬 테스트 전용 `postgres` 사용자와 빈
비밀번호를 사용하므로 그 조건으로 격리된 인스턴스여야 합니다. 예시의 주소·포트·DB명은
테스트 인스턴스 값으로 바꿉니다.

```bash
JOURNAL_UNSETTLED_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:55432/unsettled770 \
  ./gradlew :journal-ledger:core:test \
  --tests '*UnsettledSettlementConcurrencyIntegrationTest' \
  --rerun-tasks --offline --max-workers=1
```

기대 결과는 선택한 테스트 실패·오류·skip 0입니다. H2에서는 선행 트랜잭션 동안 두 번째
future가 끝나지 않는지 확인하고, PostgreSQL에서는 독립 backend PID와
`pg_stat_activity.wait_event_type = 'Lock'`도 확인합니다. 이 fixture는 매 실행 고유 schema와
합성 데이터만 사용하며 Flyway migration 검증이 아닙니다. H2 결과는 PostgreSQL 검증을
대체하지 않고, 두 실행 모두 운영 부하·긴 트랜잭션의 대기 분포·deadlock/serialization/timeout
장애 주입·분산 재시도·운영 DB 또는 과거 데이터 정합성을 증명하지 않습니다.

## API 계약 요약

| 기능 | 엔드포인트 | 주요 입력 |
| --- | --- | --- |
| 전표 생성 | `POST /api/journals` | 전표 DTO, `X-Auth-User`, maker role |
| 이벤트 기반 전표 생성 | `POST /api/journals/from-event` | 이벤트 데이터, `X-Auth-User`, maker role |
| 승인 요청 | `POST /api/journals/{id}/request-approval` | `X-Auth-User`, maker role |
| 승인 | `POST /api/journals/{id}/approve` | `X-Auth-User`, approver role |
| 전기 | `POST /api/journals/{id}/post` | `X-Auth-User`, poster role |
| 거래처별 미결 조회 | `GET /api/unsettled/businesspartner/{businessPartnerCode}` | 거래처 코드 |
| 반제 | `POST /api/unsettled/{id}/settle` | 금액, `settlementReference`, `X-User-ID` |
| FX 대시보드 조회 | `GET /api/fx/dashboard` | 입력 없음 |

### FX 대시보드 현재 스냅샷

`GET /api/fx/dashboard`는 프론트엔드가 요구하는 합계, 주요 환율(`USD/KRW`, `EUR/KRW`,
`JPY/KRW`)과 통화별 포지션을 `BigDecimal` JSON 숫자로 반환합니다. 포지션의 한도 상태는
`SAFE`, `WARNING`, `EXCEEDED` 중 하나입니다. `totalKrwAmount`와
`dailyValuationGainLoss`는 각각 포지션의 `krwAmount`와 `valuationGainLoss` 합계와 반드시
일치하며 DTO 생성 시 불일치가 거부됩니다. `gainLossPercent`는 현재 응답에 계산 기준 금액이
없으므로 이 scaffold가 공급하는 화면 표시 지표이며, 다른 필드로부터 계산한 값이 아닙니다.

현재 응답은 API 계약 연결을 위한 **결정적 읽기 전용 스냅샷 scaffold**입니다. 저장소를
조회하거나 외부 환율 공급자와 통신하지 않으며, 실시간 시장 데이터 feed가 아닙니다.
따라서 같은 배포 버전에서는 호출할 때마다 같은 값이 반환됩니다. 실제 환율과 포지션을
반영하려면 별도의 core 조회 유즈케이스와 출력 포트가 먼저 정의되어야 합니다.

```http
GET /api/fx/dashboard
```

로컬에서는 저장소 루트에서 `./gradlew :journal-ledger:api:test`를 실행하고
`FxDashboardControllerTest`가 경로, 전체 중첩 필드, 소수 직렬화와 한도 상태를 검증하는지
확인합니다. 이 검증은 운영 환율 공급자나 포지션 저장소 연결을 확인하지 않습니다.

## 잔액 재집계 Batch 흐름

```mermaid
sequenceDiagram
    participant Scheduler as 운영자/스케줄러
    participant Batch as dailyBalanceReaggregationJob
    participant Tasklet as BalanceCleanUpTasklet / Chunk
    participant Service as LedgerService
    participant Port as LedgerBalancePersistencePort
    participant DB as GL/SL Balance

    Scheduler->>Batch: startDate/endDate 또는 baseDate 전달
    Batch->>Tasklet: start Step: 기간 고정 / owner barrier 획득
    Tasklet->>Service: owner cleanup Step 실행
    Service->>Port: 전체 잠금 + owner 검증 → 기간 잔액 삭제
    Note over Batch, DB: cleanup 트랜잭션 커밋
    Batch->>Tasklet: POSTED 상세를 날짜순 100건 chunk로 조회
    Tasklet->>Service: updateLedgerBalancesBulkForReaggregation(owner, range, details)
    Service->>Port: chunk 계정 잠금 + owner 검증 → 최신 GL/SL 조회
    Service->>Port: 금액 집계 → bulk 저장
    Port->>DB: 각 chunk와 checkpoint 트랜잭션 커밋
    Batch->>Service: 전체 stripe 잠금 → POSTED/GL/SL exact reconciliation
    Service->>DB: 일치할 때만 OPEN / epoch 증가
```

초보자 관점에서는 "과거 날짜의 전표가 바뀌면 그 기간 장부를 다시 더한다"고 이해하면 됩니다. Batch는 날짜 파라미터를 해석하고 core 서비스를 호출할 뿐이며, 실제 잔액 계산과 저장 순서는 `LedgerService`와 출력 포트가 담당합니다.
V15 barrier가 전체 Job 수명 동안 입력과 공개를 통제합니다. 실패하면 부분 잔액은 DB에 남을 수
있지만 조회·전기에는 수락되지 않습니다. 같은 JobInstance 재시작은 성공한 cleanup/chunk를
건너뛰고 checkpoint에서 계속하며, 다른 인스턴스는 owner 충돌로 실패합니다.
## 재시도와 정합성 주의사항

- 동일 반제 참조번호는 다시 적용하지 않습니다.
- 같은 전표의 동시 요청은 DB 헤더 잠금으로 직렬화합니다. 커밋 후 재요청은 최신 `POSTED` 상태를 읽고 승인 상태 검증으로 거부하므로 추가 GL/SL·잔액 효과가 없습니다. 상세 동작과 검증은 [동시 전기 제어](posting-concurrency.md)를 참고합니다.
- 서로 다른 전표도 계정·통화별 공통 잠금을 먼저 확보하여 GL/SL의 최신 금액에 누적합니다. 신규 잔액과 NULL SL 차원도 포함합니다. 데드락 실패는 호출자의 전체 트랜잭션을 롤백한 뒤 새 트랜잭션으로 재시도합니다.
- 기간 검증 실패 후 재시도해도 기간을 다시 조회합니다. 재개 가능 여부는 별도 승인된 기간 관리 절차로 판단합니다.
- 과거 날짜 전표는 이후 잔액 재집계 범위를 확인해야 합니다.
- 거래처별 미결 조회는 출력 포트의 DB 조건 조회를 사용해 대량 데이터를 메모리에 올리지 않습니다.
- GL/SL 조건 조회도 `LedgerBalancePersistenceAdapter`의 DB 쿼리에서 필터링합니다.

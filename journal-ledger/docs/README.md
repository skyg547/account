# journal-ledger docs

`journal-ledger` 모듈은 회계 코어 엔진입니다. 타 모듈의 경제적 이벤트를 수신하여 전표(`JournalEntry`)를 만들고, 승인 흐름을 거쳐 GL/SL 잔액과 원천 추적 정보를 관리합니다.

## 문서 목록

1. [beginner-guide.md](./beginner-guide.md): 초보자 개념, 코드 탐색, DDD/헥사고날 구조
2. [process-flow.md](./process-flow.md): 전표·원장·미결 업무와 데이터 흐름
3. [schema.md](./schema.md): 핵심 테이블 관계와 소유권
4. [ledger-carry-forward.md](./ledger-carry-forward.md): 원장 잔액 이월 상세
5. [posting-concurrency.md](./posting-concurrency.md): 동일/서로 다른 전표의 동시 전기, 역분개, 잔액 잠금, 재시도, V13/V14/V15/V17 업그레이드와 검증
6. [layer-guide.md](./layer-guide.md): application/domain/adapter 계층과 출력 포트 경계

루트 `README.md`에는 모듈 개요와 빠른 실행 정보만 두고, 상세 설명은 위 문서에서 통합 관리합니다.

## 잔액 재집계 Batch

`journal-ledger:batch`는 API 서버가 아니라 Spring Batch 실행 모듈입니다. 현재 대표 Job은 `dailyBalanceReaggregationJob`이며, 과거 전표 수정이나 누락 전표 전기 후 GL/SL 잔액을 특정 기간 기준으로 다시 계산할 때 사용합니다.

- batch config: Job/Step 연결만 담당합니다.
- 시작 Step은 날짜 입력 전체를 검증한 뒤 요청 범위를 한 번 정규화합니다. 완전한 `startDate/endDate` 또는 `fromDate/toDate` 범위와 `baseDate/targetDate` 단일일을 받으며, 같은 날짜로 정규화되는 중복 별칭도 허용합니다. 시작 별칭 `startDate/fromDate`만 있으면 해당 날짜 하루, 종료 별칭 `endDate/toDate`만 있으면 시작 Step의 JVM 기본 시간대 전일부터 지정 종료일까지로 해석합니다. 충돌 별칭, 역전·형식 오류, 단일일과 범위의 혼용은 owner barrier 획득이나 잔액 삭제 전에 실패합니다.
- 날짜 입력이 없으면 시작 시점 JVM 기본 시간대의 전일을 한 번 선택합니다. 시작 Step은 256개 stripe를 잡고 요청 종료일을 필요한 후속일까지 확장한 유효 범위를 V15 `REBUILDING(owner JobInstance ID, range, epoch)`과 JobExecutionContext에 함께 고정합니다.
- owner cleanup 뒤 `JpaPagingItemReader`가 POSTED 상세를 날짜/ID 순으로 100건씩 읽습니다. owner writer만 barrier 안에서 쓸 수 있어 성공 chunk와 checkpoint가 함께 커밋됩니다.
- 장애 시 같은 식별 JobParameters로 같은 JobInstance를 재시작합니다. 다음 날이나 자정 이후여도 저장된 owner 유효 범위와 JobExecutionContext를 재사용하며, 완료된 cleanup과 이미 커밋된 chunk checkpoint는 유지됩니다. 다른 인스턴스, 전기, 수동 cleanup, 잔액 조회는 거부됩니다.
- 마지막 Step은 안정된 POSTED 입력과 GL/SL의 날짜·전체 key·nullable BP/부서·일별 차대·기초/기말을 compact DB 집계로 대사합니다. 정확히 일치할 때만 `OPEN`/새 epoch로 공개합니다.

초보자 설명: Spring Batch의 JobInstance는 Job 이름과 **식별(identifying) JobParameters** 조합으로
구분합니다. 아래처럼 launcher에 전달한 날짜 파라미터는 별도 non-identifying 지정이 없으면 식별값입니다.
실패 복구에는 파라미터 이름과 값을 그대로 다시 사용해야 하며, 같은 날짜라도 별칭을 바꾸거나
`run.id`/timestamp 같은 새 식별값을 더하면 새 JobInstance가 되어 cleanup부터 다시 실행됩니다.
완료된 JobInstance를 의도적으로 다시 돌릴 때도 새 식별값이 필요합니다. 날짜 파라미터가 없는 실행도
같은 원칙이므로, 실패 restart는 최초에 고정된 전일을 재사용하고 새 인스턴스만 새 시작 시점의 전일을 선택합니다.

```powershell
.\gradlew :journal-ledger:batch:bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --spring.batch.job.enabled=true --spring.batch.job.name=dailyBalanceReaggregationJob startDate=2026-04-01 endDate=2026-04-30" --console=plain
```

IntelliJ에서는 공유 실행 설정 `Journal Ledger Batch Reaggregation`을 사용할 수 있습니다.

날짜 형식은 ISO `yyyy-MM-dd`입니다. 세부 실패·재시작·운영 제한은
[posting-concurrency.md](posting-concurrency.md#재집계와-이월-경계)를 참고합니다.

## 주요 특징 및 구현 기준 (Phase 1, 4 반영)

- **Kafka 기반 비동기 전표 처리**:
  - `KafkaTransactionListener`를 통해 `transaction-events` 토픽으로부터 다른 서브레저 모듈(예: 결산, 대사, 수납)의 회계 이벤트를 수신합니다.
  - 전표 생성에 실패할 경우(예외 발생) 무음 처리하지 않고 명시적으로 `RuntimeException`을 던져, Spring Kafka의 기본 **DLQ(Dead Letter Queue)** 및 Retry 메커니즘을 유도하는 Fail-Safe 설계를 따릅니다.
- **Journal Rule Engine (자동 분개 룰 엔진)**:
  - 계정과 차대변은 활성 규칙으로 결정하지만 거래통화와 거래통화→KRW 환율은 이벤트의 명시적인 transaction provenance만 사용합니다. 통화는 `transactionCurrencyCode`/`currencyCode`/`currency`, 환율은 `transactionToBaseRate`/`exchangeRate`/`fxRate`와 각각의 `transaction.*` 형태만 허용하며 `company`, `functionalCurrency`, `reportingCurrency`, `accountingPolicy` 기준통화에서 추론하지 않습니다.
  - 지원 별칭이 여러 개 공급되면 통화는 trim·대문자 정규화 후, 환율은 숫자 비교 후 모두 같아야 합니다. 같은 값의 중복은 허용하고 충돌은 전표 생성 전에 거부합니다.
  - 필수 DSL 값이 누락되면 `null`을 반환하여 이후 단계에서 크래시를 유도하는 대신, 명시적 도메인 예외를 던져 룰의 엄격한 유효성을 보장합니다.
  - HTTP 이벤트는 Gateway가 검증한 actor로 payload identity를 덮어쓰고, Kafka와 Spring contract 이벤트는 각각 listener 전용 service principal을 maker로 사용합니다. 공유 `SYSTEM` 또는 이벤트 payload actor를 trusted identity로 사용하지 않습니다.
- **Closing Lock 통제**:
  - 전표 생성 시 결산 모듈(`ClosingStatusAdapter`)의 기간 잠금(Period Lock) 및 마감 상태를 검사하여, 닫힌 회계 기간에 소급 기표를 넣지 못하게 차단합니다.
- **미결 항목 출력 포트**:
  - `UnsettledService`는 `UnsettledItemPersistencePort`에만 의존하고, 실제 JPA 조회는 `UnsettledItemPersistenceAdapter`가 수행합니다.
  - 거래처별 활성 미결 조회는 DB 조건으로 실행하며, 반제는 처리자와 외부 참조번호를 기록해 재시도 중복 반영을 막습니다.
- **전기·잔액 출력 포트**:
  - `PostingService`는 `JournalPersistencePort`, `LedgerEntryPersistencePort`를 사용합니다.
  - `LedgerService`는 `LedgerBalancePersistencePort`를 사용하고, GL/SL 조회 조건은 JPA 어댑터의 DB 쿼리에서 적용합니다.

## 대용량 전기 저장 모드

- 기본값은 Spring Data JPA 어댑터입니다. 개발·소규모 검증에서는 JPA가 엔티티 상태 추적을 해주기 때문에 디버깅이 쉽습니다.
- 1억 건급 전기·재집계 운영 경로는 같은 포트의 JDBC bulk 어댑터로 전환할 수 있습니다.
- 설정 예시:

```yaml
journal-ledger:
  ledger:
    persistence-mode: jdbc-bulk
```

- `JdbcLedgerEntryBulkPersistenceAdapter`는 GL/SL 엔트리를 JDBC batch insert로 저장합니다.
- `JdbcLedgerBalanceBulkPersistenceAdapter`는 GL 잔액을 DB 방언별 upsert로 저장하고, SL 잔액은 거래처·부서가 `null`인 키도 안전하게 처리하도록 bulk update 후 미삽입 행만 bulk insert합니다.
- 두 저장 모드 모두 먼저 동일한 계정·통화 잠금을 획득합니다. SL의 NULL 차원과 아직 없는 잔액 행도 보호하며, JDBC 조회 객체는 분리하여 JPA 자동 flush가 bulk SQL을 덮어쓰지 않게 합니다.
- 실제 PostgreSQL 검증 명령과 범위는 [posting-concurrency.md](posting-concurrency.md)에 있습니다. 운영 데이터량의 배치 크기, 인덱스와 잠금 대기 부하 검증은 별도입니다. MySQL은 이 PostgreSQL 검증의 대상이 아니며 기본 REPEATABLE_READ에서는 잔액 갱신을 거부합니다.

## 로컬 실행

루트 [docs/local-development.md](../../docs/local-development.md)의 IntelliJ/Gradle 기준을 먼저 확인합니다.

API local과 Batch local은 서로 다른 H2 메모리 DB를 사용하지만 둘 다 module-owned Flyway
V1~V17을 적용한 뒤 Hibernate `validate`를 수행합니다. 따라서 V15 제어 테이블과
V17 역분개 operation 관계도 실제 local entrypoint에서 존재합니다. dev/prod는 runtime Flyway를 계속 끄고
승인된 별도 migration-runner가 먼저 적용한 스키마를 validate합니다.

```powershell
.\gradlew :journal-ledger:core:test :journal-ledger:api:test --console=plain --max-workers=1 --no-daemon
.\gradlew :journal-ledger:api:bootRun --console=plain
.\gradlew :journal-ledger:api:bootRun --args="--journal-ledger.ledger.persistence-mode=jdbc-bulk" --console=plain
```

IntelliJ 공유 실행 설정:

- `Journal Ledger API bootRun`
- `Journal Ledger API JDBC Bulk`

## 아카이브

소스 트리 하위에 있던 계층별 README는 현재 코드와 일부 불일치가 있어 아래로 이동했습니다.

- [archive/application-layer-readme-legacy.md](archive/application-layer-readme-legacy.md)
- [archive/domain-layer-readme-legacy.md](archive/domain-layer-readme-legacy.md)
- [archive/adapter-layer-readme-legacy.md](archive/adapter-layer-readme-legacy.md)

최신 계층 설명은 [layer-guide.md](./layer-guide.md)를 기준으로 합니다.

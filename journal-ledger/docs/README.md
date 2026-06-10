# journal-ledger docs

`journal-ledger` 모듈은 회계 코어 엔진입니다. 타 모듈의 경제적 이벤트를 수신하여 전표(`JournalEntry`)를 만들고, 승인 흐름을 거쳐 GL/SL 잔액과 원천 추적 정보를 관리합니다.

## 문서 목록

1. [beginner-guide.md](./beginner-guide.md): 초보자 개념, 코드 탐색, DDD/헥사고날 구조
2. [process-flow.md](./process-flow.md): 전표·원장·미결 업무와 데이터 흐름
3. [schema.md](./schema.md): 핵심 테이블 관계와 소유권
4. [ledger-carry-forward.md](./ledger-carry-forward.md): 원장 잔액 이월 상세

루트 `README.md`에는 모듈 개요와 빠른 실행 정보만 두고, 상세 설명은 위 문서에서 통합 관리합니다.

## 주요 특징 및 구현 기준 (Phase 1, 4 반영)

- **Kafka 기반 비동기 전표 처리**:
  - `KafkaTransactionListener`를 통해 `transaction-events` 토픽으로부터 다른 서브레저 모듈(예: 결산, 대사, 수납)의 회계 이벤트를 수신합니다.
  - 전표 생성에 실패할 경우(예외 발생) 무음 처리하지 않고 명시적으로 `RuntimeException`을 던져, Spring Kafka의 기본 **DLQ(Dead Letter Queue)** 및 Retry 메커니즘을 유도하는 Fail-Safe 설계를 따릅니다.
- **Journal Rule Engine (자동 분개 룰 엔진)**:
  - 하드코딩된 계정과 통화를 제거하고, `company`, `accountingPolicy` 등 시스템 설정 룰을 통해 통화, 차대변, 정상 잔액 방향을 판단합니다.
  - 필수 DSL 값이 누락되면 `null`을 반환하여 이후 단계에서 크래시를 유도하는 대신, 명시적 도메인 예외를 던져 룰의 엄격한 유효성을 보장합니다.
  - 전표 생성 주체(`audit actor`)를 "SYSTEM"으로 하드코딩하지 않고, 이벤트 데이터에 실린 Caller Actor를 파싱해 전표 이력 추적성을 보장합니다.
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
- 현재 H2 기준 SQL 동작은 core 테스트에서 검증했습니다. 운영 PostgreSQL/MySQL에서는 배치 크기, 인덱스, 락 대기, 재집계 동시성 부하 테스트를 별도로 수행해야 합니다.

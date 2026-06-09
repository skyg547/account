# journal-ledger docs

`journal-ledger` 모듈은 회계 코어 엔진입니다. 타 모듈의 경제적 이벤트를 수신하여 전표(`JournalEntry`)를 만들고, 승인 흐름을 거쳐 GL/SL 잔액과 원천 추적 정보를 관리합니다.

## 문서 목록

- [beginner-guide.md](./beginner-guide.md)
- [process-flow.md](./process-flow.md)
- [schema.md](./schema.md)
- [ledger-carry-forward.md](./ledger-carry-forward.md)

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

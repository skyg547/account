# journal-ledger 초보자 가이드 (Beginner Guide)

`journal-ledger` 모듈은 회계 플랫폼의 핵심 엔진으로, 거래를 회계 전표로 만들고 승인하여 원장(Ledger)에 반영하고, 나중에 데이터를 역추적할 수 있게 기록을 남기는 역할을 합니다. 이 모듈은 헥사고날 아키텍처(Ports and Adapters)로 설계되어 있습니다.

## 🌟 초보자를 위한 개념 설명

* **전표 (Journal Entry):** 영수증의 요약본입니다. 언제, 무슨 목적으로 얼마를 썼는지 또는 벌었는지 기록하는 하나의 단위입니다.
* **차변과 대변 (Debit / Credit):** 회계의 기본 원칙으로 돈의 들어옴과 나감을 구분합니다. 양쪽의 금액 합계는 항상 같아야 합니다.
* **원장 (Ledger):** 전표들이 모여서 만들어진 큰 장부입니다. "현재 우리 회사의 현금 잔액은 얼마인가?"를 물어볼 때 답을 주는 곳입니다.
* **Lineage (계보/출처):** 이 전표가 "어떤 시스템의 어떤 거래(예: 대출 시스템의 #1234 대출 실행)" 때문에 생겨났는지 꼬리표를 달아두는 것입니다. 이 꼬리표가 있어야 나중에 회계 감사 시 거래 내역을 추적할 수 있습니다.

## 주요 개념

### 전표 (JournalEntry) 와 전표 라인 (JournalDetail)
- `JournalEntry`는 회계 처리 한 건의 헤더를 의미합니다.
- `JournalDetail`은 실제 차변/대변 금액이 기록되는 상세 라인입니다.

### 상태 (JournalEntryStatus)
- `DRAFT`: 작성 중
- `REQUESTED`: 승인 요청됨
- `APPROVED`: 승인 완료
- `POSTED`: 원장 반영 완료 (전기됨)

### ID 기반 참조 원칙
- `journal-ledger`는 계정과목(`account_code`), 부서(`department_code`), 거래처(`business_partner_code`) 등을 저장할 때 `master-data`의 엔티티를 직접 연결(Join)하지 않고 코드(ID) 값만 문자열로 저장합니다.

## 코드 분석 순서 (헥사고날 아키텍처 기준)

1. **Inbound Adapter:** `JournalController` (API 엔드포인트)
2. **Inbound Port:** `JournalUseCase` 인터페이스
3. **Application Service:** `JournalService` (트랜잭션 조율)
4. **Domain Model:** `JournalEntry`, `JournalDetail` (전표 상태 전이, 차대변 일치 검증 등 핵심 로직)
5. **Outbound Port:** `JournalPersistencePort` 인터페이스
6. **Outbound Adapter:** `JpaJournalPersistenceAdapter`
7. **Repository:** Spring Data JPA 레포지토리

## 변경 시 주의사항

- 전표 승인(`APPROVED`)과 원장 반영(전기, `POSTED`)은 분리된 과정입니다. 전기가 완료되어야 실제 잔액이 변경됩니다.
- 도메인 로직은 엔티티 내부나 Domain Policy 객체에 위치해야 하며, Application Service는 이를 조율만 해야 합니다.
- 외부 모듈과의 통신은 반드시 Port/Adapter를 거쳐야 하며 엔티티 노출은 금지됩니다.
- 멀티 스테이지 Docker 환경에서 독립적인 컨테이너 구동 시 의존성(DB, Kafka 등) 연결을 항상 고려해야 합니다.
# Loan Process Flow

## 1. 전체 흐름

```mermaid
flowchart TD
    A[대출 계약 생성] --> B[대출 실행]
    B --> C[이연 항목 등록 및 수수료 인식]
    C --> D[EIR 기반 초기 상각 스케줄 생성]
    D --> E{중도상환/조건변경 발생?}
    E -- 아니오 --> F[월별/일별 상각 스케줄 처리]
    E -- 예 --> G[Loan Event 발생]
    G --> H[SCD2: 기존 데이터 이력화 및 새 버전 생성]
    H --> I[EIR 재계산 및 스케줄 재생성]
    I --> F
```

## 2. 헥사고날 아키텍처 관점의 흐름

```mermaid
sequenceDiagram
    participant API as Inbound Adapter (Web)
    participant UseCase as Inbound Port (UseCase)
    participant Domain as Domain (Loan & EIR Calculator)
    participant OutPort as Outbound Port
    participant DB as Outbound Adapter (JPA)

    API->>UseCase: 대출 생성/실행 요청 (ID 기반)
    UseCase->>OutPort: 거래처 ID 등 외부 참조 검증
    UseCase->>Domain: 초기 EIR 계산 및 스케줄 생성
    Domain-->>UseCase: 스케줄 리스트 반환
    UseCase->>OutPort: 저장 요청
    OutPort->>DB: 엔티티 변환 후 DB 반영 (SCD2 처리)
```

## 3. 핵심 기술 요소 적용 사항

### 3.1 헥사고날 분리
- 대출 로직, 이자율 계산, 상환 스케줄 계산(`EIRCalculator`)은 특정 프레임워크나 DB에 의존하지 않는 순수 도메인 모듈로 구성됩니다.
- DB 저장은 `LoanPersistencePort`를 구현하는 어댑터에서 JPA 기술을 사용해 수행됩니다.

### 3.2 ID 기반 참조 (ID-based references)
- `BusinessPartner`나 `Currency` 정보는 이름이나 일반 코드가 아닌 `business_partner_id`, `currency_id` 등 시스템 고유 식별자를 통해 연결됩니다.

### 3.3 SCD2를 활용한 재계산 이력 추적
- 중도상환 이벤트나 금리 변경 이벤트가 발생하면 `loans` 및 `eir_amortization_schedules` 테이블의 기존 레코드는 `is_current = false`, `valid_to = 현재시간`으로 마감됩니다.
- 새로운 금리와 원금을 바탕으로 생성된 새 데이터가 `is_current = true` 상태로 삽입되어 변경 추적이 투명하게 이루어집니다.

### 3.4 Multi-stage Docker 및 클라우드 배포
- 복잡한 수학 연산 로직을 포함하는 이 모듈은 다단계 빌드를 통해 컴파일되며, 프로덕션 런타임 환경에서는 JVM 최적화가 적용된 가벼운 컨테이너에서 독립적으로 구동됩니다.
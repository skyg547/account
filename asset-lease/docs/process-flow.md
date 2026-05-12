# Asset-Lease Process Flow

## 1. 전체 업무 흐름

```mermaid
flowchart TD
    A[자산/리스 계약 정보 등록] --> B{계약 종류?}
    B -->|고정자산| C[취득 분개 생성 및 자산 인식]
    B -->|IFRS 16 리스| D[사용권자산 및 리스부채 초기 인식]
    C --> E[월별 감가상각 실행]
    D --> F[월별 리스 상각/이자 스케줄 실행]
    E --> G[결산 및 회계 전표 연동]
    F --> G
    G --> H[자산 처분 / 리스 종료 또는 재측정]
```

## 2. 헥사고날 아키텍처에 따른 트랜잭션 흐름

```mermaid
sequenceDiagram
    participant API as Inbound Adapter (Web/API)
    participant UseCase as Inbound Port (UseCase)
    participant Domain as Domain (Depreciation/Lease Calc)
    participant OutPort as Outbound Port
    participant External as External Service/DB

    API->>UseCase: 감가상각 실행 요청 (월 마감 기준)
    UseCase->>OutPort: 현재 활성(Active) 자산/리스 ID 기반 조회
    OutPort-->>UseCase: 엔티티 매핑 후 반환
    UseCase->>Domain: 상각액 및 이자/원금 분리 계산
    Domain-->>UseCase: 계산 결과(상각 스케줄) 생성
    UseCase->>OutPort: SCD2 이력 추가 및 데이터 갱신 요청
    UseCase->>OutPort: Journal-Ledger 서비스 연동 (전표 생성)
    OutPort->>External: 트랜잭션 커밋
```

## 3. 핵심 아키텍처 요소

### 3.1 ID 기반 참조 처리 (ID-based references)
- 외부 시스템(조직도, 거래처 마스터)에 의존하는 항목들은 부서 코드(문자)가 아닌 `department_id`(UUID 등)로 저장됩니다. 시스템 간 결합도를 낮추고 데이터 불일치를 방지합니다.

### 3.2 상태와 금액 변화의 이력 관리 (SCD2)
- 내용연수가 연장되거나 리스 조건(임대료, 할인율)이 재측정(Remeasurement)되는 경우 기존 정보는 과거 데이터(`is_current = false`)로 마감되고, 새로운 조건의 데이터가 새 버전으로 쌓이게 되어 자산 변동의 투명한 추적과 감사가 가능해집니다.

### 3.3 분산 시스템 연계 (Ports and Adapters)
- 감가상각 및 리스 분개는 본 모듈 내에서 확정되는 것이 아니라 `JournalServicePort`라는 Outbound 인터페이스를 통해 회계 코어(Journal-Ledger) 모듈로 전달됩니다. 어댑터(Adapter)를 교체하면 시스템 연계 방식을 Kafka 메시징이나 gRPC로 유연하게 바꿀 수 있습니다.

### 3.4 Multi-stage Docker 빌드 및 배포
- CI/CD 파이프라인에서 앱은 다단계 도커 파일(`Dockerfile`)을 기반으로 빌드됩니다. 첫 번째 스테이지(builder)에서 의존성 패키지를 다운받고 앱을 조립하며, 두 번째 스테이지(runtime)에서는 순수하게 실행에 필요한 JRE와 빌드된 JAR 파일만 복사하여 경량화된 컨테이너를 구동합니다.
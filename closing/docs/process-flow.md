# Closing Process Flow

## 1. 이 모듈이 하는 일

`closing`은 "해당 회계기간을 정말 닫아도 되는가"를 통제하고, 기간 잠금 및 마감 조정을 수행하는 컨트롤 타워입니다.

## 2. 결산 캘린더 및 마감 흐름 (헥사고날 아키텍처 기반)

```mermaid
flowchart TD
    subgraph Inbound Adapters (Web / API / Job)
        A[사용자 마감 시작 요청] --> B[ClosingController]
        B --> C[ClosingUseCase / Port]
    end

    subgraph Application & Domain
        C --> D[ClosingService]
        D --> E[ClosingCalendar 엔티티 생성 및 상태 변경]
        E --> F[태스크(Task) 및 게이트(Gate) 검증 규칙 수행]
        F --> G{모든 조건 통과?}
        G -->|Yes| H[FiscalPeriod 상태 업데이트 및 Lock 생성]
    end

    subgraph Outbound Adapters
        H --> I[PeriodLockPersistencePort]
        I --> J[(DB: 기간 잠금 이력 기록)]
    end
```

## 3. 핵심 처리 원칙

### 3.1 헥사고날 통제망 연계
- 전표 생성 모듈(`journal-ledger`)은 데이터를 INSERT하기 전 반드시 Closing 모듈이 제공하는 `AccountingPeriodStatusPort`를 호출해 해당 날짜가 열려 있는지(OPEN) 확인합니다. Closing 모듈은 독립적인 도메인 규칙을 바탕으로 결과를 응답합니다.

### 3.2 ID 기반 참조 (ID-based references)
- 다른 모듈에 있는 특정 회계 기간이나 조직 단위, 사용자 정보를 참조할 때, `period_id`, `approved_by_id` 등 고유 ID만을 사용해 결합도를 낮췄습니다.
- 회계기간 정합성 조회와 상태 변경은 `FiscalPeriodControlPort` 계약을 통해 수행합니다. `closing` 도메인 엔티티는 `FiscalPeriod @ManyToOne` 대신 `fiscal_period_id` 값만 저장합니다.

### 3.3 상태 변경과 이력 추적 (SCD2 관점)
- 마감(Closing)의 특성상 상태가 한 번 변하면 되돌리기 어렵고 증적이 중요합니다. 따라서 `FiscalPeriod`의 상태가 변경되거나 잠금이 발생(Period Lock)할 때, 단순 UPDATE가 아니라 시간 정보(`valid_from`, `valid_to`)를 포함한 버전닝 또는 이력 테이블(로그성) 추가 방식을 활용해 과거 상태를 완벽히 재현할 수 있게 합니다.

### 3.4 Multi-stage Docker 운영
- 배포 파이프라인 상에서 Builder 이미지를 통해 컴파일과 테스트가 수행되고, 최종적으로 슬림한 Runtime Docker 컨테이너만 배포되어 메모리 사용량을 최소화하고 보안 취약점을 줄입니다. 마감 시즌에 트래픽이 몰리지 않으므로 경량 컨테이너 운영에 매우 적합합니다.

### 3.5 자동 평가/충당 분개 설정
- `runValuationBatch`, `runProvisionBatch`는 더미 계정/고정 금액을 쓰지 않습니다.
- 운영 설정의 `account.closing.accounting.valuation-rules.<VALUATION_TYPE>`와 `account.closing.accounting.provision-rules.<PROVISION_TYPE>`에서 차변 계정, 대변 계정, 금액을 읽어 전표를 생성합니다.
- 설정이 누락되거나 금액이 0 이하이면 자동 분개를 생성하지 않고 명시적으로 실패합니다.

예시:

```yaml
account:
  closing:
    accounting:
      valuation-rules:
        FX_RATE:
          debit-account-code: "510100"
          credit-account-code: "110100"
          amount: 1234.56
      provision-rules:
        BAD_DEBT:
          debit-account-code: "550100"
          credit-account-code: "129100"
          amount: 789.10
```

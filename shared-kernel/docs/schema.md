# Shared-Kernel Schema

## 1. 포함 타입

```mermaid
flowchart TD
    A[shared-kernel]
    A --> B[BoundedContext enum]
    A --> C[ServiceDescriptor record]
    A --> D[Masked annotation]
    A --> E[MaskingSerializer]
```

## 2. `BoundedContext`

값:
- `MASTER_DATA`
- `JOURNAL_LEDGER`
- `RECEIVABLE`
- `PAYABLE`
- `ASSET_LEASE`
- `LOAN`
- `CLOSING_RECONCILIATION_REPORTING`

의미:
- 서비스나 모듈이 어느 업무 경계에 속하는지 표현하는 enum입니다.

## 3. `ServiceDescriptor`

필드:
- `serviceName`
- `context`
- `description`

의미:
- 서비스 메타정보를 한 번에 표현하는 record입니다.

## 4. `Masked`

속성:
- `pattern`

기본값:
- `DEFAULT`

적용 대상:
- `FIELD`
- `METHOD`

의미:
- 민감정보 필드에 어떤 마스킹 규칙을 쓸지 표시하는 어노테이션입니다.

## 5. `MaskingSerializer`

역할:
- `JsonSerializer<String>`
- `ContextualSerializer`

핵심 메서드:
- `serialize(...)`
- `createContextual(...)`

지원 규칙:
- `REG_NO`
- `ACCOUNT`
- `EMAIL`
- 기본 마스킹

## 6. 읽을 때 중요한 점

- 이 모듈은 DB 스키마가 아니라 코드 레벨 공통 계약 모음입니다.
- 실제 마스킹 동작 여부는 사용하는 쪽의 Jackson 직렬화 설정과 필드 어노테이션에 달려 있습니다.

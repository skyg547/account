# Shared-Kernel Schema

## 1. 포함 타입 및 구조

DB 테이블 스키마가 아니라, 헥사고날 아키텍처의 포트(Port)들이 통신하기 위해 공유하는 코드 레벨의 **타입(Type)** 스키마입니다.

```mermaid
flowchart TD
    A[shared-kernel]
    A --> B[BoundedContext enum]
    A --> C[ServiceCapability enum]
    A --> D[ServiceDescriptor record]
    A --> E[DiscoverableService interface]
    A --> F[ServiceDiscoveryRegistry interface]
    A --> G[Masked annotation]
    A --> H[MaskingSerializer]
    A --> I[Common ID Types (추가 권장)]
```

## 2. `BoundedContext`

MSA(Microservices Architecture) 환경에서 각 독립된 도메인 경계를 정의합니다.
값: `MASTER_DATA`, `GOVERNANCE`, `JOURNAL_LEDGER`, `LOAN`, `REPORTING` 등

## 3. `ServiceCapability`

서비스가 포트(Port)를 통해 외부 어댑터에 제공하는 계약/기능의 종류입니다.
값: `SOURCE_DOCUMENT_LOOKUP`, `JOURNAL_POSTING`, `BUDGET_CONTROL` 등

## 4. `ServiceDescriptor`

Docker 및 MSA 환경에서 서비스 디스커버리에 사용되는 서비스의 메타정보(명함)입니다.
필드: `serviceName`, `context`, `capabilities`, `description`

## 5. `DiscoverableService` 및 `ServiceDiscoveryRegistry`

- **의미**: 런타임에 여러 컨테이너 인스턴스들이 자신의 존재를 알리고(Discoverable), 필요한 다른 서비스를 Capability/Context 기준으로 찾을 수 있게 해주는 공통 레지스트리 인터페이스입니다.

## 6. `Masked` 및 `MaskingSerializer`

보안 규칙 준수를 위한 어노테이션과 직렬화 도구입니다.

- 속성: `pattern` (REG_NO, ACCOUNT, EMAIL 등)
- 역할: API로 나가는 JSON 문자열에서 주민번호나 계좌번호를 마스킹 처리하여 Outbound Adapter의 보안을 책임집니다.

## 7. 아키텍처 핵심 원칙과 연결 (ID 기반 참조)

- **ID 기반 참조 지원**: `shared-kernel`은 다른 모듈의 JPA 엔티티를 직접 가져다 쓰지 못하도록 돕는 역할도 해야 합니다. `AccountId`, `LedgerId` 같은 범용적인 값 객체(Value Object)나 식별자 인터페이스를 여기에 두면, 각 모듈이 의존성 없이 ID만 주고받을 수 있게 됩니다.
- 모듈의 의존성은 언제나 `shared-kernel`을 향해야 하며, `shared-kernel`이 특정 비즈니스 모듈(예: `reporting`)을 의존하는 역방향 참조는 절대 불가합니다.

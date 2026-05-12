# Shared-Kernel Module Docs

`shared-kernel` 모듈은 여러 마이크로서비스가 공통으로 의존하는 타입, 직렬화 규칙, 유틸리티를 제공합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

**Q. shared-kernel은 무엇인가요?**
모든 모듈이 공통으로 사용하는 '아주 작은 공용 부품 상자' 또는 'DNA'라고 보면 됩니다.
각 모듈이 같은 enum, 같은 어노테이션, 같은 직렬화 규칙을 제각각 만들면 중복이 생기고 유지보수가 어려워지므로, 최소한의 공용 요소만 이곳에 둡니다.

**현재 포함된 주요 부품:**
- `BoundedContext`: 서비스가 어느 업무 그룹(예: `MASTER_DATA`, `JOURNAL_LEDGER`)에 속하는가?
- `ServiceCapability`: 서비스가 무엇을 제공하는가? (예: `JOURNAL_POSTING`)
- `ServiceDescriptor`: 서비스를 소개하는 명함 구조.
- `@Masked`: 민감정보(이메일, 계좌번호 등) 가리기 규칙.

> ⚠️ **주의:** 이 모듈은 단독으로 실행되는 서버(Spring Boot 애플리케이션)가 아니므로, `docker-compose`나 컨테이너 실행 대상이 아닙니다. 비즈니스 로직(돈 계산 등)도 이곳에 포함되어선 안 됩니다.

---

## 2. 🔄 처리 흐름 (Process Flow)

`shared-kernel`은 직접 비즈니스를 처리하지 않으며, 타 모듈이 같은 언어와 규칙을 공유하도록 돕습니다.

### 서비스 메타정보 흐름
1. **모듈 서비스** -> `BoundedContext` 선택 -> `ServiceCapability` 선택 -> `ServiceDescriptor` 생성 -> `ServiceDiscoveryRegistry` 등록
2. 이를 통해 중앙에서 어느 모듈이 어떤 능력을 가지고 있는지 식별할 수 있습니다.

### 데이터 마스킹 흐름 (Jackson Serializer)
1. 엔티티/DTO 필드에 `@Masked(pattern="EMAIL")` 부착.
2. JSON 응답 직렬화 시 `MaskingSerializer`가 개입하여 패턴에 맞게 가림 처리.
3. 예: `abc@example.com` -> `ab****@example.com`

---

## 3. 💾 데이터 스키마 및 핵심 타입 (Schema)

이 모듈은 DB 스키마가 아니라 **"코드 레벨 공통 계약 모음"**입니다.

- **BoundedContext (Enum):** `MASTER_DATA`, `GOVERNANCE`, `JOURNAL_LEDGER`, `LOAN` 등.
- **ServiceCapability (Enum):** `SOURCE_DOCUMENT_LOOKUP`, `JOURNAL_POSTING`, `BUDGET_CONTROL` 등.
- **ServiceDescriptor (Record):** `serviceName`, `context`, `capabilities`, `description`
- **Masked (Annotation):** `pattern` 속성 지원 (`REG_NO`, `ACCOUNT`, `EMAIL`, `DEFAULT`).

---

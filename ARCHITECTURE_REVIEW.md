# 전사 아키텍처(DDD & Hexagonal) 점검 결과 및 개선 가이드

본 문서는 프로젝트 내 모든 모듈의 DDD 및 헥사고날 아키텍처 준수 현황을 점검한 결과와 향후 개선이 필요한 사항을 정리한 가이드입니다.

---

## 1. 종합 점검 결과
현재 프로젝트는 **헥사고날 아키텍처의 패키지 구조**와 **도메인 중심의 모듈 설계**를 지향하고 있으나, 모듈별로 성숙도에 차이가 있으며 특히 **Rich Domain Model(DDD)** 측면에서 개선의 여지가 많습니다.

### ✅ 준수 사항 (Good)
- **패키지 분리:** `adapter`, `application.port`, `application.service`, `domain` 등의 구조를 통해 물리적 계층 분리 시도.
*   **Port/Adapter 패턴:** 외부 시스템(DB, Web)과의 결합도를 낮추기 위한 인터페이스(Port) 사용.
*   **핵심 도메인 응집:** `journal-ledger`, `closing` 등 일부 모듈에서 핵심 비즈니스 로직을 도메인 레이어에 집중.

### ⚠️ 개선 필요 사항 (Issues)
- **도메인 엔티티의 빈약함 (Anemic Domain Model):** 대부분의 엔티티가 Getter/Setter 위주로 구성되어 있으며, 비즈니스 로직이 서비스 레이어에 산재함.
- **포트(Port)의 추상화 부족:** 일부 포트 인터페이스가 구체적인 기술(JPA)이나 DTO에 의존하는 경향이 있음.
- **모듈 간 직접 참조:** 엔티티 간 직접 참조(특히 타 모듈 엔티티)로 인해 모듈 간 결합도가 높음.
- **일관성 없는 패키지 네이밍:** 모듈마다 `in/out` 패키지 사용 여부나 네이밍 규칙이 상이함.

---

## 2. 모듈별 세부 개선 방안

### [journal-ledger] - 성숙도: 상
- **현황:** 가장 모범적인 DDD 구조를 갖추고 있음. JavaDoc을 통한 비즈니스 설명이 우수함.
- **개선 방안:**
    - `JournalEntry` 내의 복잡한 상태 전이 로직을 도메인 메서드로 더 적극적으로 이동.
    - 역분개(Reversal) 로직을 도메인 서비스가 아닌 엔티티 내부 팩토리 메서드로 구현.

### [closing] - 성숙도: 중상
- **현황:** 최근 헥사고날 전환 작업을 통해 구조가 정돈됨. 검증 로직이 서비스에 집중되어 있음.
- **개선 방안:**
    - `ClosingCalendar`나 `ClosingTask` 내부에 상태 변경 가능 여부를 확인하는 자가 검증 로직(Self-Validation) 추가.
    - 서비스에 있는 "모든 태스크 완료 확인" 로직을 `ClosingCalendar` 도메인 객체의 책임으로 일부 전이.

### [tax] - 성숙도: 중
- **현황:** 단순 CRUD 위주. `validateAmounts` 등 기본적인 검증 로직은 포함되어 있으나 상태 관리가 부족함.
- **개선 방안:**
    - `TaxInvoice` 발행 시의 비즈니스 규칙(금액 정합성 등)을 생성자나 정적 팩토리 메서드에서 강제하도록 변경.
    - `JournalEntry`와의 연동을 포트(`JournalPostingPort`)를 통해서만 수행하도록 엄격히 제한.

### [expenditure-resolution / payable] - 성숙도: 중하
- **현황:** 지출 결의서(`ExpenditureResolution`) 엔티티가 지나치게 많은 타 모듈 엔티티(`Department`, `LeaseContract`, `JournalEntry`)를 직접 참조함.
- **개선 방안:**
    - **ID 기반 참조로 전환:** 타 모듈 엔티티를 객체로 참조하지 말고 `Long departmentId`와 같이 ID로 참조하여 모듈 독립성 확보.
    - 결의서 승인 프로세스(`approve`)의 복잡한 로직을 서비스에서 도메인 메서드로 이전.

### [receivable / income] - 성숙도: 중하
- **현황:** 패키지 구조가 `income`과 `receivable`로 혼용되어 있으며, 상태 전이 로직이 빈약함.
- **개선 방안:**
    - 패키지 경로를 전사 표준에 맞춰 재정의 (예: `com.ho.account.receivable...`).
    - 매출 채권의 연령 분석(Aging) 로직을 도메인 서비스로 도출하여 DDD 색채 강화.

---

## 3. 전사 아키텍처 준수 가이드라인 (Standard)

### 1) Rich Domain Model (풍부한 도메인 모델)
- **규칙:** Setter 사용을 지양하고, 비즈니스 의미가 담긴 메서드를 제공하라.
- **예시:** `setStatus(PAID)` 대신 `pay(BigDecimal amount)` 메서드를 사용하여 잔액 차감과 상태 변경을 동시에 처리.

### 2) 모듈 간 독립성 (Independence)
- **규칙:** 타 모듈의 엔티티를 JPA 연관관계(`@ManyToOne` 등)로 직접 맺지 마라.
- **방법:** `Port`를 통해 필요한 정보를 조회하거나, 단순 ID 값만 저장하여 물리적/논리적 결합도를 제거하라.

### 3) Port/Adapter 네이밍 표준
- **Inbound Port:** `...UseCase` (예: `ClosingUseCase`)
- **Outbound Port:** `...Port` (예: `JournalPostingPort`, `FiscalPeriodPersistencePort`)
- **Adapter:** `...Adapter` (예: `MonolithJournalQueryAdapter`, `TaxPersistenceAdapter`)

### 4) 금액 계산 (Precision)
- **규칙:** 모든 금액 연산은 도메인 엔티티 내부에서 `BigDecimal`을 사용하며, 반올림 정책은 도메인 상수로 정의한다.

---
**작성자:** [기획/팀장/모델러]
**작성일:** 2026-05-04

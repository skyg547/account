# 🏛️ K-Bank 차세대 재무 시스템 아키텍처 명세서 (Architecture Guide)

본 문서는 `account` 저장소의 차세대 재무/회계 시스템에 대한 **통합 아키텍처 뷰**를 제공하는 마스터 문서입니다.
새로 합류한 개발자나 AI 에이전트는 본 문서의 원칙을 최우선으로 숙지해야 합니다.

---

## 1. 시스템 컨텍스트 (System Context)

본 시스템은 은행의 핵심 재무/회계 엔진으로서, 현업(여신/수신/외환) 시스템에서 발생한 거래를 수집하여 
**총계정원장(GL)**과 **보조원장(SL)**을 생성하고, 최종적으로 **재무제표(결산) 및 규제 보고서**를 산출하는 것을 목표로 합니다.

### 1.1 하이레벨 아키텍처 (High-level Architecture)
* **Frontend:** Next.js 15 기반, React Query 및 Zustand 적용 (자세한 구조는 [Frontend Architecture](./frontend-architecture.md) 참조)
* **Backend:** Spring Boot 3.4 (Java 21), Hexagonal Architecture 기반의 멀티 모듈 구조
* **Infrastructure:** PostgreSQL(원장/마스터), H2(테스트/배치), Kafka(이벤트 스트리밍), ELK(감사 로그)

---

## 2. 백엔드 아키텍처 원칙 (Hexagonal & MSA)

기존의 거대한 단일 모놀리스(Monolith)에서 **도메인 단위의 MSA(Microservices) 지향 멀티 모듈**로 분리되어 있습니다.

### 2.1 계층(Layer) 분리 원칙
* `adapter`: 외부(Web/UI, DB, Message Broker)와의 통신만 담당.
* `application`: `port` 인터페이스를 정의하고, 비즈니스 흐름(UseCase)을 제어. **순수 도메인과 인프라의 격리 경계.**
* `domain`: JPA나 Spring 의존성이 없는 순수 자바 객체(엔티티, 밸류, 정책). 핵심 비즈니스 로직 존재.

### 2.2 모듈 간 의존성 (Dependency Flow)
모든 도메인 모듈은 하위 공통 모듈에만 의존해야 하며, **동위 도메인 모듈 간 직접 의존(JPA Entity 참조 등)은 엄격히 금지**됩니다.

```text
[ Domain Modules (loan, closing, payable 등) ] 
       │ 
       ▼ 
[ Contracts (포트 인터페이스, DTO) ] 
       │ 
       ▼ 
[ Shared-Kernel / Risk-Common (공통 유틸/예외) ]
```
* **의존성 룰:** 타 도메인 데이터가 필요할 경우 `contracts` 모듈에 정의된 `QueryPort`를 통해서만 접근. (예: `JournalQueryPort`)
* **핵심 기반 모듈:** `master-data`와 `governance`는 모든 모듈의 기초가 됩니다. (자세한 분리 상태는 [Dependency Split Status](./archive/dependency-split-status.md) 참고)

---

## 3. 핵심 데이터 및 설계 원칙

### 3.1 라인리지 (Lineage) 보장
모든 집계 데이터(보고서, 마트)는 원천 거래(여신/수신 이벤트 등)까지 **Drill-through(역추적)**가 가능해야 합니다. 
(예: `Reporting` 모듈의 주석 마트 데이터는 `Journal Detail` 테이블의 계정코드 맵핑을 통해 1:N 추적)

### 3.2 SCD2 (Slowly Changing Dimension Type 2) 
모든 마스터 데이터(계정과목, 부서, 환율, 상품 등)는 `valid_from`, `valid_to` 컬럼을 필수로 가져 과거 특정 시점의 데이터 상태를 재현할 수 있어야 합니다.

### 3.3 정밀도 (Precision) 및 데이터 타입
* **금액 데이터:** 부동소수점 오차 방지를 위해 **반드시 `BigDecimal`을 사용**합니다. (Double 사용 금지)
* **논리 삭제:** 트랜잭션 데이터의 물리적 삭제(`DELETE`)는 금지되며, 수정이 필요한 경우 **역분개(Reversal) 전표**를 발행해야 합니다.

---

## 4. 디렉토리 구조 및 분산 문서화 정책

전체 모듈이 거대해짐에 따라 **1-Directory 1-README 원칙**을 강제합니다.

### 4.1 루트 디렉토리 구조
* `app/`: 전체 애플리케이션 실행 진입점 및 환경 설정(yml).
* `contracts/`: 도메인 간 통신을 위한 DTO 및 Port 인터페이스.
* `master-data/`: 계정, 부서, 환율 등 시스템 공통 기준 정보.
* `{domain-module}/`: (예: `journal-ledger`, `closing`, `ecl`, `account-mart`) 개별 비즈니스 도메인.

### 4.2 모듈별 문서화 규칙
* 각 도메인 모듈(예: `ecl/`) 하위에는 반드시 `docs/` 폴더 또는 `README.md`가 존재해야 합니다.
* 모듈 문서는 해당 도메인의 **1) 비즈니스 목적, 2) 테이블 스키마(`schema.sql`), 3) 외부 연동 포트**를 초보자도 이해할 수 있게(🐣마크 사용 등) 설명해야 합니다.

---

## 5. 상세 참조 문서 (Reference)

각론에 해당하는 상세 명세는 아래의 하이퍼링크를 참조하십시오.

* 🖥️ **[Frontend Architecture](./frontend-architecture.md):** Next.js 15 기반 화면 및 상태 관리 아키텍처
* ⚙️ **[Infrastructure Guide](./infrastructure-guide.md):** Docker 기반 실행 및 ELK, Kafka 운영 가이드
* 🗄️ **[App Schema Guide](./schema.md):** 최상위 App 설정 및 환경별 Profile 관리 원칙
* 📜 **[Development Policies](./principles_and_policies.md):** 코드 컨벤션, 네이밍 규칙, Anti-Skeleton 정책 상세
* 🗃️ **[Archive](./archive):** 과거 모놀리식 전환기 및 2026-04 기준 의존성 분리 히스토리 보관소
# ⚙️ 신용 리스크 코어 모듈 (credit-core)

> **Role**: [리스크 모델러]
> **Metaphor**: [수식 계산기 및 핵심 장부 관리]

## 💡 초보자를 위한 개념 설명
본 모듈은 리스크 시스템의 **'두뇌와 심장'**입니다.
- **수식 계산기**: 국제 금융 규제, IFRS 9와 같은 매우 복잡한 금융 수학 공식들이 이 안에 들어있습니다. 
- **핵심 장부**: 은행의 가장 중요한 자산인 대출, 담보 정보의 원천 모델(Domain Model)을 관리합니다.
- 돈을 다루는 곳이므로 `0.000001`의 오차도 허용하지 않기 위해 `BigDecimal`을 사용하여 정밀하게 계산합니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 아키텍처의 가장 중심인 **Domain & Application Layer**입니다.
- **Port (Interface)**: 외부(API, Batch)에서 자신을 부를 수 있는 통로(`Inbound Port`)와 자신이 외부(DB, 타 시스템)를 부르기 위한 통로(`Outbound Port`)를 정의합니다.
- **Pure Logic**: 프레임워크나 외부 기술에 의존하지 않는 순수한 비즈니스 로직을 지향합니다.
- 외부 기술이 바뀌어도(예: Oracle -> PostgreSQL) 이 안의 리스크 산식은 절대 변하지 않습니다.

---

## 🏗️ 업무 단계별 구조 (Business Stages)

시스템은 '단계별 파이프라인' 구조로 설계되어 있어, 기술 부채를 최소화하고 신규 리스크 지표 추가 시의 유연성을 보장합니다.

| 단계 | 패키지 | 업무 설명 (Business Context) | 주요 컴포넌트 |
|:---:|:---|:---|:---|
| **Step 01** | `stage01_sync` | **데이터 정합성 및 통합**: ODS에서 넘어온 원천 데이터를 정제하고 리스크 산출용 통합 포지션을 생성합니다. | `RiskDataQualityService`, `IntegratedRiskPositionRepository` |
| **Step 02** | `stage02_collateral` | **담보 및 신용위험완화(CRM)**: 고객별 담보 배분 최적화(Waterfall 기법) 및 부동산 특화 담보가치를 산출합니다. | `CollateralAllocationService`, `CrCollateral` |
| **Step 03** | `stage03_staging` | **IFRS 9 스테이징**: 연체일수, 외부 등급, 조기경보 신호를 기반으로 자산의 건전성 단계(Stage 1~3)를 판정합니다. | `StagingService`, `LifetimePdService` |
| **Step 04** | `stage04_engine` | **리스크 산출 엔진**: 국제 금융 규제 수식을 바탕으로 EAD, PD, LGD, RWA, ECL을 최종 산출합니다. | `CreditRiskService`, `CreditRiskCalculator`, `EadCalculator` |
| **Step 05** | `stage05_report` | **분석 및 보고**: 산출 결과를 바탕으로 집중리스크(HHI), 스트레스 테스트, 대외 보고용 요약을 생성합니다. | `RegulatoryReportService`, `StressTestService`, `ConcentrationRiskService` |

## 📁 공통 도메인 모델 (Domain Models)

*   `domain.master`: 고객(Customer), 상품(Product), 등급(Grade) 등 기준 정보 관리.
*   `domain.account`: 기초 자산인 계좌(Account) 정보 및 원장 이력 관리.

## 💡 초보 개발자를 위한 가이드
1.  **"새로운 규제 요건이 생기면 어디를 보나요 "**
    *   산출 공식의 변경은 주로 `stage04_engine`의 Calculator 클래스들을 수정합니다.
    *   입력 데이터의 변경은 `domain` 패키지의 엔티티와 `stage01_sync`를 확인하세요.
2.  **"Stage 1~3이 무엇인가요 "**
    *   IFRS 9 회계 기준에 따른 부도 가능성 분류입니다. 숫자가 클수록 위험하며(3=이미 연체됨), 이에 따라 대손충당금(ECL)을 쌓는 방식이 달라집니다.
3.  **"CRM 배분은 왜 중요한가요 "**
    *   은행이 가진 담보를 어떤 대출에 먼저 할당하느냐에 따라 은행 전체의 위험가중자산(RWA) 수치가 달라지며, 이는 은행의 자본 적정성에 직결됩니다.

## 🧪 테스트 구조
- `src/test/java/.../domain/calculator`
  - 순수 수학/규제 공식 검증 테스트를 둡니다.
  - Spring 컨텍스트 없이 계산 결과 자체를 검증하는 위치입니다.
- `src/test/java/.../application/service/calculation`
  - EAD, PD, LGD, ECL, RWA 본산출 유즈케이스 테스트를 둡니다.
- `src/test/java/.../application/service/crm`
  - 담보배분, 규제계약, CRM 최적화처럼 담보 중심 협업 로직 테스트를 둡니다.
- `src/test/java/.../application/service/monitoring`
  - 집중도, 모니터링, 보고 계열 서비스 테스트를 둡니다.
- `src/test/java/.../application/service`
  - 특정 하위 도메인으로 분리하지 않는 교차 애플리케이션 서비스 테스트를 둡니다.

초보자 관점에서 보면, 테스트 패키지 구조도 운영 코드와 동일하게
"어떤 유즈케이스를 검증하는가"가 드러나야 DDD/헥사고날 경계를 유지하기 쉽습니다.

---
** Enterprise Risk Management System v2.0**

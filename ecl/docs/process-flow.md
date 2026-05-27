# 🌊 신용 리스크 8단계 규제 오케스트레이션 파이프라인

본 문서는 `credit-risk-service`의 핵심인 `creditRiskCalculationJob`의 8단계 처리 과정을 상세히 설명합니다.

---

## 💡 [초보자를 위한 개념 설명] 오케스트레이션이란?
음악에서 오케스트라가 지휘자의 지휘에 맞춰 여러 악기가 조화롭게 연주하듯, 리스크 시스템의 **오케스트레이션**은 복잡한 계산 단계들을 순서에 맞게, 그리고 데이터의 정합성을 유지하며 차례대로 실행하는 과정을 말합니다. 하나라도 어긋나면 은행 전체의 자본 산출값이 틀릴 수 있기 때문에 매우 정교한 흐름 제어가 필요합니다.

---

## 🚀 단계별 상세 프로세스

### Stage 1: Sync Master (규제 마스터 동기화)
- **목적**: 산출에 필요한 기초 정보(기준금리, 환율, 영업일 등)를 외부 시스템(ODS) 혹은 기준 관리 테이블로부터 가져옵니다.
- **데이터 흐름**: `External Source` → `CrRegulatoryParameter`

### Stage 2: Regulatory Contract (계약/담보 관계 생성)
- **목적**: 단순한 여신 계약과 담보 데이터를 규제 관점에서 연결합니다. 어떤 담보가 어떤 대출을 보증할 수 있는지 유효성을 검증합니다.
- **데이터 흐름**: `cr_accounts`, `cr_collaterals` → `cr_account_collaterals` (Initial)

### Stage 3: Credit Monitoring (신용 모니터링)
- **목적**: 고객의 부도 여부, 연체 일수, 조기경보 신호 등을 분석하여 IFRS 9 스테이징(Stage 1/2/3)을 판정합니다.
- **핵심 로직**: `StagingService`를 통해 유의적 신용위험 증가(SICR)를 감지합니다.

### Stage 4: Collateral Allocation (일반 담보 배분)
- **목적**: 하나의 담보가 여러 대출에 걸쳐 있을 때, **"어느 대출에 얼마를 배분해야 은행 전체의 자본(RWA)이 가장 적게 들까?"**를 고민하는 단계입니다.
- **알고리즘**: 워터폴(Waterfall) 방식을 사용하여 높은 위험 가중치 계좌부터 우선 배분합니다.

### Stage 5: Special Collateral (부동산 특화 담보 배분)
- **목적**: 아파트, 상가 등 부동산 담보의 경우 KB 시세, LTV 한도, 선순위 채권액 등을 고려하여 더 정밀하게 가용 담보 가액을 산출하고 배분합니다.
- **핵심 로직**: `ApartmentCollateralService`에서 시세 대비 담보 인정액을 계산합니다.

### Stage 6: Calculation Main (RWA/ECL 본산출)
- **목적**: 준비된 모든 데이터를 바탕으로 국제 금융 규제 산식을 돌려 최종 결과값을 뽑아냅니다.
- **산출 항목**:
    - **EAD**: 부도 시 노출액 (CCF 및 담보 차감 반영)
    - **PD/LGD**: 부도 확률 및 손실률 (IRB 마스터 기반)
    - **ECL**: 기대 손실 (시나리오별 가중 평균)
    - **RWA**: 위험 가중 자산 (SA 및 IRB 방식 병행)

### Stage 7: Consolidation (월통합 자산 생성)
- **목적**: 산출된 건별 데이터를 차주 단위(Global ID), 그룹 단위로 통합하여 경영진 보고 및 공시용 마트를 생성합니다.
- **데이터 흐름**: `cr_risk_results` → `Consolidated Asset Mart`

### Stage 8: Concentration Analysis (집중도 분석)
- **목적**: 은행의 대출이 특정 산업이나 특정 기업군에 너무 쏠려 있지 않은지 측정합니다.
- **알고리즘**: HHI(Herfindahl-Hirschman Index) 지수를 산출하여 포트폴리오 다변화 수준을 체크합니다.

---
*Last Updated: 2026-04-15 by Antigravity Modern Finance Team*

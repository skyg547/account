# 📊 리스크 데이터 마트(RDM) 상세 기술 명세서 - v5.0 고도화

> **"파편화된 은행 데이터를 리스크라는 렌즈로 정제하는 중앙 데이터 공장"**

---

## 1. 업무 관점 (Business Perspective) 💼

### 💡 왜 '마트'가 필요한가요? (초보자용)
은행 안에는 수많은 부서가 있고 각자 자기만의 방식으로 데이터를 관리합니다. 
리스크 관리자는 이 모든 정보가 한꺼번에 필요합니다. **데이터 마트**는 각 팀에 흩어진 데이터를 가져와서 리스크 산출용 통합 원장(`dim_integrated_position_master`)으로 만들어주는 역할을 합니다.

### v5.0 고도화: 금리리스크(IRRBB) 정밀 데이터 보완
정확한 리스크 산출을 위해 다음 비즈니스 항목들이 추가되었습니다.
1.  **상환 스케줄 상세**: 원금 거치 기간 및 상환 주기를 통해 미래 현금흐름의 정확한 시점을 파악합니다.
2.  **금리 상/하한선(Cap/Floor)**: 시장 금리가 급변하더라도 계약상 보호받는 금리 경계치를 반영하여 ΔNII(이자이익 변동)의 과대 산출을 방지합니다.
3.  **행동 모델링 이력**: NMD(핵심예금) 및 CPR(조기상환) 분석을 위해 3~5년치 과거 데이터를 축적합니다.
4.  **참조 인덱스 매핑**: 통화별(KRW, USD 등)로 적절한 시장 금리 곡선을 적용하기 위한 매핑 코드를 확보합니다.

---

## 2. IT 관점 (Technical Perspective) 🛠️

### 데이터 파이프라인 (ETL)
RDM은 **Extract(추출) -> Transform(변환) -> Load(적재)**의 단계를 거칩니다.
*   **ODS (Operational Data Store)**: 원천 데이터 임시 저장소.
*   **CDM (Common Data Model)**: 리스크 표준화 데이터 (`IntegratedRiskPosition`).

### 주요 고도화 필드 매핑
| ODS 테이블 | 추가 필드 | 용도 (IT 가이드) |
| :--- | :--- | :--- |
| `ods_acc_ledger` | `grace_period`, `repayment_freq` | 현금흐름 생성기(Cashflow Generator) 입력값 |
| `ods_rate_info` | `int_rate_cap`, `int_rate_floor` | 금리 시나리오 적용 시 Clipping 로직에 사용 |
| `ods_behavioral_history` | (신규 테이블) | 시계열 분석 및 통계 모형 훈련용 데이터셋 |

---

## 3. 배치 잡(Batch Job) 가이드 🚀

### 1) `integratedPositionEtlJob` (기존)
*   **기능**: 원천 데이터를 통합 원장(CDM)으로 변환 및 적재.
*   **고도화**: `IntegratedPositionProcessor`에서 상환 및 금리 상/하한 정보를 결합하여 적재하도록 로직이 강화되었습니다.

### 2) `behavioralHistoryLoadJob` (신규)
*   **기능**: 과거 잔액 및 이벤트(상환/해지) 데이터를 시계열로 축적.
*   **주기**: 매영업일 또는 매월 말일 수행.

---

## 4. 운영자를 위한 팁 (Self-Healing) 🚨
*   **데이터 불일치**: `ods_reconcile_results`를 통해 원천과 마트의 합계액 차이를 모니터링하세요.
*   **금리 조건 누락**: `ods_rate_info`에 데이터가 없으면 기본 금리만 적용되어 리스크가 과다하게 산출될 수 있으니 주기적인 품질 검사(DQ)가 필요합니다.

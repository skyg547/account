# Risk Management Module (RDM)

## 1. 개요 (Overview)
본 모듈은 Basel III 규제 준수를 위한 **리스크 데이터 마트(Risk Data Mart, RDM)**와 **위험가중자산(Risk-Weighted Assets, RWA)** 산출 로직을 담당합니다. 
전사적인 신용, 시장, 운영 리스크 데이터를 통합하여 규제 보고 및 내부 리스크 관리에 활용됩니다.

## 2. 주요 개념 설명 (for Beginners)
* **RDM (Risk Data Mart):** 리스크 산출에 필요한 모든 데이터를 한곳에 모아둔 저장소입니다. 원천 시스템(계정계)의 복잡한 데이터를 리스크 관점에서 재구성합니다.
* **RWA (Risk-Weighted Assets, 위험가중자산):** 은행이 보유한 자산(대출 등)에 자산별 위험도를 곱한 금액입니다. 위험도가 높을수록 더 많은 자기자본을 보유해야 합니다.
* **PD (Probability of Default, 부도확률):** 차주가 일정 기간 내에 부도를 낼 확률입니다.
* **LGD (Loss Given Default, 부도시 손실률):** 부도가 발생했을 때 실제 회수하지 못하고 잃게 되는 금액의 비율입니다.
* **EAD (Exposure at Default, 부도시 익스포저):** 부도가 발생하는 시점에 은행이 노출되어 있는 총금액입니다.

## 3. 핵심 엔티티 (Core Entities)
* `CreditRiskExposure`: 개별 차주 및 상품별 신용리스크 노출 현황을 관리합니다. (PD, LGD, EAD 포함)
* `RiskWeightedAsset`: 산출된 RWA 결과를 저장하며, 표준방법(SA)과 내부등급법(IRB) 결과를 모두 관리할 수 있습니다.
* `RiskParameter`: 규제기관에서 고시하거나 내부 모형을 통해 산출된 리스크 파라미터(위험가중치 등)를 관리합니다.

## 4. 모듈 의존성 (Dependencies)
* **Input:** `loan` (대출 계약 정보), `master-data` (차주 정보, 통화 정보)
* **Output:** `reporting` (대외 규제 보고서)

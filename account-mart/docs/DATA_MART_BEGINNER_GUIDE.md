# 🗄️ 리스크 데이터 마트(RDM) 서비스 개발 및 운영 가이드

> **"은행 곳곳에 흩어진 파편화된 데이터들을 리스크 산출용 정제 데이터로 바꾸는 공장"** 입니다.

## 1. 업무 관점 (Business Perspective) 💼

### 💡 데이터 마트란 무엇인가? (초보자용)
리스크를 계산하려면 대출 잔액, 고객 신용등급, 담보 시세 등 수많은 정보가 필요합니다. 하지만 이 정보들은 원래 주담대 시스템, 신용카드 시스템, 재무 시스템 등에 제각각 흩어져 있습니다. 
데이터 마트는 이 복잡한 원천 데이터들을 한데 모아 **"리스크 산출에 딱 맞는 규격"**으로 정제하고 검증하는 역할을 합니다. 마트 데이터가 틀리면 리스크 결과도 틀리기 때문에, **'진실의 원천(Source of Truth)'**이라 불리는 매우 중요한 곳입니다.

### 핵심 개념
1.  **ETL (Extract, Transform, Load)**: 원천 시스템에서 데이터를 뽑아(E), 변환하고(T), 마트에 넣는(L) 과정입니다.
2.  **Reconciliation (정합성 검증)**: "마트에 들어온 대출 합계가 실제 회계 장부랑 똑같나?"를 맞춰보는 과정입니다. (1원이라도 틀리면 경고가 뜹니다.)
3.  **Position Master**: 모든 리스크 산출의 기초가 되는 통합 계좌 원장입니다.

---

## 2. IT 관점 (Technical Perspective) 🛠️

### 시스템 아키텍처
*   **Batch Engine**: Spring Batch 기반의 대용량 ETL 파이프라인.
*   **External Integration**: KAP(한국자산평가) 등 외부 기관의 부도율 및 가산금리 데이터를 API/파일로 연동.
*   **Data Integrity**: JPA 엔티티와 SQL 초기화 스크립트를 통한 스키마 관리.

### 핵심 고도화 포인트
1.  **KAP 등급 매핑**: 외부 등급(AAA, AA+ 등)을 내부 리스크 산출 코드(K01, K02 등)로 변환하는 `KapRatingGradeResolver`를 도입했습니다.
2.  **통합 리스크 뷰**: `dim_integrated_position_master` 테이블을 통해 신용과 금리 리스크가 동일한 기초 데이터를 바라보도록 단일화했습니다.
3.  **Advanced Validation**: 데이터 누락이나 타입 불일치를 사전에 걸러내는 **Risk Data Quality** 체크 로직이 포함되어 있습니다.

---

## 3. 주니어 개발자를 위한 실행 가이드 (How-to) 🚀

### 데이터 정합성 확인법
1.  `ods_balance_hist`에 원천 잔액 데이터를 넣습니다.
2.  `integratedPositionEtlJob` 배치를 실행합니다.
3.  `dim_integrated_position_master`에 정제된 데이터가 쌓였는지 확인합니다.
4.  **검증**: `ods_reconcile_results` 테이블을 조회하여 `diff_amt`가 0인지 확인하세요.

### 장애 대응 (Self-Healing)
*   **등급 매핑 실패**: 새로운 등급 코드가 외부에서 들어오면 `RatingGradeMaster` 테이블에 매핑 정보를 추가해야 합니다.
*   **메모리 부족**: 대규모 ETL 수행 시 `-Xmx` 옵션을 충분히 확보하거나, Chunk Size를 조절하세요.

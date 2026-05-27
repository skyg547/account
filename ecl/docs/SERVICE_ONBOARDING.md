# 🛠️ 신용 리스크 서비스 개발자 온보딩 가이드 (Service Onboarding)

> **[기획/팀장]:** 신용 리스크 시스템은 국제 금융 규제와 대용량 배치 프로세스가 결합된 정밀한 도메인입니다. 본 가이드는 신규 합류한 개발자가 시스템의 구조를 빠르게 이해하고 로컬 환경을 구축할 수 있도록 돕습니다.

---

## 1. 업무 관점 (Business Perspective) 💼

### 💡 리스크란 무엇인가? (초보자용)
"누구에게 얼마를 빌려줄 때, 만약 그 사람이 돈을 못 갚으면(부도) 우리 은행은 얼마나 타격을 입고, 비상금은 얼마나 쌓아야 할까?"를 계산하는 것입니다. 
- **PD (부도율)**: 망할 확률
- **LGD (손실률)**: 망했을 때 진짜 떼일 돈의 비율
- **RWA (위험가중자산)**: 최종 리스크 점수 (이 수치의 8% 이상을 현금으로 보유해야 함)

---

## 2. 필수 지식 자가 진단 (Prerequisites)

시스템에 코드를 기여하기 전, 다음 문서들을 먼저 학습하십시오.
- [ ] **핵심 개념**: `docs/CREDIT_RISK_CONCEPTS.md` (국제 금융 규제 및 3대 파라미터)
- [ ] **데이터 모델**: `docs/CREDIT_DATA_MODEL_SPEC.md` (RDM 및 원장 구조)
- [ ] **산출 프로세스**: `docs/CREDIT_RISK_PROCESS_FLOW.md` (8단계 배치 파이프라인)

---

## 3. 로컬 개발 환경 구성 (Local Setup)

### 3.1 DB 스키마 생성
로컬 PostgreSQL에 신용 리스크 전용 스키마를 생성해야 합니다.
- **스크마 파일**: `db/schema-cr.sql`
- **방법**: Docker Compose 기동 시 자동으로 반영되나, 수동 반영 필요 시 아래 명령 실행.
  ```bash
  psql -h localhost -U risk_user -d credit_risk_db -f db/schema-cr.sql
  ```

### 3.2 서비스 기동 순서
1.  `common` 빌드: `./gradlew :common:build`
2.  `discovery-service` 기동 (포트 8761)
3.  `credit-api` 기동 (포트 8081)

---

## 4. 핵심 코드 수정 포인트 (Development Hotspots)

### "새로운 가중치 매핑(SA RW)을 추가하고 싶어요"
- **위치**: `credit-core` 모듈의 `domain.service.SaRwMapper` 클래스.
- **주의**: DB(`cr_sa_rw_masters`) 설정과 연동되므로 매핑 키값이 일치하는지 확인하십시오.

### "내부등급법(IRB) 산식을 수정하고 싶어요"
- **위치**: `credit-core` 모듈의 `domain.calculator.CreditRiskCalculator` 클래스.
- **주의**: BigDecimal 기반의 정규분포 역함수 산식이 포함되어 있으므로, 수학적 정밀도 상실 없이 구현해야 합니다.

### "배치 단계를 조정하고 싶어요"
- **위치**: `credit-batch` 모듈의 `CreditRiskBatchConfig` 설정.
- **팁**: Step 간의 순차적 데이터 의존성(예: 담보배분 -> 본산출)을 유의하십시오.

---

## 5. 장애 대응 및 로그 분석 (Troubleshooting)

- **로그 확인**: 모든 서비스 로그는 `/app/logs` 또는 컨테이너 표준 출력으로 나옵니다.
- **산출 결과 누락**: `OdsDataQualityService` 로그를 확인하여, 원천 데이터가 DQ 규칙을 통과했는지 체크하십시오.
- **배치 중단**: Spring Batch의 `BATCH_STEP_EXECUTION` 테이블에서 실패 코드(`EXIT_CODE`)를 확인하십시오.

---
*Created by Antigravity - System Reliability & Onboarding Expert*

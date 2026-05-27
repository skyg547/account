# 🛡️ 신용 리스크 관리 서비스 (Credit Risk Service)

> **Architect's Vision**: "은행의 건전성을 지탱하는 가장 견고한 방패로서, IFRS 9 및 바젤 III/IV 규제를 완벽히 준수하여 정밀한 리스크 자본을 산출하는 엔진입니다."

본 서비스는 전사의 여신 포지션 데이터를 기반으로 **신용등급(PD)**, **부도 시 손실률(LGD)**, **부도 시 노출액(EAD)**을 결합하여, 회계적 **기대손실(ECL)**과 규제적 **위험가중자산(RWA)**을 산출합니다.

---

## 👥 담당 역할 (Roles)
- **[백엔드 개발자]**: 고성능 배치 파이프라인 구축, Hexagonal Architecture 기반의 도메인 보호, 외부 시스템(ODS, Eureka) 연동 및 API 설계.
- **[리스크 모델러]**: 바젤 규제 산식 구현, 최적화 알고리즘(`Simplex`) 설계, `BigDecimal` 기반의 정밀한 리스크 엔진 로직 검증.

---

## 🏗️ 서비스 아키텍처 및 모듈 구성

본 서비스는 **Hexagonal Architecture(Port/Adapter 분리)** 원칙을 준수하여, 핵심 리스크 산출 로직(Core)이 외부 기술(DB, Framework)의 변화에 오염되지 않도록 격리되어 있습니다.

*   **`credit-api` [고객 응대 창구]**: 외부 시스템 및 프론트엔드 대시보드를 위한 REST API 계층.
*   **`credit-batch` [대용량 데이터 공장 라인]**: Spring Batch 기반 대용량 산출 엔진. **야간 정산(EOB)**을 통해 전사 리스크 지표를 확정합니다.
*   **`credit-core` [수식 계산기 및 핵심 장부 관리]**: 신용 리스크의 핵심 도메인 로직. 필드 정밀도 유지를 위해 모든 계산은 `BigDecimal` 중심의 수치 산술을 수행합니다.

---

## 💡 [초보자를 위한 개념 설명] 이 엔진이 하는 일

금융 리스크 시스템은 주니어 개발자에게 "외계어"처럼 느껴질 수 있습니다. 핵심 개념을 비유를 통해 설명해 드립니다.

1.  **Batch (공장 라인)**: 수백만 명의 대출 데이터를 하나씩 처리하는 것은 불가능합니다. 마치 거대한 컨베이어 벨트(공장 라인)에 데이터를 올려두고, 정해진 순서대로 가공하여 결과물을 만들어내는 과정입니다.
2.  **Core (계산기 & 장부)**: 리스크 산출의 '심장'입니다. 복잡한 수학 공식(계산기)이 들어있고, 은행의 가장 소중한 자산 정보(장부)를 안전하게 관리합니다.
3.  **API (응대 창구)**: 공장에서 만들어진 리스크 결과물을 보고 싶어 하는 사람들에게 친절하게 정보를 보여주는 은행 창구와 같습니다.

---

1.  **EAD (Exposure At Default)란?**
    *   쉽게 말해 **"고객이 부도를 냈을 때 우리가 떼일 수 있는 총 금액"**입니다. 단순히 현재 빌려준 돈뿐만 아니라, 고객이 언제든 찾아갈 수 있는 마이너스 통장 한도 등도 고려합니다.
2.  **PD (Probability of Default)란?**
    *   **"이 고객이 1년 안에 돈을 안 갚고 도망갈 확률"**입니다. 은행 내부 등급(AAA, BB 등)에 따라 과거 통계치를 기반으로 결정됩니다.
3.  **LGD (Loss Given Default)란?**
    *   **"진짜 부도가 났을 때, 담보 다 팔고 나서도 결국 못 건지는 비율"**입니다. 부동산 담보가 든든하다면 LGD는 낮아지고(안전), 신용대출이라면 LGD는 높아집니다(위험).
4.  **CRM (Credit Risk Mitigation) 최적화란?**
    *   **"담보 돌려막기(?)의 수학적 예술"**입니다. 하나의 담보가 여러 대출을 보증할 때, 어떤 대출에 담보를 먼저 배분해야 은행 전체의 위험도(RWA)가 가장 낮아질지 수학적 최적해(`Simplex Algorithm`)를 찾아냅니다.

---

## 🚀 배치 프로세스 수행 가이드

### 0. 대손충당금 연동 상태

2026-05-27 기준으로 `ecl`에는 `closing`이 읽는 `allowance_summary`를 생성하는 최소 경로가 추가되어 있습니다.

- `MainReportingBatchConfig`는 ECL/RWA 산출 후 `allowanceSummaryStep`을 실행합니다.
- `AllowanceSummaryService`는 기준일의 완료된 ECL 결과를 회계 summary로 재생성합니다.
- 실제 대량 집계는 `JdbcAllowanceSummaryPersistenceAdapter`의 SQL bulk `INSERT ... SELECT`로 처리합니다.
- `allowance_account_mappings`에 상품/사업부/통화별 회계 계정 매핑이 없으면 기존 summary를 지우지 않고 실패합니다.
- 루트 `settings.gradle`에는 아직 `ecl` 모듈이 포함되어 있지 않아 저장소 통합 빌드 편입은 별도 후속 작업입니다.

### 1. DB 환경 준비
배치 프로세스를 돌려보기 전, 기초 마스터 데이터와 거래 데이터를 생성해야 합니다.
```bash
# 1. 스키마 생성 (테이블 및 파티션)
psql -U admin -d credit_risk_db -f db/schema-cr.sql

# 2. 샘플 데이터 삽입 (테스트용 차주, 계좌, 담보 데이터)
psql -U admin -d credit_risk_db -f db/data-cr.sql
```

### 2. 8단계 규제 오케스트레이션 파이프라인
`creditRiskCalculationJob`은 아래 8단계를 순차적으로 수행하여 최종 리스크 지표를 산출합니다. 상세 내용은 [process-flow.md](docs/process-flow.md)를 참고하세요.

1.  **Sync Master**: ODS/CDM으로부터 최신 규제 마스터(환율, 금리 등)를 동기화합니다.
2.  **Regulatory Contract**: 여신 계약과 담보 간의 규제상 관계를 유효성 있게 매핑합니다.
3.  **Credit Monitoring**: 차주의 부도 정보 및 조기경보를 기반으로 IFRS 9 스테이징을 확정합니다.
4.  **Collateral Allocation**: 자본 절감을 극대화하는 워터폴 알고리즘으로 일반 담보를 배분합니다.
5.  **Special Collateral**: 부동산 상세 속성(LTV, 선순위 등)을 반영하여 특화 담보를 배분합니다.
6.  **Calculation (Main)**: 바젤 III/IV 표준 및 내부등급법 수식으로 **EAD, RWA, ECL**을 본산출합니다.
7.  **Consolidation**: 산출 결과를 월통합 자산 관리 마트로 이관 및 생성합니다.
8.  **Concentration Analysis**: 산업별/차주별 집중 리스크(HHI 지수)를 자동 측정합니다.

### 3. 배치 실행 (Gradle)
```bash
# 특정 기준일(baseDate)에 대한 전체 리스크 산출 배치 실행
./gradlew :credit-risk-service:credit-batch:bootRun --args='--spring.batch.job.name=creditRiskCalculationJob baseDate=2026-04-15'
```

---

## 🛠️ 기술 사양 및 규제 준수

- **Java 21 / Spring Boot 3.4**: 최신 Java 기능을 활용한 고성능 연산 처리.
- **Apache Commons Math**: 대규모 행렬 연산 및 LP(Linear Programming) 최적화 알고리즘 구현.
- **IFRS 9 / Basel III/IV 준수**: 글로벌 표준 규제 산식(Standardized & IRB Approach) 완벽 구현.

---

## 🧪 품질 및 검증 (QA)

규제 대응 시스템의 핵심은 **"재현성"**입니다. 동일한 입력값이면 10년 뒤에도 동일한 리스크 값이 나와야 합니다.
- **RWA Validation**: 산출된 결과값이 규제 산식 결과와 일치하는지 `Mock` 데이터를 통한 상시 검증.
- **Memory Optimization**: 대용량 배치 처리 시 JVM OOM 방지를 위한 `Paging Reader` 및 `Chunk` 지향 도메인 설계.

---
*Created by Antigravity - Modern Financial Engineering Team*

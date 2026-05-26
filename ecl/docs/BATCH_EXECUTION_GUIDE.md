# 🚀 신용 리스크 산출 배치 실행 가이드 (Batch Execution Guide)

> **[기획/팀장]:** 신용 리스크 배치는 전사 데이터를 처리하는 핵심 프로세스입니다. 이 가이드는 개발 및 운영 환경에서 배치를 안전하고 정확하게 실행하는 방법을 설명합니다.

---

## 1. 개요 (Overview) 📋
신용 리스크 배치는 매일 또는 매월 말일 수행되며, 원천 데이터를 기반으로 **PD, EAD, LGD, RWA**를 단계별로 산출하여 결과 테이블(`cr_risk_results`)에 저장합니다.

### 💡 초보자를 위한 개념 설명
*   **배치(Batch)**: 수천만 건의 데이터를 하나하나 처리하지 않고, 뭉텅이로 한꺼번에 처리하는 자동화 프로그램입니다.
*   **Job**: 배치 전체 작업의 단위입니다. (예: 신용리스크 산출 Job)
*   **Step**: Job을 구성하는 세부 단계입니다. (예: 데이터 품질 체크 Step, RWA 산출 Step)

---

## 2. 배치 실행 방법 (How to Run) 🛠️

### 2.1 Gradle 명령어로 실행 (로컬/개발용)
터미널에서 아래 명령을 통해 특정 기준일(`baseDate`)의 배치를 실행할 수 있습니다.

```bash
# 특정 일자(2026-04-17)로 산출 실행
./gradlew :credit-risk-service:credit-batch:bootRun --args='--job.name=creditRiskCalculationJob baseDate=20260417'
```

### 2.2 Jar 파일로 실행 (운영용)
미리 빌드된 Jar 파일을 사용하여 배치를 실행합니다.

```bash
java -jar credit-batch.jar \
     --job.name=creditRiskCalculationJob \
     baseDate=20260417 \
     table=cr_accounts \
     column=id
```

### ⚙️ 주요 파라미터 설명
| 파라미터명 | 필수 여부 | 설명 | 예시 |
| :--- | :---: | :--- | :--- |
| `baseDate` | **필수** | 리스크 산출 기준일자 (YYYYMMDD) | `20260417` |
| `job.name` | **필수** | 실행할 Job의 이름 | `creditRiskCalculationJob` |
| `table` | 선택 | 파티셔닝 대상 테이블 (기본값: `cr_accounts`) | `cr_accounts` |
| `gridSize` | 선택 | 병렬 처리 쓰레드 개수 | `4` |

### 2.3 API를 이용한 단계별 재수행 (Partial Re-run)
배치 전체를 돌리지 않고 특정 단계만 API로 기동할 수 있습니다.

**URL**: `POST http://192.168.0.104:8080/api/v1/credit-risk/batch/run?jobName={standaloneJobName}`

| 단계 | Standalone Job 명칭 | 비즈니스 의미 |
| :--- | :--- | :--- |
| **DQ** | `standaloneDqJob` | 데이터 품질 체크만 수행 |
| **모니터링** | `standaloneMonitoringJob` | 일일 자산 현황 기록만 수행 |
| **규제매핑** | `standaloneRegulatoryContractJob` | 대출-담보 매핑 테이블 생성 |
| **담보배분** | `standaloneCollateralAllocationJob` | 고객별 담보 최적 배분 연산 |
| **PD산출** | `standaloneStagingJob` | 계좌별 스테이징 및 PD 판정 |
| **EAD/LGD** | `standaloneEadCrmJob` | 부도시 손실액 관련 파라미터 산출 |
| **ECL/RWA** | `standaloneEclJob`, `standaloneRwaJob` | 최종 규제 자본 및 충당금 계산 |

---

## 3. 실행 후 검증 (Validation) ✅

배치가 정상적으로 완료되었는지 확인하려면 DB에서 다음 쿼리를 실행해 보세요.

### 3.1 배치 상태 확인 (Spring Batch Meta Data)
```sql
SELECT job_instance_id, status, exit_code, start_time, end_time 
FROM batch_job_execution 
ORDER BY job_execution_id DESC 
LIMIT 1;
```

### 3.2 산출 결과 집계 확인
```sql
-- 기준일자별 산출 건수 및 RWA 합계 확인
SELECT base_date, 
       COUNT(*) AS total_count, 
       SUM(rwa_sa) AS total_rwa_sa, 
       SUM(rwa_irb) AS total_rwa_irb 
FROM cr_risk_results 
WHERE base_date = '2026-04-17'
GROUP BY base_date;
```

---

## 4. 장애 및 재처리 가이드 (Troubleshooting) 🆘

1.  **배치 실패 시**: 에러 로그를 먼저 확인합니다. 주로 원천 데이터 누락(`ResourceNotFoundException`)이나 DB 연결 오류가 많습니다.
2.  **재처리 방법**: 본 시스템의 배치는 **Idempotency(멱등성)**가 보장됩니다. 같은 `baseDate`로 다시 돌리면 기존 데이터를 자동으로 삭제(Preparation Step)하고 새로 산출합니다.
3.  **데이터 정합성 이상**: `dqStep` 로그를 확인하여 데이터 품질 검증을 통과하지 못한 레코드가 있는지 확인하십시오.

---
*Created by Antigravity - Batch Process Architect*

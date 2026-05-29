# 💳 대손충당금(IFRS 9) 산출 배치 (ecl-batch)

> **Role**: [백엔드 개발자]
> **Metaphor**: [대용량 데이터 공장 라인]

## 💡 초보자를 위한 개념 설명
본 모듈은 재무 결산 시스템의 **'거대한 자동화 공장'**입니다.
- `account-mart`가 만든 `allowance_exposure_snapshots`를 입력으로 받아 IFRS 9 Stage/PD, EAD/LGD, 미래전망 ECL을 산출하고 `allowance_summary`를 재생성합니다.
- 한 번에 대량의 데이터를 처리해야 하므로, 메모리 효율성과 처리 속도가 매우 중요합니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 **Adapter** 레이어 중 **Inbound Adapter** 역할을 수행합니다.
- **Batch Job/Step**: 외부 트리거(스케줄러, CLI)에 의해 구동되는 어댑터입니다.
- **Core 연동**: 실제 산식이나 비즈니스 규칙은 직접 구현하지 않고, `ecl-core`의 Port(Service, Domain)를 호출하여 공장을 가동합니다.
- 이를 통해 공장 라인(Batch 기술)이 바뀌더라도 제품 설계도(Core 로직)는 그대로 유지될 수 있습니다.

---

## 🔄 단계별 재수행 가이드 (Recovery Guide)

대손충당금(IFRS 9) 산출은 기준일 snapshot 동기화부터 회계 summary 생성까지 여러 단계로 이루어져 있습니다. 특정 단계에서 중단된 경우, 필요한 단독 Job으로 해당 지점부터 재개할 수 있습니다.

| 단계 | Job ID | 설명 | 비즈니스 의미 |
|:---:|:---|:---|:---|
| **1** | `allowanceExposureSyncStep` | snapshot 동기화 | `allowance_exposure_snapshots`를 `cr_customers`, `cr_accounts`로 upsert |
| **2** | `dqStep` | 데이터 품질 검증 | 산출 불가 계좌와 필수값 누락을 사전에 차단 |
| **3** | `stagingManagerStep` | IFRS 9 Stage/PD 산출 | 계좌별 Stage와 기초 PD를 생성 |
| **4** | `eadCrmManagerStep` | EAD/LGD 산출 | CCF와 LGD를 반영한 노출/손실률 확정 |
| **5** | `eclManagerStep` | Weighted ECL 산출 | 미래전망 시나리오 가중평균 ECL 계산 |
| **6** | `allowanceEclCompletionStep` | 완료 상태 확정 | ECL 산출 결과를 summary 집계 대상으로 확정 |
| **7** | `allowanceSummaryStep` | 회계 summary 생성 | `allowance_account_mappings`와 결합해 `allowance_summary` 재생성 |

### 🚀 실행 방법 (추천)
전체 대손충당금(IFRS 9) 산출 공정(End-to-End)을 한 번에 실행하려면 `allowanceEclJob`을 사용하세요.
```bash
./gradlew :ecl:ecl-batch:bootRun --args="--spring.profiles.active=docker --spring.batch.job.enabled=true job.name=allowanceEclJob baseDate=2026-04-18 runId=RUN-20260418 modelVersion=v1"
```

### 🧪 H2 데모 실행
`demo` 프로필은 H2 메모리 DB와 Spring Batch 메타데이터를 빠르게 띄우기 위한 용도입니다. 현재 demo SQL은 오래된 fixture와 결합되지 않도록 비워 두었으므로, 실제 산출까지 보려면 `allowance_exposure_snapshots`와 모델 마스터를 먼저 적재해야 합니다. 단독 서비스 검증 절차는 `ecl/docs/ALLOWANCE_SERVICE_RUNBOOK.md`를 따르세요.
```bash
./gradlew :ecl:ecl-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.enabled=true job.name=allowanceEclJob baseDate=2026-04-18 runId=DEMO-20260418 modelVersion=demo"
```

## 💡 초보자를 위한 금융 용어
- **ECL(기대신용손실)**: 빌려준 돈 중 못 받을 것으로 예상되는 금액 (대손충당금).
- **담보/보증 반영**: 담보가치와 보증 조건을 LGD 산출에 반영하는 절차.
- **Allowance Summary**: closing 모듈이 대손충당금 전표를 만들 때 읽는 기준일별 회계 입력 summary.

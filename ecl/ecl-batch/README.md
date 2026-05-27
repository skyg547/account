# 💳 대손충당금(IFRS9) 8단계 산출 배치 (credit-batch)

> **Role**: [백엔드 개발자]
> **Metaphor**: [대용량 데이터 공장 라인]

## 💡 초보자를 위한 개념 설명
본 모듈은 재무 결산 시스템의 **'거대한 자동화 공장'**입니다.
- 은행의 수백만 개 계좌 데이터를 컨베이어 벨트에 올려두고, 8단계의 가공 공정(Sync -> Allocation -> Calculation 등)을 거쳐 최종 대손충당금 지표를 찍어냅니다.
- 한 번에 대량의 데이터를 처리해야 하므로, 메모리 효율성과 처리 속도가 매우 중요합니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 **Adapter** 레이어 중 **Inbound Adapter** 역할을 수행합니다.
- **Batch Job/Step**: 외부 트리거(스케줄러, CLI)에 의해 구동되는 어댑터입니다.
- **Core 연동**: 실제 리스크 산식이나 비즈니스 규칙은 직접 구현하지 않고, `credit-core`의 Port(Service, Domain)를 호출하여 공장을 가동합니다.
- 이를 통해 공장 라인(Batch 기술)이 바뀌더라도 제품 설계도(Core 로직)는 그대로 유지될 수 있습니다.

---

## 🔄 단계별 재수행 가이드 (Recovery Guide)

대손충당금(IFRS9) 산출은 담보 배분 및 IRB 엔진 계산 등 매우 복잡한 공정으로 이루어져 있습니다. 특정 단계에서 중단된 경우, 해당 지점부터 수동으로 재개할 수 있습니다.

| 단계 | Job ID | 설명 | 비즈니스 의미 |
|:---:|:---|:---|:---|
| **1** | `crSyncMasterJob` | 마스터 데이터 동기화 | 규제 환율, 기준 금리, 영업일 등 마스터 정보 갱신 |
| **2** | `crRegulatoryContractJob` | 규제 계약/담보 관계 생성 | ODS 원천으로부터 규제 산출용 익스포저/담보 관계 매핑 |
| **3** | `crMonitoringJob` | 신용 모니터링 | 연체 및 조기경보(EWS) 신호 집계 및 스테이징 반영 |
| **4** | `crAllocationJob` | 일반 담보 배분 | 병렬(Parallel) 워터폴 방식을 통한 담보 최적 배분 |
| **5** | `crSpecialCollateralJob` | 부동산 특화 담보 배분 | 아파트/부동산 담보의 시세 및 헤어컷 특화 배분 수행 |
| **6** | `crCalculationJob` | RWA/ECL 본산출 (Chunk) | 대용량(500 Chunk) 멀티스레드 기반 RWA, ECL 산출 |
| **7** | `crMonthlyConsolidationJob` | 월통합 계약/자산 생성 | 산출 완료된 계약의 월별 마감 데이터 영속화 |
| **8** | `crConcentrationAnalysisJob` | 집중 대손충당금(IFRS9) 분석 | 산업별/차주별 집중도(HHI 지수) 분석 수행 |

### 🚀 실행 방법 (추천)
전체 대손충당금(IFRS9) 산출 공정(End-to-End)을 한 번에 실행하려면 `creditRiskMasterJob`을 사용하세요.
```bash
./gradlew :credit-risk-service:credit-batch:bootRun --args="--spring.profiles.active=docker --spring.batch.job.name=creditRiskMasterJob baseDate=2026-04-18 --spring.batch.job.enabled=true timestamp=$(date +%s)"
```

### 🧪 H2 데모 실행
원천 데이터를 임시로 자동 생성해서 로컬에서 바로 배치를 검증하려면 `demo` 프로필을 사용합니다.
```bash
./gradlew :credit-risk-service:credit-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=creditRiskMasterJob baseDate=2026-04-18 --spring.batch.job.enabled=true"
```

## 💡 초보자를 위한 금융 용어
- **RWA(위험가중자산)**: 은행이 가진 자산의 위험도를 점수화한 금액.
- **ECL(기대신용손실)**: 빌려준 돈 중 못 받을 것으로 예상되는 금액 (대손충당금).
- **CRM(신용위험완화)**: 담보나 보증을 통해 신용 위험을 줄이는 기법.
- **HHI(집중도 지수)**: 특정 산업이나 특정인에게 대출이 얼마나 쏠려 있는가?

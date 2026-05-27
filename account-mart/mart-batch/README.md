# 🔄 리스크 데이터 마트(CDM) ETL 배치 (mart-batch)

> **Role**: [백엔드 개발자]
> **Metaphor**: [대용량 데이터 공장 라인]

## 💡 초보자를 위한 개념 설명
본 모듈은 리스크 시스템의 **'원재료 가공 공장'**입니다.
- **ETL (추출-변환-적재)**: 은행 원장 시스템(ODS)에서 가공되지 않은 데이터를 트럭으로 실어와서, 컨베이어 벨트(Spring Batch) 위에서 리스크 표준 규격으로 닦고 조여서 저장소에 쌓아둡니다.
- 대용량 데이터를 처리하므로, '얼마나 빠르고 정확하게 공장을 돌리느냐'가 핵심입니다.

## 🏗️ 아키텍처 원칙: Hexagonal Architecture (Port/Adapter)
본 모듈은 **Inbound Adapter** 역할을 수행합니다.
- **Batch Step**: 배치 프레임워크라는 기술적 도구를 통해 내부 로직을 트리거합니다.
- **Core 엔진 연동**: 데이터 변환 규칙(Transformation)이나 검증 규칙(DQ)은 `mart-core`에서 정의된 로직(Port)을 호출하여 수행합니다.
- 공장의 설비(Batch Framework)가 바뀌어도 가공 레시피(Core 로직)는 변하지 않습니다.

---

## 🔄 단계별 재수행 가이드 (Recovery Guide)

리스크 산출의 품질은 마트 데이터의 정합성에 달려 있습니다. 특정 단계에서 정합성이 깨진 경우, 해당 단계부터 재수행할 수 있습니다.

| 단계 | Job ID | 설명 | 비즈니스 의미 |
|:---:|:---|:---|:---|
| **1** | `regulatorySyncJob` | 규제 마스터 동기화 | 외부(KAP 등) 기관의 최신 금리/등급 마스터 수집 |
| **2** | `odsReconcileJob` | 원장 DQ + 대조 | 원장/담보를 청크 단위로 검증하고 총계정원장(GL)과 계좌원장(ODS) 간 잔액 일치 확인 |
| **3** | `cdmLoadJob` | 통합 포지션 적재 | ODS 데이터를 청크/멀티스레드 기반으로 통합 리스크 모델(CDM)로 가공 및 적재 |
| **4** | `martReportingJob` | 리포팅 집계 및 요약 | 산출 엔진용 기준 데이터 및 현황 리포트 테이블(RDM) 생성 |

### 🚀 실행 방법 (추천)
전체 마트 ETL 공정을 한 번에 실행하려면 `integratedPositionEtlJob`을 사용하세요.
```bash
./gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=docker --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-18 --spring.batch.job.enabled=true timestamp=$(date +%s)"
```

### 🧪 demo 실행 방법
임시 원천 데이터를 자동 생성하고 `integratedRiskEtlJob` 전체 단계를 검증하려면 `demo` 프로필을 사용합니다.

```bash
./gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-18 --spring.batch.job.enabled=true"
```

- `demo` 프로필은 H2 파일 DB를 사용하고 `RegulatoryDataTasklet`이 기준일 샘플 ODS/GL/FX 데이터를 자동 생성합니다.
- `MartBatchJobRunner`가 `time` 파라미터를 자동 추가하므로 같은 `baseDate`로 재실행해도 `JobInstanceAlreadyCompleteException` 없이 다시 돌릴 수 있습니다.
- Kafka 브로커가 없는 테스트/로컬 검증에서는 `mart.batch.cdm-event.enabled=false`로 CDM 완료 이벤트 발행을 끌 수 있습니다.

## 대용량 처리 고도화 현황
- `kapDataEtlJob`: `FlatFileItemReader -> KapExternalRatingProcessor -> JpaItemWriter` 구조로 전환했고, `chunk=1000`, `SynchronizedItemStreamReader`, 공용 `ThreadPoolTaskExecutor`를 사용합니다.
- `integratedPositionEtlJob`: `ledgerDataQualityStep`, `collateralDataQualityStep`, `odsReconcileStep`, `cdmLoadStep`로 분리했습니다. DQ는 `JpaPagingItemReader` 기반 `chunk=500`이며, CDM 적재는 기본값을 재시작 안전한 단일 스레드로 두고 필요 시 `mart.batch.cdm-load.parallel-enabled=true`로 병렬 처리할 수 있습니다.
- `odsReconcileStep`와 구형 `BatchReconcileTasklet` 모두 집계를 DB 프로젝션으로 수행해 전체 포지션을 메모리에 적재하지 않습니다.
- ODS/CDM 계열 잡은 `baseDate` 또는 `baseDt` Job Parameter를 필수로 사용합니다. `LocalDate.now()`에 의존하지 않아야 기준일 재현성과 재실행성이 보장됩니다.
- JPA reader는 entity를 그대로 core processor에 넘기지 않고 도메인 projection으로 읽어 Port/Adapter 경계를 유지합니다.

## 남은 고도화 우선순위
- `IntegratedPositionProcessor`는 캐시로 반복 조회를 줄였지만, 여전히 고객/상품/담보/환율 보강에 리포지토리 호출이 남아 있습니다. 대량 처리 구간은 DTO 프로젝션 리더 또는 조인 기반 조회로 한 번 더 줄이는 것이 다음 단계입니다.
- 데이터 범위가 명확한 적재 구간은 계좌번호 범위 기준 partitioning을 도입하면 서버 코어를 더 적극적으로 사용할 수 있습니다.

## 💡 초보자를 위한 금융 용어
- **ETL**: 추출(Extract), 변환(Transform), 적재(Load)의 약자로, 원천 데이터를 가공하여 목적지에 쌓는 과정입니다.
- **CDM**: 통합 데이터 모델. 금리/신용/유동성 리스크가 함께 쓸 수 있는 표준화된 데이터 구조입니다.
- **Reconciliation(대조)**: 장부와 실제 데이터의 잔액이 정확히 맞는지 '검사'하는 과정입니다.

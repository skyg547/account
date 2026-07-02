# 대손충당금 입력 마트 ETL 배치

`mart-batch`는 ODS/GL 데이터를 검증하고 IFRS 9 대손충당금 산출 입력 snapshot을 생성합니다.

## 표준 Job

| 순서 | Step | 의미 |
| ---: | --- | --- |
| 1 | `preProcessStep` | 기준일 기존 CDM/snapshot 정리 |
| 2 | `ledgerDataQualityStep` | 계좌 원장 DQ |
| 3 | `collateralDataQualityStep` | 담보 DQ |
| 4 | `odsReconcileStep` | ODS와 GL 대사 |
| 5 | `cdmLoadStep` | CDM 포지션 적재 |
| 6 | `allowanceExposureSnapshotStep` | ECL 입력 snapshot 재생성 |
| 7 | `cdmEventPublishStep` | downstream 이벤트 발행 |

## Core와 Batch 경계

초보자 관점에서는 `mart-batch`를 "Spring Batch 실행 껍데기"로 보면 된다. Job/Step은 reader, processor, writer, chunk, 병렬 실행, 재시작 단위를 연결하고, Stage 판정이나 DQ 같은 업무 판단은 `mart-core`가 맡는다.

- `batch.processor.*ItemProcessor`: Spring Batch의 `ItemProcessor`/`StepExecutionListener`를 구현하는 어댑터입니다.
- `core.domain.*processor`: 실제 업무 규칙을 담는 일반 Spring 컴포넌트입니다. 테스트에서는 Spring Batch 없이 직접 호출할 수 있습니다.
- `BatchStepParameterUtils`: `StepExecution`에서 `baseDate`/`baseDt`를 읽어 core가 이해하는 `LocalDate`로 넘기는 경계 변환기입니다.

이 구조를 유지하면 배치 프레임워크를 바꿔도 core 업무 규칙 테스트는 흔들리지 않습니다.
## 실행

```powershell
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false" --console=plain --max-workers=1
```

Kafka 없이 로컬 검증할 때는 `mart.batch.cdm-event.enabled=false`를 사용합니다. Vault, Config Server, Eureka가 없는 단독 로컬 실행에서는 `spring.cloud.vault.enabled=false`, `spring.cloud.config.enabled=false`, `eureka.client.enabled=false`도 함께 넣습니다.

IntelliJ에서는 `AllowanceMartBatchApplication`을 실행 클래스로 선택하고, Program arguments에 아래 값을 넣습니다.

```text
--spring.profiles.active=demo --spring.main.web-application-type=none --spring.cloud.config.enabled=false --spring.cloud.discovery.enabled=false --spring.cloud.loadbalancer.enabled=false --spring.cloud.vault.enabled=false --eureka.client.enabled=false --management.tracing.enabled=false --spring.batch.jdbc.initialize-schema=always --spring.batch.job.enabled=true --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30 --mart.batch.cdm-event.enabled=false
```

CLI 인자에 `spring.batch.job.name`이 있으면 웹 서버를 띄우지 않고 배치 실행 후 종료합니다. 포트 충돌 없이 Job만 확인하고 싶을 때 이 방식을 사용합니다.

## 대용량 처리

- DQ와 CDM 적재는 chunk 기반으로 처리합니다.
- CDM 적재 병렬화는 `mart.batch.cdm-load.parallel-enabled=true`로 켤 수 있습니다.
- 배치 설정은 reader/processor/writer 연결과 chunk/task executor 설정만 담당합니다.

## 검증

```powershell
./gradlew :account-mart:mart-batch:test --console=plain --max-workers=1 --no-daemon
```

문서와 Job 설정을 함께 바꿨다면 `account-mart/docs/README.md`의 흐름과 `IntegratedPositionEtlJobConfig`의 실제 Step 순서가 일치하는지 확인합니다.

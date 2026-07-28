# account-mart docs

`account-mart`는 IFRS 9 대손충당금 산출에 필요한 입력 데이터를 기준일 단위로 정제하고 snapshot으로 고정하는 마트 모듈입니다.

## 추천 읽기 순서

1. [../README.md](../README.md): 모듈 전체 개요와 빠른 실행
2. [DATA_MART_BEGINNER_GUIDE.md](./DATA_MART_BEGINNER_GUIDE.md): 초보자용 업무 개념과 코드 탐색
3. [ETL_INTERFACE_SPEC.md](./ETL_INTERFACE_SPEC.md): 원천 데이터와 적재 목적
4. [DATA_MART_SPEC.md](./DATA_MART_SPEC.md): 핵심 테이블과 품질 기준
5. [BATCH_LEARNING_GUIDE.md](./BATCH_LEARNING_GUIDE.md): Spring Batch/ETL 실행 구조

하위 모듈 전용 문서:

- [../mart-core/docs/MART_CORE_GUIDE_FOR_BEGINNERS.md](../mart-core/docs/MART_CORE_GUIDE_FOR_BEGINNERS.md): `mart-core` 도메인 규칙 입문
- [../mart-batch/docs/MART_BATCH_ARCHITECTURE_GUIDE.md](../mart-batch/docs/MART_BATCH_ARCHITECTURE_GUIDE.md): `mart-batch` Job/Step 구조

## 모듈 역할

| 하위 모듈 | 역할 | 실행 여부 |
| --- | --- | --- |
| `mart-core` | ODS/CDM 도메인, DQ, snapshot 생성 규칙 | 라이브러리 |
| `mart-api` | 마트 조회 API | Spring Boot API |
| `mart-batch` | 기준일 ETL, DQ, 대사, snapshot 재생성 | Spring Boot Batch |

## 헥사고날 경계 요약

- `mart-core`는 업무 규칙의 중심입니다. DQ, ODS-GL 대사, CDM 변환, snapshot 생성 규칙은 Spring Batch 없이도 테스트할 수 있어야 합니다.
- `mart-batch`는 Spring Batch 어댑터입니다. Job/Step/Reader/Writer/Chunk 설정과 `StepExecution` 파라미터 해석을 담당하고, 실제 판단은 core 컴포넌트에 위임합니다.
- `mart-api`는 조회 API 어댑터입니다. 외부 호출자는 API/조회 계약을 통해 마트 결과를 읽고, core 도메인 객체를 직접 공유하지 않습니다.
- application port는 JPA, JDBC, Kafka 같은 기술 이름을 숨기는 계약이어야 합니다. 구현체는 infrastructure에 둡니다.
## 표준 데이터 흐름

```mermaid
flowchart LR
    ODS[ODS 원천] --> DQ[Data Quality]
    DQ --> RECON[ODS-GL 대사]
    RECON --> CDM[AllowanceInputPosition]
    CDM --> SNAP[allowance_exposure_snapshots]
    SNAP --> ECL[ecl allowanceEclJob]
```

## 로컬 실행 요약

자세한 IntelliJ/Gradle 설정은 루트 [docs/local-development.md](../../docs/local-development.md)를 기준으로 합니다.

```powershell
.\gradlew :account-mart:mart-core:test --console=plain
.\gradlew :account-mart:mart-api:bootRun --console=plain
.\gradlew :account-mart:mart-batch:bootRun --args="--spring.profiles.active=demo --spring.batch.job.name=integratedPositionEtlJob baseDate=2026-04-30" --console=plain
```

`demo` 프로파일은 H2 파일 DB와 demo seed를 사용하므로 IntelliJ에서 처음 실행해 보기 쉽습니다.

## 운영 주의사항

- `allowance_input_positions`와 관련 JPA 쓰기 소유권은 `account-mart`에 있습니다.
- `ecl`은 마트 엔티티를 직접 공유하지 않고 snapshot/조회 계약을 통해 읽습니다.
- 배치 모듈은 Job/Step/병렬성/청크 설정에 집중하고, Stage 판정이나 금액 규칙은 core/domain에 둡니다.
- 기준일 재실행 시 snapshot 재생성 범위와 ODS-GL 대사 결과를 함께 확인해야 합니다.

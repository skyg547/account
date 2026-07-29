# master-data docs

This directory documents the `master-data` module after the DDD and hexagonal package cleanup.

## Boundaries

- `api`: executable Spring Boot HTTP adapter project containing REST controllers and API DTOs.
- `batch`: executable Spring Batch adapter project containing Job/Step flow and scheduler parameters.
- `core`: reusable library project containing application, domain, and persistence adapter code.
- `core.application.port.in`: input ports used by controllers.
- `core.application.command`: use-case input commands.
- `core.application.pipeline`: 기준일과 업무 집계 순서를 소유하며 batch 패키지를 역참조하지 않는 core 보고 pipeline.
- `core.application.service`: use-case flow, validation coordination, and transaction boundary.
- `core.application.service.MasterDataChangeApplier`: 승인된 변경 요청을 targetType별 실제 SCD2 반영으로 연결한다. 계정과목, 거래처, 부서, 상품 typed applier가 구현되어 있다.
- `core.application.service.MasterDataChangeApplierRegistry`: targetType별 담당 전략을 불변 Map으로 고정하고 중복 담당자를 시작 시점에 차단한다.
- `core.application.port.out.MasterDataChangePayloadDecoder`: core에서 Jackson 기술을 숨기는 payload 역직렬화 포트.
- `core.application.port.out.MasterDataVersionQueryPort`: 애플리케이션 계층이 JPA를 모르고 업무 키별 SCD2 이력 수를 조회하는 포트.
- `core.application.port.out.MasterDataValidityStatisticsPort`: 전체 엔티티 조회 없이 기준일 활성 건수를 요청하는 대량 집계 포트.
- `core.application.port.out.FiscalPeriodPersistencePort`: Closing 상태 변경용 조회/행 잠금/저장을 숨기는 포트.
- `core.domain.model`: master-data domain entities.
- `core.domain.changerequest`: controlled change request aggregate.
- `core.domain.policy`: 유효기간과 변경 요청 목표 버전처럼 여러 엔티티가 공유하는 순수 업무 정책.
- `core.application.port.out`: output ports that hide persistence details from the core.
- `core.infrastructure.persistence`: JPA persistence adapters.
- `core.infrastructure.persistence.repository`: Spring Data repositories.
- `core.infrastructure.adapter`: lookup adapters used by other modules.
- `batch.application`: scheduler-facing orchestration and report mapping only.

## Dependency Rule

The intended direction is:

```text
api -> core.application -> core.domain
core.application -> core.application.port.out
batch -> core.application -> core.domain
core.infrastructure -> core.application.port.out and core.domain
```

`api`와 `batch`는 서로 의존하지 않습니다. Controllers must not expose JPA entities directly. Application services must depend on `port.out` interfaces rather than Spring Data repositories.

## Documents

1. [beginner-guide.md](beginner-guide.md)
2. [process-flow.md](process-flow.md)
3. [schema.md](schema.md)
4. [local-run.md](local-run.md)
5. [archive/legacy-kafka-config.md](archive/legacy-kafka-config.md): 실제 adapter 없이 잘못된 prefix에 있던 과거 Kafka 설정 기록

## Local Verification

- `master-data:api`와 `master-data:batch`는 각각 독립 Spring Boot 앱이고 `master-data:core`는 실행 진입점이 없는 library입니다.
- 로컬 실행 설정은 `.run/Master Data bootRun.run.xml`을 사용합니다.
- 단위 검증은 `.\gradlew :master-data:core:test :master-data:api:test :master-data:batch:test --console=plain --max-workers=1 --no-daemon`로 수행합니다.
- 변경 요청은 지원 전략과 SCD2 목표 버전을 요청·승인·반영 단계마다 확인하고, typed applier 성공 후에만 `APPLIED`가 됩니다.
- 승인/반려/반영은 요청 행을 비관적 잠금으로 직렬화하고 JPA `lockVersion`으로 충돌을 한 번 더 감지합니다.
- Governance `sourceReference`는 승인 재시도 멱등 키이고 `appliedAt`은 실제 typed applier 완료 시각입니다.
- 예약 반영은 `status/effectiveDate` 조건으로 최대 500건을 조회합니다. 요청별 독립 트랜잭션과 `SKIP LOCKED` 파티셔닝은 코드 TODO로 남아 있습니다.
- `Master Data bootRun` IntelliJ 설정은 Config/Discovery/Vault 없이 `local` H2 API를 단독 기동하도록 표준화되어 있습니다.
- `Master Data Batch Validity` 설정은 필수 `asOfDate`를 받는 `masterDataValidityJob`을 실행하고, Job/Step은 집계 규칙을 재구현하지 않고 core pipeline에 위임합니다.
- 활성 계정과목/상품 목록과 거래처 이름 검색도 DB 기준일 query를 사용하며, 환율은 요청일 이하 최신 한 건을 선택합니다.
- 회계기간 변경은 비관적 행 잠금과 `FiscalPeriod.changeClosingStatus` 불변식을 거칩니다.
- 전체 PostgreSQL Flyway baseline, API pagination, TaxProfile 소유권, Loan/Closing의 Master Data 직접 의존은 완료 조건이 명시된 TODO입니다.

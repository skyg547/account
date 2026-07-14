# master-data docs

This directory documents the `master-data` module after the DDD and hexagonal package cleanup.

## Boundaries

- `api`: inbound REST controllers and API DTOs.
- `core.application.port.in`: input ports used by controllers.
- `core.application.command`: use-case input commands.
- `core.application.pipeline`: 기준일과 업무 집계 순서를 소유하며 batch 패키지를 역참조하지 않는 core 보고 pipeline.
- `core.application.service`: use-case flow, validation coordination, and transaction boundary.
- `core.application.service.MasterDataChangeApplier`: 승인된 변경 요청을 targetType별 실제 SCD2 반영으로 연결한다. 계정과목, 거래처, 부서, 상품 typed applier가 구현되어 있다.
- `core.application.port.out.MasterDataChangePayloadDecoder`: core에서 Jackson 기술을 숨기는 payload 역직렬화 포트.
- `core.application.port.out.MasterDataValidityStatisticsPort`: 전체 엔티티 조회 없이 기준일 활성 건수를 요청하는 대량 집계 포트.
- `core.domain.model`: master-data domain entities.
- `core.domain.changerequest`: controlled change request aggregate.
- `core.domain.policy`: shared domain policies such as validity windows.
- `core.application.port.out`: output ports that hide persistence details from the core.
- `core.infrastructure.persistence`: JPA persistence adapters.
- `core.infrastructure.persistence.repository`: Spring Data repositories.
- `core.infrastructure.adapter`: lookup adapters used by other modules.
- `batch`: batch orchestration only.

## Dependency Rule

The intended direction is:

```text
api -> core.application -> core.domain
core.application -> core.application.port.out
core.infrastructure -> core.application.port.out and core.domain
```

Controllers must not expose JPA entities directly. Application services must depend on `port.out` interfaces rather than Spring Data repositories.

## Documents

1. [beginner-guide.md](beginner-guide.md)
2. [process-flow.md](process-flow.md)
3. [schema.md](schema.md)
4. [local-run.md](local-run.md)

## Local Verification

- `master-data`는 Spring Boot 앱입니다.
- 로컬 실행 설정은 `.run/Master Data bootRun.run.xml`을 사용합니다.
- 단위 검증은 `.\gradlew :master-data:test --console=plain --max-workers=1 --no-daemon`로 수행합니다.
- 변경 요청 반영은 typed applier 성공 후에만 `APPLIED`가 되며, 예약 반영은 `status/effectiveDate` 조건으로 chunk 조회합니다.
- `Master Data bootRun` IntelliJ 설정은 Config/Discovery/Vault 없이 `local` H2 API를 단독 기동하도록 표준화되어 있습니다.
- 일일 유효성 보고서는 필수 `asOfDate`와 DB `COUNT` 집계를 사용합니다. 별도 Spring Batch Job/Step 실행 모듈은 아직 없습니다.

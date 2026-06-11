# master-data docs

This directory documents the `master-data` module after the DDD and hexagonal package cleanup.

## Boundaries

- `api`: inbound REST controllers and API DTOs.
- `core.application.port.in`: input ports used by controllers.
- `core.application.command`: use-case input commands.
- `core.application.pipeline`: batch-specific transformation/report pipelines.
- `core.application.service`: use-case flow, validation coordination, and transaction boundary.
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

# Architecture Boundaries

## API

- Keep controllers and inbound adapters responsible for mapping, authentication/authorization context, request validation, and response conversion.
- Do not place financial formulas, state transitions, persistence implementation, or batch orchestration in the API layer.
- Return DTOs rather than persistence entities.

## Core

- `application.pipeline`: perform chunk-oriented batch transformations under domain rules.
- `application.service`: define transaction boundaries and coordinate domain components and ports.
- `application.port.in`: expose use cases to inbound adapters.
- `application.port.out`: hide persistence, messaging, and external API technology.
- `domain`: own invariants, precise calculations, and valid state transitions.
- `infrastructure`: implement outbound ports with JPA, QueryDSL, JDBC bulk SQL, messaging, or external clients.

## Batch

- Keep Trigger, Job/Step flow, partitioning, TaskExecutor, chunk size, restart parameters, and listener wiring in batch.
- Move business calculations, branching rules, and state transitions into core use cases, pipelines, or domain services.
- Define stable identifying parameters and document rerun behavior.
- Verify failure propagation and avoid silently successful jobs after business failure.

## Persistence And Scale

- Prefer JDBC bulk insert/update and partition-aware access for high-volume paths.
- Document parameter mapping, indexes, lock/transaction impact, expected cardinality, and rollback.
- Avoid N+1 access and per-row remote calls in chunk processing.
- Keep database-specific code behind outbound ports.

## Financial Safety

- Use `BigDecimal` with explicit scale and rounding policy where rounding is required.
- Test boundary dates, zero/negative amounts, duplicate execution, closing periods, approval transitions, and reconciliation totals.
- Preserve audit lineage and actor/time metadata required by the domain.

---
name: account-hexagonal-change
description: Implement or review account project changes across API, core, batch, domain, and persistence while preserving hexagonal boundaries, financial precision, and high-volume batch constraints. Use for Java or SQL changes in business modules, API/core/batch module splits, financial calculations, state transitions, or batch jobs.
---

# Account Hexagonal Change

## Read The Local Contract

1. Read `AGENTS.md`, the target module `README.md`, and its `docs/*.md`.
2. Read [architecture boundaries](references/architecture-boundaries.md).
3. Determine whether the module is split into `api`, `core`, and `batch` or is still standalone.
4. Search the full call path before editing: inbound adapter, use case, domain, outbound port, adapter, migration/query, and tests.

## Assign Ownership

- Use the `controller` agent for inbound web adapters and DTO wiring.
- Use the `service` agent for application use cases and domain collaboration.
- Use the `batch` agent for Job/Step/trigger/chunk orchestration.
- Use the `sql` agent for persistence adapters, mappings, migrations, and query performance.
- Use the `test` agent for tests.
- Give each writer a disjoint file allowlist and keep shared logs with the parent Integrator.

## Implement

1. Keep business invariants in core domain/application code.
2. Keep API and batch modules as inbound adapters that depend on core.
3. Use `BigDecimal` for monetary and precision-sensitive calculations.
4. Include meaningful validation, state transition, or domain collaboration; do not create interface-only skeletons or pass-through services.
5. Design large-volume writes around bulk operations, partitioning, idempotency, restartability, and transaction boundaries.
6. Preserve existing architecture unless the Issue explicitly authorizes a separate refactor.

## Verify

1. Test the narrow core behavior first.
2. Compile or test each affected API/batch/core project.
3. Add broader build or smoke checks when contracts, Spring wiring, SQL, or build files change.
4. Run `$account-review-handoff` and report skipped verification with reason and risk.

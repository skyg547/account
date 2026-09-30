# GH-753 agent status

- Issue: https://github.com/skyg547/account/issues/753
- Branch: `agent/753-scd2-business-key`
- Worktree: `/tmp/account-753-scd2-business-key`
- Base: `b06e7de3a2e26c1e14a15d73486c02fa7216e8b8` (`origin/main`)
- Scope: `master-data/**` only. The user prohibits shared harness edits, so the parent keeps these three module-local records instead.
- Model requested for implementation/review: gpt-6-astra, xhigh.
- State: module implementation verified; independent review HOLD for migration-runner H2 compatibility. Draft publication only; not integration-ready or complete.

## Ownership

Parent Integrator owns Git/GitHub, module documentation and these records. Service, SQL and Test roles have disjoint files; a separate read-only reviewer checks the frozen result. No other modules or shared contracts are editable.

Exact parent implementation allowlist:

- `master-data/build.gradle`
- `master-data/api/build.gradle`
- `master-data/api/src/main/java/com/ho/account/masterdata/api/web/MasterDataExceptionHandler.java`
- `master-data/api/src/main/resources/application-local.yaml`
- `master-data/batch/src/main/resources/application-local.yaml`
- `master-data/api/src/test/java/com/ho/account/masterdata/MasterDataPostgresqlSchemaContextTest.java`
- `master-data/batch/src/test/java/com/ho/account/masterdata/batch/MasterDataBatchPostgresqlSchemaContextTest.java`
- `master-data/core/src/test/java/com/ho/account/masterdata/core/infrastructure/persistence/BusinessPartnerHistoricalActiveIntegrationTest.java` (implicit H2 Flyway target8)
- `master-data/README.md`
- `master-data/docs/README.md`
- `master-data/docs/process-flow.md`
- `master-data/docs/schema.md`
- `master-data/docs/local-run.md`
- `master-data/docs/beginner-guide.md` (review follow-up: directly affected SCD2 paragraphs only)
- `master-data/docs/ai-harness/agent-status.md`
- `master-data/docs/ai-harness/worklog.md`
- `master-data/docs/ai-harness/handoff.md`

Service owns the five mutation services, four typed appliers, new business-key lock port, BP/Product scalar-key persistence port additions, conflict-aware termination policy, and three existing core service test classes. SQL owns the lock adapter/entity, five affected persistence adapters, four supported type repositories, and V8/V9 forward SQL migrations. Test owns the new PostgreSQL acceptance test and existing change-request controller test. Exact paths were assigned in each delegation before edits; the final changed-file inventory is recorded in handoff.

Publication authority: user-authorized branch push and Draft PR with `Refs #753`. Parent alone publishes. Ready, merge, Issue closure and resource deletion remain separate gates.

## Verified checkpoint

602 module tests PASS (core454/API143/Batch5; PostgreSQL75 included), failure/error/skip0. API/Batch bootJars and V8/V9 resource packaging verified. Separate runner compatibility2/2 FAIL on V9 PostgreSQL syntax in existing H2 fixtures. Source/build/test40 hashes unchanged.

The parent requested a one-file scope exception for `migration-runner/src/test/java/com/ho/account/migration/MigrationExecutorH2Test.java`; no exception is assumed without a reply. A test-only V1–V8 H2 fixture patch is prepared outside the repository and independently reviewed. Until explicitly approved, the original module-only scope remains in force and the Draft carries this blocker.
